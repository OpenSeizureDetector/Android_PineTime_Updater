package uk.org.openseizuredetector.pinetime.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import uk.org.openseizuredetector.pinetime.R
import uk.org.openseizuredetector.pinetime.viewmodel.DfuUiState
import uk.org.openseizuredetector.pinetime.viewmodel.DfuViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    var firmwareUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    val dfuViewModel: DfuViewModel = hiltViewModel()
    val dfuState = dfuViewModel.dfuState.collectAsState().value

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        firmwareUri = it
    }

    // Handle Notification Permission for Android 13+ (TIRAMISU)
    val postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // Request permission as soon as the screen is displayed.
    LaunchedEffect(postNotificationPermission) {
        postNotificationPermission?.let {
            if (!it.status.isGranted) {
                it.launchPermissionRequest()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.app_name)) })
        }
    ) { paddingValues ->
        if (showScanner) {
            Column(modifier = Modifier.padding(paddingValues)) {
                DeviceScanner(onDeviceSelected = {
                    selectedDevice = it
                    showScanner = false
                })
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp), // Additional padding for content
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { launcher.launch("application/zip") }) {
                    Text(text = "Select Firmware")
                }

                firmwareUri?.let {
                    Text(text = "Selected file: ${it.path}")
                }

                Button(onClick = { showScanner = true }, enabled = firmwareUri != null) {
                    Text(text = "Select BLE Device")
                }

                selectedDevice?.let {
                    Text(text = "Selected device: $it")
                }

                // Determine if DFU can be started
                val canStartDfu = firmwareUri != null && 
                                  selectedDevice != null && 
                                  (postNotificationPermission?.status?.isGranted ?: true)

                Button(
                    onClick = {
                        dfuViewModel.startDfu(selectedDevice!!, firmwareUri!!)
                    },
                    enabled = canStartDfu
                ) {
                    Text(text = "Start DFU")
                }

                // Show a message if notification permission is required but not granted.
                if (postNotificationPermission?.status?.isGranted == false) {
                    Text("Notification permission is required to run the DFU process.")
                }

                when (dfuState) {
                    is DfuUiState.InProgress -> {
                        LinearProgressIndicator(progress = dfuState.progress / 100f)
                        Text(text = "DFU in progress: ${dfuState.progress}%")
                    }
                    is DfuUiState.Success -> {
                        Text(text = "DFU Successful!")
                    }
                    is DfuUiState.Error -> {
                        Text(text = "DFU Error: ${dfuState.message}")
                    }
                    else -> {}
                }
            }
        }
    }
}
