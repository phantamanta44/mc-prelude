package st.evening.mc.prelude.api.util.world

import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalClient
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.math.Vec2i
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun World.onClient(action: @ClientSide () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isRemote) {
        assertLogicalClient(action)
    }
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun World.onServer(action: @ServerSide () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (!isRemote) {
        assertLogicalServer(action)
    }
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun <T> World.sided(onServer: @ServerSide () -> T, onClient: @ClientSide () -> T): T {
    contract {
        callsInPlace(onServer, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onClient, InvocationKind.AT_MOST_ONCE)
    }
    return if (isRemote) {
        assertLogicalClient(onClient)
    } else {
        assertLogicalServer(onServer)
    }
}

inline fun <reified T> IBlockAccess.findTileEntity(pos: BlockPos): T? = getTileEntity(pos) as? T

// particularly useful for forge gui handlers, which receive the container coordinates as individual ints
inline fun <reified T> IBlockAccess.findTileEntity(x: Int, y: Int, z: Int): T? = findTileEntity(BlockPos(x, y, z))

@OptIn(ExperimentalContracts::class)
inline fun <reified T> IBlockAccess.useTileEntity(pos: BlockPos, action: (T) -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    findTileEntity<T>(pos)?.let(action)
}

inline fun <reified T> IBlockAccess.useTileEntity(x: Int, y: Int, z: Int, action: (T) -> Unit) {
    useTileEntity(BlockPos(x, y, z), action)
}

fun BlockPos.getChunkCoords(): Vec2i = Vec2i(x shr 4, z shr 4)
