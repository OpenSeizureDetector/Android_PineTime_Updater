package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showAboutDialog by remember { mutableStateOf(false) }

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

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    // Simple overflow menu with About only
                    var expanded by remember { mutableStateOf(false) }
                    val moreOptionsDesc = stringResource(R.string.more_options)
                    IconButton(onClick = { expanded = true }) {
                        // use a simple vertical ellipsis character as overflow icon
                        Text(
                            text = "⋮",
                            modifier = Modifier.semantics { contentDescription = moreOptionsDesc }
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_about)) }, onClick = {
                            expanded = false
                            showAboutDialog = true
                        })
                    }
                }
            )
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
                // Added descriptive text below the app bar
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Button(onClick = {
                    firmwareViewModel.fetchFirmwareIndex()
                    showFirmwareDialog = true
                }) {
                    Text(text = stringResource(R.string.select_firmware))
                }

                Button(onClick = { showScanner = true }) {
                    Text(text = stringResource(R.string.select_ble_device))
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
                    Text(text = stringResource(R.string.start_dfu))
                }

                // Dynamic status text area
                val statusText = when {
                    firmwareUri == null && selectedDevice == null -> stringResource(R.string.status_select_both)
                    firmwareUri == null -> stringResource(R.string.status_select_firmware)
                    selectedDevice == null -> stringResource(R.string.status_select_device)
                    else -> stringResource(R.string.status_ready)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DFU Status Display
                when (val state = dfuState) {
                    is DfuUiState.Waiting -> {
                        Text(text = stringResource(R.string.waiting_for_device))
                        CircularProgressIndicator()
                    }
                    is DfuUiState.InProgress -> {
                        LinearProgressIndicator(progress = state.progress / 100f)
                        Text(text = "${stringResource(R.string.dfu_in_progress)}: ${state.progress}%")
                    }
                    is DfuUiState.Success -> {
                        Text(text = stringResource(R.string.dfu_successful))
                    }
                    is DfuUiState.Error -> {
                        Text(text = "${stringResource(R.string.dfu_error)}: ${state.message}")
                    }
                    is DfuUiState.Idle -> {
                        // In Idle state, show the selection status
                        Text(text = statusText)
                        // Also show the selected file and device
                        firmwareUri?.let {
                            Text(text = "${stringResource(R.string.firmware)}: ${it.lastPathSegment}")
                        }
                        selectedDevice?.let {
                            Text(text = "${stringResource(R.string.device)}: $it")
                        }
                    }
                }

                // Download progress is shown separately and is always visible when downloading
                if (downloadState is DownloadState.Downloading) {
                    LinearProgressIndicator(progress = downloadState.progress / 100f)
                    Text(text = "${stringResource(R.string.downloading_firmware)}: ${downloadState.progress}%")
                }

                if (postNotificationPermission?.status?.isGranted == false) {
                    Text(stringResource(R.string.notification_permission_error))
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
        title = { Text(stringResource(R.string.dialog_title_select_firmware)) },
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
                            val recommendedText = if (isRecommended) " ${stringResource(R.string.recommended)}" else ""
                            val text = "${release.version}: ${release.description}$recommendedText"

                            Text(
                                text = text,
                                fontWeight = fontWeight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onFirmwareSelected(release)
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp)
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
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
