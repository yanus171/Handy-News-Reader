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

package ru.yanus171.feedexfork.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import java.util.Date;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.ArticleWebSearchActivity;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.EntryActivityNewTask;
import ru.yanus171.feedexfork.activity.LocalFile;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.utils.WaitDialog;
import ru.yanus171.feedexfork.view.ControlPanel;
import ru.yanus171.feedexfork.view.Entry;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.EntryViewFactory;
import ru.yanus171.feedexfork.view.StatusText;
import ru.yanus171.feedexfork.view.WebEntryView;
import ru.yanus171.feedexfork.view.WebViewExtended;

import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.HTTPS_SCHEME;
import static ru.yanus171.feedexfork.Constants.HTTP_SCHEME;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.adapter.DrawerAdapter.newNumber;
import static ru.yanus171.feedexfork.fragment.EntryMenu.setItemVisible;
import static ru.yanus171.feedexfork.fragment.GeneralPrefsFragment.mSetupChanged;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.IsEntryUri;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_PROGRESS_INFO;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isTapActionEnabled;
import static ru.yanus171.feedexfork.view.EntryView.LoadIcon;
import static ru.yanus171.feedexfork.view.EntryView.TAG;


public class EntryFragment extends /*SwipeRefresh*/Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    public Uri mBaseUri;
    public BaseEntryPagerAdapter mEntryPagerAdapter;
    public StatusText mStatusText = null;
    public boolean mMarkAsUnreadOnFinish = false;
    public boolean mIsFinishing = false;
    public View mBtnEndEditing = null;
    public FeedFilters mFilters = null;
    public String mAnchor = "";
    public View mRootView = null;
    static public final String WHERE_SQL_EXTRA = "WHERE_SQL_EXTRA";

    public static final String NEW_TASK_EXTRA = "NEW_TASK_EXTRA";
    public static final String STATE_RELOAD_IMG_WITH_A_LINK = "STATE_REPLACE_IMG_WITH_A_LINK";
    public static final String STATE_RELOAD_WITH_DEBUG = "STATE_RELOAD_WITH_DEBUG";

    private boolean mIgnoreNextLoading = false;
    private int mCurrentPagerPos = 0;
    private int mLastPagerPos = -1;
    private long mInitialEntryId = -1;
    private ViewPager mEntryPager;
    private View mStarFrame = null;
    @SuppressLint("StaticFieldLeak")
    private static EntryView mLeakEntryView = null;
    private String mWhereSQL;
    EntryOrientation mOrientation;
    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";
    public EntryTapZones mTapZones = null;
    public ControlPanel mControlPanel = null;
    public EntryMenu mMenu = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu( true );
        mMenu = new EntryMenu( getActivity() );
        mOrientation = new EntryOrientation( this );
        Uri uri = getActivity().getIntent().getData();
        if ( IsExternalLink( uri ) || LocalFile.Is( uri ) )
            mEntryPagerAdapter = new SingleEntryPagerAdapter( this );
        else
            mEntryPagerAdapter = new EntryPagerAdapter( this );

        mWhereSQL = getActivity().getIntent().getStringExtra( WHERE_SQL_EXTRA );
        if ( savedInstanceState != null ) {
            mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS, 0);
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            //outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID, -1);
        }

        super.onCreate(savedInstanceState);
    }

    public static boolean IsExternalLink( Uri uri ) {
        return uri == null ||
            uri.toString().startsWith( HTTP_SCHEME ) ||
            uri.toString().startsWith( HTTPS_SCHEME ) ||
            LocalFile.Is( uri ) ;
    }

    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getBaseActivity().mRootView = inflater.inflate(R.layout.fragment_entry, container, true);
        mRootView = getBaseActivity().mRootView;
        if ( PrefUtils.isTapActionEnabled() )
            mTapZones = new EntryTapZones( this );
        else {
            mTapZones = null;
            EntryTapZones.hideAll( mRootView );
        }
        mControlPanel = new ControlPanel( mRootView, this );
        if ( mTapZones != null )
            mRootView.findViewById(R.id.entryCenterBtn).setOnClickListener(v -> {
                mTapZones.Hide();
                if ( mControlPanel.isVisible() )
                    mControlPanel.hide();
                else {
                    mControlPanel.show(GetSelectedEntryView());
                    setupControlPanelButtonActions();
                }
            });

        mStatusText = new StatusText( mRootView.findViewById(R.id.statusText ),
                                      mRootView.findViewById(R.id.errorText ),
                                      mRootView.findViewById(R.id.progressBarLoader),
                                      mRootView.findViewById(R.id.progressText),
                                      FetcherService.Status() );

        setupEndEditingButton();

        mEntryPager = mRootView.findViewById(R.id.pager);
        if (savedInstanceState != null) {
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID);
            mLastPagerPos = mCurrentPagerPos;
        }

        if ( mEntryPagerAdapter instanceof EntryPagerAdapter )
            setupPageChangeListener();

        mRootView.findViewById(R.id.layoutColontitul).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.statusText).setVisibility(View.GONE);
        if ( mTapZones != null ) {
            mRootView.findViewById(R.id.leftTopBtn).setOnClickListener(v -> {
                getEntryActivity().setFullScreen(!GetIsStatusBarHidden(), GetIsActionBarHidden());
                mControlPanel.hide();
            });
            mRootView.findViewById(R.id.leftTopBtn).setOnLongClickListener(view -> {
                if (!isTapActionEnabled())
                    return true;
                GetSelectedEntryView().OpenLabelSetup();
                return true;
            });

            mRootView.findViewById(R.id.entryLeftBottomBtn).setOnClickListener(v -> {
                GetSelectedEntryView().leftBottomBtnClick();
                mControlPanel.hide();
            });
            mRootView.findViewById(R.id.entryLeftBottomBtn).setOnLongClickListener(view -> {
                if (GetSelectedEntryWebView() == null)
                    return true;
                GetSelectedEntryWebView().DownloadAllImages();
                UiUtils.toast(R.string.downloadAllImagesStarted);
                return true;
            });

            mRootView.findViewById(R.id.entryRightBottomBtn).setOnClickListener(v -> {
                GetSelectedEntryView().rightBottomBtnClick();
                mControlPanel.hide();
            });
            mRootView.findViewById(R.id.entryRightBottomBtn).setOnLongClickListener(view -> {
                if (GetSelectedEntryWebView() == null)
                    return true;
                GetSelectedEntryWebView().ReloadFullText();
                UiUtils.toast(R.string.fullTextReloadStarted);
                return true;
            });

            TextView.OnClickListener listener = view -> {
                PageDown();
                mControlPanel.hide();
            };

            mRootView.findViewById(R.id.pageDownBtn).setOnClickListener(listener);
            //rootView.findViewById(R.id.pageDownBtnVert).setOnClickListener(listener);
            mRootView.findViewById(R.id.pageDownBtn).setOnLongClickListener(v -> {
                GetSelectedEntryView().LongClickOnBottom();
                return true;
            });
        }

        setupUpStarSwipe();

        SetStarFrameWidth(0);
        UpdateHeader();

        return mRootView;
    }

    void setupControlPanelButtonActions() {
        mOrientation.setupControlPanelButtonActions( mControlPanel, getUri() );
    }

    public void PageUp() {
        mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() ).ScrollOneScreen(-1);
    }

    public void PageDown() {
        mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() ).ScrollOneScreen(1);
    }

    public void NextEntry() {
        if ( mEntryPager.getCurrentItem() < mEntryPager.getAdapter().getCount() - 1  )
            mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() + 1, true );
    }
    public void PreviousEntry() {
        if ( mEntryPager.getCurrentItem() > 0  )
            mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() - 1, true );
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BASE_URI, mBaseUri);
        //outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
        outState.putLong(STATE_INITIAL_ENTRY_ID, mInitialEntryId);
        outState.putInt(STATE_CURRENT_PAGER_POS, mCurrentPagerPos);

        super.onSaveInstanceState(outState);
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ( (EntryActivity) getActivity() ).setFullScreen();
    }

    @Override
    public void onDetach() {
        ( (EntryActivity) getActivity() ).setFullScreen();

        super.onDetach();
    }

    @Override
    public void onDestroy() {
        FetcherService.Status().deleteObserver(mStatusText);
        ArticleTextExtractor.mLastLoadedAllDoc = "";
        PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, true);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        mIsFinishing = false;
        super.onResume();
        mEntryPagerAdapter.onResume();
        mMarkAsUnreadOnFinish = false;
        if ( mSetupChanged ) {
            mSetupChanged = false;
            mEntryPagerAdapter.generateArticleContent(mCurrentPagerPos);
        }
        mOrientation.onResume();
        if ( mTapZones != null )
            mTapZones.onResune();
        update(false);
        markPrevArticleAsRead();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (GetSelectedEntryView() != null )
            GetSelectedEntryView().onStart();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ( mOrientation.onConfigurationChanged( newConfig, GetSelectedEntryView() ) ) {
            mEntryPager.setAdapter(mEntryPagerAdapter);
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }
    }


    Uri getUri(){
        return ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
    }

    @Override
    public void onPause() {
        EntryView entryView = mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() );
        if (entryView != null)
            entryView.SaveScrollPos();

        mEntryPagerAdapter.onPause();
        super.onPause();
    }

    // -------------------------------------------------------------------------
    public static int DpToPx(float dp) {
        Resources r = MainApplication.getContext().getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return (int) px;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu.onCreateOptionsMenu(menu, inflater);
        final EntryView view = GetSelectedEntryView();
        if ( view != null )
            view.onCreateOptionsMenu(menu);
        mOrientation.onCreateOptionsMenu( menu );
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        if ( GetSelectedEntryView() != null )
            GetSelectedEntryView().onPrepareOptionsMenu( menu );
        setItemVisible( menu, R.id.menu_actionbar_visible, PrefUtils.isTapActionEnabled() );
        mOrientation.onPrepareOptionsMenu( menu );
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_close: {
                close();
                return true;
            }
            case R.id.menu_full_screen: {
                getEntryActivity().setFullScreen(!GetIsStatusBarHidden(), GetIsActionBarHidden() );
                item.setChecked( GetIsStatusBarHidden() );
            }
            case R.id.menu_actionbar_visible: {
                getEntryActivity().setFullScreen( GetIsStatusBarHidden(), !GetIsActionBarHidden() );
                item.setChecked( !GetIsActionBarHidden() );
            }
            case R.id.menu_mark_as_unread: {
                mMarkAsUnreadOnFinish = true;
                CloseEntry();
                final Uri uri = GetSelectedEntryView().getUri();
                new Thread() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.update(uri, FeedData.getUnreadContentValues(), null, null);
                    }
                }.start();
                UiUtils.toast( R.string.entry_marked_unread );
                break;
            }

            case R.id.menu_show_progress_info: {
                PrefUtils.toggleBoolean( SHOW_PROGRESS_INFO, false ) ;
                item.setChecked( PrefUtils.getBoolean( SHOW_PROGRESS_INFO, false ) );
                break;
            }
            case R.id.menu_article_web_search: {
                getActivity().startActivity( new Intent(Intent.ACTION_WEB_SEARCH)
                        .setPackage(getActivity().getPackageName())
                        .setClass(getActivity(), ArticleWebSearchActivity.class) );
                break;
            }
        }
        mOrientation.onOptionsItemSelected( item, getUri());
        GetSelectedEntryView().onOptionsItemSelected(item);

        return true;
    }


    public void close() {
        getActivity().runOnUiThread(() -> {
            if ( getActivity() != null )
                getActivity().onBackPressed();
        });
    }

    public EntryActivity getEntryActivity() {
        return (EntryActivity) getActivity();
    }

    public void CloseEntry() {
        if ( !getEntryActivity().mIsNewTask )
            PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        getActivity().finish();
    }

    public long getCurrentEntryID() {
        Entry entry = mEntryPagerAdapter.GetEntry( mCurrentPagerPos );
        if ( entry != null )
            return entry.mID;
        else
            return -1;
    }
    public String getCurrentFeedID() {
        if ( GetSelectedEntryView() != null )
            return GetSelectedEntryView().getFeedID();
        else
            return "";
    }
    @SuppressLint("StaticFieldLeak")
    public void setData(final Uri uri) {
        mCurrentPagerPos = 0;
        mBaseUri = null;
        Dog.v( TAG, "setData " + uri );

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Timer timer = new Timer( "EntryFr.setData" );
                Dog.v( String.format( "EntryFragment.setData( %s )", uri == null ? "" : uri.toString() ) );

                //PrefUtils.putString( PrefUtils.LAST_URI, uri.toString() );
                if ( mEntryPagerAdapter instanceof EntryPagerAdapter ) {
                    SetEntryReadTime( uri );
                    mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
                    if ( mBaseUri.toString().endsWith( "-1" ) )
                        mBaseUri = Uri.parse(mBaseUri.toString().replace("-1", "" ));
                    Dog.v(String.format("EntryFragment.setData( %s ) baseUri = %s", uri.toString(), mBaseUri));
                    try {
                        mInitialEntryId = Long.parseLong(uri.getLastPathSegment());
                    } catch (Exception unused) {
                        mInitialEntryId = -1;
                    }
                    final ContentResolver cr = MainApplication.getContext().getContentResolver();
                    String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;
                    // Load the entriesIds list. Should be in a loader... but I was too lazy to do so
                    try ( Cursor entriesCursor = cr.query( mBaseUri, FeedData.EntryColumns.PROJECTION_ID, mWhereSQL, null, FeedData.EntryColumns.DATE + entriesOrder) ) {
                        mEntryPagerAdapter.setData(entriesCursor );
                    }
                    if ( mInitialEntryId != -1 ){
                        final int index = mEntryPagerAdapter.GetEntryIndexByID( mInitialEntryId );
                        if ( index >= 0 ) {
                            mCurrentPagerPos = index;
                            mLastPagerPos = index;
                        }
                    }

                    if ( getCurrentEntryID() != -1 ) {
                        CancelStarNotification(getCurrentEntryID());
                        try (Cursor curEntry = cr.query(EntryColumns.CONTENT_URI(getCurrentEntryID()), new String[]{EntryColumns.FEED_ID}, null, null, null)) {
                            if (curEntry.moveToFirst()) {
                                final String feedID = curEntry.getString(0);
                                mFilters = new FeedFilters(feedID);
                            }
                        }
                    }
                } else if ( mEntryPagerAdapter instanceof SingleEntryPagerAdapter ) {
                    mBaseUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() );
                    mCurrentPagerPos = 0;
                }
                timer.End();
                return null;
            }


            @Override
            protected void onPostExecute(Void result) {
                if ( mEntryPager.getAdapter() == null )
                    mEntryPager.setAdapter(mEntryPagerAdapter);
                if (mCurrentPagerPos != -1) {
                    mEntryPager.setCurrentItem(mCurrentPagerPos);
                }
            }
        }.execute();
    }

    public void SetEntryReadTime(Uri entryUri) {
        if ( !IsEntryUri( entryUri ) )
            return;
        ContentValues values = new ContentValues();
        values.put(EntryColumns.READ_DATE, new Date().getTime());
        final ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.update( entryUri, values, null, null );
    }

    public void UpdateHeader() {
        EntryView entryView = GetSelectedEntryView();
        EntryView.ProgressInfo info = new EntryView.ProgressInfo();
        if (entryView != null)
            info = entryView.getProgressInfo();
        if ( getBaseActivity() != null )
            getBaseActivity().UpdateHeader( info.max,
                                            info.progress,
                                            info.step,
                                            GetIsStatusBarHidden(),
                                            GetIsActionBarHidden() );
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timer.Start( id, "EntryFr.onCreateLoader" );
        Entry entry = mEntryPagerAdapter.GetEntry( id );
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(entry.mID), null, null, null, null);
        cursorLoader.setUpdateThrottle(100);
        return cursorLoader;
    }

    public void onNewIntent() {
        mIgnoreNextLoading = true;
    }
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if ( mIsFinishing )
            return;
        if ( mIgnoreNextLoading ) {
            mIgnoreNextLoading = false;
            return;
        }
        final EntryView view = mEntryPagerAdapter.GetEntryView(loader.getId() );
        if ( view != null ) {
            view.setCursor( cursor );
            view.loadingDataFinished();
            if ( view == GetSelectedEntryView() )
                mOrientation.loadingDataFinished( view );
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        final EntryView view = mEntryPagerAdapter.GetEntryView( loader.getId() );
        if (view != null )
            view.setCursor(null);
    }

    public EntryView GetSelectedEntryView()  {
        return mEntryPagerAdapter.GetEntryView(mCurrentPagerPos);
    }
    public WebEntryView GetSelectedEntryWebView()  {
        final EntryView entryView = GetSelectedEntryView();
        if ( entryView instanceof WebEntryView )
            return ( WebEntryView )entryView;
        return null;
    }
    public WebViewExtended GetSelectedEntryWebViewExtended() {
        final EntryView entryView = GetSelectedEntryView();
        if ( entryView == null )
            return null;
        if ( !(entryView instanceof WebEntryView) )
            return null;
        return ((WebEntryView)entryView).mWebView;
    }

    public void update(boolean invalidateContent ) {
        mBtnEndEditing.setVisibility( View.GONE );
        mBtnEndEditing.setBackgroundColor( Theme.GetToolBarColorInt() );
        EntryView view = GetSelectedEntryView();
        if (view != null && view.mCursor != null ) {
            getEntryActivity().SetTaskTitle( view.mTitle );
            getEntryActivity().invalidateOptionsMenu();
            view.update( invalidateContent );
            mStatusText.SetEntryID(String.valueOf(view.mEntryId));
            startMobilizationTask(view.mEntryId);
        }
    }
    public void restartCurrentEntryLoader() {
        UiUtils.RunOnGuiThread(() -> {
            getLoaderManager().restartLoader(mCurrentPagerPos, null, EntryFragment.this);
        });
    }

    @NonNull
    public EntryView CreateWebEntryView(int position, ViewGroup container ) {
        final Entry entry = mEntryPagerAdapter.GetEntry(position);
        final EntryView view = EntryViewFactory.Create( entry.mLink, entry.mID, this, container, position );
        view.mView.setTag(view);

        if ( mLeakEntryView == null )
            mLeakEntryView  = view;
        view.StatusStartPageLoading();
        return view;
    }
    public boolean hasVideo() {
        final WebEntryView view = GetSelectedEntryWebView();
        if ( view != null )
            return view.hasVideo();
        return false;
    }

    public static void addArticleShortcut( final Activity activity, final String name, final long entryID, final Uri uri, String iconUrl ) {
        if ( ShortcutManagerCompat.isRequestPinShortcutSupported(activity) ) {
            //Adding shortcut for MainActivity on Home screen
            new WaitDialog(activity, R.string.downloadImage, () -> {
                final IconCompat icon = LoadIcon(iconUrl);
                UiUtils.RunOnGuiThread(() -> {
                    final Intent intent = new Intent(activity, EntryActivityNewTask.class)
                            .setAction(Intent.ACTION_VIEW)
                            .setData(uri)
                            .putExtra(NEW_TASK_EXTRA, true);
                    ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(activity, String.valueOf(entryID))
                            .setIcon(icon)
                            .setShortLabel(name)
                            .setIntent(intent)
                            .build();


                    ShortcutManagerCompat.requestPinShortcut(activity, pinShortcutInfo, null);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                        UiUtils.toast(R.string.new_entry_shortcut_added);
                });
            }).execute();
        } else
            UiUtils.toast( R.string.new_feed_shortcut_add_failed );
    }
    public boolean isCurrentPage( int position ) {
        return mCurrentPagerPos == position;
    }
    private void setupEndEditingButton() {
        mBtnEndEditing = mRootView.findViewById(R.id.btnEndEditing);
        mBtnEndEditing.setVisibility( View.GONE );
        mBtnEndEditing.setOnClickListener(view -> {
            GetSelectedEntryWebView().ReloadFullText();
            UiUtils.toast( R.string.fullTextReloadStarted );
            mControlPanel.hide();
        });
    }


    private void setupPageChangeListener() {
        mEntryPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onPageSelected(int i) {
                mCurrentPagerPos = i;

                final boolean isForward = mLastPagerPos < mCurrentPagerPos;
                mLastPagerPos = i;

                if ( !getEntryActivity().mIsNewTask )
                    PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID()).toString());

                CancelStarNotification( getCurrentEntryID() );

                update(false);
                markPrevArticleAsRead();

                if ( GetSelectedEntryView() != null )
                    GetSelectedEntryView().onPageSelected();

                final String text = String.format( "+%d", isForward ? mEntryPagerAdapter.getCount() - mLastPagerPos - 1 : mLastPagerPos );
                Toast toast = Toast.makeText( getContext(), text, Toast.LENGTH_SHORT );
                TextView textView = new TextView(getContext());
                textView.setText( text );
                textView.setPadding( 10, 10, 10, 10 );
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
                    textView.setBackgroundResource( R.drawable.toast_background );
                toast.setView( textView );
                toast.show();
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    private void setupUpStarSwipe() {
        final Vibrator vibrator = (Vibrator) getContext().getSystemService( Context.VIBRATOR_SERVICE );
        mStarFrame = mRootView.findViewById(R.id.frameStar);
        final ImageView frameStarImage  = mRootView.findViewById(R.id.frameStarImage);
        final boolean prefVibrate = getBoolean(VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE, true);
        mRootView.findViewById(R.id.pageUpBtn).setOnTouchListener(new View.OnTouchListener() {
            private int initialY = 0;
            private boolean mWasVibrate = false;
            private boolean mWasSwipe = false;
            private final int MAX_HEIGHT = UiUtils.mmToPixel( 12 );
            private final int MIN_HEIGHT = UiUtils.mmToPixel( 1 );
            private long downTime = 0;
            private boolean wasUp = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                if ( event.getAction() == MotionEvent.ACTION_DOWN) {
                    //Dog.v( "onTouch ACTION_DOWN " );
                    initialY = (int) event.getY();
                    mWasVibrate = false;
                    mWasSwipe = false;
                    downTime = SystemClock.elapsedRealtime();
                    wasUp = false;
                    UiUtils.RunOnGuiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!wasUp && !mWasSwipe) {
                                GetSelectedEntryView().GoTop();
                                UiUtils.toast( R.string.list_was_scrolled_to_top );
                            }
                        }
                    }, ViewConfiguration.getLongPressTimeout() );
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_MOVE) {
                    //Dog.v("onTouch ACTION_MOVE " + (event.getY() - initialY));
                    int w = Math.max(0, (int) (event.getY() - initialY));
                    SetStarFrameWidth(Math.min(w, MAX_HEIGHT));
                    if (prefVibrate && w >= MAX_HEIGHT && !mWasVibrate) {
                        mWasVibrate = true;
                        vibrator.vibrate(VIBRATE_DURATION);
                    } else if (w < MAX_HEIGHT)
                        mWasVibrate = false;
                    if (w >= MIN_HEIGHT) {
                        mWasSwipe = true;
                        downTime = SystemClock.elapsedRealtime();
                    }
                    frameStarImage.setImageResource((w >= MAX_HEIGHT) == GetSelectedEntryView().mFavorite ? R.drawable.ic_star_border_yellow : R.drawable.ic_star_yellow);
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_UP) {
                    //Dog.v( "onTouch ACTION_UP " );
                    if ( !mWasSwipe ) {
                        if ( !IsLong() )
                            PageUp();
                    } else if ( event.getY() - initialY >= MAX_HEIGHT ) {
                        GetSelectedEntryView().SetIsFavorite( !GetSelectedEntryView().mFavorite, true );
                    }
                    SetStarFrameWidth(0);
                    wasUp = true;
                    return true;
                } else
                    SetStarFrameWidth(0);
                return false;
            }

            private boolean IsLong() {
                return SystemClock.elapsedRealtime() - downTime > ViewConfiguration.getLongPressTimeout();
            }
        });
    }


    private void SetStarFrameWidth(int w) {
        mStarFrame.setLayoutParams( new FrameLayout.LayoutParams( FrameLayout.LayoutParams.FILL_PARENT, w));
    }





    private String getCurrentEntryLink() {
        Entry entry = mEntryPagerAdapter.GetEntry( mCurrentPagerPos );
        if ( entry != null )
            return entry.mLink;
        else
            return "";
    }

    private boolean IsFeedUri( Uri uri ) {
        boolean result = false;
        if ( uri != null && uri.getPathSegments().size() > 1 )
            try {
                Long.parseLong(uri.getPathSegments().get(1));
                result = true;
            } catch ( NumberFormatException ignored ) { }
        return result;
    }

    private void markPrevArticleAsRead() {
        // Mark the previous opened article as read
        //if (entryCursor.getInt(mIsReadPos) != 1) {
        EntryView view = GetSelectedEntryView();
        if ( !mMarkAsUnreadOnFinish && mLastPagerPos != -1 && view != null && view.mCursor != null ) {
            new Thread() {
                private String mFeedID;
                private boolean mSetAsRead;
                private int mPagerPos;
                private Thread init(int pagerPos, boolean setAsRead, String feedID) {
                    mPagerPos = pagerPos;
                    mSetAsRead = setAsRead;
                    mFeedID = feedID;
                    return this;
                }
                @Override
                public void run() {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, mEntryPagerAdapter.GetEntry( mPagerPos ).mID);
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    if ( mSetAsRead ) {
                        if ( cr.update(uri, FeedData.getReadContentValues(), EntryColumns.WHERE_UNREAD + DB_AND + EntryColumns.WHERE_NOT_FAVORITE , null) > 0 )
                            newNumber(mFeedID, DrawerAdapter.NewNumberOperType.Update, true );
                    }
                    cr.update(uri, FeedData.getOldContentValues(), EntryColumns.WHERE_NEW, null);
                            /*// Update the cursor
                            Cursor updatedCursor = cr.query(uri, null, null, null, null);
                            updatedCursor.moveToFirst();
                            mEntryPagerAdapter.setUpdatedCursor(mPagerPos, updatedCursor);*/
                }
            }.init( mLastPagerPos,
                    view.getIsUnReadFromCursor(),
                    getCurrentFeedID() ).start();
        }
    }
    private void startMobilizationTask(long currentEntryID) {
        new Thread() {
            long mID;
            @Override
            public void run() {
                if (FetcherService.hasMobilizationTask(currentEntryID)) {
                    //--showSwipeProgress();
                    // If the service is not started, start it here to avoid an infinite loading
                    if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false))
                        FetcherService.Start(new Intent(MainApplication.getContext(), FetcherService.class)
                                .setAction(FetcherService.ACTION_MOBILIZE_FEEDS), true);
                }
            }
            Thread SetID( long id ) {
                mID = id;
                return this;
            }
        }.SetID(currentEntryID).start();
    }
}
