package st.evening.mc.prelude.api.gui.prefab

import st.evening.mc.prelude.api.gui.engine.LayoutEngine
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
interface EngineGuiScreen {
    val layoutEngine: LayoutEngine
}

// TODO EngineGuiScreenIfc
