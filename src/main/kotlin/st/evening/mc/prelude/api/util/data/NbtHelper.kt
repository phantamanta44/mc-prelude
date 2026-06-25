package st.evening.mc.prelude.api.util.data

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagByteArray
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagFloat
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagLongArray
import net.minecraft.nbt.NBTTagShort
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants
import st.evening.mc.prelude.api.util.collection.collectToByteArray
import st.evening.mc.prelude.api.util.collection.collectToIntArray
import st.evening.mc.prelude.api.util.collection.collectToLongArray
import st.evening.mc.prelude.coremod.mixin.NBTTagCompoundAccessor
import java.util.UUID

fun NBTBase.isEnd(): Boolean = id.toInt() == Constants.NBT.TAG_END
fun NBTBase.isByte(): Boolean = id.toInt() == Constants.NBT.TAG_BYTE
fun NBTBase.isShort(): Boolean = id.toInt() == Constants.NBT.TAG_SHORT
fun NBTBase.isInt(): Boolean = id.toInt() == Constants.NBT.TAG_INT
fun NBTBase.isLong(): Boolean = id.toInt() == Constants.NBT.TAG_LONG
fun NBTBase.isFloat(): Boolean = id.toInt() == Constants.NBT.TAG_FLOAT
fun NBTBase.isDouble(): Boolean = id.toInt() == Constants.NBT.TAG_DOUBLE
fun NBTBase.isByteArray(): Boolean = id.toInt() == Constants.NBT.TAG_BYTE_ARRAY
fun NBTBase.isString(): Boolean = id.toInt() == Constants.NBT.TAG_STRING
fun NBTBase.isList(): Boolean = id.toInt() == Constants.NBT.TAG_LIST
fun NBTBase.isCompound(): Boolean = id.toInt() == Constants.NBT.TAG_COMPOUND
fun NBTBase.isIntArray(): Boolean = id.toInt() == Constants.NBT.TAG_INT_ARRAY
fun NBTBase.isLongArray(): Boolean = id.toInt() == Constants.NBT.TAG_LONG_ARRAY

@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class NbtDsl {
    companion object {
        inline fun compound(action: NbtCompoundAction): NBTTagCompound = NBTTagCompound().runAction(action)

        fun listOf(): NBTTagList = NBTTagList()
        fun listOf(tag: NBTBase): NBTTagList = NBTTagList().apply { appendTag(tag) }
        fun <T : NBTBase> listOf(vararg tags: T): NBTTagList = tags.toNbtList()
        fun listOf(value: Int): NBTTagList = NBTTagList().apply { appendTag(NBTTagInt(value)) }
        fun listOf(vararg values: Int): NBTTagList = values.toNbtIntList()
        fun listOf(value: Float): NBTTagList = NBTTagList().apply { appendTag(NBTTagFloat(value)) }
        fun listOf(vararg values: Float): NBTTagList = values.toNbtFloatList()
        fun listOf(value: Double): NBTTagList = NBTTagList().apply { appendTag(NBTTagDouble(value)) }
        fun listOf(vararg values: Double): NBTTagList = values.toNbtDoubleList()
        fun listOf(value: String): NBTTagList = NBTTagList().apply { appendTag(NBTTagString(value)) }
        fun listOf(vararg values: String): NBTTagList = values.toNbtStringList()
    }
}

@JvmInline
@NbtDsl
value class NbtCompoundDsl(private val tag: NBTTagCompound) {
    infix fun String.byte(value: Byte): Unit = tag.setByte(this, value)
    infix fun String.short(value: Short): Unit = tag.setShort(this, value)
    infix fun String.int(value: Int): Unit = tag.setInteger(this, value)
    infix fun String.long(value: Long): Unit = tag.setLong(this, value)
    infix fun String.uuid(value: UUID): Unit = tag.setUniqueId(this, value)
    infix fun String.float(value: Float): Unit = tag.setFloat(this, value)
    infix fun String.double(value: Double): Unit = tag.setDouble(this, value)
    infix fun String.string(value: String): Unit = tag.setString(this, value)
    infix fun String.bool(value: Boolean): Unit = tag.setBoolean(this, value)
    infix fun String.byteArray(value: ByteArray): Unit = tag.setByteArray(this, value)
    infix fun String.intArray(value: IntArray): Unit = tag.setIntArray(this, value)
    infix fun String.longArray(value: LongArray): Unit = tag.setLongArray(this, value)
    infix fun String.tag(value: NBTBase): Unit = tag.setTag(this, value)
    inline infix fun String.compound(action: NbtCompoundAction): Unit = this tag NbtDsl.compound(action)

    infix fun <T : NBTBase> String.list(value: Iterable<T>): Unit = tag.setTag(this, value.toNbtList())
    infix fun <T : NBTBase> String.list(value: Array<T>): Unit = tag.setTag(this, value.toNbtList())
    infix fun String.intList(value: Iterable<Int>): Unit = tag.setTag(this, value.toNbtIntList())
    infix fun String.intList(value: IntArray): Unit = tag.setTag(this, value.toNbtIntList())
    infix fun String.floatList(value: Iterable<Float>): Unit = tag.setTag(this, value.toNbtFloatList())
    infix fun String.floatList(value: FloatArray): Unit = tag.setTag(this, value.toNbtFloatList())
    infix fun String.doubleList(value: Iterable<Double>): Unit = tag.setTag(this, value.toNbtDoubleList())
    infix fun String.doubleList(value: DoubleArray): Unit = tag.setTag(this, value.toNbtDoubleList())
    infix fun String.stringList(value: Iterable<String>): Unit = tag.setTag(this, value.toNbtStringList())
    infix fun String.stringList(value: Array<String>): Unit = tag.setTag(this, value.toNbtStringList())
    infix fun String.intArrayList(value: Iterable<IntArray>): Unit = tag.setTag(this, value.toNbtIntArrayList())
    infix fun String.intArrayList(value: Array<IntArray>): Unit = tag.setTag(this, value.toNbtIntArrayList())
    infix fun String.longArrayList(value: Iterable<LongArray>): Unit = tag.setTag(this, value.toNbtLongArrayList())
    infix fun String.longArrayList(value: Array<LongArray>): Unit = tag.setTag(this, value.toNbtLongArrayList())
}

typealias NbtCompoundAction = NbtCompoundDsl.() -> Unit

inline fun NBTTagCompound.runAction(action: NbtCompoundAction): NBTTagCompound = also { NbtCompoundDsl(it).action() }

operator fun NBTTagCompound.get(key: String): NBTBase? = getTag(key)
operator fun NBTTagCompound.contains(key: String): Boolean = hasKey(key)

fun NBTTagCompound.hasByte(key: String): Boolean = hasKey(key, Constants.NBT.TAG_BYTE)
fun NBTTagCompound.hasShort(key: String): Boolean = hasKey(key, Constants.NBT.TAG_SHORT)
fun NBTTagCompound.hasInt(key: String): Boolean = hasKey(key, Constants.NBT.TAG_INT)
fun NBTTagCompound.hasLong(key: String): Boolean = hasKey(key, Constants.NBT.TAG_LONG)
fun NBTTagCompound.hasFloat(key: String): Boolean = hasKey(key, Constants.NBT.TAG_FLOAT)
fun NBTTagCompound.hasDouble(key: String): Boolean = hasKey(key, Constants.NBT.TAG_DOUBLE)
fun NBTTagCompound.hasByteArray(key: String): Boolean = hasKey(key, Constants.NBT.TAG_BYTE_ARRAY)
fun NBTTagCompound.hasString(key: String): Boolean = hasKey(key, Constants.NBT.TAG_STRING)
fun NBTTagCompound.hasList(key: String): Boolean = hasKey(key, Constants.NBT.TAG_LIST)
fun NBTTagCompound.hasCompound(key: String): Boolean = hasKey(key, Constants.NBT.TAG_COMPOUND)
fun NBTTagCompound.hasIntArray(key: String): Boolean = hasKey(key, Constants.NBT.TAG_INT_ARRAY)
fun NBTTagCompound.hasLongArray(key: String): Boolean = hasKey(key, Constants.NBT.TAG_LONG_ARRAY)

fun NBTTagCompound.setLongArray(key: String, value: LongArray) {
    setTag(key, NBTTagLongArray(value))
}

@Suppress("UNCHECKED_CAST")
private fun <T : NBTBase> NBTTagCompound.getOrNull(key: String, typeId: Int): T? =
    if (hasKey(key, typeId)) getTag(key) as T else null

fun NBTTagCompound.getByteOrNull(key: String): Byte? = getOrNull<NBTTagByte>(key, Constants.NBT.TAG_BYTE)?.byte
fun NBTTagCompound.getShortOrNull(key: String): Short? = getOrNull<NBTTagShort>(key, Constants.NBT.TAG_SHORT)?.short
fun NBTTagCompound.getIntOrNull(key: String): Int? = getOrNull<NBTTagInt>(key, Constants.NBT.TAG_INT)?.int
fun NBTTagCompound.getLongOrNull(key: String): Long? = getOrNull<NBTTagLong>(key, Constants.NBT.TAG_LONG)?.long
fun NBTTagCompound.getFloatOrNull(key: String): Float? = getOrNull<NBTTagFloat>(key, Constants.NBT.TAG_FLOAT)?.float
fun NBTTagCompound.getDoubleOrNull(key: String): Double? =
    getOrNull<NBTTagDouble>(key, Constants.NBT.TAG_DOUBLE)?.double

fun NBTTagCompound.getStringOrNull(key: String): String? =
    getOrNull<NBTTagString>(key, Constants.NBT.TAG_STRING)?.string

fun NBTTagCompound.getBoolOrNull(key: String): Boolean? =
    getOrNull<NBTTagByte>(key, Constants.NBT.TAG_BYTE)?.let { it.byte != 0.toByte() }

fun NBTTagCompound.getByteArrayOrNull(key: String): ByteArray? =
    getOrNull<NBTTagByteArray>(key, Constants.NBT.TAG_BYTE_ARRAY)?.byteArray

fun NBTTagCompound.getIntArrayOrNull(key: String): IntArray? =
    getOrNull<NBTTagIntArray>(key, Constants.NBT.TAG_INT_ARRAY)?.intArray

fun NBTTagCompound.getLongArrayOrNull(key: String): LongArray? =
    getOrNull<NBTTagLongArray>(key, Constants.NBT.TAG_LONG_ARRAY)?.data

fun NBTTagCompound.getCompoundOrNull(key: String): NBTTagCompound? = getOrNull(key, Constants.NBT.TAG_COMPOUND)
fun NBTTagCompound.getListOrNull(key: String): NBTTagList? = getOrNull(key, Constants.NBT.TAG_LIST)

fun NBTTagCompound.getOrCreateCompound(key: String): NBTTagCompound =
    getCompoundOrNull(key) ?: NBTTagCompound().also { setTag(key, it) }

fun NBTTagCompound.getOrCreateList(key: String): NBTTagList =
    getListOrNull(key) ?: NBTTagList().also { setTag(key, it) }

inline fun NBTTagCompound.forEach(f: (Map.Entry<String, NBTBase>) -> Unit) {
    (this as NBTTagCompoundAccessor).`prelude$tagMap`().forEach(f)
}

inline fun NBTTagCompound.forEachInt(f: (String, Int) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isInt()) {
            f(key, (tag as NBTTagInt).int)
        }
    }
}

inline fun NBTTagCompound.forEachFloat(f: (String, Float) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isFloat()) {
            f(key, (tag as NBTTagFloat).float)
        }
    }
}

inline fun NBTTagCompound.forEachDouble(f: (String, Double) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isDouble()) {
            f(key, (tag as NBTTagDouble).double)
        }
    }
}

inline fun NBTTagCompound.forEachString(f: (String, String) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isString()) {
            f(key, (tag as NBTTagString).string)
        }
    }
}

inline fun NBTTagCompound.forEachIntArray(f: (String, IntArray) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isIntArray()) {
            f(key, (tag as NBTTagIntArray).intArray)
        }
    }
}

inline fun NBTTagCompound.forEachLongArray(f: (String, LongArray) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isLongArray()) {
            f(key, (tag as NBTTagLongArray).data)
        }
    }
}

inline fun NBTTagCompound.forEachCompound(f: (String, NBTTagCompound) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isCompound()) {
            f(key, tag as NBTTagCompound)
        }
    }
}

inline fun NBTTagCompound.forEachList(f: (String, NBTTagList) -> Unit) {
    forEach { (key, tag) ->
        if (tag.isList()) {
            f(key, tag as NBTTagList)
        }
    }
}

operator fun NBTTagCompound.set(key: String, tag: NBTBase): Unit = setTag(key, tag)
operator fun NBTTagCompound.set(key: String, value: Byte): Unit = setByte(key, value)
operator fun NBTTagCompound.set(key: String, value: Short): Unit = setShort(key, value)
operator fun NBTTagCompound.set(key: String, value: Int): Unit = setInteger(key, value)
operator fun NBTTagCompound.set(key: String, value: Long): Unit = setLong(key, value)
operator fun NBTTagCompound.set(key: String, value: UUID): Unit = setUniqueId(key, value)
operator fun NBTTagCompound.set(key: String, value: Float): Unit = setFloat(key, value)
operator fun NBTTagCompound.set(key: String, value: Double): Unit = setDouble(key, value)
operator fun NBTTagCompound.set(key: String, value: String): Unit = setString(key, value)
operator fun NBTTagCompound.set(key: String, value: Boolean): Unit = setBoolean(key, value)
operator fun NBTTagCompound.set(key: String, value: ByteArray): Unit = setByteArray(key, value)
operator fun NBTTagCompound.set(key: String, value: IntArray): Unit = setIntArray(key, value)
operator fun NBTTagCompound.set(key: String, value: LongArray): Unit = setLongArray(key, value)

fun NBTTagCompound.shallowMerge(o: NBTTagCompound) {
    (o as NBTTagCompoundAccessor).`prelude$tagMap`().forEach { (key, value) ->
        if (value.id == Constants.NBT.TAG_COMPOUND.toByte()) {
            getCompoundOrNull(key)?.let {
                it.shallowMerge(value as NBTTagCompound)
                return@forEach
            }
        }
        setTag(key, value)
    }
}

fun <T : NBTBase> Iterable<T>.toNbtList(): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(it) }
}

fun <T : NBTBase> Array<out T>.toNbtList(): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(it) }
}

inline fun <A, T : NBTBase> Iterable<A>.mapToNbtList(f: (A) -> T): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(f(it)) }
}

inline fun <A, T : NBTBase> Array<out A>.mapToNbtList(f: (A) -> T): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(f(it)) }
}

fun Iterable<Byte>.toNbtByteArray(): NBTTagByteArray = NBTTagByteArray(collectToByteArray())
fun ByteArray.toNbtByteArray(): NBTTagByteArray = NBTTagByteArray(this)

fun Iterable<Int>.toNbtIntArray(): NBTTagIntArray = NBTTagIntArray(collectToIntArray())
fun IntArray.toNbtIntArray(): NBTTagIntArray = NBTTagIntArray(this)

fun Iterable<Long>.toNbtLongArray(): NBTTagLongArray = NBTTagLongArray(collectToLongArray())
fun LongArray.toNbtLongArray(): NBTTagLongArray = NBTTagLongArray(this)

fun Iterable<Int>.toNbtIntList(): NBTTagList = mapToNbtList { NBTTagInt(it) }
fun IntArray.toNbtIntList(): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(NBTTagInt(it)) }
}

fun Iterable<Long>.toNbtLongList(): NBTTagList = mapToNbtList { NBTTagLong(it) }
fun LongArray.toNbtLongList(): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(NBTTagLong(it)) }
}

fun Iterable<Float>.toNbtFloatList(): NBTTagList = mapToNbtList { NBTTagFloat(it) }
fun FloatArray.toNbtFloatList(): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(NBTTagFloat(it)) }
}

fun Iterable<Double>.toNbtDoubleList(): NBTTagList = mapToNbtList { NBTTagDouble(it) }
fun DoubleArray.toNbtDoubleList(): NBTTagList = NBTTagList().also { list ->
    forEach { list.appendTag(NBTTagDouble(it)) }
}

fun Iterable<String>.toNbtStringList(): NBTTagList = mapToNbtList { NBTTagString(it) }
fun Array<out String>.toNbtStringList(): NBTTagList = mapToNbtList { NBTTagString(it) }
fun Iterable<IntArray>.toNbtIntArrayList(): NBTTagList = mapToNbtList { NBTTagIntArray(it) }
fun Array<out IntArray>.toNbtIntArrayList(): NBTTagList = mapToNbtList { NBTTagIntArray(it) }
fun Iterable<LongArray>.toNbtLongArrayList(): NBTTagList = mapToNbtList { NBTTagLongArray(it) }
fun Array<out LongArray>.toNbtLongArrayList(): NBTTagList = mapToNbtList { NBTTagLongArray(it) }

fun NBTTagList.appendByte(value: Byte): Unit = appendTag(NBTTagByte(value))
fun NBTTagList.appendShort(value: Short): Unit = appendTag(NBTTagShort(value))
fun NBTTagList.appendInt(value: Int): Unit = appendTag(NBTTagInt(value))
fun NBTTagList.appendLong(value: Long): Unit = appendTag(NBTTagLong(value))
fun NBTTagList.appendFloat(value: Float): Unit = appendTag(NBTTagFloat(value))
fun NBTTagList.appendDouble(value: Double): Unit = appendTag(NBTTagDouble(value))
fun NBTTagList.appendString(value: String): Unit = appendTag(NBTTagString(value))
fun NBTTagList.appendByteArray(value: ByteArray): Unit = appendTag(NBTTagByteArray(value))
fun NBTTagList.appendIntArray(value: IntArray): Unit = appendTag(NBTTagIntArray(value))
fun NBTTagList.appendLongArray(value: LongArray): Unit = appendTag(NBTTagLongArray(value))

operator fun NBTTagList.plusAssign(tag: NBTBase): Unit = appendTag(tag)
operator fun NBTTagList.plusAssign(value: Byte): Unit = appendByte(value)
operator fun NBTTagList.plusAssign(value: Short): Unit = appendShort(value)
operator fun NBTTagList.plusAssign(value: Int): Unit = appendInt(value)
operator fun NBTTagList.plusAssign(value: Long): Unit = appendLong(value)
operator fun NBTTagList.plusAssign(value: Float): Unit = appendFloat(value)
operator fun NBTTagList.plusAssign(value: Double): Unit = appendDouble(value)
operator fun NBTTagList.plusAssign(value: String): Unit = appendString(value)
operator fun NBTTagList.plusAssign(value: ByteArray): Unit = appendByteArray(value)
operator fun NBTTagList.plusAssign(value: IntArray): Unit = appendIntArray(value)
operator fun NBTTagList.plusAssign(value: LongArray): Unit = appendLongArray(value)

inline fun NBTTagList.forEachByte(f: (Byte) -> Unit) {
    forEach { tag ->
        if (tag.isByte()) {
            f((tag as NBTTagByte).byte)
        }
    }
}

inline fun NBTTagList.forEachShort(f: (Short) -> Unit) {
    forEach { tag ->
        if (tag.isShort()) {
            f((tag as NBTTagShort).short)
        }
    }
}

inline fun NBTTagList.forEachInt(f: (Int) -> Unit) {
    forEach { tag ->
        if (tag.isInt()) {
            f((tag as NBTTagInt).int)
        }
    }
}

inline fun NBTTagList.forEachLong(f: (Long) -> Unit) {
    forEach { tag ->
        if (tag.isLong()) {
            f((tag as NBTTagLong).long)
        }
    }
}


inline fun NBTTagList.forEachFloat(f: (Float) -> Unit) {
    forEach { tag ->
        if (tag.isFloat()) {
            f((tag as NBTTagFloat).float)
        }
    }
}

inline fun NBTTagList.forEachDouble(f: (Double) -> Unit) {
    forEach { tag ->
        if (tag.isDouble()) {
            f((tag as NBTTagDouble).double)
        }
    }
}

inline fun NBTTagList.forEachString(f: (String) -> Unit) {
    forEach { tag ->
        if (tag.isString()) {
            f((tag as NBTTagString).string)
        }
    }
}

inline fun NBTTagList.forEachByteArray(f: (ByteArray) -> Unit) {
    forEach { tag ->
        if (tag.isByteArray()) {
            f((tag as NBTTagByteArray).byteArray)
        }
    }
}

inline fun NBTTagList.forEachIntArray(f: (IntArray) -> Unit) {
    forEach { tag ->
        if (tag.isIntArray()) {
            f((tag as NBTTagIntArray).intArray)
        }
    }
}

inline fun NBTTagList.forEachLongArray(f: (LongArray) -> Unit) {
    forEach { tag ->
        if (tag.isLongArray()) {
            f((tag as NBTTagLongArray).data)
        }
    }
}

inline fun NBTTagList.forEachCompound(f: (NBTTagCompound) -> Unit) {
    forEach { tag ->
        if (tag.isCompound()) {
            f(tag as NBTTagCompound)
        }
    }
}

inline fun NBTTagList.forEachList(f: (NBTTagList) -> Unit) {
    forEach { tag ->
        if (tag.isList()) {
            f(tag as NBTTagList)
        }
    }
}
