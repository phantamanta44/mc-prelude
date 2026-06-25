package st.evening.mc.prelude.api.util.game

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.util.NonNullList
import net.minecraftforge.oredict.OreDictionary
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.FullSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.collection.forEachInline
import st.evening.mc.prelude.api.util.collection.getOrPutNonNull
import st.evening.mc.prelude.api.util.data.readString
import st.evening.mc.prelude.api.util.system.asAccessible
import java.util.IdentityHashMap
import java.util.function.Predicate

class OreDictCache(reg: ModRegistrar) {
    @OptIn(PreludeInternal::class)
    private val itemEntries: MutableMap<Item, ItemEntry> = IdentityHashMap()

    init {
        OreDictHelper.idToStackUn.forEachIndexed { oreId, stacks ->
            stacks.forEach {
                cacheOreStack(oreId, it)
            }
        }
        reg.on<OreDictionary.OreRegisterEvent> { event ->
            cacheOreStack(OreDictionary.getOreID(event.name), event.ore)
        }
    }

    @OptIn(PreludeInternal::class)
    private fun cacheOreStack(oreId: Int, stack: ItemStack) {
        val damage = stack.itemDamage
        if (damage == OreDictionary.WILDCARD_VALUE) {
            itemEntries.getOrPut(stack.item) { ItemEntry() }.wildcard += oreId
        } else {
            itemEntries.getOrPut(stack.item) { ItemEntry() }.byDamage
                .getOrPutNonNull(damage) { IntOpenHashSet() } += oreId
        }
    }

    @PreludeInternal
    fun getEntry(item: Item): ItemEntry? = itemEntries[item]

    @OptIn(PreludeInternal::class)
    fun matches(oreId: Int, item: Item, damage: Int): Boolean {
        val entry = itemEntries[item] ?: return false
        if (oreId in entry.wildcard) return true
        val byDamage = entry.byDamage[damage] ?: return false
        return oreId in byDamage
    }

    @OptIn(PreludeInternal::class)
    inline fun forEachOreId(item: Item, damage: Int, action: (Int) -> Unit) {
        val entry = getEntry(item) ?: return
        entry.wildcard.forEachInline(action)
        entry.byDamage[damage]?.forEachInline(action)
    }

    @PreludeInternal
    class ItemEntry {
        val wildcard: IntSet = IntOpenHashSet()
        val byDamage: Int2ObjectMap<IntSet> = Int2ObjectOpenHashMap()
    }
}

object OreDictHelper {
    @Suppress("UNCHECKED_CAST")
    internal val idToStackUn: List<NonNullList<ItemStack>> =
        OreDictionary::class.java.getDeclaredField("idToStackUn").asAccessible().get(null)
            as List<NonNullList<ItemStack>>

    fun getOreStacks(oreName: String): List<ItemStack> =
        if (OreDictionary.doesOreNameExist(oreName)) getOreStacks(OreDictionary.getOreID(oreName)) else emptyList()

    fun getOreStacks(oreId: Int): List<ItemStack> = if (oreId < idToStackUn.size) idToStackUn[oreId] else emptyList()

    fun matches(oreName: String, stack: ItemStack): Boolean = matches(oreName, stack.item, stack.itemDamage)

    fun matches(oreName: String, key: ItemKey?): Boolean = key != null && matches(oreName, key.item, key.meta)

    fun matches(oreName: String, item: Item, damage: Int): Boolean =
        OreDictionary.doesOreNameExist(oreName) && matches(OreDictionary.getOreID(oreName), item, damage)

    fun matches(oreId: Int, stack: ItemStack): Boolean = matches(oreId, stack.item, stack.itemDamage)

    fun matches(oreId: Int, key: ItemKey?): Boolean = key != null && matches(oreId, key.item, key.meta)

    fun matches(oreId: Int, item: Item, damage: Int): Boolean = Prelude.defns.oreDictCache.matches(oreId, item, damage)

    inline fun forEachOreId(stack: ItemStack, action: (Int) -> Unit) {
        forEachOreId(stack.item, stack.itemDamage, action)
    }

    inline fun forEachOreId(key: ItemKey?, action: (Int) -> Unit) {
        if (key == null) return
        forEachOreId(key.item, key.meta, action)
    }

    inline fun forEachOreId(item: Item, damage: Int, action: (Int) -> Unit) {
        Prelude.defns.oreDictCache.forEachOreId(item, damage, action)
    }

    fun getOreIds(stack: ItemStack): IntSet = getOreIds(stack.item, stack.itemDamage)

    fun getOreIds(key: ItemKey?): IntSet = key?.let { getOreIds(it.item, it.meta) } ?: IntSets.EMPTY_SET

    fun getOreIds(item: Item, damage: Int): IntSet {
        val result = IntOpenHashSet()
        forEachOreId(item, damage) {
            result += it
        }
        return result
    }

    fun getOreNames(stack: ItemStack): Set<String> = getOreNames(stack.item, stack.itemDamage)

    fun getOreNames(key: ItemKey?): Set<String> = key?.let { getOreNames(it.item, it.meta) } ?: emptySet()

    fun getOreNames(item: Item, damage: Int): Set<String> {
        val result = mutableSetOf<String>()
        forEachOreId(item, damage) {
            result += OreDictionary.getOreName(it)
        }
        return result
    }
}

class OreEntry(val oreName: String) : Predicate<ItemStack> {
    val oreId: Int = OreDictionary.getOreID(oreName)

    fun getOreStacks(): List<ItemStack> = OreDictHelper.getOreStacks(oreId)

    fun matches(stack: ItemStack): Boolean = matches(stack.item, stack.itemDamage)

    fun matches(key: ItemKey?): Boolean = key != null && matches(key.item, key.meta)

    fun matches(item: Item, damage: Int): Boolean = OreDictHelper.matches(oreId, item, damage)

    override fun test(t: ItemStack): Boolean = matches(t)

    override fun equals(other: Any?): Boolean = other is OreEntry && oreId == other.oreId

    override fun hashCode(): Int = oreId.hashCode()

    override fun toString(): String = oreName

    object Serializer : FullSerializer<OreEntry, NBTTagString, TJson.String> {
        override fun serializeToNbt(x: OreEntry): NBTTagString = NBTTagString(x.oreName)

        override fun deserializeFromNbt(dto: NBTTagString): OreEntry = OreEntry(dto.string)

        override fun serializeToNetwork(buf: PacketBuffer, x: OreEntry) {
            buf.writeString(x.oreName)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): OreEntry = OreEntry(buf.readString())

        override fun serializeToJson(x: OreEntry): TJson.String = TJson.String(x.oreName)

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.String): OreEntry = OreEntry(dto.value)

        object Nullable : FullSerializer<OreEntry?, NBTTagString, TJson.String> {
            override fun serializeToNbt(x: OreEntry?): NBTTagString = NBTTagString(x?.oreName ?: "")

            override fun deserializeFromNbt(dto: NBTTagString): OreEntry? =
                dto.string.takeIf { it.isNotEmpty() }?.let { OreEntry(it) }

            override fun serializeToNetwork(buf: PacketBuffer, x: OreEntry?) {
                buf.writeString(x?.oreName ?: "")
            }

            override fun deserializeFromNetwork(buf: PacketBuffer): OreEntry? =
                buf.readString().takeIf { it.isNotEmpty() }?.let { OreEntry(it) }

            override fun serializeToJson(x: OreEntry?): TJson.String = TJson.String(x?.oreName ?: "")

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.String): OreEntry? =
                dto.value.takeIf { it.isNotEmpty() }?.let { OreEntry(it) }
        }
    }
}
