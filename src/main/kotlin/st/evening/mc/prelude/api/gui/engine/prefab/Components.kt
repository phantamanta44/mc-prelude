package st.evening.mc.prelude.api.gui.engine.prefab

import net.minecraft.client.gui.inventory.GuiContainer
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.drawable.prefab.PreludeDrawables
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.Rect2i

@ClientSide.Physical
class InventorySlot(
    private val slotIndex: Int,
    private val background: GuiDrawable = PreludeDrawables.slot
) : AbstractGuiElement() {
    override val contentWidth: Int
        get() = background.width
    override val contentHeight: Int
        get() = background.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun init(context: GuiContext) {
            updateSlotPosition(context, posX, posY)
        }

        override fun setPosition(context: GuiContext, x: Int, y: Int, clipBox: Rect2i?) {
            super.setPosition(context, x, y, clipBox)
            updateSlotPosition(context, x, y)
        }

        private fun updateSlotPosition(context: GuiContext, x: Int, y: Int) {
            val gui = context.gui
            check(gui is GuiContainer) { "Inventory slot added to non-container GUI: ${gui.javaClass.simpleName}" }
            val slot = gui.inventorySlots.getSlot(slotIndex)
            slot.xPos = x - gui.guiLeft + width / 2 - 8
            slot.yPos = y - gui.guiTop + height / 2 - 8
        }

        override fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            background.drawFullSize(partialTicks, posX, posY)
        }
    }
}
