/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.utils.PrefUtils;

import static ru.yanus171.feedexfork.utils.PrefUtils.AUTO_BACKUP_ENABLED;
import static ru.yanus171.feedexfork.utils.PrefUtils.AUTO_BACKUP_INTERVAL;
import static ru.yanus171.feedexfork.utils.PrefUtils.DELETE_OLD_INTERVAL;
import static ru.yanus171.feedexfork.utils.PrefUtils.REFRESH_ENABLED;
import static ru.yanus171.feedexfork.utils.PrefUtils.REFRESH_INTERVAL;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoJobService extends JobService {
    //private static final String SIXTY_MINUTES = "3600000";
    //public static final String TASK_TAG_PERIODIC = "TASK_TAG_PERIODIC";

    /*@Override
    public int onRunTask(TaskParams taskParams) {
        FetcherService.StartService(GetAutoRefreshServiceIntent());
        return GcmNetworkManager.RESULT_SUCCESS;
    }
    */


//    static boolean isAutoUpdateEnabled() {
//        return PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true);
//    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static JobInfo GetPendingJobByID(JobScheduler jobScheduler, int ID) {
        if ( Build.VERSION.SDK_INT >= 24 ) {
            return jobScheduler.getPendingJob( ID );
        } else {
            for ( JobInfo item: jobScheduler.getAllPendingJobs() )
                if ( item.getId() == ID )
                    return item;
            return null;
        }
    }

    private static  final String LAST = "LAST_";
    private static long getTimeIntervalInMSecs(String key, long defaultValue) {
        long time = defaultValue;
        try {
            time = Math.max(60L * 1000, Long.parseLong(PrefUtils.getString(key, "")));
        } catch (Exception ignored) {
        }
        return time;
    }

    public static  final String LAST_JOB_OCCURED = "LAST_JOB_OCCURED_";
    private static final int AUTO_BACKUP_JOB_ID = 3;
    private static final int AUTO_REFRESH_JOB_ID = 1;
    private static final int DELETE_OLD_JOB_ID = 4;
    final static long DEFAULT_INTERVAL = 3600L * 1000 * 24;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void initAutoJob(Context context,
                                    final String keyInterval,
                                    final String keyEnabled,
                                    final int jobID,
                                    boolean requiresNetwork,
                                    boolean requiresCharging) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        final long currentInterval = getTimeIntervalInMSecs(keyInterval, DEFAULT_INTERVAL);
        final long lastInterval = getTimeIntervalInMSecs(LAST + keyInterval, DEFAULT_INTERVAL);
        final long currentTime = System.currentTimeMillis();
        long lastJobOccured = 0;
        try {
            lastJobOccured = PrefUtils.getLong(LAST_JOB_OCCURED + keyInterval, 0 );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        if ( PrefUtils.getBoolean( keyEnabled, true )  ) {
            if ( lastInterval != currentInterval ||
                    currentTime - lastJobOccured > currentInterval ||
                    GetPendingJobByID(jobScheduler, jobID) == null ||
                    GetPendingJobByID(jobScheduler, jobID).isRequireCharging() != requiresCharging ) {
                ComponentName serviceComponent = new ComponentName(context, AutoJobService.class);
                JobInfo.Builder builder =
                        new JobInfo.Builder(jobID, serviceComponent)
                                .setPeriodic(currentInterval)
                                .setRequiresCharging(requiresCharging)
                                .setPersisted(true)
                        //.setRequiresDeviceIdle(true)
                        ;
                if (Build.VERSION.SDK_INT >= 26) {
                    builder.setRequiresStorageNotLow(true)
                            .setRequiresBatteryNotLow(true);
                }
                if (requiresNetwork)
                    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

                jobScheduler.schedule(builder.build());
            }
        } else if ( GetPendingJobByID(jobScheduler, jobID ) != null )
            jobScheduler.cancel( jobID );
        PrefUtils.putString( LAST + keyInterval, String.valueOf( currentInterval ) );
    }
    public static void init(Context context) {
        if (Build.VERSION.SDK_INT >= 21 ) {
            initAutoJob(context, REFRESH_INTERVAL, REFRESH_ENABLED, AUTO_REFRESH_JOB_ID, true, PrefUtils.getBoolean("auto_refresh_requires_charging", false ) );
            initAutoJob( context,DELETE_OLD_INTERVAL, REFRESH_ENABLED, DELETE_OLD_JOB_ID, false, PrefUtils.getBoolean( "delete_old_requires_charging", true ) );
            initAutoJob( context, AUTO_BACKUP_INTERVAL, AUTO_BACKUP_ENABLED, AUTO_BACKUP_JOB_ID, false, false );
        }
        /*else {
            GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);
            if (isAutoUpdateEnabled()) {
                PeriodicTask task = new PeriodicTask.Builder()
                        .setService(AutoRefreshService.class)
                        .setTag(TASK_TAG_PERIODIC)
                        .setPeriod(getTimeIntervalInMSecs() / 1000)
                        .setPersisted(true)
                        .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                        .setUpdateCurrent(true)
                        .build();

                gcmNetworkManager.schedule(task);
            } else {
                gcmNetworkManager.cancelTask(TASK_TAG_PERIODIC, AutoRefreshService.class);
            }

        }*/
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        String keyInterval = "";
        if ( jobParameters.getJobId() == AUTO_BACKUP_JOB_ID )
            keyInterval = AUTO_BACKUP_INTERVAL;
        else if ( jobParameters.getJobId() == AUTO_REFRESH_JOB_ID)
            keyInterval = REFRESH_INTERVAL;
        else if ( jobParameters.getJobId() == DELETE_OLD_JOB_ID )
            keyInterval = DELETE_OLD_INTERVAL;
        final long currentTime = System.currentTimeMillis();
        final long currentInterval = getTimeIntervalInMSecs(keyInterval, DEFAULT_INTERVAL);
        long lastJobOccured = 0;
        try {
            lastJobOccured = PrefUtils.getLong(LAST_JOB_OCCURED + keyInterval, 0 );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        if ( currentTime - lastJobOccured > currentInterval ) {
            if (jobParameters.getJobId() == AUTO_BACKUP_JOB_ID)
                ExecuteAutoBackup();
            else if (jobParameters.getJobId() == DELETE_OLD_JOB_ID)
                ExecuteDeleteOld();
            else if (isSyncActive() && jobParameters.getJobId() == AUTO_REFRESH_JOB_ID)
                FetcherService.StartService(FetcherService.GetIntent(Constants.FROM_AUTO_REFRESH), true);
        }
        return false;
    }

    private boolean isSyncActive() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    private static void ExecuteAutoBackup() {
        if ( Build.VERSION.SDK_INT < 26 && FetcherService.isBatteryLow() )
            return;

        if ( System.currentTimeMillis() - PrefUtils.getLong( PrefUtils.FIRST_LAUNCH_TIME, System.currentTimeMillis() ) < 1000 * 60 * 60 * 1 )
            return;

        FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_AUTO_BACKUP ), false );
    }

    private static void ExecuteDeleteOld() {
        if ( Build.VERSION.SDK_INT < 26 && FetcherService.isBatteryLow() )
            return;

        if ( System.currentTimeMillis() - PrefUtils.getLong( PrefUtils.FIRST_LAUNCH_TIME, System.currentTimeMillis() ) < 1000 * 60 * 60 * 1 )
            return;

        FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_DELETE_OLD ), false );
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
