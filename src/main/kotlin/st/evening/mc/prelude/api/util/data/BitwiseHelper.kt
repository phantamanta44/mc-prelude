@file:Suppress("NOTHING_TO_INLINE")

package st.evening.mc.prelude.api.util.data

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

inline fun Byte.isBitSet(index: Int): Boolean = this and (1 shl index).toByte() != 0.toByte()
inline fun Short.isBitSet(index: Int): Boolean = this and (1 shl index).toShort() != 0.toShort()
inline fun Int.isBitSet(index: Int): Boolean = this and (1 shl index) != 0
inline fun Long.isBitSet(index: Int): Boolean = this and (1L shl index) != 0L

inline fun Byte.withBitSet(index: Int): Byte = this or (1 shl index).toByte()
inline fun Short.withBitSet(index: Int): Short = this or (1 shl index).toShort()
inline fun Int.withBitSet(index: Int): Int = this or (1 shl index)
inline fun Long.withBitSet(index: Int): Long = this or (1 shl index).toLong()

inline fun Byte.withBitUnset(index: Int): Byte = this and (1 shl index).toByte().inv()
inline fun Short.withBitUnset(index: Int): Short = this and (1 shl index).toShort().inv()
inline fun Int.withBitUnset(index: Int): Int = this and (1 shl index).inv()
inline fun Long.withBitUnset(index: Int): Long = this and (1 shl index).toLong().inv()

object BitwiseHelper {
    const val FF000000: Int = -0x1000000
    const val FF00000000000000: Long = -0x100000000000000
}

inline fun Int.getByte1(): Int = (this ushr 24) and 0xFF
inline fun Int.getByte1AsFloat(): Float = getByte1() / 255F
inline fun Int.getByte2(): Int = (this ushr 16) and 0xFF
inline fun Int.getByte2AsFloat(): Float = getByte2() / 255F
inline fun Int.getByte3(): Int = (this ushr 8) and 0xFF
inline fun Int.getByte3AsFloat(): Float = getByte3() / 255F
inline fun Int.getByte4(): Int = this and 0xFF
inline fun Int.getByte4AsFloat(): Float = getByte4() / 255F
