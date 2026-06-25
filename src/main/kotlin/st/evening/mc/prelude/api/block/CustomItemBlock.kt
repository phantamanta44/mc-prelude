package st.evening.mc.prelude.api.block

import net.minecraft.item.ItemBlock

interface CustomItemBlock {
    fun createBlockItem(): ItemBlock?
}
