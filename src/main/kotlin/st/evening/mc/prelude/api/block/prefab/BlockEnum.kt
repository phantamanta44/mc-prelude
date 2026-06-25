package st.evening.mc.prelude.api.block.prefab

import net.minecraft.block.Block
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.IStringSerializable
import net.minecraft.util.NonNullList
import net.minecraftforge.client.model.ModelLoader
import st.evening.mc.prelude.api.block.CustomItemBlock
import st.evening.mc.prelude.api.block.CustomModelBlock
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.item.SubsItem
import st.evening.mc.prelude.api.util.collection.containsIndex
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.getEnumConstantsAsList
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.mapPath
import st.evening.mc.prelude.api.util.system.Hoist
import st.evening.mc.prelude.api.util.system.Magic

interface BlockEnum<E> where E : Enum<E>, E : IStringSerializable {
    val enumType: Class<E>
    val propVariant: PropertyEnum<E>
    val variants: List<E>
}

interface BlockEnumIfc<E, B> : ShadowDelegate<B>, CustomModelBlock, CustomItemBlock
    where E : Enum<E>, E : IStringSerializable, B : Block, B : BlockEnum<E> {

    fun getStateFromMeta(meta: Int): IBlockState =
        if (self.variants.containsIndex(meta)) {
            self.defaultState.withProperty(self.propVariant, self.variants[meta])
        } else {
            self.defaultState
        }

    fun getMetaFromState(state: IBlockState): Int = state.getValue(self.propVariant).ordinal

    fun damageDropped(state: IBlockState): Int = getMetaFromState(state)

    fun getSubBlocks(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        self.variants.indices.forEach { items += ItemStack(self, 1, it) }
    }

    fun newStack(sub: E, count: Int): ItemStack = ItemStack(self, count, sub.ordinal)

    override fun createBlockItem(): ItemBlock = PrefabItemBlockEnum(self)

    @ClientSide.Physical
    override fun registerBlockModels() {
        ModelLoader.setCustomStateMapper(self) {
            val regName = self.registryName!!
            return@setCustomStateMapper self.variants.associate { sub ->
                self.defaultState.withProperty(self.propVariant, sub) to
                    ModelResourceLocation(regName.mapPath { "$it/${sub.getName()}" }, "normal")
            }
        }
    }

    class Delegate<E, B>(override val self: B) : BlockEnumIfc<E, B>
        where E : Enum<E>, E : IStringSerializable, B : Block, B : BlockEnum<E>
}

interface ItemBlockEnumIfc<E, B> : ShadowDelegate<ItemBlock>, SubsItem<E>, CustomModelItem
    where E : Enum<E>, E : IStringSerializable, B : Block, B : BlockEnum<E> {

    val enumBlock: B

    override fun newStack(sub: E, count: Int): ItemStack = ItemStack(self, count, sub.ordinal)

    fun getMetadata(damage: Int): Int = damage

    fun getTranslationKey(stack: ItemStack): String = stack.metadata.let {
        if (enumBlock.variants.containsIndex(it)) {
            "${enumBlock.translationKey}.${enumBlock.variants[it].getName()}"
        } else {
            enumBlock.translationKey
        }
    }

    @ClientSide.Physical
    override fun registerItemModels() {
        enumBlock.variants.forEachIndexed { i, sub ->
            ModelLoader.setCustomModelResourceLocation(
                self, i, ModelResourceLocation(self.registryName!!.mapPath { "$it/${sub.getName()}" }, "inventory")
            )
        }
    }

    class Delegate<E, B>(override val self: ItemBlock, override val enumBlock: B) : ItemBlockEnumIfc<E, B>
        where E : Enum<E>, E : IStringSerializable, B : Block, B : BlockEnum<E> {
        init {
            self.hasSubtypes = true
        }
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
open class PrefabBlockEnum<E>(override val enumType: Class<E>, material: Material, mapCol: MapColor) :
    Block(material.also { Hoist.push(enumType) }, mapCol), BlockEnum<E>,
    BlockEnumIfc<E, PrefabBlockEnum<E>> by BlockEnumIfc.Delegate(Magic.getThis())
    where E : Enum<E>, E : IStringSerializable {

    companion object {
        inline operator fun <reified E> invoke(material: Material, mapCol: MapColor): PrefabBlockEnum<E>
            where E : Enum<E>, E : IStringSerializable = PrefabBlockEnum(E::class.java, material, mapCol)

        inline operator fun <reified E> invoke(material: Material): PrefabBlockEnum<E>
            where E : Enum<E>, E : IStringSerializable = PrefabBlockEnum(E::class.java, material)
    }

    final override lateinit var propVariant: PropertyEnum<E> private set
    override val variants: List<E> = enumType.getEnumConstantsAsList()

    constructor(enumType: Class<E>, material: Material) : this(enumType, material, material.materialMapColor)

    @Suppress("UNCHECKED_CAST")
    override fun createBlockState(): BlockStateContainer =
        BlockStateContainer(this, PropertyEnum.create("variant", Hoist.pop<Class<E>>()).also { propVariant = it })
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
open class PrefabItemBlockEnum<E, B>(enumBlock: B) :
    ItemBlock(enumBlock), ItemBlockEnumIfc<E, B> by ItemBlockEnumIfc.Delegate(Magic.getThis(), enumBlock)
    where E : Enum<E>, E : IStringSerializable, B : Block, B : BlockEnum<E>
