package uk.org.openseizuredetector.pinetime

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import uk.org.openseizuredetector.pinetime.model.OsdLaunchRequest
import uk.org.openseizuredetector.pinetime.ui.MainScreen
import uk.org.openseizuredetector.pinetime.ui.theme.PineTimeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var resultIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate: Instance=$this, CallingActivity=${callingActivity?.className}")
        Log.d("MainActivity", "onCreate: Intent Flags=0x${Integer.toHexString(intent.flags)}")

        // Non-null only when started by the OpenSeizureDetector app's "Update Firmware?" prompt.
        val osdRequest = OsdLaunchRequest.from(intent)
        Log.d("MainActivity", "onCreate: osdRequest=$osdRequest")

        // This line enables edge-to-edge display.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PineTimeTheme {
                MainScreen(osdRequest = osdRequest)
            }
        }
    }

    fun setMacAddressResult(macAddress: String) {
        Log.d("MainActivity", "setMacAddressResult called with: $macAddress")
        val intent = Intent().apply {
            putExtra("mac_address", macAddress)
        }
        resultIntent = intent
        setResult(RESULT_OK, intent)
        Log.d("MainActivity", "setResult(RESULT_OK) called immediately.")
    }

    override fun finish() {
        Log.d("MainActivity", "finish() called. Instance=$this. Returning result.")
        // Ensure result is set again just before finish, in case it was lost
        if (resultIntent != null) {
            Log.d("MainActivity", "Re-setting result explicitly in finish()")
            setResult(RESULT_OK, resultIntent)
        } else {
            Log.d("MainActivity", "No result intent stored.")
        }
        super.finish()
    }
}
