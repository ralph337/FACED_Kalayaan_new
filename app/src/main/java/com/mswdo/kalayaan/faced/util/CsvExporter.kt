package com.mswdo.kalayaan.faced.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

/**
 * Exports tabular data to a CSV file under app-specific external storage and
 * shares it via FileProvider so it can be opened in Excel / Sheets or uploaded
 * (Functional Requirement #7 - export for printing/upload).
 *
 * CSV is used instead of a binary PDF/XLSX library to keep the app dependency-free;
 * both DROMIC report tables and FACED record listings can be produced this way and
 * opened directly by any spreadsheet app for printing.
 */
object CsvExporter {

    fun export(context: Context, fileName: String, header: List<String>, rows: List<List<String>>): Uri {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileWriter(file).use { writer ->
            if (header.isNotEmpty()) {
                writer.append(header.joinToString(",") { escape(it) }).append("\n")
            }
            rows.forEach { row ->
                writer.append(row.joinToString(",") { escape(it) }).append("\n")
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(context: Context, uri: Uri, mimeType: String = "text/csv"): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Export / Share Report")
    }

    private fun escape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
