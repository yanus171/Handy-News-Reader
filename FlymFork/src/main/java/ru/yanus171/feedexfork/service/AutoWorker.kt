package ru.yanus171.feedexfork.service

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.work.*
import org.json.JSONObject
import ru.yanus171.feedexfork.Constants
import ru.yanus171.feedexfork.Constants.NOTIFICATION_ID_REFRESH_SERVICE
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.service.FetcherService.CUSTOM_REFRESH_INTERVAL
import ru.yanus171.feedexfork.service.FetcherService.createCancelPI
import ru.yanus171.feedexfork.utils.Dog
import ru.yanus171.feedexfork.utils.PrefUtils.*
import ru.yanus171.feedexfork.view.EntryView.TAG
import ru.yanus171.feedexfork.view.StatusText
import java.util.concurrent.TimeUnit
import kotlin.math.max


class AutoWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        var keyInterval = ""
        if (tags.contains( AUTO_BACKUP_JOB_ID) )
            keyInterval = AUTO_BACKUP_INTERVAL
        else if ( tags.contains( AUTO_REFRESH_JOB_ID ) )
            keyInterval = REFRESH_INTERVAL
        else if ( tags.contains( DELETE_OLD_JOB_ID) )
            keyInterval = DELETE_OLD_INTERVAL
        val currentTime = System.currentTimeMillis()
        var currentInterval = getTimeIntervalInMSecs(keyInterval, DEFAULT_INTERVAL)
        if (tags.contains( AUTO_REFRESH_JOB_ID ) )
            currentInterval = minCustomRefreshInterval()
        var lastJobOccured: Long = 0
        try {
            lastJobOccured = getLong(LAST_JOB_OCCURRED + keyInterval, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (currentTime - lastJobOccured > currentInterval) {
            if (tags.contains( AUTO_BACKUP_JOB_ID ) )
                ExecuteAutoBackup()
            else if (tags.contains( DELETE_OLD_JOB_ID) )
                ExecuteDeleteOld()
            else if (isSyncActive && tags.contains( AUTO_REFRESH_JOB_ID ) )
                FetcherService.Start(FetcherService.GetIntent(Constants.FROM_AUTO_REFRESH), true)
        }
        return Result.success()
    }
    @WorkerThread
    @Override
    fun getForegroundInfo(): ForegroundInfo {
        var titleID = R.string.unknown
        if ( tags.contains( AUTO_BACKUP_JOB_ID ) )
            titleID = R.string.exportingToFile
        else if ( tags.contains( AUTO_REFRESH_JOB_ID ) )
            titleID = R.string.RefreshFeeds
        else if ( tags.contains( DELETE_OLD_JOB_ID ) )
            titleID = R.string.menu_delete_old
        val notification = StatusText.GetNotification(
                        "",
                        applicationContext.getString( titleID ),
                        R.drawable.refresh,
                        MainApplication.OPERATION_NOTIFICATION_CHANNEL_ID,
                        createCancelPI())
        return ForegroundInfo( NOTIFICATION_ID_REFRESH_SERVICE, notification )
    }

    private val isSyncActive: Boolean
        get() = ContentResolver.getMasterSyncAutomatically()

    companion object {
        private const val LAST = "LAST_"
        fun getTimeIntervalInMSecs(key: String?, defaultValue: Long): Long {
            var time = defaultValue
            try {
                time = max(60L * 1000, getString(key, "").toLong())
            } catch (ignored: Exception) {
            }
            return time
        }

        const val LAST_JOB_OCCURRED = "LAST_JOB_OCCURRED_"
        private const val AUTO_BACKUP_JOB_ID = "AUTO_BACKUP"
        private const val AUTO_REFRESH_JOB_ID = "AUTO_REFRESH"
        private const val DELETE_OLD_JOB_ID = "DELETE_OLD"
        const val DEFAULT_INTERVAL = 3600L * 1000 * 24
        private fun initAutoJob(context: Context,
                                keyInterval: String,
                                keyEnabled: String,
                                minimumInterval: Long,
                                jobTag: String,
                                requiresNetwork: Boolean,
                                requiresCharging: Boolean) {
            val currentInterval = if (minimumInterval != -1L) minimumInterval else getTimeIntervalInMSecs(keyInterval, DEFAULT_INTERVAL)
            val lastInterval = getTimeIntervalInMSecs(LAST + keyInterval, DEFAULT_INTERVAL)
            val currentTime = System.currentTimeMillis()
            var lastJobOccured: Long = 0
            try {
                lastJobOccured = getLong(LAST_JOB_OCCURRED + keyInterval, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (getBoolean(keyEnabled, true)) {
                if (lastInterval != currentInterval || currentTime - lastJobOccured > currentInterval ) {
                    val constraints = Constraints.Builder()
                    if (requiresNetwork)
                        constraints.setRequiredNetworkType(NetworkType.CONNECTED)
                    if (requiresCharging)
                        constraints.setRequiresCharging(true)
                    val periodicSyncDataWork = PeriodicWorkRequest.Builder(AutoWorker::class.java, currentInterval, TimeUnit.MILLISECONDS)
                            .addTag(jobTag)
                            .setConstraints(constraints.build()) // setting a backoff on case the work needs to retry
                            .build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(jobTag, ExistingPeriodicWorkPolicy.REPLACE, periodicSyncDataWork)
                    putString(LAST + keyInterval, currentInterval.toString())
                }
            }
        }

        private fun minCustomRefreshInterval(): Long {
            var result = getString(REFRESH_INTERVAL, DEFAULT_INTERVAL.toString()).toLong()
            val cur = MainApplication.getContext().contentResolver.query(FeedData.FeedColumns.CONTENT_URI, arrayOf(FeedData.FeedColumns.OPTIONS), FeedData.FeedColumns.IS_GROUP + Constants.DB_IS_NULL, null, null)
            while (cur!!.moveToNext()) {
                val jsonText = if (cur.isNull(0)) "" else cur.getString(0)
                if (jsonText.isNotEmpty()) try {
                    val jsonOptions = JSONObject(jsonText)
                    if ( jsonOptions.has(CUSTOM_REFRESH_INTERVAL) && result > jsonOptions.getLong(CUSTOM_REFRESH_INTERVAL))
                        result = jsonOptions.getLong(CUSTOM_REFRESH_INTERVAL)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            cur.close()
            Dog.v(TAG, "AutoJobService.minCustomRefreshInterval = $result")
            return result
        }

        private fun ExecuteAutoBackup() {
            if (Build.VERSION.SDK_INT < 26 && FetcherService.isBatteryLow()) return
            if (System.currentTimeMillis() - getLong(FIRST_LAUNCH_TIME, System.currentTimeMillis()) < 1000 * 60 * 60 * 1) return
            FetcherService.Process(FetcherService.GetIntent(Constants.FROM_AUTO_BACKUP), null)
        }

        private fun ExecuteDeleteOld() {
            if (Build.VERSION.SDK_INT < 26 && FetcherService.isBatteryLow()) return
            if (System.currentTimeMillis() - getLong(FIRST_LAUNCH_TIME, System.currentTimeMillis()) < 1000 * 60 * 60 * 1) return
            FetcherService.Process(FetcherService.GetIntent(Constants.FROM_DELETE_OLD), null)
        }
        fun init() {
            val context = MainApplication.getContext()
            if (Build.VERSION.SDK_INT >= 21) {
                initAutoJob(context, REFRESH_INTERVAL, REFRESH_ENABLED, getMinCustomRefreshInterval(), AUTO_REFRESH_JOB_ID, true, getBoolean("auto_refresh_requires_charging", false))
                initAutoJob(context, DELETE_OLD_INTERVAL, REFRESH_ENABLED, -1, DELETE_OLD_JOB_ID, false, getBoolean("delete_old_requires_charging", true))
                initAutoJob(context, AUTO_BACKUP_INTERVAL, AUTO_BACKUP_ENABLED, -1, AUTO_BACKUP_JOB_ID, false, false)
            }
        }
        @JvmName("getMinCustomRefreshInterval1")
        private fun getMinCustomRefreshInterval(): Long {
            var result = getString(REFRESH_INTERVAL, DEFAULT_INTERVAL.toString()).toLong()
            run {
                val cur: Cursor? = MainApplication.getContext().contentResolver.query(FeedData.FeedColumns.CONTENT_URI, arrayOf(FeedData.FeedColumns.OPTIONS), FeedData.FeedColumns.IS_GROUP + Constants.DB_IS_NULL, null, null)
                if (cur != null) {
                    while (cur.moveToNext()) {
                        val jsonText = if (cur.isNull(0)) "" else cur.getString(0)
                        if (jsonText.isNotEmpty()) try {
                            val jsonOptions = JSONObject(jsonText)
                            if (jsonOptions.has(CUSTOM_REFRESH_INTERVAL)) if (result > jsonOptions.getLong(CUSTOM_REFRESH_INTERVAL)) result = jsonOptions.getLong(CUSTOM_REFRESH_INTERVAL)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                cur?.close()
            }
            Dog.v(TAG, "AutoJobService.minCustomRefreshInterval = $result")
            return result
        }
    }


}