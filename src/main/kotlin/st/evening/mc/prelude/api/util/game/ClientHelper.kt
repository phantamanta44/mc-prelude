package st.evening.mc.prelude.api.util.game

import com.google.common.collect.BiMap
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.common.ModContainer
import st.evening.mc.prelude.api.util.system.asAccessible

@ClientSide.Physical
object ClientHelper {
    val tooltipFlags: ITooltipFlag
        get() = if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
            ITooltipFlag.TooltipFlags.ADVANCED
        } else {
            ITooltipFlag.TooltipFlags.NORMAL
        }

    private val guiFactories: BiMap<ModContainer, IModGuiFactory> by lazy {
        @Suppress("UNCHECKED_CAST")
        FMLClientHandler::class.java.getDeclaredField("guiFactories").asAccessible().get(FMLClientHandler.instance())
            as BiMap<ModContainer, IModGuiFactory>
    }

    fun setConfigGuiFactory(modContainer: ModContainer, factory: IModGuiFactory) {
        guiFactories[modContainer] = factory
    }
}
