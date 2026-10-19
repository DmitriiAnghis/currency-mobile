package md.anghis.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ConverterTest {
    private val rates = BnmXmlParser.parse(bnmXml("17.10.2026"), d("17.10.2026"))

    private fun convert(amount: String, from: String, to: String) =
        Converter.convert(BigDecimal(amount), from, to, rates)

    @Test
    fun foreignToMdl() = assertEquals(BigDecimal("1960.12"), convert("100", "EUR", "MDL"))

    @Test
    fun mdlToForeignReverseDirection() = assertEquals(BigDecimal("100.00"), convert("1960.12", "MDL", "EUR"))

    @Test
    fun crossRateThroughMdl() = assertEquals(BigDecimal("115.22"), convert("100", "EUR", "USD"))

    @Test
    fun nominalIsTakenIntoAccount() = assertEquals(BigDecimal("21.46"), convert("100", "RUB", "MDL"))

    @Test
    fun sameCurrencyReturnsAmount() = assertEquals(BigDecimal("42.50"), convert("42.5", "USD", "USD"))

    @Test
    fun roundTripKeepsAmount() {
        val usd = convert("250", "EUR", "USD")
        val back = Converter.convert(usd, "USD", "EUR", rates)
        assertTrue((back - BigDecimal("250")).abs() <= BigDecimal("0.01"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownCurrencyFails() {
        convert("1", "XYZ", "MDL")
    }
}
