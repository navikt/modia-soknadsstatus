package no.nav.modia.soknadsstatus

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

interface SuspendCache<KEY_TYPE: Any, VALUE_TYPE> {
    suspend fun get(
        key: KEY_TYPE,
        create: suspend CoroutineScope.() -> VALUE_TYPE,
    ): VALUE_TYPE

    fun put(
        key: KEY_TYPE,
        value: VALUE_TYPE,
    )

    fun getIfPresent(key: KEY_TYPE): VALUE_TYPE?

    val size: Int
}

class SuspendCacheImpl<KEY_TYPE: Any, VALUE_TYPE>(
    maximumSize: Long = 10_000,
    expiresAfterWrite: Duration = 1.hours,
    private val ticker: Ticker? = null,
) : SuspendCache<KEY_TYPE, VALUE_TYPE> {
    private val cache: AsyncCache<KEY_TYPE, VALUE_TYPE> =
        Caffeine
            .newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(expiresAfterWrite.toJavaDuration())
            .apply { if (ticker != null) ticker(ticker) }
            .buildAsync()

    override suspend fun get(
        key: KEY_TYPE,
        create: suspend CoroutineScope.() -> VALUE_TYPE,
    ): VALUE_TYPE =
        coroutineScope {
            val fut =
                cache.get(key) { _, _ ->
                    future {
                        create()
                    }
                }
            fut.await()
        }

    override fun put(
        key: KEY_TYPE,
        value: VALUE_TYPE,
    ) = cache.put(key, CompletableFuture.completedFuture(value))

    override fun getIfPresent(key: KEY_TYPE): VALUE_TYPE? = cache.getIfPresent(key)?.get()

    override val size: Int
        get() = cache.asMap().size
}
