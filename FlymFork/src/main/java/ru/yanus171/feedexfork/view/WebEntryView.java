package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.activity.EditFeedActivity.AUTO_SET_AS_READ;
import static ru.yanus171.feedexfork.parser.OPML.FILENAME_DATETIME_FORMAT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.service.FetcherService.EXTRA_LABEL_ID_LIST;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.IS_RSS;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.service.FetcherService.isLinkToLoad;
import static ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.AddTagButtons;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_IFRAME;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_VIDEO;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetPackageNameForAction;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetShowInBrowserIntent;
import static ru.yanus171.feedexfork.view.MenuItem.ShowMenu;
import static ru.yanus171.feedexfork.view.WebViewExtended.BASE_URL;
import static ru.yanus171.feedexfork.view.WebViewExtended.TEXT_HTML;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.LoadLinkLaterActivity;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.LabelVoc;
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
        mWebView.ScrollSmoothTo( y );
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

    public static void ShowLinkMenu(String url, String title, Context context ) {
        final MenuItem itemTitle = new MenuItem(url);
        final MenuItem itemReadNow = new MenuItem(R.string.loadLink, R.drawable.cup_new_load_now, new Intent(context, EntryActivity.class).setData(Uri.parse(url)) );
        final MenuItem itemLater = new MenuItem(R.string.loadLinkLater, R.drawable.cup_new_load_later, new Intent(context, LoadLinkLaterActivity.class).setData(Uri.parse(url)));
        final MenuItem itemLaterInFavorities = new MenuItem(R.string.loadLinkLaterStarred, R.drawable.cup_new_load_later_star, (_1, _2) ->
                LabelVoc.INSTANCE.showDialog(context, R.string.article_labels_setup_title, false, new HashSet<>(), null, (checkedLabels) -> {
                    Intent intent_ = new Intent(context, LoadLinkLaterActivity.class).setData(Uri.parse(url)).putExtra(FetcherService.EXTRA_STAR, true);
                    ArrayList<String> list = new ArrayList<>();
                    for (long labelID : checkedLabels)
                        list.add(String.valueOf(labelID));
                    intent_.putStringArrayListExtra(EXTRA_LABEL_ID_LIST, list);
                    context.startActivity(intent_);
                    return null;
                }));
        final MenuItem itemOpenLink = new MenuItem(R.string.open_link, android.R.drawable.ic_menu_send, GetShowInBrowserIntent(url) );
        final MenuItem itemShare = new MenuItem(R.string.menu_share, android.R.drawable.ic_menu_share, Intent.createChooser(
                new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, url)
                        .setType(Constants.MIMETYPE_TEXT_PLAIN), context.getString(R.string.menu_share)) );
        final MenuItem[] items = { itemTitle, itemReadNow, itemLater, itemLaterInFavorities, itemOpenLink, itemShare };
        final MenuItem[] itemsNoRead = { itemTitle, itemOpenLink, itemShare };

        ShowMenu(!isLinkToLoad(url) ? itemsNoRead : items, title, context);
    }

    @SuppressLint("SimpleDateFormat")
    public static String getDestFileName(String title) {
        return sanitizeFilename( title ) + "_" + new SimpleDateFormat(FILENAME_DATETIME_FORMAT).format(new Date());
    }
    public static String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public static void ShowImageMenu(String url, String title, Context context) {
        final MenuItem[] items = {
                new MenuItem(R.string.menu_share, android.R.drawable.ic_menu_share, (_1, _2) -> ShareImage(url, context) ),
                new MenuItem(R.string.copy_to_downloads, android.R.drawable.ic_menu_save, (_1, _2) -> {
                    File file = new File(url.replace(Constants.FILE_SCHEME, ""));
                    FileUtils.INSTANCE.copyFileToDownload( file.getAbsolutePath(), getDestFileName(title), true );
                }),
                new MenuItem(R.string.open_image, android.R.drawable.ic_menu_view, (_1, _2) -> OpenImage(url, context) )
        };
        ShowMenu(items, null, context );
    }




    public static void OpenImage( String url, Context context ) {
        try {
            File file = new File(url.replace(Constants.FILE_SCHEME, ""));
            File extTmpFile = new File(context.getCacheDir(), file.getName());
            FileUtils.INSTANCE.copy(file, extTmpFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = FileUtils.INSTANCE.getUriForFile( extTmpFile );
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "image/*");
            final String packageName = GetPackageNameForAction( "openImageTapAction" );
            if ( packageName != null )
                intent.setPackage(packageName);
            context.startActivity(intent);
        } catch ( Exception e ) {
            e.printStackTrace();
            UiUtils.toast( context, context.getString( R.string.cant_open_image ) + ": " + e.getLocalizedMessage() );
        }
    }

    public static void ShareImage( String url, Context context ) {
        try {
            File file = new File(url.replace(Constants.FILE_SCHEME, ""));
            File extTmpFile = new File(context.getCacheDir(), file.getName());
            FileUtils.INSTANCE.copy(file, extTmpFile);
            Uri contentUri = FileUtils.INSTANCE.getUriForFile( extTmpFile );
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType("image/jpeg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch ( Exception e ) {
            e.printStackTrace();
            UiUtils.toast( context, context.getString( R.string.cant_open_image ) + ": " + e.getLocalizedMessage() );
        }
    }

}

