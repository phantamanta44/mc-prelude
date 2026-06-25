package st.evening.mc.prelude.api.tileentity.prefab

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.state.observeStateGeneric
import st.evening.mc.prelude.api.util.data.DelegateSuperFactory
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.getAndBind
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.TileEntityStateObserver
import st.evening.mc.prelude.api.util.game.onServer
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.api.util.world.onServer
import java.lang.invoke.MethodHandle

interface TileEntitySided<S : ServerSideSerializable, C> {
    val data: Sided<*, S, C>
}

typealias TileEntityServerOnly<S> = TileEntitySided<S, Unit>

interface TileEntitySidedIfc<S, C, TE> : ShadowDelegate<TE>
    where S : ServerSideSerializable, TE : TileEntity, TE : TileEntitySided<S, C> {

    fun setWorldCreate(world: World) {
        self.world = world
    }

    fun readFromNBT(compound: NBTTagCompound)

    fun writeToNBT(compound: NBTTagCompound): NBTTagCompound

    @OptIn(PreludeInternal::class)
    class Delegate<S, C, TE>(override val self: TE) : TileEntitySidedIfc<S, C, TE>
        where S : ServerSideSerializable, TE : TileEntity, TE : TileEntitySided<S, C> {
        companion object {
            private val factoryWriteToNBT: DelegateSuperFactory =
                superFactory("writeToNBT", "func_189515_b", NBTTagCompound::class.java, NBTTagCompound::class.java)
            private val factoryReadFromNBT: DelegateSuperFactory =
                superFactory("readFromNBT", "func_145839_a", Void.TYPE, NBTTagCompound::class.java)

            private fun superFactory(
                name: String,
                deobfName: String,
                returnType: Class<*>,
                vararg paramTypes: Class<*>
            ): DelegateSuperFactory = DelegateSuperFactory(
                TileEntitySidedIfc::class.java, TileEntity::class.java, name, deobfName, returnType, *paramTypes
            )
        }

        private val superWriteToNBT: MethodHandle = factoryWriteToNBT.getAndBind(self)!!
        private val superReadFromNBT: MethodHandle = factoryReadFromNBT.getAndBind(self)!!

        override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
            superWriteToNBT(compound)
            self.world.onServer {
                self.data.getServer().writeToNbtServerSide(compound)
            }
            return compound
        }

        override fun readFromNBT(compound: NBTTagCompound) {
            superReadFromNBT(compound)
            self.world.onServer {
                self.data.getServer().readFromNbtServerSide(compound)
            }
        }
    }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER", "DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
abstract class PrefabTileEntitySided<T, S, C> : TileEntity(), TileEntitySided<S, C>,
    TileEntitySidedIfc<S, C, PrefabTileEntitySided<T, S, C>> by TileEntitySidedIfc.Delegate(Magic.getThis())
    where S : T, S : ServerSideSerializable, S : Observable, C : T {

    override val data: Sided<T, S, C> = initData()

    init {
        onServer {
            data.getServer().observeStateGeneric(TileEntityStateObserver(this))
        }
    }

    protected abstract fun initData(): Sided<T, S, C>
}

typealias PrefabTileEntityServerOnly<S> = PrefabTileEntitySided<Any?, S, Unit>
