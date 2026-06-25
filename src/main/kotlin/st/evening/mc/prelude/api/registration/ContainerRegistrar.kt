package st.evening.mc.prelude.api.registration

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.IGuiHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.game.assertStrongClient
import st.evening.mc.prelude.api.util.system.asAccessible

interface ContainerFactory {
    @ServerSide
    fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container?

    @ClientSide.Strong
    fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer?
}

class ContainerType(val modContainer: ModContainer, val id: Int)

@ServerSide
fun EntityPlayer.openContainer(type: ContainerType, world: World, x: Int, y: Int, z: Int) {
    openGui(type.modContainer.mod, type.id, world, x, y, z)
}

@ServerSide
fun EntityPlayer.openContainer(type: ContainerType, world: World, pos: BlockPos) {
    openContainer(type, world, pos.x, pos.y, pos.z)
}

class ContainerRegistrar(private val mod: ModContainer) : IGuiHandler {
    companion object {
        @Suppress("UNCHECKED_CAST")
        private val serverGuiHandlers: MutableMap<ModContainer, IGuiHandler> =
            NetworkRegistry::class.java.getDeclaredField("serverGuiHandlers")
                .asAccessible().get(NetworkRegistry.INSTANCE) as MutableMap<ModContainer, IGuiHandler>

        @Suppress("UNCHECKED_CAST")
        private val clientGuiHandlers: MutableMap<ModContainer, IGuiHandler> =
            NetworkRegistry::class.java.getDeclaredField("clientGuiHandlers")
                .asAccessible().get(NetworkRegistry.INSTANCE) as MutableMap<ModContainer, IGuiHandler>

        val TARGET: RegistrationTarget<ContainerRegistrar, Entry.T, ContainerType> =
            object : RegistrationTarget<ContainerRegistrar, Entry.T, ContainerType> {
                override fun init(mod: ModContainer, reg: ModRegistrar): ContainerRegistrar =
                    ContainerRegistrar(mod).also { contReg ->
                        reg.on<FMLPreInitializationEvent> {
                            // NetworkRegistry#registerGuiHandler does an annoying unnecessary mod container lookup, so
                            // we bypass it here by modifying the registries directly
                            serverGuiHandlers[mod] = contReg
                            clientGuiHandlers[mod] = contReg
                        }
                    }

                @Suppress("UNCHECKED_CAST")
                override fun <U : ContainerType> register(
                    state: ContainerRegistrar,
                    entry: Ap<Entry.T, U>
                ): RegistrationHolder<U> = state.register(entry.unap()) as RegistrationHolder<U>
            }
    }

    private val registry: MutableList<ContainerFactory> = mutableListOf()

    @OptIn(SidednessAssertion::class)
    override fun getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        assertLogicalServer {
            if (id < 0 || id >= registry.size) {
                Prelude.logger.warn("Server requested unknown container type ID {} for mod {}", id, mod.modId)
                return null
            }
            return registry[id].createServerContainer(player, world, x, y, z)
        }
    }

    @OptIn(SidednessAssertion::class)
    override fun getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        assertStrongClient {
            if (id < 0 || id > registry.size) {
                Prelude.logger.warn("Client requested unknown container type ID {} for mod {}", id, mod.modId)
                return null
            }
            return registry[id].createClientContainer(player, world, x, y, z)
        }
    }

    fun register(entry: Entry): RegistrationHolder<ContainerType> {
        Prelude.logger.trace("Registering container type: {}:{}", mod.modId, entry.factory)
        val id = registry.size // relies on deterministic registration order
        registry += entry.factory
        return RegistrationHolder.Immediate(ContainerType(mod, id))
    }

    class Entry(val factory: ContainerFactory) : Ap<Entry.T, ContainerType> {
        object T : TT
    }
}

fun Ap<ContainerRegistrar.Entry.T, *>.unap(): ContainerRegistrar.Entry = this as ContainerRegistrar.Entry

fun ModRegistrar.container(factory: ContainerFactory): RegistrationHolder<ContainerType> =
    register(ContainerRegistrar.TARGET, ContainerRegistrar.Entry(factory))
