package st.evening.mc.prelude.example

import net.minecraft.item.Item
import st.evening.mc.prelude.api.item.SubsItem
import st.evening.mc.prelude.api.item.prefab.PrefabItemEnum
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.RegistrationHolder
import st.evening.mc.prelude.api.registration.item

class ExampleItems(reg: ModRegistrar) {
    val material: SubsItem<MaterialType> by reg.myItem("material") { PrefabItemEnum() }

    private fun <I : Item> ModRegistrar.myItem(name: String, factory: () -> I): RegistrationHolder<I> =
        item(name) { factory().also { it.creativeTab = PreludeExample.defns.tab } }
}

enum class MaterialType {
    COPPER_INGOT, STEEL_INGOT
}
