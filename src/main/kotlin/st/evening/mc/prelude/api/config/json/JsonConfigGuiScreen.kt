package st.evening.mc.prelude.api.config.json

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.client.config.GuiConfig
import net.minecraftforge.fml.client.config.IConfigElement
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
open class JsonConfigGuiScreen(
    parentScreen: GuiScreen?,
    modId: String,
    configId: String?,
    title: String,
    titleLine2: String?,
    config: JsonConfig.ConfigObject
) : GuiConfig(parentScreen, showConfigObject(config), modId, configId, false, false, title, titleLine2) {
    companion object {
        fun showConfigObject(config: JsonConfig.ConfigObject): List<IConfigElement> =
            config.entries.map { (key, elem) -> AbstractJsonConfigElement.show(key, key, elem) }
    }

    constructor(
        parentScreen: GuiScreen?,
        modId: String,
        configId: String?,
        title: String,
        config: JsonConfig.ConfigObject
    ) : this(parentScreen, modId, configId, title, null, config)

    class Factory(
        private val modId: String,
        private val configId: String?,
        private val title: String,
        private val config: JsonConfig
    ) : IModGuiFactory {
        override fun initialize(minecraftInstance: Minecraft) {}

        override fun hasConfigGui(): Boolean = true

        override fun createConfigGui(parentScreen: GuiScreen?): GuiScreen =
            JsonConfigGuiScreen(parentScreen, modId, configId, title, config.root)

        override fun runtimeGuiCategories(): Set<IModGuiFactory.RuntimeOptionCategoryElement>? = null
    }
}
