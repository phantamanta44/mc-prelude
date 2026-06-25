package st.evening.mc.prelude.example

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Items
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.oredict.OreIngredient
import st.evening.mc.prelude.api.PreludeMod
import st.evening.mc.prelude.api.block.prefab.BlockFacingIfc
import st.evening.mc.prelude.api.config.json.JsonConfig
import st.evening.mc.prelude.api.config.json.defaultInit
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.creativeTab
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity
import st.evening.mc.prelude.loader.PreludeLoader

private const val MOD_ID: String = "prexample"

@PreludeLoader.Load(MOD_ID)
object PreludeExample : PreludeMod<PreludeExampleDefinitions> {
    override val modId: String
        get() = MOD_ID
    private val configData: JsonConfig = JsonConfig(MOD_ID)
    val config: ExampleConfig = ExampleConfig(configData.root)
    val defns: PreludeExampleDefinitions by PreludeLoader.inject()

    override fun register(reg: ModRegistrar): PreludeExampleDefinitions {
        configData.defaultInit(reg)
        return PreludeExampleDefinitions(reg)
    }
}

class PreludeExampleDefinitions internal constructor(reg: ModRegistrar) {
    val tab: CreativeTabs = reg.creativeTab { items.material.newStack(MaterialType.STEEL_INGOT, 1) }

    val blocks: ExampleBlocks = ExampleBlocks(reg)
    val items: ExampleItems = ExampleItems(reg)
    val tileEntities: ExampleTileEntities = ExampleTileEntities(reg)
    val containers: ExampleContainers = ExampleContainers(reg)
    val sounds: ExampleSounds = ExampleSounds(reg)

    init {
        reg.on<FMLInitializationEvent> { _ ->
            OreDictionary.registerOre("oreCopper", blocks.ore.newStack(OreType.COPPER, 1))
            OreDictionary.registerOre("ingotCopper", items.material.newStack(MaterialType.COPPER_INGOT, 1))
            OreDictionary.registerOre("blockCopper", blocks.metal.newStack(MetalType.COPPER, 1))
            OreDictionary.registerOre("ingotSteel", items.material.newStack(MaterialType.STEEL_INGOT, 1))
            OreDictionary.registerOre("blockSteel", blocks.metal.newStack(MetalType.STEEL, 1))
        }
        reg.on<RegistryEvent.Register<IRecipe>> { _ ->
            GameRegistry.addSmelting(
                blocks.ore.newStack(OreType.COPPER, 1),
                items.material.newStack(MaterialType.COPPER_INGOT, 1),
                0.7F
            )
            AlloyFurnaceRecipe.register(
                AlloyFurnaceRecipe(
                    OreIngredient("ingotIron"),
                    Ingredient.fromItem(Items.COAL),
                    items.material.newStack(MaterialType.STEEL_INGOT, 1),
                    32
                )
            )
        }
        reg.on<PlayerInteractEvent.LeftClickBlock> { event ->
            val player = event.entityPlayer
            val world = player.world
            val state = world.getBlockState(event.pos)
            if (state.block != blocks.barrel || state.getValue(BlockFacingIfc.PROP_FACING) != event.face) return@on
            event.isCanceled = true
            event.useItem = Event.Result.ALLOW
            world.onServer {
                world.useTileEntity<TileEntityBarrel>(event.pos) { it.handleExtract(player) }
            }
        }
    }
}
