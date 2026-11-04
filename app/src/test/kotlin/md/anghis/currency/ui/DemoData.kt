package md.anghis.currency.ui

import md.anghis.currency.core.DailyRates
import md.anghis.currency.core.Rate
import md.anghis.currency.core.RatePoint
import md.anghis.currency.core.Statistics
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

/** Демонстрационные курсы для скриншотов и тестов (без сети). */
object DemoData {
    val today: LocalDate = LocalDate.of(2026, 11, 4)

    val rates = DailyRates(
        today,
        listOf(
            Rate("EUR", "Euro", 1, BigDecimal("19.6012")),
            Rate("USD", "US Dollar", 1, BigDecimal("17.0123")),
            Rate("RON", "Romanian Leu", 1, BigDecimal("3.9411")),
            Rate("UAH", "Ukrainian Hryvnia", 1, BigDecimal("0.4102")),
            Rate("RUB", "Russian Ruble", 100, BigDecimal("21.4567")),
            Rate("GBP", "Pound Sterling", 1, BigDecimal("22.8930")),
            Rate("CHF", "Swiss Franc", 1, BigDecimal("21.0144")),
        ).associateBy { it.code },
    )

    /** Курс EUR→MDL за [days] дней: рабочие дни, плавная волна с небольшим ростом. */
    fun points(days: Int): List<RatePoint> =
        (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .mapIndexed { i, date ->
                val wave = kotlin.math.sin(i / 2.3) * 0.045 + i * 0.006
                RatePoint(date, BigDecimal(19.52 + wave).setScale(4, java.math.RoundingMode.HALF_UP))
            }

    fun history(days: Int, notice: String? = null): HistoryState {
        val pts = points(days)
        return HistoryState(days = days, points = pts, stats = Statistics.compute(pts), notice = notice)
    }
}

object NoActions : ConverterActions, HistoryActions {
    override fun onAmountChange(value: String) = Unit
    override fun onFromChange(code: String) = Unit
    override fun onToChange(code: String) = Unit
    override fun swap() = Unit
    override fun convert() = Unit
    override fun refreshRates() = Unit
    override fun onHistoryPair(days: Int, from: String?, to: String?) = Unit
    override fun loadHistory(days: Int, force: Boolean) = Unit
}
