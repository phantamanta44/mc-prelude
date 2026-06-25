package st.evening.mc.prelude

import st.evening.mc.prelude.api.config.json.JsonConfig

class PreludeConfig(config: JsonConfig.ConfigObject) {
    val mods: ModsConfig by config.obj("Mod loading configuration", false, ::ModsConfig)

    class ModsConfig(config: JsonConfig.ConfigObject) {
        val missingConfigKeyLoadBehaviour: MissingConfigKeyBehaviour by config.enum(
            MissingConfigKeyBehaviour.IGNORE,
            """
            What should happen if a config key is missing while loading a Prelude mod's config file?
            This could happen, for instance, if a new config key is added in an update to the mod.
            If set to IGNORE, the missing key is ignored and the default value will be used.
            If set to OVERWRITE, the config file will be overwritten to add the new keys.
            If set to FAIL, the game will crash with an error message indicating which key was missing.
            """.trimIndent()
        )
        val missingConfigKeySyncBehaviour: MissingConfigKeyBehaviour by config.enum(
            MissingConfigKeyBehaviour.IGNORE,
            """
            What should happen if a config key is missing while syncing a Prelude mod's config from a server?
            This could happen, for instance, if the mod versions are mismatched or if the server is misconfigured.
            If set to IGNORE, the missing key is ignored and the locally-configured value will be used.
            If set to OVERWRITE, the default value for the key will be used.
            If set to FAIL, the game will disconnect from the server.
            """.trimIndent()
        )

        enum class MissingConfigKeyBehaviour {
            IGNORE, OVERWRITE, FAIL
        }
    }

    val networking: NetworkingConfig by config.obj("Networking configuration", false, ::NetworkingConfig)

    class NetworkingConfig(config: JsonConfig.ConfigObject) {
        val syncDeliverBufferTtl: Long by config.long(
            1000,
            """
            The maximum time, in milliseconds, that sync packets for unbound sync hosts stay in the deliver buffer before being dropped.
            Set to zero to disable the deliver buffer entirely.
            """.trimIndent(),
            minValue = 0L
        )
        val sendItemStackCapsToServer: Boolean by config.bool(
            true,
            """
            If enabled, item stacks will include serializable capabilities when being transmitted from the client to the server.
            This happens, for example, when manipulating the player inventory in creative mode.
            """.trimIndent()
        )
    }

    val unclunkify: UnclunkifyConfig
        by config.obj("Unclunkification module configuration", factory = ::UnclunkifyConfig)

    class UnclunkifyConfig(config: JsonConfig.ConfigObject) {
        val enabled: Boolean by config.bool(false, "Enable unclunkification tweaks?")
        val globalMiningSpeedMultiplier: Float by config.float(
            1.25F, "Speed multiplier applied to all mining.",
            minValue = 0F
        )
        val toolSpeedThreshold: Float by config.float(
            8F,
            """
            Maximum tool speed for which the tool speed modifier applies.
            For reference, vanilla diamond tools have a tool speed of 8.
            """.trimIndent(),
            minValue = 0F
        )
        val toolSpeedModifier: Float by config.float(
            0.33F,
            """
            Tool speed modifier coefficient. Modified tool speed is computed as follows:
            newSpeed = oldSpeed + toolSpeedModifier * (toolSpeedThreshold - oldSpeed)
            """.trimIndent(),
            minValue = 0F, maxValue = 1F
        )
    }
}
