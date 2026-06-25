package st.evening.mc.prelude.api.event

import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.eventhandler.Event

class BlockStateChangedEvent(
    val world: World,
    val pos: BlockPos,
    val oldState: IBlockState,
    val newState: IBlockState
) : Event()
