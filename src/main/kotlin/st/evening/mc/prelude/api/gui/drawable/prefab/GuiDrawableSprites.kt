package st.evening.mc.prelude.api.gui.drawable.prefab

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.util.data.getByte1AsFloat
import st.evening.mc.prelude.api.util.data.getByte2AsFloat
import st.evening.mc.prelude.api.util.data.getByte3AsFloat
import st.evening.mc.prelude.api.util.data.getByte4AsFloat
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.MathsHelper
import st.evening.mc.prelude.api.util.math.Vec4f
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.TextureResource
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

@ClientSide.Physical
class DrawableTexture(
    override val width: Int,
    override val height: Int,
    val texture: TextureResource
) : GuiSamplable {
    override fun drawPart(
        partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        texture.bind()
        GuiRenderHelper.drawTexturedSizedQuad(x, y, drawWidth, drawHeight, u1, v1, u2, v2)
    }
}

@ClientSide.Physical
class DrawableAtlasSprite(
    val atlasTexture: TextureResource,
    val sprite: TextureAtlasSprite,
    val tint: Vec4f? = null
) : GuiSamplable {
    override val width: Int
        get() = sprite.iconWidth
    override val height: Int
        get() = sprite.iconHeight

    constructor(
        atlasTexture: TextureResource,
        sprite: TextureAtlasSprite,
        tintRed: Float,
        tintGreen: Float,
        tintBlue: Float,
        tintAlpha: Float = 1F
    ) : this(atlasTexture, sprite, Vec4f(tintRed, tintGreen, tintBlue, tintAlpha))

    constructor(atlasTexture: TextureResource, sprite: TextureAtlasSprite, tint: Int?) : this(
        atlasTexture,
        sprite,
        tint?.let { Vec4f(it.getByte2AsFloat(), it.getByte3AsFloat(), it.getByte4AsFloat(), it.getByte1AsFloat()) }
    )

    override fun draw(partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int) {
        atlasTexture.bind()
        val tint = this.tint
        if (tint != null) {
            GlStateManager.color(tint.x, tint.y, tint.z, tint.w)
            GuiRenderHelper.drawTexturedSizedQuad(
                x, y, drawWidth, drawHeight, sprite.minU, sprite.minV, sprite.maxU, sprite.maxV
            )
            RenderingHelper.resetColour()
        } else {
            GuiRenderHelper.drawTexturedSizedQuad(
                x, y, drawWidth, drawHeight, sprite.minU, sprite.minV, sprite.maxU, sprite.maxV
            )
        }
    }

    override fun drawPart(
        partialTicks: Float, x: Int, y: Int, drawWidth: Int, drawHeight: Int, u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        atlasTexture.bind()
        val su1 = sprite.minU
        val sv1 = sprite.minV
        val su2 = sprite.maxU
        val sv2 = sprite.maxV
        GuiRenderHelper.drawTexturedSizedQuad(
            x, y, drawWidth, drawHeight,
            MathsHelper.lerp(su1, su2, u1), MathsHelper.lerp(sv1, sv2, v1),
            MathsHelper.lerp(su1, su2, u2), MathsHelper.lerp(sv1, sv2, v2)
        )
    }
}

@ClientSide.Physical
fun TextureAtlasSprite.asDrawable(
    atlasTexture: TextureResource = TextureResource.ITEM_BLOCK_ATLAS,
    tint: Int? = null
): DrawableAtlasSprite = DrawableAtlasSprite(atlasTexture, this, tint)

@ClientSide.Physical
fun TextureAtlasSprite.asDrawable(
    atlasTexture: TextureResource = TextureResource.ITEM_BLOCK_ATLAS,
    tintRed: Float,
    tintGreen: Float,
    tintBlue: Float,
    tintAlpha: Float = 1F
): DrawableAtlasSprite = DrawableAtlasSprite(atlasTexture, this, tintRed, tintGreen, tintBlue, tintAlpha)
