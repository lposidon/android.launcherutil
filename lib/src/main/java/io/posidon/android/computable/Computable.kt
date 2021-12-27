package io.posidon.android.computable

import java.util.concurrent.Executors

sealed interface Computable<out T> {

    /**
     * Computes a new value, regardless of whether
     * it's already been computed
     *
     * @return The computed value
     */
    fun forceCompute(): T

    /**
     * If called after [offload], and the internal
     * value hasn't been garbage-collected yet,
     * the offloading will be cancelled
     *
     * @return The computed value, or null, if it's
     * been garbage-collected or not computed yet
     *
     * @throws NullPointerException if the value hasn't been computed yet
     */
    fun computed(): T

    /**
     * If called after [offload], and the internal
     * value hasn't been garbage-collected yet,
     * the offloading will be cancelled
     *
     * @return The computed value, or null, if it's
     * been garbage-collected or not computed yet
     */
    fun isComputed(): Boolean

    /**
     * Call this to release the internal value, to be
     * garbage-collected. This might get cancelled
     * if you try to get the value again (to not
     * have to calculate it again)
     */
    fun offload()
}


inline fun <T> Computable(
    noinline valueBuilder: () -> T,
): Computable<T> = ComputableImpl(valueBuilder)

inline fun <T> Computable(
    value: T
): Computable<T> = ConstComputableImpl(value)


/**
 * If called after [offload], and the internal
 * value hasn't been garbage-collected yet,
 * the offloading will be cancelled
 *
 * @param consumer What to do with the
 * computed value
 */
inline fun <T> Computable<T>.compute(crossinline consumer: (T) -> Unit) {
    if (isComputed()) {
        consumer(computed())
    } else computableThreadPool.execute {
        consumer(forceCompute())
    }
}
@PublishedApi
internal val computableThreadPool = Executors.newWorkStealingPool((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1))

/**
 * If called after [offload], and the internal
 * value hasn't been garbage-collected yet,
 * the offloading will be cancelled
 *
 * @return The computed value
 */
inline fun <T> Computable<T>.syncCompute(): T {
    return if (isComputed()) computed() else forceCompute()
}

/**
 * If called after [offload], and the internal
 * value hasn't been garbage-collected yet,
 * the offloading will be cancelled
 *
 * @return The computed value, or null if it's not been computed
 */
inline fun <T> Computable<T>.computedOrNull(): T? {
    return if (isComputed()) computed() else null
}


inline fun <T, C> Computable<T>.copy(
    crossinline valueBuilder: (T) -> C
): Computable<C> = when (this) {
    is ComputableImpl -> Computable {
        valueBuilder(this.valueBuilder())
    }
    is ConstComputableImpl -> Computable {
        valueBuilder(forceCompute())
    }
}