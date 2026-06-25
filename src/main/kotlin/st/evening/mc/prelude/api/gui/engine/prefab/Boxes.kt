package st.evening.mc.prelude.api.gui.engine.prefab

import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiElement
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Physical
class OffsetBox(
    private val child: GuiElement,
    private val offsetX: Int,
    private val offsetY: Int,
    private val adjustSize: Boolean = false
) : GuiElement {
    override val contentWidth: Int
        get() = if (adjustSize) (child.contentWidth - offsetX).coerceAtLeast(0) else child.contentWidth
    override val contentHeight: Int
        get() = if (adjustSize) (child.contentHeight - offsetY).coerceAtLeast(0) else child.contentHeight

    override fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment,
        alignY: DrawAlignment
    ): GuiPart = child.bake(contX + offsetX, contY + offsetY, contWidth, contHeight, alignX, alignY)
}

@ClientSide.Physical
class PaddingBox(
    private val child: GuiElement,
    private val padTop: Int = 0,
    padRight: Int = 0,
    padBottom: Int = 0,
    private val padLeft: Int = 0
) : GuiElement {
    companion object {
        fun xy(child: GuiElement, padX: Int = 0, padY: Int = 0): PaddingBox =
            PaddingBox(child, padY, padX, padY, padX)

        fun all(child: GuiElement, pad: Int): PaddingBox = PaddingBox(child, pad, pad, pad, pad)
    }

    private val totalPadX: Int = padLeft + padRight
    private val totalPadY: Int = padTop + padBottom

    override val contentWidth: Int
        get() = child.contentWidth + totalPadX
    override val contentHeight: Int
        get() = child.contentHeight + totalPadY

    override fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment,
        alignY: DrawAlignment
    ): GuiPart = Part(
        child.bake(
            contX + padLeft,
            contY + padTop,
            contWidth?.let { it - totalPadX },
            contHeight?.let { it - totalPadY },
            alignX,
            alignY
        )
    )

    private inner class Part(private val childPart: GuiPart) : GuiPart by childPart {
        override val posX: Int
            get() = childPart.posX - padLeft
        override val posY: Int
            get() = childPart.posY - padTop
        override val width: Int
            get() = childPart.width + totalPadX
        override val height: Int
            get() = childPart.height + totalPadY

        override fun setPosition(context: GuiContext, x: Int, y: Int, clipBox: Rect2i?) {
            childPart.setPosition(context, x + padLeft, y + padTop, clipBox)
        }
    }
}

@ClientSide.Physical
class BackgroundBox(
    private val child: GuiElement,
    private val background: GuiDrawable
) : GuiElement {
    override val contentWidth: Int
        get() = child.contentWidth
    override val contentHeight: Int
        get() = child.contentHeight

    override fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment,
        alignY: DrawAlignment
    ): GuiPart = Part(child.bake(contX, contY, contWidth, contHeight, alignX, alignY))

    private inner class Part(private val childPart: GuiPart) : GuiPart by childPart {
        override fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            background.draw(partialTicks, posX, posY, width, height)
            childPart.drawBackground(context, partialTicks, mouseX, mouseY)
        }
    }
}

@ClientSide.Physical
class TooltipBox(
    private val child: GuiElement,
    private val getTooltip: () -> List<String>?
) : GuiElement {
    companion object {
        fun constant(child: GuiElement, tooltip: List<String>): TooltipBox = TooltipBox(child) { tooltip }
        fun constant(child: GuiElement, vararg tooltip: String): TooltipBox = constant(child, tooltip.toList())
    }

    override val contentWidth: Int
        get() = child.contentWidth
    override val contentHeight: Int
        get() = child.contentHeight

    override fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment,
        alignY: DrawAlignment
    ): GuiPart = Part(child.bake(contX, contY, contWidth, contHeight, alignX, alignY))

    private inner class Part(private val childPart: GuiPart) : GuiPart by childPart {
        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (containsPoint(mouseX, mouseY)) {
                val tooltip = getTooltip()
                if (tooltip != null) {
                    context.gui.drawHoveringText(tooltip, mouseX, mouseY)
                    return true
                }
            }
            return false
        }
    }
}
