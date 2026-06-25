package st.evening.mc.prelude.api.data.ser

import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.util.data.BitVector
import st.evening.mc.prelude.api.util.data.enumConstantCount
import st.evening.mc.prelude.api.util.data.getEnumConstant
import st.evening.mc.prelude.api.util.data.readAndIterateBitVector
import java.util.EnumMap

class EnumMapSerializer<K : Enum<K>, V>(
    private val keyType: Class<K>,
    private val valueSerializer: NetworkSerializer<V>
) : NetworkSerializer<EnumMap<K, V>> {
    companion object {
        inline operator fun <reified K : Enum<K>, V> invoke(
            valueSerializer: NetworkSerializer<V>
        ): EnumMapSerializer<K, V> = EnumMapSerializer(K::class.java, valueSerializer)
    }

    override fun serializeToNetwork(buf: PacketBuffer, x: EnumMap<K, V>) {
        val mask = BitVector(keyType.enumConstantCount)
        x.keys.forEach { mask.set(it.ordinal) }
        mask.writeToNetwork(buf)
        x.values.forEach { valueSerializer.serializeToNetwork(buf, it) }
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): EnumMap<K, V> {
        val map = EnumMap<K, V>(keyType)
        buf.readAndIterateBitVector(keyType.enumConstantCount) { ordinal ->
            map[keyType.getEnumConstant(ordinal)] = valueSerializer.deserializeFromNetwork(buf)
        }
        return map
    }
}
