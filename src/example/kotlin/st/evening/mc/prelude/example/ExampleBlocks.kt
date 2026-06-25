package st.evening.mc.prelude.example

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.util.IStringSerializable
import st.evening.mc.prelude.api.block.prefab.PrefabBlockEnum
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.RegistrationHolder
import st.evening.mc.prelude.api.registration.block

class ExampleBlocks(reg: ModRegistrar) {
    val ore: PrefabBlockEnum<OreType> by reg.myBlock("ore") {
        PrefabBlockEnum<OreType>(Material.ROCK).apply {
            setHardness(3F)
            setResistance(5F)
            setSoundType(SoundType.STONE)
            OreType.entries.forEach {
                setHarvestLevel("pickaxe", it.harvestLevel, defaultState.withProperty(propVariant, it))
            }
            setHarvestLevel("pickaxe", 1)
        }
    }
    val metal: PrefabBlockEnum<MetalType> by reg.myBlock("metal_block") {
        PrefabBlockEnum<MetalType>(Material.IRON).apply {
            setHardness(5F)
            setResistance(10F)
            setSoundType(SoundType.METAL)
        }
    }
    val alloyFurnace: BlockAlloyFurnace by reg.myBlock("alloy_furnace") { BlockAlloyFurnace() }
    val barrel: BlockBarrel by reg.myBlock("barrel") { BlockBarrel() }

    private fun <B : Block> ModRegistrar.myBlock(name: String, factory: () -> B): RegistrationHolder<B> =
        block(name) { factory().also { it.creativeTab = PreludeExample.defns.tab } }
}

enum class OreType(val harvestLevel: Int) : IStringSerializable {
    COPPER(1);

    override fun getName(): String = name.lowercase()
}

enum class MetalType : IStringSerializable {
    COPPER, STEEL;

    override fun getName(): String = name.lowercase()
}
