package com.byebyechallan.app.ui.screens.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.data.model.DocumentChecklistItem
import com.byebyechallan.app.ui.components.EmptyState
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.FullScreenLoading
import com.byebyechallan.app.ui.theme.AmberWarning
import com.byebyechallan.app.ui.theme.RedExpired
import com.byebyechallan.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    app: ByeByeChallanApp,
    profileId: Long,
    vehicleRegNo: String,
    country: String,
    state: String,
    registrationType: String,
    vehicleType: String,
    onBack: () -> Unit,
    onDocumentClick: (DocumentChecklistItem) -> Unit
) {
    val viewModel = viewModel {
        VehicleDetailViewModel(
            profileId, vehicleRegNo, country, state, registrationType, vehicleType,
            app.documentRepository, app.sessionManager
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicleRegNo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.errorMessage != null -> Column(modifier = Modifier.padding(24.dp)) {
                    ErrorBanner(uiState.errorMessage)
                }
                uiState.items.isEmpty() -> EmptyState(
                    "No document checklist found for this vehicle's country/state/type combination."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items) { item ->
                        DocumentRow(item = item, onClick = { onDocumentClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(item: DocumentChecklistItem, onClick: () -> Unit) {
    val expired = DateUtils.isExpired(item.uploaded?.expiryDate)
    val soon = DateUtils.isExpiringSoon(item.uploaded?.expiryDate)

    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isUploaded) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (item.isUploaded) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.template.docName, style = MaterialTheme.typography.titleMedium)
                    if (item.isMandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusChip(label = "Mandatory", color = RedExpired)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                if (item.isUploaded) {
                    val tint = if (expired) RedExpired else if (soon) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        text = "Expires ${DateUtils.formatForDisplay(item.uploaded?.expiryDate)}" + if (expired) " (Expired)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint
                    )
                } else {
                    Text(
                        text = "Not uploaded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
