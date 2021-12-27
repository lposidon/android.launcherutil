package io.posidon.android.computable

import java.lang.ref.SoftReference

internal object UNINITIALIZED_VALUE

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal class ComputableImpl<T>(
    val valueBuilder: () -> T,
) : Computable<T> {

    @PublishedApi
    internal var value: Any? = UNINITIALIZED_VALUE

    @Suppress("OVERRIDE_BY_INLINE")
    override inline fun forceCompute(): T = valueBuilder()
        .also { value = it }

    override fun computed(): T {
        value.also { v ->
            when {
                v is SoftReference<*> -> {
                    val vv = v.get()
                    if (vv != null) {
                        return vv.also { value = it } as T
                    }
                }
                v != UNINITIALIZED_VALUE -> return v as T
            }
        }
        throw NullPointerException("Not computed")
    }

    override fun isComputed(): Boolean =
        value.let { v ->
            when {
                v is SoftReference<*> ->
                    v.get()?.also { value = it }.let { it != null }
                v != UNINITIALIZED_VALUE -> true
                else -> false
            }
        }

    override fun offload() {
        value.also { v ->
            if (v is SoftReference<*> || v === UNINITIALIZED_VALUE)
                return
            value = SoftReference(v)
        }
    }
}