package com.byebyechallan.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.byebyechallan.app.data.model.NotificationRecipientState
import com.byebyechallan.app.ui.components.ErrorBanner
import com.byebyechallan.app.ui.components.FullScreenLoading
import com.byebyechallan.app.ui.components.NotificationRecipientsSection
import com.byebyechallan.app.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    app: ByeByeChallanApp,
    profileId: Long,
    onBack: () -> Unit,
    onProfileUpdated: (String) -> Unit
) {
    val viewModel = viewModel {
        EditProfileViewModel(profileId, app.profileRepository, app.sessionManager)
    }
    val state by viewModel.uiState.collectAsState()
    var profileName by remember { mutableStateOf("") }
    var recipients by remember { mutableStateOf(NotificationRecipientState()) }

    val context = LocalContext.current

    LaunchedEffect(state.profileName, state.recipients) {
        val loadedName = state.profileName
        if (loadedName != null) {
            profileName = loadedName
        }
        state.recipients?.let { recipients = it }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            onProfileUpdated(profileName.trim())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.profileName == null -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    FullScreenLoading()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    NotificationRecipientsSection(
                        state = recipients,
                        onStateChange = { recipients = it },
                        defaultEmail = state.defaultEmail,
                        defaultMobile = state.defaultMobile
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ErrorBanner(state.errorMessage)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "Save Changes",
                        isLoading = state.isLoading,
                        onClick = { viewModel.updateProfile(profileName, recipients) }
                    )
                }
            }
        }
    }
}
