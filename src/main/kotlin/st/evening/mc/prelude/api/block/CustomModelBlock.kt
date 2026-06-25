package st.evening.mc.prelude.api.block

import st.evening.mc.prelude.api.util.game.ClientSide

interface CustomModelBlock {
    @ClientSide.Physical
    fun registerBlockModels()
}
