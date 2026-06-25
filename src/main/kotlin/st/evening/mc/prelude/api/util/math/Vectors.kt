package st.evening.mc.prelude.api.util.math

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import st.evening.mc.prelude.api.data.ser.FullSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TJsonDsl
import st.evening.mc.prelude.api.data.tjson.expectDoubleValue
import st.evening.mc.prelude.api.data.tjson.expectFloatValue
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.util.data.NbtDsl
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val SER_X: String = "x"
private const val SER_Y: String = "y"
private const val SER_Z: String = "z"
private const val SER_W: String = "w"

object Vectors {
    val POS_X_3I: Vec3i = Vec3i(1, 0, 0)
    val NEG_X_3I: Vec3i = Vec3i(-1, 0, 0)
    val POS_Y_3I: Vec3i = Vec3i(0, 1, 0)
    val NEG_Y_3I: Vec3i = Vec3i(0, -1, 0)
    val POS_Z_3I: Vec3i = Vec3i(0, 0, 1)
    val NEG_Z_3I: Vec3i = Vec3i(0, 0, -1)

    val POS_X_3D: Vec3d = Vec3d(1.0, 0.0, 0.0)
    val NEG_X_3D: Vec3d = Vec3d(-1.0, 0.0, 0.0)
    val POS_Y_3D: Vec3d = Vec3d(0.0, 1.0, 0.0)
    val NEG_Y_3D: Vec3d = Vec3d(0.0, -1.0, 0.0)
    val POS_Z_3D: Vec3d = Vec3d(0.0, 0.0, 1.0)
    val NEG_Z_3D: Vec3d = Vec3d(0.0, 0.0, -1.0)
}

data class Vec2i(val x: Int, val y: Int) {
    companion object {
        val ZERO: Vec2i = Vec2i(0, 0)
        val POS_X: Vec2i = Vec2i(1, 0)
        val NEG_X: Vec2i = Vec2i(-1, 0)
        val POS_Y: Vec2i = Vec2i(0, 1)
        val NEG_Y: Vec2i = Vec2i(0, -1)
    }

    operator fun plus(o: Vec2i): Vec2i = Vec2i(x + o.x, y + o.y)

    operator fun minus(o: Vec2i): Vec2i = Vec2i(x - o.x, y - o.y)

    operator fun times(s: Int): Vec2i = Vec2i(x * s, y * s)

    operator fun div(s: Int): Vec2i = Vec2i(x / s, y / s)

    operator fun unaryMinus(): Vec2i = Vec2i(-x, -y)

    infix fun dot(o: Vec2i): Int = x * o.x + y * o.y

    fun getMagnitude(): Float = sqrt((this dot this).toFloat())

    fun toFloat(): Vec2f = Vec2f(x.toFloat(), y.toFloat())

    override fun toString(): String = "($x, $y)"

    object Serializer : FullSerializer<Vec2i, NBTTagCompound, TJson.Object> {
        override fun serializeToNbt(x: Vec2i): NBTTagCompound = NbtDsl.compound {
            SER_X int x.x
            SER_Y int x.y
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): Vec2i =
            Vec2i(dto.getInteger(SER_X), dto.getInteger(SER_Y))

        override fun serializeToJson(x: Vec2i): TJson.Object = TJsonDsl.obj {
            SER_X int x.x
            SER_Y int x.y
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): Vec2i =
            Vec2i(dto.expectIntValue(SER_X), dto.expectIntValue(SER_Y))

        override fun serializeToNetwork(buf: PacketBuffer, x: Vec2i) {
            buf.writeInt(x.x).writeInt(x.y)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): Vec2i = Vec2i(buf.readInt(), buf.readInt())
    }
}

data class Vec2f(val x: Float, val y: Float) { // the minecraft Vec2f class is client-only
    companion object {
        val ZERO: Vec2f = Vec2f(0F, 0F)
        val POS_X: Vec2f = Vec2f(1F, 0F)
        val NEG_X: Vec2f = Vec2f(-1F, 0F)
        val POS_Y: Vec2f = Vec2f(0F, 1F)
        val NEG_Y: Vec2f = Vec2f(0F, -1F)
    }

    operator fun plus(o: Vec2f): Vec2f = Vec2f(x + o.x, y + o.y)

    operator fun minus(o: Vec2f): Vec2f = Vec2f(x - o.x, y - o.y)

    operator fun times(s: Float): Vec2f = Vec2f(x * s, y * s)

    operator fun div(s: Float): Vec2f = Vec2f(x / s, y / s)

    operator fun unaryMinus(): Vec2f = Vec2f(-x, -y)

    infix fun dot(o: Vec2f): Float = x * o.x + y * o.y

    fun getMagnitude(): Float = sqrt(this dot this)

    fun normalize(): Vec2f = this / getMagnitude()

    fun toInt(): Vec2i = Vec2i(x.roundToInt(), y.roundToInt())

    override fun toString(): String = "($x, $y)"

    object Serializer : FullSerializer<Vec2f, NBTTagCompound, TJson.Object> {
        override fun serializeToNbt(x: Vec2f): NBTTagCompound = NbtDsl.compound {
            SER_X float x.x
            SER_Y float x.y
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): Vec2f =
            Vec2f(dto.getFloat(SER_X), dto.getFloat(SER_Y))

        override fun serializeToJson(x: Vec2f): TJson.Object = TJsonDsl.obj {
            SER_X float x.x
            SER_Y float x.y
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): Vec2f =
            Vec2f(dto.expectFloatValue(SER_X), dto.expectFloatValue(SER_Y))

        override fun serializeToNetwork(buf: PacketBuffer, x: Vec2f) {
            buf.writeFloat(x.x).writeFloat(x.y)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): Vec2f = Vec2f(buf.readFloat(), buf.readFloat())
    }
}

operator fun Vec3i.plus(o: Vec3i): Vec3i = Vec3i(x + o.x, y + o.y, z + o.z)
operator fun Vec3i.minus(o: Vec3i): Vec3i = Vec3i(x - o.x, y - o.y, z - o.z)
operator fun Vec3i.times(s: Int): Vec3i = Vec3i(x * s, y * s, z * s)
operator fun Vec3i.div(s: Int): Vec3i = Vec3i(x / s, y / s, z / s)
operator fun Vec3i.unaryMinus(): Vec3i = Vec3i(-x, -y, -z)
infix fun Vec3i.dot(o: Vec3i): Int = x * o.x + y * o.y + z * o.z
fun Vec3i.toDouble(): Vec3d = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())

object Vec3iSerializer : FullSerializer<Vec3i, NBTTagCompound, TJson.Object> {
    override fun serializeToNbt(x: Vec3i): NBTTagCompound = NbtDsl.compound {
        SER_X int x.x
        SER_Y int x.y
        SER_Z int x.z
    }

    override fun deserializeFromNbt(dto: NBTTagCompound): Vec3i =
        Vec3i(dto.getInteger(SER_X), dto.getInteger(SER_Y), dto.getInteger(SER_Z))

    override fun serializeToJson(x: Vec3i): TJson.Object = TJsonDsl.obj {
        SER_X int x.x
        SER_Y int x.y
        SER_Z int x.z
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): Vec3i =
        Vec3i(dto.expectIntValue(SER_X), dto.expectIntValue(SER_Y), dto.expectIntValue(SER_Z))

    override fun serializeToNetwork(buf: PacketBuffer, x: Vec3i) {
        buf.writeInt(x.x).writeInt(x.y).writeInt(x.z)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Vec3i = Vec3i(buf.readInt(), buf.readInt(), buf.readInt())
}

operator fun BlockPos.plus(o: Vec3i): BlockPos = add(o)
operator fun BlockPos.minus(o: Vec3i): BlockPos = subtract(o)

object BlockPosSerializer : FullSerializer<BlockPos, NBTTagCompound, TJson.Object> {
    override fun serializeToNbt(x: BlockPos): NBTTagCompound = Vec3iSerializer.serializeToNbt(x)

    override fun deserializeFromNbt(dto: NBTTagCompound): BlockPos =
        BlockPos(dto.getInteger(SER_X), dto.getInteger(SER_Y), dto.getInteger(SER_Z))

    override fun serializeToJson(x: BlockPos): TJson.Object = Vec3iSerializer.serializeToJson(x)

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): BlockPos =
        BlockPos(dto.expectIntValue(SER_X), dto.expectIntValue(SER_Y), dto.expectIntValue(SER_Z))

    override fun serializeToNetwork(buf: PacketBuffer, x: BlockPos) {
        buf.writeLong(x.toLong())
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): BlockPos = BlockPos.fromLong(buf.readLong())
}

operator fun Vec3d.plus(o: Vec3d): Vec3d = add(o)
operator fun Vec3d.minus(o: Vec3d): Vec3d = subtract(o)
operator fun Vec3d.times(s: Double): Vec3d = scale(s)
operator fun Vec3d.div(s: Double): Vec3d = scale(1.0 / s)
operator fun Vec3d.unaryMinus(): Vec3d = scale(-1.0)
infix fun Vec3d.dot(o: Vec3d): Double = dotProduct(o)
infix fun Vec3d.cross(o: Vec3d): Vec3d = crossProduct(o)
fun Vec3d.toInt(): Vec3i = Vec3i(x.roundToInt(), y.roundToInt(), z.roundToInt())

fun Vec3d.findOrthogonal(): Vec3d = when {
    x == 0.0 -> Vectors.POS_X_3D
    y == 0.0 -> Vectors.POS_Y_3D
    z == 0.0 -> Vectors.POS_Z_3D
    else -> Vec3d(x, -y, (y * y - x * x) / z)
}

fun Vec3d.rotate(axis: Vec3d, radians: Float): Vec3d {
    val cos = MathHelper.cos(radians)
    val cocos = 1F - cos
    val sin = MathHelper.sin(radians)
    val dot = this dot axis
    val cross = this cross axis
    return Vec3d(
        cos * x + sin * cross.x + cocos * dot * axis.x,
        cos * y + sin * cross.y + cocos * dot * axis.y,
        cos * z + sin * cross.z + cocos * dot * axis.z
    )
}

fun Vec3d.projectOntoBasis(basis: Vec3d): Vec3d = basis.scale(this dot basis)

fun Vec3d.project(onto: Vec3d): Vec3d = projectOntoBasis(onto.normalize())

object Vec3dSerializer : FullSerializer<Vec3d, NBTTagCompound, TJson.Object> {
    override fun serializeToNbt(x: Vec3d): NBTTagCompound = NbtDsl.compound {
        SER_X double x.x
        SER_Y double x.y
        SER_Z double x.z
    }

    override fun deserializeFromNbt(dto: NBTTagCompound): Vec3d =
        Vec3d(dto.getDouble(SER_X), dto.getDouble(SER_Y), dto.getDouble(SER_Z))

    override fun serializeToJson(x: Vec3d): TJson.Object = TJsonDsl.obj {
        SER_X double x.x
        SER_Y double x.y
        SER_Z double x.z
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): Vec3d =
        Vec3d(dto.expectDoubleValue(SER_X), dto.expectDoubleValue(SER_Y), dto.expectDoubleValue(SER_Z))

    override fun serializeToNetwork(buf: PacketBuffer, x: Vec3d) {
        buf.writeDouble(x.x).writeDouble(x.y).writeDouble(x.z)
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): Vec3d =
        Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
}

data class Vec4f(val x: Float, val y: Float, val z: Float, val w: Float) {
    companion object {
        val ZERO: Vec4f = Vec4f(0F, 0F, 0F, 0F)
        val POS_X: Vec4f = Vec4f(1F, 0F, 0F, 0F)
        val NEG_X: Vec4f = Vec4f(-1F, 0F, 0F, 0F)
        val POS_Y: Vec4f = Vec4f(0F, 1F, 0F, 0F)
        val NEG_Y: Vec4f = Vec4f(0F, -1F, 0F, 0F)
        val POS_Z: Vec4f = Vec4f(0F, 0F, 1F, 0F)
        val NEG_Z: Vec4f = Vec4f(0F, 0F, -1F, 0F)
        val POS_W: Vec4f = Vec4f(0F, 0F, 0F, 1F)
        val NEG_W: Vec4f = Vec4f(0F, 0F, 0F, -1F)
    }

    operator fun plus(o: Vec4f): Vec4f = Vec4f(x + o.x, y + o.y, z + o.z, w + o.w)

    operator fun minus(o: Vec4f): Vec4f = Vec4f(x - o.x, y - o.y, z - o.z, w - o.w)

    operator fun times(s: Float): Vec4f = Vec4f(x * s, y * s, z * s, w * s)

    operator fun div(s: Float): Vec4f = Vec4f(x / s, y / s, z / s, w / s)

    operator fun unaryMinus(): Vec4f = Vec4f(-x, -y, -z, -w)

    infix fun dot(o: Vec4f): Float = x * o.x + y * o.y + z * o.z + w * o.w

    fun getMagnitude(): Float = sqrt(this dot this)

    fun normalize(): Vec4f = this / getMagnitude()

    override fun toString(): String = "($x, $y, $z, $w)"

    object Serializer : FullSerializer<Vec4f, NBTTagCompound, TJson.Object> {
        override fun serializeToNbt(x: Vec4f): NBTTagCompound = NbtDsl.compound {
            SER_X float x.x
            SER_Y float x.y
            SER_Z float x.z
            SER_W float x.w
        }

        override fun deserializeFromNbt(dto: NBTTagCompound): Vec4f =
            Vec4f(dto.getFloat(SER_X), dto.getFloat(SER_Y), dto.getFloat(SER_Z), dto.getFloat(SER_W))

        override fun serializeToJson(x: Vec4f): TJson.Object = TJsonDsl.obj {
            SER_X float x.x
            SER_Y float x.y
            SER_Z float x.z
            SER_W float x.w
        }

        context(_: JsonPath)
        override fun deserializeFromJson(dto: TJson.Object): Vec4f = Vec4f(
            dto.expectFloatValue(SER_X),
            dto.expectFloatValue(SER_Y),
            dto.expectFloatValue(SER_Z),
            dto.expectFloatValue(SER_W)
        )

        override fun serializeToNetwork(buf: PacketBuffer, x: Vec4f) {
            buf.writeFloat(x.x).writeFloat(x.y).writeFloat(x.z).writeFloat(x.w)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): Vec4f =
            Vec4f(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
    }
}
