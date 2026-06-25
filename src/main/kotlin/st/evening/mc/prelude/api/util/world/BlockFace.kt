package st.evening.mc.prelude.api.util.world

import net.minecraft.client.resources.I18n
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.IStringSerializable
import net.minecraft.util.Mirror
import net.minecraft.util.Rotation
import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.util.collection.getCyclic
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.mod.PreludeStrings

enum class BlockSide(val face: EnumFacing, val translationKey: String) : IStringSerializable {
    NORTH(EnumFacing.NORTH, PreludeStrings.ENUM_BLOCK_SIDE_NORTH),
    EAST(EnumFacing.EAST, PreludeStrings.ENUM_BLOCK_SIDE_EAST),
    SOUTH(EnumFacing.SOUTH, PreludeStrings.ENUM_BLOCK_SIDE_SOUTH),
    WEST(EnumFacing.WEST, PreludeStrings.ENUM_BLOCK_SIDE_WEST);

    val cw: BlockSide
        get() = entries.getCyclic(ordinal + 1)
    val ccw: BlockSide
        get() = entries.getCyclic(ordinal - 1)
    val opposite: BlockSide
        get() = entries.getCyclic(ordinal + 2)

    override fun getName(): String = name.lowercase()

    @ClientSide.Physical
    fun getLocalizedName(): String = I18n.format(translationKey)

    companion object {
        val serializer: EnumSerializer<BlockSide> = EnumSerializer()

        fun fromFace(face: EnumFacing): BlockSide? = when (face) {
            EnumFacing.NORTH -> NORTH
            EnumFacing.EAST -> EAST
            EnumFacing.SOUTH -> SOUTH
            EnumFacing.WEST -> WEST
            else -> null
        }
    }
}

fun Rotation.rotate(side: BlockSide): BlockSide = when (this) {
    Rotation.NONE -> side
    Rotation.CLOCKWISE_90 -> side.cw
    Rotation.CLOCKWISE_180 -> side.opposite
    Rotation.COUNTERCLOCKWISE_90 -> side.ccw
}

fun Mirror.mirror(side: BlockSide): BlockSide = when (this) {
    Mirror.NONE -> side

    Mirror.LEFT_RIGHT -> when (side) {
        BlockSide.NORTH -> BlockSide.SOUTH
        BlockSide.SOUTH -> BlockSide.NORTH
        else -> side
    }

    Mirror.FRONT_BACK -> when (side) {
        BlockSide.EAST -> BlockSide.WEST
        BlockSide.WEST -> BlockSide.EAST
        else -> side
    }
}

fun Entity.getFacingSide(): BlockSide = BlockSide.fromFace(horizontalFacing)
    ?: throw IllegalArgumentException("Bad horizontal face: $horizontalFacing")

enum class RelativeFace(val translationKey: String) : IStringSerializable {
    FRONT(PreludeStrings.ENUM_RELATIVE_FACE_FRONT) {
        override fun getFace(front: BlockSide): EnumFacing = front.face
    },
    LEFT(PreludeStrings.ENUM_RELATIVE_FACE_LEFT) {
        override fun getFace(front: BlockSide): EnumFacing = front.cw.face
    },
    BACK(PreludeStrings.ENUM_RELATIVE_FACE_BACK) {
        override fun getFace(front: BlockSide): EnumFacing = front.opposite.face
    },
    RIGHT(PreludeStrings.ENUM_RELATIVE_FACE_RIGHT) {
        override fun getFace(front: BlockSide): EnumFacing = front.ccw.face
    },
    UP(PreludeStrings.ENUM_RELATIVE_FACE_UP) {
        override fun getFace(front: BlockSide): EnumFacing = EnumFacing.UP
    },
    DOWN(PreludeStrings.ENUM_RELATIVE_FACE_DOWN) {
        override fun getFace(front: BlockSide): EnumFacing = EnumFacing.DOWN
    };

    abstract fun getFace(front: BlockSide): EnumFacing

    override fun getName(): String = name.lowercase()

    @ClientSide.Physical
    fun getLocalizedName(): String = I18n.format(translationKey)

    companion object {
        private val CLOCKWISE: Array<RelativeFace> = arrayOf(FRONT, LEFT, BACK, RIGHT)

        val serializer: EnumSerializer<RelativeFace> = EnumSerializer()

        fun fromFace(front: BlockSide, face: EnumFacing): RelativeFace = when (face) {
            EnumFacing.UP -> UP
            EnumFacing.DOWN -> DOWN
            else -> CLOCKWISE.getCyclic(face.horizontalIndex + 2 - front.ordinal)
        }
    }
}
