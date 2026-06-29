package com.byebyechallan.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.ui.components.EmptyState
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.FullScreenLoading
import com.byebyechallan.app.ui.theme.AmberWarning
import com.byebyechallan.app.ui.theme.RedExpired
import com.byebyechallan.app.util.DateUtils
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: ByeByeChallanApp,
    onProfileClick: (id: Long, name: String) -> Unit,
    onAddProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = viewModel {
        HomeViewModel(
            app.profileRepository,
            app.documentRepository,
            app.vehicleLocalStore,
            app.sessionManager
        )
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Profiles") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    app.authRepository.logout()
                                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                    onLogout()
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddProfileClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Profile")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> FullScreenLoading()
                state.errorMessage != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    ErrorBanner(state.errorMessage)
                }
                state.profiles.isEmpty() -> EmptyState("No profiles yet. Tap \"Add Profile\" to create your first one.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.profiles) { card ->
                        ProfileCard(
                            data = card,
                            onClick = { onProfileClick(card.profile.id, card.profile.profileName) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(data: ProfileCardData, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(data.profile.profileName, style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${data.vehicleCount} vehicle${if (data.vehicleCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val doc = data.soonestExpiringDoc
            if (doc != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val expired = DateUtils.isExpired(doc.expiryDate)
                val soon = DateUtils.isExpiringSoon(doc.expiryDate)
                val tint = if (expired) RedExpired else if (soon) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (expired || soon) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "${doc.docName ?: "Document"} expires ${DateUtils.formatForDisplay(doc.expiryDate)}" +
                            if (expired) " (Expired)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint
                    )
                }
            }
        }
    }
}
