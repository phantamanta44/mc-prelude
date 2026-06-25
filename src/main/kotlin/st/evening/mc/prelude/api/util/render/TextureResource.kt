package st.evening.mc.prelude.api.util.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiIngame
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.ITextureObject
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.util.ResourceLocation
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
class TextureResource(val path: ResourceLocation) {
    companion object {
        val ITEM_BLOCK_ATLAS: TextureResource = TextureResource(TextureMap.LOCATION_BLOCKS_TEXTURE)
        val MISSING: TextureResource = TextureResource(TextureMap.LOCATION_MISSING_TEXTURE)
        val WIDGETS: TextureResource = TextureResource(GuiIngame.WIDGETS_TEX_PATH)
    }

    private var textureObject: ITextureObject? = null

    private fun getOrLoadTexture(): ITextureObject {
        textureObject?.let { return it }

        val texMgr = Minecraft.getMinecraft().textureManager
        @Suppress("UNNECESSARY_SAFE_CALL")
        texMgr.getTexture(path)?.let {
            textureObject = it
            return it
        }

        val tex = SimpleTexture(path)
        texMgr.loadTexture(path, tex)
        textureObject = tex
        return tex
    }

    fun bind() {
        // may be problematic if the texture unit was deleted for some reason
        GlStateManager.bindTexture(getOrLoadTexture().glTextureId)
    }
}
