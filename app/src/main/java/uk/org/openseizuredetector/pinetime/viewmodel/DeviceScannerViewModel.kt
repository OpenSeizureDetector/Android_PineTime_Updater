package uk.org.openseizuredetector.pinetime.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("MissingPermission") // Permissions are requested in the UI layer.
@HiltViewModel
class DeviceScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _devices = MutableStateFlow<List<ScanResult>>(emptyList())
    val devices = _devices.asStateFlow()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            viewModelScope.launch {
                val currentDevices = _devices.value.toMutableList()
                val existingDevice = currentDevices.find { it.device.address == result.device.address }

                // Add device only if it's new and has a name.
                if (existingDevice == null && result.device.name != null) {
                    currentDevices.add(result)
                    _devices.value = currentDevices
                }
            }
        }
    }

    fun startScan() {
        scanner?.startScan(scanCallback)
    }

    override fun onCleared() {
        super.onCleared()
        scanner?.stopScan(scanCallback)
    }
}
