package md.anghis.currency.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Сохраняет CSV во временную папку и открывает системное меню «Поделиться»
 * (почта, мессенджер, Google Диск). BOM в начале нужен, чтобы Excel прочитал UTF-8.
 */
fun shareCsv(context: Context, fileName: String, csv: String) {
    val dir = File(context.cacheDir, "export").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText("﻿" + csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Отправить $fileName").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
