package st.evening.mc.prelude.api.util.data

import net.minecraftforge.fml.relauncher.FMLLaunchHandler
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.type.Maybe
import st.evening.mc.prelude.api.type.orNull
import st.evening.mc.prelude.api.util.system.InvokeHelper
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType
import java.util.IdentityHashMap

interface ShadowDelegate<out T> {
    val self: T
}

@PreludeInternal
class DelegateSuperFactory(
    private val shadowInterface: Class<out ShadowDelegate<*>>,
    private val receiverType: Class<*>,
    deobfMethodName: String,
    obfMethodName: String?,
    returnType: Class<*>,
    vararg paramTypes: Class<*>
) {
    private val methodName: String = obfMethodName?.let {
        if (FMLLaunchHandler.isDeobfuscatedEnvironment()) deobfMethodName else it
    } ?: deobfMethodName
    private val methodType: MethodType = MethodType.methodType(returnType, paramTypes)
    private val cache: MutableMap<Class<*>, Maybe<MethodHandle>> = IdentityHashMap()

    fun get(callerClass: Class<*>): MethodHandle? {
        cache[callerClass]?.let { return it.orNull() }
        if (isHostClass(callerClass)) {
            return newInvoker(callerClass).also { cache[callerClass] = Maybe.fromNullable(it) }
        }
        var hostClass = callerClass.superclass
        while (true) {
            require(hostClass != null) {
                "Caller ${callerClass.name} does not implement shadow interface ${shadowInterface.name}!"
            }
            if (isHostClass(hostClass)) break
            hostClass = hostClass.superclass
        }
        cache[hostClass]?.let {
            cache[callerClass] = it
            return it.orNull()
        }
        val invoker = newInvoker(hostClass)
        val cacheEntry = Maybe.fromNullable(invoker)
        cache[hostClass] = cacheEntry
        cache[callerClass] = cacheEntry
        return invoker
    }

    private fun isHostClass(clazz: Class<*>): Boolean = clazz.interfaces.contains(shadowInterface)

    private fun newInvoker(hostClass: Class<*>): MethodHandle? {
        val lookup = InvokeHelper.getLookupForCaller(hostClass)
        return try {
            lookup.findSpecial(receiverType, methodName, methodType, hostClass)
        } catch (_: IllegalAccessException) { // probably because supermethod is abstract
            null
        }
    }
}

@PreludeInternal
fun DelegateSuperFactory.getAndBind(receiver: Any): MethodHandle? = get(receiver.javaClass)?.bindTo(receiver)
