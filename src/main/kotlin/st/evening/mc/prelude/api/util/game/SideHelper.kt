package st.evening.mc.prelude.api.util.game

import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.Side
import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked
import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.util.data.UnboundGenericProperty
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
@ICRestrictAnnotation(ServerSide.LOGICAL)
annotation class ServerSide {
    companion object {
        const val PHYSICAL: String = "prelude.side.physical.SERVER"
        const val LOGICAL: String = "prelude.side.logical.SERVER"
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
    @ICRestrictAnnotation(LOGICAL, PHYSICAL)
    annotation class Physical
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
@ICRestrictAnnotation(ClientSide.LOGICAL)
annotation class ClientSide {
    companion object {
        const val PHYSICAL: String = "prelude.side.physical.CLIENT"
        const val LOGICAL: String = "prelude.side.logical.CLIENT"
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
    @ICRestrictAnnotation(PHYSICAL)
    annotation class Physical

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
    @ICRestrictAnnotation(PHYSICAL, LOGICAL)
    annotation class Strong
}

@ICRestrict(ServerSide.LOGICAL, ClientSide.LOGICAL)
fun absurdLogicalSide(): Nothing = throw IllegalStateException()

@ICRestrict(ServerSide.PHYSICAL, ClientSide.PHYSICAL)
fun absurdPhysicalSide(): Nothing = throw IllegalStateException()

@RequiresOptIn
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class SidednessAssertion

@SidednessAssertion
@OptIn(ExperimentalContracts::class)
inline fun <T> assertLogicalServer(action: @ServerSide () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return (@ICUnchecked(ServerSide.LOGICAL) action())
}

@SidednessAssertion
@OptIn(ExperimentalContracts::class)
inline fun <T> assertPhysicalServer(action: @ServerSide.Physical () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return (@ICUnchecked(ServerSide.PHYSICAL, ServerSide.LOGICAL) action())
}

@SidednessAssertion
@OptIn(ExperimentalContracts::class)
inline fun <T> assertLogicalClient(action: @ClientSide () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return (@ICUnchecked(ClientSide.LOGICAL) action())
}

@SidednessAssertion
@OptIn(ExperimentalContracts::class)
inline fun <T> assertPhysicalClient(action: @ClientSide.Physical () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return (@ICUnchecked(ClientSide.PHYSICAL) action())
}

@SidednessAssertion
@OptIn(ExperimentalContracts::class)
inline fun <T> assertStrongClient(action: @ClientSide.Strong () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return assertPhysicalClient {
        assertLogicalClient {
            action()
        }
    }
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
fun <T> onPhysicalServer(action: @ServerSide.Physical () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (FMLCommonHandler.instance().side.isServer) assertPhysicalServer(action) else null
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
fun <T> onPhysicalClient(action: @ClientSide.Physical () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (FMLCommonHandler.instance().side.isClient) assertPhysicalClient(action) else null
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
fun <T> physicalSided(onServer: @ServerSide.Physical () -> T, onClient: @ClientSide.Physical () -> T): T {
    contract {
        callsInPlace(onServer, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onClient, InvocationKind.AT_MOST_ONCE)
    }
    return when (FMLCommonHandler.instance().side) {
        Side.SERVER -> assertPhysicalServer(onServer)
        Side.CLIENT -> assertPhysicalClient(onClient)
    }
}

interface SideHelper {
    val logicalSideOrNull: Side?

    companion object : SideHelper by physicalSided({ Server() }, { Client() })

    @ServerSide.Physical
    class Server internal constructor() : SideHelper {
        override val logicalSideOrNull: Side
            get() = Side.SERVER
    }

    @ClientSide.Physical
    class Client internal constructor() : SideHelper {
        override val logicalSideOrNull: Side?
            get() = when (FMLCommonHandler.instance().effectiveSide) {
                Side.CLIENT -> if (Minecraft.getMinecraft().world != null) Side.CLIENT else null
                Side.SERVER -> Side.SERVER
            }
    }
}

val SideHelper.logicalSide: Side
    get() = logicalSideOrNull ?: throw IllegalStateException("Logical side queried while not in-game!")

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun <T> onServer(action: @ServerSide () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    // bit of a footgun here: when not in-game (e.g. on the client title screen), both this and onClient will fail!
    // this is useful behaviour when we need to check whether we're in-game (e.g. when getting item tooltips), but it
    // might be confusing in other situations where we expect a clean partitioning of game states
    return if (SideHelper.logicalSideOrNull == Side.SERVER) assertLogicalServer(action) else null
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun <T> onClient(action: @ClientSide () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (SideHelper.logicalSideOrNull == Side.CLIENT) assertLogicalClient(action) else null // see above^
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
fun <T> onStrongClient(action: @ClientSide.Strong () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (SideHelper.logicalSideOrNull == Side.CLIENT) assertStrongClient(action) else null // see above^
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun <T> sided(onServer: @ServerSide () -> T, onClient: @ClientSide () -> T): T {
    contract {
        callsInPlace(onServer, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onClient, InvocationKind.AT_MOST_ONCE)
    }
    return when (SideHelper.logicalSide) {
        Side.SERVER -> assertLogicalServer(onServer)
        Side.CLIENT -> assertLogicalClient(onClient)
    }
}

@OptIn(ExperimentalContracts::class, SidednessAssertion::class)
inline fun <T> sidedStrong(onServer: @ServerSide () -> T, noinline onClient: @ClientSide.Strong () -> T): T {
    contract {
        callsInPlace(onServer, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onClient, InvocationKind.AT_MOST_ONCE)
    }
    return when (SideHelper.logicalSide) {
        Side.SERVER -> assertLogicalServer(onServer)
        Side.CLIENT -> assertStrongClient(onClient)
    }
}

interface Sided<out T, out S : T, out C : T> : UnboundGenericProperty<T> {
    override fun getPropertyValue(): T = get()

    fun get(): T

    @Suppress("UNCHECKED_CAST")
    @ServerSide
    fun getServer(): S

    @Suppress("UNCHECKED_CAST")
    @ClientSide
    fun getClient(): C

    companion object {
        inline fun <T : Any, S : T, C : T> of(
            serverFactory: @ServerSide () -> S,
            clientFactory: @ClientSide () -> C
        ): Sided<T, S, C> = sided({ ServerOnly(serverFactory()) }, { ClientOnly(clientFactory()) })

        inline fun <T : Any, S : T, C : T> ofStrong(
            serverFactory: @ServerSide () -> S,
            noinline clientFactory: @ClientSide.Strong () -> C
        ): Sided<T, S, C> = sidedStrong({ ServerOnly(serverFactory()) }, { ClientOnly(clientFactory()) })

        inline fun <S : Any> serverOnly(factory: @ServerSide () -> S): SidedAny<S, Unit> =
            sided({ ServerOnly(factory()) }, { ClientOnly(Unit) })

        inline fun <S : Any> clientOnly(factory: @ClientSide () -> S): SidedAny<Unit, S> =
            sided({ ServerOnly(Unit) }, { ClientOnly(factory()) })
    }

    @ServerSide
    class ServerOnly<T : Any>(private val instance: T) : Sided<T, T, Nothing> {
        override fun get(): T = getServer()

        @ServerSide
        override fun getServer(): T = instance

        @ClientSide
        override fun getClient(): Nothing = absurdLogicalSide()
    }

    @ClientSide
    class ClientOnly<T : Any>(private val instance: T) : Sided<T, Nothing, T> {
        override fun get(): T = getClient()

        @ServerSide
        override fun getServer(): Nothing = absurdLogicalSide()

        @ClientSide
        override fun getClient(): T = instance
    }
}

typealias SidedAny<S, C> = Sided<Any, S, C>

enum class Sides(val test: (Side) -> Boolean) {
    CLIENT({ it.isClient }),
    SERVER({ it.isServer }),
    BOTH({ true });

    companion object {
        val serializer: EnumSerializer<Sides> = EnumSerializer()
    }
}
