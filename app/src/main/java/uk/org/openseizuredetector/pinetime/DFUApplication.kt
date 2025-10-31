package uk.org.openseizuredetector.pinetime

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DFUApplication : Application() {

    private val DFU_CHANNEL_ID = "dfu"

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDfuNotificationChannel(this)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDfuNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // On modern Android, notification channel importance can't be changed after it's created.
        // We must first delete the old channel if it exists with the wrong importance.
        notificationManager.deleteNotificationChannel(DFU_CHANNEL_ID)

        // Now, we can create the new channel with the correct importance level.
        val channel = NotificationChannel(
            DFU_CHANNEL_ID,
            context.getString(R.string.dfu_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT // This is the crucial change.
        )
        channel.description = context.getString(R.string.dfu_channel_description)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }
}
