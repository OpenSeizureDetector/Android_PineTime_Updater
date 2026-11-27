package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import uk.org.openseizuredetector.pinetime.R

/**
 * The permissions needed to talk to the watch. Identical to the list DeviceScanner requests, so the
 * manual flow (scanner first) and the OpenSeizureDetector-started flow (scanner skipped) behave the same.
 */
val BLUETOOTH_PERMISSIONS: List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

/**
 * Asks for [BLUETOOTH_PERMISSIONS] when [visible] and they are not all granted yet. Disappears by itself
 * as soon as they are granted. Same request/settings-fallback behaviour as DeviceScanner.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionDialog(
    permissionState: MultiplePermissionsState,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasRequested by remember { mutableStateOf(false) }
    var requestInFlight by remember { mutableStateOf(false) }
    var resultReceived by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasRequested = true
        resultReceived = true
    }

    // Permission state objects update on resume: wait for it before showing the dialog again
    LaunchedEffect(resultReceived) {
        if (resultReceived) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
            delay(100)
            requestInFlight = false
            resultReceived = false
        }
    }

    val showSettingsFallback = hasRequested && !permissionState.allPermissionsGranted &&
            !permissionState.shouldShowRationale

    if (visible && !permissionState.allPermissionsGranted && !requestInFlight) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_title_bluetooth_permission)) },
            text = {
                Text(
                    stringResource(
                        if (showSettingsFallback) R.string.permissions_required_to_update_settings
                        else R.string.permissions_required_to_update
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (showSettingsFallback) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        } else {
                            requestInFlight = true
                            launcher.launch(BLUETOOTH_PERMISSIONS.toTypedArray())
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (showSettingsFallback) R.string.open_settings else R.string.grant_permissions
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
