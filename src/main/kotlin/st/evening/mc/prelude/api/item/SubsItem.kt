package st.evening.mc.prelude.api.item

import net.minecraft.item.ItemStack

interface SubsItem<S> {
    fun newStack(sub: S, count: Int): ItemStack
}
