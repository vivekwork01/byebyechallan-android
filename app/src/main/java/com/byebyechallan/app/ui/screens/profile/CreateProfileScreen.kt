package com.byebyechallan.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    app: ByeByeChallanApp,
    onBack: () -> Unit,
    onProfileCreated: () -> Unit
) {
    val viewModel = viewModel { CreateProfileViewModel(app.profileRepository, app.sessionManager) }
    val state by viewModel.uiState.collectAsState()
    var profileName by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Profile created", Toast.LENGTH_SHORT).show()
            onProfileCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxWidth()) {
            Text(
                text = "Give this profile a name - e.g. \"My Cars\", \"Family\", or a person's name. " +
                    "You can add multiple vehicles under it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text("Profile Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ErrorBanner(state.errorMessage)
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Create Profile",
                isLoading = state.isLoading,
                onClick = { viewModel.createProfile(profileName) }
            )
        }
    }
}
