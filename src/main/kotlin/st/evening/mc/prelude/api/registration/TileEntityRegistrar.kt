package st.evening.mc.prelude.api.registration

import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.registry.GameRegistry
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.type.Ap
import st.evening.mc.prelude.api.type.TT

class TileEntityType<out T : TileEntity>(
    val id: ResourceLocation,
    val teClass: Class<out T>,
    val factory: (World, IBlockState) -> T
)

class TileEntityRegistrar(private val modId: String) {
    companion object {
        val TARGET: RegistrationTarget<TileEntityRegistrar, Entry.T, TileEntityType<*>> =
            object : RegistrationTarget<TileEntityRegistrar, Entry.T, TileEntityType<*>> {
                override fun init(mod: ModContainer, reg: ModRegistrar): TileEntityRegistrar =
                    TileEntityRegistrar(mod.modId)

                override fun <U : TileEntityType<*>> register(
                    state: TileEntityRegistrar,
                    entry: Ap<Entry.T, U>
                ): RegistrationHolder<U> = state.register(entry.unap())
            }

        fun <T : TileEntity> getDefaultFactory(teClass: Class<T>): (World, IBlockState) -> T {
            val ctor = teClass.getConstructor()
            return { _, _ -> ctor.newInstance() }
        }

        inline fun <reified T : TileEntity> getDefaultFactory(): (World, IBlockState) -> T =
            getDefaultFactory(T::class.java)
    }

    fun <T : TileEntityType<*>> register(entry: Entry<T>): RegistrationHolder<T> {
        val key = ResourceLocation(modId, entry.name)
        Prelude.logger.trace("Registering tile entity type: {}", key)
        val type = entry.factory(key)
        GameRegistry.registerTileEntity(type.teClass, key)
        Prelude.defns.tileEntityLoadService.register(type)
        return RegistrationHolder.Immediate(type)
    }

    class Entry<T : TileEntityType<*>>(val name: String, val factory: (ResourceLocation) -> T) : Ap<Entry.T, T> {
        object T : TT
    }
}

fun <T : TileEntityType<*>> Ap<TileEntityRegistrar.Entry.T, T>.unap(): TileEntityRegistrar.Entry<T> =
    this as TileEntityRegistrar.Entry<T>

fun <T : TileEntity> ModRegistrar.tileEntity(
    name: String,
    teClass: Class<T>,
    factory: (World, IBlockState) -> T = TileEntityRegistrar.getDefaultFactory(teClass)
): RegistrationHolder<TileEntityType<T>> = register(
    TileEntityRegistrar.TARGET,
    TileEntityRegistrar.Entry(name) { TileEntityType(it, teClass, factory) }
)

inline fun <reified T : TileEntity> ModRegistrar.tileEntity(
    name: String,
    noinline factory: (World, IBlockState) -> T = TileEntityRegistrar.getDefaultFactory<T>()
): RegistrationHolder<TileEntityType<T>> = tileEntity(name, T::class.java, factory)
