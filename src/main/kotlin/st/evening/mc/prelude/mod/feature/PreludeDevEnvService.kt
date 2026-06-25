package st.evening.mc.prelude.mod.feature

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.FileResourcePack
import net.minecraft.client.resources.SimpleReloadableResourceManager
import net.minecraft.launchwrapper.LaunchClassLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.prelude.api.util.system.asAccessible
import st.evening.mc.prelude.coremod.PreludeSetupHook
import st.evening.mc.prelude.coremod.mixin.AbstractResourcePackAccessor
import st.evening.mc.prelude.coremod.mixin.FallbackResourceManagerAccessor
import sun.misc.URLClassPath
import sun.net.util.URLUtil
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.IdentityHashMap
import java.util.jar.JarFile

// this primarily deals with the odd way that RFG sets up the dev env runtime classpath, which can cause crashes when
// hotswapping due to stale jar file objects
@Suppress("PrivatePropertyName")
internal class PreludeDevEnvService private constructor() : Thread("Prelude Dev Env Service") {
    companion object {
        private val logger: Logger = LogManager.getLogger("prelude-devenv")

        fun init() {
            try {
                PreludeDevEnvService().start()
            } catch (e: Exception) {
                // the setup probably failed because we're in a non-openjdk environment
                // this is a non-essential feature anyways, so we can just give it up if it doesn't work
                logger.warn("Prelude dev env service initialization failed!", e)
            }
        }
    }

    init {
        isDaemon = true
    }

    private val jarFactoryFileCache: MutableMap<String, JarFile>
    private val jarFactoryUrlCache: MutableMap<JarFile, URL>

    init {
        val cJarFileFactory: Class<*> = Class.forName("sun.net.www.protocol.jar.JarFileFactory")
        @Suppress("UNCHECKED_CAST")
        jarFactoryFileCache =
            cJarFileFactory.getDeclaredField("fileCache").asAccessible().get(null) as MutableMap<String, JarFile>
        @Suppress("UNCHECKED_CAST")
        jarFactoryUrlCache =
            cJarFileFactory.getDeclaredField("urlCache").asAccessible().get(null) as MutableMap<JarFile, URL>
    }

    private val cJarLoader: Class<*> = Class.forName($$"sun.misc.URLClassPath$JarLoader")
    private val fJarLoader_jar: Field = cJarLoader.getDeclaredField("jar").asAccessible()

    private val loaders: List<*>
    private val watchService: WatchService
    private val watchTable: MutableMap<WatchKey, MutableMap<Path, WatchEntry>> = IdentityHashMap()

    init {
        val ucp = URLClassLoader::class.java.getDeclaredField("ucp").asAccessible()
            .get(PreludeSetupHook::class.java.classLoader as LaunchClassLoader) as URLClassPath
        loaders = URLClassPath::class.java.getDeclaredField("loaders").asAccessible().get(ucp) as List<*>
        val fs = FileSystems.getDefault()
        watchService = fs.newWatchService()
        try {
            val setupTable = mutableMapOf<Path, MutableMap<Path, WatchEntry>>()
            ucp.urLs.forEachIndexed { index, url ->
                if (url.protocol != "file") return@forEachIndexed
                val jarPathString = url.file.replace('/', File.separatorChar)
                if (jarPathString.endsWith('/')) return@forEachIndexed // not a jar
                val jarPath = fs.getPath(jarPathString)
                val parentPath = jarPath.parent
                val parentSubTable = setupTable[parentPath]
                if (parentSubTable != null) {
                    parentSubTable[jarPath.fileName] = WatchEntry(url, index)
                } else {
                    val newParentSubTable = mutableMapOf<Path, WatchEntry>()
                    newParentSubTable[jarPath.fileName] = WatchEntry(url, index)
                    setupTable[parentPath] = newParentSubTable
                    watchTable[parentPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)] =
                        newParentSubTable
                }
            }
        } catch (e: Exception) {
            watchService.close()
            throw e
        }
        logger.info(
            "Initialized dev env service -- watching {} files in {} directories",
            watchTable.values.sumOf { it.values.size }, watchTable.size
        )
    }

    override fun run() {
        watchService.use { svc ->
            while (true) {
                val watchKey = svc.take()
                val subTable = watchTable.getValue(watchKey)
                try {
                    watchKey.pollEvents().forEach { event ->
                        val path = event.context() as? Path ?: return@forEach
                        val (url, index) = subTable[path] ?: return@forEach
                        val loader = loaders.getOrNull(index) ?: return@forEach
                        if (cJarLoader.isInstance(loader)) {
                            if (fJarLoader_jar.get(loader) != null) {
                                logger.info("Detected change in {}; invalidating jar objects", path)
                                fJarLoader_jar.set(loader, null)
                                jarFactoryFileCache.remove(URLUtil.urlNoFragString(url))?.let {
                                    jarFactoryUrlCache.remove(it)
                                }
                                onPhysicalClient {
                                    invalidateResourcePacks((watchKey.watchable() as Path).resolve(path))
                                }
                            }
                        } else {
                            logger.warn(
                                "Detected change in {}, but it's not a jar loader, so we'll just forget about it: {}",
                                path, loader.javaClass.canonicalName
                            )
                            subTable.remove(path)
                        }
                    }
                } finally {
                    if (subTable.isEmpty()) {
                        watchKey.cancel()
                        watchTable.remove(watchKey)
                    } else {
                        watchKey.reset()
                    }
                }
            }
        }
    }

    @ClientSide.Physical
    private fun invalidateResourcePacks(path: Path) {
        val mc = Minecraft.getMinecraft()
        mc.addScheduledTask {
            val resourceManager = mc.resourceManager
            if (resourceManager is SimpleReloadableResourceManager) {
                resourceManager.domainResourceManagers.values.forEach { domainManager ->
                    (domainManager as FallbackResourceManagerAccessor).`prelude$resourcePacks`().forEach { pack ->
                        if (
                            pack is FileResourcePack &&
                            (pack as AbstractResourcePackAccessor).`prelude$resourcePackFile`().toPath() == path
                        ) {
                            pack.close()
                        }
                    }
                }
            }
        }
    }

    private data class WatchEntry(val url: URL, val index: Int)
}
