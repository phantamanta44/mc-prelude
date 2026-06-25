package st.evening.mc.prelude.api.gui.drawable.prefab

import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.drawable.drawTiled
import st.evening.mc.prelude.api.gui.drawable.drawTiledColumn
import st.evening.mc.prelude.api.gui.drawable.drawTiledRow
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
class Drawable9Tile private constructor(
    override val width: Int,
    override val height: Int,
    private val topLeft: GuiSamplable,
    private val top: GuiSamplable,
    private val topRight: GuiSamplable,
    private val left: GuiSamplable,
    private val center: GuiSamplable,
    private val right: GuiSamplable,
    private val bottomLeft: GuiSamplable,
    private val bottom: GuiSamplable,
    private val bottomRight: GuiSamplable
) : GuiDrawable {
    companion object {
        fun fromSlices(
            width: Int,
            height: Int,
            texture: GuiSamplable,
            leftCut: Int,
            rightCut: Int,
            topCut: Int,
            bottomCut: Int
        ): Drawable9Tile {
            val texWidth = texture.width
            val centerWidth = texWidth - leftCut - rightCut
            val texWidthF = texWidth.toFloat()
            val u2 = leftCut / texWidthF
            val u3 = 1F - rightCut / texWidthF
            val texHeight = texture.height
            val centerHeight = texHeight - topCut - bottomCut
            val texHeightF = texHeight.toFloat()
            val v2 = topCut / texHeightF
            val v3 = 1F - bottomCut / texHeightF
            return Drawable9Tile(
                width,
                height,
                texture.slice(leftCut, topCut, 0F, 0F, u2, v2),
                texture.slice(centerWidth, topCut, u2, 0F, u3, v2),
                texture.slice(rightCut, topCut, u3, 0F, 1F, v2),
                texture.slice(leftCut, centerHeight, 0F, v2, u2, v3),
                texture.slice(centerWidth, centerHeight, u2, v2, u3, v3),
                texture.slice(rightCut, centerHeight, u3, v2, 1F, v3),
                texture.slice(leftCut, bottomCut, 0F, v3, u2, 1F),
                texture.slice(centerWidth, bottomCut, u2, v3, u3, 1F),
                texture.slice(rightCut, bottomCut, u3, v3, 1F, 1F)
            )
        }

        fun fromSlices(
            texture: GuiSamplable,
            leftCut: Int,
            rightCut: Int,
            topCut: Int,
            bottomCut: Int
        ): Drawable9Tile = fromSlices(texture.width, texture.height, texture, leftCut, rightCut, topCut, bottomCut)
    }

    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {
        val centerX = x + left.width
        val rightX = x + drawWidth - right.width
        val centerWidth = rightX - centerX
        val centerY = y + top.height
        val bottomY = y + drawHeight - bottom.height
        val centerHeight = bottomY - centerY
        topLeft.drawFullSize(partialTicks, x, y)
        top.drawTiledRow(partialTicks, centerX, y, centerWidth)
        topRight.drawFullSize(partialTicks, rightX, y)
        left.drawTiledColumn(partialTicks, x, centerY, centerHeight)
        center.drawTiled(partialTicks, centerX, centerY, centerWidth, centerHeight)
        right.drawTiledColumn(partialTicks, rightX, centerY, centerHeight)
        bottomLeft.drawFullSize(partialTicks, x, bottomY)
        bottom.drawTiledRow(partialTicks, centerX, bottomY, centerWidth)
        bottomRight.drawFullSize(partialTicks, rightX, bottomY)
    }

    override fun resize(newWidth: Int, newHeight: Int): GuiDrawable =
        Drawable9Tile(newWidth, newHeight, topLeft, top, topRight, left, center, right, bottomLeft, bottom, bottomRight)
}
