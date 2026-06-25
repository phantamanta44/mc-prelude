package st.evening.mc.prelude.api.gui.engine

import st.evening.mc.prelude.api.gui.engine.prefab.AbsoluteLayout
import st.evening.mc.prelude.api.gui.engine.prefab.FlexLayout
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TableLayout
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
annotation class GuiElementDsl

@ClientSide.Physical
@GuiElementDsl
@JvmInline
value class GuiElementDslContext<out T : GuiElement>(val element: T) {
    inline fun configure(configureAction: T.() -> Unit) {
        configureAction(element)
    }
}

typealias GuiElementDslAction<T> = GuiElementDslContext<T>.() -> Unit

@ClientSide.Physical
inline fun <T : GuiElement> T.runAction(
    wrapper: (T) -> GuiElement = { it },
    action: GuiElementDslAction<T>
): GuiElement = wrapper(this.also { action(GuiElementDslContext(it)) })

@ClientSide.Physical
inline fun <T : GuiElement> GuiElementDslContext<AbsoluteLayout>.addChild(
    x: Int, y: Int, child: T, wrapper: (T) -> GuiElement = { it }, action: GuiElementDslAction<T> = {}
) {
    element.addChild(x, y, child.runAction(wrapper, action))
}

@ClientSide.Physical
inline fun <T : GuiElement> GuiElementDslContext<StackLayout>.addChild(
    child: T,
    alignX: DrawAlignment = DrawAlignment.CENTER,
    alignY: DrawAlignment = DrawAlignment.CENTER,
    wrapper: (T) -> GuiElement = { it },
    action: GuiElementDslAction<T> = {}
) {
    element.addChild(child.runAction(wrapper, action), alignX, alignY)
}

@ClientSide.Physical
inline fun <T : GuiElement> GuiElementDslContext<FlexLayout>.addChild(
    child: T,
    flex: Float = 0F,
    align: DrawAlignment? = null,
    wrapper: (T) -> GuiElement = { it },
    action: GuiElementDslAction<T> = {}
) {
    element.addChild(child.runAction(wrapper, action), flex, align)
}

@ClientSide.Physical
inline fun <T : GuiElement> GuiElementDslContext<TableLayout>.setChild(
    rowIndex: Int, columnIndex: Int, child: T, wrapper: (T) -> GuiElement = { it }, action: GuiElementDslAction<T> = {}
) {
    element.setChild(rowIndex, columnIndex, child.runAction(wrapper, action))
}
