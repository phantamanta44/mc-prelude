package st.evening.mc.prelude.grs

import com.cleanroommc.groovyscript.api.GroovyPlugin
import com.cleanroommc.groovyscript.compat.mods.GroovyContainer
import com.cleanroommc.groovyscript.sandbox.RunConfig
import net.minecraftforge.fml.common.Loader
import st.evening.mc.prelude.PreludeConsts
import st.evening.mc.prelude.loader.ModRegistrarImpl

class GrSPreludePlugin : GroovyPlugin {
    override fun getModId(): String = PreludeConsts.MOD_ID

    override fun getContainerName(): String = "Prelude"

    override fun onCompatLoaded(container: GroovyContainer<*>) {
        val registrar = ModRegistrarImpl(
            Loader.instance().indexedModList[RunConfig.modMetadata.modId] ?: throw IllegalStateException(
                "Could not find GroovyScript injected mod container for mod ID: ${RunConfig.modMetadata.modId}"
            )
        )
        container.addProperty(GrSRegistrarWrapper(registrar))
    }
}
