package ru.yanus171.feedexfork.view;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.util.Date;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.utils.PrefUtils;

public class ScreenOnKeeper {

    private static final String KEY_SCREEN_ON_DURATION = "screen_on_duration";
    private static final float MINUTES_TO_MS = 60 * 1000;

    private PowerManager.WakeLock mWakeLock = null;
    private Handler mHandler = null;
    private Runnable mScreenOffRunnable = null;
    private Date mLastInteractionTime = new Date();
    private boolean mIsRunning = false;

    public ScreenOnKeeper() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static long getDurationMs() {
        return (long) (PrefUtils.getFloatFromText(KEY_SCREEN_ON_DURATION, 0F) * MINUTES_TO_MS);
    }

    public static boolean isEnabled() {
        return getDurationMs() > 0;
    }

    public void start() {
        if (mIsRunning || !isEnabled())
            return;

        mIsRunning = true;
        acquireWakeLock();
        resetTimer();
    }

    public void stop() {
        mIsRunning = false;
        releaseWakeLock();
        cancelScheduledTurnOff();
    }


    public void resetTimer() {
        if (!mIsRunning || !isEnabled())
            return;

        mLastInteractionTime = new Date();
        cancelScheduledTurnOff();
        acquireWakeLock();
        scheduleTurnOff();
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) MainApplication.getContext().getSystemService(android.content.Context.POWER_SERVICE);
            if (powerManager != null) {
                releaseWakeLock();
                if ( mWakeLock == null )
                    mWakeLock = powerManager.newWakeLock( PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "HandyReader:KeepScreenOn" );
                long durationMs = getDurationMs();
                if (durationMs > 0 && !mWakeLock.isHeld())
                    mWakeLock.acquire(durationMs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            try {
                if (mWakeLock.isHeld())
                    mWakeLock.release();
            } catch (Exception ignored) {}
            mWakeLock = null;
        }
    }

    private void scheduleTurnOff() {
        if (mScreenOffRunnable == null) {
            mScreenOffRunnable = () -> {
                if (!mIsRunning)
                    return;

                long elapsedTime = new Date().getTime() - mLastInteractionTime.getTime();
                long durationMs = getDurationMs();

                if (elapsedTime >= durationMs) {
                    releaseWakeLock();
                    mIsRunning = false;
                } else
                    mHandler.postDelayed(mScreenOffRunnable, durationMs - elapsedTime);
            };
        }

        mHandler.postDelayed(mScreenOffRunnable, getDurationMs());
    }

    private void cancelScheduledTurnOff() {
        if (mScreenOffRunnable != null)
            mHandler.removeCallbacks(mScreenOffRunnable);
    }
}