package st.evening.mc.prelude.api.util.system

import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor

object InvokeHelper {
    // is this stable across stdlib implementations? seems to work on hotspot and openj9
    private val cLookup: Constructor<MethodHandles.Lookup> =
        MethodHandles.Lookup::class.java.getDeclaredConstructor(Class::class.java).asAccessible()

    fun getLookupForCaller(callerClass: Class<*>): MethodHandles.Lookup = cLookup.newInstance(callerClass)
}
