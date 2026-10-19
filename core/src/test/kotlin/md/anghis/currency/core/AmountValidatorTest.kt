package md.anghis.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class AmountValidatorTest {
    private fun invalid(input: String) = assertTrue(input, AmountValidator.validate(input) is AmountResult.Invalid)

    @Test
    fun acceptsIntegerAndDecimals() {
        assertEquals(AmountResult.Valid(BigDecimal("100")), AmountValidator.validate("100"))
        assertEquals(AmountResult.Valid(BigDecimal("12.50")), AmountValidator.validate("12,50"))
        assertEquals(AmountResult.Valid(BigDecimal("0.5")), AmountValidator.validate(" 0.5 "))
        assertEquals(AmountResult.Valid(BigDecimal("1000")), AmountValidator.validate("1 000"))
    }

    @Test
    fun rejectsEmpty() {
        invalid("")
        invalid("   ")
    }

    @Test
    fun rejectsNegative() = invalid("-5")

    @Test
    fun rejectsZero() {
        invalid("0")
        invalid("0,00")
    }

    @Test
    fun rejectsLetters() {
        invalid("abc")
        invalid("12a")
        invalid("1.2.3")
    }
}
