package com.byebyechallan.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipInputStream

object XlsxHtmlRenderer {

    fun renderFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                renderFromStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun renderFromFile(file: File): String? {
        return try {
            file.inputStream().use { input ->
                renderFromStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun renderFromStream(input: java.io.InputStream): String? {
        var sharedStringsXml: String? = null
        var sheetXml: String? = null

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> sharedStringsXml = zip.readBytes().toString(Charsets.UTF_8)
                    "xl/worksheets/sheet1.xml" -> sheetXml = zip.readBytes().toString(Charsets.UTF_8)
                }
                if (sharedStringsXml != null && sheetXml != null) break
                entry = zip.nextEntry
            }
        }

        val sheet = sheetXml ?: return null
        val sharedStrings = sharedStringsXml?.let { parseSharedStrings(it) } ?: emptyList()
        return renderSheetHtml(sheet, sharedStrings)
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val strings = mutableListOf<String>()
        val siPattern = Regex("""<si[^>]*>(.*?)</si>""", RegexOption.DOT_MATCHES_ALL)
        siPattern.findAll(xml).forEach { match ->
            val block = match.groupValues[1]
            val text = Regex("""<t[^>]*>([^<]*)</t>""")
                .findAll(block)
                .joinToString("") { it.groupValues[1] }
            strings.add(text)
        }
        return strings
    }

    private fun renderSheetHtml(sheetXml: String, sharedStrings: List<String>): String {
        val html = StringBuilder("<table>")
        val rowPattern = Regex("""<row[^>]*>(.*?)</row>""", RegexOption.DOT_MATCHES_ALL)
        rowPattern.findAll(sheetXml).forEach { rowMatch ->
            html.append("<tr>")
            val cellPattern = Regex("""<c([^>]*)>(.*?)</c>""", RegexOption.DOT_MATCHES_ALL)
            cellPattern.findAll(rowMatch.groupValues[1]).forEach { cellMatch ->
                val attrs = cellMatch.groupValues[1]
                val body = cellMatch.groupValues[2]
                val value = Regex("""<v>([^<]*)</v>""").find(body)?.groupValues?.get(1).orEmpty()
                val inline = Regex("""<t[^>]*>([^<]*)</t>""").find(body)?.groupValues?.get(1)
                val cellText = when {
                    inline != null -> inline
                    attrs.contains(""" t="s"""") -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                    else -> value
                }
                html.append("<td>").append(escapeHtml(cellText)).append("</td>")
            }
            html.append("</tr>")
        }
        html.append("</table>")
        return html.toString()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
