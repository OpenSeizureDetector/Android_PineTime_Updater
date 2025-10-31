package uk.org.openseizuredetector.pinetime.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import uk.org.openseizuredetector.pinetime.service.DfuService
import javax.inject.Inject

sealed class DfuUiState {
    object Idle : DfuUiState()
    object Waiting : DfuUiState() // New state for immediate feedback
    data class InProgress(val progress: Int) : DfuUiState()
    object Success : DfuUiState()
    data class Error(val message: String) : DfuUiState()
}

@HiltViewModel
class DfuViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _dfuState = MutableStateFlow<DfuUiState>(DfuUiState.Idle)
    val dfuState = _dfuState.asStateFlow()

    private val dfuProgressListener = object : no.nordicsemi.android.dfu.DfuProgressListenerAdapter() {
        override fun onDfuProcessStarting(deviceAddress: String) {
            _dfuState.value = DfuUiState.InProgress(0)
        }

        override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            _dfuState.value = DfuUiState.InProgress(percent)
        }

        override fun onDfuCompleted(deviceAddress: String) {
            _dfuState.value = DfuUiState.Success
        }

        override fun onDfuAborted(deviceAddress: String) {
            _dfuState.value = DfuUiState.Error("DFU Aborted")
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String) {
            _dfuState.value = DfuUiState.Error(message)
        }
    }

    init {
        DfuServiceListenerHelper.registerProgressListener(context, dfuProgressListener)
    }

    override fun onCleared() {
        super.onCleared()
        DfuServiceListenerHelper.unregisterProgressListener(context, dfuProgressListener)
    }

    fun startDfu(deviceAddress: String, firmwareUri: Uri) {
        // Set state to Waiting immediately
        _dfuState.value = DfuUiState.Waiting

        val initiator = DfuServiceInitiator(deviceAddress)
            .setDeviceName("PineTime")
            .setKeepBond(true)
            .setZip(firmwareUri)
            .setMtu(0)
            .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
            .setPacketsReceiptNotificationsEnabled(true)
            .setPacketsReceiptNotificationsValue(10)
        initiator.start(context, DfuService::class.java)
    }

    fun resetDfuState() {
        _dfuState.value = DfuUiState.Idle
    }
}
