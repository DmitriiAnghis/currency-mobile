package md.anghis.currency.core

import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate

/** Загружает сырой XML BNM на дату. Бросает IOException при проблемах с сетью. */
fun interface RateSource {
    suspend fun fetchXml(date: LocalDate): String
}

/** Локальное хранилище ответов источника, переживает перезапуск приложения. */
interface RateCache {
    fun load(date: LocalDate): String?
    fun save(date: LocalDate, xml: String)
    /** Самый свежий сохранённый ответ. */
    fun latest(): Pair<LocalDate, String>?
}

data class RatesResult(
    val rates: DailyRates,
    /** Данные взяты из кэша, потому что источник недоступен. */
    val fromCache: Boolean,
    /** Пояснение для пользователя: выходной, нет сети и т. п. null, если всё штатно. */
    val notice: String?,
)

data class HistoryResult(
    val points: List<RatePoint>,
    /** Сколько дат не удалось получить ни из сети, ни из кэша. */
    val missingDays: Int,
    /** Хотя бы одна точка взята из кэша из-за ошибки сети. */
    val usedCacheOffline: Boolean,
)

class RatesRepository(
    private val source: RateSource,
    private val cache: RateCache,
    private val maxFallbackDays: Int = 10,
) {
    /**
     * Курсы на [date]. Если на дату курса нет, берётся последний доступный за [maxFallbackDays] дней.
     * Если нет сети, берётся кэш на эту дату или самый свежий кэш.
     */
    suspend fun ratesFor(date: LocalDate): RatesResult {
        var day = date
        var networkError: Exception? = null
        repeat(maxFallbackDays + 1) {
            val cachedSameDay = cache.load(day)?.let { parseOrNull(it, day) }
            if (cachedSameDay != null && day.isBefore(date)) {
                return RatesResult(cachedSameDay, fromCache = false, notice = fallbackNotice(date, cachedSameDay.date))
            }
            try {
                val xml = source.fetchXml(day)
                val rates = BnmXmlParser.parse(xml, day)
                cache.save(rates.date, xml)
                val notice = if (rates.date != date) fallbackNotice(date, rates.date) else null
                return RatesResult(rates, fromCache = false, notice = notice)
            } catch (e: EmptyResponseException) {
                day = day.minusDays(1)
            } catch (e: IOException) {
                networkError = e
                return offline(date, e)
            } catch (e: InvalidResponseException) {
                networkError = e
                return offline(date, e)
            }
        }
        throw NoDataException("Нет курсов за последние $maxFallbackDays дней до ${date.fmt()}", networkError)
    }

    private fun offline(date: LocalDate, cause: Exception): RatesResult {
        val reason = if (cause is IOException) "Нет подключения к источнику" else "Источник вернул некорректные данные"
        val cached = cache.load(date)?.let { parseOrNull(it, date) }
            ?: cache.latest()?.let { (d, xml) -> parseOrNull(xml, d) }
            ?: throw NoDataException("$reason, сохранённых курсов нет", cause)
        return RatesResult(
            cached,
            fromCache = true,
            notice = "$reason. Показан сохранённый курс от ${cached.date.fmt()}",
        )
    }

    /**
     * Курс пары [from]→[to] за последние [days] дней до [today] включительно.
     * Прошлые даты берутся из кэша, недостающие загружаются. Выходные пропускаются:
     * BNM публикует курс только в рабочие дни.
     */
    suspend fun history(from: String, to: String, days: Int, today: LocalDate): HistoryResult {
        val byDate = sortedMapOf<LocalDate, DailyRates>()
        var missing = 0
        var usedCache = false
        var offline = false
        for (offset in (days - 1) downTo 0) {
            val day = today.minusDays(offset.toLong())
            if (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) continue
            val cached = cache.load(day)?.let { parseOrNull(it, day) }
            if (cached != null && day != today) {
                byDate[cached.date] = cached
                continue
            }
            if (offline) {
                if (cached != null) byDate[cached.date] = cached.also { usedCache = true } else missing++
                continue
            }
            try {
                val xml = source.fetchXml(day)
                val rates = BnmXmlParser.parse(xml, day)
                cache.save(rates.date, xml)
                byDate[rates.date] = rates
            } catch (e: EmptyResponseException) {
                // праздник: курса на эту дату нет
            } catch (e: IOException) {
                offline = true // дальше не ждём таймаутов на каждой дате
                if (cached != null) byDate[cached.date] = cached.also { usedCache = true } else missing++
            } catch (e: InvalidResponseException) {
                if (cached != null) byDate[cached.date] = cached.also { usedCache = true } else missing++
            }
        }
        val points = byDate.values.mapNotNull { rates ->
            runCatching { RatePoint(rates.date, Converter.rate(from, to, rates)) }.getOrNull()
        }
        return HistoryResult(points, missing, usedCache || (offline && points.isNotEmpty()))
    }

    private fun parseOrNull(xml: String, date: LocalDate): DailyRates? =
        try {
            BnmXmlParser.parse(xml, date)
        } catch (e: RatesException) {
            null
        }

    private fun fallbackNotice(requested: LocalDate, actual: LocalDate) =
        "На ${requested.fmt()} курс не опубликован (выходной или праздник). Используется последний курс от ${actual.fmt()}"
}

fun LocalDate.fmt(): String = format(BnmXmlParser.DATE_FORMAT)
