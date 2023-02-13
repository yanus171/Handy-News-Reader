/**
 * Flym
 * <p>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.ArticleWebSearchActivity;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.activity.HomeActivityNewTask;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.adapter.EntriesCursorAdapter;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.parser.OPML;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.EntryLabelColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.Label;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.utils.WaitDialog;
import ru.yanus171.feedexfork.view.Entry;
import ru.yanus171.feedexfork.view.StatusText;

import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_ASC;
import static ru.yanus171.feedexfork.Constants.DB_DESC;
import static ru.yanus171.feedexfork.Constants.EMPTY_WHERE_SQL;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.AUTO_SET_AS_READ;
import static ru.yanus171.feedexfork.adapter.DrawerAdapter.newNumber;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.ShowDialog;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.TakeMarkAsReadList;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.getItemIsRead;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.mMarkAsReadList;
import static ru.yanus171.feedexfork.fragment.EntryFragment.LoadIcon;
import static ru.yanus171.feedexfork.fragment.EntryFragment.NEW_TASK_EXTRA;
import static ru.yanus171.feedexfork.fragment.EntryFragment.WHERE_SQL_EXTRA;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.LAST_READ_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_NOT_FAVORITE;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.URI_ENTRIES_FOR_FEED;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_BIG_IMAGE;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_CATEGORY;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_TEXT_PREVIEW;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_URL;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_PROGRESS_INFO;
import static ru.yanus171.feedexfork.utils.UiUtils.CreateTextView;
import static ru.yanus171.feedexfork.view.EntryView.mImageDownloadObservable;

public class EntriesListFragment extends /*SwipeRefreshList*/Fragment implements Observer {
    public static final String STATE_CURRENT_URI = "STATE_CURRENT_URI";
    private static final String STATE_ORIGINAL_URI = "STATE_ORIGINAL_URI";
    private static final String STATE_SHOW_FEED_INFO = "STATE_SHOW_FEED_INFO";
    //private static final String STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE";
    private static final String STATE_SHOW_TEXT_IN_ENTRY_LIST = "STATE_SHOW_TEXT_IN_ENTRY_LIST";
    private static final String STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST = "STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST";
    private static final String STATE_SHOW_UNREAD_ONLY = "STATE_SHOW_UNREAD";
    private static final String STATE_LAST_VISIBLE_ENTRY_ID = "STATE_LAST_VISIBLE_ENTRY_ID";
    private static final String STATE_LAST_VISIBLE_OFFSET = "STATE_LAST_VISIBLE_OFFSET";
    private static final String STATE_LABEL_FILTER_LIST = "STATE_LABEL_FILTER_LIST";


    private static final int ENTRIES_LOADER_ID = 1;
    //private static final int NEW_ENTRIES_NUMBER_LOADER_ID = 2;
    private static final int FILTERS_LOADER_ID = 3;
    private static final String STATE_OPTIONS = "STATE_OPTIONS";
    public static final long ALL_LABELS = -2L;
    private static final long NO_LABEL = -1L;
    public static final String LABEL_ID_EXTRA = "LABEL_ID";

    public static Uri mCurrentUri = null;

    private Uri mOriginalUri;
    private boolean mOriginalUriShownEntryText = false;
    private boolean mShowFeedInfo = false;
    private boolean mShowTextInEntryList = false;
    public EntriesCursorAdapter mEntriesCursorAdapter = null;
    private Cursor mJustMarkedAsReadEntries;
    private FloatingActionButton mFab;
    public ListView mListView;
    public static boolean mShowUnReadOnly = false;
    private boolean mNeedSetSelection = false;
    private long mLastVisibleTopEntryID = 0;
    private int mLastListViewTopOffset = 0;
    private Menu mMenu = null;
    private TextView mTextViewFilterLabels = null;
    private FeedFilters mFilters = null;
    private boolean mIsResumed = false;
    static private boolean mIsSearch = false;
    private HashSet<Long> mLabelsID = new HashSet<Long>();
    public boolean mIsSingleLabel = false;

    //private long mListDisplayDate = new Date().getTime();
    //boolean mBottomIsReached = false;
    static class VisibleReadItem {
        final String ITEM_SEP = "__####__";

        String mUri;
        Boolean mIsRead = false;
        public VisibleReadItem( String uri, boolean isRead ) {
            mUri = uri;
            mIsRead = isRead;
        }
        public VisibleReadItem( String data ) {
            String[] list = TextUtils.split( data, ITEM_SEP );
            mIsRead = Boolean.parseBoolean( list[0] );
            mUri = list[1];
        }
        String ToString() {
            return mIsRead.toString() + ITEM_SEP + mUri;
        }
    }
    public static final HashSet<String> mWasVisibleList = new HashSet<>();
    private JSONObject mOptions;

    public boolean IsOldestFirst() { return mShowTextInEntryList || PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false); }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if ( id == ENTRIES_LOADER_ID ) {
                Timer.Start(ENTRIES_LOADER_ID, "EntriesListFr.onCreateLoader");

                String entriesOrder = IsOldestFirst() ? DB_ASC : DB_DESC;
                //String where = "(" + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
                String[] projection = EntryColumns.PROJECTION_WITH_TEXT;//   mShowTextInEntryList ? EntryColumns.PROJECTION_WITH_TEXT : EntryColumns.PROJECTION_WITHOUT_TEXT;
                final String where = GetWhereSQL();
                String orderField = mCurrentUri == LAST_READ_CONTENT_URI ? EntryColumns.READ_DATE: EntryColumns.DATE;
                CursorLoader cursorLoader = new CursorLoader(getActivity(), mCurrentUri, projection, where, null, orderField + entriesOrder);
                cursorLoader.setUpdateThrottle(150);
                Status().End(mStatus);
                mStatus = Status().Start(R.string.article_list_loading, true);
                return cursorLoader;
            } else if ( id == FILTERS_LOADER_ID) {
                Timer.Start(FILTERS_LOADER_ID, "EntriesListFr.Filters.onCreateLoader");
                final String feedID = mCurrentUri.getPathSegments().get(1);
                CursorLoader cursorLoader = new CursorLoader(getActivity(), FeedFilters.getCursorUri(feedID), FeedFilters.getCursorProjection(), null, null, null);
                cursorLoader.setUpdateThrottle(150);
                //Status().End(mStatus);
                //mStatus = Status().Start(R.string.article_list_loading, true);
                return cursorLoader;
            }
            return null;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            if (loader.getId() == ENTRIES_LOADER_ID) {
                Timer.End(ENTRIES_LOADER_ID);
                Timer timer = new Timer("EntriesListFragment.onCreateLoader");

                    mEntriesCursorAdapter.changeCursor(data);
                    if (mNeedSetSelection) {
                        mNeedSetSelection = false;
                        mListView.setSelection(IsOldestFirst() ? mEntriesCursorAdapter.GetTopNewPos() : mEntriesCursorAdapter.GetBottomNewPos());
                    }
                    RestoreListScrollPosition();

                getActivity().setProgressBarIndeterminateVisibility(false);
                UpdateTopTapZoneVisibility();
                UiUtils.RunOnGuiThread( () -> UpdateHeader(), 1000 );

                Status().End(mStatus);
                timer.End();
            } else if (loader.getId() == FILTERS_LOADER_ID && mEntriesCursorAdapter != null ) {
                mFilters = new FeedFilters(data);
                mEntriesCursorAdapter.setFilter(mFilters);
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            if ( loader.getId() == ENTRIES_LOADER_ID ) {
                Status().End( mStatus );
                //getActivity().setProgressBarIndeterminateVisibility( true );
                mEntriesCursorAdapter.changeCursor(Constants.EMPTY_CURSOR);
            }
        }

    };

    @NotNull
    public String GetWhereSQL() {
        String labelSQL = "";
        if ( mLabelsID.contains( ALL_LABELS ) )
            labelSQL = DB_AND + EntryColumns._ID + " IN ( SELECT " + EntryLabelColumns.ENTRY_ID + " FROM " + EntryLabelColumns.TABLE_NAME + ")";
        else if ( !mLabelsID.isEmpty() ) {
            ArrayList<String> listCondition = new ArrayList<>();
            for( Long item: mLabelsID )
                listCondition.add( "(" + EntryColumns._ID + " IN ( SELECT " + EntryLabelColumns.ENTRY_ID + " FROM " + EntryLabelColumns.TABLE_NAME +
                    " WHERE " + EntryLabelColumns.LABEL_ID + " = " + item + "))" );
            labelSQL = DB_AND + TextUtils.join(DB_AND, listCondition);
        }
        final String unreadSQL = mShowUnReadOnly ? " AND " + EntryColumns.WHERE_UNREAD : "";
        return EMPTY_WHERE_SQL + (mIsSearch ? "" : (labelSQL + unreadSQL));
    }

    private void RestoreListScrollPosition() {
        if ( mLastVisibleTopEntryID != -1 ) {
            int pos = mEntriesCursorAdapter.GetPosByID(mLastVisibleTopEntryID);
            if ( pos != -1 )
                mListView.setSelectionFromTop(pos, mLastListViewTopOffset);
        }
    }

    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.IS_REFRESHING.equals(key)) {
                UpdateActions();
            }
        }
    };
    private StatusText mStatusText = null;
    public static Uri mSearchQueryUri = null;
    private int mStatus = 0;

    private void UpdateActions() {
        if ( mMenu == null )
            return;

        mMenu.findItem(R.id.menu_share_starred).setVisible(true);

        MenuItem item = mMenu.findItem( R.id.menu_toogle_toogle_unread_all );
        if (mShowUnReadOnly) {
            item.setTitle(R.string.all_entries);
            item.setIcon(R.drawable.ic_check_box_outline_blank);
        } else {
            item.setTitle(R.string.unread_entries);
            item.setIcon(R.drawable.ic_check_box);
        }

        if ( mCurrentUri != null ) {
            int uriMatch = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
            item.setVisible( uriMatch != FeedDataContentProvider.URI_UNREAD_ENTRIES );
            mMenu.findItem(R.id.menu_show_entry_text).setVisible( uriMatch != FeedDataContentProvider.URI_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_UNREAD_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_FAVORITES );
            mMenu.findItem( R.id.menu_show_entry_text ).setChecked( mShowTextInEntryList );
        }

        boolean isCanRefresh = !EntryColumns.FAVORITES_CONTENT_URI.equals( mCurrentUri ) && !EntryColumns.LAST_READ_CONTENT_URI.equals( mCurrentUri ) && !mIsSingleLabel;
        if ( mCurrentUri != null && mCurrentUri.getPathSegments().size() > 1 ) {
            String feedID = mCurrentUri.getPathSegments().get(1);
            isCanRefresh = !feedID.equals(FetcherService.GetExtrenalLinkFeedID());
        }
        boolean isRefresh = PrefUtils.getBoolean( PrefUtils.IS_REFRESHING, false );
        mMenu.findItem(R.id.menu_cancel_refresh).setVisible( isRefresh );
        mMenu.findItem(R.id.menu_refresh).setVisible( !isRefresh && isCanRefresh );
        mMenu.findItem(R.id.menu_filter_by_labels).setVisible( !mIsSingleLabel );

        if ( getBaseActivity().mProgressBarRefresh != null ) {
            if (isRefresh)
                getBaseActivity().mProgressBarRefresh.setVisibility(View.VISIBLE);
            else
                getBaseActivity().mProgressBarRefresh.setVisibility(View.GONE);
        }

        mTextViewFilterLabels.setVisibility((mIsSingleLabel || mIsSearch || mLabelsID.isEmpty()) ? View.GONE : View.VISIBLE );
        mTextViewFilterLabels.setText(Html.fromHtml( getContext().getString( R.string.filter_label_title ) + ": " + LabelVoc.INSTANCE.getStringList(mLabelsID ) ) );
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timer timer = new Timer( "EntriesListFragment.onCreate" );

        setHasOptionsMenu(true);

        Dog.v( "EntriesListFragment.onCreate" );

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
            mOriginalUri = savedInstanceState.getParcelable(STATE_ORIGINAL_URI);
            mOriginalUriShownEntryText = savedInstanceState.getBoolean(STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST);
            mShowFeedInfo = savedInstanceState.getBoolean(STATE_SHOW_FEED_INFO);
            mShowTextInEntryList = savedInstanceState.getBoolean(STATE_SHOW_TEXT_IN_ENTRY_LIST);
            mShowUnReadOnly = savedInstanceState.getBoolean(STATE_SHOW_UNREAD_ONLY, PrefUtils.getBoolean(STATE_SHOW_UNREAD_ONLY, false ));
            try {
                mOptions = savedInstanceState.containsKey( STATE_OPTIONS ) ? new JSONObject( savedInstanceState.getString( STATE_OPTIONS ) ) : new JSONObject();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Dog.v( String.format("EntriesListFragment.onCreate mShowUnRead = %b", mShowUnReadOnly) );

            //if ( mShowTextInEntryList )
            //    mNeedSetSelection = true;
            HomeActivity activity = (HomeActivity) getActivity();
            mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mShowTextInEntryList, mShowUnReadOnly, GetActivity());
        } else
            mShowUnReadOnly = PrefUtils.getBoolean(STATE_SHOW_UNREAD_ONLY, false );

        timer.End();
    }

    @Override
    public void onStart() {
        super.onStart();
        Timer timer = new Timer( "EntriesListFragment.onStart" );

        //refreshUI(); // Should not be useful, but it's a security
        //refreshSwipeProgress();
        PrefUtils.registerOnPrefChangeListener(mPrefListener);

        mFab = getActivity().findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markAllAsRead();
            }
        });
        if ( !PrefUtils.getBoolean("show_mark_all_as_read_button", true) )
            mFab.hide();

        mLastVisibleTopEntryID = PrefUtils.getLong( STATE_LAST_VISIBLE_ENTRY_ID, -1 );
        mLastListViewTopOffset = PrefUtils.getInt( STATE_LAST_VISIBLE_OFFSET, 0 );
        UpdateActions();
        timer.End();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void onResume() {
        super.onResume();
        PrefUtils.putBoolean( PREF_ARTICLE_TAP_ENABLED_TEMP, true );
        mImageDownloadObservable.addObserver(this);
        mIsResumed = true;
    }

    @Override
    public void onPause() {
        mIsResumed = false;
        mImageDownloadObservable.deleteObserver( this);
        super.onPause();
    }

    private Uri GetUri(int pos) {
        final long id = mEntriesCursorAdapter.getItemId(pos);
        return mEntriesCursorAdapter.EntryUri(id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timer timer = new Timer( "EntriesListFragment.onCreateView" );

        getBaseActivity().mRootView = inflater.inflate(R.layout.fragment_entry_list, container, true);
        View rootView = getBaseActivity().mRootView;
        mTextViewFilterLabels = rootView.findViewById(R.id.filter_by_labels);
        mTextViewFilterLabels.setBackgroundColor(Theme.GetColorInt(Theme.TEXT_COLOR_BACKGROUND, R.string.default_text_color_background ) );
        mTextViewFilterLabels.setOnClickListener(view -> FilterByLabels());
        mStatusText  = new StatusText( (TextView)rootView.findViewById( R.id.statusText ),
                                   (TextView)rootView.findViewById( R.id.errorText ),
                                   (ProgressBar) rootView.findViewById( R.id.progressBarLoader),
                                   (TextView)rootView.findViewById( R.id.progressText ),
                                   Status());

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        AppCompatActivity activity = ( ( AppCompatActivity )getActivity() );
        //toolbar.setBackgroundColor( Theme.GetColorInt("toolBarColor",  ) );
        activity.setSupportActionBar( toolbar );
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //activity.getSupportActionBar().setBackgroundDrawable( new ColorDrawable( Theme.GetColorInt("toolBarColor", R.color.light_theme_color_primary ) ) );
//        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
//        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);

        getBaseActivity().mProgressBarRefresh = rootView.findViewById(R.id.progressBarRefresh);
        getBaseActivity().mProgressBarRefresh.setBackgroundColor(Theme.GetToolBarColorInt() );
        mListView = rootView.findViewById(android.R.id.list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mListView.setNestedScrollingEnabled(true);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if ( scrollState != SCROLL_STATE_IDLE )
                    return;
                UpdateTopTapZoneVisibility();
                UpdateHeader();
                //Dog.v( String.format( "EntriesListFragment.onScrollStateChanged(%d) last=%d count=%d", scrollState, mListView.getLastVisiblePosition(), mListView.getCount() ) );
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ( mEntriesCursorAdapter == null || !mIsResumed )
                    return;
                for ( int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++ ) {
                    String item = new VisibleReadItem( GetUri( i ).toString(), false ).ToString();
                    mWasVisibleList.add(item);
                }
                if ( mShowTextInEntryList || isAlsoSetAsRead() ) {
                    final int pos = firstVisibleItem - 2;
                    final Uri uri = GetUri( pos );
                    
                    synchronized( EntriesCursorAdapter.class ) {
                        if (mMarkAsReadList.add(uri.toString()) && !getItemIsRead(pos)) {
                            newNumber(mCurrentUri.getPathSegments().get(1), DrawerAdapter.NewNumberOperType.Update, true);
                            Dog.v( String.format( "EntriesListFragment.onScroll(%d, %d) id = %s", firstVisibleItem, visibleItemCount, uri.toString() ) );
                        }
                    }
                }
                if ( firstVisibleItem > 0 ) {
                    mLastVisibleTopEntryID = mEntriesCursorAdapter.getItemId(firstVisibleItem);
                    View v = mListView.getChildAt(0);
                    mLastListViewTopOffset = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
                }
                {
                    ArrayList<Long> list = new ArrayList<>();
                    for ( int pos = firstVisibleItem; pos < firstVisibleItem + visibleItemCount; pos++ )
                        list.add( mEntriesCursorAdapter.getItemId( pos ) );
                    FetcherService.setEntryIDActiveList( list );
                }
                Status().HideByScroll();

                int max = mEntriesCursorAdapter == null ? 0 : mEntriesCursorAdapter.getCount();
                int value = mListView.getFirstVisiblePosition();
                getBaseActivity().UpdateHeaderProgressOnly(max, value, getHeaderStep());
                //Dog.v( String.format( "EntriesListFragment.onScroll(%d, %d)", firstVisibleItem, visibleItemCount ) );
            }
        });

        if (mEntriesCursorAdapter != null) {
            SetListViewAdapter();
        }

        if ( false /*PrefUtils.getBoolean(PrefUtils.DISPLAY_TIP, true)*/ && mListView instanceof ListView ) {
            final TextView header = CreateTextView(mListView.getContext());
            header.setMinimumHeight(UiUtils.dpToPixel(70));
            int footerPadding = UiUtils.dpToPixel(10);
            header.setPadding(footerPadding, footerPadding, footerPadding, footerPadding);
            header.setText(R.string.tip_sentence);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setCompoundDrawablePadding(UiUtils.dpToPixel(5));
            header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_about, 0, R.drawable.ic_action_cancel_gray, 0);
            header.setClickable(true);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListView.removeHeaderView(header);
                    PrefUtils.putBoolean(PrefUtils.DISPLAY_TIP, false);
                }
            });
            mListView.addHeaderView(header);
        }

        if ( mListView instanceof ListView )
            UiUtils.addEmptyFooterView(mListView, 90);

        TextView emptyView = CreateTextView( getContext() );
        emptyView.setText( getString( R.string.no_entries ) );
        mListView.setEmptyView( emptyView );

        UpdateHeader();
        timer.End();
        return rootView;
    }

    private void UpdateTopTapZoneVisibility() {
        HomeActivity activity = (HomeActivity) getActivity();
        if ( GetActivity().mPageUpBtn != null )
            GetActivity().mPageUpBtn.setVisibility( mListView.getFirstVisiblePosition() == 0 || activity.GetIsActionBarEntryListHidden() ? View.GONE : View.VISIBLE );
        if ( GetActivity().mPageUpBtnFS != null )
            GetActivity().mPageUpBtnFS.setVisibility( mListView.getFirstVisiblePosition() == 0 || !activity.GetIsActionBarEntryListHidden() ? View.GONE : View.VISIBLE );
    }


    private HomeActivity GetActivity() {
        return (HomeActivity)getActivity();
    }


    public void UpdateHeader() {
        int max = mEntriesCursorAdapter == null ? 0 : mEntriesCursorAdapter.getCount();
        if ( mListView == null )
            return;
        int value = mListView.getFirstVisiblePosition();
        if ( getActivity() == null )
            return;
        getBaseActivity().UpdateHeader(max,
                                       value,
                                       getHeaderStep(),
                                       HomeActivity.GetIsStatusBarEntryListHidden(),
                                       HomeActivity.GetIsActionBarEntryListHidden());
    }

    private int getHeaderStep() {
        return 1;
    }

    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    private void SetListViewAdapter() {
        mListView.setAdapter(mEntriesCursorAdapter);
        mNeedSetSelection = true;
    }

    public static void ShowDeleteDialog(Context context, final String title, final long id, final String entryLink) {
        AlertDialog dialog = new AlertDialog.Builder(context) //
                .setIcon(android.R.drawable.ic_dialog_alert) //
                .setTitle( R.string.question_delete_entry ) //
                .setMessage( title ) //
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> new Thread() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.delete(EntryColumns.CONTENT_URI(id), null, null);
                        EntryUrlVoc.INSTANCE.remove( entryLink );
                    }
                }.start()).setNegativeButton(android.R.string.no, null).create();
        dialog.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }


    @Override
    public void onStop() {
        PrefUtils.unregisterOnPrefChangeListener(mPrefListener);

        PrefUtils.putBoolean(STATE_SHOW_UNREAD_ONLY, mShowUnReadOnly);
        PrefUtils.putLong( STATE_LAST_VISIBLE_ENTRY_ID, mLastVisibleTopEntryID );
        PrefUtils.putInt( STATE_LAST_VISIBLE_OFFSET, mLastListViewTopOffset );


        if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
            mJustMarkedAsReadEntries.close();
        }

        ApplyOldAndReadArticleList();

        mFab = null;

        super.onStop();
    }

    private void ApplyOldAndReadArticleList() {
        new Thread() {
            HashSet<String> mVisibleList = null;
            ArrayList<String> mMarkAsReadList = null;
            Thread init( HashSet<String> wasVisibleList, ArrayList<String> markAsReadList ) {
                mVisibleList = (HashSet<String>) wasVisibleList.clone();
                mMarkAsReadList = markAsReadList;
                return this;
            }
            @Override public void run() {
                EntriesListFragment.SetVisibleItemsAsOld( mVisibleList );
                EntriesListFragment.SetItemsAsRead( mMarkAsReadList );
            }
        }.init( mWasVisibleList, TakeMarkAsReadList( true ) ).start();
        mWasVisibleList.clear();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
        outState.putParcelable(STATE_ORIGINAL_URI, mOriginalUri);
        outState.putBoolean(STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST, mOriginalUriShownEntryText);
        outState.putBoolean(STATE_SHOW_FEED_INFO, mShowFeedInfo);
        outState.putBoolean(STATE_SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryList);
        //outState.putLong(STATE_LIST_DISPLAY_DATE, mListDisplayDate);
        outState.putBoolean(STATE_SHOW_UNREAD_ONLY, mShowUnReadOnly);
        if ( mOptions != null )
            outState.putString( STATE_OPTIONS, mOptions.toString() );
        super.onSaveInstanceState(outState);
    }

    /*@Override
    public void onRefresh() {
        startRefresh();
    }*/

    /*@Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (id >= 0) { // should not happen, but I had a crash with this on PlayStore...
            startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mCurrentUri, id)));
        }
    }*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        menu.clear(); // This is needed to remove a bug on Android 4.0.3

        inflater.inflate(R.menu.entry_list, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        // Use a custom search icon for the SearchView in AppBar
        int searchImgId = androidx.appcompat.R.id.search_button;
        ImageView v = searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search);

        if (EntryColumns.isSearchUri(mCurrentUri)) {
            searchItem.expandActionView();
            // Without that, it just does not work
            searchView.post(() -> {
                searchView.setQuery(mCurrentUri.getLastPathSegment(), false);
                searchView.clearFocus();
            });
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    setData(EntryColumns.SEARCH_URI(newText), true, true, false, mOptions);
                }
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            setData(mOriginalUri, true, false, mOriginalUriShownEntryText, mOptions);
            getActivity().invalidateOptionsMenu();
            return false;
        });

        UpdateActions();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        menu.findItem( R.id.menu_show_article_url_toggle).setChecked(PrefUtils.getBoolean( SHOW_ARTICLE_URL, false ));
        menu.findItem( R.id.menu_show_article_category_toggle).setChecked(PrefUtils.getBoolean( PrefUtils.SHOW_ARTICLE_CATEGORY, true ));
        menu.findItem( R.id.menu_show_article_text_preview_toggle).setChecked( PrefUtils.getBoolean( PrefUtils.SHOW_ARTICLE_TEXT_PREVIEW, true ));
        menu.findItem( R.id.menu_show_article_big_image_toggle ).setChecked( PrefUtils.getBoolean( PrefUtils.SHOW_ARTICLE_BIG_IMAGE, false ));
        menu.findItem( R.id.menu_show_progress_info).setChecked(PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ));
        menu.findItem( R.id.menu_show_entry_text ).setVisible( IsFeedUri( mCurrentUri ) );
        menu.findItem( R.id.menu_copy_feed ).setVisible( IsFeedUri( mCurrentUri ) );
        menu.findItem( R.id.menu_edit_feed ).setVisible( IsFeedUri( mCurrentUri ) );
        menu.findItem( R.id.menu_full_screen ).setChecked(HomeActivity.GetIsStatusBarEntryListHidden() );
        menu.findItem( R.id.menu_actionbar_visible ).setChecked(!HomeActivity.GetIsActionBarEntryListHidden() );
        menu.findItem( R.id.menu_show_article_text_preview_toggle ).setEnabled( !mShowTextInEntryList );
        menu.findItem( R.id.menu_show_article_big_image_toggle ).setEnabled( !mShowTextInEntryList && PrefUtils.IsShowArticleBigImagesEnabled( mCurrentUri ) );
    }
    @SuppressLint("Range")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_article_web_search: {
                startActivity(new Intent( Intent.ACTION_WEB_SEARCH )
                                  .setPackage( getContext().getPackageName() )
                                  .setClass(getContext(), ArticleWebSearchActivity.class));
                break;
            }
            case R.id.menu_add_feed: {
                startActivity( new Intent( Intent.ACTION_INSERT ).setData(FeedColumns.CONTENT_URI ) );
                break;
            }

            case R.id.menu_share_starred: {
                if (mEntriesCursorAdapter != null) {
                    StringBuilder starredList = new StringBuilder();
                    final boolean temp = mShowUnReadOnly;
                    mShowUnReadOnly = false;
                    final HashSet<Long> tempLabelsID = mLabelsID;
                    final HashSet<Long> labeledArticlesID = new HashSet<>();
                    for ( Label label: LabelVoc.INSTANCE.getList() ) {
                        starredList.append( "\n" + label.mName + ": \n" );
                        starredList.append( "----------------------------------------\n" );
                        SetSingleLabel( label.mID );
                        AddAtricleLinks(starredList, labeledArticlesID);
                    }
                    starredList.append( "\n" + getContext().getString( R.string.withoutLabel ) + ": \n" );
                    starredList.append( "----------------------------------------\n" );
                    try ( Cursor cursor = getContext().getContentResolver().query(mCurrentUri, new String[]{EntryColumns._ID, EntryColumns.TITLE, EntryColumns.LINK}, null, null,
                                                                                  EntryColumns.DATE + (IsOldestFirst() ? DB_ASC : DB_DESC)) ){
                        AddAtricleLinks(cursor, starredList, null, labeledArticlesID );
                    }

                    mShowUnReadOnly = temp;
                    mLabelsID = tempLabelsID;
                    final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(Intent.EXTRA_STREAM, DebugApp.CreateFileUri(getContext().getCacheDir().getAbsolutePath(), "shared_article_links.txt", starredList.toString()) );
                    startActivity(Intent.createChooser( emailIntent, getString(R.string.share_favorites_title)) );
                }
                return true;
            }

            case R.id.menu_refresh: {
                startRefresh();
                return true;
            }

            case R.id.menu_start_auto_refersh: {
                FetcherService.StartService(FetcherService.GetIntent(Constants.FROM_AUTO_REFRESH), true);
                return true;
            }

            case R.id.menu_cancel_refresh: {
                FetcherService.cancelRefresh();
                return true;
            }

            case R.id.menu_unstarr_articles: {
                new AlertDialog.Builder(getContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle( R.string.question )
                    .setMessage( R.string.unstarAllArtcilesComfirm)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> new AlertDialog.Builder(getContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.question)
                        .setMessage(R.string.unstarAllArtcilesComfirm2)
                        .setPositiveButton(android.R.string.yes, (dialog1, which1) -> new Thread() {
                            @Override
                            public void run() {
                                FetcherService.unstarAllFeedEntries(mCurrentUri);
                            }
                        }.start()).setNegativeButton(android.R.string.no, null).show()).setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_reset_feed_and_delete_all_articles: {
                new AlertDialog.Builder(getContext()) //
                    .setIcon(android.R.drawable.ic_dialog_alert) //
                    .setTitle( R.string.question ) //
                    .setMessage( R.string.deleteAllEntries ) //
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> new Thread() {
                        @Override
                        public void run() {
                            FetcherService.deleteAllFeedEntries(mCurrentUri, WHERE_NOT_FAVORITE + DB_AND +  GetWhereSQL());
                            // reset feed
                            if ( FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == URI_ENTRIES_FOR_FEED ) {
                                final String feedID = mCurrentUri.getPathSegments().get( 1 );
                                ContentValues values = new ContentValues();
                                values.putNull(FeedColumns.LAST_UPDATE);
                                values.putNull(FeedColumns.ICON_URL);
                                values.putNull(FeedColumns.REAL_LAST_UPDATE);
                                final ContentResolver cr = getContext().getContentResolver();
                                cr.update(FeedColumns.CONTENT_URI(feedID), values, null, null);
                            }
                            UiUtils.RunOnGuiThread(() -> mEntriesCursorAdapter.notifyDataSetChanged());
                        }
                    }.start()).setNegativeButton(android.R.string.no, null).show();
                return true;
            }
            case R.id.menu_mark_all_as_read: {
                markAllAsRead();
                return true;
            }
            case R.id.menu_mark_all_as_unread: {
                ShowMarkAllAsUnReadDialog( getContext() );
                return true;
            }
            case R.id.menu_delete_old: {
                FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_DELETE_OLD ), false );
                return true;
            }
            case R.id.menu_reload_all_texts: {
                FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_RELOAD_ALL_TEXT )
                                                 .setData( mCurrentUri )
                                                 .putExtra( WHERE_SQL_EXTRA, GetWhereSQL() ), false );
                return true;
            }
            case R.id.menu_show_article_url_toggle: {
                PrefUtils.toggleBoolean( SHOW_ARTICLE_URL, false );
                item.setChecked( PrefUtils.getBoolean( SHOW_ARTICLE_URL, false ) );
                mEntriesCursorAdapter.notifyDataSetChanged();
                return true;
            }
            case R.id.menu_show_article_text_preview_toggle: {
                PrefUtils.toggleBoolean( SHOW_ARTICLE_TEXT_PREVIEW, false );
                item.setChecked( PrefUtils.getBoolean( SHOW_ARTICLE_TEXT_PREVIEW, false ) );
                mEntriesCursorAdapter.notifyDataSetChanged();
                return true;
            }
            case R.id.menu_show_article_big_image_toggle: {
                PrefUtils.toggleBoolean( SHOW_ARTICLE_BIG_IMAGE, false );
                item.setChecked( PrefUtils.getBoolean( SHOW_ARTICLE_BIG_IMAGE, false ) );
                mEntriesCursorAdapter.notifyDataSetChanged();
                return true;
            }
            case R.id.menu_show_article_category_toggle: {
                PrefUtils.toggleBoolean( SHOW_ARTICLE_CATEGORY, true );
                item.setChecked( PrefUtils.getBoolean( SHOW_ARTICLE_CATEGORY, true ) );
                mEntriesCursorAdapter.notifyDataSetChanged();
                return true;
            }
            case R.id.menu_show_progress_info: {
                PrefUtils.toggleBoolean( SHOW_PROGRESS_INFO, false ) ;
                item.setChecked( PrefUtils.getBoolean( SHOW_PROGRESS_INFO, false ) );
                break;
            }
            case R.id.menu_show_entry_text: {
                if ( IsFeedUri( mCurrentUri) ) {
                    mShowTextInEntryList = !mShowTextInEntryList;
                    item.setChecked( mShowTextInEntryList );
                    final String feedID = mCurrentUri.getPathSegments().get(1);
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryList);
                    cr.update(FeedColumns.CONTENT_URI(feedID), values, null, null);
                    setData( mCurrentUri, mShowFeedInfo, false, mShowTextInEntryList, mOptions );
                }
                return true;
            }
            case R.id.menu_create_backup: {
                OPML.OnMenuExportImportClick( getActivity(), OPML.ExportImport.Backup );
                return true;
            }
            case R.id.menu_create_auto_backup: {
                FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_AUTO_BACKUP ), false );
                return true;
            }
            case R.id.menu_add_feed_shortcut: {
                if ( ShortcutManagerCompat.isRequestPinShortcutSupported(getContext()) ) {
                    //Adding shortcut for MainActivity on Home screen

                    String name = "";
                    IconCompat image = null;
                    String feedUrl = "";
                    String iconUrl = "";
                    if ( IsAllLabels() ) {
                        name = getContext().getString( R.string.labels_group_title );
                        image = IconCompat.createWithResource(getContext(), R.drawable.label_brown_small);
                    } else if ( mIsSingleLabel ) {
                        name = LabelVoc.INSTANCE.get(GetSingleLabelID()).mName;
                        image = IconCompat.createWithResource(getContext(), R.drawable.label_brown_small);
                    } else if ( EntryColumns.CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString(R.string.all_entries);
                        image = IconCompat.createWithResource( getContext(), R.drawable.cup_new_pot);
                    } else if ( EntryColumns.FAVORITES_CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString(R.string.favorites);
                        image = IconCompat.createWithResource( getContext(), R.drawable.cup_new_star );
                    } else if ( EntryColumns.LAST_READ_CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString(R.string.last_read);
                        image = IconCompat.createWithResource( getContext(), R.drawable.cup_new_load_now );
                    } else if ( EntryColumns.UNREAD_ENTRIES_CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString( R.string.unread_entries );
                        image = IconCompat.createWithResource(getContext(), R.drawable.cup_new_unread);
                    } else if ( EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() ).equals(mCurrentUri) ) {
                        name = getContext().getString( R.string.externalLinks );
                        image = IconCompat.createWithResource(getContext(), R.drawable.cup_new_load_later);
                    } else {
                        long feedID = Long.parseLong( mCurrentUri.getPathSegments().get(1) );
                        Cursor cursor = getContext().getContentResolver().query(FeedColumns.CONTENT_URI(feedID),
                                                                                new String[]{FeedColumns.NAME, FeedColumns.ICON_URL },
                                                                                null, null, null);
                        if (cursor.moveToFirst()) {
                            name = cursor.getString(0);
                            if (!cursor.isNull(2) )
                                feedUrl = cursor.getString( 2 );
                            if (!cursor.isNull(1) ) {
                                iconUrl = cursor.getString(1);
//                                final Bitmap bitmap = UiUtils.getFaviconBitmap(feedID, feedUrl, cursor.getString(1) );
//                                if ( bitmap != null )
//                                    image = IconCompat.createWithBitmap(bitmap);
                            }
                        }
                        cursor.close();
                    }

                    final Intent intent =
                        new Intent(getContext(), HomeActivityNewTask.class)
                            .setFlags( Intent.FLAG_ACTIVITY_CLEAR_TASK )
                            .setAction(Intent.ACTION_MAIN).setData( mCurrentUri )
                            .putExtra( NEW_TASK_EXTRA, true );

                    if ( !feedUrl.isEmpty() )
                        intent.putExtra( Constants.EXTRA_LINK, feedUrl );
                    if ( mIsSingleLabel )
                        intent.putExtra(LABEL_ID_EXTRA, GetSingleLabelID());
                    final String finalIconUrl = iconUrl;
                    final String finalName = name;
                    final IconCompat finalImage = image;
                    new WaitDialog(getActivity(), R.string.downloadImage, () -> {
                        final IconCompat icon = (finalImage == null) ? LoadIcon(finalIconUrl) : finalImage;
                        getActivity().runOnUiThread(() -> {

                            ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(getContext(), mCurrentUri.toString() + intent.getLongExtra(LABEL_ID_EXTRA, 0))
                                .setIcon(icon)
                                .setShortLabel(finalName)
                                .setIntent(intent)
                                .setLongLived()
                                .build();
                            ShortcutManagerCompat.requestPinShortcut( getContext(), pinShortcutInfo, null);
                            if(Build.VERSION.SDK_INT< Build.VERSION_CODES.O)
                                Toast.makeText(

                            getContext(),R.string.new_feed_shortcut_added,Toast.LENGTH_LONG).show();
                        });
                    }).execute();
                } else
                    Toast.makeText( getContext(), R.string.new_feed_shortcut_add_failed, Toast.LENGTH_LONG ).show();
                return true;
            }
            case R.id.menu_toogle_toogle_unread_all: {
                //int uriMatch = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
                mShowUnReadOnly = !mShowUnReadOnly;
                setData( mCurrentUri, mShowFeedInfo, false, mShowTextInEntryList, mOptions );
                UpdateActions();
                return true;
            }
            case R.id.menu_full_screen: {
                HomeActivity activity1 = (HomeActivity) getActivity();
                activity1.setFullScreen(!HomeActivity.GetIsStatusBarEntryListHidden(), HomeActivity.GetIsActionBarEntryListHidden() );
                item.setChecked( HomeActivity.GetIsStatusBarEntryListHidden() );
                break;
            }
            case R.id.menu_actionbar_visible: {
                HomeActivity activity1 = (HomeActivity) getActivity();
                activity1.setFullScreen( HomeActivity.GetIsStatusBarEntryListHidden(), !HomeActivity.GetIsActionBarEntryListHidden() );
                item.setChecked( !HomeActivity.GetIsActionBarEntryListHidden() );
                break;
            }
            case R.id.menu_filter_by_labels: {
                FilterByLabels();
                break;
            }
            case R.id.menu_edit_feed: {
                if ( IsFeedUri( mCurrentUri) ) {
                    final String feedID = mCurrentUri.getPathSegments().get(1);
                    startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedColumns.CONTENT_URI(feedID)));
                }
                break;
            }
            case R.id.menu_copy_feed: {
                if ( IsFeedUri( mCurrentUri) ) {
                    final String feedID = mCurrentUri.getPathSegments().get(1);
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    Cursor cursor = cr.query( FeedColumns.CONTENT_URI( feedID ), null, null, null, null );
                    if ( cursor != null ) {
                        if ( cursor.moveToFirst() ) {
                            ContentValues values = new ContentValues();
                            values.put(FeedColumns.GROUP_ID, cursor.getLong( cursor.getColumnIndex( FeedColumns.GROUP_ID )) );
                            values.put(FeedColumns.ICON_URL, cursor.getString( cursor.getColumnIndex( FeedColumns.ICON_URL )) );
                            values.put(FeedColumns.IS_AUTO_REFRESH, cursor.getLong( cursor.getColumnIndex( FeedColumns.IS_AUTO_REFRESH )) );
                            values.put(FeedColumns.IS_IMAGE_AUTO_LOAD, cursor.getLong( cursor.getColumnIndex( FeedColumns.IS_IMAGE_AUTO_LOAD )) );
                            values.put(FeedColumns.NAME, cursor.getString( cursor.getColumnIndex( FeedColumns.NAME )) );
                            values.put(FeedColumns.OPTIONS, cursor.getString( cursor.getColumnIndex( FeedColumns.OPTIONS )) );
                            values.put(FeedColumns.RETRIEVE_FULLTEXT, cursor.getLong( cursor.getColumnIndex( FeedColumns.RETRIEVE_FULLTEXT )) );
                            values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, cursor.getLong( cursor.getColumnIndex( FeedColumns.SHOW_TEXT_IN_ENTRY_LIST )) );
                            values.put(FeedColumns.URL, cursor.getString( cursor.getColumnIndex( FeedColumns.URL )) + "/" );
                            cr.insert(FeedColumns.CONTENT_URI, values);
                        }
                        cursor.close();
                        UiUtils.toast( getActivity(), R.string.feed_copied );
                    }
                }
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void AddAtricleLinks(StringBuilder starredList, HashSet<Long> articlesID) {
        try( Cursor cursor = getContext().getContentResolver().query(mCurrentUri, new String[]{EntryColumns._ID, EntryColumns.TITLE, EntryColumns.LINK}, GetWhereSQL(), null,
                                                                EntryColumns.DATE + (IsOldestFirst() ? DB_ASC : DB_DESC)) ) {
            AddAtricleLinks(cursor, starredList, articlesID, null );
        }
    }
    private void AddAtricleLinks(Cursor cursor, StringBuilder starredList, HashSet<Long> articlesID, HashSet<Long> articlesToIgnoreID) {
        int titlePos = cursor.getColumnIndex(EntryColumns.TITLE);
        int linkPos = cursor.getColumnIndex(EntryColumns.LINK);
        int idPos = cursor.getColumnIndex(EntryColumns._ID);
        while (cursor.moveToNext()) {
            starredList.append(cursor.getString(titlePos)).append("\n").append(cursor.getString(linkPos)).append("\n\n");
            final long id = cursor.getLong(idPos);
            if (articlesID != null && (articlesToIgnoreID == null || articlesToIgnoreID.contains( id )) )
                articlesID.add( id );
        }
    }

    public void FilterByLabels() {
        LabelVoc.INSTANCE.showDialog(getContext(), R.string.filter_by_labels, true, mLabelsID, mEntriesCursorAdapter, (checkedLabels) -> {
            mLabelsID = checkedLabels;
            ArrayList<String> list = new ArrayList<>();
            for ( Long item: mLabelsID )
                list.add( String.valueOf( item ) );
            PrefUtils.putString( STATE_LABEL_FILTER_LIST + mCurrentUri, TextUtils.join( ",", list ) );
            UiUtils.RunOnGuiThread(() -> {
                restartLoaders();
                UpdateActions();
            } );
            return null;
        } );
    }


    @SuppressLint("PrivateResource")
    private void markAllAsRead() {
        if (mEntriesCursorAdapter != null) {
            Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.light_theme_color_primary))
                    .setAction(R.string.undo, v -> new Thread() {
                        @Override
                        public void run() {
                            if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                                ArrayList<Integer> ids = new ArrayList<>();
                                while (mJustMarkedAsReadEntries.moveToNext()) {
                                    ids.add(mJustMarkedAsReadEntries.getInt(0));
                                }
                                ContentResolver cr = MainApplication.getContext().getContentResolver();
                                String where = BaseColumns._ID + " IN (" + TextUtils.join(",", ids) + ')';
                                cr.update(EntryColumns.CONTENT_URI, FeedData.getUnreadContentValues(), where, null);

                                mJustMarkedAsReadEntries.close();
                            }
                        }
                    }.start());
            snackbar.getView().setBackgroundResource(R.color.material_grey_900);
            snackbar.show();

            new Thread() {
                @Override
                public void run() {
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    //String where = EntryColumns.WHERE_UNREAD + Constants.DB_AND + '(' + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
                    String where = EntryColumns.WHERE_UNREAD;
                    if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                        mJustMarkedAsReadEntries.close();
                    }
                    mJustMarkedAsReadEntries = cr.query(mCurrentUri, new String[]{BaseColumns._ID}, where, null, null);
                    if ( mCurrentUri != null && Constants.NOTIF_MGR != null  ) {
                        Constants.NOTIF_MGR.cancel( Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT );
                        Constants.NOTIF_MGR.cancel( Constants.NOTIFICATION_ID_MANY_ITEMS_MARKED_STARRED );
                        if ( mJustMarkedAsReadEntries.moveToFirst() )
                            do {
                                Constants.NOTIF_MGR.cancel( mJustMarkedAsReadEntries.getInt(0) );
                            } while ( mJustMarkedAsReadEntries.moveToNext());
                        mJustMarkedAsReadEntries.moveToFirst();
                    }

                    cr.update(mCurrentUri, FeedData.getReadContentValues(), where, null);
                    TakeMarkAsReadList( true );
                }
            }.start();


        }
    }

    private void startRefresh() {
        if ( mCurrentUri != null && !PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            int uriMatcher = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
            if ( uriMatcher == FeedDataContentProvider.URI_ENTRIES_FOR_FEED  ) {
                FetcherService.StartService( new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(Constants.FEED_ID, mCurrentUri.getPathSegments().get(1)), true);
            } else if ( FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == FeedDataContentProvider.URI_ENTRIES_FOR_GROUP ) {
                FetcherService.StartService( new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(Constants.GROUP_ID, mCurrentUri.getPathSegments().get(1)), true);
            } else {
                FetcherService.StartService( new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS), true);
            }
        }

    }

    public Uri getUri() {
        return mOriginalUri;
    }

    public void setData(Uri uri, boolean showFeedInfo, boolean isSearchUri, boolean showTextInEntryList, JSONObject options) {
        mIsSearch = isSearchUri;
        mOptions = options;
        if ( getActivity() == null ) // during configuration changes
            return;
        Timer timer = new Timer( "EntriesListFragment.setData" );

        Dog.v( String.format( "EntriesListFragment.setData( %s )", uri.toString() ) );
        mCurrentUri = uri;
        if ( isSearchUri )
            mSearchQueryUri = uri;
        else  {
            mSearchQueryUri = null;
            mOriginalUri = mCurrentUri;
            mOriginalUriShownEntryText = showTextInEntryList;
        }

        String labelFilterState = PrefUtils.getString( STATE_LABEL_FILTER_LIST + mCurrentUri, "" );
        if ( !labelFilterState.isEmpty() ) {
            mLabelsID.clear();
            for (String item : TextUtils.split(labelFilterState, "," ))
                mLabelsID.add(Long.parseLong(item));
        }
        mShowFeedInfo = showFeedInfo;
        mShowTextInEntryList = showTextInEntryList;

        ApplyOldAndReadArticleList();

        mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mShowTextInEntryList, mShowUnReadOnly, GetActivity());
        SetListViewAdapter();
        //if ( mListView instanceof ListView )
            mListView.setDividerHeight( mShowTextInEntryList ? 10 : 0 );
        //mListDisplayDate = new Date().getTime();
        if (mCurrentUri != null)
            restartLoaders();

        //refreshUI();
        UpdateHeader();
        UpdateActions();
        mStatusText.SetFeedID( mCurrentUri );
        timer.End();
    }

    private boolean isAlsoSetAsRead() {
        try {
            return mOptions.has( AUTO_SET_AS_READ ) && mOptions.getBoolean( AUTO_SET_AS_READ );
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean IsFeedUri( Uri uri ) {
        boolean result = false;
        if ( uri != null && uri.getPathSegments().size() > 1 )
            try {
                long feedID = Long.parseLong(uri.getPathSegments().get(1));
                result = feedID != Long.parseLong( FetcherService.GetExtrenalLinkFeedID() );
            } catch ( NumberFormatException ignored ) { }
        return result;
    }

    private void restartLoaders() {

        LoaderManager loaderManager = LoaderManager.getInstance( this );

        //HACK: 2 times to workaround a hard-to-reproduce bug with non-refreshing loaders...
        Timer.Start( ENTRIES_LOADER_ID, "EntriesListFr.restartLoaders() mEntriesLoader" );
        loaderManager.restartLoader(ENTRIES_LOADER_ID, null, mLoader);
        if ( IsFeedUri( mCurrentUri ) )
            loaderManager.restartLoader(FILTERS_LOADER_ID, null, mLoader);
        //Timer.Start( NEW_ENTRIES_NUMBER_LOADER_ID, "EntriesListFr.restartLoaders() mEntriesNumberLoader" );
        //loaderManager.restartLoader(NEW_ENTRIES_NUMBER_LOADER_ID, null, mEntriesNumberLoader);

        //loaderManager.restartLoader(ENTRIES_LOADER_ID, null, mEntriesLoader);
        //loaderManager.restartLoader(NEW_ENTRIES_NUMBER_LOADER_ID, null, mEntriesNumberLoader);
    }

    /*private void refreshUI() {
        if (mNewEntriesNumber > 0) {
            mRefreshListBtn.setText(getResources().getQuantityString(R.plurals.number_of_new_entries, mNewEntriesNumber, mNewEntriesNumber));
            mRefreshListBtn.setVisibility(View.VISIBLE);
        } else {
            mRefreshListBtn.setVisibility(View.GONE);
        }
    }*/
    public static void SetVisibleItemsAsOld(HashSet<String> uriList) {
        final ArrayList<ContentProviderOperation> updates = new ArrayList<>();
        for (String data : uriList) {
            VisibleReadItem item = new VisibleReadItem( data );
            updates.add(
                ContentProviderOperation.newUpdate(Uri.parse(item.mUri))
                    .withValues(FeedData.getOldContentValues())
                    .withSelection(EntryColumns.WHERE_NEW, null)
                    .build());
            if ( item.mIsRead )
                updates.add(
                    ContentProviderOperation.newUpdate(Uri.parse(item.mUri))
                        .withValues(FeedData.getReadContentValues())
                        .withSelection(EntryColumns.WHERE_UNREAD, null)
                        .build());
        }
        if (!updates.isEmpty()) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            try {
                cr.applyBatch(FeedData.AUTHORITY, updates);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void SetItemsAsRead(ArrayList<String> uriList) {
        final ArrayList<ContentProviderOperation> updates = new ArrayList<>();
        for (String data : uriList) {
            updates.add(
                ContentProviderOperation.newUpdate(Uri.parse(data))
                    .withValues(FeedData.getReadContentValues())
                    .withSelection(EntryColumns.WHERE_UNREAD, null)
                    .build());
        }
        if (!updates.isEmpty()) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            try {
                cr.applyBatch(FeedData.AUTHORITY, updates);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void update(Observable observable, Object o) {
        //if ( !mShowTextInEntryList )
        //    return;
        if ( o instanceof Entry && mEntriesCursorAdapter != null ) {
            final Entry entry = (Entry) o;
            if (entry.mRestorePosition ) {
                final int pos = mEntriesCursorAdapter.GetPosByID(entry.mID);
                if (pos >= mListView.getFirstVisiblePosition() && pos <= mListView.getLastVisiblePosition()) {
                    mEntriesCursorAdapter.mIgnoreClearContentVocOnCursorChange = true;
                    mEntriesCursorAdapter.notifyDataSetChanged();
                    RestoreListScrollPosition();
                }
            } else
                mEntriesCursorAdapter.notifyDataSetChanged();
        } else if ( o instanceof EntriesCursorAdapter.ListViewTopPos) {
            mListView.setSelectionFromTop( ((EntriesCursorAdapter.ListViewTopPos)o).mPos, 0 );
        }
    }

    public void ClearSingleLabel() {
        mIsSingleLabel = false;
        mLabelsID.clear();
    }
    public void SetSingleLabel( Long id ) {
        mIsSingleLabel = id != NO_LABEL;
        mLabelsID.clear();
        if ( id != NO_LABEL )
            mLabelsID.add( id );
    }

    public boolean IsAllLabels() {
        return mIsSingleLabel && mLabelsID.contains( ALL_LABELS );
    }

    public Long GetSingleLabelID() {
        return mLabelsID.size() == 1 && mIsSingleLabel ? (Long) mLabelsID.toArray()[0] : NO_LABEL;
    }
    public void ShowMarkAllAsUnReadDialog(Context context) {
        ShowDialog(context, R.string.question_mark_all_as_unread, new Runnable() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(FeedData.EntryColumns.CONTENT_URI, FeedData.getUnreadContentValues(), null, null);
            }
        } );
    }


}
