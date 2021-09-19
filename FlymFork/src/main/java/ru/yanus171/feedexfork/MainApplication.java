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
 */

package ru.yanus171.feedexfork;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.StrictMode;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.lang.reflect.Method;

import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.FileVoc;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;


import static ru.yanus171.feedexfork.service.FetcherService.Status;

public class MainApplication extends Application {

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    public static FileVoc mImageFileVoc = null;
    public static FileVoc mHTMLFileVoc = null;
    public static LabelVoc mLabelVoc = null;

    public static final String OPERATION_NOTIFICATION_CHANNEL_ID = "operation_channel";
    public static final String READING_NOTIFICATION_CHANNEL_ID = "reading_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Status();
        mImageFileVoc = new FileVoc(FileUtils.INSTANCE.GetImagesFolder() );
        mHTMLFileVoc = new FileVoc(FileUtils.INSTANCE.GetHTMLFolder() );

        try {
            ProviderInstaller.installIfNeeded(mContext);
            //Toast.makeText( mContext,  "ProviderInstaller.installIfNeeded", Toast.LENGTH_LONG  ).show();
        } catch (GooglePlayServicesRepairableException e) {
            //Toast.makeText( mContext,  "GooglePlayServicesRepairableException", Toast.LENGTH_LONG  ).show();
            GoogleApiAvailability.getInstance().showErrorNotification(mContext, e.getConnectionStatusCode());
        } catch (GooglePlayServicesNotAvailableException e) {
            //Toast.makeText( mContext,  "GooglePlayServicesNotAvailableException", Toast.LENGTH_LONG  ).show();
        }

        LabelVoc.INSTANCE.initInThread();
        BaseActivity.InitLocale( mContext );

        Thread.setDefaultUncaughtExceptionHandler(new DebugApp().new UncaughtExceptionHandler(this));

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                Dog.e("disableDeathOnFileUriExposure", e);
            }
        }
        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);


        if (Build.VERSION.SDK_INT >= 26) {
            Context context = MainApplication.getContext();
            {
                NotificationChannel channel = new NotificationChannel(OPERATION_NOTIFICATION_CHANNEL_ID, context.getString(R.string.long_operation), NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(context.getString(R.string.long_operation));
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            {
                NotificationChannel channel = new NotificationChannel(READING_NOTIFICATION_CHANNEL_ID, context.getString(R.string.reading_article), NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(context.getString(R.string.reading_article));
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }

        mImageFileVoc.init1();
        mHTMLFileVoc.init1();
        EntryUrlVoc.INSTANCE.initInThread();
    }


    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        BaseActivity.InitLocale( mContext );
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext( BaseActivity.InitLocale( base ));
    }
}
