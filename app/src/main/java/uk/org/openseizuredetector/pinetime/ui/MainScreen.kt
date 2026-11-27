package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import uk.org.openseizuredetector.pinetime.MainActivity
import uk.org.openseizuredetector.pinetime.R
import uk.org.openseizuredetector.pinetime.model.FirmwareIndex
import uk.org.openseizuredetector.pinetime.model.FirmwareRelease
import uk.org.openseizuredetector.pinetime.model.OsdFirmwareRules
import uk.org.openseizuredetector.pinetime.model.OsdLaunchRequest
import uk.org.openseizuredetector.pinetime.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(osdRequest: OsdLaunchRequest? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // The UI now only needs to manage the selected device address and dialog visibility.
    var selectedDevice by remember { mutableStateOf<String?>(osdRequest?.macAddress?.takeIf { it.isNotBlank() }) }
    var showWatchDialog by remember { mutableStateOf(false) }
    var showFirmwareDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }
    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }
    var permissionRequestInFlight by remember { mutableStateOf(false) }
    var requestResultReceived by remember { mutableStateOf(false) }

    // Bluetooth ("Nearby devices") permissions. In the manual flow DeviceScanner has already obtained them;
    // when started from OpenSeizureDetector the scanner is skipped, so ask here, before DFU can be started.
    val bluetoothPermissionState = rememberMultiplePermissionsState(BLUETOOTH_PERMISSIONS)
    var showBluetoothPermissionDialog by remember { mutableStateOf(osdRequest != null) }

    val dfuViewModel: DfuViewModel = hiltViewModel()
    val dfuState = dfuViewModel.dfuState.collectAsState().value

    val firmwareViewModel: FirmwareViewModel = hiltViewModel()
    val firmwareState = firmwareViewModel.firmwareState.collectAsState().value
    val downloadState = firmwareViewModel.downloadState.collectAsState().value
    // The firmware URI is now sourced directly from the ViewModel.
    val firmwareUri by firmwareViewModel.downloadedUri.collectAsState()

    // --- Started from the OpenSeizureDetector app: offer only the one qualifying release ---
    // Qualifies = marked recommended AND newer than the version installed on the watch.
    val osdRelease: FirmwareRelease? = remember(firmwareState, osdRequest) {
        if (osdRequest != null && firmwareState is FirmwareUiState.Success)
            OsdFirmwareRules.qualifyingRelease(firmwareState.index, osdRequest.installedVersion)
        else null
    }
    // The list shown in the firmware dialog: just that one release (or none).
    val visibleFirmwareState: FirmwareUiState =
        if (osdRequest != null && firmwareState is FirmwareUiState.Success)
            FirmwareUiState.Success(FirmwareIndex(releases = listOfNotNull(osdRelease), recommendedFw = 0))
        else firmwareState
    LaunchedEffect(osdRequest) {
        if (osdRequest != null) firmwareViewModel.fetchFirmwareIndex()
    }
    LaunchedEffect(osdRelease) {
        // Download it right away so that only "Start DFU" is left for the user.
        osdRelease?.let { firmwareViewModel.downloadFirmware(it) }
    }

    // Reset DFU state if a new firmware is selected (by observing the URI)
    LaunchedEffect(firmwareUri) {
        dfuViewModel.resetDfuState()
    }

    // Reset DFU state if a new device is selected
    LaunchedEffect(selectedDevice) {
        dfuViewModel.resetDfuState()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hasRequestedNotificationPermission = true
        requestResultReceived = true
    }

    val postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    val isNotificationPermissionPermanentlyDenied =
        hasRequestedNotificationPermission &&
            (postNotificationPermission?.status as? PermissionStatus.Denied)?.shouldShowRationale == false

    LaunchedEffect(requestResultReceived) {
        if (requestResultReceived) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
            delay(100)
            permissionRequestInFlight = false
            requestResultReceived = false

            if (postNotificationPermission?.status?.isGranted == true) {
                notificationPermissionDenied = false
                val device = selectedDevice
                val firmware = firmwareUri
                if (device != null && firmware != null) {
                    dfuViewModel.startDfu(device, firmware)
                }
            } else {
                notificationPermissionDenied = true
                showNotificationPermissionDialog = true
            }
        }
    }

    if (showFirmwareDialog) {
        FirmwareDialog(
            firmwareState = visibleFirmwareState,
            onDismiss = { showFirmwareDialog = false },
            onFirmwareSelected = { firmwareViewModel.downloadFirmware(it) }
        )
    }

    if (showWatchDialog) {
        DeviceScanner(
            onDismiss = { showWatchDialog = false },
            onDeviceSelected = {
                selectedDevice = it
                showWatchDialog = false
                Log.d("MainScreen", "Device selected: $it. Attempting to set Activity result via MainActivity helper.")
                (context as? MainActivity)?.setMacAddressResult(it)
                    ?: Log.w("MainScreen", "Context is not MainActivity, cannot set result via helper")
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    BluetoothPermissionDialog(
        permissionState = bluetoothPermissionState,
        visible = showBluetoothPermissionDialog,
        onDismiss = { showBluetoothPermissionDialog = false }
    )

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = { Text(stringResource(R.string.dialog_title_notification_permission)) },
            text = {
                Text(
                    if (isNotificationPermissionPermanentlyDenied) {
                        stringResource(R.string.notification_permission_rationale_settings)
                    } else {
                        stringResource(R.string.notification_permission_rationale)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isNotificationPermissionPermanentlyDenied) {
                            showNotificationPermissionDialog = false
                            val settingsIntent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(settingsIntent)
                        } else {
                            showNotificationPermissionDialog = false
                            permissionRequestInFlight = true
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text(
                        if (isNotificationPermissionPermanentlyDenied) {
                            stringResource(R.string.open_settings)
                        } else {
                            stringResource(R.string.action_grant_notifications)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.star_of_life_48x48),
                            contentDescription = stringResource(R.string.osd_logo_content_description),
                            modifier = Modifier
                                .size(32.dp) // Adjust size as needed
                                .padding(end = 8.dp)
                            )
                            Text(stringResource(id = R.string.app_name))
                    }
                        },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    // Simple overflow menu with About only
                    var expanded by remember { mutableStateOf(false) }
                    val moreOptionsDesc = stringResource(R.string.more_options)
                    IconButton(onClick = { expanded = true }) {
                        // use a simple vertical ellipsis character as overflow icon
                        Text(
                            text = "⋮",
                            // Set the color to be the same as the title color
                            color = MaterialTheme.colorScheme.onPrimary,
                            // Make the icon a bit larger and bolder to improve visibility
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
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
        val hasDfuPrerequisites = firmwareUri != null && selectedDevice != null
        val canStartDfu = hasDfuPrerequisites

        val firmwareStatusText = when (val state = downloadState) {
            is DownloadState.Downloading ->
                "${stringResource(R.string.downloading_firmware)}: ${state.progress}%"
            is DownloadState.Error ->
                "${stringResource(R.string.download_error)}: ${state.message}"
            else -> firmwareUri?.lastPathSegment?.let {
                stringResource(R.string.status_selected_value, it)
            } ?: if (osdRequest != null && firmwareState is FirmwareUiState.Success && osdRelease == null) {
                stringResource(R.string.osd_firmware_up_to_date)
            } else {
                stringResource(R.string.status_no_software_selected)
            }
        }
        val firmwareHasTick = firmwareUri != null && downloadState !is DownloadState.Error

        val deviceStatusText = selectedDevice?.let {
            stringResource(R.string.status_selected_value, it)
        } ?: stringResource(R.string.status_no_watch_selected)
        val deviceHasTick = selectedDevice != null

        val installStatusText = when (val state = dfuState) {
            is DfuUiState.Waiting -> stringResource(R.string.waiting_for_device)
            is DfuUiState.InProgress ->
                "${stringResource(R.string.dfu_in_progress)}: ${state.progress}%"
            is DfuUiState.Success -> stringResource(R.string.dfu_successful)
            is DfuUiState.Error ->
                "${stringResource(R.string.dfu_error)}: ${state.message}"
            is DfuUiState.Idle -> {
                if (canStartDfu) stringResource(R.string.status_ready)
                else stringResource(R.string.status_not_ready)
            }
        }
        val installHasTick = dfuState is DfuUiState.Success ||
            (dfuState is DfuUiState.Idle && canStartDfu)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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

            ActionButtonRow(
                buttonLabelRes = R.string.select_ble_device,
                statusText = deviceStatusText,
                showTick = deviceHasTick,
                onClick = { showWatchDialog = true }
            )

            ActionButtonRow(
                buttonLabelRes = R.string.select_firmware,
                statusText = firmwareStatusText,
                showTick = firmwareHasTick,
                enabled = osdRequest == null || osdRelease != null,
                onClick = {
                    firmwareViewModel.fetchFirmwareIndex()
                    showFirmwareDialog = true
                }
            )

            ActionButtonRow(
                buttonLabelRes = R.string.start_dfu,
                statusText = installStatusText,
                showTick = installHasTick,
                enabled = canStartDfu,
                onClick = {
                    val device = selectedDevice
                    val firmware = firmwareUri
                    if (device == null || firmware == null) return@ActionButtonRow

                    // Never start the DFU without the Bluetooth permissions (the service would crash)
                    if (!bluetoothPermissionState.allPermissionsGranted) {
                        showBluetoothPermissionDialog = true
                        return@ActionButtonRow
                    }

                    if (postNotificationPermission == null || postNotificationPermission.status.isGranted) {
                        notificationPermissionDenied = false
                        dfuViewModel.startDfu(device, firmware)
                    } else {
                        showNotificationPermissionDialog = true
                    }
                }
            )

            if (notificationPermissionDenied && postNotificationPermission?.status?.isGranted == false) {
                Text(stringResource(R.string.notification_permission_error))
            }
        }
    }
}

@Composable
private fun ActionButtonRow(
    buttonLabelRes: Int,
    statusText: String,
    showTick: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 400 || configuration.fontScale > 1.1f

    if (isCompact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(buttonLabelRes), textAlign = TextAlign.Center)
            }

            ActionStatusText(
                text = statusText,
                showTick = showTick,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(buttonLabelRes), textAlign = TextAlign.Center)
            }

            ActionStatusText(
                text = statusText,
                showTick = showTick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionStatusText(
    text: String,
    showTick: Boolean,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp)
) {
    val selectedIconDescription = stringResource(R.string.status_selected_icon_content_description)

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTick) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = selectedIconDescription
                }
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FirmwareDialog(
    firmwareState: FirmwareUiState,
    onDismiss: () -> Unit,
    onFirmwareSelected: (FirmwareRelease) -> Unit
) {
    var selectedReleaseId by remember(firmwareState) { mutableStateOf<String?>(null) }

    when (firmwareState) {
        is FirmwareUiState.Loading -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.dialog_title_select_firmware)) },
                text = { CircularProgressIndicator() },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        is FirmwareUiState.Success -> {
            val releasesById = firmwareState.index.releases.associateBy { it.fname }
            val items = firmwareState.index.releases.mapIndexed { index, release ->
                val isRecommended = index == firmwareState.index.recommendedFw
                val supportingText = buildString {
                    append(release.description)
                    if (isRecommended) {
                        append(" ")
                        append(stringResource(R.string.recommended))
                    }
                }
                SelectionDialogItem(
                    id = release.fname,
                    title = release.version,
                    supportingText = supportingText
                )
            }

            SingleChoiceSelectionDialog(
                title = stringResource(R.string.dialog_title_select_firmware),
                hint = stringResource(R.string.select_firmware_hint),
                items = items,
                selectedItemId = selectedReleaseId,
                onItemSelected = { selectedReleaseId = it },
                onDismiss = onDismiss,
                onConfirm = {
                    selectedReleaseId
                        ?.let { releasesById[it] }
                        ?.let(onFirmwareSelected)
                    onDismiss()
                },
                confirmText = stringResource(R.string.action_select),
                dismissText = stringResource(R.string.action_cancel),
                confirmEnabled = selectedReleaseId != null
            )
        }

        is FirmwareUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.dialog_title_select_firmware)) },
                text = { Text(firmwareState.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }

        else -> Unit
    }
}
