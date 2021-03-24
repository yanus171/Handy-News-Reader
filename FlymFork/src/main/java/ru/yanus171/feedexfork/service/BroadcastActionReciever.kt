package ru.yanus171.feedexfork.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.yanus171.feedexfork.provider.FeedData

class BroadcastActionReciever : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra("StopReadingService"))
            context.stopService(Intent(context, ReadingService::class.java))
        else if ( intent.hasExtra("FetchingServiceStart" ) )
            FetcherService.cancelRefresh()
    }

    companion object {
        const val Action = FeedData.PACKAGE_NAME + ".service.BROADCAST_ACTION"
    }
}