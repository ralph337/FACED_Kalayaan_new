package com.mswdo.kalayaan.faced.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mswdo.kalayaan.faced.data.repository.FacedFullRecord
import java.io.File
import java.io.FileWriter

/**
 * Generates a native Microsoft Excel compatible spreadsheet (.xls via HTML table format)
 * matching the official DROMIC Disaster Report template (DISASTER REPORT FORM.xlsx).
 */
object ExcelExporter {

    fun exportDromicReport(
        context: Context,
        fileName: String,
        records: List<FacedFullRecord>,
        evacCenterName: String,
        disaster: String,
        date: String
    ): Uri {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, fileName)

        val html = buildString {
            append("<html><head><meta charset='utf-8'><style>")
            append("table { border-collapse: collapse; width: 100%; font-family: Arial; font-size: 9pt; }")
            append("th, td { border: 1px solid black; padding: 4px; text-align: center; vertical-align: middle; }")
            append("th { background-color: #d9e1f2; font-weight: bold; color: #000; }")
            append(".header { font-weight: bold; font-size: 11pt; text-align: left; border: none; }")
            append("</style></head><body>")
            
            append("<table>")
            append("<tr><td colspan='13' class='header'>Republic of the Philippines</td></tr>")
            append("<tr><td colspan='13' class='header'>Province of Laguna</td></tr>")
            append("<tr><td colspan='13' class='header'>Municipality of KALAYAAN</td></tr>")
            append("<tr><td colspan='13' class='header'><b>DISASTER RELIEF &amp; DROMIC REPORT (OFFICIAL TEMPLATE)</b></td></tr>")
            append("<tr><td colspan='13' class='header'>Evacuation Center: $evacCenterName &nbsp;&nbsp;&nbsp;&nbsp; Date: $date &nbsp;&nbsp;&nbsp;&nbsp; Disaster: $disaster</td></tr>")
            append("<tr><td colspan='13' style='border:none;'>&nbsp;</td></tr>")

            // Table Headers matching DROMIC template form exactly
            append("<tr>")
            append("<th rowspan='2'>NO.</th>")
            append("<th rowspan='2'>NAME OF FAMILY HEAD<br>(SURNAME, FIRST NAME, MIDDLE NAME)</th>")
            append("<th colspan='3'>NO. OF FAMILY</th>")
            append("<th colspan='7'>AGE AND SEX DISTRIBUTION OF IDPs INSIDE EVACUATION CENTER</th>")
            append("<th rowspan='2'>CATEGORY<br>(PWD / Solo Parent / Pregnant / Lactating / IP / 4Ps)</th>")
            append("</tr>")

            append("<tr>")
            append("<th>M</th><th>F</th><th>TOT</th>")
            append("<th>INFANT<br>(0-6M)</th>")
            append("<th>TODDLER<br>(7M-2Y)</th>")
            append("<th>PRE-SCHOOL<br>(3-5Y)</th>")
            append("<th>SCHOOL AGE<br>(6-12Y)</th>")
            append("<th>TEEN<br>(13-17Y)</th>")
            append("<th>ADULT<br>(18-59Y)</th>")
            append("<th>SENIOR<br>(60+Y)</th>")
            append("</tr>")

            records.forEachIndexed { index, full ->
                val r = full.record
                val totalMembers = 1 + full.members.size
                val maleCount = (if (r.sex.equals("Male", true)) 1 else 0) + full.members.count { it.sex.equals("Male", true) }
                val femaleCount = (if (r.sex.equals("Female", true)) 1 else 0) + full.members.count { it.sex.equals("Female", true) }

                // Age group calculations based on birthdates of head + members
                val allAges = listOf(r.age) + full.members.map { it.age }
                val infant = allAges.count { it == 0 }
                val toddler = allAges.count { it in 1..2 }
                val preSchool = allAges.count { it in 3..5 }
                val schoolAge = allAges.count { it in 6..12 }
                val teen = allAges.count { it in 13..17 }
                val adult = allAges.count { it in 18..59 }
                val senior = allAges.count { it >= 60 }

                val categories = full.vulnerableTags.map { it.vulnerabilityType }.toMutableList()
                if (r.is4Ps) categories.add("4Ps Family")
                val catStr = categories.distinct().joinToString(", ")

                append("<tr>")
                append("<td>${index + 1}</td>")
                append("<td style='text-align: left;'>${r.headName}</td>")
                append("<td>$maleCount</td><td>$femaleCount</td><td>$totalMembers</td>")
                append("<td>$infant</td><td>$toddler</td><td>$preSchool</td><td>$schoolAge</td><td>$teen</td><td>$adult</td><td>$senior</td>")
                append("<td style='text-align: left;'>$catStr</td>")
                append("</tr>")
            }

            append("</table></body></html>")
        }

        FileWriter(file).use { it.write(html) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(context: Context, uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Export / Share Excel Report")
    }
}
