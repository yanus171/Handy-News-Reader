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

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.AppBarLayout;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.fragment.GeneralPrefsFragment;
import ru.yanus171.feedexfork.parser.FileSelectDialog;
import ru.yanus171.feedexfork.parser.OPML;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.AutoJobService;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

import static ru.yanus171.feedexfork.Constants.DB_COUNT;
import static ru.yanus171.feedexfork.Constants.DB_IS_FALSE;
import static ru.yanus171.feedexfork.Constants.DB_IS_NULL;
import static ru.yanus171.feedexfork.Constants.DB_IS_TRUE;
import static ru.yanus171.feedexfork.Constants.DB_OR;
import static ru.yanus171.feedexfork.Constants.EXTRA_LINK;
import static ru.yanus171.feedexfork.MainApplication.mHTMLFileVoc;
import static ru.yanus171.feedexfork.MainApplication.mImageFileVoc;
import static ru.yanus171.feedexfork.activity.HomeActivity.AppBarLayoutState.COLLAPSED;
import static ru.yanus171.feedexfork.activity.HomeActivity.AppBarLayoutState.EXPANDED;
import static ru.yanus171.feedexfork.adapter.DrawerAdapter.EXTERNAL_ENTRY_POS;
import static ru.yanus171.feedexfork.adapter.DrawerAdapter.LABEL_GROUP_POS;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.ALL_LABELS;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.LABEL_ID_EXTRA;
import static ru.yanus171.feedexfork.fragment.EntryFragment.NEW_TASK_EXTRA;
import static ru.yanus171.feedexfork.parser.OPML.AUTO_BACKUP_OPML_FILENAME;
import static ru.yanus171.feedexfork.parser.OPML.importFromOpml;
import static ru.yanus171.feedexfork.parser.OPML.mImportFileSelectDialog;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.FAVORITES_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.LAST_READ_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.UNREAD_ENTRIES_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.IS_GROUP_EXPANDED;
import static ru.yanus171.feedexfork.provider.FeedData.getGroupExpandedValues;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.FileUtils.SUB_FOLDER;
import static ru.yanus171.feedexfork.view.EntryView.TAG;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.HideTapZonesText;

@SuppressWarnings("ConstantConditions")
public class HomeActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";
    private static final String STATE_IS_STATUSBAR_ENTRY_LIST_HIDDEN = "STATE_IS_STATUSBAR_ENTRY_LIST_HIDDEN";
    private static final String STATE_IS_ACTIONBAR_ENTRY_LIST_HIDDEN = "STATE_IS_ACTIONBAR_ENTRY_LIST_HIDDEN";

    private static final int FAVORITES_DRAWER_PAS = 2;
    private static final int LAST_READ_DRAWER_POS = 4;

    public View mPageUpBtn = null;
    public View mPageUpBtnFS = null;
    private int mStatus = 0;
    public boolean mIsNewTask = false;

    //private static final String FEED_ALL_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
    //        EntryColumns.FEED_ID + '=' + FeedColumns.TABLE_NAME + '.' + FeedColumns._ID + ')';


    private static final int LOADER_ID = 0;
    private static final int PERMISSIONS_REQUEST_IMPORT_FROM_OPML = 1;

    public EntriesListFragment mEntriesFragment;
    private DrawerLayout mDrawerLayout;
    private View mLeftDrawer;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mTitle;
    private int mCurrentDrawerPos;
    enum AppBarLayoutState {
        EXPANDED,
        COLLAPSED,
        IDLE
    };

    private AppBarLayoutState mAppBarLayoutState = AppBarLayoutState.IDLE;

    private boolean IsRememberLast() {
        return !mIsNewTask && PrefUtils.getBoolean( PrefUtils.REMEBER_LAST_ENTRY, true );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timer timer = new Timer( "HomeActivity.onCreate" );
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS );
        super.onCreate(savedInstanceState);
        mIsNewTask = getIntent() != null && getIntent().getBooleanExtra( NEW_TASK_EXTRA, false );

        if ( getIntent().hasCategory( "LoadLinkLater" ) )
            finish();
        else
            setContentView(R.layout.activity_home);

        if ( PrefUtils.getLong( PrefUtils.FIRST_LAUNCH_TIME, -1 ) == -1 )
            PrefUtils.putLong( PrefUtils.FIRST_LAUNCH_TIME, System.currentTimeMillis() );

        mEntriesFragment = (EntriesListFragment) getSupportFragmentManager().findFragmentById(R.id.entries_list_fragment);

        mTitle = getTitle().toString();

        mLeftDrawer = findViewById(R.id.left_drawer);
        //mLeftDrawer.setBackgroundColor(ContextCompat.getColor( this, PrefUtils.IsLightTheme() ?  R.color.light_background : R.color.dark_background));

        mDrawerList = findViewById(R.id.drawer_list);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> {
            selectDrawerItem(position);
            CloseDrawer();
        });
        mDrawerList.setOnItemLongClickListener((parent, view, position, id) -> {
            if ( position == DrawerAdapter.LABEL_GROUP_POS ) {
                startActivity(new Intent(getApplicationContext(), LabelListActivity.class ));
                return true;
            } else if ( DrawerAdapter.isLabelPos( position ) ) {

            } else if (id > 0) {
                startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedColumns.CONTENT_URI(id)));
                return true;
            }
            return false;
        });

        mCurrentDrawerPos = 0;
        if ( IsRememberLast() )
            mCurrentDrawerPos = PrefUtils.getInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);


        mDrawerLayout = findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {
                    Dog.v(String.format( "newNumber onDrawerOpened" ) );
                    if ( mDrawerAdapter != null ) {
                        mDrawerAdapter.notifyDataSetChanged();
                        mDrawerAdapter.updateNumbersAsync();
                    }
                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {

                }

                @Override
                public void onDrawerStateChanged(int newState) {

                }
            });
        }

        //if (!PrefUtils.getBoolean(PrefUtils.REMEBER_LAST_ENTRY, true))
        //    selectDrawerItem(0);

        Timer.Start( LOADER_ID, "HomeActivity.initLoader" );
        getLoaderManager().initLoader(LOADER_ID, null, this);

        if (Build.VERSION.SDK_INT >= 21 )
            AutoJobService.init(this);


        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                FetcherService.StartService(new Intent(HomeActivity.this, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS), true);
            }
        }

        // Ask the permission to import the feeds
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED  ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.storage_request_explanation).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_IMPORT_FROM_OPML);
                    }
                });
                builder.show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_IMPORT_FROM_OPML);
            }
        }

        TapZonePreviewPreference.SetupZoneSizes(findViewById(R.id.layout_root), false, true);

        {
            final View.OnClickListener listener = view -> {
                setFullScreen(GetIsStatusBarEntryListHidden(), !GetIsActionBarEntryListHidden());
                AppBarLayout appBar = mEntriesFragment.getView().findViewById(R.id.appbar);

                if (!GetIsActionBarEntryListHidden())
                    appBar.setExpanded(true);
                UpdateHeader();
            };
            findViewById(R.id.rightTopBtn).setOnClickListener(listener);
            findViewById(R.id.rightTopBtnFS).setOnClickListener(listener);
        }
        {
            final View.OnLongClickListener listener = view -> {
                mEntriesFragment.FilterByLabels();
                return true;
            };
            findViewById(R.id.rightTopBtn).setOnLongClickListener(listener);
            findViewById(R.id.rightTopBtnFS).setOnLongClickListener(listener);
        }
        {
            final View.OnClickListener listener = view -> {
                setFullScreen(!GetIsStatusBarEntryListHidden(), GetIsActionBarEntryListHidden());
            };
            findViewById(R.id.leftTopBtn).setOnClickListener( listener );
            findViewById(R.id.leftTopBtnFS).setOnClickListener( listener );
        }

        mPageUpBtn = findViewById( R.id.pageUpBtn );
        mPageUpBtnFS = findViewById( R.id.pageUpBtnFS );

        {
            View.OnClickListener listener = (view -> PageUpDown(-1));
            mPageUpBtn.setOnClickListener(listener);
            mPageUpBtnFS.setOnClickListener(listener);
        }
        {
            View.OnLongClickListener listener = (view -> {
                if ( mEntriesFragment.mListView.getCount() == 0 )
                    return false;
                mEntriesFragment.mListView.setSelection( 0 );
                Toast.makeText( HomeActivity.this, R.string.list_was_scrolled_to_top, Toast.LENGTH_SHORT ).show();
                return true;
            });

            mPageUpBtn.setOnLongClickListener(listener);
            mPageUpBtnFS.setOnLongClickListener(listener);
        }

        findViewById(R.id.pageDownBtn).setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                PageUpDown( 1 );
            }
        });
        findViewById(R.id.pageDownBtn).setOnLongClickListener(new TextView.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if ( mEntriesFragment.mListView.getCount() == 0 )
                    return false;
                mEntriesFragment.mListView.setSelection( mEntriesFragment.mListView.getCount() - 1 );
                Toast.makeText( HomeActivity.this, R.string.list_was_scrolled_to_bottom, Toast.LENGTH_SHORT ).show();
                return true;
            }
        });

        ( (AppBarLayout)mEntriesFragment.getView().findViewById(R.id.appbar) ).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            // State

            @Override
            public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {
                if (i == 0) {
                    mAppBarLayoutState = EXPANDED;
                } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()) {
                    mAppBarLayoutState = COLLAPSED;
                } else {
                    mAppBarLayoutState = AppBarLayoutState.IDLE;
                }
            }

        });
        timer.End();
    }

    private void UpdateHeader() {
        if (mEntriesFragment != null)
            mEntriesFragment.UpdateHeader();
    }

    static public boolean GetIsActionBarEntryListHidden() {
        return PrefUtils.getBoolean(STATE_IS_ACTIONBAR_ENTRY_LIST_HIDDEN, false);
    }
    static public boolean GetIsStatusBarEntryListHidden() {
        return PrefUtils.getBoolean(STATE_IS_STATUSBAR_ENTRY_LIST_HIDDEN, false);
    }

    public void setFullScreen( boolean statusBarHidden, boolean actionBarHidden ) {
        findViewById(R.id.leftTopBtn).setVisibility( actionBarHidden ? View.GONE : View.VISIBLE );
        findViewById(R.id.leftTopBtnFS).setVisibility( !actionBarHidden ? View.GONE : View.VISIBLE );
        findViewById(R.id.rightTopBtn).setVisibility( actionBarHidden ? View.GONE : View.VISIBLE );
        findViewById(R.id.rightTopBtnFS).setVisibility( !actionBarHidden ? View.GONE : View.VISIBLE );
        mPageUpBtn.setVisibility( actionBarHidden ? View.GONE : View.VISIBLE );
        mPageUpBtnFS.setVisibility( !actionBarHidden ? View.GONE : View.VISIBLE );
        setFullScreen( statusBarHidden, actionBarHidden, STATE_IS_STATUSBAR_ENTRY_LIST_HIDDEN, STATE_IS_ACTIONBAR_ENTRY_LIST_HIDDEN );
    }

    private void PageUpDown( int downOrUp ) {
        int appBarHeight = ( mAppBarLayoutState == EXPANDED ) ? mEntriesFragment.getView().findViewById(R.id.appbar).getHeight() : 0;
        View statusView =  mEntriesFragment.getView().findViewById(R.id.statusLayout);
        if ( GetIsActionBarEntryListHidden() )
            appBarHeight = getSupportActionBar().getHeight();
        final int statusHeight = statusView.isShown() ? statusView.getHeight() : 0;
        final float coeff = PrefUtils.getBoolean("page_up_down_90_pct", false) ? 0.9F : 0.98F;
        mEntriesFragment.mListView.smoothScrollBy((int) (downOrUp * ( mEntriesFragment.mListView.getHeight() - appBarHeight - statusHeight) * coeff), PAGE_SCROLL_DURATION_MSEC * 2 );
        if ( downOrUp > 0 )
            ( (AppBarLayout)mRootView.findViewById(R.id.appbar) ).setExpanded( false );
    }

    private void CloseDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mLeftDrawer);
                }
            }, 50);
        }
    }

    @Override
    public void onPause() {
        //EntriesCursorAdapter.mMarkAsReadList.clear();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timer timer = new Timer("HomeActivity.onResume");
        final Intent intent = getIntent();
        setIntent( new Intent() );

        if ( intent.getData() != null ) {
            mEntriesFragment.ClearSingleLabel();
            if ( intent.hasExtra(LABEL_ID_EXTRA) ) {
                mEntriesFragment.SetSingleLabel(intent.getLongExtra(LABEL_ID_EXTRA, 0));
                PrefUtils.putBoolean( DrawerAdapter.PREF_IS_LABEL_GROUP_EXPANDED, true );
                notifyDrawableAdapter();
            }
            if ( intent.getData().equals( FAVORITES_CONTENT_URI ) )
                selectDrawerItem(FAVORITES_DRAWER_PAS);
            else if ( intent.getData().equals( UNREAD_ENTRIES_CONTENT_URI ) )
                selectDrawerItem( 0 );
            else if ( !mEntriesFragment.mIsSingleLabel && intent.getData().equals(CONTENT_URI ) )
                selectDrawerItem( 1 );
            else if ( intent.getData().equals( ENTRIES_FOR_FEED_CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() ) ) )
                selectDrawerItem( 3 );
            else if ( intent.getData().equals( LAST_READ_CONTENT_URI ) )
                selectDrawerItem( LAST_READ_DRAWER_POS );
            else {
                if ( mEntriesFragment.mIsSingleLabel ) {
                    PrefUtils.putBoolean( DrawerAdapter.PREF_IS_LABEL_GROUP_EXPANDED, true );
                    notifyDrawableAdapter();
                    if ( mDrawerAdapter != null )
                        selectDrawerItem(DrawerAdapter.getLabelPositionByID(mEntriesFragment.GetSingleLabelID()));
                    mNewFeedUri = CONTENT_URI;
                } else {
                    final long feedID;
                    if ( intent.hasExtra( EXTRA_LINK ) ) {
                        feedID = EntryUrlVoc.INSTANCE.get( intent.getStringExtra( EXTRA_LINK ) );
                    } else
                        feedID = GetFeedID(intent.getData());
                    Log.v( TAG, "HomeActivity feedID = "  + feedID );
                    if ( expandFeedGroup(feedID) )
                        getLoaderManager().restartLoader(LOADER_ID, null, this);
                    else if ( mDrawerAdapter != null )
                        selectDrawerItem(mDrawerAdapter.getItemPosition(feedID));
                    if ( mDrawerAdapter == null )
                        mNewFeedUri = FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID);
                }

            }
            if ( IsRememberLast()  )
                PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        } else if ( IsRememberLast()  ) {
            String lastUri = PrefUtils.getString(PrefUtils.LAST_ENTRY_URI, "");
            if (!lastUri.isEmpty() && !lastUri.contains("-1")) {
                startActivity(FetcherService.GetEntryActivityIntent(Intent.ACTION_VIEW, Uri.parse(lastUri) ));
            }
        }

        if ( GeneralPrefsFragment.mSetupChanged ) {
            Timer.Start( LOADER_ID, "HomeActivity.restartLoader LOADER_ID" );
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
        //if ( mDrawerAdapter != null  )
        //    selectDrawerItem( mCurrentDrawerPos );
        setFullScreen( GetIsStatusBarEntryListHidden(), GetIsActionBarEntryListHidden() );
        HideTapZonesText(findViewById(R.id.layout_root));
        if ( mDrawerLayout != null )
            mDrawerLayout.findViewById( R.id.drawer_header ).setBackgroundColor( Theme.GetToolBarColorInt() );
        SetTaskTitle( mTitle );
        timer.End();
    }

    private boolean expandFeedGroup(long feedID) {
        boolean result = false;
        Cursor cur = getContentResolver().query(FeedColumns.CONTENT_URI(feedID), new String[]{ FeedColumns.GROUP_ID }, null, null, null );
        if ( cur.moveToNext() ) {
            final long groupID = cur.getLong(0 );
            if ( groupID > 0 ) {
                int records = getContentResolver().update( FeedColumns.CONTENT_URI(groupID),
                                                           getGroupExpandedValues(),
                                                           IS_GROUP_EXPANDED + DB_IS_FALSE  + DB_OR + IS_GROUP_EXPANDED + DB_IS_NULL,
                                                           null);
                result = records > 0;
            }
        }
        cur.close();
        return result;
    }

    private void notifyDrawableAdapter() {
        if ( mDrawerAdapter != null )
            mDrawerAdapter.notifyDataSetChanged();
    }

    private long GetFeedID(Uri uri) {
        final String result = GetSecondLastSegment(uri);
        return Long.parseLong( result );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // We reset the current drawer position
        // selectDrawerItem(0);
        setIntent( intent );
    }



    public void onBackPressed() {
        // Before exiting from app the navigation drawer is opened
        if (mDrawerLayout != null && !mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    public void onClickEditFeeds(View view) {
        startActivity(new Intent(this, EditFeedsListActivity.class));
    }
    public void onClickArticleWebSearch(View view) {
        startActivity( new Intent( Intent.ACTION_WEB_SEARCH )
                       .setPackage( this.getPackageName() )
                       .setClass( this, ArticleWebSearchActivity.class) );
    }

    public void onClickAdd(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        startActivity(new Intent(Intent.ACTION_INSERT).setData(FeedColumns.CONTENT_URI));
    }


    public void onClickSettings(View view) {
        startActivity(new Intent(this, GeneralPrefsActivity.class));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mImportFileSelectDialog.onActivityResult(this, requestCode, resultCode, data, true);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Timer.Start( LOADER_ID, "HomeActivity.onCreateLoader" );
        CursorLoader cursorLoader =
                new CursorLoader(this,
                        FeedColumns.GROUPED_FEEDS_CONTENT_URI,
                        new String[]{FeedColumns._ID, FeedColumns.URL, FeedColumns.NAME,
                                FeedColumns.IS_GROUP, FeedColumns.ICON_URL, FeedColumns.LAST_UPDATE,
                                FeedColumns.ERROR, FeedColumns.SHOW_TEXT_IN_ENTRY_LIST,
                                IS_GROUP_EXPANDED, FeedColumns.IS_AUTO_REFRESH, FeedColumns.OPTIONS, FeedColumns.IMAGES_SIZE},
                        "(" + FeedColumns.WHERE_GROUP + DB_OR +
                                     FeedColumns.GROUP_ID + DB_IS_NULL + DB_OR +
                                     FeedColumns.GROUP_ID + "=0" + DB_OR +
                                     FeedColumns.GROUP_ID + " IN (SELECT " + FeedColumns._ID +
                                " FROM " + FeedColumns.TABLE_NAME +
                                " WHERE " + IS_GROUP_EXPANDED + DB_IS_TRUE + "))" + FeedData.getWhereNotExternal(),
                        null,
                        null);
        //cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        mStatus = Status().Start( R.string.feed_list_loading, true );
        return cursorLoader;
    }


    public static Uri mNewFeedUri = Uri.EMPTY;
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Timer.End( LOADER_ID );
        Timer timer = new Timer( "HomeActivity.onLoadFinished" );

        boolean needSelect = false;
        if (mDrawerAdapter != null ) {
            mDrawerAdapter.setCursor(cursor);
        } else {
            mDrawerAdapter = new DrawerAdapter(this, cursor, mDrawerLayout.findViewById( R.id.progressBarDrawer ) );
            mDrawerList.setAdapter(mDrawerAdapter);
            // We don't have any menu yet, we need to display it
            needSelect = true;
        }
        if ( GeneralPrefsFragment.mSetupChanged ) {
            GeneralPrefsFragment.mSetupChanged = false;
            needSelect = true;
        }

        //Dog.v( TAG, "onLoadFinished mNewFeedUri = " + mNewFeedUri.toString() );

        if ( !mNewFeedUri.equals( Uri.EMPTY ) ) {
            if ( mEntriesFragment.IsAllLabels() ) {
                mCurrentDrawerPos = LABEL_GROUP_POS;
            } else if ( mEntriesFragment.mIsSingleLabel ) {
                mCurrentDrawerPos = DrawerAdapter.getLabelPositionByID(mEntriesFragment.GetSingleLabelID());
            } else {
                final long feedID = GetFeedID(mNewFeedUri);
                //Dog.v( TAG, "onLoadFinished feedID = " + feedID + ", mNewFeedUri = " + mNewFeedUri.toString() );
                if ( expandFeedGroup(feedID) ) {
                    getLoaderManager().restartLoader(LOADER_ID, null, this);
                    return;
                }
                mCurrentDrawerPos = mDrawerAdapter.getItemPosition(feedID);
            }
            needSelect = true;
            mDrawerList.smoothScrollToPosition( mCurrentDrawerPos );
            CloseDrawer();
            mNewFeedUri = Uri.EMPTY;
        }

        if ( needSelect )
            mDrawerList.post(() -> selectDrawerItem(mCurrentDrawerPos));

        timer.End();
        Status().End( mStatus );
    }

    private String GetSecondLastSegment( final Uri uri) {
        List<String> list = uri.getPathSegments();
        if ( list.size() == 3 )
            return list.get(list.size() - 2);
        else
            return "";
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDrawerAdapter.setCursor(null);
    }

    private void selectDrawerItem(int position) {
        Timer timer = new Timer( "HomeActivity.selectDrawerItem" );
        mCurrentDrawerPos = position;
        Uri newUri;
        mEntriesFragment.ClearSingleLabel();
        boolean showFeedInfo = true;

        switch (position) {
            case 0:
                newUri = UNREAD_ENTRIES_CONTENT_URI;
                mTitle = getString( R.string.unread_entries );
                break;
            case 1:
                newUri = CONTENT_URI;
                mTitle = getString( R.string.all_entries );
                break;
            case FAVORITES_DRAWER_PAS:
                newUri = FAVORITES_CONTENT_URI;
                mTitle = getString( R.string.favorites );
                break;
            case LAST_READ_DRAWER_POS:
                newUri = LAST_READ_CONTENT_URI;
                mTitle = getString( R.string.last_read );
                break;
            case EXTERNAL_ENTRY_POS:
                newUri = ENTRIES_FOR_FEED_CONTENT_URI( GetExtrenalLinkFeedID() );
                mTitle = getString( R.string.externalLinks );
                showFeedInfo = false;
                break;
            case LABEL_GROUP_POS:
                newUri = CONTENT_URI;
                mTitle = getString( R.string.labels_group_title );
                mEntriesFragment.SetSingleLabel( ALL_LABELS );
                showFeedInfo = true;
                break;
            default:
                if ( DrawerAdapter.isLabelPos( position )) {
                    newUri = CONTENT_URI;
                    mEntriesFragment.SetSingleLabel( DrawerAdapter.getLabelList().get( position - LABEL_GROUP_POS - 1 ).mID );
                    showFeedInfo = true;
                } else {
                    long feedOrGroupId = mDrawerAdapter.getItemId(position);
                    if (feedOrGroupId != -1) {
                        if (mDrawerAdapter.isItemAGroup(position)) {
                            newUri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                        } else {
                            newUri = ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
                            showFeedInfo = false;
                        }
                    } else
                        newUri = UNREAD_ENTRIES_CONTENT_URI;
                }
                mTitle = mDrawerAdapter.getItemName(position);

                break;

        }
        //if (!newUri.equals(mEntriesFragment.getUri()))
        mEntriesFragment.setData(newUri,
                showFeedInfo,
                false,
                mDrawerAdapter != null && mDrawerAdapter.isShowTextInEntryList(position),
                                 mDrawerAdapter != null ? mDrawerAdapter.getOptions(position) : new JSONObject());

        //mDrawerList.setSelection( position );
        mDrawerList.setItemChecked(position, true);
        //mDrawerList.smoothScrollToPositionFromTop(mFirstVisibleItem, 0, 0);

        // First open => we open the drawer for you
        if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
            PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false);
            if (mDrawerLayout != null) {
                mDrawerLayout.postDelayed(() -> mDrawerLayout.openDrawer(mLeftDrawer), 500);
            }
        }

        // Set title & icon
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setHomeAsUpIndicator( 0 );
            switch (mCurrentDrawerPos) {
                case 0:
                    SetActionbarIndicator( R.drawable.cup_new_unread);
                    break;
                case 1:
                    SetActionbarIndicator( R.drawable.cup_new_pot );
                    break;
                case FAVORITES_DRAWER_PAS:
                    SetActionbarIndicator( R.drawable.cup_new_star);
                    break;
                case LAST_READ_DRAWER_POS:
                    SetActionbarIndicator( R.drawable.cup_new_load_now);
                    break;
                case 3:
                    SetActionbarIndicator( R.drawable.cup_new_load_later );
                    break;
                default:
                    Drawable image = mDrawerAdapter.getItemIcon( position ) == null ?
                                     null :
                                     new BitmapDrawable( MainApplication.getContext().getResources(),
                                                         UiUtils.getScaledBitmap( mDrawerAdapter.getItemIcon( position ), 32 ) );
                    getSupportActionBar().setHomeAsUpIndicator( image );
                    break;
            }
            SetTaskTitle( mTitle );
            if ( mEntriesFragment.getView() != null )
                ( (AppBarLayout)mEntriesFragment.getView().findViewById(R.id.appbar) ).setExpanded( true );
        }
        if ( !mIsNewTask )
            PrefUtils.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);

        getSupportActionBar().setTitle( mTitle );
        // Put the good menu
        invalidateOptionsMenu();
        timer.End();
    }

    private void SetActionbarIndicator( int imageResource) {
        Bitmap original = BitmapFactory.decodeResource(getResources(), imageResource);
        int size = UiUtils.dpToPixel( 32 );
        Bitmap b = Bitmap.createScaledBitmap( original, size, size, false);
        Drawable d = new BitmapDrawable(getResources(), b);
        getSupportActionBar().setHomeAsUpIndicator( d );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mImageFileVoc.init1();
        mHTMLFileVoc.init1();
        //OPML.OnRequestPermissionResult(this, requestCode, grantResults);

        //if (requestCode == PERMISSIONS_REQUEST_IMPORT_FROM_OPML ) {
            // If request is cancelled, the result arrays are empty.
            if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                 new File(FileSelectDialog.Companion.getPublicDir().getAbsolutePath() + "/" + SUB_FOLDER, AUTO_BACKUP_OPML_FILENAME).exists() )
                Theme.CreateDialog( this )
                    .setMessage( R.string.import_from_backup_after_permission_granted )
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> importFromOpml( HomeActivity.this ) )
                    .setNegativeButton(android.R.string.no, null )
                    .create().show();
        //}
    }

    @Override
    public void onStart() {
        super.onStart();
        mBrightness.mTapAction = () -> PageUpDown(1);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if ( hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
            setFullScreen( GetIsStatusBarEntryListHidden(), GetIsActionBarEntryListHidden() );
    }
}
