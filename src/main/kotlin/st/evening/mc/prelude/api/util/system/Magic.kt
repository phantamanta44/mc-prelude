package st.evening.mc.prelude.api.util.system

// see PreludeClassTransformer.MagicVisitor
object Magic {
    @JvmStatic
    fun <T : Any> getThis(): T = throw NotImplementedError()

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    annotation class Use
}
