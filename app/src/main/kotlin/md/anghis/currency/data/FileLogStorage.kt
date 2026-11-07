package md.anghis.currency.data

import md.anghis.currency.core.LogStorage
import java.io.File

/** Журнал конвертаций в файле history.txt во внутренней памяти приложения. */
class FileLogStorage(baseDir: File) : LogStorage {
    private val file = File(baseDir, "history.txt")

    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        val tmp = File(file.parentFile, "history.txt.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}
