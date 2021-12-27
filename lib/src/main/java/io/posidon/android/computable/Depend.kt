package io.posidon.android.computable

inline fun <T, C> Computable<T>.dependentUse(
    crossinline valueBuilder: (T) -> C
): Computable<C> = Computable {
    valueBuilder(syncCompute()).also {
        offload()
    }
}

inline fun <T> Computable<T>.dependentUse(): Computable<T> =
    Computable { syncCompute().also { offload() } }

@JvmName("component1p")
inline operator fun <A, B> Computable<Pair<A, B>>.component1(): Computable<A> =
    dependentUse { it.first }

@JvmName("component2p")
inline operator fun <A, B> Computable<Pair<A, B>>.component2(): Computable<B> =
    dependentUse { it.second }

inline operator fun <A, B, C> Computable<Triple<A, B, C>>.component1(): Computable<A> =
    dependentUse { it.first }

inline operator fun <A, B, C> Computable<Triple<A, B, C>>.component2(): Computable<B> =
    dependentUse { it.second }

inline operator fun <A, B, C> Computable<Triple<A, B, C>>.component3(): Computable<C> =
    dependentUse { it.third }