package com.mswdo.kalayaan.faced.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mswdo.kalayaan.faced.data.repository.FacedFullRecord

object FacedPdfGenerator {

    /**
     * Uses Android's native WebView Print engine to render highly-precise HTML+CSS
     * into a PDF document, bypassing Canvas scaling/ANR issues entirely.
     */
    fun printHtmlPdf(context: Context, full: FacedFullRecord, onComplete: () -> Unit) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "FACED_${full.record.serial}"
                val adapter = view.createPrintDocumentAdapter(jobName)
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                
                printManager.print(jobName, adapter, attributes)
                onComplete()
            }
        }

        val htmlContent = generateHtml(full)
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    private fun field(number: String, label: String, value: String, width: String = "100%"): String {
        return """
            <div style="display: inline-block; width: $width; margin-bottom: 2px;">
                <div style="display: flex; align-items: flex-end;">
                    <span style="white-space: nowrap; margin-right: 5px;">$number. $label</span>
                    <span style="border-bottom: 1px solid black; flex-grow: 1; text-align: center; font-weight: bold; min-width: 20px;">$value</span>
                </div>
            </div>
        """
    }

    private fun renderFacedCard(full: FacedFullRecord, copyType: String): String {
        val r = full.record
        
        var memberRows = ""
        for (i in 0 until 8) {
            val m = full.members.getOrNull(i)
            if (m != null) {
                val vuln = full.vulnerableTags.filter { it.personName == m.name }.joinToString(", ") { it.vulnerabilityType.take(4) }
                memberRows += "<tr><td>${m.name}</td><td>${m.relation}</td><td>${m.birthDate}</td><td>${m.age}</td><td>${m.sex.take(1)}</td><td>${m.education}</td><td>${m.occupation}</td><td>$vuln</td></tr>"
            } else {
                memberRows += "<tr><td>&nbsp;</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>"
            }
        }

        val nameParts = r.headName.trim().split(Regex("\\s+"))
        val lastName = if (nameParts.size > 1) nameParts.last() else r.headName
        val firstName = if (nameParts.size > 1) nameParts.dropLast(1).joinToString(" ") else ""

        return """
            <div class="card">
                <div class="header-container">
                    <div style="width: 50px; text-align: center; color: #1a365d; font-weight: bold; font-size: 14pt;">DSWD</div>
                    <div class="header-text">
                        <h5>Republic of the Philippines<br>Department of Social Welfare and Development</h5>
                        <h3>FAMILY ASSISTANCE CARD IN<br>EMERGENCIES AND DISASTERS (FACED)</h3>
                    </div>
                    <div class="serial-box">
                        <div style="font-size: 5pt; margin-bottom: 2px;"><i>THIS CARD IS NOT FOR SALE</i><br><b>$copyType</b></div>
                        <div class="title">OFFICIAL USE ONLY</div>
                        <div style="text-align: left; padding: 2px; font-size: 6pt;">SERIAL NUMBER: <b>${r.serial}</b></div>
                    </div>
                </div>
                
                <div class="section-title">LOCATION OF THE AFFECTED FAMILY</div>
                <div style="display: flex;">
                    <div style="flex: 1;">
                        ${field("1", "REGION", "CALABARZON")}
                        ${field("2", "PROVINCE", "Laguna")}
                        ${field("3", "CITY/MUNICIPALITY", "Kalayaan")}
                    </div>
                    <div style="flex: 1; padding-left: 10px;">
                        ${field("4", "DISTRICT", "4th District")}
                        ${field("5", "BARANGAY", r.barangay)}
                        ${field("6", "EVACUATION CENTER/SITE", r.evacCenterName)}
                    </div>
                </div>
                
                <div class="section-title">HEAD OF THE FAMILY</div>
                <div style="display: flex;">
                    <div style="flex: 1;">
                        ${field("7", "LAST NAME", lastName)}
                        ${field("8", "FIRST NAME", firstName)}
                        ${field("9", "MIDDLE NAME", "")}
                        ${field("10", "NAME EXT.", "")}
                        ${field("11", "BIRTHDATE", r.birthDate)}
                        ${field("12", "AGE", r.age.toString())}
                        ${field("13", "BIRTHPLACE", r.birthPlace)}
                        <div style="margin-top: 2px;">14. SEX &nbsp;&nbsp; 
                           <span class="checkbox">${if(r.sex.equals("Male", true)) "X" else ""}</span> MALE &nbsp;&nbsp;
                           <span class="checkbox">${if(r.sex.equals("Female", true)) "X" else ""}</span> FEMALE
                        </div>
                    </div>
                    <div style="flex: 1; padding-left: 10px;">
                        ${field("15", "CIVIL STATUS", r.civilStatus)}
                        ${field("16", "MOTHER'S MAIDEN NAME", r.mothersMaidenName)}
                        ${field("17", "RELIGION", r.religion)}
                        ${field("18", "OCCUPATION", r.occupation)}
                        ${field("19", "MONTHLY INCOME", "Php ${r.monthlyIncome}")}
                        ${field("20", "ID PRESENTED", r.idType)}
                        ${field("21", "ID NUMBER", r.idNumber)}
                        ${field("22", "CONTACT NUMBER", r.contactNumber)}
                    </div>
                </div>
                
                <div style="margin-top: 2px;">
                    ${field("23", "PERMANENT ADDRESS", r.permanentAddress)}
                </div>
                <div style="margin-top: 2px;">
                    24. OTHERS &nbsp;&nbsp; <span class="checkbox">${if(r.is4Ps) "X" else ""}</span> 4Ps Beneficiary &nbsp;&nbsp; <span class="checkbox"></span> IP
                </div>
                
                <div class="section-title">25. FAMILY INFORMATION</div>
                <table>
                    <tr style="background-color: #e2e8f0; text-align: center;">
                        <th>FAMILY MEMBERS</th><th>RELATION</th><th>BIRTHDATE</th><th>AGE</th><th>SEX</th>
                        <th>HIGHEST ED.</th><th>OCCUPATION</th><th>VULNERABILITY</th>
                    </tr>
                    $memberRows
                </table>
                
                <div class="section-title" style="margin-top: 0;">ACCOUNT INFORMATION</div>
                <div style="font-size: 6pt; margin-bottom: 4px;"><i>Note: In case the family head does not have a bank or an e-wallet account, any of the family members with a validated account can be indicated.</i></div>
                <div style="display: flex;">
                    <div style="flex: 1;">
                        ${field("26", "BANK/E-WALLET", "")}
                        ${field("27", "ACCOUNT NAME", "")}
                    </div>
                    <div style="flex: 1; padding-left: 10px;">
                        ${field("28", "ACCOUNT TYPE", "")}
                        ${field("29", "ACCOUNT NUMBER", "")}
                    </div>
                </div>

                <div style="display: flex; margin-top: 4px;">
                    <div style="flex: 1;">
                        <div class="section-title" style="background-color: #e2e8f0; color: black; margin-right: 5px;">30. HOUSE OWNERSHIP</div>
                        <span class="checkbox"></span> OWNER &nbsp; <span class="checkbox"></span> RENTER &nbsp; <span class="checkbox"></span> SHARER
                    </div>
                    <div style="flex: 1;">
                        <div class="section-title" style="background-color: #e2e8f0; color: black; margin-left: 5px;">31. SHELTER DAMAGE CLASSIFICATION</div>
                        <span class="checkbox">${if(r.shelterDamage.name.contains("PARTIALLY")) "X" else ""}</span> PARTIALLY DAMAGED &nbsp; 
                        <span class="checkbox">${if(r.shelterDamage.name.contains("TOTALLY")) "X" else ""}</span> TOTALLY DAMAGED
                    </div>
                </div>
                
                <div style="display: flex; margin-top: 15px;">
                    <div style="width: 70px; text-align: center;">
                        <div class="sig-box" style="margin: 0 auto;"></div>
                        <div style="font-size: 6pt;">Right Thumbmark</div>
                    </div>
                    <div style="flex: 1; padding: 0 10px; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div class="sig-line"></div>
                            <div style="font-size: 6pt; text-align: center;">Signature/ Thumbmark of Family Head</div>
                        </div>
                        <div>
                            <div class="sig-line">${DateUtils.today()}</div>
                            <div style="font-size: 6pt; text-align: center;">Date Registered</div>
                        </div>
                    </div>
                    <div style="flex: 1; padding: 0 10px; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div class="sig-line"></div>
                            <div style="font-size: 6pt; text-align: center;">Name/ Signature of Barangay Captain</div>
                        </div>
                        <div>
                            <div class="sig-line">Lalaine R. De Leon, RSW</div>
                            <div style="font-size: 6pt; text-align: center;">Name/Signature of LSWDO</div>
                        </div>
                    </div>
                </div>
                
                <div class="footer-text">
                    <b>32. DATA PRIVACY DECLARATION</b><br>
                    All data and information indicated herein shall be used for identification purposes for the implementation of disaster risk reduction and management (DRRM) programs, projects, and activities and its disclosure shall be in compliance to Republic Act 10173 (Data Privacy Act of 2012).
                </div>
            </div>
        """
    }

    private fun renderAssistanceCard(full: FacedFullRecord, copyType: String): String {
        val r = full.record
        var assistanceRows = ""
        for (i in 0 until 24) {
            val a = full.assistanceHistory.getOrNull(i)
            if (a != null) {
                assistanceRows += "<tr><td>${a.date}</td><td>${a.receivingMember}</td><td>${a.disaster}</td><td>${a.itemName}</td><td>${a.unit}</td><td>${a.qty}</td><td>${a.estimatedCost}</td><td>${a.provider}</td><td></td></tr>"
            } else {
                assistanceRows += "<tr><td>&nbsp;</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>"
            }
        }

        return """
            <div class="card">
                <div class="header-container">
                    <div style="width: 50px; text-align: center; color: #1a365d; font-weight: bold; font-size: 14pt;">DSWD</div>
                    <div class="header-text">
                        <h5>Republic of the Philippines<br>Department of Social Welfare and Development</h5>
                        <h3>FAMILY ASSISTANCE CARD IN<br>EMERGENCIES AND DISASTERS (FACED)</h3>
                    </div>
                    <div class="serial-box">
                        <div style="font-size: 5pt; margin-bottom: 2px;"><i>THIS CARD IS NOT FOR SALE</i><br><b>$copyType</b></div>
                        <div class="title">OFFICIAL USE ONLY</div>
                        <div style="text-align: left; padding: 2px; font-size: 6pt;">SERIAL NUMBER: <b>${r.serial}</b></div>
                    </div>
                </div>
                
                <div class="section-title">35. FAMILY ASSISTANCE RECORD</div>
                <table>
                    <tr style="background-color: #e2e8f0; text-align: center;">
                        <th rowspan="2">DATE</th><th rowspan="2">NAME OF RECEIVING<br>FAMILY MEMBER</th>
                        <th colspan="5">ASSISTANCE PROVIDED</th><th rowspan="2">PROVIDER</th><th rowspan="2">SIGNATURE/<br>THUMBMARK</th>
                    </tr>
                    <tr style="background-color: #e2e8f0; text-align: center;">
                        <th>EMERGENCY / DISASTER</th><th>ASSISTANCE</th><th>UNIT</th><th>QUANTITY</th><th>COST</th>
                    </tr>
                    $assistanceRows
                </table>
            </div>
        """
    }

    private fun generateHtml(full: FacedFullRecord): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    @page { size: A4 landscape; margin: 8mm; }
                    body { font-family: Arial, sans-serif; font-size: 6.5pt; color: #000; margin: 0; padding: 0; }
                    .card-container { display: grid; grid-template-columns: 1fr 1fr; gap: 10mm; }
                    .card { display: flex; flex-direction: column; overflow: hidden; }
                    .header-container { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2px; }
                    .header-text { text-align: center; flex-grow: 1; }
                    .header-text h3 { margin: 0; font-size: 9pt; color: #1a365d; }
                    .header-text h5 { margin: 0; font-size: 7pt; font-weight: normal; }
                    .serial-box { border: 1px solid black; text-align: center; width: 120px; font-size: 7pt; }
                    .serial-box .title { background-color: #1a365d; color: white; padding: 2px; }
                    .section-title { background-color: #1a365d; color: white; font-weight: bold; text-align: center; padding: 2px; font-size: 7pt; margin-top: 2px; margin-bottom: 2px; }
                    table { border-collapse: collapse; width: 100%; margin-bottom: 2px; table-layout: fixed; }
                    td, th { border: 1px solid black; padding: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
                    .checkbox { display: inline-block; width: 8px; height: 8px; border: 1px solid black; margin-right: 2px; text-align: center; line-height: 8px; font-size: 6pt; font-weight: bold;}
                    .sig-box { border: 1px solid black; height: 40px; width: 60px; }
                    .sig-line { border-bottom: 1px solid black; width: 100%; text-align: center; margin-top: 15px; height: 12px; }
                    .footer-text { text-align: center; font-size: 5.5pt; margin-top: 5px; }
                </style>
            </head>
            <body>
                <!-- PAGE 1: FACED FORM -->
                <div class="card-container">
                    ${renderFacedCard(full, "BENEFICIARY'S COPY")}
                    ${renderFacedCard(full, "SOCIAL WORKER'S COPY")}
                </div>
                
                <div style="page-break-before: always;"></div>
                
                <!-- PAGE 2: ASSISTANCE RECORD -->
                <div class="card-container">
                    ${renderAssistanceCard(full, "BENEFICIARY'S COPY")}
                    ${renderAssistanceCard(full, "SOCIAL WORKER'S COPY")}
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
