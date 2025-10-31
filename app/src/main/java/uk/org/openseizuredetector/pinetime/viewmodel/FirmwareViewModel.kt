package uk.org.openseizuredetector.pinetime.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.org.openseizuredetector.pinetime.model.FirmwareIndex
import uk.org.openseizuredetector.pinetime.model.FirmwareRelease
import uk.org.openseizuredetector.pinetime.remote.FirmwareService
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed class FirmwareUiState {
    object Idle : FirmwareUiState()
    object Loading : FirmwareUiState()
    data class Success(val index: FirmwareIndex) : FirmwareUiState()
    data class Error(val message: String) : FirmwareUiState()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    // Finished state is no longer needed, as the URI is now part of the main state.
    data class Error(val message: String) : DownloadState()
}

@HiltViewModel
class FirmwareViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: FirmwareService
) : ViewModel() {

    private val _firmwareState = MutableStateFlow<FirmwareUiState>(FirmwareUiState.Idle)
    val firmwareState = _firmwareState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    // Expose the downloaded URI as a state.
    private val _downloadedUri = MutableStateFlow<Uri?>(null)
    val downloadedUri = _downloadedUri.asStateFlow()

    fun fetchFirmwareIndex() {
        // Reset states when starting a new fetch
        _downloadedUri.value = null
        _downloadState.value = DownloadState.Idle
        viewModelScope.launch {
            _firmwareState.value = FirmwareUiState.Loading
            try {
                val index = service.getIndex()
                _firmwareState.value = FirmwareUiState.Success(index)
            } catch (e: Exception) {
                _firmwareState.value = FirmwareUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun downloadFirmware(release: FirmwareRelease) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0)
            try {
                val response = service.downloadFirmware(release.fname)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val file = File(context.cacheDir, release.fname)
                    val fileOutputStream = FileOutputStream(file)
                    val inputStream = body.byteStream()
                    val totalBytes = body.contentLength()
                    var bytesCopied = 0L

                    inputStream.use { input ->
                        fileOutputStream.use { output ->
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesCopied += read
                                val progress = ((bytesCopied * 100) / totalBytes).toInt()
                                _downloadState.value = DownloadState.Downloading(progress)
                            }
                        }
                    }
                    // Update the dedicated URI state.
                    _downloadedUri.value = Uri.fromFile(file)
                    // Set download to idle once finished.
                    _downloadState.value = DownloadState.Idle 
                } else {
                    _downloadState.value = DownloadState.Error("Download failed")
                }
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
