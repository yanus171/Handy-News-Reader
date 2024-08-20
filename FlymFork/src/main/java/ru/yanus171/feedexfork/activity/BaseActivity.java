package ru.yanus171.feedexfork.activity;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.READING_NOTIFICATION;
import static ru.yanus171.feedexfork.utils.Theme.GetToolBarColorInt;

import android.app.ActivityManager;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.service.ReadingService;
import ru.yanus171.feedexfork.utils.Brightness;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;


public abstract class BaseActivity extends AppCompatActivity {

    View mDecorView;
    public Brightness mBrightness = null;

    public static final int PAGE_SCROLL_DURATION_MSEC = 450;
    private int mLastMax = 0;
    private int mLastProgress = 0;
    private int mLastStep = 0;
    public ProgressBar mProgressBarRefresh = null;
    private static int mActivityCount = 0;
    private View mMarginRight = null;
    private View mMarginLeft = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        InitLocale(this);
        UiUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();
        StartReadingServiceIfNeeded();
    }

    @Override
    public void onDestroy() {
        mActivityCount--;
        if ( mActivityCount == 0 )
            stopService( new Intent(this, ReadingService.class) );
        super.onDestroy();
    }

    private void StartReadingServiceIfNeeded() {
        if ( Build.VERSION.SDK_INT > 30 )
            return;
        if ( ( mActivityCount == 0 || !isServiceRunning( ReadingService.class ) ) && PrefUtils.getBoolean(READING_NOTIFICATION, false ) ){
            Intent serviceIntent = new Intent(this, ReadingService.class);
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
                startForegroundService( serviceIntent );
            else
                startService( serviceIntent );
        }
        mActivityCount++;
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
                mDecorView.setSystemUiVisibility(getFullScreenFlags());
            else
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        }
        if (getSupportActionBar() != null) {

            if (actionBarHidden)
                getSupportActionBar().hide();
            else
                getSupportActionBar().show();
        }
        UpdateHeader(mLastMax, mLastProgress, mLastStep, statusBarHidden, actionBarHidden);
        invalidateOptionsMenu();
    }

    private int getFullScreenFlags() {
        int result = View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if ( !PrefUtils.getBoolean( "show_navigation_bar_in_fullscreen", false ) )
            result = result | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        return result;

    }

    private View mHeaderLayout = null;
    public ProgressBar mProgressBar = null;
    private TextView mLabelClock = null;
    private TextView mLabelBattery = null;
    private TextView mLabelDate = null;
    private TextView mLabelRemaining = null;
    public View mRootView = null;

    public void UpdateHeaderProgressOnly(int max, int progress, int step) {
        if ( mProgressBar == null )
            return;
        mProgressBar.setMax( max );
        mLastMax = max;
        mLastProgress = progress;
        mLastStep = step;
        mProgressBar.setProgress( progress );
    }
    public void UpdateHeader(int max, int progress, int step, boolean isStatusBarHidden, boolean isToolBarHidden) {
        if ( mRootView == null )
            return;
        if ( mProgressBar == null || mLabelClock == null || mLabelBattery == null || mLabelDate == null || mLabelRemaining == null ) {
            mProgressBar = mRootView.findViewById(R.id.progressBar);
            mProgressBar.setProgress(0);
            mHeaderLayout = mRootView.findViewById(R.id.layoutColontitul);
            mLabelClock = UiUtils.SetupSmallTextView(mRootView, R.id.textClock);
            mLabelClock.setText( "" );
            mLabelBattery = UiUtils.SetupSmallTextView(mRootView, R.id.textBattery);
            mLabelBattery.setText( "" );
            mLabelDate = UiUtils.SetupSmallTextView(mRootView, R.id.textDate);
            mLabelDate.setText( "" );
            mLabelRemaining = UiUtils.SetupSmallTextView(mRootView, R.id.textRemaining);
            mLabelRemaining.setText( "" );
            mMarginRight = mRootView.findViewById(R.id.marginRight);
            mMarginLeft = mRootView.findViewById(R.id.marginLeft);
        }
        if ( mProgressBar == null || mLabelClock == null || mLabelBattery == null || mLabelDate == null ||  mLabelRemaining == null )
            return;
        boolean isLayoutVisible = false;
        if ( PrefUtils.getBoolean("article_text_footer_show_progress", true ) ) {
            isLayoutVisible = true;
            mProgressBar.setVisibility( View.VISIBLE );
            mProgressBar.setMax( max );
            mLastMax = max;
            mLastProgress = progress;
            mLastStep = step;
            mProgressBar.setProgress( progress );
            ( (LinearLayout) mProgressBar.getParent() ).setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor() ) );
            String color = Theme.GetColor( "article_text_footer_progress_color", R.string.default_article_text_footer_color);
            if (Build.VERSION.SDK_INT >= 21 )
                mProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor(color )));
            mProgressBar.setScaleY( PrefUtils.getIntFromText( "article_text_footer_progress_height", 1 ) );
        } else
            mProgressBar.setVisibility( View.GONE );
        isLayoutVisible = SetupHeaderLabel(mLabelClock, new SimpleDateFormat("HH:mm").format(new Date()), "article_text_footer_show_clock", isStatusBarHidden, isLayoutVisible, isToolBarHidden);
        isLayoutVisible = SetupHeaderLabel(mLabelBattery, GetBatteryText(), "article_text_footer_show_battery", isStatusBarHidden, isLayoutVisible, isToolBarHidden);
        isLayoutVisible = SetupHeaderLabel(mLabelDate, GetDateText(), "article_text_footer_show_date", isStatusBarHidden, isLayoutVisible, isToolBarHidden);
        isLayoutVisible = SetupHeaderLabel(mLabelRemaining, GetRemainingText( progress, max, step ), "article_text_footer_show_remaining", true, isLayoutVisible, isToolBarHidden);
        mRootView.findViewById( R.id.layoutColontitul ).setVisibility( isLayoutVisible ? View.VISIBLE : View.GONE );
        {
            final int backgroundColor = getHeaderBackgroundColor(isToolBarHidden);
            mRootView.findViewById( R.id.layoutColontitul ).setBackgroundColor( backgroundColor );
            if ( mProgressBarRefresh != null )
                mProgressBarRefresh.setBackgroundColor( backgroundColor );
        }
        {
            final int value = PrefUtils.getIntFromText("article_text_footer_offset", 0);
            mMarginRight.setMinimumWidth(UiUtils.mmToPixel(value));
            mMarginLeft.setMinimumWidth(UiUtils.mmToPixel(value));
        }
    }

    private static int getHeaderBackgroundColor(boolean isToolBarHidden) {
        return !isToolBarHidden ? Theme.GetToolBarColorInt() : Color.TRANSPARENT;
    }

    private static boolean SetupHeaderLabel( TextView textView, String text, String key, boolean show, boolean isLayoutVisible, boolean isToolBarHidden) {
        if ( PrefUtils.getBoolean(key, true ) && show ) {
            isLayoutVisible = true;
            textView.setVisibility( View.VISIBLE );
            textView.setTextSize(COMPLEX_UNIT_DIP, 8 + PrefUtils.getFontSizeFooterClock() );
            textView.setText( text );
            textView.setTextColor(Theme.GetColorInt("article_text_footer_clock_color", R.string.default_article_text_footer_color) );

            textView.setBackgroundColor( getHeaderBackgroundColor(isToolBarHidden) );
        } else
            textView.setVisibility( View.GONE );
        return isLayoutVisible;
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
    private String GetRemainingText( int value, int max, int step ) {
        if ( step == 0 )
            return "";
        if ( value < 0 )
            return "";
        final int result = (max - value) / step;
        if ( result <= 0 )
            return "";
        final int maxSteps = max / step;
        return maxSteps == result ? String.format("+%d", result) : String.format("+%d/%d", result, maxSteps);
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
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public void SetTaskTitle(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            setTaskDescription( new ActivityManager.TaskDescription( text, R.drawable.ic_cup, Theme.GetToolBarColorInt() ) );
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTaskDescription( new ActivityManager.TaskDescription( text ) );
    }

}
