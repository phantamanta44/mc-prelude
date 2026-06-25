package st.evening.mc.prelude.api.gui.engine.prefab

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextComponentTranslation
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.drawAsProgress
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.FloatArithmetic
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

@ClientSide.Physical
class ProgressBarDisplay(
    private val barBackground: GuiDrawable,
    private val barForeground: GuiSamplable,
    private val getProgress: () -> Float,
    private val orientation: DrawOrientation = DrawOrientation.LEFT_TO_RIGHT,
    private val barPadTop: Int = 0,
    barPadRight: Int = 0,
    barPadBottom: Int = 0,
    private val barPadLeft: Int = 0
) : AbstractGuiElement() {
    companion object {
        fun fromMax(
            barBackground: GuiDrawable,
            barForeground: GuiSamplable,
            getValue: () -> Float,
            getMax: () -> Float,
            orientation: DrawOrientation = DrawOrientation.LEFT_TO_RIGHT,
            barPadTop: Int = 0,
            barPadRight: Int = 0,
            barPadBottom: Int = 0,
            barPadLeft: Int = 0
        ): ProgressBarDisplay = ProgressBarDisplay(
            barBackground, barForeground,
            { getMax().let { if (it <= 0) 0F else (getValue() / it) } },
            orientation, barPadTop, barPadRight, barPadBottom, barPadLeft
        )
    }

    private val totalPadX: Int = barPadLeft + barPadRight
    private val totalPadY: Int = barPadTop + barPadBottom

    override val contentWidth: Int
        get() = barBackground.width
    override val contentHeight: Int
        get() = barBackground.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart =
        Part(posX, posY, width, height)

    private inner class Part(
        posX: Int,
        posY: Int,
        override val width: Int,
        override val height: Int
    ) : AbstractGuiPart(posX, posY) {
        override fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            barBackground.draw(partialTicks, posX, posY, width, height)
        }

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            val fill = getProgress()
            if (fill <= 0) return
            barForeground.drawAsProgress(
                partialTicks,
                posX + barPadLeft,
                posY + barPadTop,
                width - totalPadX,
                height - totalPadY,
                orientation,
                fill.coerceAtMost(1F)
            )
        }
    }
}

@ClientSide.Physical
class TextDisplay(
    private val getText: () -> ITextComponent,
    private val fixedWidth: Int? = null,
    private val textAlign: DrawAlignment = DrawAlignment.START,
    private val colour: Int = GuiRenderHelper.DEFAULT_TEXT_COLOUR,
    private val dropShadow: Boolean = false,
    private val fontRenderer: FontRenderer = Minecraft.getMinecraft().fontRenderer,
) : AbstractGuiElement() {
    companion object {
        fun constant(
            text: ITextComponent,
            fixedWidth: Int? = null,
            textAlign: DrawAlignment = DrawAlignment.START,
            colour: Int = GuiRenderHelper.DEFAULT_TEXT_COLOUR,
            dropShadow: Boolean = false,
            fontRenderer: FontRenderer = Minecraft.getMinecraft().fontRenderer
        ): TextDisplay = TextDisplay({ text }, fixedWidth, textAlign, colour, dropShadow, fontRenderer)

        fun fromI18n(
            key: String,
            vararg args: Any?,
            fixedWidth: Int? = null,
            textAlign: DrawAlignment = DrawAlignment.START,
            colour: Int = GuiRenderHelper.DEFAULT_TEXT_COLOUR,
            dropShadow: Boolean = false,
            fontRenderer: FontRenderer = Minecraft.getMinecraft().fontRenderer
        ): TextDisplay =
            constant(TextComponentTranslation(key, args), fixedWidth, textAlign, colour, dropShadow, fontRenderer)

        fun fromString(
            text: String,
            fixedWidth: Int? = null,
            textAlign: DrawAlignment = DrawAlignment.START,
            colour: Int = GuiRenderHelper.DEFAULT_TEXT_COLOUR,
            dropShadow: Boolean = false,
            fontRenderer: FontRenderer = Minecraft.getMinecraft().fontRenderer
        ): TextDisplay = constant(TextComponentString(text), fixedWidth, textAlign, colour, dropShadow, fontRenderer)
    }

    override val contentWidth: Int
        get() = fixedWidth ?: fontRenderer.getStringWidth(getText().formattedText)
    override val contentHeight: Int
        get() = fontRenderer.FONT_HEIGHT

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            val textString = getText().formattedText
            val offsetX = textAlign.computeOffset(
                FloatArithmetic,
                fontRenderer.getStringWidth(textString).toFloat(),
                width.toFloat()
            )
            fontRenderer.drawString(textString, posX.toFloat() + offsetX, posY.toFloat(), colour, dropShadow)
            RenderingHelper.resetColour()
        }
    }
}
