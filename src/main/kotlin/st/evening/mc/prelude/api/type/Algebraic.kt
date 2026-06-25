package st.evening.mc.prelude.api.type

import java.util.Collections

@JvmInline
value class Box<T>(val value: T) : Ap<Box.T, T> {
    object T : TT
}

fun <T> Ap<Box.T, T>.unap(): T = (this as Box<T>).value

sealed interface Maybe<T> {
    companion object {
        fun <T : Any> fromNullable(value: T?): Maybe<T> = if (value != null) Just(value) else Nothing()
    }

    data class Just<T>(val value: T) : Maybe<T> {
        override fun toString(): String = "Just($value)"
    }

    object Nothing : Maybe<Any> {
        // technically not sound, but should work under erasure
        @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
        inline operator fun <T> invoke(): Maybe<T> = this as Maybe<T>

        override fun toString(): String = "Nothing"
    }
}

inline fun <A, B> Maybe<A>.map(f: (A) -> B): Maybe<B> = when (this) {
    is Maybe.Just<A> -> Maybe.Just(f(value))
    is Maybe.Nothing -> Maybe.Nothing()
}

inline fun <A, B> Maybe<A>.flatMap(f: (A) -> Maybe<B>): Maybe<B> = when (this) {
    is Maybe.Just<A> -> f(value)
    is Maybe.Nothing -> Maybe.Nothing()
}

inline fun <O, T : O> Maybe<T>.orElse(f: () -> O): O = when (this) {
    is Maybe.Just<T> -> value
    is Maybe.Nothing -> f()
}

fun <T> Maybe<T>.orNull(): T? = orElse { null }

fun <T> Maybe<T>.box(): Box<T>? = when (this) {
    is Maybe.Just<T> -> Box(value)
    is Maybe.Nothing -> null
}

sealed interface Either<A, B> {
    data class Left<A, B>(val value: A) : Either<A, B> {
        override fun toString(): String = "Left($value)"
    }

    data class Right<A, B>(val value: B) : Either<A, B> {
        override fun toString(): String = "Right($value)"
    }
}

inline fun <A, B, C> Either<A, B>.mapLeft(f: (A) -> C): Either<C, B> = when (this) {
    is Either.Left<A, B> -> Either.Left(f(value))
    is Either.Right<A, B> -> Either.Right(value)
}

inline fun <A, B, D> Either<A, B>.mapRight(f: (B) -> D): Either<A, D> = when (this) {
    is Either.Left<A, B> -> Either.Left(value)
    is Either.Right<A, B> -> Either.Right(f(value))
}

inline fun <A, B, C, D> Either<A, B>.bimap(f: (A) -> C, g: (B) -> D): Either<C, D> = when (this) {
    is Either.Left<A, B> -> Either.Left(f(value))
    is Either.Right<A, B> -> Either.Right(g(value))
}

sealed interface CList<T> : Iterable<T> {
    companion object {
        fun <T> of(value: T): CList<T> = Cons(value, Nil())
    }

    object Nil : CList<Any> {
        // technically not sound, but should work under erasure
        @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
        inline operator fun <T> invoke(): CList<T> = this as CList<T>

        override fun toString(): String = "Nil"

        override fun iterator(): Iterator<Any> = Collections.emptyIterator()
    }

    data class Cons<T>(val value: T, val tail: CList<T>) : CList<T> {
        override fun toString(): String = "$value : $tail"

        override fun iterator(): Iterator<T> = IteratorImpl(this)

        private class IteratorImpl<T>(private var list: Cons<T>?) : Iterator<T> {
            override fun hasNext(): Boolean = list != null

            override fun next(): T {
                val here = list ?: throw NoSuchElementException()
                list = here.tail as? Cons<T>
                return here.value
            }
        }
    }
}

fun <T> CList<T>.reverse(): CList<T> {
    var rev: CList<T> = CList.Nil()
    var fwd = this
    while (true) {
        when (fwd) {
            is CList.Nil -> break
            is CList.Cons<T> -> {
                rev = CList.Cons(fwd.value, rev)
                fwd = fwd.tail
            }
        }
    }
    return rev
}

inline fun <T, A> CList<T>.foldl(initial: A, f: (A, T) -> A): A {
    var acc = initial
    var list = this
    while (true) {
        when (list) {
            is CList.Nil -> break
            is CList.Cons<T> -> {
                acc = f(acc, list.value)
                list = list.tail
            }
        }
    }
    return acc
}

inline fun <T, A> CList<T>.foldr(initial: A, f: (T, A) -> A): A = reverse().foldl(initial) { a, t -> f(t, a) }

inline fun <T> CList<T>.foldl1(f: (T, T) -> T): T? = when (this) {
    is CList.Nil -> null
    is CList.Cons<T> -> tail.foldl(value, f)
}

inline fun <T> CList<T>.foldr1(f: (T, T) -> T): T? = reverse().foldl1 { x, y -> f(y, x) }
