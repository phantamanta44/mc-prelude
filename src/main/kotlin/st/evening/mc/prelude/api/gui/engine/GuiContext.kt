package st.evening.mc.prelude.api.gui.engine

import net.minecraft.client.gui.GuiScreen
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
interface GuiContext {
    val gui: GuiScreen
    var focused: FocusableGuiPart?

    fun notifyLayoutChanged(changedElement: GuiPart)
}
