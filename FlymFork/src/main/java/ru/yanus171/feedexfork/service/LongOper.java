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
import android.os.PowerManager;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.StatusText;

public class LongOper {
    private static PowerManager.WakeLock mWakeLock = null;
    public static Boolean mCancelRefresh = false;

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
        try {
            if ( mWakeLock == null )
                mWakeLock = getWakeLock();
            mWakeLock.acquire( 10 * MILLS_IN_MINUTE );
            oper.run();
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText( this, getString( R.string.error ) + ": " + e.getMessage(), Toast.LENGTH_LONG ).show();
            DebugApp.SendException( e, getContext() );
        } finally {
            if ( service != null )
                Status().SetNotificationTitle( "", null );
            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);
            if ( service != null )
                service.stopForeground(true);
            resetCancelRefresh();
            mWakeLock.release();
        }
    }

    public static void resetCancelRefresh() {
        synchronized (mCancelRefresh) {
            mCancelRefresh = false;
        }
    }
    public static boolean isCancelRefresh() {
        synchronized (mCancelRefresh) {
            if ( !mIsWiFi && Status().mBytesRecievedLast > PrefUtils.getMaxSingleRefreshTraffic() * 1024 * 1024 )
                return true;
            //if (mCancelRefresh) {
            //    contentResolver().delete( TaskColumns.CONTENT_URI, null, null );
            //}
            return mCancelRefresh;
        }
    }
    public static void cancelRefresh() {
        synchronized (mCancelRefresh) {
            MainApplication.getContext().getContentResolver().delete( FeedData.TaskColumns.CONTENT_URI, null, null );
            mCancelRefresh = true;
        }
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
