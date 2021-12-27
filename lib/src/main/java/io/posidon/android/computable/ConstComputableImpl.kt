package io.posidon.android.computable

@JvmInline
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal value class ConstComputableImpl<T>(
    val value: Any?
) : Computable<T> {

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun forceCompute(): T = value as T

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun computed(): T = value as T

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun isComputed() = true

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun offload() {}
}