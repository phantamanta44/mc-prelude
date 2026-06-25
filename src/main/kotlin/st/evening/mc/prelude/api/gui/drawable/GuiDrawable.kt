package st.evening.mc.prelude.api.gui.drawable

import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.FloatArithmetic
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.math.MathsHelper
import st.evening.mc.prelude.api.util.render.gui.DrawAxis
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation
import kotlin.math.ceil

@ClientSide.Physical
interface GuiDrawable {
    val width: Int
    val height: Int

    fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int)

    fun resize(newWidth: Int, newHeight: Int): GuiDrawable = GuiDrawableResizeWrapper(this, newWidth, newHeight)
}

@ClientSide.Physical
private class GuiDrawableResizeWrapper(
    private val parent: GuiDrawable,
    override val width: Int,
    override val height: Int
) : GuiDrawable {
    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {
        parent.draw(partialTicks, x, y, drawWidth, drawHeight)
    }

    override fun resize(newWidth: Int, newHeight: Int): GuiDrawable =
        GuiDrawableResizeWrapper(parent, newWidth, newHeight)
}

@ClientSide.Physical
fun GuiDrawable.drawFullSize(partialTicks: Float, x: Int, y: Int) {
    draw(partialTicks, x, y, width, height)
}

@ClientSide.Physical
interface GuiSamplable : GuiDrawable {
    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {
        drawPart(partialTicks, x, y, drawWidth, drawHeight, 0F, 0F, 1F, 1F)
    }

    fun drawPart(
        partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float
    )

    fun slice(sliceWidth: Int, sliceHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float): GuiSamplable =
        GuiSamplableSlice(this, sliceWidth, sliceHeight, u1, v1, u2, v2)

    override fun resize(newWidth: Int, newHeight: Int): GuiSamplable = slice(newWidth, newHeight, 0F, 0F, 1F, 1F)
}

@ClientSide.Physical
private class GuiSamplableSlice(
    private val parent: GuiSamplable,
    override val width: Int,
    override val height: Int,
    private val sliceU1: Float,
    private val sliceV1: Float,
    private val sliceU2: Float,
    private val sliceV2: Float
) : GuiSamplable {
    private fun scaleU(subU: Float): Float = MathsHelper.lerp(sliceU1, sliceU2, subU)

    private fun scaleV(subV: Float): Float = MathsHelper.lerp(sliceV1, sliceV2, subV)

    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {
        parent.drawPart(partialTicks, x, y, drawWidth, drawHeight, sliceU1, sliceV1, sliceU2, sliceV2)
    }

    override fun drawPart(
        partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        parent.drawPart(partialTicks, x, y, drawWidth, drawHeight, scaleU(u1), scaleV(v1), scaleU(u2), scaleV(v2))
    }

    override fun slice(sliceWidth: Int, sliceHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float): GuiSamplable {
        return GuiSamplableSlice(parent, sliceWidth, sliceHeight, scaleU(u1), scaleV(v1), scaleU(u2), scaleV(v2))
    }

    override fun resize(newWidth: Int, newHeight: Int): GuiSamplable =
        GuiSamplableSlice(parent, newWidth, newHeight, sliceU1, sliceV1, sliceU2, sliceV2)
}

@ClientSide.Physical
fun GuiSamplable.drawSizedPart(
    partialTicks: Float,
    x: Int,
    y: Int,
    drawWidth: Int,
    drawHeight: Int,
    u: Float,
    v: Float,
    uWidth: Float,
    vHeight: Float
) {
    val w = width
    val h = height
    drawPart(partialTicks, x, y, drawWidth, drawHeight, u / w, v / h, (u + uWidth) / w, (v + vHeight) / h)
}

@ClientSide.Physical
fun GuiSamplable.sliceSized(
    sliceWidth: Int,
    sliceHeight: Int,
    u: Float,
    v: Float,
    uWidth: Float,
    vHeight: Float
): GuiSamplable {
    val w = width
    val h = height
    return slice(sliceWidth, sliceHeight, u / w, v / h, (u + uWidth) / w, (v + vHeight) / h)
}

@ClientSide.Physical
fun GuiSamplable.sliceSized(sliceWidth: Int, sliceHeight: Int, u: Int, v: Int): GuiSamplable =
    sliceSized(sliceWidth, sliceHeight, u.toFloat(), v.toFloat(), sliceWidth.toFloat(), sliceHeight.toFloat())

@ClientSide.Physical
fun GuiSamplable.drawAsProgress(
    partialTicks: Float,
    x: Int,
    y: Int,
    barWidth: Int,
    barHeight: Int,
    orientation: DrawOrientation,
    fill: Float
) {
    if (fill <= 0F) return
    if (fill >= 1F) {
        draw(partialTicks, x, y, barWidth, barHeight)
        return
    }
    when (orientation.axis) {
        DrawAxis.X -> {
            val drawWidth = ceil(fill * (barWidth - 1)).toInt()
            val drawX = x + orientation.offsetRef.computeOffset(IntArithmetic, 0, drawWidth, barWidth)
            val uWidth = drawWidth / barWidth.toFloat()
            val u = orientation.offsetRef.computeOffset(FloatArithmetic, 0F, uWidth, 1F)
            drawPart(partialTicks, drawX, y, drawWidth, barHeight, u, 0F, u + uWidth, 1F)
        }

        DrawAxis.Y -> {
            val drawHeight = ceil(fill * (barHeight - 1)).toInt()
            val drawY = y + orientation.offsetRef.computeOffset(IntArithmetic, 0, drawHeight, barHeight)
            val vHeight = drawHeight / barHeight.toFloat()
            val v = orientation.offsetRef.computeOffset(FloatArithmetic, 0F, vHeight, 1F)
            drawPart(partialTicks, x, drawY, barWidth, drawHeight, 0F, v, 1F, v + vHeight)
        }
    }
}

@ClientSide.Physical
fun GuiSamplable.drawFullSizeAsProgress(
    partialTicks: Float,
    x: Int,
    y: Int,
    orientation: DrawOrientation,
    fill: Float
) {
    drawAsProgress(partialTicks, x, y, width, height, orientation, fill)
}

@ClientSide.Physical
fun GuiSamplable.drawTiledRow(partialTicks: Float, rowX: Int, rowY: Int, rowWidth: Int) {
    val tileWidth = width
    val tileHeight = height
    var remWidth = rowWidth
    var x = rowX
    while (remWidth >= tileWidth) {
        draw(partialTicks, x, rowY, tileWidth, tileHeight)
        x += tileWidth
        remWidth -= tileWidth
    }
    if (remWidth > 0) {
        drawPart(partialTicks, x, rowY, remWidth, tileHeight, 0F, 0F, remWidth / tileWidth.toFloat(), 1F)
    }
}

@ClientSide.Physical
fun GuiSamplable.drawTiledColumn(partialTicks: Float, colX: Int, colY: Int, colHeight: Int) {
    val tileWidth = width
    val tileHeight = height
    var remHeight = colHeight
    var y = colY
    while (remHeight >= tileHeight) {
        draw(partialTicks, colX, y, tileWidth, tileHeight)
        y += tileHeight
        remHeight -= tileHeight
    }
    if (remHeight > 0) {
        drawPart(partialTicks, colX, y, tileWidth, remHeight, 0F, 0F, 1F, remHeight / tileHeight.toFloat())
    }
}

@ClientSide.Physical
fun GuiSamplable.drawTiled(partialTicks: Float, gridX: Int, gridY: Int, gridWidth: Int, gridHeight: Int) {
    val tileWidth = width
    val fullColCount = gridWidth / tileWidth
    val overflowWidth = gridWidth % tileWidth
    val overflowUWidth = overflowWidth / tileWidth.toFloat()

    val tileHeight = height
    val fullRowCount = gridHeight / tileHeight
    val overflowHeight = gridHeight % tileHeight
    val overflowVHeight = overflowHeight / tileHeight.toFloat()

    var y = gridY
    repeat(fullRowCount) {
        var x = gridX
        repeat(fullColCount) {
            draw(partialTicks, x, y, tileWidth, tileHeight)
            x += tileWidth
        }
        if (overflowWidth > 0) {
            drawPart(partialTicks, x, y, overflowWidth, tileHeight, 0F, 0F, overflowUWidth, 1F)
        }
        y += tileHeight
    }
    if (overflowHeight > 0) {
        var x = gridX
        repeat(fullColCount) {
            drawPart(partialTicks, x, y, tileWidth, overflowHeight, 0F, 0F, 1F, overflowVHeight)
            x += tileWidth
        }
        if (overflowWidth > 0) {
            drawPart(partialTicks, x, y, overflowWidth, overflowHeight, 0F, 0F, overflowUWidth, overflowVHeight)
        }
    }
}

@ClientSide.Physical
interface UntexturedDrawable : GuiSamplable {
    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int)

    override fun drawPart(
        partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        draw(partialTicks, x, y, drawWidth, drawHeight)
    }

    override fun resize(newWidth: Int, newHeight: Int): GuiSamplable

    override fun slice(sliceWidth: Int, sliceHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float): GuiSamplable =
        resize(sliceWidth, sliceHeight)
}
