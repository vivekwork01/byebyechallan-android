package com.byebyechallan.app.ui.screens.vehicle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.data.model.CountryDto
import com.byebyechallan.app.data.model.RegistrationDto
import com.byebyechallan.app.data.model.StateDto
import com.byebyechallan.app.data.model.VehicleTypeResponseDto
import com.byebyechallan.app.ui.components.DropdownSelector
import com.byebyechallan.app.ui.components.DocumentPreviewCard
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.PrimaryButton
import com.byebyechallan.app.data.model.VehicleSummary
import com.byebyechallan.app.util.AuthenticatedImageLoader
import com.byebyechallan.app.util.FileUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    app: ByeByeChallanApp,
    profileId: Long,
    onBack: () -> Unit,
    onVehicleAdded: (VehicleSummary) -> Unit
) {
    val viewModel = viewModel {
        AddVehicleViewModel(
            profileId,
            app.masterRepository,
            app.documentRepository,
            app.profileRepository,
            app.sessionManager
        )
    }
    val state by viewModel.uiState.collectAsState()

    var registrationNo by remember { mutableStateOf("") }
    var vehicleName by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<CountryDto?>(null) }
    var selectedState by remember { mutableStateOf<StateDto?>(null) }
    var selectedRegType by remember { mutableStateOf<RegistrationDto?>(null) }
    var selectedVehicleType by remember { mutableStateOf<VehicleTypeResponseDto?>(null) }

    var rcFileUri by remember { mutableStateOf<Uri?>(null) }
    var rcFileName by remember { mutableStateOf<String?>(null) }
    var registrationDate by remember { mutableStateOf<LocalDate?>(null) }
    var rcExpiryDate by remember { mutableStateOf<LocalDate?>(null) }
    var showRegistrationDatePicker by remember { mutableStateOf(false) }
    var showRcExpiryDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var authToken by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        authToken = app.sessionManager.getToken()
    }
    val imageLoader = remember(authToken) { AuthenticatedImageLoader.create(context, authToken) }

    val rcFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        rcFileUri = uri
        rcFileName = uri?.let { FileUtils.getDisplayFileName(context, it) }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Vehicle added successfully", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Vehicle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Registration Certificate",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upload your RC and enter registration details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (rcFileUri != null) {
                DocumentPreviewCard(
                    previewUri = rcFileUri,
                    previewUrl = null,
                    displayFileName = rcFileName,
                    previewFileKey = null,
                    imageLoader = imageLoader,
                    authToken = authToken
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (rcFileUri != null) {
                OutlinedButton(
                    onClick = { rcFilePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Different RC File")
                }
            } else {
                OutlinedButton(
                    onClick = { rcFilePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Registration Certificate")
                }
            }

            if (!rcFileName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selected: $rcFileName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registration Date",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { showRegistrationDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(registrationDate?.toString() ?: "Select Registration Date")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RC Expiry Date",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { showRcExpiryDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rcExpiryDate?.toString() ?: "Select RC Expiry Date")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = registrationNo,
                onValueChange = { registrationNo = it.uppercase() },
                label = { Text("Vehicle Registration No.") },
                placeholder = { Text("e.g. KA01AB1234") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = vehicleName,
                onValueChange = { vehicleName = it },
                label = { Text("Vehicle Name (optional)") },
                placeholder = { Text("e.g. John's Honda Activa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            DropdownSelector(
                label = "Country",
                options = state.countries,
                selectedOption = selectedCountry,
                optionLabel = { it.countryName },
                onOptionSelected = {
                    selectedCountry = it
                    selectedState = null
                    selectedRegType = null
                    viewModel.onCountrySelected(it.countryId)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DropdownSelector(
                label = "State",
                options = state.states,
                selectedOption = selectedState,
                optionLabel = { it.stateName },
                enabled = selectedCountry != null,
                onOptionSelected = { selectedState = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DropdownSelector(
                label = "Registration Type",
                options = state.registrationTypes,
                selectedOption = selectedRegType,
                optionLabel = { it.registrationType },
                enabled = selectedCountry != null,
                onOptionSelected = { selectedRegType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DropdownSelector(
                label = "Vehicle Type",
                options = state.vehicleTypes,
                selectedOption = selectedVehicleType,
                optionLabel = { it.vehicleTypeName },
                enabled = state.vehicleTypes.isNotEmpty(),
                onOptionSelected = { selectedVehicleType = it }
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ErrorBanner(state.errorMessage)
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Add Vehicle",
                isLoading = state.isSaving,
                onClick = {
                    val rcFile = rcFileUri?.let { FileUtils.copyUriToCacheFile(context, it) }
                    viewModel.saveVehicle(
                        registrationNo = registrationNo,
                        vehicleName = vehicleName,
                        country = selectedCountry?.countryId ?: "",
                        state = selectedState?.stateId ?: "",
                        registrationType = selectedRegType?.registrationCode ?: "",
                        vehicleTypeId = selectedVehicleType?.vehicleTypeId ?: "",
                        rcFile = rcFile,
                        registrationDate = registrationDate,
                        rcExpiryDate = rcExpiryDate,
                        onSaved = onVehicleAdded
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRegistrationDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = registrationDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showRegistrationDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        registrationDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showRegistrationDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRegistrationDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRcExpiryDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = rcExpiryDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showRcExpiryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        rcExpiryDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showRcExpiryDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRcExpiryDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
