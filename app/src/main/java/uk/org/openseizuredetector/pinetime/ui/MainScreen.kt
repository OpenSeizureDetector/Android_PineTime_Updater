package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import uk.org.openseizuredetector.pinetime.R
import uk.org.openseizuredetector.pinetime.model.FirmwareRelease
import uk.org.openseizuredetector.pinetime.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    // The UI now only needs to manage the selected device address and dialog visibility.
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var showFirmwareDialog by remember { mutableStateOf(false) }

    val dfuViewModel: DfuViewModel = hiltViewModel()
    val dfuState = dfuViewModel.dfuState.collectAsState().value

    val firmwareViewModel: FirmwareViewModel = hiltViewModel()
    val firmwareState = firmwareViewModel.firmwareState.collectAsState().value
    val downloadState = firmwareViewModel.downloadState.collectAsState().value
    // The firmware URI is now sourced directly from the ViewModel.
    val firmwareUri by firmwareViewModel.downloadedUri.collectAsState()

    // Reset DFU state if a new firmware is selected (by observing the URI)
    LaunchedEffect(firmwareUri) {
        dfuViewModel.resetDfuState()
    }

    // Reset DFU state if a new device is selected
    LaunchedEffect(selectedDevice) {
        dfuViewModel.resetDfuState()
    }

    val postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    LaunchedEffect(postNotificationPermission) {
        postNotificationPermission?.let {
            if (!it.status.isGranted) {
                it.launchPermissionRequest()
            }
        }
    }

    if (showFirmwareDialog) {
        FirmwareDialog(
            firmwareState = firmwareState,
            onDismiss = { showFirmwareDialog = false },
            onFirmwareSelected = { firmwareViewModel.downloadFirmware(it) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.app_name)) })
        }
    ) { paddingValues ->
        if (showScanner) {
            Column(modifier = Modifier.padding(paddingValues)) {
                DeviceScanner(onDeviceSelected = {
                    selectedDevice = it
                    showScanner = false
                })
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    firmwareViewModel.fetchFirmwareIndex()
                    showFirmwareDialog = true
                }) {
                    Text(text = "Select Firmware")
                }

                Button(onClick = { showScanner = true }) {
                    Text(text = "Select BLE Device")
                }

                val canStartDfu = firmwareUri != null &&
                        selectedDevice != null &&
                        (postNotificationPermission?.status?.isGranted ?: true)

                Button(
                    onClick = {
                        dfuViewModel.startDfu(selectedDevice!!, firmwareUri!!)
                    },
                    enabled = canStartDfu
                ) {
                    Text(text = "Start DFU")
                }

                // Dynamic status text area
                val statusText = when {
                    firmwareUri == null && selectedDevice == null -> "Select firmware and device"
                    firmwareUri == null -> "Select firmware"
                    selectedDevice == null -> "Select device"
                    else -> "Ready to start"
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DFU Status Display
                when (val state = dfuState) {
                    is DfuUiState.Waiting -> {
                        Text(text = "Waiting for device...")
                        CircularProgressIndicator()
                    }
                    is DfuUiState.InProgress -> {
                        LinearProgressIndicator(progress = state.progress / 100f)
                        Text(text = "DFU in progress: ${state.progress}%")
                    }
                    is DfuUiState.Success -> {
                        Text(text = "DFU Successful!")
                    }
                    is DfuUiState.Error -> {
                        Text(text = "DFU Error: ${state.message}")
                    }
                    is DfuUiState.Idle -> {
                        // In Idle state, show the selection status
                        Text(text = statusText)
                        // Also show the selected file and device
                        firmwareUri?.let {
                            Text(text = "Firmware: ${it.lastPathSegment}")
                        }
                        selectedDevice?.let {
                            Text(text = "Device: $it")
                        }
                    }
                }

                // Download progress is shown separately and is always visible when downloading
                if (downloadState is DownloadState.Downloading) {
                    LinearProgressIndicator(progress = downloadState.progress / 100f)
                    Text(text = "Downloading firmware: ${downloadState.progress}%")
                }

                if (postNotificationPermission?.status?.isGranted == false) {
                    Text("Notification permission is required to run the DFU process.")
                }
            }
        }
    }
}

@Composable
private fun FirmwareDialog(
    firmwareState: FirmwareUiState,
    onDismiss: () -> Unit,
    onFirmwareSelected: (FirmwareRelease) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Firmware") },
        text = {
            when (firmwareState) {
                is FirmwareUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is FirmwareUiState.Success -> {
                    val recommendedIndex = firmwareState.index.recommendedFw
                    LazyColumn {
                        itemsIndexed(firmwareState.index.releases) { index, release ->
                            val isRecommended = index == recommendedIndex
                            val fontWeight = if (isRecommended) FontWeight.Bold else FontWeight.Normal
                            val text = if (isRecommended) {
                                "${release.version}: ${release.description} (Recommended)"
                            } else {
                                "${release.version}: ${release.description}"
                            }

                            Text(
                                text = text,
                                fontWeight = fontWeight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        onFirmwareSelected(release)
                                        onDismiss()
                                    }.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                is FirmwareUiState.Error -> {
                    Text(firmwareState.message)
                }
                else -> {}
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
