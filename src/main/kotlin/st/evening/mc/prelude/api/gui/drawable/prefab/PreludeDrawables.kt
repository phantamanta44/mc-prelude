package st.evening.mc.prelude.api.gui.drawable.prefab

import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.sliceSized
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.TextureResource

@ClientSide.Physical
object PreludeTextures {
    val windowBg: TextureResource = TextureResource(Prelude.resource("textures/gui/window.png"))
    val slot: TextureResource = TextureResource(Prelude.resource("textures/gui/slot.png"))
    val progress: TextureResource = TextureResource(Prelude.resource("textures/gui/progress.png"))
}

@ClientSide.Physical
object PreludeDrawables {
    val widgets: GuiSamplable = DrawableTexture(256, 256, TextureResource.WIDGETS)
    val button: GuiDrawable = Drawable9Tile.fromSlices(widgets.sliceSized(200, 20, 0, 66), 2, 2, 2, 3)
    val buttonHovered: GuiDrawable = Drawable9Tile.fromSlices(widgets.sliceSized(200, 20, 0, 86), 2, 2, 2, 3)
    val buttonDisabled: GuiDrawable = Drawable9Tile.fromSlices(widgets.sliceSized(200, 20, 0, 46), 2, 2, 2, 3)

    val windowBg: GuiDrawable = Drawable9Tile.fromSlices(DrawableTexture(12, 12, PreludeTextures.windowBg), 4, 4, 4, 4)
    val slot: GuiDrawable = Drawable9Tile.fromSlices(DrawableTexture(18, 18, PreludeTextures.slot), 1, 1, 1, 1)
    val progressBg: GuiSamplable
    val progressFg: GuiSamplable

    init {
        val progressTex = DrawableTexture(22, 32, PreludeTextures.progress)
        progressBg = progressTex.sliceSized(22, 16, 0, 0)
        progressFg = progressTex.sliceSized(22, 16, 0, 16)
    }
}
