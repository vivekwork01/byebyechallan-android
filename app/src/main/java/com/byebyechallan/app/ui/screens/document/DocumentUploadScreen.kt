package com.byebyechallan.app.ui.screens.document

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.FullScreenLoading
import com.byebyechallan.app.ui.components.PrimaryButton
import com.byebyechallan.app.util.DateUtils
import com.byebyechallan.app.util.FileUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadScreen(
    app: ByeByeChallanApp,
    profileId: Long,
    vehicleRegNo: String,
    docTemplateId: String,
    docName: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var userId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { userId = app.sessionManager.getUserId() }

    if (userId == null) {
        FullScreenLoading()
        return
    }

    val viewModel = viewModel {
        DocumentUploadViewModel(userId!!, profileId, vehicleRegNo, docTemplateId, docName, app.documentRepository)
    }
    val uiState by viewModel.uiState.collectAsState()

    var pickedFileUri by remember { mutableStateOf<Uri?>(null) }
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var expiryDate by remember { mutableStateOf<LocalDate?>(null) }
    var notifyEmail by remember { mutableStateOf(true) }
    var notifyWhatsApp by remember { mutableStateOf(false) }
    var notifySms by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Pre-fill expiry date from an existing record once it loads (edit mode)
    LaunchedEffect(uiState.existingDoc) {
        uiState.existingDoc?.expiryDate?.let { dateStr ->
            DateUtils.parseToEpochMillis(dateStr)?.let { millis ->
                expiryDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        uiState.existingDoc?.let {
            notifyEmail = it.email
            notifyWhatsApp = it.whatsApp
            notifySms = it.sms
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pickedFileUri = uri
        pickedFileName = uri?.lastPathSegment
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(docName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoadingExisting) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) { FullScreenLoading() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            if (uiState.existingDoc != null) {
                Text(
                    text = "A document is already uploaded for this item. Pick a new file only if you want to replace it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- File picker ----
            OutlinedButton(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (pickedFileName != null) "Selected: $pickedFileName" else "Choose File (PDF or Image)")
            }

            if (pickedFileUri == null && uiState.existingDoc?.s3Link != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Existing file on record", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- Expiry date ----
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(expiryDate?.toString() ?: "Select Expiry Date")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- Notification preferences ----
            Text("Notify me before expiry via:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            ToggleRow("Email", notifyEmail) { notifyEmail = it }
            ToggleRow("WhatsApp", notifyWhatsApp) { notifyWhatsApp = it }
            ToggleRow("SMS", notifySms) { notifySms = it }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ErrorBanner(uiState.errorMessage)
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = if (uiState.existingDoc != null) "Update Document" else "Upload Document",
                isLoading = uiState.isSaving,
                onClick = {
                    val file = pickedFileUri?.let { FileUtils.copyUriToCacheFile(context, it) }
                    viewModel.submit(file, expiryDate, notifyEmail, notifyWhatsApp, notifySms)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expiryDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
