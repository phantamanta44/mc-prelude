package st.evening.mc.prelude.api.util.world

import net.minecraft.util.Rotation

operator fun Rotation.unaryMinus(): Rotation = when (this) {
    Rotation.NONE -> Rotation.NONE
    Rotation.CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90
    Rotation.CLOCKWISE_180 -> Rotation.CLOCKWISE_180
    Rotation.COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90
}

operator fun Rotation.plus(o: Rotation): Rotation = add(o)

operator fun Rotation.minus(o: Rotation): Rotation = when (this) {
    Rotation.NONE -> -o
    Rotation.CLOCKWISE_90 -> when (o) {
        Rotation.NONE -> Rotation.CLOCKWISE_90
        Rotation.CLOCKWISE_90 -> Rotation.NONE
        Rotation.CLOCKWISE_180 -> Rotation.COUNTERCLOCKWISE_90
        Rotation.COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_180
    }
    Rotation.CLOCKWISE_180 -> when (o) {
        Rotation.NONE -> Rotation.CLOCKWISE_180
        Rotation.CLOCKWISE_90 -> Rotation.CLOCKWISE_90
        Rotation.CLOCKWISE_180 -> Rotation.NONE
        Rotation.COUNTERCLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90
    }
    Rotation.COUNTERCLOCKWISE_90 -> when (o) {
        Rotation.NONE -> Rotation.COUNTERCLOCKWISE_90
        Rotation.CLOCKWISE_90 -> Rotation.CLOCKWISE_180
        Rotation.CLOCKWISE_180 -> Rotation.CLOCKWISE_90
        Rotation.COUNTERCLOCKWISE_90 -> Rotation.NONE
    }
}
