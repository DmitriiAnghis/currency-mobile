package md.anghis.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ConversionLogTest {
    private val now = LocalDateTime.of(2026, 11, 7, 14, 30, 15)
    private val rateDate = LocalDate.of(2026, 11, 6)

    private fun ConversionLog.put(amount: String, from: String = "EUR", to: String = "MDL",
                                  result: String = "1945.23", cache: Boolean = false) =
        add(BigDecimal(amount), from, to, BigDecimal(result), rateDate, cache, now)

    @Test
    fun `новые записи сверху, время без секунд`() {
        val log = ConversionLog(MemoryLogStorage())
        log.put("100")
        val second = log.put("10", "USD", "EUR", "9.18")
        assertEquals(listOf(2L, 1L), log.entries.map { it.id })
        assertEquals("07.11.2026 14:30", second.timeText)
        assertEquals("100 EUR = 1945.23 MDL", log.entries[1].text)
        assertEquals("BNM", log.entries[1].source)
    }

    @Test
    fun `журнал переживает перезапуск`() {
        val storage = MemoryLogStorage()
        val log = ConversionLog(storage)
        log.put("100")
        log.put("12.5", "GBP", "USD", "16.19", cache = true)
        val again = ConversionLog(storage)
        assertEquals(log.entries, again.entries)
        assertEquals("кэш", again.entries[0].source)
        assertEquals(0, again.skippedLines)
    }

    @Test
    fun `повреждённые строки пропускаются и считаются`() {
        val good = ConversionLog.encode(LogEntry(5, now.withSecond(0), BigDecimal("1"), "EUR", "MDL",
            BigDecimal("19.45"), rateDate, false))
        val storage = MemoryLogStorage("мусор\n$good\n1;2;3\n9;bad;1;EUR;MDL;1;2026-11-06;bnm\n")
        val log = ConversionLog(storage)
        assertEquals(1, log.size)
        assertEquals(3, log.skippedLines)
        assertEquals(6L, log.put("2").id)
    }

    @Test
    fun `неизвестный источник считается повреждением`() {
        assertNull(ConversionLog.decode("1;2026-11-07T14:30;1;EUR;MDL;1;2026-11-06;internet"))
    }

    @Test
    fun `удаление одной записи`() {
        val storage = MemoryLogStorage()
        val log = ConversionLog(storage)
        log.put("1"); log.put("2"); log.put("3")
        assertTrue(log.delete(2))
        assertFalse(log.delete(42))
        assertEquals(listOf(3L, 1L), ConversionLog(storage).entries.map { it.id })
    }

    @Test
    fun `удалённую запись можно вернуть на место`() {
        val storage = MemoryLogStorage()
        val log = ConversionLog(storage)
        log.put("1"); val middle = log.put("2"); log.put("3")
        log.delete(middle.id)
        log.restore(middle)
        log.restore(middle) // повторно не дублируется
        assertEquals(listOf(3L, 2L, 1L), ConversionLog(storage).entries.map { it.id })
    }

    @Test
    fun `очистка удаляет всё`() {
        val storage = MemoryLogStorage()
        val log = ConversionLog(storage)
        log.put("1"); log.put("2")
        assertEquals(2, log.clear())
        assertEquals(0, ConversionLog(storage).size)
    }

    @Test
    fun `лимит записей убирает самые старые`() {
        val log = ConversionLog(MemoryLogStorage(), maxEntries = 3)
        listOf("1", "2", "3", "4").forEach { log.put(it) }
        assertEquals(listOf("4", "3", "2"), log.entries.map { it.amount.toPlainString() })
    }

    @Test
    fun `поиск по валюте, сумме, дате и источнику`() {
        val log = ConversionLog(MemoryLogStorage())
        log.put("100")
        log.put("7", "USD", "RON", "30.12", cache = true)
        assertEquals(listOf("7"), log.search("usd").map { it.amount.toPlainString() })
        assertEquals(listOf("100"), log.search("eur  MDL").map { it.amount.toPlainString() })
        assertEquals(2, log.search("06.11.2026").size)
        assertEquals(1, log.search("кэш").size)
        assertEquals(2, log.search("  ").size)
        assertTrue(log.search("gbp").isEmpty())
    }

    @Test
    fun `get находит запись по id`() {
        val log = ConversionLog(MemoryLogStorage())
        val e = log.put("1")
        assertEquals(e, log.get(e.id))
        assertNull(log.get(99))
    }

    @Test
    fun `CSV журнала для Excel`() {
        val log = ConversionLog(MemoryLogStorage())
        log.put("100")
        val csv = ConversionLog.toCsv(log.entries).split("\r\n")
        assertEquals(ConversionLog.CSV_HEADER, csv[0])
        assertEquals("07.11.2026 14:30;100;EUR;1945.23;MDL;06.11.2026;BNM", csv[1])
    }

    @Test
    fun `таблица курсов - популярные сверху и поиск`() {
        val rates = BnmXmlParser.parse(bnmXml("06.11.2026"), LocalDate.of(2026, 11, 6))
        assertEquals(listOf("EUR", "USD", "RUB"), RatesTable.rows(rates).map { it.code })
        assertEquals(listOf("USD"), RatesTable.rows(rates, "dollar").map { it.code })
        assertEquals(listOf("RUB"), RatesTable.rows(rates, " rub ").map { it.code })
    }

    @Test
    fun `CSV курсов с курсом за единицу`() {
        val rates = BnmXmlParser.parse(bnmXml("06.11.2026"), LocalDate.of(2026, 11, 6))
        val lines = RatesTable.toCsv(rates).trim().split("\r\n")
        assertEquals("Код;Валюта;Номинал;MDL;За 1 ед.;Дата", lines[0])
        assertEquals("RUB;Russian Ruble;100;21.4567;0.2146;06.11.2026", lines[3])
    }
}
