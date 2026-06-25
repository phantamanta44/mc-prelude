package st.evening.mc.prelude.api.util.render.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.tessellate


@ClientSide.Physical
object GuiRenderHelper {
    const val DEFAULT_TEXT_COLOUR: Int = 0x404040

    fun drawTexturedQuad(
        x1: Double, y1: Double, x2: Double, y2: Double, u1: Double, v1: Double, u2: Double, v2: Double
    ) {
        Tessellator.getInstance().tessellate(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX) {
            pos(x1, y2, 0.0).tex(u1, v2).endVertex()
            pos(x2, y2, 0.0).tex(u2, v2).endVertex()
            pos(x2, y1, 0.0).tex(u2, v1).endVertex()
            pos(x1, y1, 0.0).tex(u1, v1).endVertex()
        }
    }

    fun drawTexturedQuad(
        x1: Int, y1: Int, x2: Int, y2: Int, u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        drawTexturedQuad(
            x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(),
            u1.toDouble(), v1.toDouble(), u2.toDouble(), v2.toDouble()
        )
    }

    fun drawTexturedSizedQuad(
        x: Double, y: Double, width: Double, height: Double, u1: Double, v1: Double, u2: Double, v2: Double
    ) {
        drawTexturedQuad(x, y, x + width, y + height, u1, v1, u2, v2)
    }

    fun drawTexturedSizedQuad(
        x: Int, y: Int, width: Int, height: Int, u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        drawTexturedSizedQuad(
            x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(),
            u1.toDouble(), v1.toDouble(), u2.toDouble(), v2.toDouble()
        )
    }

    fun drawUntexturedQuad(x1: Double, y1: Double, x2: Double, y2: Double) {
        Tessellator.getInstance().tessellate(GL11.GL_QUADS, DefaultVertexFormats.POSITION) {
            pos(x1, y2, 0.0).endVertex()
            pos(x2, y2, 0.0).endVertex()
            pos(x2, y1, 0.0).endVertex()
            pos(x1, y1, 0.0).endVertex()
        }
    }

    fun drawUntexturedQuad(x1: Int, y1: Int, x2: Int, y2: Int) {
        drawUntexturedQuad(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
    }

    fun drawUntexturedSizedQuad(x: Double, y: Double, width: Double, height: Double) {
        drawUntexturedQuad(x, y, x + width, y + height)
    }

    fun drawUntexturedSizedQuad(x: Int, y: Int, width: Int, height: Int) {
        drawUntexturedSizedQuad(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    }

    fun drawAtlasSprite(x1: Double, y1: Double, x2: Double, y2: Double, sprite: TextureAtlasSprite) {
        drawTexturedQuad(
            x1, y1, x2, y2,
            sprite.minU.toDouble(), sprite.minV.toDouble(), sprite.maxU.toDouble(), sprite.maxV.toDouble()
        )
    }

    fun drawAtlasSprite(x1: Int, y1: Int, x2: Int, y2: Int, sprite: TextureAtlasSprite) {
        drawTexturedQuad(x1, y1, x2, y2, sprite.minU, sprite.minV, sprite.maxU, sprite.maxV)
    }

    fun drawSizedAtlasSprite(x: Double, y: Double, width: Double, height: Double, sprite: TextureAtlasSprite) {
        drawAtlasSprite(x, y, x + width, y + height, sprite)
    }

    fun drawSizedAtlasSprite(x: Int, y: Int, width: Int, height: Int, sprite: TextureAtlasSprite) {
        drawAtlasSprite(x, y, x + width, y + height, sprite)
    }

    inline fun withGuiItemSetup(crossinline action: () -> Unit) {
        RenderHelper.enableGUIStandardItemLighting()
        action()
        RenderHelper.disableStandardItemLighting()
    }

    fun drawItemAndOverlay(
        x: Int,
        y: Int,
        stack: ItemStack,
        owner: EntityLivingBase? = null,
        countText: String? = null
    ) {
        if (stack.isEmpty) return
        withGuiItemSetup {
            val mc = Minecraft.getMinecraft()
            val renderItem = mc.renderItem
            renderItem.renderItemAndEffectIntoGUI(owner, stack, x, y)
            renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, stack, x, y, countText)
        }
    }

    fun drawItem(x: Int, y: Int, stack: ItemStack, owner: EntityLivingBase? = null) {
        if (stack.isEmpty) return
        withGuiItemSetup {
            Minecraft.getMinecraft().renderItem.renderItemAndEffectIntoGUI(owner, stack, x, y)
        }
    }
}
