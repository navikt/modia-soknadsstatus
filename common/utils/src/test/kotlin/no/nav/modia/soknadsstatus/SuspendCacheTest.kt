package no.nav.modia.soknadsstatus

import com.google.common.testing.FakeTicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_METHOD)
class SuspendCacheTest {
    private val ticker = FakeTicker()
    lateinit var cache: SuspendCache<String, String>

    @BeforeEach
    fun setUp() {
        cache = SuspendCacheImpl(ticker = ticker::read)
    }

    @Test
    fun `skal lagre element i cachen ved put`() {
        cache.put("test", "test_value")
        val value = cache.getIfPresent("test")
        assertEquals("test_value", value)
    }

    @Test
    fun `skal hente data fra annen kilde og lagre om key ikke finnes i cache`() = runBlocking {
        assertEquals(0, cache.size)
        val value = cache.get("test") {
            provideValue("test_value")
        }
        assertEquals("test_value", value)
        val valueFromCache = cache.getIfPresent("test")
        assertEquals("test_value", valueFromCache)
    }
}

private suspend fun provideValue(value: String): String {
    delay(100)
    return value
}
