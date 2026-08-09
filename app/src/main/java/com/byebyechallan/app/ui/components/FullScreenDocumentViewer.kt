package com.byebyechallan.app.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDocumentViewer(
    previewUri: Uri?,
    previewUrl: String?,
    displayFileName: String?,
    previewFileKey: String?,
    imageLoader: ImageLoader,
    authToken: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = displayFileName ?: "Document",
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    )
                }
            ) { padding ->
                DocumentPreviewContent(
                    previewUri = previewUri,
                    previewUrl = previewUrl,
                    displayFileName = displayFileName,
                    previewFileKey = previewFileKey,
                    imageLoader = imageLoader,
                    authToken = authToken,
                    displayMode = DocumentPreviewDisplayMode.FullScreen,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            }
        }
    }
}
