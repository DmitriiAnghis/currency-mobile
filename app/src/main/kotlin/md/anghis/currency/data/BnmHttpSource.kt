package md.anghis.currency.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import md.anghis.currency.core.RateSource
import md.anghis.currency.core.fmt
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** Загружает официальный курс BNM на дату. Любая сетевая ошибка — IOException. */
class BnmHttpSource(private val timeoutMs: Int = 10_000) : RateSource {
    override suspend fun fetchXml(date: LocalDate): String = withContext(Dispatchers.IO) {
        val url = URL("https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=${date.fmt()}")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("Accept", "application/xml")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("BNM ответил кодом $code")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Ошибка сети: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }
}
