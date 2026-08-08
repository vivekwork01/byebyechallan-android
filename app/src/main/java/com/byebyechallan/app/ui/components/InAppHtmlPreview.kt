package com.byebyechallan.app.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun InAppHtmlPreview(
    bodyHtml: String,
    modifier: Modifier = Modifier
) {
    val fullHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: sans-serif;
                    font-size: 15px;
                    line-height: 1.5;
                    color: #1a1a1a;
                    margin: 0;
                    padding: 4px;
                    word-wrap: break-word;
                }
                p { margin: 0 0 12px; }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    font-size: 13px;
                }
                th, td {
                    border: 1px solid #d0d0d0;
                    padding: 6px 8px;
                    vertical-align: top;
                }
                tr:nth-child(even) { background: #fafafa; }
            </style>
        </head>
        <body>$bodyHtml</body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
        }
    )
}
