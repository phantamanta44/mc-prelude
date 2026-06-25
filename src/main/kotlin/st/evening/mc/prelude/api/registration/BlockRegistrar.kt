package st.evening.mc.prelude.api.registration

import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.registry.ForgeRegistries
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.block.CustomItemBlock
import st.evening.mc.prelude.api.block.CustomModelBlock
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.onPhysicalClient

class BlockRegistrar(private val modId: String) {
    companion object {
        val TARGET: RegistrationTarget<BlockRegistrar, Entry.T, Block> =
            object : RegistrationTarget<BlockRegistrar, Entry.T, Block> {
                override fun init(mod: ModContainer, reg: ModRegistrar): BlockRegistrar =
                    BlockRegistrar(mod.modId).also { blockReg ->
                        reg.on<RegistryEvent.Register<Block>>(listener = blockReg::dispatchBlocks)
                        reg.on<RegistryEvent.Register<Item>>(listener = blockReg::dispatchItems)
                        onPhysicalClient {
                            reg.on<ModelRegistryEvent> { blockReg.dispatchModels() }
                        }
                    }

                override fun <U : Block> register(state: BlockRegistrar, entry: Ap<Entry.T, U>): RegistrationHolder<U> =
                    state.register(entry.unap())
            }
    }

    private val blockRegQueue: MutableList<BlockRegTask> = mutableListOf()
    private val itemRegQueue: MutableList<Block> = mutableListOf()

    @ClientSide.Physical
    private val modelRegQueue: MutableList<ModelRegTask> = mutableListOf()

    private fun <B : Block> register(entry: Entry<B>): RegistrationHolder<B> {
        val key = ResourceLocation(modId, entry.name)
        Prelude.logger.trace("Registering block: {}", key)
        blockRegQueue += BlockRegTask(key, entry.factory)
        return RegistrationHolder.FromForgeRegistry(ForgeRegistries.BLOCKS, key)
    }

    private fun dispatchBlocks(event: RegistryEvent.Register<Block>) {
        Prelude.logger.debug("Dispatching {} {} block registrations", blockRegQueue.size, modId)
        blockRegQueue.forEach { task ->
            Prelude.logger.trace("Dispatching block registration: {}", task.key)
            val block = task.factory().apply {
                registryName = task.key
                translationKey = "${task.key.namespace}.${task.key.path}"
            }
            event.registry.register(block)
            itemRegQueue += block
        }
        blockRegQueue.clear()
    }

    private fun dispatchItems(event: RegistryEvent.Register<Item>) {
        Prelude.logger.debug("Dispatching {} {} block item registrations", itemRegQueue.size, modId)
        itemRegQueue.forEach { block ->
            Prelude.logger.trace("Dispatching block item registration: {}", block.registryName)
            val item = if (block is CustomItemBlock) {
                block.createBlockItem()
            } else {
                ItemBlock(block)
            }
            item?.let {
                it.registryName = block.registryName!!
                event.registry.register(it)
            }
            onPhysicalClient { modelRegQueue += ModelRegTask(block, item) }
        }
        itemRegQueue.clear()
    }

    @ClientSide.Physical
    private fun dispatchModels() {
        Prelude.logger.debug("Dispatching {} {} block model registrations", modelRegQueue.size, modId)
        modelRegQueue.forEach { task ->
            Prelude.logger.trace("Dispatching block model registration: {}", task.block.registryName)
            if (task.block is CustomModelBlock) {
                task.block.registerBlockModels()
            }
            task.item?.let {
                if (it is CustomModelItem) {
                    it.registerItemModels()
                } else {
                    ModelLoader.setCustomModelResourceLocation(
                        it, 0, ModelResourceLocation(it.registryName!!, "inventory")
                    )
                }
            }
        }
        modelRegQueue.clear()
    }

    class Entry<B : Block>(val name: String, val factory: () -> B) : Ap<Entry.T, B> {
        object T : TT
    }

    private class BlockRegTask(val key: ResourceLocation, val factory: () -> Block)

    @ClientSide.Physical
    private class ModelRegTask(val block: Block, val item: ItemBlock?)
}

fun <B : Block> Ap<BlockRegistrar.Entry.T, B>.unap(): BlockRegistrar.Entry<B> = this as BlockRegistrar.Entry<B>

fun <B : Block> ModRegistrar.block(name: String, factory: () -> B): RegistrationHolder<B> =
    register(BlockRegistrar.TARGET, BlockRegistrar.Entry(name, factory))
