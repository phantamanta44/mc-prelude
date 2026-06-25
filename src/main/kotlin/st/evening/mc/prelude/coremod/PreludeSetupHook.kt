package st.evening.mc.prelude.coremod

import net.minecraftforge.fml.common.ModContainerFactory
import net.minecraftforge.fml.relauncher.IFMLCallHook
import org.objectweb.asm.Type
import st.evening.mc.prelude.loader.PreludeLoader
import st.evening.mc.prelude.loader.PreludeModContainer

internal class PreludeSetupHook : IFMLCallHook {
    override fun injectData(data: Map<String, Any?>) {}

    override fun call(): Void? {
        ModContainerFactory.instance()
            .registerContainerType(Type.getType(PreludeLoader.Load::class.java), PreludeModContainer::class.java)
        return null
    }
}
