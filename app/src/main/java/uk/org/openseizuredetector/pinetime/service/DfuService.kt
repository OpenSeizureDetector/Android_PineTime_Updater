package uk.org.openseizuredetector.pinetime.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.dfu.DfuBaseService
import uk.org.openseizuredetector.pinetime.MainActivity
import uk.org.openseizuredetector.pinetime.R

@AndroidEntryPoint
class DfuService : DfuBaseService() {

    override fun onCreate() {
        // On modern Android, we must immediately call startForeground() with a type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("dfu", getString(R.string.dfu_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, "dfu")
                .setContentTitle(getString(R.string.app_name))
                .setContentText("DFU service is running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    DfuBaseService.NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(DfuBaseService.NOTIFICATION_ID, notification)
            }
        }

        super.onCreate()
    }

    override fun getNotificationTarget(): Class<out Activity> {
        return MainActivity::class.java
    }

    override fun isDebug(): Boolean {
        // Set to true to show library logs in logcat.
        return false
    }
}
