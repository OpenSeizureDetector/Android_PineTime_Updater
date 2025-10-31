/*
Based on Nordic Semiconductors DFU Example app.
 */

package uk.org.openseizuredetector.pinetime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.common.navigation.NavigationView
import no.nordicsemi.android.common.theme.NordicTheme
//import no.nordicsemi.android.dfu.analytics.DfuAnalytics
//import no.nordicsemi.android.dfu.analytics.HandleDeepLinkEvent
import no.nordicsemi.android.dfu.navigation.DfuDestinations
import no.nordicsemi.android.dfu.storage.DeepLinkHandler
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            NordicTheme {
                NavigationView(DfuDestinations)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

    }
}
