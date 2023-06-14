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

import static android.app.NotificationManager.IMPORTANCE_LOW;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.database.CursorWindow;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.StrictMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import ru.yanus171.feedexfork.activity.ArticleWebSearchActivity;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.HomeActivityNewTask;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.FileVoc;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.AppSelectPreference;


import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.FAVORITES_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.LAST_READ_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.UNREAD_ENTRIES_CONTENT_URI;
import static ru.yanus171.feedexfork.service.FetcherService.Status;

import androidx.annotation.RequiresApi;

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
    public static final String UNREAD_NOTIFICATION_CHANNEL_ID = "unread_channel";
    public static final String MARKED_AS_STARRED_NOTIFICATION_CHANNEL_ID = "mark_as_starred_channel";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate() {
        super.onCreate();
        enlargeCursorWindowSize();

        mContext = getApplicationContext();
        AppSelectPreference.Init();
        Status();
        mImageFileVoc = new FileVoc(FileUtils.INSTANCE.GetImagesFolder() );
        mHTMLFileVoc = new FileVoc(FileUtils.INSTANCE.GetHTMLFolder() );

        GoogleCheck.INSTANCE.check();

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
            createNotificationChannel(OPERATION_NOTIFICATION_CHANNEL_ID, R.string.long_operation, IMPORTANCE_LOW);
            createNotificationChannel(UNREAD_NOTIFICATION_CHANNEL_ID, R.string.unread_article, IMPORTANCE_HIGH);
            createNotificationChannel(MARKED_AS_STARRED_NOTIFICATION_CHANNEL_ID, R.string.markAsStarred, IMPORTANCE_HIGH);
            if ( Build.VERSION.SDK_INT <= 30 )
                createNotificationChannel(READING_NOTIFICATION_CHANNEL_ID, R.string.reading_article, IMPORTANCE_LOW);
        }

        mImageFileVoc.init1();
        mHTMLFileVoc.init1();
        EntryUrlVoc.INSTANCE.initInThread();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

            ArrayList<ShortcutInfo> list = new ArrayList<ShortcutInfo>();
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            list.add( new ShortcutInfo.Builder(getContext(), "idSearch")
                .setShortLabel( getContext().getString( R.string.menu_article_web_search ) )
                .setIcon(Icon.createWithResource(getContext(), R.drawable.cup_new_add))
                .setIntent(new Intent( Intent.ACTION_WEB_SEARCH )
                               .setPackage( getContext().getPackageName() )
                               .setClass(getContext(), ArticleWebSearchActivity.class))
                .build() );
            list.add( new ShortcutInfo.Builder(getContext(), "idExternal")
                .setShortLabel( getContext().getString( R.string.last_read ) )
                .setIcon(Icon.createWithResource(getContext(), R.drawable.cup_new_load_now))
                .setIntent(new Intent(getContext(), HomeActivityNewTask.class)
                           .setAction( Intent.ACTION_MAIN )
                           .setData( LAST_READ_CONTENT_URI ) )
                          .build() );
            list.add( new ShortcutInfo.Builder(getContext(), "idFavorities")
                          .setShortLabel( getContext().getString( R.string.favorites ) )
                          .setIcon(Icon.createWithResource(getContext(), R.drawable.cup_with_star))
                          .setIntent(new Intent(getContext(), HomeActivityNewTask.class)
                                         .setAction( Intent.ACTION_MAIN )
                                         .setData( FAVORITES_CONTENT_URI ))
                          .build() );
            list.add( new ShortcutInfo.Builder(getContext(), "idUnread")
                          .setShortLabel( getContext().getString( R.string.unread_entries ) )
                          .setIcon(Icon.createWithResource(getContext(), R.drawable.cup_new_unread))
                          .setIntent(new Intent(getContext(), HomeActivityNewTask.class)
                                         .setAction( Intent.ACTION_MAIN )
                                         .setData( UNREAD_ENTRIES_CONTENT_URI ))
                          .build() );

            shortcutManager.setDynamicShortcuts(list);
        }
    }

    private void createNotificationChannel(String channelId, int captionID, int importance) {
        Context context = MainApplication.getContext();
        NotificationChannel channel = new NotificationChannel(channelId, context.getString(captionID), importance);
        channel.setDescription(context.getString(captionID));
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void enlargeCursorWindowSize() {
        try {
            @SuppressLint("DiscouragedPrivateApi") Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize" );
            field.setAccessible( true );
            field.set( null, 100 * 1024 *  1024 );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
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
