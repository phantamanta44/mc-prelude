package st.evening.mc.prelude.api.block.prefab

import net.minecraft.block.Block
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.Mirror
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.util.data.DelegateSuperFactory
import st.evening.mc.prelude.api.util.data.ShadowDelegate
import st.evening.mc.prelude.api.util.data.getAndBind
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.getFacingSide
import st.evening.mc.prelude.api.util.world.mirror
import st.evening.mc.prelude.api.util.world.rotate
import java.lang.invoke.MethodHandle
import java.lang.Float.TYPE as TFloat
import java.lang.Integer.TYPE as TInt

interface BlockSidedIfc : ShadowDelegate<Block> {
    companion object {
        val PROP_FACING: PropertyEnum<BlockSide> = PropertyEnum.create("facing", BlockSide::class.java)
    }

    fun getStateFromMeta(meta: Int): IBlockState = self.defaultState.withProperty(PROP_FACING, BlockSide.entries[meta])

    fun getMetaFromState(state: IBlockState): Int = state.getValue(PROP_FACING).ordinal

    fun withRotation(state: IBlockState, rot: Rotation): IBlockState =
        state.withProperty(PROP_FACING, rot.rotate(state.getValue(PROP_FACING)))

    fun withMirror(state: IBlockState, mirror: Mirror): IBlockState =
        state.withProperty(PROP_FACING, mirror.mirror(state.getValue(PROP_FACING)))

    fun getValidRotations(world: World, pos: BlockPos): Array<EnumFacing>? = EnumFacing.HORIZONTALS

    fun getStateForPlacement(
        world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int,
        placer: EntityLivingBase, hand: EnumHand
    ): IBlockState

    @OptIn(PreludeInternal::class)
    class Delegate(override val self: Block) : BlockSidedIfc {
        companion object {
            private val factoryGetStateForPlacement: DelegateSuperFactory = DelegateSuperFactory(
                BlockSidedIfc::class.java, Block::class.java, "getStateForPlacement", null, IBlockState::class.java,
                World::class.java, BlockPos::class.java, EnumFacing::class.java, TFloat, TFloat, TFloat, TInt,
                EntityLivingBase::class.java, EnumHand::class.java
            )
        }

        private val superGetStateForPlacement: MethodHandle = factoryGetStateForPlacement.getAndBind(self)!!

        override fun getStateForPlacement(
            world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int,
            placer: EntityLivingBase, hand: EnumHand
        ): IBlockState =
            (superGetStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand) as IBlockState)
                .withProperty(PROP_FACING, placer.getFacingSide().opposite)
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
open class PrefabBlockSided(material: Material, mapCol: MapColor) :
    Block(material, mapCol), BlockSidedIfc by BlockSidedIfc.Delegate(Magic.getThis()) {

    constructor(material: Material) : this(material, material.materialMapColor)

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(self, BlockSidedIfc.PROP_FACING)
}
