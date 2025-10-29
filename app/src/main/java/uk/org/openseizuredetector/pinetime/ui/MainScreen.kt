package uk.org.openseizuredetector.pinetime.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.org.openseizuredetector.pinetime.BleDevice
import uk.org.openseizuredetector.pinetime.UiState
import uk.org.openseizuredetector.pinetime.net.FirmwareItem

@Composable
fun MainScreen(
    state: UiState,
    onRequestPermissions: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelectDevice: (BleDevice) -> Unit,
    onFetchFirmwares: () -> Unit,
    onSelectFirmware: (FirmwareItem) -> Unit,
    onDownloadFirmware: () -> Unit,
    onStartUpdate: () -> Unit,
    onCancelUpdate: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {

        Text("PineTime DFU Updater", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))

        Row {
            Button(onClick = onRequestPermissions, modifier = Modifier.padding(end = 8.dp)) {
                Text("Grant Permissions")
            }
            Button(onClick = onToggleBluetooth) {
                Text("Enable Bluetooth")
            }
        }
        Text("Bluetooth enabled: ${state.bluetoothEnabled}")
        Text("Permissions OK: ${state.hasScanPermission}")

        Spacer(Modifier.height(16.dp))

        Text("1) Select DFU Device", style = MaterialTheme.typography.titleMedium)
        Row {
            if (!state.scanning) {
                Button(onClick = onStartScan, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Scan for DFU Devices")
                }
            } else {
                Button(onClick = onStopScan, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Stop Scan")
                }
            }
        }
        if (state.dfuDevices.isEmpty()) {
            Text("No DFU devices found yet.")
        } else {
            LazyColumn(Modifier.heightIn(max = 200.dp)) {
                items(state.dfuDevices) { dev ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Button(onClick = { onSelectDevice(dev) }) {
                            Text("${dev.name ?: "(Unnamed)"} - ${dev.address}")
                        }
                    }
                }
            }
        }
        Text("Selected device: ${state.selectedDevice?.let { it.name ?: it.address } ?: "None"}")

        Spacer(Modifier.height(16.dp))

        Text("2) Select and Download Firmware", style = MaterialTheme.typography.titleMedium)
        Row {
            Button(onClick = onFetchFirmwares, modifier = Modifier.padding(end = 8.dp)) {
                Text("Load Firmware List")
            }
        }
        Text(state.firmwareIndexStatus)
        if (state.firmwareItems.isNotEmpty()) {
            LazyColumn(Modifier.heightIn(max = 220.dp)) {
                items(state.firmwareItems) { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Button(onClick = { onSelectFirmware(item) }) {
                            val label = buildString {
                                append(item.name)
                                item.version?.let { append(" v$it") }
                            }
                            Text(label)
                        }
                    }
                }
            }
        }
        Text("Selected firmware: ${state.selectedFirmware?.name ?: "None"}")
        Row {
            Button(
                onClick = onDownloadFirmware,
                enabled = state.selectedFirmware != null,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Text("Download Selected Firmware")
            }
        }
        if (state.downloadStatus.isNotBlank()) {
            Text("${state.downloadStatus} (${state.downloadProgress}%)")
        }

        Spacer(Modifier.height(16.dp))

        Text("3) Start Update", style = MaterialTheme.typography.titleMedium)
        Row {
            Button(
                onClick = onStartUpdate,
                enabled = state.selectedDevice != null && state.downloadedZipPath != null && !state.dfuInProgress,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Start Update")
            }
            Button(
                onClick = onCancelUpdate,
                enabled = state.dfuInProgress
            ) {
                Text("Cancel")
            }
        }
        Text("DFU Status: ${state.dfuStatus}")
        if (state.dfuInProgress) {
            LinearProgressIndicator(
                progress = state.dfuProgressPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        state.dfuCompletedMessage?.let {
            Text("Result: $it")
        }
    }
}
