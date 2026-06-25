package st.evening.mc.prelude.api.item

import st.evening.mc.prelude.api.util.game.ClientSide

interface CustomModelItem {
    @ClientSide.Physical
    fun registerItemModels()
}
