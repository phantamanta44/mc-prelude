package st.evening.mc.prelude.loader

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.LoadController
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.LoaderException
import net.minecraftforge.fml.common.MetadataCollection
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.ModMetadata
import net.minecraftforge.fml.common.discovery.ModCandidate
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation.EnumHolder
import net.minecraftforge.fml.common.event.FMLConstructionEvent
import net.minecraftforge.fml.common.event.FMLEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.versioning.ArtifactVersion
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion
import net.minecraftforge.fml.common.versioning.VersionRange
import st.evening.mc.prelude.api.PreludeMod
import st.evening.mc.prelude.api.util.game.Sides
import java.io.File
import java.net.URL
import java.security.cert.Certificate
import java.util.Locale

internal class PreludeModContainer<T : Any>(
    private val modClassName: String,
    private val modCandidate: ModCandidate,
    private val annotData: Map<String, Any>
) : ModContainer {
    private val modId: String

    init {
        val modId = annotData["value"] as? String ?: throw LoaderException("No mod ID specified for $modClassName")
        if (modId.isEmpty() || modId.length > 64 || modId.lowercase(Locale.ENGLISH) != modId) {
            throw LoaderException("Invalid mod ID \"$modId\" for $modClassName")
        }
        this.modId = modId
    }

    override fun getModId(): String = modId

    override fun getSource(): File = modCandidate.modContainer

    override fun getOwnedPackages(): List<String> = modCandidate.containedPackages

    override fun isImmutable(): Boolean = false

    override fun acceptableMinecraftVersionRange(): VersionRange =
        Loader.instance().minecraftModContainer.staticVersionRange

    override fun shouldLoadInEnvironment(): Boolean = annotData["sideOnly"]?.let {
        enumValueOf<Sides>((it as EnumHolder).value).test(FMLCommonHandler.instance().side)
    } != false

    override fun getCustomModProperties(): Map<String, String> = ModContainer.EMPTY_PROPERTIES

    override fun getCustomResourcePackClass(): Class<*> = if (source.isDirectory) {
        Class.forName("net.minecraftforge.fml.client.FMLFolderResourcePack", true, javaClass.classLoader)
    } else {
        Class.forName("net.minecraftforge.fml.client.FMLFileResourcePack", true, javaClass.classLoader)
    }

    // -----------------------------------------------------------------------------------------------------------------

    private var _metaCache: ModMetadataCache? = null
    private val metaCache: ModMetadataCache
        get() = _metaCache ?: throw LoaderException("Mod metadata for $modId accessed before binding")

    override fun bindMetadata(mc: MetadataCollection) {
        _metaCache = ModMetadataCache(mc.getMetadataForId(modId, mapOf()))
    }

    override fun getMetadata(): ModMetadata = metaCache.meta

    override fun getName(): String = metadata.name

    override fun getVersion(): String = metadata.version

    override fun getProcessedVersion(): ArtifactVersion = metaCache.processedVersion

    override fun getDisplayVersion(): String = version

    override fun getRequirements(): Set<ArtifactVersion> = metadata.requiredMods

    override fun getDependencies(): List<ArtifactVersion> = metadata.dependencies

    override fun getDependants(): List<ArtifactVersion> = metadata.dependants

    override fun getSortingRules(): String = metadata.printableSortingRules()

    override fun getSharedModDescriptor(): Map<String, String> = mapOf(
        "modsystem" to "FML",
        "id" to modId,
        "version" to displayVersion,
        "name" to name,
        "url" to metadata.url,
        "authors" to metadata.getAuthorList(),
        "description" to metadata.description
    )

    override fun getUpdateUrl(): URL? = metaCache.updateUrl

    // -----------------------------------------------------------------------------------------------------------------

    private var enabled: Boolean = true
    private var classVersion: Int = 0
    private var _modObjectCache: ModObjectCache<T>? = null
    private val modObjectCache: ModObjectCache<T>
        get() = _modObjectCache
            ?: throw IllegalStateException("Mod object for $modId accessed before initialization!")

    override fun setEnabledState(enabled: Boolean) {
        this.enabled = enabled
    }

    @Suppress("UnstableApiUsage")
    override fun registerBus(bus: EventBus, controller: LoadController): Boolean {
        if (!enabled) return false
        bus.register(this)
        return true
    }

    @Suppress("UnstableApiUsage")
    @Subscribe
    fun constructMod(event: FMLConstructionEvent) {
        PreludeLoader.logger.debug("Loading mod {} from file {}", modId, source)
        try {
            event.modClassLoader.addFile(source)
        } catch (e: Exception) {
            throw LoaderException("Could not add source for mod $modId to class loader: $source", e)
        }
        event.modClassLoader.clearNegativeCacheFor(modCandidate.classList)

        MinecraftForge.preloadCrashClasses(event.asmHarvestedData, modId, modCandidate.classList)

        @Suppress("UNCHECKED_CAST")
        val modObject = try {
            Class.forName(modClassName, true, event.modClassLoader).kotlin.objectInstance
        } catch (e: Exception) {
            throw LoaderException("Could not construct mod $modId", e)
        } as? PreludeMod<T> // this cast can be thought of as binding T to the PreludeMod's argument
            ?: throw LoaderException("Main class of mod $modId is not a Prelude mod: $modClassName")

        PreludeLoader.logger.debug("Initializing mod {}", modId)
        val registrar = ModRegistrarImpl(this)
        val definitions = modObject.register(registrar)

        NetworkRegistry.INSTANCE.register(
            this,
            modObject.javaClass,
            modObject.compatibleVersionRange,
            event.asmHarvestedData
        )

        val disableable = if (event.reverseDependencies.get(modId).isEmpty()) {
            ModContainer.Disableable.RESTART
        } else {
            ModContainer.Disableable.DEPENDENCIES
        }

        PreludeLoader.registerMod(modObject.javaClass, this)
        _modObjectCache = ModObjectCache(registrar, modObject, definitions, disableable)
    }

    override fun getMod(): Any = modObjectCache.modObject

    override fun matches(mod: Any?): Boolean = this.mod === mod // equality of reference!

    fun getModDefinitions(): T = modObjectCache.modDefinitions

    override fun canBeDisabled(): ModContainer.Disableable = modObjectCache.disableable

    override fun getGuiClassName(): String? = null // use ModRegistrar#setConfigGuiFactory instead

    override fun getSigningCertificate(): Certificate? = null // TODO code signing

    override fun setClassVersion(classVersion: Int) {
        this.classVersion = classVersion
    }

    override fun getClassVersion(): Int = classVersion

    @Suppress("UnstableApiUsage")
    @Subscribe
    fun handleModStateEvent(event: FMLEvent) {
        modObjectCache.registrar.handleModStateEvent(event)
    }

    private class ModMetadataCache(val meta: ModMetadata) {
        val processedVersion: ArtifactVersion = DefaultArtifactVersion(meta.modId, meta.version)
        val updateUrl: URL? = meta.updateJSON?.takeIf { it.isNotEmpty() }?.let { URL(it) }
    }

    private class ModObjectCache<T : Any>(
        val registrar: ModRegistrarImpl,
        val modObject: Any,
        val modDefinitions: T,
        val disableable: ModContainer.Disableable
    )
}
