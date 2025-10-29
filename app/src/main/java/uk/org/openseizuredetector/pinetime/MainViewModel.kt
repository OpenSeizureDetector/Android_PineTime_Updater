package uk.org.openseizuredetector.pinetime

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.dfu.DfuServiceController
import no.nordicsemi.android.dfu.DfuServiceInitiator
import uk.org.openseizuredetector.pinetime.dfu.DfuService
import uk.org.openseizuredetector.pinetime.net.FirmwareIndexLoader
import uk.org.openseizuredetector.pinetime.net.FirmwareItem
import uk.org.openseizuredetector.pinetime.net.FirmwareDownloader
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class UiState(
    val bluetoothEnabled: Boolean = false,
    val hasScanPermission: Boolean = false,
    val scanning: Boolean = false,
    val dfuDevices: List<BleDevice> = emptyList(),
    val selectedDevice: BleDevice? = null,

    val firmwareItems: List<FirmwareItem> = emptyList(),
    val firmwareIndexStatus: String = "",
    val selectedFirmware: FirmwareItem? = null,
    val downloadStatus: String = "",
    val downloadProgress: Int = 0,
    val downloadedZipPath: String? = null,

    val dfuStatus: String = "",
    val dfuProgressPercent: Int = 0,
    val dfuInProgress: Boolean = false,
    val dfuCompletedMessage: String? = null
)

data class BleDevice(
    val name: String?,
    val address: String
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    companion object {
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = (modelClass.classLoader!!
                    .loadClass("android.app.AppGlobals")
                    .getMethod("getInitialApplication")
                    .invoke(null) as Application)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
        }

        private val DFU_SERVICE_UUID: UUID =
            UUID.fromString("00001530-1212-EFDE-1523-785FEABCD123")
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private val foundDevices = ConcurrentHashMap<String, BleDevice>()

    private var dfuController: DfuServiceController? = null

    fun startScan() {
        updateBluetoothState()
        if (!_uiState.value.hasScanPermission) {
            setStatus("Grant required permissions to scan.")
            return
        }
        if (!_uiState.value.bluetoothEnabled) {
            setStatus("Enable Bluetooth to scan.")
            return
        }

        val manager = app.getSystemService<BluetoothManager>()
        val adapter = manager?.adapter ?: return
        scanner = adapter.bluetoothLeScanner ?: return

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(DFU_SERVICE_UUID)).build(),
            ScanFilter.Builder().setDeviceName("DfuTarg").build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        foundDevices.clear()
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val name = device.name
                val hasDfuService =
                    result.scanRecord?.serviceUuids?.contains(ParcelUuid(DFU_SERVICE_UUID)) == true
                val isDfuName = name?.contains("DfuTarg", ignoreCase = true) == true
                if (hasDfuService || isDfuName) {
                    val bdAddr = device.address ?: return
                    foundDevices[bdAddr] = BleDevice(name = name, address = bdAddr)
                    _uiState.value = _uiState.value.copy(
                        dfuDevices = foundDevices.values.sortedBy { it.name ?: it.address }
                    )
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                setStatus("Scan failed: $errorCode")
                _uiState.value = _uiState.value.copy(scanning = false)
            }
        }

        scanner?.startScan(filters, settings, scanCallback)
        _uiState.value = _uiState.value.copy(scanning = true)

        scope.launch {
            delay(10_000)
            stopScan()
        }
    }

    fun stopScan() {
        scanCallback?.let { cb -> scanner?.stopScan(cb) }
        _uiState.value = _uiState.value.copy(scanning = false)
    }

    fun selectDevice(device: BleDevice) {
        _uiState.value = _uiState.value.copy(selectedDevice = device)
    }

    private fun updateBluetoothState() {
        val manager = app.getSystemService<BluetoothManager>()
        val isEnabled = manager?.adapter?.isEnabled == true
        val hasPerm = hasScanPermissions(app)
        _uiState.value = _uiState.value.copy(
            bluetoothEnabled = isEnabled,
            hasScanPermission = hasPerm
        )
    }

    private fun hasScanPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setStatus(message: String) {
        _uiState.value = _uiState.value.copy(firmwareIndexStatus = message)
    }

    fun fetchFirmwareIndex() {
        scope.launch {
            _uiState.value = _uiState.value.copy(firmwareIndexStatus = "Loading firmware list...")
            val items = withContext(Dispatchers.IO) {
                FirmwareIndexLoader.load("https://osdapi.org.uk/static/pineTimeSdFw/index.json")
            }
            if (items.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    firmwareIndexStatus = "No firmware found or failed to load.",
                    firmwareItems = emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    firmwareIndexStatus = "Select a firmware",
                    firmwareItems = items
                )
            }
        }
    }

    fun selectFirmware(item: FirmwareItem) {
        _uiState.value = _uiState.value.copy(
            selectedFirmware = item,
            downloadedZipPath = null,
            downloadStatus = ""
        )
    }

    fun downloadSelectedFirmware() {
        val fw = _uiState.value.selectedFirmware ?: run {
            _uiState.value = _uiState.value.copy(downloadStatus = "Select firmware first.")
            return
        }

        scope.launch {
            _uiState.value = _uiState.value.copy(downloadStatus = "Downloading...", downloadProgress = 0)
            val outFile = java.io.File(app.cacheDir, "fw_${fw.safeName()}.zip")
            val ok = withContext(Dispatchers.IO) {
                FirmwareDownloader.downloadWithProgress(
                    fw.url,
                    outFile,
                ) { bytesRead, contentLength ->
                    val pct = if (contentLength > 0L) {
                        ((bytesRead * 100L) / contentLength).toInt().coerceIn(0, 100)
                    } else 0
                    _uiState.value = _uiState.value.copy(downloadProgress = pct)
                }
            }
            if (ok) {
                _uiState.value = _uiState.value.copy(
                    downloadStatus = "Downloaded: ${outFile.name}",
                    downloadedZipPath = outFile.absolutePath
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    downloadStatus = "Download failed",
                    downloadedZipPath = null
                )
            }
        }
    }

    fun startDfu(context: Context) {
        val dev = _uiState.value.selectedDevice
        val zipPath = _uiState.value.downloadedZipPath
        if (dev == null || zipPath == null) {
            _uiState.value = _uiState.value.copy(dfuStatus = "Select device and download firmware first.")
            return
        }

        val initiator = DfuServiceInitiator(dev.address)
            .setDeviceName(dev.name ?: "DFU Device")
            .setKeepBond(false)
            .setDisableNotification(false)
            .setForeground(true)
            .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)

        initiator.setZip(zipPath)
        _uiState.value = _uiState.value.copy(
            dfuStatus = "Starting DFU...",
            dfuInProgress = true,
            dfuProgressPercent = 0,
            dfuCompletedMessage = null
        )
        dfuController = initiator.start(context, DfuService::class.java)
    }

    fun cancelDfu() {
        dfuController?.abort()
        _uiState.value = _uiState.value.copy(dfuStatus = "Cancelling DFU...", dfuInProgress = false)
    }

    fun onDfuProgress(percent: Int, part: Int, total: Int) {
        _uiState.value = _uiState.value.copy(
            dfuProgressPercent = percent,
            dfuStatus = "Updating ($part/$total): $percent%"
        )
    }

    fun onDfuEvent(message: String) {
        _uiState.value = _uiState.value.copy(
            dfuStatus = message
        )
    }

    fun onDfuError(message: String) {
        _uiState.value = _uiState.value.copy(
            dfuInProgress = false,
            dfuStatus = message
        )
    }

    fun onDfuCompleted(message: String) {
        _uiState.value = _uiState.value.copy(
            dfuInProgress = false,
            dfuStatus = message,
            dfuCompletedMessage = message
        )
    }

    override fun onCleared() {
        super.onCleared()
        try {
            stopScan()
        } catch (_: Throwable) { }
    }
}

private fun uk.org.openseizuredetector.pinetime.net.FirmwareItem.safeName(): String {
    return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
