package md.anghis.currency.core

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Разбор XML Национального банка Молдовы:
 * `<ValCurs Date="17.10.2026"><Valute><CharCode>EUR</CharCode><Nominal>1</Nominal>
 * <Name>Euro</Name><Value>19.6012</Value></Valute>...</ValCurs>`
 */
object BnmXmlParser {
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /**
     * @param requestedDate дата запроса; используется, если в ответе нет атрибута Date.
     * @throws EmptyResponseException если в ответе нет ни одной валюты.
     * @throws InvalidResponseException если XML повреждён или поля некорректны.
     */
    fun parse(xml: String, requestedDate: LocalDate): DailyRates {
        if (xml.isBlank()) throw EmptyResponseException("Источник вернул пустой ответ")
        val root = try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                // Защита от XXE. Парсер Android эту настройку не поддерживает, там DTD и так не загружаются.
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            }
            factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.trim().toByteArray(Charsets.UTF_8)))
                .documentElement
        } catch (e: Exception) {
            throw InvalidResponseException("Ответ источника не является корректным XML", e)
        }
        if (root.tagName != "ValCurs") throw InvalidResponseException("Неожиданный корневой элемент <${root.tagName}>")

        val date = root.getAttribute("Date").takeIf { it.isNotBlank() }?.let {
            try {
                LocalDate.parse(it, DATE_FORMAT)
            } catch (e: DateTimeParseException) {
                throw InvalidResponseException("Некорректная дата курса: $it", e)
            }
        } ?: requestedDate

        val nodes = root.getElementsByTagName("Valute")
        val rates = LinkedHashMap<String, Rate>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            val code = el.text("CharCode").uppercase()
            val nominal = el.text("Nominal").toIntOrNull()
            val value = el.text("Value").replace(',', '.').toBigDecimalOrNull()
            if (code.length != 3 || nominal == null || nominal <= 0 || value == null || value.signum() <= 0) {
                throw InvalidResponseException("Некорректная запись валюты №${i + 1} ($code)")
            }
            rates[code] = Rate(code, el.text("Name"), nominal, value)
        }
        if (rates.isEmpty()) throw EmptyResponseException("Нет курсов на ${date.format(DATE_FORMAT)}")
        return DailyRates(date, rates)
    }

    private fun Element.text(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()

    private fun String.toBigDecimalOrNull(): BigDecimal? = try {
        BigDecimal(this)
    } catch (e: NumberFormatException) {
        null
    }
}
