package uk.org.openseizuredetector.pinetime

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.getSystemService
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import uk.org.openseizuredetector.pinetime.ui.MainScreen

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val dfuProgressListener = object : DfuServiceListenerHelper.DfuProgressListenerAdapter() {
        override fun onDeviceConnected(deviceAddress: String) {
            viewModel.onDfuEvent("Connected to $deviceAddress")
        }
        override fun onDeviceDisconnected(deviceAddress: String) {
            viewModel.onDfuEvent("Disconnected from $deviceAddress")
        }
        override fun onDfuProcessStarted(deviceAddress: String) {
            viewModel.onDfuEvent("DFU started")
        }
        override fun onDfuProcessStarting(deviceAddress: String) {
            viewModel.onDfuEvent("DFU starting")
        }
        override fun onEnablingDfuMode(deviceAddress: String) {
            viewModel.onDfuEvent("Enabling DFU mode")
        }
        override fun onFirmwareValidating(deviceAddress: String) {
            viewModel.onDfuEvent("Validating firmware")
        }
        override fun onDeviceNotSupported(deviceAddress: String) {
            viewModel.onDfuError("Device not supported")
        }
        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String) {
            viewModel.onDfuError("Error: $message")
        }
        override fun onProgressChanged(
            deviceAddress: String,
            percent: Int,
            speed: Float,
            avgSpeed: Float,
            currentPart: Int,
            partsTotal: Int
        ) {
            viewModel.onDfuProgress(percent, currentPart, partsTotal)
        }
        override fun onDfuCompleted(deviceAddress: String) {
            viewModel.onDfuCompleted("DFU completed successfully")
        }
        override fun onDfuAborted(deviceAddress: String) {
            viewModel.onDfuError("DFU aborted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = viewModel(factory = MainViewModel.factory(application))
        setContent {
            MaterialTheme {
                MainScreen(
                    state = viewModel.uiState.collectAsState().value,
                    onRequestPermissions = { requestNeededPermissions() },
                    onToggleBluetooth = { toggleBluetoothEnabled() },
                    onStartScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onSelectDevice = { viewModel.selectDevice(it) },
                    onFetchFirmwares = { viewModel.fetchFirmwareIndex() },
                    onSelectFirmware = { viewModel.selectFirmware(it) },
                    onDownloadFirmware = { viewModel.downloadSelectedFirmware() },
                    onStartUpdate = { viewModel.startDfu(this) },
                    onCancelUpdate = { viewModel.cancelDfu() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener)
    }

    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(this, dfuProgressListener)
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun toggleBluetoothEnabled() {
        val manager = getSystemService<BluetoothManager>()
        val adapter = manager?.adapter
        if (adapter != null && !adapter.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}
