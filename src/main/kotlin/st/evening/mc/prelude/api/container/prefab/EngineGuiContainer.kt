package st.evening.mc.prelude.api.container.prefab

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import org.lwjgl.input.Mouse
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.gui.ExtraMouseInputScreen
import st.evening.mc.prelude.api.gui.prefab.EngineGuiScreen
import st.evening.mc.prelude.api.util.data.DelegateSuperFactory
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.getAndBind
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.coremod.addons.GuiContainerAddons
import java.lang.invoke.MethodHandle
import java.lang.Character.TYPE as TChar
import java.lang.Float.TYPE as TFloat
import java.lang.Integer.TYPE as TInt
import java.lang.Long.TYPE as TLong
import java.lang.Void.TYPE as TVoid

@ClientSide.Strong
interface EngineGuiContainerIfc<G> : ShadowDelegate<G>, ExtraMouseInputScreen
    where G : GuiContainer, G : EngineGuiScreen {

    fun initGui()

    fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int)

    fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int)

    fun renderHoveredToolTip(mouseX: Int, mouseY: Int)

    fun updateScreen()

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)

    fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long)

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int)

    override fun handleExtraMouseInput(mouseX: Int, mouseY: Int) {
        val scroll = Mouse.getEventDWheel()
        if (scroll != 0) {
            self.layoutEngine.onMouseScroll(mouseX, mouseY, scroll)
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int)

    fun onGuiClosed()

    @OptIn(PreludeInternal::class)
    class Delegate<G>(override val self: G) : EngineGuiContainerIfc<G> where G : GuiContainer, G : EngineGuiScreen {
        companion object {
            private val factoryInitGui: DelegateSuperFactory = superFactory("initGui", "func_73866_w_", TVoid)
            private val factoryDrawGuiContainerForegroundLayer: DelegateSuperFactory =
                superFactory("drawGuiContainerForegroundLayer", "func_146979_b", TVoid, TInt, TInt)
            private val factoryDrawGuiContainerBackgroundLayer: DelegateSuperFactory =
                superFactory("drawGuiContainerBackgroundLayer", "func_146976_a", TVoid, TFloat, TInt, TInt)
            private val factoryRenderHoveredToolTip: DelegateSuperFactory =
                superFactory("renderHoveredToolTip", "func_191948_b", TVoid, TInt, TInt)
            private val factoryUpdateScreen: DelegateSuperFactory = superFactory("updateScreen", "func_73876_c", TVoid)
            private val factoryMouseClicked: DelegateSuperFactory =
                superFactory("mouseClicked", "func_73864_a", TVoid, TInt, TInt, TInt)
            private val factoryMouseClickMove: DelegateSuperFactory =
                superFactory("mouseClickMove", "func_146273_a", TVoid, TInt, TInt, TInt, TLong)
            private val factoryMouseReleased: DelegateSuperFactory =
                superFactory("mouseReleased", "func_146286_b", TVoid, TInt, TInt, TInt)
            private val factoryKeyTyped: DelegateSuperFactory =
                superFactory("keyTyped", "func_73869_a", TVoid, TChar, TInt)
            private val factoryOnGuiClosed: DelegateSuperFactory = superFactory("onGuiClosed", "func_146281_b", TVoid)

            private fun superFactory(
                name: String,
                obfName: String,
                returnType: Class<*>,
                vararg paramTypes: Class<*>
            ): DelegateSuperFactory = DelegateSuperFactory(
                EngineGuiContainerIfc::class.java, GuiContainer::class.java, name, obfName, returnType, *paramTypes
            )
        }

        private val superInitGui: MethodHandle = factoryInitGui.getAndBind(self)!!
        private val superDrawGuiContainerForegroundLayer: MethodHandle =
            factoryDrawGuiContainerForegroundLayer.getAndBind(self)!!
        private val superDrawGuiContainerBackgroundLayer: MethodHandle? =
            factoryDrawGuiContainerBackgroundLayer.getAndBind(self)
        private val superRenderHoveredToolTip: MethodHandle = factoryRenderHoveredToolTip.getAndBind(self)!!
        private val superUpdateScreen: MethodHandle = factoryUpdateScreen.getAndBind(self)!!
        private val superMouseClicked: MethodHandle = factoryMouseClicked.getAndBind(self)!!
        private val superMouseClickMove: MethodHandle = factoryMouseClickMove.getAndBind(self)!!
        private val superMouseReleased: MethodHandle = factoryMouseReleased.getAndBind(self)!!
        private val superKeyTyped: MethodHandle = factoryKeyTyped.getAndBind(self)!!
        private val superOnGuiClosed: MethodHandle = factoryOnGuiClosed.getAndBind(self)!!

        private var partialTicks: Float = 0F

        override fun initGui() {
            superInitGui()
            val engine = self.layoutEngine
            val root = engine.bakeLayout(self, self.width, self.height)
            (self as GuiContainerAddons).`prelude$setGeometry`(root.posX, root.posY, root.width, root.height)
            engine.init()
        }

        override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
            superDrawGuiContainerForegroundLayer(mouseX, mouseY)
            RenderingHelper.pushMatrix {
                RenderingHelper.translate2(-self.guiLeft, -self.guiTop)
                self.layoutEngine.drawOverlay(partialTicks, mouseX, mouseY)
            }
        }

        override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
            this.partialTicks = partialTicks
            superDrawGuiContainerBackgroundLayer?.invoke(partialTicks, mouseX, mouseY)
            self.layoutEngine.drawBackground(partialTicks, mouseX, mouseY)
            self.layoutEngine.drawForeground(partialTicks, mouseX, mouseY)
        }

        override fun renderHoveredToolTip(mouseX: Int, mouseY: Int) {
            if (!self.layoutEngine.drawTooltip(partialTicks, mouseX, mouseY)) {
                superRenderHoveredToolTip(mouseX, mouseY)
            }
        }

        override fun updateScreen() {
            superUpdateScreen()
            self.layoutEngine.tick()
        }

        override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
            if (!self.layoutEngine.onMouseClick(mouseX, mouseY, mouseButton)) {
                superMouseClicked(mouseX, mouseY, mouseButton)
            }
        }

        override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
            if (!self.layoutEngine.onMouseDragged(mouseX, mouseY, clickedMouseButton)) {
                superMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
            }
        }

        override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
            if (!self.layoutEngine.onMouseReleased(mouseX, mouseY, state)) {
                superMouseReleased(mouseX, mouseY, state)
            }
        }

        override fun keyTyped(typedChar: Char, keyCode: Int) {
            if (!self.layoutEngine.onKeyTyped(typedChar, keyCode)) {
                superKeyTyped(typedChar, keyCode)
            }
        }

        override fun onGuiClosed() {
            self.layoutEngine.kill()
            superOnGuiClosed()
        }
    }
}

@ClientSide.Strong
@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
abstract class PrefabEngineGuiContainer<C : Container>(protected val container: C) :
    GuiContainer(container), EngineGuiScreen,
    EngineGuiContainerIfc<PrefabEngineGuiContainer<C>> by EngineGuiContainerIfc.Delegate(Magic.getThis()) {
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        self.drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
        renderHoveredToolTip(mouseX, mouseY)
    }
}
