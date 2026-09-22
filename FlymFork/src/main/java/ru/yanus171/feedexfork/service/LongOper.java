package ru.yanus171.feedexfork.service;

import static android.content.Context.POWER_SERVICE;
import static ru.yanus171.feedexfork.Constants.MILLS_IN_MINUTE;
import static ru.yanus171.feedexfork.MainApplication.OPERATION_NOTIFICATION_CHANNEL_ID;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.service.BroadcastActionReciever.Action;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.service.FetcherService.mIsWiFi;
import static ru.yanus171.feedexfork.view.StatusText.GetPendingIntentRequestCode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.StatusText;

import java.util.concurrent.atomic.AtomicBoolean;

public class LongOper {
    private static PowerManager.WakeLock mWakeLock = null;
    public static Boolean mCancelRefresh = false;

    /* Traffic-limit (non-WiFi) cancellation has already been reported to the user. */
    private static boolean mTrafficLimitReported = false;

    /*
     * A full refresh of a large feed collection (1000+ feeds) runs for far longer than
     * any fixed wakelock timeout, so instead of a single hard cap the lock is renewed
     * in a loop until the operation finishes. Otherwise the lock expires at an
     * arbitrary point mid-run, the system stops holding the CPU, and the refresh
     * silently stalls (no error, no abort) until the user restarts it manually.
     */
    private static final long WAKE_LOCK_RENEW_INTERVAL = 8 * MILLS_IN_MINUTE;
    private static final long WAKE_LOCK_TIMEOUT = 5 * WAKE_LOCK_RENEW_INTERVAL;

    LongOper(int textID, Runnable oper, Service service) {
        this( MainApplication.getContext().getString( textID ), oper, service );
    }
    public LongOper(int textID, Runnable oper) {
        this( MainApplication.getContext().getString( textID ), oper, null );
    }
    LongOper(String title, Runnable oper, Service service) {
        if ( service != null )
            service.startForeground(Constants.NOTIFICATION_ID_REFRESH_SERVICE, StatusText.GetNotification("", title, R.drawable.refresh, OPERATION_NOTIFICATION_CHANNEL_ID, createCancelPI()));
        if ( service != null )
            Status().SetNotificationTitle( title, createCancelPI() );
        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true);
        resetCancelRefresh();
        final AtomicBoolean isOperRunning = new AtomicBoolean( true );
        final Thread wakeLockRenewer = new Thread( () -> {
            while ( isOperRunning.get() ) {
                try {
                    Thread.sleep( WAKE_LOCK_RENEW_INTERVAL );
                } catch ( InterruptedException e ) {
                    break;
                }
                try {
                    if ( isOperRunning.get() && mWakeLock != null && !mWakeLock.isHeld() )
                        mWakeLock.acquire( WAKE_LOCK_TIMEOUT );
                } catch ( Exception ignored ) {
                }
            }
        }, "LongOper-WakeLockRenewer" );
        wakeLockRenewer.setDaemon( true );
        try {
            if ( mWakeLock == null )
                mWakeLock = getWakeLock();
            if ( !mWakeLock.isHeld() )
                mWakeLock.acquire( WAKE_LOCK_TIMEOUT );
            wakeLockRenewer.start();
            oper.run();
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText( this, getString( R.string.error ) + ": " + e.getMessage(), Toast.LENGTH_LONG ).show();
            DebugApp.SendException( e, getContext() );
        } finally {
            isOperRunning.set( false );
            wakeLockRenewer.interrupt();
            if ( service != null )
                Status().SetNotificationTitle( "", null );
            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);
            if ( service != null )
                service.stopForeground(true);
            resetCancelRefresh();
            if ( mWakeLock != null && mWakeLock.isHeld() )
                mWakeLock.release();
        }
    }

    public static boolean isCancelRefresh() {
        synchronized (mCancelRefresh) {
            final boolean isTrafficLimitExceeded = !mIsWiFi && Status().mBytesRecievedLast > PrefUtils.getMaxSingleRefreshTraffic() * 1024 * 1024;
            if ( isTrafficLimitExceeded && !mTrafficLimitReported ) {
                mTrafficLimitReported = true;
                notifyTrafficLimitExceeded();
            }
            return isTrafficLimitExceeded || mCancelRefresh;
        }
    }
    public static void resetCancelRefresh() {
        synchronized (mCancelRefresh) {
            mCancelRefresh = false;
        }
        mTrafficLimitReported = false;
    }
    public static void cancelRefresh() {
        synchronized (mCancelRefresh) {
            MainApplication.getContext().getContentResolver().delete( FeedData.TaskColumns.CONTENT_URI, null, null );
            mCancelRefresh = true;
        }
    }

    /*
     * Like FetcherService.ShowEventNotification: a real system notification
     * (notification area, not a toast). Shown once per refresh session.
     */
    private static void notifyTrafficLimitExceeded() {
        if ( Build.VERSION.SDK_INT < 26 && !PrefUtils.getBoolean( PrefUtils.NOTIFICATIONS_ENABLED, true ) )
            return;
        final Intent intent = new Intent( MainApplication.getContext(), HomeActivity.class );
        final PendingIntent contentIntent = PendingIntent.getActivity( MainApplication.getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );
        final NotificationCompat.Builder builder = new NotificationCompat.Builder( MainApplication.getContext() )
                .setContentIntent( contentIntent )
                .setSmallIcon( R.mipmap.ic_launcher )
                .setWhen( System.currentTimeMillis() )
                .setAutoCancel( true )
                .setContentTitle( MainApplication.getContext().getString( R.string.max_single_refresh_traffic_mb ) )
                .setContentText( MainApplication.getContext().getString( R.string.max_single_refresh_traffic_mb_descr ) );
        if ( Build.VERSION.SDK_INT >= 26 )
            builder.setChannelId( MainApplication.UNREAD_NOTIFICATION_CHANNEL_ID );
        Constants.NOTIF_MGR.notify( Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT, builder.build() );
    }


    private static PowerManager.WakeLock getWakeLock() {
        PowerManager powerManager = (PowerManager) MainApplication.getContext().getSystemService(POWER_SERVICE);
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Handy::LongOper");
    }

    public static PendingIntent createCancelPI() {
        Context context = getContext();
        Intent intent = new Intent(context, BroadcastActionReciever.class);
        intent.setAction( Action );
        intent.putExtra("FetchingServiceStart", true );
        return PendingIntent.getBroadcast(context, GetPendingIntentRequestCode(), intent, PendingIntent.FLAG_IMMUTABLE);
    }




}
