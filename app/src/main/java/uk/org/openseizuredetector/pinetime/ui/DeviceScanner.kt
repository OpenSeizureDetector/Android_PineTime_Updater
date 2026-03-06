package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import uk.org.openseizuredetector.pinetime.R
import uk.org.openseizuredetector.pinetime.viewmodel.DeviceScannerViewModel

@SuppressLint("MissingPermission") // Permissions are requested before starting scans.
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceScanner(
    onDismiss: () -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: DeviceScannerViewModel = hiltViewModel()
    val devices = viewModel.devices.collectAsState().value
    var selectedAddress by remember { mutableStateOf<String?>(null) }

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // We use Accompanist only for reading the state/rationale status
    val permissionState = rememberMultiplePermissionsState(requiredPermissions)

    var hasRequested by remember { mutableStateOf(false) }
    var permissionRequestInFlight by remember { mutableStateOf(false) }
    var requestResultReceived by remember { mutableStateOf(false) }

    // Use our own launcher to know exactly when the batch request is finished
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // The system dialog flow is complete.
        hasRequested = true
        requestResultReceived = true
    }

    // When result is received, wait for Resume (state update) then un-hide dialog
    LaunchedEffect(requestResultReceived) {
        if (requestResultReceived) {
            // Wait for app to be in RESUMED state (permission state updates happen on resume)
            lifecycleOwner.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }

            // Allow a brief moment for permission state objects to update
            delay(100)

            permissionRequestInFlight = false
            requestResultReceived = false
        }
    }

    val showSettingsFallback = hasRequested && !permissionState.allPermissionsGranted &&
            !permissionState.shouldShowRationale

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.startScan()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    val showDialog = !permissionState.allPermissionsGranted && !permissionRequestInFlight

    if (permissionState.allPermissionsGranted) {
        val items = devices.map { device ->
            val address = device.device.address
            SelectionDialogItem(
                id = address,
                title = device.device.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unnamed_watch),
                supportingText = address
            )
        }

        SingleChoiceSelectionDialog(
            title = stringResource(R.string.dialog_title_select_watch),
            hint = stringResource(R.string.select_watch_hint_dialog),
            items = items,
            selectedItemId = selectedAddress,
            onItemSelected = { selectedAddress = it },
            onDismiss = onDismiss,
            onConfirm = { selectedAddress?.let(onDeviceSelected) },
            confirmText = stringResource(R.string.action_select),
            dismissText = stringResource(R.string.action_cancel),
            confirmEnabled = selectedAddress != null,
            emptyStateContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(text = stringResource(R.string.scanning_for_devices))
                }
            }
        )
    } else if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_title_select_watch)) },
            text = {
                Text(
                    text = if (showSettingsFallback) {
                        stringResource(R.string.permissions_required_to_scan_settings)
                    } else {
                        stringResource(R.string.permissions_required_to_scan)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (showSettingsFallback) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        } else {
                            // Hide dialog immediately (because permissionRequestInFlight becomes true)
                            permissionRequestInFlight = true
                            permissionLauncher.launch(requiredPermissions.toTypedArray())
                        }
                    }
                ) {
                    Text(
                        text = if (showSettingsFallback) {
                            stringResource(R.string.open_settings)
                        } else {
                            stringResource(R.string.grant_permissions)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
