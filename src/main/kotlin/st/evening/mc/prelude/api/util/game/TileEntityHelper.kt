package st.evening.mc.prelude.api.util.game

import net.minecraft.tileentity.TileEntity
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.util.collection.WeakValidity

fun TileEntity.getTileEntityWeakValidity(): WeakValidity {
    if (isInvalid) return WeakValidity.WEAK_VALID
    val world = this.world ?: return WeakValidity.WEAK_VALID
    val pos = this.pos ?: return WeakValidity.WEAK_VALID
    if (!world.isBlockLoaded(pos) || world.getTileEntity(pos) !== this) return WeakValidity.WEAK_VALID
    return WeakValidity.VALID
}


class TileEntityStateObserver(private val tileEntity: TileEntity) : Observer.Simple, Observer.Indexed {
    override val weakValidity: WeakValidity
        get() = WeakValidity.VALID

    override fun onObservableUpdate() {
        tileEntity.markDirty()
    }

    override fun onObservableUpdate(index: Int) {
        tileEntity.markDirty()
    }
}
