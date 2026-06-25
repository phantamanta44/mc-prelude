package st.evening.mc.prelude.api.util.math

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.api.data.ser.FullSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TJsonDsl
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.render.gui.DrawAxis

interface IntRectangle {
    val posX: Int
    val posY: Int
    val width: Int
    val height: Int
}

fun IntRectangle.getPosition(axis: DrawAxis): Int = when (axis) {
    DrawAxis.X -> posX
    DrawAxis.Y -> posY
}

fun IntRectangle.getSize(axis: DrawAxis): Int = when (axis) {
    DrawAxis.X -> width
    DrawAxis.Y -> height
}

fun IntRectangle.containsPoint(x: Int, y: Int): Boolean =
    x >= posX && y >= posY && x < posX + width && y < posY + height

operator fun IntRectangle.contains(pos: Vec2i): Boolean = containsPoint(pos.x, pos.y)

fun IntRectangle.intersects(o: IntRectangle): Boolean {
    val tx = posX
    val ox = o.posX
    if (tx >= ox + o.width || ox >= tx + width) return false
    val ty = posY
    val oy = o.posY
    return !(ty >= oy + o.height || oy >= ty + height)
}

data class Rect2i(
    override val posX: Int,
    override val posY: Int,
    override val width: Int,
    override val height: Int
) : IntRectangle {
    override fun toString(): String = "($posX + $width, $posY + $height)"

    object Serializer : FullSerializer<Rect2i, NBTTagCompound, TJson.Object> {
        private const val SER_X: String = "x"
        private const val SER_Y: String = "y"
        private const val SER_WIDTH: String = "width"
        private const val SER_HEIGHT: String = "height"

        override fun serializeToNbt(x: Rect2i): NBTTagCompound = NbtDsl.compound {
            SER_X int x.posX
            SER_Y int x.posY
            SER_WIDTH int x.width
            SER_HEIGHT int x.height
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): Rect2i =
            Rect2i(dto.getInteger(SER_X), dto.getInteger(SER_Y), dto.getInteger(SER_WIDTH), dto.getInteger(SER_HEIGHT))

        override fun serializeToJson(x: Rect2i): TJson.Object = TJsonDsl.obj {
            SER_X int x.posX
            SER_Y int x.posY
            SER_WIDTH int x.width
            SER_HEIGHT int x.height
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): Rect2i = Rect2i(
            dto.expectIntValue(SER_X),
            dto.expectIntValue(SER_Y),
            dto.expectIntValue(SER_WIDTH),
            dto.expectIntValue(SER_HEIGHT)
        )

        override fun serializeToNetwork(buf: PacketBuffer, x: Rect2i) {
            buf.writeInt(x.posX).writeInt(x.posY).writeInt(x.width).writeInt(x.height)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): Rect2i =
            Rect2i(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt())
    }
}

fun IntRectangle.getBoundingRect(): Rect2i = Rect2i(posX, posY, width, height)
