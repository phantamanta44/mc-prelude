package st.evening.mc.prelude.api.item.prefab

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraftforge.client.model.ModelLoader
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.item.SubsItem
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.enumConstantCount
import st.evening.mc.prelude.api.util.data.getEnumConstantsAsList
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.mapPath
import st.evening.mc.prelude.api.util.system.Magic

interface ItemEnum2Ifc<A : Enum<A>, B : Enum<B>> : ShadowDelegate<Item>, SubsItem<Pair<A, B>>, CustomModelItem {
    val variants: List<Pair<A, B>>

    fun getMetadata(a: A, b: B): Int

    fun newStack(a: A, b: B, count: Int): ItemStack = ItemStack(self, count, getMetadata(a, b))

    override fun newStack(sub: Pair<A, B>, count: Int): ItemStack = newStack(sub.first, sub.second, count)

    fun getMetadata(damage: Int): Int = damage

    fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        if (self.isInCreativeTab(tab)) {
            variants.indices.forEach { items += ItemStack(self, 1, it) }
        }
    }

    fun getTranslationKey(stack: ItemStack): String = variants.getOrNull(stack.metadata)?.let { (a, b) ->
        "${self.translationKey}.${a.name.lowercase()}.${b.name.lowercase()}"
    } ?: self.translationKey

    @ClientSide.Physical
    override fun registerItemModels() {
        variants.forEachIndexed { i, (a, b) ->
            ModelLoader.setCustomModelResourceLocation(
                self, i,
                ModelResourceLocation(
                    self.registryName!!.mapPath { "$it/${a.name.lowercase()}/${b.name.lowercase()}" },
                    "inventory"
                )
            )
        }
    }

    class Delegate<A : Enum<A>, B : Enum<B>>(override val self: Item, aType: Class<A>, bType: Class<B>) :
        ItemEnum2Ifc<A, B> {

        private val bCount: Int = bType.enumConstantCount

        override val variants: List<Pair<A, B>> = bType.getEnumConstantsAsList().let { bs ->
            aType.getEnumConstantsAsList().flatMap { a -> bs.map { b -> a to b } }
        }

        init {
            self.hasSubtypes = true
        }

        override fun getMetadata(a: A, b: B): Int = a.ordinal * bCount + b.ordinal
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
open class PrefabItemEnum2<A : Enum<A>, B : Enum<B>>(aType: Class<A>, bType: Class<B>) : Item(),
    ItemEnum2Ifc<A, B> by ItemEnum2Ifc.Delegate(Magic.getThis(), aType, bType) {
    companion object {
        inline operator fun <reified A : Enum<A>, reified B : Enum<B>> invoke(): PrefabItemEnum2<A, B> =
            PrefabItemEnum2(A::class.java, B::class.java)
    }
}
