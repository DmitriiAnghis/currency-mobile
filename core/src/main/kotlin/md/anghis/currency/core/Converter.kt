package md.anghis.currency.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object Converter {
    /** Сколько единиц [to] стоит одна единица [from]. Кросс-курс считается через MDL. */
    fun rate(from: String, to: String, rates: DailyRates): BigDecimal {
        if (from == to) return BigDecimal.ONE
        val fromMdl = rates.mdlPerUnit(from) ?: throw IllegalArgumentException("Нет курса для $from")
        val toMdl = rates.mdlPerUnit(to) ?: throw IllegalArgumentException("Нет курса для $to")
        return fromMdl.divide(toMdl, MathContext.DECIMAL64)
    }

    /** Переводит [amount] из [from] в [to], результат округлён до копеек. */
    fun convert(amount: BigDecimal, from: String, to: String, rates: DailyRates): BigDecimal =
        amount.multiply(rate(from, to, rates)).setScale(2, RoundingMode.HALF_UP)
}
