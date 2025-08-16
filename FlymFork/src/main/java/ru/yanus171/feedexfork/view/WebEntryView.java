package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.activity.EditFeedActivity.AUTO_SET_AS_READ;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.IS_RSS;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.AddTagButtons;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_IFRAME;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_VIDEO;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.view.WebViewExtended.BASE_URL;
import static ru.yanus171.feedexfork.view.WebViewExtended.TEXT_HTML;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Date;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class WebEntryView extends EntryView {
    public WebViewExtended mWebView = null;
    public int mLastContentLength = 0;
    public long mLastSetHTMLTime = 0;
    public String mEntryLink = "";
    public boolean mWasAutoUnStar = false;
    boolean mIsAutoMarkVisibleAsRead = false;
    private ArrayList<String> mImagesToDl = new ArrayList<>();
    String mData = "";
    public String mDataWithWebLinks = "";
    public boolean mHasScripts = false;
    public boolean mIsEditingMode = false;
    private int mScrollY = 0;

    public WebEntryView(EntryActivity activity, ViewGroup container ) {
        super( activity );
        mWebView = new WebViewExtended( activity, this );
        container.addView(mWebView);
        mView = mWebView;
    }

    @Override
    protected int GetScrollY() {
        if (mScrollY != 0)
            return mScrollY;
        return mWebView.GetContentHeight() * mScrollPartY != 0 ? (int) (mWebView.GetContentHeight() * mScrollPartY) : 0;
    }

    @Override
    protected void ScrollTo( int y) {
        mWebView.ScrollTo( y );
    }

    @Override
    public void ScrollToBottom() {
        AddNavigationHistoryStep();
        ScrollTo((int) mWebView.GetContentHeight() - mWebView.getHeight() );
    }

    @Override
    public void PageChange(int delta, StatusText statusText) {
        ScrollTo((int) (mWebView.getScrollY() + delta * (mWebView.getHeight() - statusText.GetHeight()) *
                (getBoolean("page_up_down_90_pct", false) ? 0.9 : 0.98)));
    }

    @Override
    protected double GetViewScrollPartY() {
        return mWebView.getContentHeight() != 0 ? mWebView.getScrollY() / mWebView.GetContentHeight() : 0;
    }

    @Override
    public ProgressInfo getProgressInfo( int statusHeight ){
        int webViewHeight = mWebView.getMeasuredHeight();
        int contentHeight = (int) Math.floor(mWebView.getContentHeight() * mWebView.getScale());
        ProgressInfo result = new ProgressInfo();
        result.max = contentHeight - webViewHeight;
        result.progress = mWebView.getScrollY();
        result.step = mWebView.getHeight() - statusHeight;
        return result;
    }
    @Override
    public void InvalidateContentCache() {
        mLastContentLength = 0;
    }

    @SuppressLint("Range")
    @Override
    public boolean setHtml(final long entryId,
                           Uri articleListUri,
                           Cursor newCursor,
                           FeedFilters filters,
                           boolean isFullTextShown,
                           boolean forceUpdate,
                           EntryActivity activity) {
        super.setHtml( entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity );
        Timer timer = new Timer("EntryView.setHtml");
        mLastSetHTMLTime = new Date().getTime();

        mEntryLink = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.LINK));

        final String feedID = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID));
        final String author = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.AUTHOR));
        final String categories = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.CATEGORIES));
        final long timestamp = newCursor.getLong(newCursor.getColumnIndex(FeedData.EntryColumns.DATE));
        //final String feedTitle = filters.removeTextFromTitle( newCursor.getString(newCursor.getColumnIndex(FeedData.FeedColumns.NAME)) );
        String title =
                newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.TITLE));
        if ( filters != null )
            title = filters.removeText(title, DB_APPLIED_TO_TITLE );
        final String enclosure = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE));
        mWasAutoUnStar = newCursor.getInt(newCursor.getColumnIndex(FeedData.EntryColumns.IS_WAS_AUTO_UNSTAR)) == 1;
        mScrollPartY = !newCursor.isNull(newCursor.getColumnIndex(FeedData.EntryColumns.SCROLL_POS)) ?
                newCursor.getDouble(newCursor.getColumnIndex(FeedData.EntryColumns.SCROLL_POS)) : 0;
        boolean hasOriginal = !feedID.equals(GetExtrenalLinkFeedID());
        mIsAutoMarkVisibleAsRead = false;
        try {
            JSONObject options = new JSONObject( newCursor.getString(newCursor.getColumnIndex(FeedData.FeedColumns.OPTIONS)) );
            hasOriginal = hasOriginal && options.has( IS_RSS ) && options.getBoolean( IS_RSS );
            mIsAutoMarkVisibleAsRead = options.has(AUTO_SET_AS_READ) && options.getBoolean(AUTO_SET_AS_READ);
        } catch (Exception ignored) {

        }
        String contentText;
        if (mLoadTitleOnly)
            contentText = mActivity.getString(R.string.loading);
        else {
            try {
                if (!feedID.equals(GetExtrenalLinkFeedID()) &&
                        (!FileUtils.INSTANCE.isMobilized(mEntryLink, newCursor) || (forceUpdate && !isFullTextShown))) {
                    isFullTextShown = false;
                    contentText = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT));
                    if ( filters != null )
                        contentText = filters.removeText(contentText, DB_APPLIED_TO_CONTENT );
                } else {
                    isFullTextShown = true;
                    contentText = FileUtils.INSTANCE.loadMobilizedHTML(mEntryLink, newCursor);
                }
                if (contentText == null)
                    contentText = "";
            } catch (IllegalStateException e) {
                e.printStackTrace();
                contentText = "Context too large";
            }
        }

        if (!mLoadTitleOnly && contentText.length() == mLastContentLength ) {
            EndStatus();
            return isFullTextShown;
        }
        mLastContentLength = contentText.length();
        //getSettings().setBlockNetworkLoads(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(false);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebView.setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor()));
        // Text zoom level from preferences
        //int fontSize = PrefUtils.getFontSize();
        //if (fontSize != 0) {
        mWebView.getSettings().setTextZoom(100);
        //}

        final String finalContentText = contentText;
        final boolean finalIsFullTextShown = isFullTextShown;
        final boolean finalHasOriginal = hasOriginal;
        final String finalTitle = title;

        new Thread() {
            @Override
            public void run() {
                final String dataWithLinks = mWebView.generateHtmlContent(feedID, articleListUri, finalTitle, mEntryLink, finalContentText, categories, enclosure, author, timestamp, finalIsFullTextShown, finalHasOriginal);
                final ArrayList<String> imagesToDl = new ArrayList<>();
                final String data = HtmlUtils.replaceImageURLs( dataWithLinks, "", mEntryId, mEntryLink, false, imagesToDl, null, mMaxImageDownloadCount );
                synchronized (mWebView) {
                    mImagesToDl = imagesToDl;
                    mData = data;
                    mDataWithWebLinks = dataWithLinks;
                    mHasScripts = dataWithLinks.contains( "<script" );
                }
                UiUtils.RunOnGuiThread(() -> LoadData());
            }
        }.start();
        mTitle = title;
        timer.End();
        return isFullTextShown;
    }
    @Override
    public void onResume() {
        mWebView.onResume();
    }
    @Override
    public void onPause() {
        mWebView.onPause();
    }
    @NotNull
    private ArrayList<String> GetImageListCopy() {
        final ArrayList<String> imagesToDl;
        synchronized ( mWebView ) {
            imagesToDl = (ArrayList<String>) mImagesToDl.clone();
        }
        return imagesToDl;
    }
    public String GetData() {
        synchronized (mWebView) {
            return mData;
        }
    }

    public String GetDataWithLinks() {
        synchronized (mWebView) {
            return mDataWithWebLinks;
        }
    }

    public void UpdateImages( final boolean downloadImages ) {
        if ( !downloadImages )
            StatusStartPageLoading();
        Dog.v( EntryView.TAG, "UpdateImages" );
        new Thread() {
            @Override
            public void run() {
                final String data = HtmlUtils.replaceImageURLs( mDataWithWebLinks, mEntryId, mEntryLink, downloadImages);
                synchronized (mWebView) {
                    mData = data;
                }
                UiUtils.RunOnGuiThread(() -> {
                    if ( !IsStatusStartPageLoading() )
                        mScrollY = mWebView.getScrollY();
                    if ( !downloadImages )
                        LoadData();
                });
            }
        }.start();
    }

    public void LoadData() {
        Dog.v( EntryView.TAG, "LoadDate" );
        if ( mContentWasLoaded && GetViewScrollPartY() > 0 )
            mScrollPartY = GetViewScrollPartY();
        final String data;
        synchronized (mWebView) {
            data = mData;
        }
        mWebView.mLastContentHeight = 0;
        mWebView.loadDataWithBaseURL(BASE_URL, data, TEXT_HTML, Constants.UTF8, null);
    }

    public void DownLoadImages() {
        final ArrayList<String> imagesToDl = GetImageListCopy();
        if ( !imagesToDl.isEmpty() )
            new Thread(() -> {
                FetcherService.downloadEntryImages("", mEntryId, mEntryLink, imagesToDl);
                ClearImageList();
            }).start();
    }

    private void ClearImageList() {
        synchronized ( mWebView ) {
            mImagesToDl.clear();
        }
    }
    public boolean hasVideo() {
        return PATTERN_VIDEO.matcher(mDataWithWebLinks).find() ||
                PATTERN_IFRAME.matcher(mDataWithWebLinks).find();
    }
    public void moveToAnchor(WebView view, String hash) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Dog.v( TAG, "EntryView.moveToAnchor " + hash );
            view.evaluateJavascript("javascript:window.location.hash = '" + hash + "';", null);
            mScrollY = 0;
        }
    }

    public void UpdateTags() {
        final int status = Status().Start(mActivity.getString(R.string.last_update), true);
        Document doc = Jsoup.parse(ArticleTextExtractor.mLastLoadedAllDoc, NetworkUtils.getUrlDomain(mEntryLink));
        AddTagButtons(doc, mEntryLink);
        final String data = mWebView.generateHtmlContent("-1", Uri.EMPTY,"", mEntryLink, doc.toString(), "", "", "", 0, true, false);
        synchronized (mWebView) {
            mData = data;
        }
        mScrollY = mWebView.getScrollY();
        LoadData();
        Status().End(status);
    }
    public static void NotifyToUpdate(final long entryId, final String entryLink, final boolean restorePosition) {
        UiUtils.RunOnGuiThread(() -> {
            Dog.v( EntryView.TAG, String.format( "NotifyToUpdate( %d )", entryId ) );
            WebViewExtended.mImageDownloadObservable.notifyObservers(new Entry(entryId, entryLink, restorePosition) );
        }, 0 );//NOTIFY_OBSERVERS_DELAY_MS);
    }

}

