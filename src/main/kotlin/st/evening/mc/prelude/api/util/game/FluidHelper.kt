package st.evening.mc.prelude.api.util.game

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.MathHelper
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.IFluidHandler

fun FluidStack.copyWithAmount(amount: Int): FluidStack? = if (amount == 0) null else copy().also { it.amount = amount }

fun FluidStack.forceCopyWithAmount(amount: Int): FluidStack = copy().also { it.amount = amount }

fun FluidStack.getOrCreateDataTag(): NBTTagCompound = tag ?: NBTTagCompound().also { tag = it }

@ClientSide.Physical
fun FluidStack.getStillSprite(): TextureAtlasSprite =
    Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite(fluid.getStill(this).toString())

@ClientSide.Physical
fun FluidStack.getFlowingSprite(): TextureAtlasSprite =
    Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite(fluid.getFlowing(this).toString())

fun IFluidTank.isEmpty(): Boolean = fluidAmount <= 0

fun IFluidHandler.isFluidHandlerEmpty(): Boolean { // overload ambiguity for objects that are both tanks and handlers
    tankProperties.forEach { tank ->
        val fluid = tank.contents
        if (fluid != null && fluid.amount > 0) return true
    }
    return false
}

fun IFluidTank.computeComparatorValue(): Int {
    val stack = fluid
    return if (stack != null && stack.amount > 0) 1 + 14 * stack.amount / capacity else 0
}

fun IFluidHandler.computeFluidHandlerComparatorValue(): Int {
    var count = 0
    var normFill = 0F
    val tanks = tankProperties
    tanks.forEach { tank ->
        val stack = tank.contents
        if (stack == null || stack.amount <= 0) return@forEach
        count++
        normFill += stack.amount / tank.capacity.toFloat()
    }
    return if (count > 0) 1 + MathHelper.floor(14F * normFill / tankProperties.size) else 0
}
