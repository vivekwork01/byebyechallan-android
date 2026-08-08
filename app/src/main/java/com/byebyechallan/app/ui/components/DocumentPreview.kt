package com.byebyechallan.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.byebyechallan.app.util.AuthenticatedFileDownloader
import com.byebyechallan.app.util.DocxHtmlRenderer
import com.byebyechallan.app.util.DocumentFileType
import com.byebyechallan.app.util.DocumentPreviewLoader
import com.byebyechallan.app.util.FileUtils
import com.byebyechallan.app.util.XlsxHtmlRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DocumentPreviewCard(
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    previewFileKey: String?,
    imageLoader: ImageLoader,
    authToken: String?
) {
    val context = LocalContext.current
    val fileType = FileUtils.resolveDocumentFileType(
        context,
        displayFileName ?: previewFileKey,
        previewUri
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Document Preview", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            when (fileType) {
                DocumentFileType.IMAGE -> {
                    val previewSource = previewUri ?: previewUrl
                    if (previewSource != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(previewSource)
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = displayFileName ?: "Document preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 320.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        PreviewUnavailableMessage("Image preview is not available")
                    }
                }
                DocumentFileType.PDF -> {
                    PdfDocumentPreview(
                        previewUri = previewUri,
                        previewUrl = previewUrl,
                        displayFileName = displayFileName ?: previewFileKey,
                        authToken = authToken,
                        maxPages = 20
                    )
                }
                DocumentFileType.OFFICE -> {
                    OfficeDocumentPreview(
                        previewUri = previewUri,
                        previewUrl = previewUrl,
                        displayFileName = displayFileName ?: previewFileKey,
                        authToken = authToken
                    )
                }
                DocumentFileType.TEXT -> {
                    TextDocumentPreview(
                        previewUri = previewUri,
                        previewUrl = previewUrl,
                        displayFileName = displayFileName ?: previewFileKey,
                        authToken = authToken
                    )
                }
                DocumentFileType.OTHER -> {
                    UnsupportedDocumentPreview(
                        displayFileName = displayFileName ?: previewFileKey,
                        message = "In-app preview is not available for this file type."
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfDocumentPreview(
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    authToken: String?,
    maxPages: Int = 20
) {
    val context = LocalContext.current
    var isLoading by remember(previewUri, previewUrl) { mutableStateOf(true) }
    var errorMessage by remember(previewUri, previewUrl) { mutableStateOf<String?>(null) }
    var pageBitmaps by remember(previewUri, previewUrl) { mutableStateOf<List<Bitmap>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose {
            pageBitmaps.forEach { it.recycle() }
        }
    }

    LaunchedEffect(previewUri, previewUrl) {
        val previous = pageBitmaps
        pageBitmaps = emptyList()
        previous.forEach { it.recycle() }

        isLoading = true
        errorMessage = null

        val bitmaps = withContext(Dispatchers.IO) {
            renderPdfPages(context, previewUri, previewUrl, displayFileName, authToken, maxPages)
        }

        if (bitmaps.isNullOrEmpty()) {
            errorMessage = "Couldn't load PDF preview"
        } else {
            pageBitmaps = bitmaps
        }
        isLoading = false
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null -> PreviewUnavailableMessage(errorMessage!!)
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pageBitmaps.forEachIndexed { index, bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF page ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficeDocumentPreview(
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    authToken: String?
) {
    val context = LocalContext.current
    val mimeType = previewUri?.let { FileUtils.getMimeType(context, it) }
    when {
        FileUtils.isDocxFile(displayFileName, mimeType) -> {
            InAppHtmlDocumentPreview(
                previewUri = previewUri,
                previewUrl = previewUrl,
                displayFileName = displayFileName,
                authToken = authToken,
                renderFromUri = { uri -> DocxHtmlRenderer.renderFromUri(context, uri) },
                renderFromFile = { file -> DocxHtmlRenderer.renderFromFile(file) },
                emptyMessage = "Couldn't render this Word document in the app."
            )
        }
        FileUtils.isXlsxFile(displayFileName) || mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> {
            InAppHtmlDocumentPreview(
                previewUri = previewUri,
                previewUrl = previewUrl,
                displayFileName = displayFileName,
                authToken = authToken,
                renderFromUri = { uri -> XlsxHtmlRenderer.renderFromUri(context, uri) },
                renderFromFile = { file -> XlsxHtmlRenderer.renderFromFile(file) },
                emptyMessage = "Couldn't render this spreadsheet in the app."
            )
        }
        else -> {
            UnsupportedDocumentPreview(
                displayFileName = displayFileName,
                message = "In-app preview supports .docx and .xlsx. Older .doc / .xls files are not supported yet."
            )
        }
    }
}

@Composable
private fun TextDocumentPreview(
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    authToken: String?
) {
    val context = LocalContext.current
    InAppHtmlDocumentPreview(
        previewUri = previewUri,
        previewUrl = previewUrl,
        displayFileName = displayFileName,
        authToken = authToken,
        renderFromUri = { uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val text = stream.bufferedReader().readText()
                if (text.isNotBlank()) "<pre>${escapeHtml(text)}</pre>" else null
            }
        },
        renderFromFile = { file ->
            val text = file.readText()
            if (text.isNotBlank()) "<pre>${escapeHtml(text)}</pre>" else null
        },
        emptyMessage = "Couldn't read this text file."
    )
}

@Composable
private fun InAppHtmlDocumentPreview(
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    authToken: String?,
    renderFromUri: (Uri) -> String?,
    renderFromFile: (File) -> String?,
    emptyMessage: String
) {
    val context = LocalContext.current
    var isLoading by remember(previewUri, previewUrl) { mutableStateOf(true) }
    var htmlContent by remember(previewUri, previewUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(previewUri, previewUrl) {
        isLoading = true
        htmlContent = withContext(Dispatchers.IO) {
            previewUri?.let { renderFromUri(it) }
                ?: DocumentPreviewLoader.loadFile(
                    context = context,
                    previewUri = null,
                    previewUrl = previewUrl,
                    displayFileName = displayFileName,
                    authToken = authToken
                )?.let { renderFromFile(it) }
        }
        isLoading = false
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        htmlContent.isNullOrBlank() -> {
            UnsupportedDocumentPreview(displayFileName = displayFileName, message = emptyMessage)
        }
        else -> {
            InAppHtmlPreview(
                bodyHtml = htmlContent!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 420.dp)
            )
        }
    }
}

@Composable
private fun UnsupportedDocumentPreview(
    displayFileName: String?,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            displayFileName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

@Composable
private fun PreviewUnavailableMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun renderPdfPages(
    context: android.content.Context,
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    authToken: String?,
    maxPages: Int = 3
): List<Bitmap>? {
    val descriptor = openPdfDescriptor(context, previewUri, previewUrl, displayFileName, authToken)
        ?: return null

    return try {
        PdfRenderer(descriptor).use { renderer ->
            val pageCount = minOf(renderer.pageCount, maxPages)
            buildList(pageCount) {
                for (index in 0 until pageCount) {
                    renderer.openPage(index).use { page ->
                        val scale = 2
                        val bitmap = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        add(bitmap)
                    }
                }
            }
        }
    } catch (e: Exception) {
        null
    } finally {
        try {
            descriptor.close()
        } catch (_: Exception) {
        }
    }
}

private suspend fun openPdfDescriptor(
    context: android.content.Context,
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    authToken: String?
): ParcelFileDescriptor? {
    previewUri?.let { uri ->
        return context.contentResolver.openFileDescriptor(uri, "r")
    }

    if (!previewUrl.isNullOrBlank()) {
        val cachedFile = AuthenticatedFileDownloader.downloadToCache(
            context = context,
            url = previewUrl,
            authToken = authToken,
            preferredFileName = displayFileName
        ) ?: return null
        return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    return null
}
