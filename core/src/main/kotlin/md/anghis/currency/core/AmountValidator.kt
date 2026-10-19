package md.anghis.currency.core

import java.math.BigDecimal

sealed interface AmountResult {
    data class Valid(val amount: BigDecimal) : AmountResult
    data class Invalid(val message: String) : AmountResult
}

object AmountValidator {
    private val NUMBER = Regex("""\d+([.,]\d+)?""")
    private val MAX = BigDecimal("1000000000000")

    fun validate(input: String): AmountResult {
        val text = input.trim().replace(" ", "").replace(" ", "")
        return when {
            text.isEmpty() -> AmountResult.Invalid("Введите сумму")
            text.startsWith("-") -> AmountResult.Invalid("Сумма не может быть отрицательной")
            !NUMBER.matches(text) -> AmountResult.Invalid("Сумма должна быть числом, например 100 или 12,50")
            else -> {
                val amount = BigDecimal(text.replace(',', '.'))
                when {
                    amount.signum() == 0 -> AmountResult.Invalid("Сумма должна быть больше нуля")
                    amount > MAX -> AmountResult.Invalid("Слишком большая сумма")
                    else -> AmountResult.Valid(amount)
                }
            }
        }
    }
}
