package md.anghis.currency.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ConverterStateTest {
    @Test
    fun `слово валюта согласуется с числом`() {
        assertEquals("валюта", currencyWord(1))
        assertEquals("валюта", currencyWord(21))
        assertEquals("валюты", currencyWord(3))
        assertEquals("валюты", currencyWord(44))
        assertEquals("валют", currencyWord(5))
        assertEquals("валют", currencyWord(11))
        assertEquals("валют", currencyWord(14))
        assertEquals("валют", currencyWord(112))
    }

    @Test
    fun `шапка при загрузке и без курса`() {
        assertEquals("Загрузка курса BNM…", ConverterState().headerStatus)
        assertEquals("Курс не загружен", ConverterState(loading = false).headerStatus)
    }

    @Test
    fun `шапка показывает источник, дату и число валют`() {
        val state = ConverterState(loading = false, rates = DemoData.rates)
        assertEquals("BNM · курс на 04.11.2026 · 8 валют", state.headerStatus)
        assertEquals("Сохранённый курс от 04.11.2026 · 8 валют", state.copy(fromCache = true).headerStatus)
    }

    @Test
    fun `популярные валюты идут первыми`() {
        val state = ConverterState(rates = DemoData.rates)
        assertEquals(listOf("MDL", "EUR", "USD", "RON", "UAH", "RUB", "GBP", "CHF"), state.currencies)
    }

    @Test
    fun `кнопка активна только когда курсы загружены`() {
        assertFalse(ConverterState().canConvert)
        assertTrue(ConverterState(rates = DemoData.rates).canConvert)
    }

    @Test
    fun `текст результата содержит сумму и оба курса`() {
        val text = formatResult(BigDecimal(100), "EUR", "MDL", DemoData.rates)
        val lines = text.lines()
        assertEquals("100 EUR = 1960.12 MDL", lines[0])
        assertTrue(lines[1].startsWith("1 EUR = 19.6012 MDL · 1 MDL = 0.051"))
    }
}
