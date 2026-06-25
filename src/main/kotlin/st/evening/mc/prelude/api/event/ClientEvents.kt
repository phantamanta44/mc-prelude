package st.evening.mc.prelude.api.event

import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.eventhandler.Event

class ClientClickBlockEvent(val pos: BlockPos, val face: EnumFacing) : Event() {
    var clickResult: EnumActionResult = EnumActionResult.PASS
}
