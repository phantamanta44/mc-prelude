package st.evening.mc.prelude.api.registration

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.registry.ForgeRegistries
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.onPhysicalClient

class ItemRegistrar(private val modId: String) {
    companion object {
        val TARGET: RegistrationTarget<ItemRegistrar, Entry.T, Item> =
            object : RegistrationTarget<ItemRegistrar, Entry.T, Item> {
                override fun init(mod: ModContainer, reg: ModRegistrar): ItemRegistrar =
                    ItemRegistrar(mod.modId).also { itemReg ->
                        reg.on<RegistryEvent.Register<Item>>(listener = itemReg::dispatchItems)
                        onPhysicalClient {
                            reg.on<ModelRegistryEvent> { itemReg.dispatchModels() }
                        }
                    }

                override fun <U : Item> register(state: ItemRegistrar, entry: Ap<Entry.T, U>): RegistrationHolder<U> =
                    state.register(entry.unap())
            }
    }

    private val itemRegQueue: MutableList<ItemRegTask> = mutableListOf()

    @ClientSide.Physical
    private val modelRegQueue: MutableList<Item> = mutableListOf()

    private fun <I : Item> register(entry: Entry<I>): RegistrationHolder<I> {
        val key = ResourceLocation(modId, entry.name)
        Prelude.logger.trace("Registering item: {}", key)
        itemRegQueue += ItemRegTask(key, entry.factory)
        return RegistrationHolder.FromForgeRegistry(ForgeRegistries.ITEMS, key)
    }

    private fun dispatchItems(event: RegistryEvent.Register<Item>) {
        Prelude.logger.debug("Dispatching {} {} item registrations", itemRegQueue.size, modId)
        itemRegQueue.forEach { task ->
            Prelude.logger.trace("Dispatching item registration: {}", task.key)
            val item = task.factory().apply {
                registryName = task.key
                translationKey = "${task.key.namespace}.${task.key.path}"
            }
            event.registry.register(item)
            onPhysicalClient { modelRegQueue += item }
        }
        itemRegQueue.clear()
    }

    @ClientSide.Physical
    private fun dispatchModels() {
        Prelude.logger.debug("Dispatching {} {} item model registrations", modelRegQueue.size, modId)
        modelRegQueue.forEach { item ->
            Prelude.logger.trace("Dispatching item model registration: {}", item.registryName)
            if (item is CustomModelItem) {
                item.registerItemModels()
            } else {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, ModelResourceLocation(item.registryName!!, "inventory")
                )
            }
        }
        modelRegQueue.clear()
    }

    class Entry<I : Item>(val name: String, val factory: () -> I) : Ap<Entry.T, I> {
        object T : TT
    }

    private class ItemRegTask(val key: ResourceLocation, val factory: () -> Item)
}

fun <I : Item> Ap<ItemRegistrar.Entry.T, I>.unap(): ItemRegistrar.Entry<I> = this as ItemRegistrar.Entry<I>

fun <I : Item> ModRegistrar.item(name: String, factory: () -> I): RegistrationHolder<I> =
    register(ItemRegistrar.TARGET, ItemRegistrar.Entry(name, factory))
