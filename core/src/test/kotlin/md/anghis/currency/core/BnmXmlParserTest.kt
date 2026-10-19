package md.anghis.currency.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BnmXmlParserTest {
    @Test
    fun parsesRatesNominalAndDate() {
        val rates = BnmXmlParser.parse(bnmXml("17.10.2026"), d("18.10.2026"))
        assertEquals(d("17.10.2026"), rates.date)
        assertEquals(setOf("EUR", "USD", "RUB"), rates.rates.keys)
        assertEquals(BigDecimal("19.6012"), rates.rates.getValue("EUR").value)
        assertEquals(100, rates.rates.getValue("RUB").nominal)
        assertEquals(0, BigDecimal("0.214567").compareTo(rates.mdlPerUnit("RUB")))
        assertEquals(BigDecimal.ONE, rates.mdlPerUnit("MDL"))
    }

    @Test
    fun usesRequestedDateWhenAttributeMissing() {
        val xml = bnmXml("17.10.2026").replace(""" Date="17.10.2026"""", "")
        assertEquals(d("16.10.2026"), BnmXmlParser.parse(xml, d("16.10.2026")).date)
    }

    @Test(expected = EmptyResponseException::class)
    fun emptyValCursIsEmptyResponse() {
        BnmXmlParser.parse(EMPTY_XML, d("18.10.2026"))
    }

    @Test(expected = EmptyResponseException::class)
    fun blankBodyIsEmptyResponse() {
        BnmXmlParser.parse("   ", d("18.10.2026"))
    }

    @Test(expected = InvalidResponseException::class)
    fun htmlInsteadOfXmlIsInvalid() {
        BnmXmlParser.parse("<html><body>Service unavailable</body></html>", d("18.10.2026"))
    }

    @Test(expected = InvalidResponseException::class)
    fun brokenXmlIsInvalid() {
        BnmXmlParser.parse("<ValCurs Date=\"17.10.2026\"><Valute>", d("17.10.2026"))
    }

    @Test(expected = InvalidResponseException::class)
    fun nonNumericValueIsInvalid() {
        BnmXmlParser.parse(bnmXml("17.10.2026", eur = "abc"), d("17.10.2026"))
    }
}
