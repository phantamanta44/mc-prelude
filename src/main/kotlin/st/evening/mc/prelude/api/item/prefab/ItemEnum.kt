package st.evening.mc.prelude.api.item.prefab

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraftforge.client.model.ModelLoader
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.item.SubsItem
import st.evening.mc.prelude.api.util.collection.containsIndex
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.getEnumConstantsAsList
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.mapPath
import st.evening.mc.prelude.api.util.system.Magic

interface ItemEnumIfc<E : Enum<E>> : ShadowDelegate<Item>, SubsItem<E>, CustomModelItem {
    val variants: List<E>

    override fun newStack(sub: E, count: Int): ItemStack = ItemStack(self, count, sub.ordinal)

    fun getMetadata(damage: Int): Int = damage

    fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        if (self.isInCreativeTab(tab)) {
            variants.indices.forEach { items += ItemStack(self, 1, it) }
        }
    }

    fun getTranslationKey(stack: ItemStack): String = stack.metadata.let {
        if (variants.containsIndex(it)) {
            "${self.translationKey}.${variants[it].name.lowercase()}"
        } else {
            self.translationKey
        }
    }

    @ClientSide.Physical
    override fun registerItemModels() {
        variants.forEachIndexed { i, sub ->
            ModelLoader.setCustomModelResourceLocation(
                self, i,
                ModelResourceLocation(self.registryName!!.mapPath { "$it/${sub.name.lowercase()}" }, "inventory")
            )
        }
    }

    class Delegate<E : Enum<E>>(override val self: Item, enumType: Class<E>) : ItemEnumIfc<E> {
        override val variants: List<E> = enumType.getEnumConstantsAsList()

        init {
            self.hasSubtypes = true
        }
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
open class PrefabItemEnum<E : Enum<E>>(enumType: Class<E>) : Item(),
    ItemEnumIfc<E> by ItemEnumIfc.Delegate(Magic.getThis(), enumType) {
    companion object {
        inline operator fun <reified E : Enum<E>> invoke(): PrefabItemEnum<E> = PrefabItemEnum(E::class.java)
    }
}
