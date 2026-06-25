package st.evening.mc.prelude.coremod

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex

@MCVersion("1.12.2")
@SortingIndex(1540)
internal class PreludeLoadingPlugin : IFMLLoadingPlugin {
    private val thisPackage: String
        get() = javaClass.`package`.name

    override fun getASMTransformerClass(): Array<String> = arrayOf("$thisPackage.PreludeClassTransformer")

    override fun getModContainerClass(): String? = null

    override fun getSetupClass(): String = "$thisPackage.PreludeSetupHook"

    override fun injectData(data: Map<String, Any>) {}

    override fun getAccessTransformerClass(): String? = null
}
