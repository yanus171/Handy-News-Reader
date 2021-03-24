package ru.yanus171.feedexfork.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.yanus171.feedexfork.Constants.NOTIFICATION_ID_READING_SERVICE
import ru.yanus171.feedexfork.MainApplication.READING_NOTIFICATION_CHANNEL_ID
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.service.BroadcastActionReciever.Companion.Action
import ru.yanus171.feedexfork.view.StatusText

class ReadingService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun createCancelReadingServicePI(): PendingIntent {
        val intent = Intent(this, BroadcastActionReciever::class.java)
        intent.action = Action
        intent.putExtra("StopReadingService", true)
        return PendingIntent.getBroadcast(this, StatusText.GetPendingIntentRequestCode(), intent, 0)
    }
    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        val nf = StatusText.GetNotification(getString(R.string.article_reading_notification_hint), "", R.drawable.transparent, READING_NOTIFICATION_CHANNEL_ID, createCancelReadingServicePI())
        startForeground(NOTIFICATION_ID_READING_SERVICE, nf)
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

}
