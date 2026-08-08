package com.byebyechallan.app.ui.screens.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.PrimaryButton
import com.byebyechallan.app.data.model.VehicleSummary

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

    val context = LocalContext.current

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
                    viewModel.saveVehicle(
                        registrationNo = registrationNo,
                        vehicleName = vehicleName,
                        country = selectedCountry?.countryId ?: "",
                        state = selectedState?.stateId ?: "",
                        registrationType = selectedRegType?.registrationCode ?: "",
                        vehicleTypeId = selectedVehicleType?.vehicleTypeId ?: "",
                        onSaved = onVehicleAdded
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
