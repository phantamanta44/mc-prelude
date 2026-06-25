package st.evening.mc.prelude.api.gui.drawable.prefab

import net.minecraft.client.gui.Gui
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.UntexturedDrawable
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.RenderingHelper

@ClientSide.Physical
class DrawableBlank(override val width: Int, override val height: Int) : UntexturedDrawable {
    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {}

    override fun resize(newWidth: Int, newHeight: Int): GuiSamplable = DrawableBlank(newWidth, newHeight)
}

@ClientSide.Physical
class DrawableRect(override val width: Int, override val height: Int, val colour: Int) : UntexturedDrawable {
    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {
        Gui.drawRect(x, y, x + drawWidth, y + drawHeight, colour)
        RenderingHelper.resetColour()
    }

    override fun resize(newWidth: Int, newHeight: Int): GuiSamplable = DrawableRect(newWidth, newHeight, colour)
}
