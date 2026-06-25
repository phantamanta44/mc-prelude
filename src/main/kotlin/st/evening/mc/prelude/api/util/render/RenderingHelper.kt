package st.evening.mc.prelude.api.util.render

import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11
import st.evening.mc.prelude.api.util.data.getByte1AsFloat
import st.evening.mc.prelude.api.util.data.getByte2AsFloat
import st.evening.mc.prelude.api.util.data.getByte3AsFloat
import st.evening.mc.prelude.api.util.data.getByte4AsFloat
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.coremod.mixin.GlStateManagerAccessor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ClientSide.Physical
@OptIn(ExperimentalContracts::class)
inline fun Tessellator.tessellate(
    drawMode: Int,
    vertexFormat: VertexFormat,
    crossinline action: BufferBuilder.() -> Unit
) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val buf = buffer
    buf.begin(drawMode, vertexFormat)
    buf.action()
    draw()
}

@ClientSide.Physical
object RenderingHelper { // would have preferred RenderHelper but that clashes with n.m.client.renderer.RenderHelper
    private val stencilState: GlStateManager.StencilState = GlStateManagerAccessor.`prelude$stencilState`()
    private var stencilRef: Int = 0 // these won't track state changes by other mods, but we can't really do better
    private var stencilTestEnabled: Boolean = false // we'll just assume they properly clean up after themselves

    @OptIn(ExperimentalContracts::class)
    inline fun pushMatrix(crossinline action: () -> Unit) {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        GlStateManager.pushMatrix()
        action()
        GlStateManager.popMatrix()
    }

    @OptIn(ExperimentalContracts::class)
    inline fun pushAttrib(crossinline action: () -> Unit) {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        GlStateManager.pushAttrib()
        action()
        GlStateManager.popAttrib()
    }

    fun translate(x: Int, y: Int, z: Int) {
        GlStateManager.translate(x.toFloat(), y.toFloat(), z.toFloat())
    }

    fun translateMotionInterpolation(entity: Entity, partialTicks: Float) {
        GlStateManager.translate(
            entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks,
            entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks,
            entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks
        )
    }

    fun translateInverseMotionInterpolation(entity: Entity, partialTicks: Float) {
        GlStateManager.translate(
            -(entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks),
            -(entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks),
            -(entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks)
        )
    }

    fun translate2(x: Int, y: Int) {
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0F)
    }

    fun translate2(x: Float, y: Float) {
        GlStateManager.translate(x, y, 0F)
    }

    fun translate2(x: Double, y: Double) {
        GlStateManager.translate(x, y, 0.0)
    }

    fun rotateNorthTo(face: EnumFacing) {
        when (face) {
            EnumFacing.NORTH -> {}
            EnumFacing.EAST -> GlStateManager.rotate(90F, 0F, 1F, 0F)
            EnumFacing.SOUTH -> GlStateManager.rotate(180F, 0F, 1F, 0F)
            EnumFacing.WEST -> GlStateManager.rotate(270F, 0F, 1F, 0F)
            EnumFacing.UP -> GlStateManager.rotate(90F, 1F, 0F, 0F)
            EnumFacing.DOWN -> GlStateManager.rotate(-90F, 1F, 0F, 0F)
        }
    }

    fun setColourRgb(colour: Int) {
        GlStateManager.color(colour.getByte2AsFloat(), colour.getByte3AsFloat(), colour.getByte4AsFloat())
    }

    fun setColourRgb(colour: Int, alpha: Float) {
        GlStateManager.color(colour.getByte2AsFloat(), colour.getByte3AsFloat(), colour.getByte4AsFloat(), alpha)
    }

    fun setColourArgb(colour: Int) {
        GlStateManager.color(
            colour.getByte2AsFloat(),
            colour.getByte3AsFloat(),
            colour.getByte4AsFloat(),
            colour.getByte1AsFloat()
        )
    }

    fun resetColour() {
        GlStateManager.color(1F, 1F, 1F, 1F)
    }

    fun stencilFunc(func: Int, ref: Int, mask: Int) {
        if (func == stencilState.func.func && ref == stencilRef && mask == stencilState.func.mask) return
        stencilState.func.func = func
        stencilRef = ref
        stencilState.func.mask = mask
        GL11.glStencilFunc(func, ref, mask)
    }

    fun stencilMask(mask: Int) {
        if (mask == stencilState.mask) return
        stencilState.mask = mask
        GL11.glStencilMask(mask)
    }

    fun stencilOp(sfail: Int, dpfail: Int, dppass: Int) { // prefer the nomenclature used in the khronos docs
        if (sfail == stencilState.fail && dpfail == stencilState.zfail && dppass == stencilState.zpass) return
        stencilState.fail = sfail
        stencilState.zfail = dpfail
        stencilState.zpass = dppass
        GL11.glStencilOp(sfail, dpfail, dppass)
    }

    fun enableStencilTest() {
        if (stencilTestEnabled) return
        stencilTestEnabled = true
        GL11.glEnable(GL11.GL_STENCIL_TEST)
    }

    fun disableStencilTest() {
        if (!stencilTestEnabled) return
        stencilTestEnabled = false
        GL11.glDisable(GL11.GL_STENCIL_TEST)
    }

    fun setLightmapCoords(lightmapX: Float, lightmapY: Float) {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightmapX, lightmapY)
    }

    fun setLightmapCoordsPacked(light: Int) {
        setLightmapCoords((light and 0xFFFF).toFloat(), (light ushr 16).toFloat())
    }

    inline fun withFixedLighting(lightmapX: Float, lightmapY: Float, crossinline action: () -> Unit) {
        val prevLightmapX = OpenGlHelper.lastBrightnessX
        val prevLightmapY = OpenGlHelper.lastBrightnessY
        setLightmapCoords(lightmapX, lightmapY)
        action()
        setLightmapCoords(prevLightmapX, prevLightmapY)
    }

    inline fun withFullLighting(crossinline action: () -> Unit) {
        withFixedLighting(240F, 240F, action)
    }
}
