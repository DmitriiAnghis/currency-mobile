package md.anghis.currency.core

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Одна запись журнала конвертаций. */
data class LogEntry(
    val id: Long,
    val time: LocalDateTime,
    val amount: BigDecimal,
    val from: String,
    val to: String,
    val result: BigDecimal,
    val rateDate: LocalDate,
    val fromCache: Boolean,
) {
    val timeText: String get() = time.format(TIME_FORMAT)
    val text: String get() = "${amount.stripTrailingZeros().toPlainString()} $from = ${result.toPlainString()} $to"
    val source: String get() = if (fromCache) "кэш" else "BNM"

    /** Поиск без учёта регистра по валюте, сумме, результату и датам; слова через пробел — все должны найтись. */
    fun matches(query: String): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = listOf(timeText, text, rateDate.fmt(), source).joinToString(" ").lowercase()
        return words.all { it in haystack }
    }

    companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}

/** Где хранится текст журнала (файл в приложении, память в тестах). */
interface LogStorage {
    fun read(): String?
    fun write(text: String)
}

class MemoryLogStorage(var text: String? = null) : LogStorage {
    override fun read() = text
    override fun write(text: String) {
        this.text = text
    }
}

/**
 * Журнал конвертаций: новые записи сверху, не больше [maxEntries].
 * Хранится построчно: id;время;сумма;из;в;результат;дата курса;cache|bnm.
 */
class ConversionLog(private val storage: LogStorage, private val maxEntries: Int = 300) {
    private val items = mutableListOf<LogEntry>()

    /** Сколько строк файла не удалось прочитать (повреждённые строки пропускаются). */
    var skippedLines: Int = 0
        private set

    init {
        storage.read()?.lineSequence()?.filter { it.isNotBlank() }?.forEach { line ->
            val entry = decode(line)
            if (entry != null) items += entry else skippedLines++
        }
        items.sortByDescending { it.id }
    }

    val entries: List<LogEntry> get() = items.toList()
    val size: Int get() = items.size

    fun get(id: Long): LogEntry? = items.firstOrNull { it.id == id }

    fun search(query: String): List<LogEntry> = items.filter { it.matches(query) }

    fun add(
        amount: BigDecimal,
        from: String,
        to: String,
        result: BigDecimal,
        rateDate: LocalDate,
        fromCache: Boolean,
        time: LocalDateTime = LocalDateTime.now(),
    ): LogEntry {
        val entry = LogEntry((items.maxOfOrNull { it.id } ?: 0) + 1, time.withSecond(0).withNano(0),
            amount, from, to, result, rateDate, fromCache)
        items.add(0, entry)
        while (items.size > maxEntries) items.removeAt(items.lastIndex)
        save()
        return entry
    }

    fun delete(id: Long): Boolean {
        val removed = items.removeAll { it.id == id }
        if (removed) save()
        return removed
    }

    /** Возвращает удалённую запись на место (кнопка «Вернуть» в полосе после удаления). */
    fun restore(entry: LogEntry) {
        if (items.any { it.id == entry.id }) return
        items += entry
        items.sortByDescending { it.id }
        save()
    }

    fun clear(): Int {
        val count = items.size
        items.clear()
        save()
        return count
    }

    private fun save() = storage.write(items.joinToString("\n") { encode(it) })

    companion object {
        const val CSV_HEADER = "Время;Сумма;Из;Результат;В;Дата курса;Источник"

        fun encode(e: LogEntry): String = listOf(
            e.id, e.time, e.amount.toPlainString(), e.from, e.to, e.result.toPlainString(), e.rateDate,
            if (e.fromCache) "cache" else "bnm",
        ).joinToString(";")

        fun decode(line: String): LogEntry? = runCatching {
            val p = line.trim().split(";")
            require(p.size == 8)
            LogEntry(p[0].toLong(), LocalDateTime.parse(p[1]), BigDecimal(p[2]), p[3], p[4], BigDecimal(p[5]),
                LocalDate.parse(p[6]), when (p[7]) {
                    "cache" -> true
                    "bnm" -> false
                    else -> error("source")
                })
        }.getOrNull()

        /** CSV для Excel: разделитель «;», десятичная запятая не нужна — суммы как есть. */
        fun toCsv(entries: List<LogEntry>): String = buildString {
            append(CSV_HEADER).append("\r\n")
            entries.forEach { e ->
                append(listOf(e.timeText, e.amount.toPlainString(), e.from, e.result.toPlainString(), e.to,
                    e.rateDate.fmt(), e.source).joinToString(";")).append("\r\n")
            }
        }
    }
}

/** Таблица курсов на вкладке «Курсы»: популярные валюты сверху, поиск по коду и названию. */
object RatesTable {
    val POPULAR = listOf("EUR", "USD", "RON", "UAH", "RUB", "GBP")

    fun rows(rates: DailyRates, query: String = ""): List<Rate> {
        val q = query.trim().lowercase()
        val ordered = POPULAR.mapNotNull { rates.rates[it] } +
            rates.rates.values.filter { it.code !in POPULAR }.sortedBy { it.code }
        return if (q.isEmpty()) ordered
        else ordered.filter { q in it.code.lowercase() || q in it.name.lowercase() }
    }

    fun toCsv(rates: DailyRates): String = buildString {
        append("Код;Валюта;Номинал;MDL;За 1 ед.;Дата").append("\r\n")
        rows(rates).forEach { r ->
            append(listOf(r.code, r.name, r.nominal, r.value.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString(),
                r.perUnit.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString(), rates.date.fmt())
                .joinToString(";")).append("\r\n")
        }
    }
}
