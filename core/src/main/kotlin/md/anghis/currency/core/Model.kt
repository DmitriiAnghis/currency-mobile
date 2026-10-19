package md.anghis.currency.core

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

const val BASE_CURRENCY = "MDL"

/** Официальный курс одной валюты: [nominal] единиц стоят [value] леев. */
data class Rate(
    val code: String,
    val name: String,
    val nominal: Int,
    val value: BigDecimal,
) {
    /** Стоимость одной единицы валюты в MDL. */
    val perUnit: BigDecimal get() = value.divide(BigDecimal(nominal), MathContext.DECIMAL64)
}

/** Курсы BNM на одну дату. */
data class DailyRates(
    val date: LocalDate,
    val rates: Map<String, Rate>,
) {
    val currencies: List<String> get() = listOf(BASE_CURRENCY) + rates.keys.sorted()

    /** Стоимость одной единицы [code] в MDL или null, если валюты нет в курсах. */
    fun mdlPerUnit(code: String): BigDecimal? =
        if (code == BASE_CURRENCY) BigDecimal.ONE else rates[code]?.perUnit
}

data class RatePoint(val date: LocalDate, val value: BigDecimal)

open class RatesException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Источник ответил, но курсов на дату нет (выходной, праздник, дата в будущем). */
class EmptyResponseException(message: String) : RatesException(message)

/** Ответ источника не удалось разобрать. */
class InvalidResponseException(message: String, cause: Throwable? = null) : RatesException(message, cause)

/** Нет ни сети, ни сохранённых данных. */
class NoDataException(message: String, cause: Throwable? = null) : RatesException(message, cause)
