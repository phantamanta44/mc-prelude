package st.evening.mc.prelude.example

import st.evening.mc.prelude.api.registration.ContainerType
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.container

class ExampleContainers(reg: ModRegistrar) {
    val alloyFurnace: ContainerType by reg.container(ContainerAlloyFurnace.Factory)
}
