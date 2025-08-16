package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.service.FetcherService.Status;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;

import java.util.Stack;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;

public abstract class EntryView {
    EntryActivity mActivity = null;
    public boolean mLoadTitleOnly = false;
    public boolean mContentWasLoaded = false;
    public double mScrollPartY = 0;
    public Cursor mCursor = null;
    private int mStatus = 0;
    public String mTitle;
    public static final String TAG = "EntryView";

    private final Stack<Integer> mHistoryAnchorScrollY = new Stack<>();
    public long mEntryId = -1;
    public View mView = null;

    EntryView( EntryActivity activity ) {
        mActivity = activity;
    }

    public boolean CanGoBack() {
        return !mHistoryAnchorScrollY.isEmpty();
    }
    public void ClearHistoryAnchor() {
        mHistoryAnchorScrollY.clear();
        mActivity.mEntryFragment.SetupZones();
    }

    public void GoBack() {
        if (CanGoBack())
            ScrollTo(mHistoryAnchorScrollY.pop());
        mActivity.mEntryFragment.SetupZones();
    }

    public void GoTop() {
        AddNavigationHistoryStep();
        ScrollTo(0 );
    }

    public void AddNavigationHistoryStep() {
        mHistoryAnchorScrollY.push(GetScrollY());
        mActivity.mEntryFragment.SetupZones();
    }

    protected abstract int GetScrollY();
    protected abstract void ScrollTo( int y );
    public abstract void ScrollToBottom();
    public abstract void PageChange(int delta, StatusText statusText);
    protected abstract double GetViewScrollPartY();

    public void SaveScrollPos() {
        if ( !mContentWasLoaded )
            return;
        mScrollPartY = GetViewScrollPartY();
        if ( mScrollPartY > 0.0001 ) {
            //Dog.v(TAG, String.format("EnrtyView.SaveScrollPos (entry %d) mScrollPartY = %f getScrollY() = %d, view.getContentHeight() = %f", mEntryId, mScrollPartY, getScrollY(), GetContentHeight()));
            ContentValues values = new ContentValues();
            values.put(FeedData.EntryColumns.SCROLL_POS, mScrollPartY);
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            SetNotifyEnabled(false ); try {
                cr.update(FeedData.EntryColumns.CONTENT_URI(mEntryId), values, null, null);
            } finally {
                SetNotifyEnabled( true );
            }
        }
    }

    public abstract ProgressInfo getProgressInfo( int statusHeight );

    static public class ProgressInfo {
        public int max;
        public int progress;
        public int step;
    }

    public void StatusStartPageLoading() {
        synchronized (this) {
            if (mStatus == 0)
                mStatus = Status().Start(R.string.web_page_loading, true);
        }
    }
    public void EndStatus() {
        synchronized (this) {
            if ( !mContentWasLoaded && !mLoadTitleOnly )
                return;
            if (mStatus != 0)
                Status().End(mStatus);
            mStatus = 0;
        }
    }
    public boolean IsStatusStartPageLoading() {
        synchronized (this) {
            return mStatus == 0;
        }
    }
    public void InvalidateContentCache() {

    }
    public boolean setHtml(final long entryId,
                           Uri articleListUri,
                           Cursor newCursor,
                           FeedFilters filters,
                           boolean isFullTextShown,
                           boolean forceUpdate,
                           EntryActivity activity) {
        mActivity = activity;
        mEntryId = entryId;
        return true;
    }
    public void onResume() {

    }
    public void onPause() {

    }
}

