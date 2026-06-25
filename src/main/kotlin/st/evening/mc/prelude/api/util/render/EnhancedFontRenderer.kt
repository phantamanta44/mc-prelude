package st.evening.mc.prelude.api.util.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.coremod.mixin.FontRendererAccessor
import kotlin.math.roundToInt

@ClientSide.Physical
class EnhancedFontRenderer(
    private val mc: Minecraft,
    fontTexture: ResourceLocation,
    textureManager: TextureManager,
    unicode: Boolean
) : FontRenderer(mc.gameSettings, fontTexture, textureManager, unicode) {
    companion object {
        private const val PUA_START: Char = '\uE000'
        private const val PUA_END: Char = '\uF900'

        fun fromParent(parent: FontRenderer): EnhancedFontRenderer {
            parent as FontRendererAccessor
            val renderer = EnhancedFontRenderer(
                Minecraft.getMinecraft(),
                parent.`prelude$locationFontTexture`(),
                parent.`prelude$renderEngine`(),
                parent.unicodeFlag
            )
            renderer.FONT_HEIGHT = parent.FONT_HEIGHT
            renderer.bidiFlag = parent.bidiFlag
            return renderer
        }

        fun fromDefault(): EnhancedFontRenderer = fromParent(Minecraft.getMinecraft().fontRenderer)
    }

    private val icons: MutableList<IconEntry> = mutableListOf()

    private var drawingShadow: Boolean = false
    private var lastRed: Float = 1F
    private var lastGreen: Float = 1F
    private var lastBlue: Float = 1F

    fun addIcon(
        icon: GuiDrawable,
        useTextColour: Boolean = false,
        drawShadow: Boolean = false,
        vAlign: DrawAlignment = DrawAlignment.CENTER
    ): Char {
        // assume we'll never exhaust the 6400-char private use area
        icons += IconEntry(icon, useTextColour, drawShadow, vAlign)
        return PUA_START + (icons.size - 1)
    }

    private fun getIcon(ch: Char): IconEntry? {
        if (ch !in PUA_START..<PUA_END) return null
        val k = ch - PUA_START
        return orNull(k < icons.size) { icons[k] }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun getAlpha(): Float = (this as FontRendererAccessor).`prelude$alpha`()

    override fun getCharWidth(character: Char): Int =
        (getIcon(character) ?: return super.getCharWidth(character)).drawable.width + 1

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        super.setColor(r, g, b, a)
        lastRed = r
        lastGreen = g
        lastBlue = b
    }

    override fun renderString(text: String, x: Float, y: Float, color: Int, dropShadow: Boolean): Int {
        if (!dropShadow) return super.renderString(text, x, y, color, false)
        drawingShadow = true
        val width = super.renderString(text, x, y, color, true)
        drawingShadow = false
        return width
    }

    override fun renderUnicodeChar(ch: Char, italic: Boolean): Float {
        val icon = getIcon(ch) ?: return super.renderUnicodeChar(ch, italic)
        icon.draw()
        return (icon.drawable.width + 1).toFloat()
    }

    private inner class IconEntry(
        val drawable: GuiDrawable,
        val useTextColour: Boolean,
        val drawShadow: Boolean,
        val vAlign: DrawAlignment
    ) {
        fun draw() {
            if (useTextColour) {
                if (drawShadow || !drawingShadow) {
                    drawDrawable()
                }
            } else if (drawingShadow) {
                if (drawShadow) {
                    val alpha = getAlpha()
                    GlStateManager.color(0.25F, 0.25F, 0.25F, alpha)
                    drawDrawable()
                    GlStateManager.color(lastRed, lastGreen, lastBlue, alpha)
                }
            } else {
                val alpha = getAlpha()
                GlStateManager.color(1F, 1F, 1F, alpha)
                drawDrawable()
                GlStateManager.color(lastRed, lastGreen, lastBlue, alpha)
            }
        }

        private fun drawDrawable() {
            // vanilla FontRenderer shrinks the character by 0.01 in both dimensions, but we can probably skip that
            drawable.drawFullSize(
                mc.renderPartialTicks,
                posX.roundToInt(),
                posY.roundToInt() + vAlign.computeOffset(IntArithmetic, drawable.height, FONT_HEIGHT - 1)
            )
        }
    }
}
