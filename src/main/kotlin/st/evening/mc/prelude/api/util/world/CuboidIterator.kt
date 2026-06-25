package st.evening.mc.prelude.api.util.world

import net.minecraft.util.math.BlockPos

class CuboidIterator(fromPos: BlockPos, toPos: BlockPos) : Iterator<BlockPos> {
    private val xi: Int
    private val yi: Int
    private val xf: Int
    private val yf: Int
    private val zf: Int
    private var x: Int
    private var y: Int
    private var z: Int

    init {
        if (fromPos.x <= toPos.x) {
            xi = fromPos.x
            xf = toPos.x
        } else {
            xi = toPos.x
            xf = fromPos.x
        }
        if (fromPos.y <= toPos.y) {
            yi = fromPos.y
            yf = toPos.y
        } else {
            yi = toPos.y
            yf = fromPos.y
        }
        if (fromPos.z <= toPos.z) {
            z = fromPos.z
            zf = toPos.z
        } else {
            z = toPos.z
            zf = fromPos.z
        }
        x = xi
        y = yi
    }

    override fun hasNext(): Boolean = z <= zf

    override fun next(): BlockPos = BlockPos(x, y, z).also {
        if (x < xf) {
            x++
        } else {
            x = xi
            if (y < yf) {
                y++
            } else {
                y = yi
                z++
            }
        }
    }
}
