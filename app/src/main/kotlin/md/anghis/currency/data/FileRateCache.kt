package md.anghis.currency.data

import md.anghis.currency.core.RateCache
import java.io.File
import java.time.LocalDate

/** Хранит каждый успешный ответ BNM в файле rates/ГГГГ-ММ-ДД.xml во внутренней памяти приложения. */
class FileRateCache(baseDir: File) : RateCache {
    private val dir = File(baseDir, "rates").apply { mkdirs() }

    private fun file(date: LocalDate) = File(dir, "$date.xml")

    override fun load(date: LocalDate): String? = file(date).takeIf { it.exists() }?.readText()

    override fun save(date: LocalDate, xml: String) {
        val tmp = File(dir, "$date.xml.tmp")
        tmp.writeText(xml)
        if (!tmp.renameTo(file(date))) {
            file(date).writeText(xml)
            tmp.delete()
        }
    }

    override fun latest(): Pair<LocalDate, String>? =
        dir.listFiles { f -> f.name.endsWith(".xml") }
            ?.mapNotNull { f -> runCatching { LocalDate.parse(f.nameWithoutExtension) to f }.getOrNull() }
            ?.maxByOrNull { it.first }
            ?.let { (date, f) -> date to f.readText() }
}
