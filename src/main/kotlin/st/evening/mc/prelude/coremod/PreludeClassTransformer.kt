package st.evening.mc.prelude.coremod

import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import st.evening.mc.prelude.asm.SafeClassWriter

class PreludeClassTransformer : IClassTransformer {
    companion object {
        private const val ASM_VERSION: Int = Opcodes.ASM5
        private val BLACKLIST_PATTERN: Regex =
            Regex("""^(?:javax?|kotlin|org\.objectweb\.asm|st\.evening\.mc\.prelude\.(?:asm|coremod))\..+""")
        private const val MARKER: String = $$"Lst/evening/mc/prelude/api/util/system/Magic$Use;"
        private const val MAGIC: String = "st/evening/mc/prelude/api/util/system/Magic"
    }

    private val markerVisitor: MarkerVisitor = MarkerVisitor(ASM_VERSION)

    override fun transform(name: String, transformedName: String, bytes: ByteArray?): ByteArray? {
        if (bytes == null) return null
        if (BLACKLIST_PATTERN.matches(name)) return bytes
        try {
            val reader = ClassReader(bytes)
            reader.accept(markerVisitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            if (!markerVisitor.marked) return bytes

            val writer = SafeClassWriter(reader, 0)
            reader.accept(MagicVisitor(ASM_VERSION, writer), 0)
            return writer.toByteArray()
        } catch (e: Exception) {
            System.err.println("Prelude class transformer failed for class: $transformedName")
            e.printStackTrace()
            throw e
        }
    }

    private class MarkerVisitor(api: Int) : ClassVisitor(api) {
        var marked: Boolean = false
            private set

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            marked = false
        }

        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
            if (desc == MARKER) {
                marked = true
            }
            return null
        }
    }

    // ideally, this would be implemented as an intrinsic, but it would suck to maintain an entire kotlin compiler
    // plugin just for that
    private class MagicVisitor(api: Int, downstream: ClassVisitor) : ClassVisitor(api, downstream) {
        override fun visitMethod(
            access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            return MagicMethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions))
        }
    }

    private class MagicMethodVisitor(api: Int, downstream: MethodVisitor) : MethodVisitor(api, downstream) {
        override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
            if (opcode == Opcodes.INVOKESTATIC && owner == MAGIC) {
                when (name) {
                    "getThis" -> super.visitVarInsn(Opcodes.ALOAD, 0)
                    else -> throw IllegalArgumentException(name)
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf)
            }
        }
    }
}
