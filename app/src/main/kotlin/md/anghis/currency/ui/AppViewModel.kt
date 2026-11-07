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
import md.anghis.currency.core.ConversionLog
import md.anghis.currency.core.Converter
import md.anghis.currency.core.DailyRates
import md.anghis.currency.core.LogEntry
import md.anghis.currency.core.PeriodStats
import md.anghis.currency.core.RatePoint
import md.anghis.currency.core.RatesException
import md.anghis.currency.core.RatesRepository
import md.anghis.currency.core.Statistics
import md.anghis.currency.core.fmt
import md.anghis.currency.data.BnmHttpSource
import md.anghis.currency.data.FileLogStorage
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
    val canConvert: Boolean get() = rates != null && from.isNotBlank() && to.isNotBlank()
    /** Строка в тёмной шапке: какие курсы сейчас загружены. */
    val headerStatus: String
        get() {
            val r = rates ?: return if (loading) "Загрузка курса BNM…" else "Курс не загружен"
            val count = r.currencies.size
            val where = if (fromCache) "Сохранённый курс от ${r.date.fmt()}" else "BNM · курс на ${r.date.fmt()}"
            return "$where · $count ${currencyWord(count)}"
        }
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

/** Журнал конвертаций: записи, строка поиска, ждём ли подтверждения очистки, сообщение в полосе. */
data class JournalState(
    val entries: List<LogEntry> = emptyList(),
    val query: String = "",
    val confirmClear: Boolean = false,
    val message: String? = null,
    /** Последняя удалённая запись: её можно вернуть кнопкой в полосе. */
    val undo: LogEntry? = null,
) {
    val shown: List<LogEntry> get() = entries.filter { it.matches(query) }
    val caption: String
        get() = when {
            entries.isEmpty() -> "Пока пусто. Каждая конвертация сохраняется сюда."
            shown.size != entries.size -> "Найдено ${shown.size} из ${entries.size}"
            else -> "${entries.size} ${recordWord(entries.size)}, новые сверху"
        }
}

/** Действия экрана конвертера: экран зависит от интерфейса, а не от ViewModel (удобно для превью и тестов). */
interface ConverterActions {
    fun onAmountChange(value: String)
    fun onFromChange(code: String)
    fun onToChange(code: String)
    fun swap()
    fun convert()
    fun refreshRates()
}

/** Действия вкладок «Курсы» и «Журнал». */
interface JournalActions {
    fun useCurrency(code: String)
    fun onJournalQuery(value: String)
    fun repeat(id: Long)
    fun delete(id: Long)
    fun undoDelete()
    fun askClear()
    fun cancelClear()
    fun confirmClear()
    fun dismissMessage()
}

/** Действия экранов «Неделя» и «Месяц». */
interface HistoryActions {
    fun onHistoryPair(days: Int, from: String? = null, to: String? = null)
    fun loadHistory(days: Int, force: Boolean = false)
}

class AppViewModel(app: Application) : AndroidViewModel(app), ConverterActions, HistoryActions, JournalActions {
    private val repository = RatesRepository(BnmHttpSource(), FileRateCache(app.filesDir))
    private val log = ConversionLog(FileLogStorage(app.filesDir))

    private val _journal = MutableStateFlow(
        JournalState(
            entries = log.entries,
            message = if (log.skippedLines > 0) "Журнал был повреждён: пропущено строк: ${log.skippedLines}" else null,
        ),
    )
    val journal: StateFlow<JournalState> = _journal.asStateFlow()

    private val _converter = MutableStateFlow(ConverterState())
    val converter: StateFlow<ConverterState> = _converter.asStateFlow()

    private val _week = MutableStateFlow(HistoryState(days = 7))
    val week: StateFlow<HistoryState> = _week.asStateFlow()

    private val _month = MutableStateFlow(HistoryState(days = 30))
    val month: StateFlow<HistoryState> = _month.asStateFlow()

    init {
        refreshRates()
    }

    override fun refreshRates() {
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

    override fun onAmountChange(value: String) = _converter.update { it.copy(amount = value, inputError = null, result = null) }
    override fun onFromChange(code: String) = _converter.update { it.copy(from = code, result = null) }
    override fun onToChange(code: String) = _converter.update { it.copy(to = code, result = null) }
    override fun swap() = _converter.update { it.copy(from = it.to, to = it.from, result = null) }

    override fun convert() {
        val state = _converter.value
        val rates = state.rates ?: return
        when (val parsed = AmountValidator.validate(state.amount)) {
            is AmountResult.Invalid -> _converter.update { it.copy(inputError = parsed.message, result = null) }
            is AmountResult.Valid -> {
                _converter.update {
                    it.copy(inputError = null, result = formatResult(parsed.amount, state.from, state.to, rates))
                }
                runCatching {
                    log.add(parsed.amount, state.from, state.to,
                        Converter.convert(parsed.amount, state.from, state.to, rates), rates.date, state.fromCache)
                }
                _journal.update { it.copy(entries = log.entries) }
            }
        }
    }

    override fun useCurrency(code: String) = _converter.update {
        it.copy(from = code, to = if (code == "MDL") "EUR" else "MDL", result = null, inputError = null)
    }

    override fun onJournalQuery(value: String) = _journal.update { it.copy(query = value) }

    override fun repeat(id: Long) {
        val entry = log.get(id) ?: return
        _converter.update {
            it.copy(amount = entry.amount.plain(), from = entry.from, to = entry.to, inputError = null, result = null)
        }
        convert()
    }

    override fun delete(id: Long) {
        val entry = log.get(id) ?: return
        log.delete(id)
        _journal.update {
            it.copy(entries = log.entries, message = "Удалено: ${entry.text}", undo = entry, confirmClear = false)
        }
    }

    override fun undoDelete() {
        val entry = _journal.value.undo ?: return
        log.restore(entry)
        _journal.update { it.copy(entries = log.entries, message = null, undo = null) }
    }

    override fun askClear() = _journal.update { it.copy(confirmClear = it.entries.isNotEmpty(), message = null, undo = null) }
    override fun cancelClear() = _journal.update { it.copy(confirmClear = false) }

    override fun confirmClear() {
        val count = log.clear()
        _journal.update {
            it.copy(entries = log.entries, confirmClear = false, undo = null,
                message = "Журнал очищен: удалено $count ${recordWord(count)}")
        }
    }

    override fun dismissMessage() = _journal.update { it.copy(message = null, undo = null) }

    private fun flowFor(days: Int) = if (days == 7) _week else _month

    override fun onHistoryPair(days: Int, from: String?, to: String?) {
        flowFor(days).update { it.copy(from = from ?: it.from, to = to ?: it.to) }
        loadHistory(days, force = true)
    }

    override fun loadHistory(days: Int, force: Boolean) {
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

/** Текст результата: первая строка — сумма, вторая — прямой и обратный курс. */
fun formatResult(amount: BigDecimal, from: String, to: String, rates: DailyRates): String {
    val value = Converter.convert(amount, from, to, rates)
    val rate = Converter.rate(from, to, rates)
    val reverse = Converter.rate(to, from, rates)
    return "${amount.plain()} $from = ${value.plain()} $to\n" +
        "1 $from = ${rate.short()} $to · 1 $to = ${reverse.short()} $from"
}

/** «1 валюта», «2 валюты», «5 валют». */
fun currencyWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "валют"
        mod10 == 1 -> "валюта"
        mod10 in 2..4 -> "валюты"
        else -> "валют"
    }
}

/** «1 запись», «2 записи», «5 записей». */
fun recordWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "записей"
        mod10 == 1 -> "запись"
        mod10 in 2..4 -> "записи"
        else -> "записей"
    }
}

fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()
fun BigDecimal.short(): String = setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
