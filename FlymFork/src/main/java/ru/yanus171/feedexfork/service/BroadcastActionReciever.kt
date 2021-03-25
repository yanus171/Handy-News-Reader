package ru.yanus171.feedexfork.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import android.widget.Toast
import ru.yanus171.feedexfork.Constants
import ru.yanus171.feedexfork.Constants.EXTRA_ID
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.provider.FeedData

class BroadcastActionReciever : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when {
            intent.hasExtra("StopReadingService") -> context.stopService(Intent(context, ReadingService::class.java))
            intent.hasExtra("FetchingServiceStart") -> FetcherService.cancelRefresh()
            intent.hasExtra("UnstarArticle") -> {
                val link = intent.getStringExtra(EXTRA_TEXT)
                val count = context.contentResolver.update(FetcherService.GetEntryUri(link), FeedData.getUnstarContentValues(), null, null)
                val notificationID = intent.getIntExtra(EXTRA_ID, 0)
                if (Constants.NOTIF_MGR != null)
                    Constants.NOTIF_MGR.cancel( notificationID )
                Toast.makeText(context, if (count > 0) R.string.articleWasUnstarred else R.string.articleNotFound, Toast.LENGTH_LONG).show()
            }
        }

    }

    companion object {
        const val Action = FeedData.PACKAGE_NAME + ".service.BROADCAST_ACTION"
    }
}