package st.evening.mc.prelude.api.util.text

import st.evening.mc.prelude.api.util.math.FastMathsHelper
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val SI_PREFIXES: List<String> = listOf("", "k", "M", "G", "T", "P", "E", "Z")
private val SI_PREFIXES_FP: List<String> =
    listOf("f", "p", "n", "\u03bc", "m", "", "k", "M", "G", "T", "P", "E", "Z", "Y")

fun Int.toStringSI(unit: String): String {
    if (this == 0) return "0 $unit"
    val magnitude = FastMathsHelper.floorLog10(absoluteValue) / 3
    if (magnitude == 0) return "$this $unit"
    return "%.2f %s%s".format(this / FastMathsHelper.exp10Int(magnitude * 3).toFloat(), SI_PREFIXES[magnitude], unit)
}

fun Long.toStringSI(unit: String): String {
    if (this == 0L) return "0 $unit"
    val magnitude = FastMathsHelper.floorLog10(absoluteValue) / 3
    if (magnitude == 0) return "$this $unit"
    return "%.2f %s%s".format(this / FastMathsHelper.exp10Long(magnitude * 3).toFloat(), SI_PREFIXES[magnitude], unit)
}

fun Float.toStringSI(unit: String): String {
    val abs = absoluteValue
    if (abs <= 1e-15F) return "0 $unit"
    val magnitude = floor(log10(abs) / 3F).toInt().coerceIn(-5, 8)
    return "%.2f %s%s".format(this / 10F.pow(magnitude * 3), SI_PREFIXES_FP[magnitude + 5], unit)
}

fun Double.toStringSI(unit: String?): String {
    val abs = absoluteValue
    if (abs <= 1e-15) return "0 $unit"
    val magnitude = floor(log10(abs) / 3.0).toInt().coerceIn(-5, 8)
    return "%.2f %s%s".format(this / 10.0.pow(magnitude * 3), SI_PREFIXES_FP[magnitude + 5], unit)
}

fun Float.toStringPercentage(): String {
    if (this <= 0F) return "0%"
    val percent = this * 100F
    if (percent < 1F) {
        return if (percent < 0.01F) "<.01%" else "%.2f%%".format(percent)
    }
    val rounded = percent.roundToInt()
    return if (abs(percent - rounded) <= 0.05F) "$rounded%" else "%.1f%%".format(percent)
}

fun Double.toStringPercentage(): String {
    if (this <= 0.0) return "0%"
    val percent = this * 100.0
    if (percent < 1.0) {
        return if (percent < 0.01) "<.01%" else "%.2f%%".format(percent)
    }
    val rounded = percent.roundToInt()
    return if (abs(percent - rounded) <= 0.05) "$rounded%" else "%.1f%%".format(percent)
}
