package st.evening.mc.prelude.api

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.util.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.util.game.ClientSide

interface PreludeMod<T : Any> {
    val modId: String
    val compatibleVersionRange: String?
        get() = null

    fun register(reg: ModRegistrar): T
}

fun PreludeMod<*>.newModLogger(): Logger = LogManager.getLogger(modId)

fun PreludeMod<*>.resource(path: String): ResourceLocation = ResourceLocation(modId, path)

@ClientSide.Physical
fun PreludeMod<*>.modelResource(path: String, variant: String): ModelResourceLocation =
    ModelResourceLocation("$modId:$path", variant)
