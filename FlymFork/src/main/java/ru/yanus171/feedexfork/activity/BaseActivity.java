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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.Brightness;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.Theme.GetToolBarColorInt;


public abstract class BaseActivity extends AppCompatActivity {

    View mDecorView;
    public Brightness mBrightness = null;

    public static final int PAGE_SCROLL_DURATION_MSEC = 450;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        InitLocale(this);
        UiUtils.setTheme(this);
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
        if ( getSupportActionBar() != null )
            getSupportActionBar().setBackgroundDrawable( new ColorDrawable(GetToolBarColorInt() ) );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS );
            getWindow().setStatusBarColor(GetToolBarColorInt());
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
        if (defaultLanguage.equals(lang)) {
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


    public void setFullScreen( boolean statusBarHidden, boolean actionBarHidden,
                               String keyStatusBarHidden, String keyActionBarHidden) {
        PrefUtils.putBoolean(keyStatusBarHidden, statusBarHidden);
        PrefUtils.putBoolean(keyActionBarHidden, actionBarHidden);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (statusBarHidden)
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                                                     View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                                     View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                                     View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                                     View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                                     View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            else
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        } //else
          //  setFullScreenOld(statusBarHidden)

        if (getSupportActionBar() != null) {

            if (actionBarHidden)
                getSupportActionBar().hide();
            else
                getSupportActionBar().show();
        }

        invalidateOptionsMenu();
    }

    public static void UpdateFooter( ProgressBar progressBar, int max, int progress,
                                     TextView labelClock, TextView labelDate, TextView labelBattery, boolean isStatusBarHiddent) {
        if ( progressBar == null || labelClock == null || labelBattery == null || labelDate == null )
            return;
        if ( PrefUtils.getBoolean("article_text_footer_show_progress", true ) ) {
            progressBar.setVisibility( View.VISIBLE );
            progressBar.setMax( max );
            progressBar.setProgress( progress );
            ( (LinearLayout) progressBar.getParent() ).setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor() ) );
            String color = Theme.GetColor( "article_text_footer_progress_color", R.string.default_article_text_footer_color);
            if (Build.VERSION.SDK_INT >= 21 )
                progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor(color )));
            progressBar.setScaleY( PrefUtils.getIntFromText( "article_text_footer_progress_height", 1 ) );
        } else
            progressBar.setVisibility( View.GONE );

        if ( PrefUtils.getBoolean( "article_text_footer_show_clock", true ) && isStatusBarHiddent ) {
            labelClock.setTextSize(COMPLEX_UNIT_DIP, 8 + PrefUtils.getFontSizeFooterClock() );
            labelClock.setText( new SimpleDateFormat("HH:mm").format(new Date()) );
            labelClock.setTextColor(Theme.GetColorInt( "article_text_footer_clock_color", R.string.default_article_text_footer_color) );
            labelClock.setBackgroundColor( Theme.GetColorInt( "article_text_footer_clock_color_background", R.string.transparent_color) );
        } else {
            labelClock.setText("");
            labelClock.setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor() ) );
        }

        if ( PrefUtils.getBoolean( "article_text_footer_show_battery", true ) && isStatusBarHiddent ) {
            labelBattery.setTextSize(COMPLEX_UNIT_DIP, 8 + PrefUtils.getFontSizeFooterClock() );
            labelBattery.setText( GetBatteryText() );
            labelBattery.setTextColor(Theme.GetColorInt( "article_text_footer_clock_color", R.string.default_article_text_footer_color) );
            labelBattery.setBackgroundColor( Theme.GetColorInt( "article_text_footer_clock_color_background", R.string.transparent_color) );
        } else {
            labelBattery.setText("");
            labelBattery.setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor() ) );
        }

        if ( PrefUtils.getBoolean( "article_text_footer_show_date", true ) && isStatusBarHiddent ) {
            labelDate.setTextSize(COMPLEX_UNIT_DIP, 8 + PrefUtils.getFontSizeFooterClock() );
            labelDate.setText( GetDateText() );
            labelDate.setTextColor(Theme.GetColorInt( "article_text_footer_clock_color", R.string.default_article_text_footer_color) );
            labelDate.setBackgroundColor( Theme.GetColorInt( "article_text_footer_clock_color_background", R.string.transparent_color) );
        } else {
            labelDate.setText("");
            labelDate.setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor() ) );
        }
    }
    private static String GetBatteryText() {
        Intent intent = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        double level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        double scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = (int) (level / scale * 100);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        String statusStr = "";
        if (plugged != 0)
            statusStr = "~";
        return String.format("%s%d %%", statusStr, percent);
    }
    private static String GetDateText() {
        final Calendar cal = Calendar.getInstance();
        final String week = new DateFormatSymbols().getShortWeekdays()[cal.get(Calendar.DAY_OF_WEEK)];
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.format( "%d %s", day, week  );
    }
    protected TextView SetupTextView(int id) {
        TextView result = findViewById(id);
        UiUtils.SetupTextView(result);
        return result;
    }
    protected TextView SetupSmallTextView(int id) {
        TextView result = findViewById(id);
        UiUtils.SetupSmallTextView(result);
        return result;
    }
    protected void SetupFont( View view ) {
        if ( view instanceof ViewGroup ) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++)
                SetupFont( parent.getChildAt(i));
        } else if ( view instanceof TextView )
            UiUtils.SetTypeFace( (TextView) view);
    }
}
