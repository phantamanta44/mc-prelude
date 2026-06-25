package st.evening.mc.prelude.api.util.game

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.prelude.api.data.inbt.ImmutableNbt
import st.evening.mc.prelude.api.data.inbt.matchesNullable
import st.evening.mc.prelude.api.data.inbt.toImmutable
import st.evening.mc.prelude.api.data.ser.FullSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TJsonDsl
import st.evening.mc.prelude.api.data.tjson.expectByte
import st.evening.mc.prelude.api.data.tjson.expectShort
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.getStringOrNull
import st.evening.mc.prelude.api.util.data.hasString
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.data.readString
import java.util.function.Predicate

data class BlockKey(val block: Block, val meta: Int = 0) : Predicate<IBlockState> {
    fun matches(state: IBlockState): Boolean = state.block == block && state.block.getMetaFromState(state) == meta

    override fun test(t: IBlockState): Boolean = matches(t)

    @Suppress("DEPRECATION")
    fun asState(): IBlockState = block.getStateFromMeta(meta)

    object Serializer : FullSerializer<BlockKey, NBTTagCompound, TJson.Object> {
        private const val SER_BLOCK: String = "block"
        private const val SER_META: String = "meta"

        override fun serializeToNbt(x: BlockKey): NBTTagCompound = NbtDsl.compound {
            SER_BLOCK string x.block.registryName.toString()
            if (x.meta != 0) {
                SER_META byte x.meta.toByte()
            }
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): BlockKey =
            BlockKey(RegistryHelper.getAssertBlock(dto.getString(SER_BLOCK)), dto.getByte(SER_META).toInt())

        override fun serializeToJson(x: BlockKey): TJson.Object = TJsonDsl.obj {
            SER_BLOCK string x.block.registryName.toString()
            if (x.meta != 0) {
                SER_META byte x.meta.toByte()
            }
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): BlockKey = BlockKey(
            dto.useStringValue(SER_BLOCK) {
                RegistryHelper.getAssertBlock(contextOf<JsonPath>(), it)
            },
            dto.expectByte(SER_META)?.toInt() ?: 0
        )

        override fun serializeToNetwork(buf: PacketBuffer, x: BlockKey) {
            buf.writeInt(Block.getIdFromBlock(x.block)).writeByte(x.meta)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): BlockKey =
            BlockKey(RegistryHelper.getAssertBlock(buf.readInt()), buf.readByte().toInt())

        object Nullable : FullSerializer<BlockKey?, NBTTagCompound, TJson.Object> {
            override fun serializeToNbt(x: BlockKey?): NBTTagCompound {
                if (x == null) return NBTTagCompound()
                return NbtDsl.compound {
                    SER_BLOCK string x.block.registryName.toString()
                    if (x.meta != 0) {
                        SER_META byte x.meta.toByte()
                    }
                }
            }

            override fun deserializeFromNbt(dto: NBTTagCompound): BlockKey? = dto.getStringOrNull(SER_BLOCK)?.let {
                BlockKey(RegistryHelper.getAssertBlock(it), dto.getByte(SER_META).toInt())
            }

            override fun serializeToJson(x: BlockKey?): TJson.Object {
                if (x == null) return TJson.Object()
                return TJsonDsl.obj {
                    SER_BLOCK string x.block.registryName.toString()
                    if (x.meta != 0) {
                        SER_META byte x.meta.toByte()
                    }
                }
            }

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Object): BlockKey? {
                val block = dto.useString(SER_BLOCK) {
                    RegistryHelper.getAssertBlock(contextOf<JsonPath>(), it)
                } ?: return null
                return BlockKey(block, dto.expectByte(SER_META)?.toInt() ?: 0)
            }

            override fun serializeToNetwork(buf: PacketBuffer, x: BlockKey?) {
                if (x == null) {
                    buf.writeVarInt(0)
                } else {
                    buf.writeVarInt(Block.getIdFromBlock(x.block) + 1).writeByte(x.meta)
                }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): BlockKey? {
                val id = buf.readVarInt()
                if (id == 0) return null
                return BlockKey(RegistryHelper.getAssertBlock(id - 1), buf.readByte().toInt())
            }
        }
    }
}

data class ItemKey(
    val item: Item,
    val meta: Int = 0,
    val dataTag: ImmutableNbt.Compound? = null,
    val capsTag: ImmutableNbt.Compound = ImmutableNbt.Compound.EMPTY
) : Predicate<ItemStack> {
    init {
        require(item != Items.AIR) { "Cannot create an item key for air!" }
    }

    fun matches(stack: ItemStack): Boolean = stack.item == item && stack.metadata == meta &&
        dataTag matchesNullable stack.dataTagOrNull && stack.areCapsCompatible(capsTag)

    override fun test(t: ItemStack): Boolean = matches(t)

    fun newStack(count: Int): ItemStack = item.newStackWithData(count, meta, dataTag?.toNbt(), capsTag.toNbt())

    companion object {
        fun fromStack(stack: ItemStack): ItemKey? = if (stack.isEmpty) null else ItemKey(
            stack.item,
            stack.metadata,
            stack.dataTagOrNull?.toImmutable(),
            stack.getCapsTag()?.toImmutable() ?: ImmutableNbt.Compound.EMPTY
        )
    }

    object Serializer : FullSerializer<ItemKey, NBTTagCompound, TJson.Object> {
        private const val SER_ITEM: String = "item"
        private const val SER_META: String = "meta"
        private const val SER_TAG: String = "tag"
        private const val SER_CAPS: String = "caps"

        override fun serializeToNbt(x: ItemKey): NBTTagCompound = NbtDsl.compound {
            SER_ITEM string x.item.registryName.toString()
            if (x.meta != 0) {
                // technically meta can be any integer; only *damage* is actually limited to shorts
                // thus, we must hope that nobody defines meta != damage
                SER_META short x.meta.toShort()
            }
            x.dataTag?.let { SER_TAG tag it.toNbt() }
            if (x.capsTag.entries.isNotEmpty()) {
                SER_CAPS tag x.capsTag.toNbt()
            }
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): ItemKey = ItemKey(
            RegistryHelper.getAssertItem(dto.getString(SER_ITEM)),
            dto.getShort(SER_META).toInt(),
            dto.getCompoundOrNull(SER_TAG)?.toImmutable(),
            dto.getCompoundOrNull(SER_CAPS)?.toImmutable() ?: ImmutableNbt.Compound.EMPTY
        )

        override fun serializeToJson(x: ItemKey): TJson.Object = TJsonDsl.obj {
            SER_ITEM string x.item.registryName.toString()
            if (x.meta != 0) {
                SER_META short x.meta.toShort() // ditto^
            }
            x.dataTag?.let {
                SER_TAG elem ImmutableNbt.Compound.Serializer.serializeToJson(it)
            }
            if (x.capsTag.entries.isNotEmpty()) {
                SER_CAPS elem ImmutableNbt.Compound.Serializer.serializeToJson(x.capsTag)
            }
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): ItemKey = ItemKey(
            dto.useStringValue(SER_ITEM) {
                RegistryHelper.getAssertItem(contextOf<JsonPath>(), it)
            },
            dto.expectShort(SER_META)?.toInt() ?: 0,
            dto.useObject(SER_TAG) {
                ImmutableNbt.Compound.Serializer.deserializeFromJson(it)
            },
            dto.useObject(SER_CAPS) {
                ImmutableNbt.Compound.Serializer.deserializeFromJson(it)
            } ?: ImmutableNbt.Compound.EMPTY
        )

        override fun serializeToNetwork(buf: PacketBuffer, x: ItemKey) {
            buf.writeInt(Item.REGISTRY.getIDForObject(x.item)).writeShort(x.meta) // ditto^
            ImmutableNbt.Compound.Serializer.Nullable.serializeToNetwork(buf, x.dataTag)
            ImmutableNbt.Compound.Serializer.serializeToNetwork(buf, x.capsTag)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): ItemKey = ItemKey(
            RegistryHelper.getAssertItem(buf.readInt()),
            buf.readShort().toInt(),
            ImmutableNbt.Compound.Serializer.Nullable.deserializeFromNetwork(buf),
            ImmutableNbt.Compound.Serializer.deserializeFromNetwork(buf)
        )

        object Nullable : FullSerializer<ItemKey?, NBTTagCompound, TJson.Object> {
            override fun serializeToNbt(x: ItemKey?): NBTTagCompound =
                if (x != null) Serializer.serializeToNbt(x) else NBTTagCompound()

            override fun deserializeFromNbt(dto: NBTTagCompound): ItemKey? =
                if (dto.hasString(SER_ITEM)) Serializer.deserializeFromNbt(dto) else null

            override fun serializeToJson(x: ItemKey?): TJson.Object =
                if (x != null) Serializer.serializeToJson(x) else TJson.Object()

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Object): ItemKey? =
                if (dto[SER_ITEM] is TJson.String) Serializer.deserializeFromJson(dto) else null

            override fun serializeToNetwork(buf: PacketBuffer, x: ItemKey?) {
                if (x == null) {
                    buf.writeInt(0)
                } else {
                    buf.writeInt(Item.REGISTRY.getIDForObject(x.item) + 1).writeShort(x.meta)
                    ImmutableNbt.Compound.Serializer.Nullable.serializeToNetwork(buf, x.dataTag)
                    ImmutableNbt.Compound.Serializer.serializeToNetwork(buf, x.capsTag)
                }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): ItemKey? {
                val id = buf.readInt()
                if (id == 0) return null
                return ItemKey(
                    RegistryHelper.getAssertItem(id - 1),
                    buf.readShort().toInt(),
                    ImmutableNbt.Compound.Serializer.Nullable.deserializeFromNetwork(buf),
                    ImmutableNbt.Compound.Serializer.deserializeFromNetwork(buf)
                )
            }
        }
    }
}

infix fun ItemKey?.matchesNullable(stack: ItemStack): Boolean = this?.matches(stack) ?: stack.isEmpty

data class FluidKey(val fluid: Fluid, val dataTag: ImmutableNbt.Compound? = null) : Predicate<FluidStack?> {
    constructor(stack: FluidStack) : this(stack.fluid, stack.tag.toImmutable())

    fun matches(stack: FluidStack?): Boolean =
        stack != null && stack.amount > 0 && stack.fluid == fluid && dataTag matchesNullable stack.tag

    override fun test(t: FluidStack?): Boolean = matches(t)

    fun newStack(amount: Int): FluidStack? = orNull(amount > 0) { FluidStack(fluid, amount, dataTag?.toNbt()) }

    companion object {
        fun fromStack(stack: FluidStack?): FluidKey? = stack?.let { FluidKey(it) }
    }

    object Serializer : FullSerializer<FluidKey, NBTTagCompound, TJson.Object> {
        private const val SER_FLUID: String = "fluid"
        private const val SER_TAG: String = "tag"

        override fun serializeToNbt(x: FluidKey): NBTTagCompound = NbtDsl.compound {
            SER_FLUID string FluidRegistry.getFluidName(x.fluid)
            x.dataTag?.let { SER_TAG tag it.toNbt() }
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): FluidKey = FluidKey(
            RegistryHelper.getAssertFluid(dto.getString(SER_FLUID)),
            dto.getCompoundOrNull(SER_TAG)?.toImmutable()
        )

        override fun serializeToJson(x: FluidKey): TJson.Object = TJsonDsl.obj {
            SER_FLUID string FluidRegistry.getFluidName(x.fluid)
            x.dataTag?.let {
                SER_TAG elem ImmutableNbt.Compound.Serializer.serializeToJson(it)
            }
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): FluidKey = FluidKey(
            dto.useStringValue(SER_FLUID) {
                RegistryHelper.getAssertFluid(contextOf<JsonPath>(), it)
            },
            dto.useObject(SER_TAG) {
                ImmutableNbt.Compound.Serializer.deserializeFromJson(it)
            }
        )

        override fun serializeToNetwork(buf: PacketBuffer, x: FluidKey) {
            // fluids get numeric ids, but we're told not to use them (see FluidRegistry::getRegisteredFluidIDs)
            buf.writeString(FluidRegistry.getFluidName(x.fluid))
            ImmutableNbt.Compound.Serializer.Nullable.serializeToNetwork(buf, x.dataTag)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): FluidKey = FluidKey(
            RegistryHelper.getAssertFluid(buf.readString()),
            ImmutableNbt.Compound.Serializer.Nullable.deserializeFromNetwork(buf)
        )

        object Nullable : FullSerializer<FluidKey?, NBTTagCompound, TJson.Object> {
            override fun serializeToNbt(x: FluidKey?): NBTTagCompound =
                if (x != null) Serializer.serializeToNbt(x) else NBTTagCompound()

            override fun deserializeFromNbt(dto: NBTTagCompound): FluidKey? =
                if (dto.hasString(SER_FLUID)) Serializer.deserializeFromNbt(dto) else null

            override fun serializeToJson(x: FluidKey?): TJson.Object =
                if (x != null) Serializer.serializeToJson(x) else TJson.Object()

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Object): FluidKey? =
                if (dto[SER_FLUID] is TJson.String) Serializer.deserializeFromJson(dto) else null

            override fun serializeToNetwork(buf: PacketBuffer, x: FluidKey?) {
                if (x == null) {
                    buf.writeString("")
                } else {
                    buf.writeString(FluidRegistry.getFluidName(x.fluid)) // ditto^
                    ImmutableNbt.Compound.Serializer.Nullable.serializeToNetwork(buf, x.dataTag)
                }
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): FluidKey? {
                val fluidName = buf.readString()
                if (fluidName.isEmpty()) return null
                return FluidKey(
                    RegistryHelper.getAssertFluid(fluidName),
                    ImmutableNbt.Compound.Serializer.Nullable.deserializeFromNetwork(buf)
                )
            }
        }
    }
}

infix fun FluidKey?.matchesNullable(stack: FluidStack?): Boolean =
    this?.matches(stack) ?: (stack == null || stack.amount <= 0)
