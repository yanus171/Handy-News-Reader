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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.util.Date;
import java.util.regex.Matcher;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.fragment.EntryFragment.NO_DB_EXTRA;
import static ru.yanus171.feedexfork.service.FetcherService.GetEnryUri;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.utils.PrefUtils.DISPLAY_ENTRIES_FULLSCREEN;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;

public class EntryActivity extends BaseActivity {

    public EntryFragment mEntryFragment = null;

    private static final String STATE_IS_STATUSBAR_HIDDEN = "STATE_IS_STATUSBAR_HIDDEN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        mEntryFragment = (EntryFragment) getSupportFragmentManager().findFragmentById(R.id.entry_fragment);

        final Intent intent = getIntent();
        final String TEXT = MainApplication.getContext().getString(R.string.loadingLink) + "...";
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            final Matcher m = HtmlUtils.HTTP_PATTERN.matcher(text);
            if (m.find()) {
                final String url = text.substring(m.start(), m.end());
                final String title = text.substring(0, m.start());
                LoadAndOpenLink(url, title, TEXT);
            }
        } else if (intent.getScheme() != null && intent.getScheme().startsWith("http")) {
            final String url = intent.getDataString();
            final String title = intent.getDataString();
            LoadAndOpenLink(url, title, TEXT);
        }

        mEntryFragment.setData(getIntent().getData());

        //if (savedInstanceState == null) { // Put the data only the first time (the fragment will save its state)
        //}
        //mEntryFragment.setData(intent.getData());


        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        FetcherService.mCancelRefresh = false;

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        //setFullScreen( (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0, GetIsActionBarHidden() );
                        //PrefUtils.putBoolean(STATE_IS_STATUSBAR_HIDDEN, (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0);
                    }
                });

        if (getBoolean(DISPLAY_ENTRIES_FULLSCREEN, false))
            setFullScreen(true, true);
    }
    private void LoadAndOpenLink(final String url, final String title, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = GetEnryUri(url);
                if (entryUri == null) {
                    final String feedID = GetExtrenalLinkFeedID();
                    Timer timer = new Timer("LoadAndOpenLink insert");
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.TITLE, title);
                    values.put(EntryColumns.SCROLL_POS, 0);
                    values.put(EntryColumns.DATE, (new Date()).getTime());
                    values.put(EntryColumns.LINK, url);
                    values.put(EntryColumns.ABSTRACT, text);
                    values.put(EntryColumns.MOBILIZED_HTML, text);
                    entryUri = cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), values);
                    SetEntryID(entryUri);
                    entryUri = Uri.withAppendedPath(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), entryUri.getLastPathSegment());
                    PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, entryUri.toString());//FetcherService.OpenLink(entryUri);
                    timer.End();

                    FetcherService.LoadLink(feedID, url, title, FetcherService.ForceReload.Yes, true, true);
                } else
                    SetEntryID( entryUri );
            }

            private void SetEntryID(Uri entryUri) {
                final long entryID = Long.parseLong( entryUri.getLastPathSegment() );
                mEntryFragment.SetEntryID( 0, entryID );
                FetcherService.addActiveEntryID(entryID);
                UiUtils.RunOnGuiThread(new Runnable() {
                    @Override
                    public void run() {
                        mEntryFragment.getLoaderManager().restartLoader(0, null, mEntryFragment);
                    }
                } );
            }
        }).start();
        setIntent( getIntent().putExtra( NO_DB_EXTRA, true ) );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            setFullScreen();
    }

    private static final String STATE_IS_ACTIONBAR_HIDDEN = "STATE_IS_ACTIONBAR_HIDDEN";


    //public boolean mIsStatusBarHidden, mIsActionBarHidden;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Bundle b = getIntent().getExtras();
            if (b != null && b.getBoolean(Constants.INTENT_FROM_WIDGET, false)) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
            }
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


    }

    @Override
    public void onBackPressed() {
        /*SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        editor.putString(PrefUtils.LAST_ENTRY_URI, "");
        editor.commit();*/
        PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        FetcherService.clearActiveEntryID();
        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = getContentResolver();
                cr.delete(FeedData.TaskColumns.CONTENT_URI, FeedData.TaskColumns.ENTRY_ID + " = " + mEntryFragment.getCurrentEntryID(), null);
                FetcherService.setDownloadImageCursorNeedsRequery(true);

                if ( !mEntryFragment.mMarkAsUnreadOnFinish )
                    //mark as read
                    if ( mEntryFragment.getCurrentEntryID() != -1 )
                        cr.update(EntryColumns.CONTENT_URI(  mEntryFragment.getCurrentEntryID() ), FeedData.getReadContentValues(), null, null);


            }
        }.start();

        //mEntryFragment.mEntryPagerAdapter.GetEntryView( mEntryFragment.mEntryPagerAdapter.SaveScrollPos( true );
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

//        outState.putBoolean(STATE_IS_ACTIONBAR_HIDDEN, mIsActionBarHidden);
//        outState.putBoolean(STATE_IS_STATUSBAR_HIDDEN, mIsStatusBarHidden);

        super.onSaveInstanceState(outState);
    }

    @Override
    //protected void onRestoreInstanceState(Bundle savedInstanceState) {
    protected void onResume() {
//        setFullScreen(savedInstanceState.getBoolean(STATE_IS_STATUSBAR_HIDDEN),
//                savedInstanceState.getBoolean(STATE_IS_ACTIONBAR_HIDDEN));

        //super.onRestoreInstanceState(savedInstanceState);
        super.onResume();

        setFullScreen();
    }

    public void setFullScreen() {
        setFullScreen(GetIsStatusBarHidden(), GetIsActionBarHidden());
    }

    static public boolean GetIsStatusBarHidden() {
        return PrefUtils.getBoolean(STATE_IS_STATUSBAR_HIDDEN, false);
    }

    static public boolean GetIsActionBarHidden() {
        return PrefUtils.getBoolean(STATE_IS_ACTIONBAR_HIDDEN, false);
    }

    public void setFullScreen(boolean statusBarHidden, boolean actionBarHidden) {
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
        } else {
            setFullScreenOld(statusBarHidden);
        }

        if (getSupportActionBar() != null) {
            if (actionBarHidden)
                getSupportActionBar().hide();
            else
                getSupportActionBar().show();
        }
        if (mEntryFragment != null)
            mEntryFragment.UpdateFooter();

        invalidateOptionsMenu();
    }

//    public void setFullScreenWithNavBar() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE |
//                    //View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//
//        } else {
//            setFullScreenOld(true);
//        }
//
//    }

    private void setFullScreenOld(boolean fullScreen) {
        if (fullScreen) {

            if (GetIsStatusBarHidden()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Dog.d("onKeyDown isTracking = " + event.isTracking());
        boolean accepted = true;
        String pref = PrefUtils.getString("volume_buttons_action", PrefUtils.VOLUME_BUTTONS_ACTION_DEFAULT);
        if (pref.equals(PrefUtils.VOLUME_BUTTONS_ACTION_PAGE_UP_DOWN)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                mEntryFragment.PageDown();
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                mEntryFragment.PageUp();
            else
                accepted = false;
        } else if (pref.equals(PrefUtils.VOLUME_BUTTONS_ACTION_SWITCH_ENTRY)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                mEntryFragment.NextEntry();
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                mEntryFragment.PreviousEntry();
            else
                accepted = false;
        } else
            accepted = false;
        if (accepted)
            event.startTracking();
        return accepted || super.onKeyDown(keyCode, event);

    }

    @Override
    public boolean  onKeyLongPress(int keyCode, KeyEvent event) {
        Dog.d("onKeyDown isTracking = " + event.isTracking());
        return false;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }


}