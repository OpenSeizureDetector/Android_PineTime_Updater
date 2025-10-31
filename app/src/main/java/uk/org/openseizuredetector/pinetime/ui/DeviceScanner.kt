package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import uk.org.openseizuredetector.pinetime.viewmodel.DeviceScannerViewModel

@SuppressLint("MissingPermission") // Permissions are handled by the Accompanist library.
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceScanner(
    onDeviceSelected: (String) -> Unit
) {
    val viewModel: DeviceScannerViewModel = hiltViewModel()
    val devices = viewModel.devices.collectAsState().value

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        } else {
            viewModel.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permissionState.allPermissionsGranted) {
            Text(text = "Scanning for devices...")
            LazyColumn {
                items(devices) { device ->
                    // Format the display name to include the MAC address.
                    val name = device.device.name
                    val address = device.device.address
                    val displayName = if (name.isNullOrEmpty()) {
                        address
                    } else {
                        "$name ($address)"
                    }
                    Text(
                        text = displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onDeviceSelected(device.device.address) }
                    )
                }
            }
        } else {
            Text("Bluetooth and Location permissions are required to scan for devices.")
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}
