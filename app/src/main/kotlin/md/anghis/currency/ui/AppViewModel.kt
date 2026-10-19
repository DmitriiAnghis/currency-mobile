package md.anghis.currency.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import md.anghis.currency.core.AmountResult
import md.anghis.currency.core.AmountValidator
import md.anghis.currency.core.Converter
import md.anghis.currency.core.DailyRates
import md.anghis.currency.core.PeriodStats
import md.anghis.currency.core.RatePoint
import md.anghis.currency.core.RatesException
import md.anghis.currency.core.RatesRepository
import md.anghis.currency.core.Statistics
import md.anghis.currency.core.fmt
import md.anghis.currency.data.BnmHttpSource
import md.anghis.currency.data.FileRateCache
import java.math.BigDecimal
import java.time.LocalDate

private val POPULAR = listOf("MDL", "EUR", "USD", "RON", "UAH", "RUB", "GBP")

data class ConverterState(
    val loading: Boolean = true,
    val rates: DailyRates? = null,
    val fromCache: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
    val amount: String = "100",
    val from: String = "EUR",
    val to: String = "MDL",
    val result: String? = null,
    val inputError: String? = null,
) {
    val currencies: List<String>
        get() {
            val all = rates?.currencies ?: return POPULAR
            return POPULAR.filter { it in all } + all.filterNot { it in POPULAR }
        }
    val canConvert: Boolean get() = rates != null && amount.isNotBlank() && from.isNotBlank() && to.isNotBlank()
    val sourceLine: String?
        get() = rates?.let {
            if (fromCache) "Источник: сохранённый курс BNM от ${it.date.fmt()}"
            else "Источник: Национальный банк Молдовы, официальный курс на ${it.date.fmt()}"
        }
}

data class HistoryState(
    val days: Int,
    val loading: Boolean = false,
    val from: String = "EUR",
    val to: String = "MDL",
    val points: List<RatePoint> = emptyList(),
    val stats: PeriodStats? = null,
    val notice: String? = null,
    val error: String? = null,
    val loadedFor: Triple<String, String, LocalDate>? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RatesRepository(BnmHttpSource(), FileRateCache(app.filesDir))

    private val _converter = MutableStateFlow(ConverterState())
    val converter: StateFlow<ConverterState> = _converter.asStateFlow()

    private val _week = MutableStateFlow(HistoryState(days = 7))
    val week: StateFlow<HistoryState> = _week.asStateFlow()

    private val _month = MutableStateFlow(HistoryState(days = 30))
    val month: StateFlow<HistoryState> = _month.asStateFlow()

    init {
        refreshRates()
    }

    fun refreshRates() {
        _converter.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.ratesFor(LocalDate.now())
                _converter.update {
                    it.copy(loading = false, rates = result.rates, fromCache = result.fromCache,
                        notice = result.notice, error = null)
                }
            } catch (e: RatesException) {
                _converter.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun onAmountChange(value: String) = _converter.update { it.copy(amount = value, inputError = null, result = null) }
    fun onFromChange(code: String) = _converter.update { it.copy(from = code, result = null) }
    fun onToChange(code: String) = _converter.update { it.copy(to = code, result = null) }
    fun swap() = _converter.update { it.copy(from = it.to, to = it.from, result = null) }

    fun convert() {
        val state = _converter.value
        val rates = state.rates ?: return
        when (val parsed = AmountValidator.validate(state.amount)) {
            is AmountResult.Invalid -> _converter.update { it.copy(inputError = parsed.message, result = null) }
            is AmountResult.Valid -> {
                val value = Converter.convert(parsed.amount, state.from, state.to, rates)
                val rate = Converter.rate(state.from, state.to, rates)
                val reverse = Converter.rate(state.to, state.from, rates)
                _converter.update {
                    it.copy(
                        inputError = null,
                        result = "${parsed.amount.plain()} ${state.from} = ${value.plain()} ${state.to}\n" +
                            "1 ${state.from} = ${rate.short()} ${state.to} · 1 ${state.to} = ${reverse.short()} ${state.from}",
                    )
                }
            }
        }
    }

    private fun flowFor(days: Int) = if (days == 7) _week else _month

    fun onHistoryPair(days: Int, from: String? = null, to: String? = null) {
        flowFor(days).update { it.copy(from = from ?: it.from, to = to ?: it.to) }
        loadHistory(days, force = true)
    }

    fun loadHistory(days: Int, force: Boolean = false) {
        val flow = flowFor(days)
        val current = flow.value
        val today = LocalDate.now()
        val key = Triple(current.from, current.to, today)
        if (!force && current.loadedFor == key) return
        flow.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val history = repository.history(current.from, current.to, days, today)
                val notice = buildList {
                    if (history.usedCacheOffline) add("Нет сети: показаны сохранённые данные, последние от ${history.points.lastOrNull()?.date?.fmt() ?: "—"}")
                    if (history.missingDays > 0) add("Нет данных за ${history.missingDays} раб. дн.")
                }.joinToString(". ").ifEmpty { null }
                flow.update {
                    it.copy(loading = false, points = history.points, stats = Statistics.compute(history.points),
                        notice = notice, error = if (history.points.isEmpty()) "Нет данных за период" else null,
                        loadedFor = key)
                }
            } catch (e: Exception) {
                flow.update { it.copy(loading = false, error = "Не удалось загрузить историю: ${e.message}") }
            }
        }
    }
}

fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()
fun BigDecimal.short(): String = setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
