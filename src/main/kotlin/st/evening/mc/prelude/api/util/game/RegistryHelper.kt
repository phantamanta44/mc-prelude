package st.evening.mc.prelude.api.util.game

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fml.common.registry.ForgeRegistries
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath

// the getAssert functions are mainly for deserialization, which is why they throw SerializationExceptions
object RegistryHelper {
    fun getAssertBlock(blockName: ResourceLocation): Block =
        ForgeRegistries.BLOCKS.getValue(blockName) ?: throw SerializationException("Unknown block: $blockName")

    fun getAssertBlock(blockName: String): Block = getAssertBlock(ResourceLocation(blockName))

    fun getAssertBlock(path: JsonPath, blockName: ResourceLocation): Block =
        ForgeRegistries.BLOCKS.getValue(blockName) ?: throw SerializationException(path, "Unknown block: $blockName")

    fun getAssertBlock(path: JsonPath, blockName: String): Block = getAssertBlock(path, ResourceLocation(blockName))

    fun getAssertBlock(blockId: Int): Block =
        Block.getBlockById(blockId) ?: throw SerializationException("Unknown block ID: $blockId")

    fun getAssertItem(itemName: ResourceLocation): Item =
        ForgeRegistries.ITEMS.getValue(itemName) ?: throw SerializationException("Unknown item: $itemName")

    fun getAssertItem(itemName: String): Item = getAssertItem(ResourceLocation(itemName))

    fun getAssertItem(path: JsonPath, itemName: ResourceLocation): Item =
        ForgeRegistries.ITEMS.getValue(itemName) ?: throw SerializationException(path, "Unknown item: $itemName")

    fun getAssertItem(path: JsonPath, itemName: String): Item = getAssertItem(path, ResourceLocation(itemName))

    fun getAssertItem(itemId: Int): Item =
        Item.getItemById(itemId) ?: throw SerializationException("Unknown item ID: $itemId")

    fun getAssertFluid(fluidName: String): Fluid =
        FluidRegistry.getFluid(fluidName) ?: throw SerializationException("Unknown fluid: $fluidName")

    fun getAssertFluid(path: JsonPath, fluidName: String): Fluid =
        FluidRegistry.getFluid(fluidName) ?: throw SerializationException(path, "Unknown fluid: $fluidName")
}
