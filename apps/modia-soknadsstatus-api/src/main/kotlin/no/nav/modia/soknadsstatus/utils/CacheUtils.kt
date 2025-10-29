package no.nav.modia.soknadsstatus.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

object CacheUtils {
    fun <KEY, VALUE> createCache(
        expireAfterWrite: Duration = 1.hours,
        maximumSize: Long = 10_000,
    ): Cache<KEY, VALUE> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(expireAfterWrite.toJavaDuration())
            .maximumSize(maximumSize)
            .build()
}
