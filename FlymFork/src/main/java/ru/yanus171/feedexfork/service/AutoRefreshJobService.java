package ru.yanus171.feedexfork.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import ru.yanus171.feedexfork.utils.PrefUtils;

import static ru.yanus171.feedexfork.service.AutoService.LAST;
import static ru.yanus171.feedexfork.utils.PrefUtils.AUTO_BACKUP_INTERVAL;
import static ru.yanus171.feedexfork.utils.PrefUtils.REFRESH_INTERVAL;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoRefreshJobService extends JobService {
    public static final int AUTO_UPDATE_JOB_ID = 1;

    public AutoRefreshJobService() {
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (AutoService.isAutoUpdateEnabled() )
            FetcherService.StartService( AutoService.GetAutoRefreshServiceIntent() );
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static void initAutoRefresh(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        final long DEFAULT_INTERVAL = 3600L * 1000 * 1;
        final long currentInterval = AutoService.getTimeIntervalInMSecs(REFRESH_INTERVAL, DEFAULT_INTERVAL);
        if (AutoService.isAutoUpdateEnabled() ) {
            final long lastInterval = AutoService.getTimeIntervalInMSecs(LAST + REFRESH_INTERVAL, DEFAULT_INTERVAL);
            if (lastInterval != currentInterval || AutoService.GetPendingJobByID( jobScheduler, AUTO_UPDATE_JOB_ID ) == null ) {
                ComponentName serviceComponent = new ComponentName(context, AutoRefreshJobService.class);
                JobInfo.Builder builder =
                        new JobInfo.Builder(AUTO_UPDATE_JOB_ID, serviceComponent)
                                .setPeriodic(currentInterval)
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                .setRequiresCharging(false)
                                .setPersisted(true)
                        //.setRequiresDeviceIdle(true)
                        ;
                if (Build.VERSION.SDK_INT >= 26) {
                    builder.setRequiresStorageNotLow(true)
                            .setRequiresBatteryNotLow(true);
                }
                jobScheduler.schedule(builder.build() );
            }
        } else
            jobScheduler.cancel(AUTO_UPDATE_JOB_ID);
        PrefUtils.putString( LAST + REFRESH_INTERVAL, String.valueOf( currentInterval ) );
    }

}
