package com.byebyechallan.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipInputStream

object DocxHtmlRenderer {

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
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zip.readBytes().toString(Charsets.UTF_8)
                    return renderDocumentXml(xml)
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun renderDocumentXml(xml: String): String {
        val paragraphs = xml.split("</w:p>")
        val html = StringBuilder()
        for (paragraph in paragraphs) {
            if (!paragraph.contains("<w:p")) continue
            html.append("<p>")
            val runs = paragraph.split("</w:r>")
            for (run in runs) {
                if (!run.contains("<w:r")) continue
                val text = extractTagValues(run, "w:t").joinToString("")
                if (text.isEmpty()) continue
                val escaped = escapeHtml(text)
                val content = buildString {
                    if (run.contains("<w:b") || run.contains("w:b/>")) append("<strong>")
                    if (run.contains("<w:i") || run.contains("w:i/>")) append("<em>")
                    append(escaped)
                    if (run.contains("<w:i") || run.contains("w:i/>")) append("</em>")
                    if (run.contains("<w:b") || run.contains("w:b/>")) append("</strong>")
                }
                html.append(content)
            }
            html.append("</p>")
        }
        return html.toString().trim()
    }

    private fun extractTagValues(xml: String, tag: String): List<String> {
        val pattern = Regex("""<$tag[^>]*>([^<]*)</$tag>""")
        return pattern.findAll(xml).map { it.groupValues[1] }.toList()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
