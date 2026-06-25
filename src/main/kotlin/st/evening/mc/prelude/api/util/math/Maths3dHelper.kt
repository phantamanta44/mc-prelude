package st.evening.mc.prelude.api.util.math

import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min

object Maths3dHelper {
    fun castOntoPlane(rayOrigin: Vec3d, rayDir: Vec3d, planarPoint: Vec3d, planeNormal: Vec3d): Vec3d? {
        val a = planeNormal dot rayDir
        if (a == 0.0) return null
        val scale = planeNormal dot (planarPoint - rayOrigin) / a
        return if (scale > 0) rayOrigin + rayDir.scale(scale) else null
    }

    fun intersectsRay(prism: AxisAlignedBB, lineMin: Vec3d, lineMax: Vec3d): Boolean {
        val dx = lineMax.x - lineMin.x
        val dy = lineMax.y - lineMin.y
        val dz = lineMax.z - lineMin.z

        // project to xy plane
        var d = dy / dx
        var inter1 = (prism.minX - lineMin.x) * d + lineMin.y
        var inter2 = (prism.maxX - lineMin.x) * d + lineMin.y
        if (min(inter1, inter2) > prism.maxY || max(inter1, inter2) < prism.minY) return false

        // project to xz plane
        d = dz / dx
        inter1 = (prism.minX - lineMin.x) * d + lineMin.z
        inter2 = (prism.maxX - lineMin.x) * d + lineMin.z
        if (min(inter1, inter2) > prism.maxZ || max(inter1, inter2) < prism.minZ) return false

        // project to yz plane
        d = dz / dy
        inter1 = (prism.minY - lineMin.y) * d + lineMin.z
        inter2 = (prism.maxY - lineMin.y) * d + lineMin.z
        return !(min(inter1, inter2) > prism.maxZ || max(inter1, inter2) < prism.minZ)
    }
}
