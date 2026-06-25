package st.evening.mc.prelude.api.item.prefab

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraftforge.client.model.ModelLoader
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.item.SubsItem
import st.evening.mc.prelude.api.util.collection.forEachInline
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.mapPath
import st.evening.mc.prelude.api.util.system.Magic

interface ItemTabled<T> {
    val variants: Int2ObjectMap<T>

    fun getVariantName(variant: T): String
}

interface ItemTabledIfc<T, I> : ShadowDelegate<I>, SubsItem<Int>, CustomModelItem
    where I : Item, I : ItemTabled<T> {

    override fun newStack(sub: Int, count: Int): ItemStack = ItemStack(self, count, sub)

    fun getMetadata(damage: Int): Int = damage

    fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        if (self.isInCreativeTab(tab)) {
            self.variants.forEachInline { meta, _ -> items += ItemStack(self, 1, meta) }
        }
    }

    fun getTranslationKey(stack: ItemStack): String = self.variants[stack.metadata]?.let {
        "${self.translationKey}.${self.getVariantName(it)}"
    } ?: self.translationKey

    @ClientSide.Physical
    override fun registerItemModels() {
        self.variants.forEachInline { meta, variant ->
            ModelLoader.setCustomModelResourceLocation(
                self, meta,
                ModelResourceLocation(
                    self.registryName!!.mapPath { "$it/${self.getVariantName(variant)}" },
                    "inventory"
                )
            )
        }
    }

    class Delegate<T, I>(override val self: I) : ItemTabledIfc<T, I> where I : Item, I : ItemTabled<T> {
        init {
            self.hasSubtypes = true
        }
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
open class PrefabItemMutableTabled<T>(private val getName: (T) -> String = { it.toString() }) : Item(), ItemTabled<T>,
    ItemTabledIfc<T, PrefabItemMutableTabled<T>> by ItemTabledIfc.Delegate(Magic.getThis()) {

    private val _variants: Int2ObjectMap<T> = Int2ObjectOpenHashMap()

    override val variants: Int2ObjectMap<T> = Int2ObjectMaps.unmodifiable(_variants)

    override fun getVariantName(variant: T): String = getName(variant)

    fun addVariant(meta: Int, variant: T): ItemTabled<T> {
        check(delegate.name() == null) { "Item cannot be modified after registration!" }
        val existing = _variants.put(meta, variant)
        if (existing != null) {
            throw IllegalStateException(
                "Conflicting variant registered for meta $meta: " +
                    "${getVariantName(existing)} -> ${getVariantName(variant)}"
            )
        }
        return this
    }
}
