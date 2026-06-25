package st.evening.mc.prelude.api.gui.engine.prefab

import st.evening.mc.prelude.api.gui.engine.ClickResult
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiElement
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.intersects
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Physical
abstract class AbstractGuiElement : GuiElement {
    abstract fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart

    override fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment,
        alignY: DrawAlignment
    ): GuiPart {
        val width = contentWidth
        val height = contentHeight
        return bakeDimensioned(
            contWidth?.let { contX + alignX.computeOffset(IntArithmetic, width, it) } ?: contX,
            contHeight?.let { contY + alignY.computeOffset(IntArithmetic, height, it) } ?: contY,
            width,
            height
        )
    }
}

@ClientSide.Physical
abstract class AbstractCachedSizeGuiElement : AbstractGuiElement() {
    private var cachedContentWidth: Int = -1
    private var cachedContentHeight: Int = -1

    protected fun invalidateContentWidth() {
        cachedContentWidth = -1
    }

    protected fun invalidateContentHeight() {
        cachedContentHeight = -1
    }

    protected fun invalidateContentDimensions() {
        cachedContentWidth = -1
        cachedContentHeight = -1
    }

    protected abstract fun computeContentWidth(): Int

    protected abstract fun computeContentHeight(): Int

    override val contentWidth: Int
        get() {
            val cached = cachedContentWidth
            if (cached >= 0) return cached
            return computeContentWidth().also { cachedContentWidth = it }
        }
    override val contentHeight: Int
        get() {
            val cached = cachedContentHeight
            if (cached >= 0) return cached
            return computeContentHeight().also { cachedContentHeight = it }
        }
}

@ClientSide.Physical
abstract class AbstractGuiLayout(
    val fixedWidth: Int?,
    val fixedHeight: Int?
) : AbstractCachedSizeGuiElement() {
    override val contentWidth: Int
        get() {
            val fixedWidth = this.fixedWidth
            return if (fixedWidth != null && fixedWidth >= 0) fixedWidth else super.contentWidth
        }
    override val contentHeight: Int
        get() {
            val fixedHeight = this.fixedHeight
            return if (fixedHeight != null && fixedHeight >= 0) fixedHeight else super.contentHeight
        }

    override fun bake(
        contX: Int,
        contY: Int,
        contWidth: Int?,
        contHeight: Int?,
        alignX: DrawAlignment,
        alignY: DrawAlignment
    ): GuiPart {
        val width: Int
        val posX: Int
        val fixedWidth = this.fixedWidth
        if (fixedWidth != null) {
            width = if (fixedWidth < 0) super.contentWidth else fixedWidth
            posX = contWidth?.let { contX + alignX.computeOffset(IntArithmetic, width, it) } ?: contX
        } else {
            width = contWidth ?: super.contentWidth
            posX = contX
        }

        val height: Int
        val posY: Int
        val fixedHeight = this.fixedHeight
        if (fixedHeight != null) {
            height = if (fixedHeight < 0) super.contentHeight else fixedHeight
            posY = contHeight?.let { contY + alignY.computeOffset(IntArithmetic, height, it) } ?: contY
        } else {
            height = contHeight ?: super.contentHeight
            posY = contY
        }
        return bakeDimensioned(posX, posY, width, height)
    }
}

@ClientSide.Physical
abstract class AbstractGuiPart(posX: Int, posY: Int) : GuiPart {
    final override var posX: Int = posX
        private set
    final override var posY: Int = posY
        private set
    protected var inClipBox: Boolean = true
        private set
    override val visible: Boolean
        get() = inClipBox

    override var weakValidity: WeakValidity = WeakValidity.VALID

    override fun setPosition(context: GuiContext, x: Int, y: Int, clipBox: Rect2i?) {
        posX = x
        posY = y
        inClipBox = clipBox == null || intersects(clipBox)
    }

    override fun kill() {
        weakValidity = WeakValidity.WEAK_VALID
    }
}

@ClientSide.Physical
open class GuiLayoutPart(
    posX: Int,
    posY: Int,
    override val width: Int,
    override val height: Int,
    override val children: List<GuiPart>
) : AbstractGuiPart(posX, posY) {
    constructor(posX: Int, posY: Int, width: Int, height: Int, vararg children: GuiPart) :
        this(posX, posY, width, height, children.toList())

    constructor(posX: Int, posY: Int, width: Int, height: Int, child: GuiPart) :
        this(posX, posY, width, height, listOf(child))

    override fun init(context: GuiContext) {
        children.forEach { it.init(context) }
    }

    override fun setPosition(context: GuiContext, x: Int, y: Int, clipBox: Rect2i?) {
        val diffX = x - posX
        val diffY = y - posY
        super.setPosition(context, x, y, clipBox)
        children.forEach {
            it.setPosition(context, it.posX + diffX, it.posY + diffY, clipBox)
        }
    }

    override fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
        children.forEach {
            if (it.visible) {
                it.drawBackground(context, partialTicks, mouseX, mouseY)
            }
        }
    }

    override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
        children.forEach {
            if (it.visible) {
                it.drawForeground(context, partialTicks, mouseX, mouseY)
            }
        }
    }

    override fun drawOverlay(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
        children.forEach {
            if (it.visible) {
                it.drawOverlay(context, partialTicks, mouseX, mouseY)
            }
        }
    }

    override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
        children.forEach {
            if (it.visible && it.drawTooltip(context, partialTicks, mouseX, mouseY)) {
                return true
            }
        }
        return false
    }

    override fun tick(context: GuiContext) {
        children.forEach { it.tick(context) }
    }

    override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
        children.forEach {
            if (it.visible) {
                val result = it.onMouseClick(context, mouseX, mouseY, mouseButton)
                if (result != ClickResult.Ignore) {
                    return result
                }
            }
        }
        return ClickResult.Ignore
    }

    override fun onMouseRelease(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        children.forEach {
            if (it.visible && it.onMouseRelease(context, mouseX, mouseY, mouseButton)) {
                return true
            }
        }
        return false
    }

    override fun onMouseScroll(context: GuiContext, mouseX: Int, mouseY: Int, scroll: Int): Boolean {
        children.forEach {
            if (it.visible && it.onMouseScroll(context, mouseX, mouseY, scroll)) {
                return true
            }
        }
        return false
    }

    override fun kill() {
        super.kill()
        children.forEach { it.kill() }
    }
}
