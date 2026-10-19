package md.anghis.currency.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RatesRepositoryTest {
    private val friday = d("16.10.2026")
    private val sunday = d("18.10.2026")

    @Test
    fun onlineReturnsRatesAndCachesThem() = runTest {
        val cache = MemoryCache()
        val repo = RatesRepository(FakeSource(mapOf(friday to bnmXml("16.10.2026"))), cache)
        val result = repo.ratesFor(friday)
        assertFalse(result.fromCache)
        assertNull(result.notice)
        assertNotNull(cache.load(friday))
    }

    @Test
    fun weekendFallsBackToLastPublishedRate() = runTest {
        val repo = RatesRepository(FakeSource(mapOf(friday to bnmXml("16.10.2026"))), MemoryCache())
        val result = repo.ratesFor(sunday)
        assertEquals(friday, result.rates.date)
        assertTrue(result.notice!!.contains("16.10.2026"))
    }

    @Test
    fun offlineUsesLatestCacheWithDate() = runTest {
        val cache = MemoryCache().apply { save(friday, bnmXml("16.10.2026")) }
        val repo = RatesRepository(FakeSource(online = false), cache)
        val result = repo.ratesFor(sunday)
        assertTrue(result.fromCache)
        assertEquals(friday, result.rates.date)
        assertTrue(result.notice!!.contains("16.10.2026"))
    }

    @Test(expected = NoDataException::class)
    fun offlineWithoutCacheFails() = runTest {
        RatesRepository(FakeSource(online = false), MemoryCache()).ratesFor(sunday)
    }

    @Test
    fun invalidResponseFallsBackToCache() = runTest {
        val cache = MemoryCache().apply { save(friday, bnmXml("16.10.2026")) }
        val repo = RatesRepository(FakeSource(mapOf(sunday to "<html>oops</html>")), cache)
        assertTrue(repo.ratesFor(sunday).fromCache)
    }

    @Test
    fun historySkipsWeekendsAndUsesCacheForPastDays() = runTest {
        val days = (0L..9L).map { d("09.10.2026").plusDays(it) } // 09.10 (пт) .. 18.10 (вс)
        val responses = days.associateWith { bnmXml(it.fmt(), eur = "19.${it.dayOfMonth}") }
        val cache = MemoryCache().apply { save(d("12.10.2026"), responses.getValue(d("12.10.2026"))) }
        val source = FakeSource(responses)
        val history = RatesRepository(source, cache).history("EUR", "MDL", 10, sunday)

        assertEquals(6, history.points.size) // 9, 12, 13, 14, 15, 16
        assertTrue(history.points.none { it.date.dayOfWeek.value >= 6 })
        assertFalse(d("12.10.2026") in source.requested)
        assertEquals(BigDecimal("19.16"), history.points.last().value.stripTrailingZeros())
        assertEquals(0, history.missingDays)
    }

    @Test
    fun historyOfflineCountsMissingDays() = runTest {
        val cache = MemoryCache().apply { save(friday, bnmXml("16.10.2026")) }
        val history = RatesRepository(FakeSource(online = false), cache).history("EUR", "USD", 7, sunday)
        assertEquals(1, history.points.size)
        assertEquals(4, history.missingDays) // 12–15.10
        assertTrue(history.usedCacheOffline)
    }

    @Test
    fun historyWithNoDataIsEmpty() = runTest {
        val history = RatesRepository(FakeSource(), MemoryCache()).history("EUR", "MDL", 7, sunday)
        assertTrue(history.points.isEmpty())
        assertNull(Statistics.compute(history.points))
    }
}
