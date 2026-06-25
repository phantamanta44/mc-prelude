package st.evening.mc.prelude.example

import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.registration.tileEntity

class ExampleTileEntities(reg: ModRegistrar) {
    val alloyFurnace: TileEntityType<TileEntityAlloyFurnace> by reg.tileEntity("alloy_furnace")
    val barrel: TileEntityType<TileEntityBarrel> by reg.tileEntity("barrel")
}
