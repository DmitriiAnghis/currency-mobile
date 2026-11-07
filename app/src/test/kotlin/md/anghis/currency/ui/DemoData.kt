package md.anghis.currency.ui

import md.anghis.currency.core.ConversionLog
import md.anghis.currency.core.DailyRates
import md.anghis.currency.core.MemoryLogStorage
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
            Rate("HUF", "Hungarian Forint", 100, BigDecimal("4.8765")),
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

    /** Журнал из нескольких конвертаций (часть посчитана по сохранённому курсу). */
    fun journal(): JournalState {
        val log = ConversionLog(MemoryLogStorage())
        val day = today.atTime(9, 0)
        listOf(
            Triple("75", "EUR" to "USD", 0L), Triple("2000", "UAH" to "EUR", 7L),
            Triple("300", "RON" to "MDL", 30L), Triple("12.5", "GBP" to "USD", 31L),
            Triple("50", "USD" to "RON", 62L), Triple("1000", "MDL" to "EUR", 65L),
            Triple("250", "EUR" to "MDL", 70L),
        ).forEachIndexed { i, (amount, pair, minutes) ->
            val a = BigDecimal(amount)
            log.add(a, pair.first, pair.second,
                md.anghis.currency.core.Converter.convert(a, pair.first, pair.second, rates),
                today, fromCache = i in 2..3, time = day.plusMinutes(minutes * 7))
        }
        return JournalState(entries = log.entries)
    }
}

object NoActions : ConverterActions, HistoryActions, JournalActions {
    override fun onAmountChange(value: String) = Unit
    override fun onFromChange(code: String) = Unit
    override fun onToChange(code: String) = Unit
    override fun swap() = Unit
    override fun convert() = Unit
    override fun refreshRates() = Unit
    override fun onHistoryPair(days: Int, from: String?, to: String?) = Unit
    override fun loadHistory(days: Int, force: Boolean) = Unit
    override fun useCurrency(code: String) = Unit
    override fun onJournalQuery(value: String) = Unit
    override fun repeat(id: Long) = Unit
    override fun delete(id: Long) = Unit
    override fun undoDelete() = Unit
    override fun askClear() = Unit
    override fun cancelClear() = Unit
    override fun confirmClear() = Unit
    override fun dismissMessage() = Unit
}
