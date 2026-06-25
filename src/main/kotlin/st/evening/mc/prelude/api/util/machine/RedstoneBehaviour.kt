package st.evening.mc.prelude.api.util.machine

import net.minecraft.client.resources.I18n
import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.mod.PreludeStrings

enum class RedstoneBehaviour(val translationKey: String) {
    NONE(PreludeStrings.ENUM_REDSTONE_BEHAVIOUR_NONE) {
        override fun canWork(redstoneState: Boolean): Boolean = true
    },
    ACTIVE_LOW(PreludeStrings.ENUM_REDSTONE_BEHAVIOUR_ACTIVE_LOW) {
        override fun canWork(redstoneState: Boolean): Boolean = !redstoneState
    },
    ACTIVE_HIGH(PreludeStrings.ENUM_REDSTONE_BEHAVIOUR_ACTIVE_HIGH) {
        override fun canWork(redstoneState: Boolean): Boolean = redstoneState
    };

    abstract fun canWork(redstoneState: Boolean): Boolean

    @ClientSide.Physical
    fun getLocalizedName(): String = I18n.format(translationKey)

    companion object {
        val serializer: EnumSerializer<RedstoneBehaviour> = EnumSerializer()
    }
}
