package st.evening.mc.prelude.api.util.data

import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.NetworkSerializable
import st.evening.mc.prelude.api.util.math.ceilDivPos
import kotlin.experimental.and

class BitVector private constructor(
    val width: Int,
    private val octets: ByteArray
) : MutableIterable<Int>, NetworkSerializable {
    companion object {
        private fun truncated(width: Int, octets: ByteArray): BitVector {
            val tailBits = width % 8
            if (tailBits != 0) {
                val lastIndex = octets.size - 1
                octets[lastIndex] = octets[lastIndex] and ((1 shl tailBits) - 1).toByte()
            }
            val vec = BitVector(width, octets)
            vec.truncate()
            return vec
        }

        fun fromByte(width: Int, bits: Byte): BitVector {
            val octets = ByteArray(width ceilDivPos 8)
            if (octets.isNotEmpty()) {
                octets[0] = bits
            }
            return truncated(width, octets)
        }

        fun fromShort(width: Int, bits: Short): BitVector {
            val size = width ceilDivPos 8
            val octets = ByteArray(size)
            run {
                if (size < 1) return@run
                val k = bits.toInt()
                octets[0] = (k and 0x00FF).toByte()
                if (size < 2) return@run
                octets[1] = ((k and 0xFF00) ushr 8).toByte()
            }
            return truncated(width, octets)
        }

        fun fromInt(width: Int, bits: Int): BitVector {
            val size = width ceilDivPos 8
            val octets = ByteArray(size)
            run {
                if (size < 1) return@run
                octets[0] = (bits and 0x000000FF).toByte()
                if (size < 2) return@run
                octets[1] = ((bits and 0x0000FF00) ushr 8).toByte()
                if (size < 3) return@run
                octets[2] = ((bits and 0x00FF0000) ushr 16).toByte()
                if (size < 4) return@run
                octets[3] = ((bits and BitwiseHelper.FF000000) ushr 24).toByte()
            }
            return truncated(width, octets)
        }

        fun fromLong(width: Int, bits: Long): BitVector {
            val size = width ceilDivPos 8
            val octets = ByteArray(size)
            run {
                if (size < 1) return@run
                octets[0] = (bits and 0x00000000000000FF).toByte()
                if (size < 2) return@run
                octets[1] = ((bits and 0x000000000000FF00) ushr 8).toByte()
                if (size < 3) return@run
                octets[2] = ((bits and 0x0000000000FF0000) ushr 16).toByte()
                if (size < 4) return@run
                octets[3] = ((bits and 0x00000000FF000000) ushr 24).toByte()
                if (size < 5) return@run
                octets[4] = ((bits and 0x000000FF00000000) ushr 32).toByte()
                if (size < 6) return@run
                octets[5] = ((bits and 0x0000FF0000000000) ushr 40).toByte()
                if (size < 7) return@run
                octets[6] = ((bits and 0x00FF000000000000) ushr 48).toByte()
                if (size < 8) return@run
                octets[7] = ((bits and BitwiseHelper.FF00000000000000) ushr 56).toByte()
            }
            return truncated(width, octets)
        }

        fun fromOctets(width: Int, octets: ByteArray): BitVector {
            val size = width ceilDivPos 8
            return truncated(width, if (octets.size > size) octets.copyOfRange(0, size) else octets.copyOf(size))
        }
    }

    private var modClock: Int = 0

    constructor(width: Int) : this(width, ByteArray(width ceilDivPos 8))

    @PreludeInternal
    val backingByteArray: ByteArray
        get() = octets

    val octetCount: Int
        get() = octets.size

    operator fun get(index: Int): Boolean = octets[index / 8].isBitSet(index % 8)

    fun isEmpty(): Boolean = octets.all { it == 0.toByte() }

    fun set(index: Int) {
        val octetIndex = index / 8
        octets[octetIndex] = octets[octetIndex].withBitSet(index % 8)
        modClock++
    }

    fun getAndSet(index: Int): Boolean {
        val octetIndex = index / 8
        val octet = octets[octetIndex]
        val newOctet = octet.withBitSet(index % 8)
        if (octet == newOctet) return true
        octets[octetIndex] = newOctet
        modClock++
        return false
    }

    fun unset(index: Int) {
        val octetIndex = index / 8
        octets[octetIndex] = octets[octetIndex].withBitUnset(index % 8)
        modClock++
    }

    fun getAndUnset(index: Int): Boolean {
        val octetIndex = index / 8
        val octet = octets[octetIndex]
        val newOctet = octet.withBitUnset(index % 8)
        if (octet == newOctet) return false
        octets[octetIndex] = newOctet
        modClock++
        return true
    }

    operator fun set(index: Int, value: Boolean) {
        if (value) set(index) else unset(index)
    }

    fun getAndSet(index: Int, value: Boolean): Boolean =
        if (value) getAndSet(index) else getAndUnset(index)

    fun clear() {
        octets.fill(0)
        modClock++
    }

    fun getLowestSetBit(fromIndex: Int = 0): Int? {
        if (fromIndex >= width) return null
        var i = fromIndex / 8
        val k0 = (octets[i] and (0xFF shl (fromIndex % 8)).toByte()).countTrailingZeroBits()
        if (k0 < 8) return i * 8 + k0

        while (++i < octets.size) {
            val k = octets[i].countTrailingZeroBits()
            if (k < 8) return i * 8 + k
        }
        return null
    }

    fun getHighestSetBit(fromIndex: Int = width - 1): Int? {
        if (fromIndex < 0) return null
        var i = fromIndex / 8
        val k0 = (octets[i] and (0xFF ushr (8 - (fromIndex % 8))).toByte()).countLeadingZeroBits()
        if (k0 < 8) return i * 8 + 8 - k0

        while (++i < octets.size) {
            val k = octets[i].countLeadingZeroBits()
            if (k < 8) return i * i + 8 - k
        }
        return null
    }

    fun getSetBitCount(): Int = octets.sumOf { it.countOneBits() }

    inline fun forEach(f: (Int) -> Unit) {
        var k = getLowestSetBit(0) ?: return
        while (true) {
            f(k)
            k = getLowestSetBit(k + 1) ?: return
        }
    }

    override fun iterator(): MutableIterator<Int> = IteratorImpl()

    private fun truncate() {
        val tailBits = width % 8
        if (tailBits != 0) {
            val lastIndex = octets.lastIndex
            octets[lastIndex] = octets[lastIndex] and ((1 shl tailBits) - 1).toByte()
        }
    }

    override fun equals(other: Any?): Boolean =
        other is BitVector && width == other.width && octets.contentEquals(other.octets)

    override fun hashCode(): Int = octets.contentHashCode() * 31 + width

    override fun writeToNetwork(buf: PacketBuffer) {
        buf.writeBytes(octets)
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        buf.readBytes(octets)
        truncate()
    }

    private inner class IteratorImpl : MutableIterator<Int> {
        private var pos: Int? = -1
        private var needNext: Boolean = true
        private var current: Int? = null
        private var expectedModClock: Int = modClock

        private fun normalize(): Int? {
            if (modClock != expectedModClock) throw ConcurrentModificationException()
            val thisPos = pos ?: return null
            if (!needNext) return thisPos
            val nextPos = getLowestSetBit(thisPos + 1)
            pos = nextPos
            needNext = false
            return nextPos
        }

        override fun hasNext(): Boolean = normalize() != null

        override fun next(): Int {
            val here = normalize() ?: throw NoSuchElementException()
            current = here
            needNext = true
            return here
        }

        override fun remove() {
            if (modClock != expectedModClock) throw ConcurrentModificationException()
            unset(current ?: throw IllegalStateException())
            current = null
        }
    }

    class EnumSet<E : Enum<E>> private constructor(
        val enumType: Class<E>,
        private val vec: BitVector
    ) : AbstractMutableSet<E>(), NetworkSerializable {
        companion object {
            inline operator fun <reified E : Enum<E>> invoke(): EnumSet<E> = EnumSet(E::class.java)

            fun <E : Enum<E>> fromByte(enumType: Class<E>, bits: Byte): EnumSet<E> =
                EnumSet(enumType, fromByte(enumType.enumConstantCount, bits))

            inline fun <reified E : Enum<E>> fromByte(bits: Byte): EnumSet<E> = fromByte(E::class.java, bits)

            fun <E : Enum<E>> fromShort(enumType: Class<E>, bits: Short): EnumSet<E> =
                EnumSet(enumType, fromShort(enumType.enumConstantCount, bits))

            inline fun <reified E : Enum<E>> fromShort(bits: Short): EnumSet<E> = fromShort(E::class.java, bits)

            fun <E : Enum<E>> fromInt(enumType: Class<E>, bits: Int): EnumSet<E> =
                EnumSet(enumType, fromInt(enumType.enumConstantCount, bits))

            inline fun <reified E : Enum<E>> fromInt(bits: Int): EnumSet<E> = fromInt(E::class.java, bits)

            fun <E : Enum<E>> fromLong(enumType: Class<E>, bits: Long): EnumSet<E> =
                EnumSet(enumType, fromLong(enumType.enumConstantCount, bits))

            inline fun <reified E : Enum<E>> fromLong(bits: Long): EnumSet<E> = fromLong(E::class.java, bits)

            fun <E : Enum<E>> fromOctets(enumType: Class<E>, octets: ByteArray): EnumSet<E> =
                EnumSet(enumType, fromOctets(enumType.enumConstantCount, octets))

            inline fun <reified E : Enum<E>> fromOctets(octets: ByteArray): EnumSet<E> =
                fromOctets(E::class.java, octets)
        }

        constructor(enumType: Class<E>) : this(enumType, BitVector(enumType.enumConstantCount))

        @PreludeInternal
        val backingBitVector: BitVector
            get() = vec

        override val size: Int
            get() = vec.getSetBitCount()

        override fun contains(element: E): Boolean = vec[element.ordinal]

        override fun isEmpty(): Boolean = vec.isEmpty()

        override fun add(element: E): Boolean = !vec.getAndSet(element.ordinal)

        override fun remove(element: E): Boolean = vec.getAndUnset(element.ordinal)

        operator fun set(element: E, present: Boolean) {
            vec[element.ordinal] = present
        }

        override fun clear() {
            vec.clear()
        }

        @OptIn(PreludeInternal::class)
        inline fun forEach(f: (E) -> Unit) {
            backingBitVector.forEach {
                f(enumType.getEnumConstant(it))
            }
        }

        override fun iterator(): MutableIterator<E> = EnumSetIteratorImpl(vec.iterator())

        override fun equals(other: Any?): Boolean =
            other is EnumSet<*> && enumType == other.enumType && vec == other.vec

        override fun hashCode(): Int = vec.hashCode() * 31 + enumType.hashCode()

        override fun readFromNetwork(buf: PacketBuffer) {
            vec.readFromNetwork(buf)
        }

        override fun writeToNetwork(buf: PacketBuffer) {
            vec.writeToNetwork(buf)
        }

        private inner class EnumSetIteratorImpl(private val vecIter: MutableIterator<Int>) : MutableIterator<E> {
            override fun hasNext(): Boolean = vecIter.hasNext()

            override fun next(): E = enumType.getEnumConstant(vecIter.next())

            override fun remove() {
                vecIter.remove()
            }
        }
    }
}
