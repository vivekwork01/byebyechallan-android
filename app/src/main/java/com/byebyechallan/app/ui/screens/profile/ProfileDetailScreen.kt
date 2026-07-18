package com.byebyechallan.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.runtime.LaunchedEffect
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.data.local.LocalVehicle
import com.byebyechallan.app.ui.components.EmptyState
import com.byebyechallan.app.ui.components.FullScreenLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    app: ByeByeChallanApp,
    navController: NavHostController,
    profileId: Long,
    profileName: String,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onVehicleClick: (LocalVehicle) -> Unit
) {
    val viewModel = viewModel {
        ProfileDetailViewModel(
            profileId,
            app.profileRepository,
            app.sessionManager,
            app.vehicleLocalStore
        )
    }
    val state by viewModel.uiState.collectAsState()

    // Observe flag set by AddVehicle and refresh vehicles when requested
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val refresh = backStackEntry.savedStateHandle.get<Boolean>("refreshVehicles") ?: false
            if (refresh) {
                viewModel.loadVehicles()
                backStackEntry.savedStateHandle.set("refreshVehicles", false)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Vehicle")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> FullScreenLoading()
                state.vehicles.isEmpty() -> EmptyState(
                    "No vehicles yet under this profile. Tap \"Add Vehicle\" to get started."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.vehicles) { vehicle ->
                        VehicleRow(vehicle = vehicle, onClick = { onVehicleClick(vehicle) })
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(vehicle: LocalVehicle, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.registrationNo, style = MaterialTheme.typography.titleMedium)
            vehicle.vehicleName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = vehicle.vehicleType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
