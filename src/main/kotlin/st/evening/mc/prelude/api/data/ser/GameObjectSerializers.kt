package st.evening.mc.prelude.api.data.ser

import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.common.registry.ForgeRegistries
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TJsonDsl
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.data.tjson.forEachString
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.hasString
import st.evening.mc.prelude.api.util.data.readFluidStack
import st.evening.mc.prelude.api.util.data.writeFluidStack
import st.evening.mc.prelude.api.util.game.RegistryHelper
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.game.getCapsTag
import st.evening.mc.prelude.api.util.game.hasCapsTag

object ItemStackSerializer : FullSerializer<ItemStack, NBTTagCompound, TJson.Object> {
    private const val SER_ITEM: String = "item"
    private const val SER_COUNT: String = "count"
    private const val SER_META: String = "meta"
    private const val SER_TAG: String = "tag"
    private const val SER_CAPS: String = "caps"

    override fun serializeToNbt(x: ItemStack): NBTTagCompound = if (x.isEmpty) NBTTagCompound() else x.serializeNBT()

    override fun deserializeFromNbt(dto: NBTTagCompound): ItemStack =
        if (dto.hasString("id")) ItemStack(dto) else ItemStack.EMPTY

    override fun serializeToNetwork(buf: PacketBuffer, x: ItemStack) {
        buf.writeItemStack(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): ItemStack = buf.readItemStack()

    override fun serializeToJson(x: ItemStack): TJson.Object = if (x.isEmpty) TJson.Object() else TJsonDsl.obj {
        SER_ITEM string x.item.registryName.toString()
        SER_COUNT int x.count
        val meta = x.metadata
        if (meta != 0) {
            SER_META int meta
        }
        x.dataTagOrNull?.let {
            SER_TAG elem NbtCompoundSerializer.serializeToJson(it)
        }
        x.getCapsTag()?.let {
            SER_CAPS elem NbtCompoundSerializer.serializeToJson(it)
        }
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): ItemStack {
        if (dto.isEmpty()) return ItemStack.EMPTY
        val count = dto.expectIntValue(SER_COUNT)
        if (count <= 0) return ItemStack.EMPTY
        val item = dto.useStringValue(SER_ITEM) {
            RegistryHelper.getAssertItem(contextOf<JsonPath>(), it)
        }
        if (item == Items.AIR) return ItemStack.EMPTY
        val stack = ItemStack(
            item, count, dto.expectInt(SER_META) ?: 0,
            dto.useObject(SER_CAPS) { NbtCompoundSerializer.deserializeFromJson(it) }
        )
        dto.useObject(SER_TAG) {
            stack.tagCompound = NbtCompoundSerializer.deserializeFromJson(it)
        }
        return stack
    }
}

object FluidStackSerializer : FullSerializer<FluidStack?, NBTTagCompound, TJson.Object> {
    private const val SER_FLUID: String = "fluid"
    private const val SER_AMOUNT: String = "amount"
    private const val SER_TAG: String = "tag"

    override fun serializeToNbt(x: FluidStack?): NBTTagCompound = x?.writeToNBT(NBTTagCompound()) ?: NBTTagCompound()

    override fun deserializeFromNbt(dto: NBTTagCompound): FluidStack? = FluidStack.loadFluidStackFromNBT(dto)

    override fun serializeToNetwork(buf: PacketBuffer, x: FluidStack?) {
        buf.writeFluidStack(x)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): FluidStack? = buf.readFluidStack()

    override fun serializeToJson(x: FluidStack?): TJson.Object = if (x == null) TJson.Object() else TJsonDsl.obj {
        SER_FLUID string x.fluid.name
        SER_AMOUNT int x.amount
        x.tag?.let {
            SER_TAG elem NbtCompoundSerializer.serializeToJson(it)
        }
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): FluidStack? {
        if (dto.isEmpty()) return null
        val amount = dto.expectIntValue(SER_AMOUNT)
        if (amount <= 0) return null
        val fluid = dto.useStringValue(SER_FLUID) {
            RegistryHelper.getAssertFluid(contextOf<JsonPath>(), it)
        }
        return FluidStack(
            fluid,
            amount,
            dto.useObject(SER_TAG) {
                NbtCompoundSerializer.deserializeFromJson(it)
            }
        )
    }
}

object BlockStateSerializer : FullSerializer<IBlockState, NBTTagCompound, TJson.Object> {
    private const val SER_BLOCK: String = "block"
    private const val SER_PROPS: String = "props"

    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> IProperty<T>.getNameUnchecked(value: Comparable<*>): String = getName(value as T)

    private fun <T : Comparable<T>> IBlockState.prop(property: IProperty<T>, value: String): IBlockState {
        val parsed = property.parseValue(value)
        return if (parsed.isPresent) withProperty(property, parsed.get()) else this
    }

    context(_: JsonPath)
    private fun <T : Comparable<T>> IBlockState.propWithPath(property: IProperty<T>, value: String): IBlockState {
        val parsed = property.parseValue(value)
        if (!parsed.isPresent) {
            throw SerializationException.withPath("Invalid value for property ${property.name}: $value")
        }
        return withProperty(property, parsed.get())
    }

    override fun serializeToNbt(x: IBlockState): NBTTagCompound = NbtDsl.compound {
        SER_BLOCK string x.block.registryName.toString()
        if (x.properties.isNotEmpty()) {
            SER_PROPS compound {
                x.properties.forEach { (k, v) ->
                    k.name string k.getNameUnchecked(v)
                }
            }
        }
    }

    override fun deserializeFromNbt(dto: NBTTagCompound): IBlockState {
        val blockId = dto.getString(SER_BLOCK) ?: return Blocks.AIR.defaultState
        val block = ForgeRegistries.BLOCKS.getValue(ResourceLocation(blockId)) ?: return Blocks.AIR.defaultState
        var state = block.defaultState
        dto.getCompoundOrNull(SER_PROPS)?.let { propsDto ->
            val stateCont = block.blockState
            propsDto.keySet.forEach { propName ->
                val prop = stateCont.getProperty(propName) ?: return@forEach
                state = state.prop(prop, propsDto.getString(propName))
            }
        }
        return state
    }

    override fun serializeToNetwork(buf: PacketBuffer, x: IBlockState) {
        buf.writeVarInt(Block.getStateId(x))
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): IBlockState = Block.getStateById(buf.readVarInt())

    override fun serializeToJson(x: IBlockState): TJson.Object = TJsonDsl.obj {
        SER_BLOCK string x.block.registryName.toString()
        if (x.properties.isNotEmpty()) {
            SER_PROPS obj {
                x.properties.forEach { (k, v) ->
                    k.name string k.getNameUnchecked(v)
                }
            }
        }
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): IBlockState {
        if (dto.isEmpty()) return Blocks.AIR.defaultState
        val block = dto.useStringValue(SER_BLOCK) {
            RegistryHelper.getAssertBlock(contextOf<JsonPath>(), it)
        }
        var state = block.defaultState
        dto.useObject(SER_PROPS) { propsDto ->
            val stateCont = block.blockState
            propsDto.forEachString { key, value ->
                val prop = stateCont.getProperty(key)
                    ?: throw SerializationException.withPath("Block ${block.registryName} has no such property: $key")
                state = state.propWithPath(prop, value)
            }
        }
        return state
    }
}
