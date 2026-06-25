package st.evening.mc.prelude

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetHandlerPlayServer
import net.minecraftforge.classloading.FMLForgePlugin
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.apache.logging.log4j.Logger
import st.evening.mc.prelude.api.ClientApi
import st.evening.mc.prelude.api.PreludeMod
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.config.json.JsonConfig
import st.evening.mc.prelude.api.config.json.defaultInitSync
import st.evening.mc.prelude.api.data.sync.SyncService
import st.evening.mc.prelude.api.event.ConfigSyncEvent
import st.evening.mc.prelude.api.event.InitializationCompleteEvent
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.newModLogger
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.capability
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.registration.s2cPacket
import st.evening.mc.prelude.api.tileentity.TileEntityLoadService
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.OreDictCache
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.absurdPhysicalSide
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.prelude.api.util.game.physicalSided
import st.evening.mc.prelude.loader.PreludeLoader
import st.evening.mc.prelude.mod.PreludeConfigGuiScreen
import st.evening.mc.prelude.mod.feature.PreludeDevEnvService
import st.evening.mc.prelude.mod.feature.Unclunkify
import st.evening.mc.prelude.mod.network.S2CBindSyncedContainer
import st.evening.mc.prelude.mod.network.S2CSyncConfig
import st.evening.mc.prelude.mod.network.S2CSyncData

@PreludeLoader.Load(PreludeConsts.MOD_ID)
object Prelude : PreludeMod<PreludeDefinitions> {
    override val modId: String
        get() = PreludeConsts.MOD_ID

    internal val logger: Logger = newModLogger()
    private val configData: JsonConfig = JsonConfig(PreludeConsts.MOD_ID)
    val config: PreludeConfig = PreludeConfig(configData.root)
    val defns: PreludeDefinitions by PreludeLoader.inject()

    override fun register(reg: ModRegistrar): PreludeDefinitions {
        logger.info("Begin.")
        configData.loadOrSaveToDisk()
        onPhysicalClient {
            reg.on<InitializationCompleteEvent> { _ ->
                reg.setConfigGuiFactory(PreludeConfigGuiScreen.Factory(configData.root))
            }
            reg.on<ConfigChangedEvent.OnConfigChangedEvent> { event ->
                if (event.modID == PreludeConsts.MOD_ID) {
                    configData.saveToDisk()
                }
            }
        }
        configData.defaultInitSync(reg)
        return physicalSided({ PreludeDefinitions.Server(reg) }, { PreludeDefinitions.Client(reg) })
    }
}

sealed class PreludeDefinitions(reg: ModRegistrar) {
    val tileEntityLoadService: TileEntityLoadService = TileEntityLoadService()
    val syncService: SyncService = SyncService.create(reg)
    val oreDictCache: OreDictCache = OreDictCache(reg)

    val s2cSyncConfig: PacketType.S2C<S2CSyncConfig>
        by reg.s2cPacket(S2CSyncConfig.Serializer, S2CSyncConfig.Handler)
    val s2cSyncData: PacketType.S2C<S2CSyncData>
        by reg.s2cPacket(S2CSyncData.Serializer, S2CSyncData.Handler)
    val s2cBindSyncedContainer: PacketType.S2C<S2CBindSyncedContainer>
        by reg.s2cPacket(S2CBindSyncedContainer.Serializer, S2CBindSyncedContainer.Handler)

    val capItemStore: Capability<ItemStore> by reg.capability { ItemStore.Empty }

    @ClientSide.Physical
    abstract fun client(): ClientApi

    init {
        if (!FMLForgePlugin.RUNTIME_DEOBF) {
            PreludeDevEnvService.init()
        }
        Unclunkify.init(reg)
        reg.on<FMLNetworkEvent.ServerConnectionFromClientEvent> @OptIn(SidednessAssertion::class) { event ->
            if (event.isLocal) return@on
            (event.handler as? NetHandlerPlayServer)?.let { // this cast really shouldn't fail
                val player = it.player
                val configs = mutableMapOf<String, MutableMap<String, NBTTagCompound>>()
                MinecraftForge.EVENT_BUS.post(ConfigSyncEvent.Build(player, configs))
                if (configs.isNotEmpty()) {
                    assertLogicalServer {
                        s2cSyncConfig.sendTo(S2CSyncConfig(configs), player)
                    }
                }
            }
        }
    }

    @ServerSide.Physical
    internal class Server(reg: ModRegistrar) : PreludeDefinitions(reg) {
        @ClientSide.Physical
        override fun client(): ClientApi = absurdPhysicalSide()
    }

    @ClientSide.Physical
    internal class Client(reg: ModRegistrar) : PreludeDefinitions(reg) {
        private val clientApi: ClientApi = ClientApi(reg)

        override fun client(): ClientApi = clientApi
    }
}
