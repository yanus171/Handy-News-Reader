
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.adapter.EntriesCursorAdapter;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.Entry;
import ru.yanus171.feedexfork.view.StatusText;

import static ru.yanus171.feedexfork.activity.EditFeedActivity.AUTO_SET_AS_READ;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_CATEGORY;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_URL;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_PROGRESS_INFO;
import static ru.yanus171.feedexfork.view.EntryView.mImageDownloadObservable;

public class EntriesListFragment extends /*SwipeRefreshList*/Fragment implements Observer {
    private static final String STATE_CURRENT_URI = "STATE_CURRENT_URI";
    private static final String STATE_ORIGINAL_URI = "STATE_ORIGINAL_URI";
    private static final String STATE_SHOW_FEED_INFO = "STATE_SHOW_FEED_INFO";
    //private static final String STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE";
    private static final String STATE_SHOW_TEXT_IN_ENTRY_LIST = "STATE_SHOW_TEXT_IN_ENTRY_LIST";
    private static final String STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST = "STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST";
    private static final String STATE_SHOW_UNREAD = "STATE_SHOW_UNREAD";
    private static final String STATE_LAST_VISIBLE_ENTRY_ID = "STATE_LAST_VISIBLE_ENTRY_ID";
    private static final String STATE_LAST_VISIBLE_OFFSET = "STATE_LAST_VISIBLE_OFFSET";


    private static final int ENTRIES_LOADER_ID = 1;
    //private static final int NEW_ENTRIES_NUMBER_LOADER_ID = 2;
    private static final int FILTERS_LOADER_ID = 3;
    private static final String STATE_OPTIONS = "STATE_OPTIONS";

    private Uri mCurrentUri, mOriginalUri;
    private boolean mOriginalUriShownEntryText = false;
    private boolean mShowFeedInfo = false;
    private boolean mShowTextInEntryList = false;
    private EntriesCursorAdapter mEntriesCursorAdapter;
    private Cursor mJustMarkedAsReadEntries;
    private FloatingActionButton mFab;
    public ListView mListView;
    private ProgressBar mProgressBarRefresh = null;
    private ProgressBar mProgressBar = null;
    private TextView mLabelClock = null;
    private TextView mLabelBattery = null;
    public boolean mShowUnRead = false;
    private boolean mNeedSetSelection = false;
    private long mLastVisibleTopEntryID = 0;
    private int mLastListViewTopOffset = 0;
    private Menu mMenu = null;
    private FeedFilters mFilters = null;

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
    private final ArrayList<String> mWasVisibleList = new ArrayList<>();
    private JSONObject mOptions;

    public boolean IsOldestFirst() { return mShowTextInEntryList || PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false); }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if ( id == ENTRIES_LOADER_ID ) {
                Timer.Start(ENTRIES_LOADER_ID, "EntriesListFr.onCreateLoader");

                String entriesOrder = IsOldestFirst() ? Constants.DB_ASC : Constants.DB_DESC;
                //String where = "(" + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
                String[] projection = EntryColumns.PROJECTION_WITH_TEXT;//   mShowTextInEntryList ? EntryColumns.PROJECTION_WITH_TEXT : EntryColumns.PROJECTION_WITHOUT_TEXT;
                CursorLoader cursorLoader = new CursorLoader(getActivity(), mCurrentUri, projection, null, null, EntryColumns.DATE + entriesOrder);
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

                mEntriesCursorAdapter.swapCursor(data);
                if (mNeedSetSelection) {
                    mNeedSetSelection = false;
                    mListView.setSelection(IsOldestFirst() ? mEntriesCursorAdapter.GetTopNewPos() : mEntriesCursorAdapter.GetBottomNewPos());
                }
                RestoreListScrollPosition();
                getActivity().setProgressBarIndeterminateVisibility(false);
                Status().End(mStatus);
                timer.End();
            } else if (loader.getId() == FILTERS_LOADER_ID) {
                mFilters = new FeedFilters(data);
                mEntriesCursorAdapter.setFilter(mFilters);
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            if ( loader.getId() == ENTRIES_LOADER_ID ) {
                Status().End( mStatus );
                //getActivity().setProgressBarIndeterminateVisibility( true );
                mEntriesCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
            }
        }

    };

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

        if (EntryColumns.FAVORITES_CONTENT_URI.equals(mCurrentUri)) {
            mMenu.findItem(R.id.menu_refresh).setVisible(false);
            mMenu.findItem(R.id.menu_share_starred).setVisible(true);
        }

        MenuItem item = mMenu.findItem( R.id.menu_toogle_toogle_unread_all );
        if (mShowUnRead) {
            item.setTitle(R.string.all_entries);
            item.setIcon(R.drawable.ic_check_box_outline_blank);
        } else {
            item.setTitle(R.string.unread_entries);
            item.setIcon(R.drawable.ic_check_box);
        }

        if ( mCurrentUri != null ) {
            int uriMatch = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
            item.setVisible(uriMatch != FeedDataContentProvider.URI_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_UNREAD_ENTRIES );
            mMenu.findItem(R.id.menu_show_entry_text).setVisible(uriMatch != FeedDataContentProvider.URI_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_UNREAD_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_FAVORITES &&
                    uriMatch != FeedDataContentProvider.URI_FAVORITES_UNREAD );
        }

        boolean isCanRefresh = !EntryColumns.FAVORITES_CONTENT_URI.equals( mCurrentUri );
        if ( mCurrentUri != null && mCurrentUri.getPathSegments().size() > 1 ) {
            String feedID = mCurrentUri.getPathSegments().get(1);
            isCanRefresh = !feedID.equals(FetcherService.GetExtrenalLinkFeedID());
        }
        boolean isRefresh = PrefUtils.getBoolean( PrefUtils.IS_REFRESHING, false );
        mMenu.findItem(R.id.menu_cancel_refresh).setVisible( isRefresh );
        mMenu.findItem(R.id.menu_refresh).setVisible( !isRefresh && isCanRefresh );


        if ( mProgressBarRefresh != null ) {
            if (isRefresh)
                mProgressBarRefresh.setVisibility(View.VISIBLE);
            else
                mProgressBarRefresh.setVisibility(View.GONE);
        }
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
            mShowUnRead = savedInstanceState.getBoolean(STATE_SHOW_UNREAD, PrefUtils.getBoolean( STATE_SHOW_UNREAD, false ));
            try {
                mOptions = savedInstanceState.containsKey( STATE_OPTIONS ) ? new JSONObject( savedInstanceState.getString( STATE_OPTIONS ) ) : new JSONObject();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Dog.v( String.format( "EntriesListFragment.onCreate mShowUnRead = %b", mShowUnRead ) );

            //if ( mShowTextInEntryList )
            //    mNeedSetSelection = true;
            mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mShowTextInEntryList, mShowUnRead);
        } else
            mShowUnRead = PrefUtils.getBoolean( STATE_SHOW_UNREAD, false );

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

        if (mCurrentUri != null) {
            // If the list is empty when we are going back here, try with the last display date
//            if (mNewEntriesNumber != 0 && mOldUnreadEntriesNumber == 0) {
//                mListDisplayDate = new Date().getTime();
//            } else {
//                mAutoRefreshDisplayDate = true; // We will try to update the list after if necessary
//            }
            restartLoaders();
        }
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
        mImageDownloadObservable.addObserver(this);
    }

    @Override
    public void onPause() {
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

        View rootView = inflater.inflate(R.layout.fragment_entry_list, container, true);

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

        mProgressBarRefresh = rootView.findViewById(R.id.progressBarRefresh);
        mListView = rootView.findViewById(android.R.id.list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mListView.setNestedScrollingEnabled(true);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ( mEntriesCursorAdapter == null )
                    return;
                if ( GetActivity().mPageUpBtn != null )
                    GetActivity().mPageUpBtn.setVisibility( firstVisibleItem == 0 ? View.GONE : View.VISIBLE );
                for ( int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++ ) {
                    String item = new VisibleReadItem( GetUri( i ).toString(), isAlsoSetAsRead() ).ToString();
                    synchronized ( mWasVisibleList ) {
                        if (!mWasVisibleList.contains(item))
                            mWasVisibleList.add(item);
                    }
                }
                SetIsRead( firstVisibleItem - 2);
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
                UpdateFooter();
                Status().HideByScroll();
                Dog.v( String.format( "EntriesListFragment.onScroll(%d, %d)", firstVisibleItem, visibleItemCount ) );
            }
        });

        if (mEntriesCursorAdapter != null) {
            SetListViewAdapter();
        }

        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_TIP, true) && mListView instanceof ListView ) {
            final TextView header = new TextView(mListView.getContext());
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

        TextView emptyView = new TextView( getContext() );
        emptyView.setText( getString( R.string.no_entries ) );
        mListView.setEmptyView( emptyView );

        mProgressBar = rootView.findViewById(R.id.progressBar);
        mProgressBar.setProgress( 0 );
        mLabelClock = rootView.findViewById(R.id.textClock);
        mLabelClock.setText("");
        mLabelBattery = rootView.findViewById(R.id.textBattery);
        mLabelBattery.setText("");

        timer.End();
        return rootView;
    }

    private HomeActivity GetActivity() {
        return (HomeActivity)getActivity();
    }


    public void UpdateFooter() {
        BaseActivity.UpdateFooter(mProgressBar,
                                  mEntriesCursorAdapter.getCount(),
                                  mListView.getFirstVisiblePosition(),
                                  mLabelClock,
                                  mLabelBattery,
                                  HomeActivity.GetIsStatusBarEntryListHidden() );
    }

    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    private void SetListViewAdapter() {


        mListView.setAdapter(mEntriesCursorAdapter);
        mNeedSetSelection = true;
    }




    private void SetIsRead(final int pos) {
        if ( !mShowTextInEntryList )
            return;
        final Uri uri = GetUri( pos );
        if ( EntriesCursorAdapter.mMarkAsReadList.contains( uri ) )
            return;
        class Run implements Runnable {
            private int mEntryPos;
            private Run(final int entryPos) {
                mEntryPos = entryPos;
            }
            @Override
            public void run() {
                if (mEntryPos < mListView.getFirstVisiblePosition() || mEntryPos > mListView.getLastVisiblePosition()) {
                    EntriesCursorAdapter.mMarkAsReadList.add(uri);
                    new Thread() {
                        @Override
                        public void run() {
                            FeedDataContentProvider.mNotifyEnabled = false;
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update( uri, FeedData.getReadContentValues(), EntryColumns.WHERE_UNREAD, null);
                            FeedDataContentProvider.mNotifyEnabled = true;
                        }
                    }.start();
                }
            }
        }
        UiUtils.RunOnGuiThread(  new Run( pos ), 2000);

    }

    public static void ShowDeleteDialog(Context context, final String title, final long id) {
        AlertDialog dialog = new AlertDialog.Builder(context) //
                .setIcon(android.R.drawable.ic_dialog_alert) //
                .setTitle( R.string.question_delete_entry ) //
                .setMessage( title ) //
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread() {
                            @Override
                            public void run() {
                                ContentResolver cr = MainApplication.getContext().getContentResolver();
                                cr.delete(EntryColumns.CONTENT_URI(id), null, null);
                            }
                        }.start();
                    }
                }).setNegativeButton(android.R.string.no, null).create();
        dialog.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }


    @Override
    public void onStop() {
        PrefUtils.unregisterOnPrefChangeListener(mPrefListener);

        PrefUtils.putBoolean( STATE_SHOW_UNREAD, mShowUnRead );
        PrefUtils.putLong( STATE_LAST_VISIBLE_ENTRY_ID, mLastVisibleTopEntryID );
        PrefUtils.putInt( STATE_LAST_VISIBLE_OFFSET, mLastListViewTopOffset );


        if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
            mJustMarkedAsReadEntries.close();
        }

        synchronized ( mWasVisibleList ) {
            FetcherService.StartService( FetcherService.GetIntent(Constants.SET_VISIBLE_ITEMS_AS_OLD)
                                         .putStringArrayListExtra(Constants.URL_LIST, mWasVisibleList) );
            mWasVisibleList.clear();
        }
        mFab = null;

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
        outState.putParcelable(STATE_ORIGINAL_URI, mOriginalUri);
        outState.putBoolean(STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST, mOriginalUriShownEntryText);
        outState.putBoolean(STATE_SHOW_FEED_INFO, mShowFeedInfo);
        outState.putBoolean(STATE_SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryList);
        //outState.putLong(STATE_LIST_DISPLAY_DATE, mListDisplayDate);
        outState.putBoolean( STATE_SHOW_UNREAD, mShowUnRead);
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
        ImageView v = (ImageView) searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search);

        if (EntryColumns.isSearchUri(mCurrentUri)) {
            searchItem.expandActionView();
            searchView.post(new Runnable() { // Without that, it just does not work
                @Override
                public void run() {
                    searchView.setQuery(mCurrentUri.getLastPathSegment(), false);
                    searchView.clearFocus();
                }
            });
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText))
                    setData(EntryColumns.SEARCH_URI(newText), true, true, false, mOptions);
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setData(mOriginalUri, true, false, mOriginalUriShownEntryText, mOptions);
                getActivity().invalidateOptionsMenu();
                return false;
            }
        });


        UpdateActions();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        menu.findItem( R.id.menu_show_article_url_toggle).setChecked(PrefUtils.getBoolean( SHOW_ARTICLE_URL, false ));
        menu.findItem( R.id.menu_show_article_category_toggle).setChecked(PrefUtils.getBoolean( PrefUtils.SHOW_ARTICLE_CATEGORY, true ));
        menu.findItem( R.id.menu_show_progress_info).setChecked(PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ));
        menu.findItem( R.id.menu_show_entry_text ).setVisible( IsFeedUri( mCurrentUri ) );
        menu.findItem( R.id.menu_copy_feed ).setVisible( IsFeedUri( mCurrentUri ) );
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_share_starred: {
                if (mEntriesCursorAdapter != null) {
                    StringBuilder starredList = new StringBuilder();
                    Cursor cursor = mEntriesCursorAdapter.getCursor();
                    if (cursor != null && !cursor.isClosed()) {
                        int titlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                        int linkPos = cursor.getColumnIndex(EntryColumns.LINK);
                        if (cursor.moveToFirst()) {
                            do {
                                starredList.append(cursor.getString(titlePos)).append("\n").append(cursor.getString(linkPos)).append("\n\n");
                            } while (cursor.moveToNext());
                        }
                        startActivity(Intent.createChooser(
                                new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_favorites_title))
                                        .putExtra(Intent.EXTRA_TEXT, starredList.toString()).setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)
                        ));
                    }
                }
                return true;
            }

            case R.id.menu_refresh: {
                startRefresh();
                return true;
            }

            case R.id.menu_cancel_refresh: {
                FetcherService.cancelRefresh();
                return true;
            }

            case R.id.menu_toggle_theme: {
                PrefUtils.ToogleTheme( new Intent(getContext(), HomeActivity.class) );
                return true;
            }


            case R.id.menu_reset_feed_and_delete_all_articles: {
                //if ( FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == FeedDataContentProvider.URI_ENTRIES_FOR_FEED ) {
                    new AlertDialog.Builder(getContext()) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle( R.string.question ) //
                            .setMessage( R.string.deleteAllEntries ) //
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            FetcherService.deleteAllFeedEntries(mCurrentUri);
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null).show();
                //}
                return true;
            }
            case R.id.menu_mark_all_as_read: {
                markAllAsRead();
                return true;

            }
            case R.id.menu_delete_old: {
                FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_DELETE_OLD ) );
                return true;

            }
            case R.id.menu_show_article_url_toggle: {
                PrefUtils.toggleBoolean( SHOW_ARTICLE_URL, false );
                item.setChecked( PrefUtils.getBoolean( SHOW_ARTICLE_URL, false ) );
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
                    final String feedID = mCurrentUri.getPathSegments().get(1);
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    ContentValues values = new ContentValues();
                    mShowTextInEntryList = !mShowTextInEntryList;
                    values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryList);
                    cr.update(FeedColumns.CONTENT_URI(feedID), values, null, null);
                    setData( mCurrentUri, mShowFeedInfo, false, mShowTextInEntryList, mOptions );
                }
                return true;
            }
            case R.id.menu_create_auto_backup: {
                FetcherService.StartService( FetcherService.GetIntent( Constants.FROM_AUTO_BACKUP ) );
                return true;
            }
            case R.id.menu_add_feed_shortcut: {
                if ( ShortcutManagerCompat.isRequestPinShortcutSupported(getContext()) ) {
                    //Adding shortcut for MainActivity on Home screen

                    String name = "";
                    IconCompat image = null;
                    if ( EntryColumns.CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString(R.string.all_entries);
                        image = IconCompat.createWithResource(getContext(), R.drawable.cup_new_pot);
                    } else if ( EntryColumns.FAVORITES_CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString(R.string.favorites);
                        image = IconCompat.createWithResource( getContext(), R.drawable.cup_new_star );
                    } else if ( EntryColumns.UNREAD_ENTRIES_CONTENT_URI.equals(mCurrentUri) ) {
                        name = getContext().getString( R.string.unread_entries );
                        image = IconCompat.createWithResource(getContext(), R.drawable.cup_new_unread);
                    } else if ( FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() ).equals(mCurrentUri) ) {
                        name = getContext().getString( R.string.externalLinks );
                        image = IconCompat.createWithResource(getContext(), R.drawable.cup_new_load_later);
                    } else {
                        long feedID = Long.parseLong( mCurrentUri.getPathSegments().get(1) );
                        Cursor cursor = getContext().getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedID),
                                new String[]{FeedData.FeedColumns.NAME, FeedColumns.ICON_URL},
                                null, null, null);
                        if (cursor.moveToFirst()) {
                            name = cursor.getString(0);
                            if (!cursor.isNull(1) && !cursor.isNull(2))
                                image = IconCompat.createWithBitmap(UiUtils.getFaviconBitmap( cursor.getString( 1 ) ));
                        }
                        cursor.close();
                    }

                    ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(getContext(), mCurrentUri.toString())
                            .setIcon(image)
                            .setShortLabel(name)
                            .setIntent(new Intent(getContext(), HomeActivity.class).setAction(Intent.ACTION_MAIN).setData( mCurrentUri ))
                            .build();
                    ShortcutManagerCompat.requestPinShortcut(getContext(), pinShortcutInfo, null);
                    if ( Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O )
                        Toast.makeText( getContext(), R.string.new_feed_shortcut_added, Toast.LENGTH_LONG ).show();
                } else
                    Toast.makeText( getContext(), R.string.new_feed_shortcut_add_failed, Toast.LENGTH_LONG ).show();
                return true;
            }
            case R.id.menu_toogle_toogle_unread_all: {
                int uriMatch = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
                mShowUnRead = !mShowUnRead;
                if ( uriMatch == FeedDataContentProvider.URI_ENTRIES_FOR_FEED ||
                     uriMatch == FeedDataContentProvider.URI_UNREAD_ENTRIES_FOR_FEED ) {
                    long feedID = Long.parseLong( mCurrentUri.getPathSegments().get(1) );
                    Uri uri = mShowUnRead ? EntryColumns.UNREAD_ENTRIES_FOR_FEED_CONTENT_URI(feedID) : EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID);
                    setData( uri, mShowFeedInfo, false, mShowTextInEntryList, mOptions );
                } else if ( uriMatch == FeedDataContentProvider.URI_ENTRIES_FOR_GROUP ||
                            uriMatch == FeedDataContentProvider.URI_UNREAD_ENTRIES_FOR_GROUP ) {
                    long groupID = Long.parseLong( mCurrentUri.getPathSegments().get(1) );
                    Uri uri = mShowUnRead ? EntryColumns.UNREAD_ENTRIES_FOR_GROUP_CONTENT_URI(groupID) : EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(groupID);
                    setData( uri, mShowFeedInfo, false, mShowTextInEntryList, mOptions );
                } else if ( uriMatch == FeedDataContentProvider.URI_FAVORITES ||
                    uriMatch == FeedDataContentProvider.URI_FAVORITES_UNREAD ) {
                    Uri uri = mShowUnRead ? EntryColumns.FAVORITES_UNREAD_CONTENT_URI : EntryColumns.FAVORITES_CONTENT_URI;
                    setData( uri, mShowFeedInfo, false, mShowTextInEntryList, mOptions );
                }
                UpdateActions();
                return true;
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


    @SuppressLint("PrivateResource")
    private void markAllAsRead() {
        if (mEntriesCursorAdapter != null) {
            Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.light_theme_color_primary))
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new Thread() {
                                @Override
                                public void run() {
                                    if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                                        ArrayList<Integer> ids = new ArrayList<>();
                                        while (mJustMarkedAsReadEntries.moveToNext()) {
                                            ids.add(mJustMarkedAsReadEntries.getInt(0));
                                        }
                                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                                        String where = BaseColumns._ID + " IN (" + TextUtils.join(",", ids) + ')';
                                        cr.update(FeedData.EntryColumns.CONTENT_URI, FeedData.getUnreadContentValues(), where, null);

                                        mJustMarkedAsReadEntries.close();
                                    }
                                }
                            }.start();
                        }
                    });
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
                }
            }.start();


        }
    }

    private void startRefresh() {
        if ( mCurrentUri != null && !PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            int uriMatcher = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
            if ( uriMatcher == FeedDataContentProvider.URI_ENTRIES_FOR_FEED ||
                 uriMatcher == FeedDataContentProvider.URI_UNREAD_ENTRIES_FOR_FEED ) {
                FetcherService.StartService( new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(Constants.FEED_ID, mCurrentUri.getPathSegments().get(1)));
            } else if ( FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == FeedDataContentProvider.URI_ENTRIES_FOR_GROUP ) {
                FetcherService.StartService( new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(Constants.GROUP_ID, mCurrentUri.getPathSegments().get(1)));
            } else {
                FetcherService.StartService( new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }

    }

    public Uri getUri() {
        return mOriginalUri;
    }

    public void setData(Uri uri, boolean showFeedInfo, boolean isSearchUri, boolean showTextInEntryList, JSONObject options) {
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

        mShowFeedInfo = showFeedInfo;
        mShowTextInEntryList = showTextInEntryList;
        new Thread() {
            @Override
            public void run() {
                synchronized ( mWasVisibleList ) {
                    SetVisibleItemsAsOld(mWasVisibleList);
                    mWasVisibleList.clear();
                }
            }
        }.start();
        mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mShowTextInEntryList, mShowUnRead);
        SetListViewAdapter();
        //if ( mListView instanceof ListView )
            mListView.setDividerHeight( mShowTextInEntryList ? 10 : 0 );
        //mListDisplayDate = new Date().getTime();
        if (mCurrentUri != null) {
            restartLoaders();
        }

        //refreshUI();
        UpdateFooter();
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
                Long.parseLong(uri.getPathSegments().get(1));
                result = true;
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
    public static void SetVisibleItemsAsOld(ArrayList<String> uriList) {
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
    @Override
    public void update(Observable observable, Object o) {
        //if ( !mShowTextInEntryList )
        //    return;
        if ( o instanceof Entry ) {
            final Entry entry = (Entry) o;
            final int pos = mEntriesCursorAdapter.GetPosByID(entry.mID);
            if (pos >= mListView.getFirstVisiblePosition() && pos <= mListView.getLastVisiblePosition()) {
                mEntriesCursorAdapter.mIgnoreClearContentVocOnCursorChange = true;
                mEntriesCursorAdapter.notifyDataSetChanged();
                RestoreListScrollPosition();
            }
        } else if ( o instanceof EntriesCursorAdapter.ListViewTopPos) {
            mListView.setSelectionFromTop( ((EntriesCursorAdapter.ListViewTopPos)o).mPos, 0 );
        }
    }
}
