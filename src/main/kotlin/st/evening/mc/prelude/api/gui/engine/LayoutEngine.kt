package st.evening.mc.prelude.api.gui.engine

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import st.evening.mc.prelude.api.util.collection.ImmutableLinkedSortedSet
import st.evening.mc.prelude.api.util.collection.WeakValidityReference
import st.evening.mc.prelude.api.util.collection.containsIndex
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import java.util.NavigableSet

@ClientSide.Physical
class LayoutEngine(private val root: GuiElement) {
    companion object {
        inline fun <T : GuiElement> fromDsl(
            root: T,
            wrapper: (T) -> GuiElement = { it },
            action: GuiElementDslAction<T>
        ): LayoutEngine = LayoutEngine(root.runAction(wrapper, action))
    }

    private var activeContext: ContextImpl? = null

    fun bakeLayout(gui: GuiScreen, parentWidth: Int?, parentHeight: Int?): GuiPart {
        val rootPart = root.bake(0, 0, parentWidth, parentHeight, DrawAlignment.CENTER, DrawAlignment.CENTER)
        activeContext = ContextImpl(gui, rootPart)
        return rootPart
    }

    fun init() {
        activeContext?.init()
    }

    fun drawBackground(partialTicks: Float, mouseX: Int, mouseY: Int) {
        activeContext?.drawBackground(partialTicks, mouseX, mouseY)
    }

    fun drawForeground(partialTicks: Float, mouseX: Int, mouseY: Int) {
        activeContext?.drawForeground(partialTicks, mouseX, mouseY)
    }

    fun drawOverlay(partialTicks: Float, mouseX: Int, mouseY: Int) {
        activeContext?.drawOverlay(partialTicks, mouseX, mouseY)
    }

    fun drawTooltip(partialTicks: Float, mouseX: Int, mouseY: Int): Boolean =
        activeContext?.drawTooltip(partialTicks, mouseX, mouseY) ?: false

    fun tick() {
        activeContext?.tick()
    }

    fun onMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean =
        activeContext?.onMouseClick(mouseX, mouseY, mouseButton) ?: false

    fun onMouseDragged(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean =
        activeContext?.onMouseDragged(mouseX, mouseY, mouseButton) ?: false

    fun onMouseReleased(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean =
        activeContext?.onMouseReleased(mouseX, mouseY, mouseButton) ?: false

    fun onMouseScroll(mouseX: Int, mouseY: Int, scroll: Int): Boolean =
        activeContext?.onMouseScroll(mouseX, mouseY, scroll) ?: false

    fun onKeyTyped(typedChar: Char, keyCode: Int): Boolean = activeContext?.onKeyTyped(typedChar, keyCode) ?: false

    fun kill() {
        activeContext?.kill()
        activeContext = null
    }

    private class ContextImpl(override val gui: GuiScreen, private val part: GuiPart) : GuiContext {
        override var focused: FocusableGuiPart? by WeakValidityReference()

        private var focusTargetCache: NavigableSet<FocusableGuiPart>? = null
        private val activeDrags: Array<DragState?> = arrayOfNulls(3)

        fun init() {
            part.init(this)
        }

        fun drawBackground(partialTicks: Float, mouseX: Int, mouseY: Int) {
            part.drawBackground(this, partialTicks, mouseX, mouseY)
        }

        fun drawForeground(partialTicks: Float, mouseX: Int, mouseY: Int) {
            part.drawForeground(this, partialTicks, mouseX, mouseY)
        }

        fun drawOverlay(partialTicks: Float, mouseX: Int, mouseY: Int) {
            part.drawOverlay(this, partialTicks, mouseX, mouseY)
        }

        fun drawTooltip(partialTicks: Float, mouseX: Int, mouseY: Int): Boolean =
            part.drawTooltip(this, partialTicks, mouseX, mouseY)

        fun tick() {
            part.tick(this)
        }

        fun onMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
            when (val result = part.onMouseClick(this, mouseX, mouseY, mouseButton)) {
                ClickResult.Ignore -> return false
                ClickResult.Consume -> return true
                is ClickResult.StartDrag -> {
                    if (activeDrags.containsIndex(mouseButton)) {
                        activeDrags[mouseButton] = DragState(result.part, Minecraft.getSystemTime())
                    }
                    return true
                }
            }
        }

        fun onMouseDragged(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
            if (activeDrags.containsIndex(mouseButton)) {
                activeDrags[mouseButton]?.let { drag ->
                    drag.part.onMouseDrag(
                        this, mouseX, mouseY, mouseButton, Minecraft.getSystemTime() - drag.dragStartTime
                    )
                    return true
                }
            }
            return false
        }

        fun onMouseReleased(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
            if (activeDrags.containsIndex(mouseButton)) {
                activeDrags[mouseButton]?.let { drag ->
                    activeDrags[mouseButton] = null
                    if (drag.part.onMouseRelease(this, mouseX, mouseY, mouseButton)) {
                        return true
                    }
                }
            }
            return part.onMouseRelease(this, mouseX, mouseY, mouseButton)
        }

        fun onMouseScroll(mouseX: Int, mouseY: Int, scroll: Int): Boolean {
            return part.onMouseScroll(this, mouseX, mouseY, scroll)
        }

        fun onKeyTyped(typedChar: Char, keyCode: Int): Boolean {
            focused?.let {
                if (it.onKeyTyped(this, typedChar, keyCode)) {
                    return true
                }
            }
            if (keyCode == Keyboard.KEY_TAB) {
                val focusTargets = getFocusTargets()
                if (focusTargets.isNotEmpty()) {
                    val next = if (GuiScreen.isShiftKeyDown()) {
                        focused?.let { focusTargets.lower(it) } ?: focusTargets.last()
                    } else {
                        focused?.let { focusTargets.higher(it) } ?: focusTargets.first()
                    }
                    if (next != null) {
                        focused = next
                        return true
                    }
                }
            }
            return false
        }

        fun kill() {
            part.kill()
        }

        override fun notifyLayoutChanged(changedElement: GuiPart) {
            focusTargetCache = null // TODO more granular gui layout invalidation
        }

        private fun getFocusTargets(): NavigableSet<FocusableGuiPart> =
            focusTargetCache ?: buildFocusTargetCache().also { focusTargetCache = it }

        private fun buildFocusTargetCache(): NavigableSet<FocusableGuiPart> {
            val targets = mutableListOf<FocusableGuiPart>()
            collectFocusTargets(part, targets)
            return ImmutableLinkedSortedSet.ofBy(targets, FocusableGuiPart.FocusIndexComparator)
        }

        companion object {
            private fun collectFocusTargets(part: GuiPart, targets: MutableCollection<FocusableGuiPart>) {
                if (part is FocusableGuiPart) {
                    targets += part
                }
                part.children.forEach {
                    collectFocusTargets(it, targets)
                }
            }
        }

        private class DragState(val part: GuiPart, val dragStartTime: Long)
    }
}
