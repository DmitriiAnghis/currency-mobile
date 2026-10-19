package md.anghis.currency.core

import java.io.IOException
import java.time.LocalDate

fun bnmXml(date: String, eur: String = "19.6012", usd: String = "17.0123", rub: String = "21.4567") = """
<?xml version="1.0" encoding="UTF-8"?>
<ValCurs Date="$date" name="Official exchange rate">
  <Valute ID="47"><NumCode>978</NumCode><CharCode>EUR</CharCode><Nominal>1</Nominal><Name>Euro</Name><Value>$eur</Value></Valute>
  <Valute ID="44"><NumCode>840</NumCode><CharCode>USD</CharCode><Nominal>1</Nominal><Name>US Dollar</Name><Value>$usd</Value></Valute>
  <Valute ID="36"><NumCode>643</NumCode><CharCode>RUB</CharCode><Nominal>100</Nominal><Name>Russian Ruble</Name><Value>$rub</Value></Valute>
</ValCurs>
""".trimIndent()

const val EMPTY_XML = """<?xml version="1.0" encoding="UTF-8"?><ValCurs Date="18.10.2026" name="Official exchange rate"></ValCurs>"""

class MemoryCache : RateCache {
    val data = sortedMapOf<LocalDate, String>()
    override fun load(date: LocalDate) = data[date]
    override fun save(date: LocalDate, xml: String) {
        data[date] = xml
    }
    override fun latest() = data.entries.lastOrNull()?.let { it.key to it.value }
}

/** Источник, отвечающий по таблице; отсутствующая дата = пустой ответ (выходной). */
class FakeSource(
    private val responses: Map<LocalDate, String> = emptyMap(),
    var online: Boolean = true,
) : RateSource {
    val requested = mutableListOf<LocalDate>()
    override suspend fun fetchXml(date: LocalDate): String {
        requested += date
        if (!online) throw IOException("no network")
        return responses[date] ?: EMPTY_XML
    }
}

fun d(text: String): LocalDate = LocalDate.parse(text, BnmXmlParser.DATE_FORMAT)
