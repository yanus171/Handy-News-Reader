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
package ru.yanus171.feedexfork.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import java.util.Locale;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.Brightness;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.service.FetcherService.Status;


public abstract class BaseActivity extends AppCompatActivity {

    View mDecorView;
    public Brightness mBrightness = null;

    public static final int PAGE_SCROLL_DURATION_MSEC = 450;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        InitLocale(this);
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( mBrightness == null && findViewById( R.id.dimFrame ) != null )
            mBrightness = new Brightness( this, findViewById( R.id.dimFrame ).getRootView() );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(Constants.NOTIFICATION_ID_REFRESH_SERVICE);
        }
        if ( mBrightness != null ) {
            mBrightness.OnResume();
        }
        Status().UpdateText();
    }

    @Override
    public void onPause() {
        if ( mBrightness != null ) {
            mBrightness.OnPause();
        }
        Status().ClearProgress();
        super.onPause();
    }
    // ----------------------------------------------------------------
    private final static String defaultLanguage = "System";
    private static Locale GetLocale(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = prefs.getString(PrefUtils.LANGUAGE, defaultLanguage);
        if (lang.equals(defaultLanguage)) {
            if (context.getResources().getConfiguration().locale != null) {
                lang = context.getResources().getConfiguration().locale.getLanguage();
            }
        }
        return new Locale(lang);
    }

    public static Context InitLocale( final Context context ) {
        Context newContext = context;
        Locale locale = GetLocale(context);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        if (Build.VERSION.SDK_INT >= 17) {
            newContext = context.createConfigurationContext(config);
        } else
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        return newContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext( InitLocale( base ));
    }

    private static final String STATE_IS_STATUSBAR_HIDDEN = "STATE_IS_STATUSBAR_HIDDEN";
    private static final String STATE_IS_ACTIONBAR_HIDDEN = "STATE_IS_ACTIONBAR_HIDDEN";

    static public boolean GetIsStatusBarHidden() {
        return PrefUtils.getBoolean(STATE_IS_STATUSBAR_HIDDEN, false);
    }
    static public boolean GetIsActionBarHidden() {
        return PrefUtils.getBoolean(STATE_IS_ACTIONBAR_HIDDEN, false);
    }
    public void setFullScreen() {
        setFullScreen( GetIsStatusBarHidden(), GetIsActionBarHidden());
    }

    public void setFullScreen( boolean statusBarHidden, boolean actionBarHidden) {
        PrefUtils.putBoolean(STATE_IS_STATUSBAR_HIDDEN, statusBarHidden);
        PrefUtils.putBoolean(STATE_IS_ACTIONBAR_HIDDEN, actionBarHidden);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (statusBarHidden) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                                     View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                                     View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            } else {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
//        } else {
//            setFullScreenOld(statusBarHidden);
        }

        if (getSupportActionBar() != null) {
            if (actionBarHidden)
                getSupportActionBar().hide();
            else
                getSupportActionBar().show();
        }

        invalidateOptionsMenu();
    }

}
