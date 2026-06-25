package st.evening.mc.prelude.api.util.game

import net.minecraftforge.fml.common.Loader
import st.evening.kt.invokecontrol.ICConstant
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FILE
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@ICRestrictAnnotation("${RequireMod.PERMISSION}.!{modId}")
annotation class RequireMod(val modId: String) {
    companion object {
        const val PERMISSION: String = "prelude.require_mod"
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> ifModLoaded(@ICConstant modId: String, action: @RequireMod("!{modId}") () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (Loader.isModLoaded(modId)) {
        @ICUnchecked("${RequireMod.PERMISSION}.!{modId}")
        action()
    } else {
        null
    }
}

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FILE
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@ICRestrictAnnotation("${RequireMod.PERMISSION}.${RequireJei.MOD_ID}")
annotation class RequireJei {
    companion object {
        const val MOD_ID: String = "jei"
        val LOADED: Boolean = Loader.isModLoaded(MOD_ID)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> ifJeiLoaded(action: @RequireJei () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (RequireJei.LOADED) {
        @ICUnchecked("${RequireMod.PERMISSION}.${RequireJei.MOD_ID}")
        action()
    } else {
        null
    }
}
