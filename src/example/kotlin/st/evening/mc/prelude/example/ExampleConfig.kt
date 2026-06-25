package st.evening.mc.prelude.example

import st.evening.mc.prelude.api.config.json.JsonConfig

class ExampleConfig(config: JsonConfig.ConfigObject) {
    val alloyFurnaceEnergyCapacity: Int by config.int(
        80000, "Size of the alloy furnace's energy buffer.",
        minValue = 0, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART
    )
    val alloyFurnaceEnergyInputRate: Int by config.int(
        512, "Input rate limit for the alloy furnace's energy buffer.",
        minValue = 0, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART
    )
    val alloyFurnaceEnergyCost: Int by config.int(
        128, "Energy consumed by the alloy furnace each tick while in operation.",
        minValue = 0
    )

    val barrelCapacity: Int by config.int(
        4096,
        """
        Number of items that can be stored in the barrel.
        If set to a lower value, existing barrels may void excess items!
        """.trimIndent(),
        minValue = 1, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART
    )
}
