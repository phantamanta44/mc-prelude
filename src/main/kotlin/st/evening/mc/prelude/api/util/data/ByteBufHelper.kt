package st.evening.mc.prelude.api.util.data

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.prelude.api.util.game.RegistryHelper
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun ByteBuf.readableToArray(): ByteArray = ByteArray(readableBytes()).also { readBytes(it) }

@OptIn(ExperimentalContracts::class)
inline fun buildArrayFromPacketBuffer(action: (PacketBuffer) -> Unit): ByteArray {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val buf = Unpooled.buffer()
    action(PacketBuffer(buf))
    val array = buf.readableToArray()
    buf.release()
    return array
}

fun PacketBuffer.readString(): String = readString(32767)

fun PacketBuffer.writeFluidStack(stack: FluidStack?) {
    if (stack == null) {
        writeInt(-1)
        return
    }
    writeInt(stack.amount)
    writeString(FluidRegistry.getFluidName(stack.fluid))
    writeCompoundTag(stack.tag)
}

fun PacketBuffer.readFluidStack(): FluidStack? {
    val amount = readInt()
    if (amount <= 0) return null
    return FluidStack(RegistryHelper.getAssertFluid(readString()), amount, readCompoundTag())
}

inline fun PacketBuffer.readAndIterateBitVector(width: Int, f: (Int) -> Unit) {
    val vec = BitVector(width)
    vec.readFromNetwork(this)
    vec.forEach(f)
}
