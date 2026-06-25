package st.evening.mc.prelude.asm

import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.net.URL

// based on the safe class writer implementation from tconevo
// https://github.com/phantamanta44/tinkers-evolution/blob/7ce95f70e5b342968d96e4a7aee694c48bddb359/src/main/java/xyz/phanta/tconevo/coremod/util/SafeClassWriter.java
class SafeClassWriter(reader: ClassReader, flags: Int) : ClassWriter(reader, flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String = try {
        if (type1 == type2) type1 else (ClassData.resolve(type1) meet ClassData.resolve(type2))
    } catch (e: Exception) {
        throw RuntimeException("Could not compute meet: $type1 /\\ $type2", e)
    }
}

private class ClassData(val deobfName: String, deobfSuperName: String?) {
    val superClass: ClassData? by lazy { deobfSuperName?.let { resolve(it) } }

    private class ClassDataReader : ClassVisitor(Opcodes.ASM5) {
        private var data: ClassData? = null

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String,
            superName: String?,
            interfaces: Array<out String>
        ) {
            data = ClassData(
                FMLDeobfuscatingRemapper.INSTANCE.map(name),
                superName?.let { FMLDeobfuscatingRemapper.INSTANCE.map(it) }
            )
        }

        fun finish(): ClassData = data ?: throw IllegalStateException("Did not receive class data!")
    }

    companion object {
        private const val SKIP_EVERYTHING: Int =
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES

        // matches prefixes "java/", "javax/", "jdk/", "com/sun/", and "sun/"
        private val loadablePattern: Regex = Regex("""^(?:j(?:avax?|dk)|(?:com/)?sun)/.+""")
        private val cache: MutableMap<String, ClassData> = mutableMapOf()

        fun resolve(className: String): ClassData = cache[className] ?: run {
            val data = doResolve(className)
            check(data.deobfName == className) { "Class $className resolved with mismatching name ${data.deobfName}" }
            cache[className] = data
            return@run data
        }

        private fun doResolve(className: String): ClassData {
            if (className.matches(loadablePattern)) {
                val clazz = Launch.classLoader.loadClass(className.replace('/', '.'))
                return ClassData(clazz.name.replace('.', '/'), clazz.superclass?.name?.replace('.', '/'))
            } else {
                val classUrl = findClass(className)
                val reader = ClassDataReader()
                try {
                    classUrl.openStream().use { ClassReader(it).accept(reader, SKIP_EVERYTHING) }
                } catch (e: Exception) {
                    throw ClassNotFoundException("Failed to read class binary for $className: $classUrl", e)
                }
                return reader.finish()
            }
        }

        private fun findClass(className: String): URL {
            Launch.classLoader.getResource("$className.class")?.let { return it }
            val obfName = FMLDeobfuscatingRemapper.INSTANCE.unmap(className)
            if (obfName == className) {
                throw ClassNotFoundException("Could not find class binary: $className")
            }
            Launch.classLoader.getResource("$obfName.class")?.let { return it }
            throw ClassNotFoundException("Could not find class binary: $className ($obfName)")
        }
    }
}

private infix fun ClassData.meet(o: ClassData): String {
    var a: ClassData? = this
    var b: ClassData? = o
    val aParents = mutableSetOf<String>()
    val bParents = mutableSetOf<String>()
    while (true) {
        var progress = false
        if (a != null) {
            if (a.deobfName in bParents) {
                return a.deobfName
            }
            aParents += a.deobfName
            a = a.superClass
            progress = true
        }
        if (b != null) {
            if (b.deobfName in aParents) {
                return b.deobfName
            }
            bParents += b.deobfName
            b = b.superClass
            progress = true
        }
        if (!progress) {
            return "java/lang/Object"
        }
    }
}
