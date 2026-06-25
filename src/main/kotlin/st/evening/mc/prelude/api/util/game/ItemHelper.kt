package st.evening.mc.prelude.api.util.game

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.items.ItemHandlerHelper
import st.evening.mc.prelude.api.data.inbt.ImmutableNbt
import st.evening.mc.prelude.api.util.data.NbtCompoundAction
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.coremod.mixin.ItemStackAccessor


object ItemHelper {
    // accounts for mods that patch the max stack size, e.g. stackup
    val defaultStackCapacity: Int = InvHelper.emptyInventory.inventoryStackLimit
}

fun Item.newStackWithDataNoCopy(
    count: Int, meta: Int, dataTag: NBTTagCompound?, capsTag: NBTTagCompound? = null
): ItemStack = ItemStack(this, count, meta, capsTag).also { stack ->
    dataTag?.let { stack.tagCompound = it }
}

fun Item.newStackWithData(count: Int, meta: Int, dataTag: NBTTagCompound?, capsTag: NBTTagCompound? = null): ItemStack =
    newStackWithDataNoCopy(count, meta, dataTag?.copy(), capsTag?.copy())

fun Item.newStackWithData(count: Int, meta: Int, dataAction: NbtCompoundAction): ItemStack =
    newStackWithDataNoCopy(count, meta, NbtDsl.compound(dataAction), null)

fun Block.newStackWithDataNoCopy(
    count: Int, meta: Int, dataTag: NBTTagCompound?, capsTag: NBTTagCompound? = null
): ItemStack = Item.getItemFromBlock(this).newStackWithDataNoCopy(count, meta, dataTag, capsTag)

fun Block.newStackWithData(
    count: Int, meta: Int, dataTag: NBTTagCompound?, capsTag: NBTTagCompound? = null
): ItemStack = newStackWithDataNoCopy(count, meta, dataTag?.copy(), capsTag?.copy())

fun Block.newStackWithData(count: Int, meta: Int, dataAction: NbtCompoundAction): ItemStack =
    newStackWithDataNoCopy(count, meta, NbtDsl.compound(dataAction), null)

fun ItemStack.copyWithSize(count: Int): ItemStack = ItemHandlerHelper.copyStackWithSize(this, count)

fun ItemStack.isEqual(stack: ItemStack): Boolean = ItemStack.areItemStacksEqual(this, stack)

fun ItemStack.stacksWith(stack: ItemStack): Boolean = ItemHandlerHelper.canItemStacksStack(this, stack)

val ItemStack.dataTagOrNull: NBTTagCompound?
    get() = orNull(hasTagCompound()) { tagCompound }

fun ItemStack.getOrCreateDataTag(): NBTTagCompound =
    if (hasTagCompound()) tagCompound!! else NBTTagCompound().also { tagCompound = it }

@Suppress("CAST_NEVER_SUCCEEDS")
fun ItemStack.hasCapsTag(): Boolean = (this as ItemStackAccessor).`prelude$capabilities`()?.hasCapsTag() ?: false

@Suppress("CAST_NEVER_SUCCEEDS")
fun ItemStack.getCapsTag(): NBTTagCompound? =
    (this as ItemStackAccessor).`prelude$capabilities`()?.serializeNBT()?.takeIf { !it.isEmpty }

@Suppress("CAST_NEVER_SUCCEEDS")
fun ItemStack.areCapsCompatible(capsTag: NBTTagCompound): Boolean =
    (this as ItemStackAccessor).`prelude$capabilities`() areCompatibleNullable capsTag

@Suppress("CAST_NEVER_SUCCEEDS")
fun ItemStack.areCapsCompatible(capsTag: ImmutableNbt.Compound): Boolean =
    (this as ItemStackAccessor).`prelude$capabilities`() areCompatibleNullable capsTag

@ClientSide.Physical
fun ItemStack.getColouredTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
    val iter = getTooltip(Minecraft.getMinecraft().player, tooltipFlags).iterator()
    if (iter.hasNext()) {
        tooltip += "${item.getForgeRarity(this).color}${iter.next()}"
        iter.forEach {
            tooltip += "${TextFormatting.GRAY}$it"
        }
    } else {
        tooltip += "${item.getForgeRarity(this).color}${displayName}"
    }
}
