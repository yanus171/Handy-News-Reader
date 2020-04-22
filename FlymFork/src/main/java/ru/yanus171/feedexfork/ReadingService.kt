package ru.yanus171.feedexfork

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.yanus171.feedexfork.Constants.NOTIFICATION_ID_READING_SERVICE
import ru.yanus171.feedexfork.MainApplication.READING_NOTIFICATION_CHANNEL_ID
import ru.yanus171.feedexfork.view.StatusText

class ReadingService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart( intent, startId )
        startForeground( NOTIFICATION_ID_READING_SERVICE,
                StatusText.GetNotification( getString(R.string.article_reading_notification_hint), "", R.drawable.transparent, READING_NOTIFICATION_CHANNEL_ID ) )
    }

    override fun onDestroy() {
        stopForeground( true )
        super.onDestroy()
    }

}
