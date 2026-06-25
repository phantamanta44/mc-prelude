package st.evening.mc.prelude.api.gui.engine

import st.evening.mc.prelude.api.util.collection.WeaklyValid
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Physical
interface GuiElement {
    val contentWidth: Int
    val contentHeight: Int

    fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment = DrawAlignment.START,
        alignY: DrawAlignment = DrawAlignment.START
    ): GuiPart
}

@ClientSide.Physical
interface GuiPart : IntRectangle, WeaklyValid {
    val visible: Boolean

    val children: List<GuiPart>
        get() = emptyList()

    fun init(context: GuiContext) {}

    fun setPosition(context: GuiContext, x: Int, y: Int, clipBox: Rect2i?)

    fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {}

    fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {}

    fun drawOverlay(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {}

    fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean = false

    fun tick(context: GuiContext) {}

    fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult = ClickResult.Ignore

    fun onMouseDrag(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int, dragTime: Long) {
        throw UnsupportedOperationException()
    }

    fun onMouseRelease(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean = false

    fun onMouseScroll(context: GuiContext, mouseX: Int, mouseY: Int, scroll: Int): Boolean = false

    fun kill() {}
}

@ClientSide.Physical
sealed interface ClickResult {
    object Ignore : ClickResult
    object Consume : ClickResult
    class StartDrag(val part: GuiPart) : ClickResult
}

@ClientSide.Physical
interface FocusableGuiPart : GuiPart {
    val isFocusable: Boolean
        get() = true

    val focusIndex: Int
        get() = 0

    fun onKeyTyped(context: GuiContext, typedChar: Char, keyCode: Int): Boolean = false

    object FocusIndexComparator : Comparator<FocusableGuiPart> {
        override fun compare(o1: FocusableGuiPart, o2: FocusableGuiPart): Int = o1.focusIndex.compareTo(o2.focusIndex)
    }
}
