package md.anghis.currency.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class Trend(val label: String) {
    RISE("Рост"),
    FALL("Падение"),
    FLAT("Без изменений"),
}

data class PeriodStats(
    val min: RatePoint,
    val max: RatePoint,
    val average: BigDecimal,
    val first: RatePoint,
    val last: RatePoint,
    /** last - first в единицах курса. */
    val absoluteChange: BigDecimal,
    /** Изменение в процентах относительно первого значения. */
    val percentChange: BigDecimal,
    val trend: Trend,
)

object Statistics {
    /** Изменения меньше этого порога (в единицах курса, после округления до 4 знаков) считаются нулевыми. */
    private const val SCALE = 4

    /** Статистика по точкам за период или null, если данных нет. */
    fun compute(points: List<RatePoint>): PeriodStats? {
        if (points.isEmpty()) return null
        val sorted = points.sortedBy { it.date }
        val first = sorted.first()
        val last = sorted.last()
        val sum = sorted.fold(BigDecimal.ZERO) { acc, p -> acc + p.value }
        val average = sum.divide(BigDecimal(sorted.size), SCALE, RoundingMode.HALF_UP)
        val absolute = (last.value - first.value).setScale(SCALE, RoundingMode.HALF_UP)
        val percent = if (first.value.signum() == 0) {
            BigDecimal.ZERO.setScale(2)
        } else {
            (last.value - first.value).multiply(BigDecimal(100))
                .divide(first.value, MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP)
        }
        val trend = when (absolute.signum()) {
            1 -> Trend.RISE
            -1 -> Trend.FALL
            else -> Trend.FLAT
        }
        return PeriodStats(
            min = sorted.minBy { it.value },
            max = sorted.maxBy { it.value },
            average = average,
            first = first,
            last = last,
            absoluteChange = absolute,
            percentChange = percent,
            trend = trend,
        )
    }
}
