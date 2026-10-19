package md.anghis.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class StatisticsTest {
    private fun points(vararg values: String) = values.mapIndexed { i, v ->
        RatePoint(d("01.10.2026").plusDays(i.toLong()), BigDecimal(v))
    }

    @Test
    fun minMaxAverageAndChange() {
        val stats = Statistics.compute(points("19.50", "19.80", "19.40", "20.00"))!!
        assertEquals(BigDecimal("19.40"), stats.min.value)
        assertEquals(d("03.10.2026"), stats.min.date)
        assertEquals(BigDecimal("20.00"), stats.max.value)
        assertEquals(BigDecimal("19.6750"), stats.average)
        assertEquals(BigDecimal("0.5000"), stats.absoluteChange)
        assertEquals(BigDecimal("2.56"), stats.percentChange)
        assertEquals(Trend.RISE, stats.trend)
    }

    @Test
    fun fallingTrendAndNegativePercent() {
        val stats = Statistics.compute(points("20.00", "19.00"))!!
        assertEquals(BigDecimal("-1.0000"), stats.absoluteChange)
        assertEquals(BigDecimal("-5.00"), stats.percentChange)
        assertEquals(Trend.FALL, stats.trend)
    }

    @Test
    fun flatTrendWhenFirstEqualsLast() {
        val stats = Statistics.compute(points("19.5", "19.9", "19.5"))!!
        assertEquals(Trend.FLAT, stats.trend)
        assertEquals(BigDecimal("0.00"), stats.percentChange)
    }

    @Test
    fun unorderedPointsAreSortedByDate() {
        val stats = Statistics.compute(points("10", "20").reversed())!!
        assertEquals(Trend.RISE, stats.trend)
    }

    @Test
    fun singlePoint() {
        val stats = Statistics.compute(points("19.6"))!!
        assertEquals(stats.min, stats.max)
        assertEquals(Trend.FLAT, stats.trend)
    }

    @Test
    fun emptyDataSetGivesNull() = assertNull(Statistics.compute(emptyList()))
}
