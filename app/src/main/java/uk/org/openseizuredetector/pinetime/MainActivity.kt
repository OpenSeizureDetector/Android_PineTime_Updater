package uk.org.openseizuredetector.pinetime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import uk.org.openseizuredetector.pinetime.ui.MainScreen
import uk.org.openseizuredetector.pinetime.ui.theme.PineTimeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This line enables edge-to-edge display.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PineTimeTheme {
                MainScreen()
            }
        }
    }
}
