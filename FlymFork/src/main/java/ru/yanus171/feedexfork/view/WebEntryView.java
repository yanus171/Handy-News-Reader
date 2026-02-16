package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.Constants.MILLS_IN_SECOND;
import static ru.yanus171.feedexfork.fragment.EntryFragment.STATE_RELOAD_IMG_WITH_A_LINK;
import static ru.yanus171.feedexfork.fragment.EntryFragment.STATE_RELOAD_WITH_DEBUG;
import static ru.yanus171.feedexfork.fragment.EntryMenu.setVisible;
import static ru.yanus171.feedexfork.fragment.EntryMenu.setItemChecked;
import static ru.yanus171.feedexfork.parser.OPML.FILENAME_DATETIME_FORMAT;
import static ru.yanus171.feedexfork.service.FetcherService.EXTRA_LABEL_ID_LIST;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.service.FetcherService.isLinkToLoad;
import static ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_IFRAME;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_VIDEO;
import static ru.yanus171.feedexfork.utils.PrefUtils.CATEGORY_EXTRACT_RULES;
import static ru.yanus171.feedexfork.utils.PrefUtils.CONTENT_TEXT_ROOT_EXTRACT_RULES;
import static ru.yanus171.feedexfork.utils.PrefUtils.DATE_EXTRACT_RULES;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetPackageNameForAction;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetShowInBrowserIntent;
import static ru.yanus171.feedexfork.view.MenuItem.ShowMenu;
import static ru.yanus171.feedexfork.view.WebViewExtended.BASE_URL;
import static ru.yanus171.feedexfork.view.WebViewExtended.TEXT_HTML;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.LoadLinkLaterActivity;
import ru.yanus171.feedexfork.activity.MessageBox;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.fragment.EntryTextSearch;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.Log;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class WebEntryView extends EntryView implements WebViewExtended.EntryViewManager, Observer {
    public WebViewExtended mWebView = null;
    private ArrayList<String> mImagesToDl = new ArrayList<>();
    String mData = "";
    public String mDataWithWebLinks = "";
    public boolean mHasScripts = false;
    public boolean mIsEditingMode = false;
    public boolean mIsFullTextShown = true;
    private boolean mRetrieveFullText = false;
    public long mLastSetHTMLTime = 0;
    private boolean mIsWithTables;
    private boolean mWasAutoUnStar = false;
    boolean mLoadTitleOnly = false;
    private int mLastContentHash = 0;

    private int mIsWithTablePos = -1, mEnclosurePos, mRetrieveFullTextPos;

    private EntryTextSearch mSearch = null;
    HashSet<String> mNotLoadedUrlSet = new HashSet<>();

    public WebEntryView(EntryFragment fragment, ViewGroup container, long entryId, int position) {
        super(fragment, entryId, position);
        mLoadTitleOnly = true;
        mWebView = new WebViewExtended(getContext(), this);
        container.addView(mWebView);
        mView = mWebView;
        mWebView.setListener(this);
        mWebView.mScrollChangeListener = () -> {
            if ( mEntryFragment.mTapZones != null )
                mEntryFragment.mTapZones.Hide();
            if ( mEntryFragment.mControlPanel != null )
                mEntryFragment.mControlPanel.hide();

            if (!mFavorite)
                return;
            if (mRetrieveFullText && !mIsFullTextShown)
                return;
            if (!getBoolean("entry_auto_unstart_at_bottom", true))
                return;
            if (mWasAutoUnStar)
                return;
            if (!mContentWasLoaded)
                return;
            if (IsScrollAtBottom() && new Date().getTime() - mLastSetHTMLTime > MILLS_IN_SECOND * 5) {
                final Uri uri = getUri();
                mWasAutoUnStar = true;
                new Thread() {
                    @Override
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put(FeedData.EntryColumns.IS_WAS_AUTO_UNSTAR, 1);
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.update(uri, values, null, null);
                    }
                }.start();
                SetIsFavorite(false, true);
            }
        };
        mSearch = new EntryTextSearch( mWebView );
        EntryUrlVoc.INSTANCE.getMObservable().addObserver( this );
    }

    @Override
    public void Destroy() {
        super.Destroy();
        EntryUrlVoc.INSTANCE.getMObservable().deleteObserver( this );
    }
    @Override
    protected int GetScrollY() {
        return mWebView.GetContentHeight() * mScrollPartY != 0 ? (int) (mWebView.GetContentHeight() * mScrollPartY) : 0;
    }

    @Override
    protected void ScrollTo(int y, boolean smooth) {
        if (smooth)
            mWebView.ScrollSmoothTo(y);
        else
            mWebView.scrollTo(0, y);
        SaveScrollPos();
    }

    @Override
    public void LongClickOnBottom() {
        AddNavigationHistoryStep();
        ScrollTo((int) mWebView.GetContentHeight() - mWebView.getHeight(), false);
        UiUtils.toastShort( R.string.list_was_scrolled_to_bottom );
    }

    @Override
    public void leftBottomBtnClick() {
        mEntryFragment.PreviousEntry();
    }
    @Override
    public void rightBottomBtnClick() {
        mEntryFragment.NextEntry();
    }

    @Override
    public void ScrollOneScreen(int direction) {
        ScrollTo((int) (mWebView.getScrollY() + direction * mWebView.getPageHeight() * getPageChangeMultiplier()), true);
    }

    @Override
    protected double GetViewScrollPartY() {
        return mWebView.getContentHeight() != 0 ? mWebView.getScrollY() / mWebView.GetContentHeight() : 0;
    }

    @Override
    public ProgressInfo getProgressInfo() {
        int webViewHeight = mWebView.getMeasuredHeight();
        int contentHeight = (int) Math.floor(mWebView.getContentHeight() * mWebView.getScale()) - webViewHeight;
        ProgressInfo result = new ProgressInfo();
        final int screenCount = mWebView.getPageHeight() == 0 ? 1 : contentHeight / mWebView.getPageHeight();
        result.max = screenCount;
        result.progress = (int) (((long)screenCount * mWebView.getScrollY()) / (float)contentHeight);
        result.step = 1;
        return result;
    }

    @SuppressLint("Range")
    public void setHtml(Uri articleListUri, FeedFilters filters ) {
        //super.setHtml( entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity );
        Timer timer = new Timer("EntryView.setHtml");
        mLastSetHTMLTime = new Date().getTime();
        WebEntryContent content = new WebEntryContent( mCursor, articleListUri, filters, mLoadTitleOnly, mIsFullTextShown );
        mWasAutoUnStar = mCursor.getInt(mCursor.getColumnIndex(FeedData.EntryColumns.IS_WAS_AUTO_UNSTAR)) == 1;

        {
            final int contentHash = content.getContentHash();
            if ( mLastContentHash != 0 && mLastContentHash == contentHash ) {
                if ( !mLoadTitleOnly ) {
                    mContentWasLoaded = true;
                    EndStatus();
                }
                return;
            }
            mLastContentHash = contentHash;
        }
        //getSettings().setBlockNetworkLoads(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(false);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebView.setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor()));
        // Text zoom level from preferences
        //int fontSize = PrefUtils.getFontSize();
        //if (fontSize != 0) {
        mWebView.getSettings().setTextZoom(100);
        final HashSet<String> notLoadedUrlSet = (HashSet<String>) mNotLoadedUrlSet.clone();
        new Thread() {
            @Override
            public void run() {
                final String dataWithLinks = content.generateHtml();
                final ArrayList<String> imagesToDl = new ArrayList<>();
                String data = HtmlUtils.replaceImageURLs(dataWithLinks, "", mEntryId, mEntryLink, false, imagesToDl, null, mMaxImageDownloadCount, notLoadedUrlSet);
                synchronized (mWebView) {
                    mImagesToDl = imagesToDl;
                    mData = data;
                    mDataWithWebLinks = dataWithLinks;
                    mHasScripts = dataWithLinks.contains("<script");
                    mNotLoadedUrlSet = notLoadedUrlSet;
                }
                UiUtils.RunOnGuiThread(() -> LoadData( false ));
            }
        }.start();
        mTitle = content.mTitle;
        timer.End();
    }


    @Override
    public void onResume() {
        mWebView.onResume();
    }

    @Override
    public void onPause() {
        mWebView.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if ( !mContentWasLoaded )
            LoadData( true );
    }

    @NotNull
    private ArrayList<String> GetImageListCopy() {
        final ArrayList<String> imagesToDl;
        synchronized (mWebView) {
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

    public void UpdateImagesAndLinks(final boolean downloadImages) {
        if (!downloadImages)
            StatusStartPageLoading();
        Dog.v(EntryView.TAG, "UpdateImages");
        InvalidateContentCache();
        new Thread() {
            @Override
            public void run() {
                final String data = HtmlUtils.replaceImageURLs(mDataWithWebLinks, mEntryId, mEntryLink, downloadImages, mNotLoadedUrlSet);
                synchronized (mWebView) {
                    mData = data;
                }
                UiUtils.RunOnGuiThread(() -> {
                    if (!downloadImages)
                        LoadData( true );
                });
            }
        }.start();
    }

    public void LoadData( boolean updateScrollPart ) {
        Dog.v(EntryView.TAG, "LoadDate");
        if ( updateScrollPart && mContentWasLoaded && GetViewScrollPartY() > 0) {
            mScrollPartY = GetViewScrollPartY();
            Log( "LoadData mScrollPartY = " + mScrollPartY );
        }
        final String data;
        synchronized (mWebView) {
            data = mData;
        }
        mWebView.mLastContentHeight = 0;
        mWebView.loadDataWithBaseURL(BASE_URL, data, TEXT_HTML, Constants.UTF8, null);
    }

    public void DownLoadImages() {
        final ArrayList<String> imagesToDl = GetImageListCopy();
        if (!imagesToDl.isEmpty())
            new Thread(() -> {
                FetcherService.downloadEntryImages("", mEntryId, mEntryLink, imagesToDl);
                ClearImageList();
            }).start();
    }

    private void ClearImageList() {
        synchronized (mWebView) {
            mImagesToDl.clear();
        }
    }

    public boolean hasVideo() {
        return PATTERN_VIDEO.matcher(mDataWithWebLinks).find() ||
                PATTERN_IFRAME.matcher(mDataWithWebLinks).find();
    }

    public void moveToAnchor(WebView view, String hash) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Dog.v(TAG, "EntryView.moveToAnchor " + hash);
            view.evaluateJavascript("javascript:window.location.hash = '" + hash + "';", null);
        }
    }

    public void UpdateTags() {
        final int status = Status().Start(getContext().getString(R.string.last_update), true);
        WebEntryContent content = new WebEntryContent(ArticleTextExtractor.mLastLoadedAllDoc, NetworkUtils.getUrlDomain(mEntryLink), mEntryLink);
        final String data = content.generateHtml();
        synchronized (mWebView) {
            mData = data;
        }
        LoadData( true );
        Status().End(status);
    }

    public static void NotifyToUpdate(final long entryId, final String entryLink, final boolean restorePosition) {
        UiUtils.RunOnGuiThread(() -> {
            Dog.v(EntryView.TAG, String.format("NotifyToUpdate( %d )", entryId));
            WebViewExtended.mImageDownloadObservable.notifyObservers(new Entry(entryId, entryLink, restorePosition));
        }, 0);//NOTIFY_OBSERVERS_DELAY_MS);
    }

    public static void ShowLinkMenu(String url, String title, Context context) {
        final MenuItem itemTitle = new MenuItem(url);
        final MenuItem itemReadNow = new MenuItem(R.string.loadLink, R.drawable.cup_new_load_now, new Intent(context, EntryActivity.class).setData(Uri.parse(url)));
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
        final MenuItem itemOpenLink = new MenuItem(R.string.open_link, android.R.drawable.ic_menu_send, GetShowInBrowserIntent(url));
        final MenuItem itemShare = new MenuItem(R.string.menu_share, android.R.drawable.ic_menu_share, Intent.createChooser(
                new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, url)
                        .setType(Constants.MIMETYPE_TEXT_PLAIN), context.getString(R.string.menu_share)));
        final MenuItem[] items = {itemTitle, itemReadNow, itemLater, itemLaterInFavorities, itemOpenLink, itemShare};
        final MenuItem[] itemsNoRead = {itemTitle, itemOpenLink, itemShare};

        ShowMenu(!isLinkToLoad(url) ? itemsNoRead : items, title, context, () -> getWebPageTitle( url ));
    }

    static String getWebPageTitle(String link) {
        InputStream response = null;
        try {
            response = new URL(link).openStream();
            Scanner scanner = new Scanner(response);
            String responseBody = scanner.useDelimiter("\\A").next();
            final int start = responseBody.indexOf("<title>") + 7;
            final int end = responseBody.indexOf("</title>");
            if ( start >= 0 && end > start )
                return responseBody.substring(start, end);
            else
                return link;
        } catch (IOException e) {
            DebugApp.AddErrorToLog( null, e );
        } finally {
            try {
                response.close();
            } catch (Exception e) {
                DebugApp.AddErrorToLog( null, e );
            }
        }
        return "";
    }

    @SuppressLint("SimpleDateFormat")
    public static String getDestFileName(String title) {
        return sanitizeFilename(title) + "_" + new SimpleDateFormat(FILENAME_DATETIME_FORMAT).format(new Date());
    }

    public static String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public static void ShowImageMenu(String url, String title, Context context) {
        final MenuItem[] items = {
                new MenuItem(R.string.menu_share, android.R.drawable.ic_menu_share, (_1, _2) -> ShareImage(url, context)),
                new MenuItem(R.string.copy_to_downloads, android.R.drawable.ic_menu_save, (_1, _2) -> {
                    File file = new File(url.replace(Constants.FILE_SCHEME, ""));
                    FileUtils.INSTANCE.copyFileToDownload(file.getAbsolutePath(), getDestFileName(title), true);
                }),
                new MenuItem(R.string.open_image, android.R.drawable.ic_menu_view, (_1, _2) -> OpenImage(url, context))
        };
        ShowMenu(items, null, context);
    }


    public static void OpenImage(String url, Context context) {
        try {
            File file = new File(url.replace(Constants.FILE_SCHEME, ""));
            File extTmpFile = new File(context.getCacheDir(), file.getName());
            FileUtils.INSTANCE.copy(file, extTmpFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = FileUtils.INSTANCE.getUriForFile(extTmpFile);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "image/*");
            final String packageName = GetPackageNameForAction("openImageTapAction");
            if (packageName != null)
                intent.setPackage(packageName);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            UiUtils.toast( context.getString(R.string.cant_open_image) + ": " + e.getLocalizedMessage());
        }
    }

    public static void ShareImage(String url, Context context) {
        try {
            File file = new File(url.replace(Constants.FILE_SCHEME, ""));
            File extTmpFile = new File(context.getCacheDir(), file.getName());
            FileUtils.INSTANCE.copy(file, extTmpFile);
            Uri contentUri = FileUtils.INSTANCE.getUriForFile(extTmpFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType("image/jpeg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            UiUtils.toast( context.getString(R.string.cant_open_image) + ": " + e.getLocalizedMessage());
        }
    }

    @Override
    public boolean IsScrollAtBottom() {
        return mWebView.getScrollY() + mWebView.getMeasuredHeight() >= (int) Math.floor(mWebView.GetContentHeight()) - mWebView.getMeasuredHeight() * 0.4;
    }

    @Override
    public void generateArticleContent() {
        super.generateArticleContent();
        if ( mContentWasLoaded )
            return;
        setHtml(mEntryFragment.mBaseUri, mEntryFragment.mFilters);
        Dog.v(String.format("generateArticleContent view.mScrollY  (entry %s) view.mScrollPartY = %f", mEntryId, mScrollPartY));
    }

    @Override
    public void onClickOriginalText() {
        mEntryFragment.getActivity().runOnUiThread(() -> {
            mIsFullTextShown = false;
            update(true);
        });
    }

    @Override
    public void onClickFullText() {
        UiUtils.RunOnGuiThread( () -> {
            mIsFullTextShown = true;
            final boolean alreadyMobilized = FileUtils.INSTANCE.isMobilized(mEntryLink, mCursor);

            if (alreadyMobilized) {
                mEntryFragment.getActivity().runOnUiThread(() -> update(true));
            } else
                LoadFullText(ArticleTextExtractor.MobilizeType.Yes, false, false);
        } );
    }

    public void LoadFullText(final ArticleTextExtractor.MobilizeType mobilize, final boolean isForceReload, final boolean withScripts) {
        //final BaseActivity activity = (BaseActivity) getActivity();
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        // since we have acquired the networkInfo, we use it for basic checks
        if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            new Thread() {
                @Override
                public void run() {
                    int status = FetcherService.Status().Start(getContext().getString(R.string.loadFullText), true);
                    try {
                        FetcherService.mobilizeEntry(mEntryId,
                                mEntryFragment.mFilters,
                                mobilize,
                                FetcherService.AutoDownloadEntryImages.Yes,
                                false,
                                true,
                                isForceReload,
                                withScripts);
                        UiUtils.RunOnGuiThread(() -> update( true ) );
                    } finally {
                        FetcherService.Status().End(status);
                    }
                }
            }.start();
        } else
            UiUtils.RunOnGuiThread(() -> UiUtils.showMessage(mEntryFragment.getActivity(), R.string.network_error));
    }

    @Override
    public void onReloadFullText() {
        UiUtils.RunOnGuiThread(this::ReloadFullText);
    }

    public void ReloadFullText() {
        int status = FetcherService.Status().Start("Reload fulltext", true);
        mIsEditingMode = false;
        update( false );
        try {
            LoadFullText(ArticleTextExtractor.MobilizeType.Yes, true, false);
        } finally {
            FetcherService.Status().End(status);
        }
    }

    @Override
    public void onClose() {
        mEntryFragment.close();
    }

    @Override
    public void onClickEnclosure() {
        mEntryFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String enclosure = mCursor.getString(mEnclosurePos);

                final int position1 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + DrawerAdapter.FIRST_ENTRY_POS());

                final Uri uri = Uri.parse(enclosure.substring(0, position1));
                final String filename = uri.getLastPathSegment();

                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.open_enclosure)
                        .setMessage(getContext().getString(R.string.file) + ": " + filename)
                        .setPositiveButton(R.string.open_link, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showEnclosure(uri, enclosure, position1, position2);
                            }
                        }).setNegativeButton(R.string.download_and_save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    DownloadManager.Request r = new DownloadManager.Request(uri);
                                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                    r.allowScanningByMediaScanner();
                                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    DownloadManager dm = (DownloadManager) MainApplication.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                                    dm.enqueue(r);
                                } catch (Exception e) {
                                    UiUtils.showMessage(mEntryFragment.getActivity(), R.string.error);
                                }
                            }
                        }).show();
            }
        });
    }

    private void showEnclosure(Uri uri, String enclosure, int position1, int position2) {
        try {
            mEntryFragment.getActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + DrawerAdapter.FIRST_ENTRY_POS(), position2)), 0);
        } catch (Exception e) {
            try {
                mEntryFragment.getActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode - let the browser handle this
            } catch (Throwable t) {
                UiUtils.showMessage(mEntryFragment.getActivity(), t.getMessage());
            }
        }
    }

    @Override
    public void onStartVideoFullScreen() {
        //BaseActivity activity = (BaseActivity) getActivity();
        //activity.setNormalFullScreen(true);
    }

    @Override
    public void onEndVideoFullScreen() {
        //BaseActivity activity = (BaseActivity) getActivity();
        //activity.setNormalFullScreen(false);
    }

    @Override
    public FrameLayout getVideoLayout() {
        View layout = mWebView;
        return (layout == null ? null : (FrameLayout) layout.findViewById(R.id.videoLayout));
    }


    @Override
    public void openTagMenu(final String className, final String baseUrl, final String paramValue) {
        ScrollView scroll = new ScrollView(getContext());
        final LinearLayout parent = new LinearLayout(getContext());
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setGravity(Gravity.CENTER);
        UiUtils.AddText(parent, null, getContext().getString(R.string.open_tag_menu_hint)).setTextColor(Theme.GetTextColorReadInt());
        final RadioGroup groupUrl = new RadioGroup(getContext());
        //groupUrl.setGravity( Gravity.CENTER );
        parent.addView(groupUrl);
        int id = 0;
        String keyUrl = baseUrl.replaceAll("http.+?//", "").replaceAll("www.", "");
        if (!keyUrl.endsWith("/"))
            keyUrl = keyUrl + "/";//.substring( 0, keyUrl.length() - 1 );
        while (keyUrl.contains("/")) {
            keyUrl = keyUrl.substring(0, keyUrl.lastIndexOf("/"));
            id++;
            RadioButton btn = new RadioButton(getContext());
            btn.setText(keyUrl);
            btn.setTag(keyUrl);
            btn.setId(id);
            groupUrl.addView(btn);
            btn.setChecked(true);
        }

        scroll.addView(parent);
        scroll.setPadding(0, 0, 0, 20);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(scroll);
        builder.setTitle(getContext().getString(R.string.open_tag_menu_dialog_title) + className);

        final AlertDialog dialog = builder.show();
        AddActionButton(parent, R.string.setFullTextRoot, view -> {
            setFullTextRoot(GetSelectedUrlPart(groupUrl), className);
            dialog.dismiss();
        });
        AddActionButton(parent, paramValue.equals("hide") ? R.string.hide : R.string.show, view -> {
            if (paramValue.equals("hide"))
                removeClass(className);
            else if (paramValue.equals("show"))
                returnClass(className);
            dialog.dismiss();
        });
        AddActionButton(parent, R.string.set_category, view -> {
            setCategory(GetSelectedUrlPart(groupUrl), className);
            dialog.dismiss();
        });

        AddActionButton(parent, R.string.set_date, view -> {
            setDate(GetSelectedUrlPart(groupUrl), className);
            dialog.dismiss();
        });

        AddActionButton(parent, R.string.copyClassNameToClipboard, view -> {
            copyToClipboard(className);
            dialog.dismiss();
        });

        AddActionButton(parent, android.R.string.cancel, view -> dialog.dismiss());
    }

    private void AddActionButton(LinearLayout parent, int captionID, View.OnClickListener listener) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(20, 20, 20, 20);
        lp.gravity = Gravity.CENTER;
        TextView view = UiUtils.AddText(parent, lp, getContext().getString(captionID));
        view.setBackgroundResource(R.drawable.btn_background);
        view.setOnClickListener(listener);
    }

    private String GetSelectedUrlPart(RadioGroup groupUrl) {
        return (String) groupUrl.findViewById(groupUrl.getCheckedRadioButtonId()).getTag();
    }

    private void copyToClipboard(String text) {
        ((android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setText(text);
        Toast.makeText(getContext(), getContext().getString(R.string.text_was_copied_to_clipboard) + ": " + text, Toast.LENGTH_LONG).show();
    }

    private void setFullTextRoot(String baseUrl, String className) {
        setClassObject(baseUrl, className, CONTENT_TEXT_ROOT_EXTRACT_RULES, getContext().getString(R.string.full_text_root_default));
    }

    private void setCategory(String baseUrl, String className) {
        setClassObject(baseUrl, className, CATEGORY_EXTRACT_RULES, "");
    }

    private void setDate(String baseUrl, String className) {
        setClassObject(baseUrl, className, DATE_EXTRACT_RULES, "");
    }

    private void setClassObject(String baseUrl, String className, String prefKey, String defaultPrefValue) {
        ArrayList<String> ruleList = HtmlUtils.Split(PrefUtils.getString(prefKey, defaultPrefValue),
                Pattern.compile("\\n|\\s"));
        int index = -1;
        for (int i = 0; i < ruleList.size(); i++) {
            final String line = ruleList.get(i);
            final String[] list1 = line.split(":");
            final String url = list1[0];
            if (url.equals(baseUrl)) {
                index = i;
                break;
            }
        }
        final String newRule = baseUrl + ":class=" + className;
        if (index != -1)
            ruleList.remove(index);
        ruleList.add(0, newRule);
        PrefUtils.putStringCommit(prefKey, TextUtils.join("\n", ruleList));
        ActionAfterRulesEditing();
    }

    public void removeClass(String className) {
        final String oldPref = PrefUtils.getString(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, "");
        if (!PrefUtils.GetRemoveClassList().contains(className)) {
            PrefUtils.putStringCommit(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, oldPref + "\n" + className);
        }
        ActionAfterRulesEditing();
    }

    private void ActionAfterRulesEditing() {
        UiUtils.RunOnGuiThread(() -> UpdateTags());
    }

    public void returnClass(String classNameList) {
        final ArrayList<String> list = PrefUtils.GetRemoveClassList();
        boolean needRefresh = false;
        for (String className : TextUtils.split(classNameList, " "))
            if (list.contains(className)) {
                needRefresh = true;
                list.remove(className);
            }
        if (needRefresh)
            PrefUtils.putStringCommit(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, TextUtils.join("\n", list));
        ActionAfterRulesEditing();
    }


    @Override
    public void downloadImage(final String url) {
        new Thread(() -> {
            FetcherService.mCancelRefresh = false;
            int status = FetcherService.Status().Start(getContext().getString(R.string.downloadImage), true);
            try {
                NetworkUtils.downloadImage(mEntryId, mEntryLink, url, false, true);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FetcherService.Status().End(status);
            }

        }).start();
    }

    @Override
    public void downloadNextImages() {
        UiUtils.RunOnGuiThread(() -> {
            FetcherService.mMaxImageDownloadCount += PrefUtils.getImageDownloadCount();
            UpdateImagesAndLinks(true);
        });

    }

    @Override
    public void downloadAllImages() {
        UiUtils.RunOnGuiThread(this::DownloadAllImages);
    }

    public void DownloadAllImages() {
        FetcherService.mMaxImageDownloadCount = 0;
        UpdateImagesAndLinks(true);
    }

    @Override
    public void loadingDataFinished() {
        super.loadingDataFinished();
        if (mLoadTitleOnly && mEntryFragment.isCurrentPage( mPosition ) ) {
            mLoadTitleOnly = false;
            mEntryFragment.restartCurrentEntryLoader();
        }
        generateArticleContent();
    }
    @Override
    protected void readDataFromDB() {
        super.readDataFromDB();
        FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
        if ( mIsWithTablePos == -1 ) {
            mIsWithTablePos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_WITH_TABLES);
            mEnclosurePos = mCursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
            mRetrieveFullTextPos = mCursor.getColumnIndex(FeedData.FeedColumns.RETRIEVE_FULLTEXT);
        }
        mRetrieveFullText = mCursor.getInt(mRetrieveFullTextPos) == 1;
        mIsWithTables = mCursor.getInt(mIsWithTablePos) == 1;
        mIsFullTextShown = FileUtils.INSTANCE.isMobilized(mEntryLink, mCursor);
    }

    @Override
    public void onOptionsItemSelected(android.view.MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_load_all_images: {
                DownloadAllImages();
                break;
            }
            case R.id.menu_labels: {
                OpenLabelSetup();
                break;
            }
            case R.id.menu_open_link: {
                getContext().startActivity(GetShowInBrowserIntent(mEntryLink));
                break;
            }

            case R.id.menu_reload_full_text:
            case R.id.menu_reload_full_text_toolbar: {

                ReloadFullText();
                break;
            }
            case R.id.menu_reload_full_text_without_mobilizer: {
                int status = FetcherService.Status().Start("Reload fulltext", true);
                try {
                    LoadFullText(ArticleTextExtractor.MobilizeType.No, true, false);
                } finally {
                    FetcherService.Status().End(status);
                }
                break;
            }

            case R.id.menu_reload_with_tables_toggle: {
                int status = FetcherService.Status().Start("Reload with|out tables", true);
                try {
                    SetIsWithTables(!mIsWithTables);
                    item.setChecked(mIsWithTables);
                    LoadFullText(ArticleTextExtractor.MobilizeType.No, true, false);
                } finally {
                    FetcherService.Status().End(status);
                }
                break;
            }

            case R.id.menu_replace_img_with_a_link_toggle: {
                int status = FetcherService.Status().Start("Replace img with a link", true);
                try {
                    PrefUtils.toggleBoolean(STATE_RELOAD_IMG_WITH_A_LINK, false);
                    item.setChecked(PrefUtils.getBoolean(STATE_RELOAD_IMG_WITH_A_LINK, false));
                    mEntryFragment.getActivity().invalidateOptionsMenu();
                } finally {
                    FetcherService.Status().End(status);
                }
                break;
            }

            case R.id.menu_reload_full_text_with_debug_toggle: {
                int status = FetcherService.Status().Start("Reload with|out debug", true);
                try {
                    PrefUtils.toggleBoolean(STATE_RELOAD_WITH_DEBUG, false);
                    item.setChecked(PrefUtils.getBoolean(STATE_RELOAD_WITH_DEBUG, false));
                    mEntryFragment.getActivity().invalidateOptionsMenu();
                } finally {
                    FetcherService.Status().End(status);
                }
                break;
            }

            case R.id.menu_reload_full_text_with_tags: {
                int status = FetcherService.Status().Start("Reload fulltext", true);
                try {
                    mIsEditingMode = true;
                    LoadFullText(ArticleTextExtractor.MobilizeType.Tags, true, false);
                } finally {
                    FetcherService.Status().End(status);
                }
                update( true );
                break;
            }

            case R.id.menu_reload_full_text_with_scripts: {
                int status = FetcherService.Status().Start("Reload fulltext", true);
                try {
                    LoadFullText(ArticleTextExtractor.MobilizeType.Yes, true, true);
                } finally {
                    FetcherService.Status().End(status);
                }
                break;
            }

            case R.id.menu_edit_article_url: {
                final EditText editText = new EditText(getContext());
                editText.setText(mEntryLink);
                final AlertDialog d = new AlertDialog.Builder(getContext())
                        .setView(editText)
                        .setTitle(R.string.menu_edit_article_url)
                        .setIcon(R.drawable.ic_edit)
                        .setMessage(Html.fromHtml(getContext().getString(R.string.edit_article_url_title_message)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            final Uri uri = getUri();
                            if (!mEntryFragment.getEntryActivity().mIsNewTask)
                                PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, getUri().toString());
                            new Thread() {
                                @Override
                                public void run() {
                                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                                    ContentValues values = new ContentValues();
                                    final String newLink = editText.getText().toString();
                                    values.put(FeedData.EntryColumns.LINK, newLink);
                                    cr.update(uri, values, null, null);
                                    EntryUrlVoc.INSTANCE.set(newLink, mEntryId);
                                }
                            }.start();
                        }).create();
                d.show();
                final TextView tv = d.findViewById(android.R.id.message);
                tv.setAutoLinkMask(Linkify.ALL);
                tv.setTextIsSelectable(true);
                break;
            }
            case R.id.menu_share_all_text: {
                Spanned spanned = Html.fromHtml(GetData());
                char[] chars = new char[spanned.length()];
                TextUtils.getChars(spanned, 0, spanned.length(), chars, 0);
                String plainText = new String(chars);
                plainText = plainText.replaceAll( "body(.)*", "" );
                getContext().startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_TEXT, plainText)
                                .setType(Constants.MIMETYPE_TEXT_PLAIN),
                        getContext().getString(R.string.menu_share)));
                break;
            }
            case R.id.menu_cancel_refresh: {
                FetcherService.cancelRefresh();
                break;
            }

            case R.id.menu_font_bold: {
                PrefUtils.toggleBoolean(PrefUtils.ENTRY_FONT_BOLD, false);
                item.setChecked( PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ) );
                update(true);
                break;
            }
            case R.id.menu_show_html: {
                showHTML();
                break;
            }
            case R.id.menu_edit_feed: {
                final String feedId = mEntryFragment.getCurrentFeedID();
                if (!feedId.isEmpty() && !feedId.equals(FetcherService.GetExtrenalLinkFeedID()))
                    getContext().startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedData.FeedColumns.CONTENT_URI(feedId)));
                break;
            }

            case R.id.menu_search_next: {
                mWebView.findNext( true );
                break;
            }

            case R.id.menu_search_previous: {
                mWebView.findNext( false );
                break;
            }
            case R.id.menu_share: {
                share();
                break;
            }
        }
    }

    @Override
    public void onPageSelected() {
        if ( mLoadTitleOnly )
            mEntryFragment.getLoaderManager().restartLoader(mPosition, null, mEntryFragment);
        else if ( mEntryFragment.mTapZones != null )
            DisableTapActionsIfVideo();
        mLoadTitleOnly = false;
    }

    private void share() {
        if (mEntryLink != null)
            share( getContext(), Uri.parse(mEntryLink), mCursor.getString(mTitlePos) );
    }

    private void showHTML() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;
        final String html = "<root>" + GetDataWithLinks() + "</root>";
        String htmlFormatted = NetworkUtils.formatXML( html );
        Uri fileUri = DebugApp.CreateFileUri(getContext().getCacheDir().getAbsolutePath(), "html.html", html);
        FileUtils.INSTANCE.copyFileToDownload( new File(getContext().getCacheDir().getAbsolutePath(), "html.html" ).getPath(), true );
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(fileUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            getContext().startActivity(intent);
        } catch ( ActivityNotFoundException ignored ) {
            MessageBox.Show(htmlFormatted);
        }
    }
    private void SetIsWithTables(final boolean withTables) {
        if ( mIsWithTables == withTables )
            return;
        mIsWithTables = withTables;
        final Uri uri = getUri();
        ContentValues values = new ContentValues();
        values.put(FeedData.EntryColumns.IS_WITH_TABLES, mIsWithTables? 1 : 0);
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.update(uri, values, null, null);
        mEntryFragment.getActivity().invalidateOptionsMenu();
    }
    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu( menu );

        setVisible( menu, R.id.menu_search );
        setVisible( menu, R.id.menu_reload_full_text_toolbar );
        setVisible( menu, R.id.menu_search_next );
        setVisible( menu, R.id.menu_search_previous );
        setVisible( menu, R.id.menu_go_back );
        setVisible( menu, R.id.menu_disable_all_tap_actions );
        setVisible( menu, R.id.menu_load_all_images );
        setVisible( menu, R.id.menu_load_all_images );
        setVisible( menu, R.id.menu_labels );
        setVisible( menu, R.id.menu_open_link );
        setVisible( menu, R.id.menu_reload_full_text );
        setVisible( menu, R.id.menu_reload_full_text_toolbar );
        setVisible( menu, R.id.menu_reload_full_text_without_mobilizer );
        setVisible( menu, R.id.menu_reload_with_tables_toggle );
        setVisible( menu, R.id.menu_replace_img_with_a_link_toggle );
        setVisible( menu, R.id.menu_reload_full_text_with_debug_toggle );
        setVisible( menu, R.id.menu_reload_full_text_with_tags );
        setVisible( menu, R.id.menu_reload_full_text_with_scripts );
        setVisible( menu, R.id.menu_edit_article_url );
        setVisible( menu, R.id.menu_share_all_text );
        setVisible( menu, R.id.menu_cancel_refresh );
        setVisible( menu, R.id.menu_font_bold );
        setVisible( menu, R.id.menu_show_html );
        setVisible( menu, R.id.menu_edit_feed );
        setVisible( menu, R.id.menu_search_next );
        setVisible( menu, R.id.menu_search_previous );
        setVisible( menu, R.id.menu_share );

        setItemChecked( menu, R.id.menu_font_bold, PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ));
        setItemChecked( menu, R.id.menu_reload_with_tables_toggle, mIsWithTables );
        setItemChecked( menu, R.id.menu_reload_full_text_with_debug_toggle, PrefUtils.getBoolean( STATE_RELOAD_WITH_DEBUG, false ) );
        setItemChecked( menu, R.id.menu_replace_img_with_a_link_toggle, PrefUtils.getBoolean( STATE_RELOAD_IMG_WITH_A_LINK, false ) );
    }

    @Override
    public void setupControlPanelButtonActions() {
        super.setupControlPanelButtonActions();
        setupButtonAction(R.id.btn_share, false, v -> {
            share();
        });

        setupButtonAction(R.id.btn_reload, false, v -> ReloadFullText());

    }

    @Override
    public void ScrollToPage(int page) {
        mScrollPartY = page / (double) mWebView.getPageCount();
        Log( "ScrollToPage mScrollPartY = " + mScrollPartY );
        mWebView.ScrollToY();
    }

    @Override
    public void update(boolean isGenerateArticleContent) {
        super.update(isGenerateArticleContent);
        mEntryFragment.mBtnEndEditing.setVisibility(mIsEditingMode ? View.VISIBLE : View.GONE);
    }
    public void onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mSearch.onCreateOptionsMenu( menu );
    }

    @Override
    public void update(Observable observable, Object o) {
        if ( observable == EntryUrlVoc.INSTANCE.getMObservable() ) {
            String url = (String)o;
            if ( mNotLoadedUrlSet.contains( url ) )
                UpdateImagesAndLinks(false);
        }
    }

    public void DisableTapActionsIfVideo() {
        if (mLoadTitleOnly)
            return;
        final boolean enabled;
        synchronized (this) {
            enabled = mIsFullTextShown ||
                    !PrefUtils.getBoolean("disable_tap_actions_when_video", true) ||
                    !hasVideo();
        }

        if (enabled != isArticleTapEnabledTemp()) {
            PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, enabled);
            if ( mEntryFragment.mTapZones != null )
                mEntryFragment.mTapZones.Update();
            Toast.makeText(MainApplication.getContext(),
                    enabled ?
                            MainApplication.getContext().getString(R.string.tap_actions_were_enabled) :
                            MainApplication.getContext().getString(R.string.video_tag_found_in_article) + ". " + getContext().getString(R.string.tap_actions_were_disabled),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void EndStatus() {
        synchronized (this) {
            if ( !mContentWasLoaded && !mLoadTitleOnly )
                return;
            super.EndStatus();
        }
    }

    @Override
    public void StatusStartPageLoading() {
        if ( !mLoadTitleOnly )
            super.StatusStartPageLoading();
    }
}
//