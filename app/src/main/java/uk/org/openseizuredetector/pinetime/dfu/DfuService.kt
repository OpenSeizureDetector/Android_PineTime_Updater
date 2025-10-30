package uk.org.openseizuredetector.pinetime.dfu

import android.app.Activity
import android.content.Intent
import no.nordicsemi.android.dfu.DfuBaseService
import uk.org.openseizuredetector.pinetime.MainActivity

class DfuService : DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity>? {
        return MainActivity::class.java
    }

    override fun isDebug(): Boolean = true

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
}