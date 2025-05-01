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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.activity.BaseActivity.PAGE_SCROLL_DURATION_MSEC;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.AUTO_SET_AS_READ;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.CategoriesToOutput;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.IsFeedUri;
import static ru.yanus171.feedexfork.parser.OPML.FILENAME_DATETIME_FORMAT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.service.FetcherService.EXTRA_LABEL_ID_LIST;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.IS_RSS;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.service.FetcherService.isLinkToLoad;
import static ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.AddTagButtons;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_CATEGORY;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_DATE;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_HIDDEN;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_FULL_TEXT_ROOT_CLASS;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_IFRAME;
import static ru.yanus171.feedexfork.utils.HtmlUtils.PATTERN_VIDEO;
import static ru.yanus171.feedexfork.utils.PrefUtils.ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR_BACKGROUND;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_BACKGROUND_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_LEFT_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_BORDER_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_COLOR;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetPackageNameForAction;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetShowInBrowserIntent;
import static ru.yanus171.feedexfork.view.FontSelectPreference.DefaultFontFamily;
import static ru.yanus171.feedexfork.view.FontSelectPreference.GetTypeFaceLocalUrl;
import static ru.yanus171.feedexfork.view.MenuItem.ShowMenu;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Observable;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
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
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class EntryView extends WebView implements Handler.Callback {

    private static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";
    public static final String TAG = "EntryView";
    private static final String NO_MENU = "NO_MENU_";
    public static final String BASE_URL = "";
    private static final int CLICK_ON_WEBVIEW = 1;
    private static final int CLICK_ON_URL = 2;
    private static final int TOGGLE_TAP_ZONE_VISIBIILTY = 3;
    public static final int TOUCH_PRESS_POS_DELTA = 5;
    public boolean mWasAutoUnStar = false;

    public long mEntryId = -1;
    public boolean mHasScripts = false;
    private String mEntryLink = "";
    public Runnable mScrollChangeListener = null;
    private int mLastContentLength = 0;
    private Stack<Integer> mHistoryAchorScrollY = new Stack<>();
    private final Handler mHandler = new Handler(this);
    private int mScrollY = 0;
    private int mStatus = 0;
    public boolean mLoadTitleOnly = false;
    public boolean mContentWasLoaded = false;
    private double mLastContentHeight = 0;
    private long mLastTimeScrolled = 0;
    public String mDataWithWebLinks = "";
    public boolean mIsEditingMode = false;
    public long mLastSetHTMLTime = 0;
    private ArrayList<String> mImagesToDl = new ArrayList<>();
    public String mTitle;
    private boolean mIsScrollScheduled = false;

    private static String GetCSS(final String text, final String url, boolean isEditingMode) {
        String mainFontLocalUrl = GetTypeFaceLocalUrl(PrefUtils.getString("fontFamily", DefaultFontFamily), isEditingMode);
        final CustomClassFontInfo customFontInfo = GetCustomClassAndFontName("font_rules", url);
        if ( !customFontInfo.mKeyword.isEmpty() && customFontInfo.mClassName.isEmpty() )
            mainFontLocalUrl = GetTypeFaceLocalUrl( customFontInfo.mFontName, isEditingMode );
        String mainFontSize = PrefUtils.getFontSizeText(0 );
        String textAlign = getAlign(text);
        return "<head><style type='text/css'> "
            + "@font-face { font-family:\"MainFont\"; src: url(\"" + mainFontLocalUrl + "\");" + "} \n"
            + "@font-face { font-family:\"CustomFont\"; src: url(\"" + GetTypeFaceLocalUrl(customFontInfo.mFontName, isEditingMode) + "\");}\n"
            + "body { font-family: \"MainFont\"; font-size: " + mainFontSize + "; text-align:" + textAlign + "; font-weight: " + getFontBold() + "; "
            + "font-size: " + mainFontSize + "; color: " + Theme.GetTextColor() + "; background-color:" + Theme.GetBackgroundColor() + "; "
            + "max-width: 100%; margin: " + getMargins() + "; " +  PrefUtils.getString( "main_font_css_text", "" ) + "}\n "
            + "* {word-break: break-word}\n"//+ "* {max-width: 100%; word-break: break-word}\n"
            + "title, h1, h2 {font-weight: normal; text-align:center; line-height: 120%}\n "
            + "title, h1 {font-size: " + PrefUtils.getFontSizeText(4 ) + "; margin-top: 1.0cm; margin-bottom: 0.1em}\n "
            + "h2 {font-size: " + PrefUtils.getFontSizeText(2 ) + "}\n "
            + "}body{color: #000; text-align: justify; background-color: #fff;}\n"
            + "a.no_draw_link {color: " + Theme.GetTextColor() + "; background: " + Theme.GetBackgroundColor() + "; text-decoration: none" + "}\n"
            + "a {color: " + Theme.GetColor(LINK_COLOR, R.string.default_link_color) + "; background: " + Theme.GetColor(LINK_COLOR_BACKGROUND, R.string.default_text_color_background) +
            (getBoolean("underline_links", true) ? "" : "; text-decoration: none") + "}\n"
            + "h1 {color: inherit; text-decoration: none}\n"
            + "img {display: inline;max-width: 100%;height: auto; " + (PrefUtils.isImageWhiteBackground() ? "background: white" : "") + "}\n"
            //+ ".inverted .math {color: white; background-color: black; } "
            + "iframe {allowfullscreen; position:relative;top:0;left:0;width:100%;height:100%;}\n"
            + "pre {white-space: pre-wrap;}\n "
            + "blockquote {border-left: thick solid " + Theme.GetColor(QUOTE_LEFT_COLOR, android.R.color.black) + "; background-color:" + Theme.GetColor(QUOTE_BACKGROUND_COLOR, android.R.color.black) + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em}\n "
            + "td {font-weight: " + getFontBold() + "; text-align:" + textAlign + "}\n "
            + "hr {width: 100%; color: #777777; align: center; size: 1}\n "
            + "p.button {text-align: center}\n "
            + "math {color: " + Theme.GetTextColor() + "; background-color:" + Theme.GetBackgroundColor() + "}\n "
            + "p {font-family: \"MainFont\"; font-size: " + mainFontSize + "; margin: 0.8em 0 0.8em 0; text-align:" + textAlign + "}\n "
            + getCustomFontClassStyle("p", url, customFontInfo)
            + getCustomFontClassStyle("span", url, customFontInfo)
            + getCustomFontClassStyle("div", url, customFontInfo)
            + "p.subtitle { text-align: center; color: " + Theme.GetColor(SUBTITLE_COLOR, android.R.color.black) + "; border-top:1px " + Theme.GetColor(SUBTITLE_BORDER_COLOR, android.R.color.black) + "; border-bottom:1px " + Theme.GetColor(SUBTITLE_BORDER_COLOR, android.R.color.black) + "; padding-top:2px; padding-bottom:2px; font-weight:800 }\n "
            + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em}\n "
            + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0}\n "
            + "div.bottom-page {display: block; min-height: 80vh}\n "
            + "div.button-section {padding: 0.8cm 0; margin: 0; text-align: center}\n "
            + "div {text-align:" + textAlign + "}\n "
            + "div.toc {text-align: center}\n "
            + "p.toc {text-align: center}\n "
            //+ "* { -webkit-tap-highlight-color: rgba(" + Theme.GetToolBarColorRGBA() + "); } "
            + ".categories {font-style: italic; color: " + Theme.GetColor(SUBTITLE_COLOR, android.R.color.black) + "}\n "
            + ".button-section p {font-family: \"MainFont\"; font-size: " + mainFontSize + "; margin: 0.1cm 0 0.2cm 0}\n "
            + ".button-section p.marginfix {margin: 0.2cm 0 0.2cm 0}\n"
            + ".button-section input, .button-section a {font-family: \"MainFont\"; font-size: " + mainFontSize + "; color: #FFFFFF; background-color: " + Theme.GetToolBarColor() + "; text-decoration: none; border: none; border-radius:0.2cm; margin: 0.2cm}\n "
            + "." + TAG_BUTTON_CLASS + ", ." + TAG_BUTTON_CLASS_CATEGORY + ", ." + TAG_BUTTON_CLASS_DATE + ", ." + TAG_BUTTON_CLASS_CATEGORY + ", ." + TAG_BUTTON_FULL_TEXT_ROOT_CLASS + ", ." + TAG_BUTTON_CLASS_HIDDEN
            + " { font-size: " + mainFontSize + "; color: #FFFFFF; background-color: " + Theme.GetToolBarColor() + "; text-decoration: none; border: none; border-radius:0.2cm;  margin-right: 0.2cm; padding-top: 0.0cm; padding-bottom: 0.0cm; padding-left: 0.2cm; padding-right: 0.2cm}\n "
            + "." + TAG_BUTTON_CLASS + " i { background-color: " + Theme.GetToolBarColor() + "}\n "
            + "." + TAG_BUTTON_CLASS_CATEGORY + " i {background-color: #00AAAA}\n "
            + "." + TAG_BUTTON_CLASS_DATE + " i {background-color: #0000AA}\n "
            + "." + TAG_BUTTON_FULL_TEXT_ROOT_CLASS + " i {background-color: #00AA00}\n "
            + "." + TAG_BUTTON_CLASS_HIDDEN + " i {background-color: #888888}\n "
            + PrefUtils.getString( "custom_css_text", "" )
            + "</style><meta name='viewport' content='width=device-width'/></head>";
    }

    @NotNull
    private static String getCustomFontClassStyle(String tag, String url, CustomClassFontInfo info) {
        if ( info.mKeyword.isEmpty() )
            return "";
        else
            return tag + ( info.mClassName.isEmpty() ? "" : "." + info.mClassName)
                       +   "{font-family: \"CustomFont\"; " + info.mStyleText + "} ";
    }

    static class CustomClassFontInfo {
        float mLetterSpacing = 0.01F;
        String mClassName = "";
        String mKeyword = "";
        String mFontName = "";
        String mStyleText = "";
        CustomClassFontInfo( String line ) {
            String[] list1 = line.split(":");
            if ( list1.length >= 1 ) {
                mKeyword = list1[0];
                if (list1.length >= 2) {
                    String[] list2 = list1[1].split("=");
                    if (list2.length >= 2) {
                        mClassName = list2[0].toLowerCase();
                        mFontName = list2[1];
                    }
                    if (list2.length >= 3)
                        mStyleText = list2[2];
                }
            }
            for ( int i = 0; i < list1.length; i++ )
                if (i >= 2)
                    mStyleText += ":" + list1[i];
        }

    }
    private static CustomClassFontInfo GetCustomClassAndFontName(final String key, final String url ) {
        final String pref = PrefUtils.getString( key, "" );
        for( String line: pref.split( "\\n" ) ) {
            if ((line == null) || line.isEmpty())
                continue;
            try {
                CustomClassFontInfo info = new CustomClassFontInfo( line );
                if (url.contains(info.mKeyword)) {
                    return info;
                }
            } catch (Exception e) {
                Dog.e(e.getMessage());
            }
        }
        return new CustomClassFontInfo("");
    }


    private static String getFontBold() {
        if (getBoolean(PrefUtils.ENTRY_FONT_BOLD, false))
            return "bold;";
        else
            return "normal;";
    }

    private static String getMargins() {
        if (getBoolean(PrefUtils.ENTRY_MAGRINS, true))
            return "4%";
        else
            return "0.1cm";
    }

    public static String getAlign(String text) {
        if (isTextRTL(text))
            return "right";
        else if (getBoolean(PrefUtils.ENTRY_TEXT_ALIGN_JUSTIFY, false))
            return "justify";
        else
            return "left";
    }

    private static boolean isWordRTL(String s) {
        if (s.isEmpty()) {
            return false;
        }
        char c = s.charAt(0);
        return c >= 0x590 && c <= 0x6ff;
    }

    public static boolean isTextRTL(String text) {
        if ( text == null )
            return false;
        String[] list = TextUtils.split(text, " ");
        for (String item : list)
            if (isWordRTL(item))
                return true;
        return false;
    }

    public static boolean isRTL(String text) {
        final int directionality = Character.getDirectionality(Locale.getDefault().getDisplayName().charAt(0));
        return (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) && isTextRTL(text);
    }

    private static final String BODY_START = "<body dir=\"%s\">";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START_WITH_LINK = "<h1><a class='no_draw_link' href=\"%s\">";
    private static final String TITLE_START = "<h1>";
    //private static final String TITLE_MIDDLE = "'>";
    private static final String TITLE_END = "</h1>";
    private static final String TITLE_END_WITH_LINK = "</a></h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    private static final String CATEGORIES_START = "<p class='categories'>";
    private static final String CATEGORIES_END = "</p>";
    private static final String BUTTON_SECTION_START = "<div class='button-section'>";
    private static final String BOTTOM_PAGE = "<div class='bottom-page'/>";
    private static final String BUTTON_SECTION_END = "</div>";
    private static String BUTTON_START( String layout ) {
        return ( layout.equals( ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL ) ? "" : "<p class='button'>" ) + "<input type='button' value='";
    }
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static String BUTTON_END( String layout ) {
        return "'/>" + (layout.equals(ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL) ? "" : "</p>");
    }
    // the separate 'marginfix' selector in the following is only needed because the CSS box model treats <input> and <a> elements differently
    private static final String LINK_BUTTON_START = "<p class='marginfix'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "'>";
    private static final String LINK_BUTTON_END = "</a></p>";
    private static final String IMAGE_ENCLOSURE = "[@]image/";
    private static final long TAP_TIMEOUT = 300;
    private static final long MOVE_TIMEOUT = 800;

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();
    private final ImageDownloadJavaScriptObject mImageDownloadObject = new ImageDownloadJavaScriptObject();
    public static final ImageDownloadObservable mImageDownloadObservable = new ImageDownloadObservable();
    private EntryViewManager mEntryViewMgr;
    String mData = "";
    public double mScrollPartY = 0;
    boolean mIsAutoMarkVisibleAsRead = false;
    private EntryActivity mActivity;
    private long mPressedTime = 0;
    private long mMovedTime = 0;



    public EntryView(Context context) {
        super(context);
        init();
    }

    public EntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setListener(EntryViewManager manager) {
        mEntryViewMgr = manager;
    }

    @SuppressLint("Range")
    public boolean setHtml(final long entryId,
                           Uri articleListUri,
                           Cursor newCursor,
                           FeedFilters filters,
                           boolean isFullTextShown,
                           boolean forceUpdate,
                           EntryActivity activity) {
        Timer timer = new Timer("EntryView.setHtml");
        mLastSetHTMLTime = new Date().getTime();

        mEntryId = entryId;
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
            contentText = getContext().getString(R.string.loading);
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

        mActivity = activity;
        if (!mLoadTitleOnly && contentText.length() == mLastContentLength ) {
            EndStatus();
            return isFullTextShown;
        }
        mLastContentLength = contentText.length();
        //getSettings().setBlockNetworkLoads(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setSupportZoom(false);
        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor()));
        // Text zoom level from preferences
        //int fontSize = PrefUtils.getFontSize();
        //if (fontSize != 0) {
            getSettings().setTextZoom(100);
        //}

        final String finalContentText = contentText;
        final boolean finalIsFullTextShown = isFullTextShown;
        final boolean finalHasOriginal = hasOriginal;
        final String finalTitle = title;

        new Thread() {
            @Override
            public void run() {
                final String dataWithLinks = generateHtmlContent(feedID, articleListUri, finalTitle, mEntryLink, finalContentText, categories, enclosure, author, timestamp, finalIsFullTextShown, finalHasOriginal);
                final ArrayList<String> imagesToDl = new ArrayList<>();
                final String data = HtmlUtils.replaceImageURLs( dataWithLinks, "", mEntryId, mEntryLink, false, imagesToDl, null, mMaxImageDownloadCount );
                synchronized (EntryView.this) {
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

    public void InvalidateContentCache() {
        mLastContentLength = 0;
    }

    private String generateHtmlContent(String feedID, Uri articleListUri, String title, String link, String contentText, String categories,
                                       String enclosure, String author,
                                       long timestamp, boolean canSwitchToFullText, boolean hasOriginalText) {
        Timer timer = new Timer("EntryView.generateHtmlContent");

        StringBuilder content = new StringBuilder(GetCSS(title, link, mIsEditingMode))
            .append(String.format(BODY_START, isTextRTL(title) ? "rtl" : "inherit"));

        if (link == null)
            link = "";

        if (getBoolean("entry_text_title_link", true))
            content.append(String.format(TITLE_START_WITH_LINK, link + NO_MENU)).append(title).append(TITLE_END_WITH_LINK);
        else
            content.append(TITLE_START).append(title).append(TITLE_END);

        content.append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));
        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }
        content.append(dateStringBuilder);
        content.append(SUBTITLE_END);
        if ( !feedID.equals( -1 ) && articleListUri != Uri.EMPTY && !IsFeedUri(articleListUri) ) {
            content.append(SUBTITLE_START);
            Cursor cursor = getContext().getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedID), new String[]{FeedData.FeedColumns.NAME}, null, null, null );
            cursor.moveToFirst();
            content.append( cursor.getString( 0 ) );
            cursor.close();
            content.append(SUBTITLE_END);
        }

        if (categories != null && !categories.isEmpty())
            content.append( CATEGORIES_START ).append( CategoriesToOutput( categories ) ).append( CATEGORIES_END );

        content.append(contentText);


        final String layout = PrefUtils.getString( "setting_article_text_buttons_layout", ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL );
        if ( !layout.equals( "Hidden" ) ) {
            content.append(BUTTON_SECTION_START);
            if (!feedID.equals(FetcherService.GetExtrenalLinkFeedID())) {

                if (!canSwitchToFullText) {
                    content.append(BUTTON_START(layout));
                    content.append(context.getString(R.string.get_full_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickFullText();");
                    content.append(BUTTON_END(layout));
                } else if (hasOriginalText) {
                    content.append(BUTTON_START(layout));
                    content.append(context.getString(R.string.original_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickOriginalText();");
                    content.append(BUTTON_END(layout));
                }

            }

            if (canSwitchToFullText)
                content.append(BUTTON_START(layout)).append(context.getString(R.string.btn_reload_full_text)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onReloadFullText();").append(BUTTON_END(layout));
            if (enclosure != null && enclosure.length() > 6 && !enclosure.contains(IMAGE_ENCLOSURE)) {
                content.append(BUTTON_START(layout)).append(context.getString(R.string.see_enclosure)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onClickEnclosure();").append(BUTTON_END(layout));
            }
            content.append(BUTTON_START(layout)).append(context.getString(R.string.menu_go_back)).append(BUTTON_MIDDLE)
                .append("injectedJSObject.onClose();").append(BUTTON_END(layout));
            /*if (link.length() > 0) {
                content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(context.getString(R.string.see_link)).append(LINK_BUTTON_END);
            }*/

            content.append(BUTTON_SECTION_END).append(BOTTOM_PAGE).append(BODY_END);
        }

        timer.End();
        return content.toString();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {

        StatusStartPageLoading();
        setBackgroundColor(Color.parseColor(Theme.GetBackgroundColor()));

        Timer timer = new Timer("EntryView.init");

            getSettings().setAllowFileAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            setTextDirection(TEXT_DIRECTION_LOCALE);
        // For scrolling
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(true);
        getSettings().setUseWideViewPort(true);
        // For color

        // For javascript
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());
        addJavascriptInterface(mImageDownloadObject, mImageDownloadObject.toString());
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        //    setNestedScrollingEnabled(true);
        // For HTML5 video
        setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // if a view already exists then immediately terminate the new one
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                FrameLayout videoLayout = mEntryViewMgr.getVideoLayout();
                if (videoLayout != null) {
                    mCustomView = view;

                    setVisibility(View.GONE);
                    videoLayout.setVisibility(View.VISIBLE);
                    videoLayout.addView(view);
                    mCustomViewCallback = callback;

                    mEntryViewMgr.onStartVideoFullScreen();
                }
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();

                if (mCustomView == null) {
                    return;
                }

                FrameLayout videoLayout = mEntryViewMgr.getVideoLayout();
                if (videoLayout != null) {
                    setVisibility(View.VISIBLE);
                    videoLayout.setVisibility(View.GONE);

                    // HideByScroll the custom view.
                    mCustomView.setVisibility(View.GONE);

                    // Remove the custom view from its container.
                    videoLayout.removeView(mCustomView);
                    mCustomViewCallback.onCustomViewHidden();

                    mCustomView = null;

                    mEntryViewMgr.onEndVideoFullScreen();
                }
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                Bitmap result = super.getDefaultVideoPoster();
                if (result == null)
                    result = BitmapFactory.decodeResource(MainApplication.getContext().getResources(), android.R.drawable.presence_video_online);
                return result;
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if ( mContentWasLoaded )
                    return;
                Status().ChangeProgress( String.format( "%d %% ...", progress ) );
            }


        });

        setOnLongClickListener(view -> {
            HitTestResult hitTestResult = ((WebView) view).getHitTestResult();
            if (hitTestResult.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE ) {
                String link = hitTestResult.getExtra();
                ShowImageMenu( link, mTitle, view.getContext() );
                return true;
            }
            return false;
        });
        setWebViewClient( new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, final String url) {
                DoNotShowMenu(true);
                if ( System.currentTimeMillis() - mMovedTime < MOVE_TIMEOUT  )
                    return true;

                final Context context = getContext();
                try {
                    String anchor = "";
                    if ( url.contains("#") ) {
                        anchor = url.substring(url.indexOf('#') + 1);
                        anchor = URLDecoder.decode(anchor);
                    }

                    if (url.startsWith(Constants.FILE_SCHEME))
                        OpenImage(url, context);
                    else if (!anchor.isEmpty() && url.replace( "#" + anchor, "" ).equals( mEntryLink ) || !url.contains( "http" ) ) {
                        if ( anchor.isEmpty() )
                            ScrollTo( 0 );
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            moveToAnchor(view, anchor);
                            AddNavigationHistoryStep();
                        }
                    } else if (url.contains(NO_MENU)) {
                        getContext().startActivity(GetShowInBrowserIntent(url.replace(NO_MENU, "")));
                    } else {
                        final String urlWithoutRegexSymbols =
                            url.replace( "/", "." ).
                                replace( ":", "." ).
                                replace( "?", "." ).
                                replace( "+", "." ).
                                replace( "&", "&amp;" ).
                                replace( "*", "." ).
                                replace( ",", "." );
                        final Pattern REGEX = Pattern.compile("<a[^>]+?href=.url.+?>(.+?)</a>".replace("url", urlWithoutRegexSymbols ), Pattern.CASE_INSENSITIVE);
                        Matcher matcher = REGEX.matcher(mData);
                        String title = matcher.find() ? Jsoup.parse(matcher.group(1 ) ).text() : url;
                        title = url.equals( title )  ? "" : title;

                        ShowLinkMenu(url, title, context);
                    }
                 } catch ( ActivityNotFoundException e ) {
                     Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show();
                 }
                 return true;
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Status().ChangeProgress( "started..." );
                mContentWasLoaded = false;
                StatusStartPageLoading();
                super.onPageStarted( view, url, favicon );
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished( view, url );
                //Dog.v( "EntryView.onPageFinished url = " + url );
                if ( url.equals( "about:blank" ) ) {
                    Status().ChangeProgress("finished.");
                    if (!mLoadTitleOnly)
                        mContentWasLoaded = true;
                    if (mActivity.mEntryFragment != null)
                        mActivity.mEntryFragment.DisableTapActionsIfVideo(EntryView.this);
                    if ( !mIsScrollScheduled ) {
                        if (mContentWasLoaded)
                            DownLoadImages();
                        ScheduleScrollTo(view, new Date().getTime());
                    }
                } else
                    DoNotShowMenu(false);
            }

            private void ScheduleScrollTo(final WebView view, long startTime) {
                //Dog.v(TAG, "ScheduleScrollTo() mEntryID = " + mEntryId + ", mScrollPartY=" + mScrollPartY + ", GetScrollY() = " + GetScrollY() + ", GetContentHeight()=" + GetContentHeight() );
                double newContentHeight = GetContentHeight();
                final String searchText = mActivity.getIntent().getStringExtra( "SCROLL_TEXT" );
                final boolean isSearch = searchText != null && !searchText.isEmpty();
                if ( !mIsScrollScheduled && newContentHeight > 0 && newContentHeight == mLastContentHeight) {
                    if ( isSearch ) {
                        //Dog.v(TAG, "ScheduleScrollTo() isSearchText" );
                        UiUtils.RunOnGuiThread(() -> {
                            if (Build.VERSION.SDK_INT >= 16)
                                findAllAsync(searchText);
                            else
                                findAll(searchText);

                        } );
                        UiUtils.RunOnGuiThread(view::clearMatches, 5000 );
                    } else
                        UiUtils.RunOnGuiThread(() ->
                           {
                               if (mActivity.mEntryFragment != null)
                                   mActivity.mEntryFragment.UpdateHeader();
                               if ( mActivity.mEntryFragment != null && !mActivity.mEntryFragment.mAnchor.isEmpty() )
                                   moveToAnchor( view, mActivity.mEntryFragment.mAnchor );
                               else
                                   ScrollToY();
                           });
                    if ( new Date().getTime() - startTime < 1000 )
                        PostDelayed( view, startTime );
                    EndStatus();
                } else
                    PostDelayed( view, startTime );
                mLastContentHeight = newContentHeight;
            }

            void PostDelayed( final WebView view, long startTime ) {
                if ( !mIsScrollScheduled ) {
                    mIsScrollScheduled = true;
                    view.postDelayed(() -> {
                        mIsScrollScheduled = false;
                        ScheduleScrollTo(view, startTime);
                    }, 350);
                }
            }
        });

        setOnTouchListener(new OnTouchListener() {
            private float mPressedY;
            private float mPressedX;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ( mActivity == null || mActivity.mEntryFragment == null )
                    return false;
                mActivity.mEntryFragment.mAnchor = "";
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mPressedX = event.getX();
                    mPressedY = event.getY();
                    mPressedTime = System.currentTimeMillis();
                    //Log.v( TAG, "ACTION_DOWN mPressedTime=" + mPressedTime );
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getX() - mPressedX) > TOUCH_PRESS_POS_DELTA ||
                            Math.abs(event.getY() - mPressedY) > TOUCH_PRESS_POS_DELTA) {
                        mPressedTime = 0;
                        mMovedTime = System.currentTimeMillis();
                    }
//                    final int scrollDelta = (int) (event.getY() - mPressedY);
//                    if ( Math.abs( scrollDelta ) > 50  ) {
////                        if ( scrollDelta > 0)
////                            mActivity.getSupportActionBar().show();
////                        else
////                            mActivity.mToolbar.hide();
//                        mActivity.mToolbar.getLayoutParams().height = scrollDelta ;
//                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //Log.v( TAG, "ACTION_DOWN time delta=" + ( System.currentTimeMillis() - mPressedTime ) );
                    if ( System.currentTimeMillis() - mPressedTime < TAP_TIMEOUT &&
                            Math.abs(event.getX() - mPressedX) < TOUCH_PRESS_POS_DELTA &&
                            Math.abs(event.getY() - mPressedY) < TOUCH_PRESS_POS_DELTA &&
                            System.currentTimeMillis() - mLastTimeScrolled > 500 &&
                            isArticleTapEnabledTemp() &&
                            //EntryActivity.GetIsActionBarHidden() &&
                            !mActivity.mHasSelection) {
                        //final HitTestResult hr = getHitTestResult();
                        //Log.v( TAG, "HitTestResult type=" + hr.getType() + ", extra=" + hr.getExtra()  );
                        mHandler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 0);
                        mHandler.sendEmptyMessageDelayed(TOGGLE_TAP_ZONE_VISIBIILTY, 150);
                    }
                }
                return false;
            }
        });

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            postVisualStateCallback(1, new VisualStateCallback() {
//                @Override
//                public void onComplete(long l) {
//                    Dog.v( TAG, "EntriView.postVisualStateCallback.onComplete" );
//                }
//            });
//        }
        //setNestedScrollingEnabled( true );
        timer.End();
    }

    private void moveToAnchor(WebView view, String hash) {
        Dog.v( TAG, "EntryView.moveToAnchor " + hash );
        view.evaluateJavascript("javascript:window.location.hash = '" + hash + "';", null);
        mScrollY = 0;
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

    public static String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
    @SuppressLint("SimpleDateFormat")
    public static String getDestFileName(String title) {
        return sanitizeFilename( title ) + "_" + new SimpleDateFormat(FILENAME_DATETIME_FORMAT).format(new Date());
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



    private void DownLoadImages() {
        final ArrayList<String> imagesToDl = GetImageListCopy();
        if ( !imagesToDl.isEmpty() )
            new Thread(() -> {
                FetcherService.downloadEntryImages("", mEntryId, mEntryLink, imagesToDl);
                ClearImageList();
            }).start();
    }

    private void ClearImageList() {
        synchronized ( EntryView.this ) {
            mImagesToDl.clear();
        }
    }

    @NotNull
    private ArrayList<String> GetImageListCopy() {
        final ArrayList<String> imagesToDl;
        synchronized ( EntryView.this ) {
            imagesToDl = (ArrayList<String>) mImagesToDl.clone();
        }
        return imagesToDl;
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

    private void EndStatus() {
        synchronized (EntryView.this) {
            if ( !mContentWasLoaded && !mLoadTitleOnly )
                return;
            if (mStatus != 0)
                Status().End(mStatus);
            mStatus = 0;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == CLICK_ON_URL) {
            mHandler.removeMessages(CLICK_ON_WEBVIEW);
            mActivity.closeContextMenu();
            return true;
        }
        if ( msg.what == TOGGLE_TAP_ZONE_VISIBIILTY ) {
            toggleTapZoneVisibility();
            return true;
        }
        if (msg.what == CLICK_ON_WEBVIEW)
            return true;
        return false;
    }

    private void toggleTapZoneVisibility() {
        if ( mActivity != null && mActivity.mEntryFragment != null )
            mActivity.mEntryFragment.toggleTapZoneVisibility();
    }

    private void ScrollToY() {
        Dog.v(TAG, "EntryView.ScrollToY() mEntryID = " + mEntryId + ", mScrollPartY=" + mScrollPartY + ", GetScrollY() = " + GetScrollY());
        if (GetScrollY() > 0)
            EntryView.this.scrollTo(0, GetScrollY());
    }


    /*@Override
    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if ( scrollY > 50 && clampedY ) {
            mActivity.setFullScreen(false, EntryActivity.GetIsActionBarHidden());
        }

    }*/

    public String GetData() {
        synchronized (EntryView.this) {
            return mData;
        }
    }

    public String GetDataWithLinks() {
        synchronized (EntryView.this) {
            return mDataWithWebLinks;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        Status().HideByScroll();
        //int contentHeight = (int) Math.floor(GetContentHeight());
        //int webViewHeight = getMeasuredHeight();
        if ( mActivity != null && mActivity.mEntryFragment != null )
            mActivity.mEntryFragment.UpdateHeader();
        mLastTimeScrolled = System.currentTimeMillis();
        if (mScrollChangeListener != null)
            mScrollChangeListener.run();
    }

    public boolean IsScrollAtBottom() {
        return getScrollY() + getMeasuredHeight() >= (int) Math.floor(GetContentHeight()) - getMeasuredHeight() * 0.4;
    }

    public void UpdateImages( final boolean downloadImages ) {
        if ( !downloadImages )
            StatusStartPageLoading();
        Dog.v( TAG, "UpdateImages" );
        new Thread() {
            @Override
            public void run() {
                final String data = HtmlUtils.replaceImageURLs( mDataWithWebLinks, mEntryId, mEntryLink, downloadImages);
                synchronized (EntryView.this) {
                    mData = data;
                }
                UiUtils.RunOnGuiThread(() -> {
                    if ( !IsStatusStartPageLoading() )
                        mScrollY = getScrollY();
                    if ( !downloadImages )
                        LoadData();
                });
            }
        }.start();
    }

    public void UpdateTags() {
        final int status = Status().Start(getContext().getString(R.string.last_update), true);
        Document doc = Jsoup.parse(ArticleTextExtractor.mLastLoadedAllDoc, NetworkUtils.getUrlDomain(mEntryLink));
        AddTagButtons(doc, mEntryLink);
        final String data = generateHtmlContent("-1", Uri.EMPTY,"", mEntryLink, doc.toString(), "", "", "", 0, true, false);
        synchronized (EntryView.this) {
            mData = data;
        }
        mScrollY = getScrollY();
        LoadData();
        Status().End(status);
    }

    private void LoadData() {
        Dog.v( TAG, "LoadDate" );
        if ( mContentWasLoaded && GetViewScrollPartY() > 0 )
            mScrollPartY = GetViewScrollPartY();
        //StatusStartPageLoading();
        final String data;
        synchronized (EntryView.this) {
            data = mData;
        }
        mLastContentHeight = 0;
        loadDataWithBaseURL(BASE_URL, data, TEXT_HTML, Constants.UTF8, null);
    }

    public void StatusStartPageLoading() {
        synchronized (EntryView.this) {
            if (mStatus == 0)
                mStatus = Status().Start(R.string.web_page_loading, true);
        }
    }
    private boolean IsStatusStartPageLoading() {
        synchronized (EntryView.this) {
            return mStatus == 0;
        }
    }

    static int NOTIFY_OBSERVERS_DELAY_MS = 1000;

    public static void NotifyToUpdate(final long entryId, final String entryLink, final boolean restorePosition) {

        UiUtils.RunOnGuiThread(new Runnable() {
            @Override
            public void run() {
                Dog.v( TAG, String.format( "NotifyToUpdate( %d )", entryId ) );
                EntryView.mImageDownloadObservable.notifyObservers(new Entry(entryId, entryLink, restorePosition) );//ScheduledNotifyObservers(entryId, entryLink);
            }
        }, 0 );//NOTIFY_OBSERVERS_DELAY_MS);
    }

    static HashMap<Long, Long> mLastNotifyObserversTime = new HashMap<>();
    static HashMap<Long, Boolean> mLastNotifyObserversScheduled = new HashMap<>();

    static void ScheduledNotifyObservers(final long entryId, final String entryLink) {
        mLastNotifyObserversTime.put(entryId, new Date().getTime());
        if (!mLastNotifyObserversScheduled.containsKey(entryId)) {
            mLastNotifyObserversScheduled.put(entryId, true);
            UiUtils.RunOnGuiThread(new ScheduledEntryNotifyObservers(entryId, entryLink), NOTIFY_OBSERVERS_DELAY_MS);
        }
    }

    public void ClearHistoryAnchor() {
        mHistoryAchorScrollY.clear();
        mActivity.mEntryFragment.SetupZones();
    }
    public boolean CanGoBack() {
        return !mHistoryAchorScrollY.isEmpty();
    }

    public void GoBack() {
        if (CanGoBack())
            scrollTo(0, mHistoryAchorScrollY.pop());
        mActivity.mEntryFragment.SetupZones();
    }

    public void GoTop() {
        AddNavigationHistoryStep();
        scrollTo(0, 0);
    }

    public void AddNavigationHistoryStep() {
        mHistoryAchorScrollY.push(getScrollY());
        mActivity.mEntryFragment.SetupZones();
    }


    public interface EntryViewManager {
        void onClickOriginalText();

        void onClickFullText();

        void onClickEnclosure();

        void onReloadFullText();

        void onClose();

        void onStartVideoFullScreen();

        void onEndVideoFullScreen();

        FrameLayout getVideoLayout();

        void downloadImage(String url);

        void openTagMenu(String className, String baseUrl, String paramValue);

        void downloadNextImages();

        void downloadAllImages();
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickOriginalText() {
            DoNotShowMenu(true);
            mEntryViewMgr.onClickOriginalText();
        }

        @JavascriptInterface
        public void onClickFullText() {
            DoNotShowMenu(true);
            mEntryViewMgr.onClickFullText();
        }

        @JavascriptInterface
        public void onClickEnclosure() {
            DoNotShowMenu(true);
            mEntryViewMgr.onClickEnclosure();
        }

        @JavascriptInterface
        public void onReloadFullText() {
            DoNotShowMenu(true);
            mEntryViewMgr.onReloadFullText();
        }

        @JavascriptInterface
        public void onClose() {
            DoNotShowMenu(false);
            mEntryViewMgr.onClose();
        }
    }

    private void DoNotShowMenu( boolean hideTapZones  ) {
        mHandler.sendEmptyMessage(CLICK_ON_URL);
        if ( hideTapZones )
            mHandler.removeMessages( TOGGLE_TAP_ZONE_VISIBIILTY );
        mActivity.closeOptionsMenu();
    }

    private class ImageDownloadJavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "ImageDownloadJavaScriptObject";
        }

        @JavascriptInterface
        public void downloadImage(String url) {
            DoNotShowMenu(true);
            mEntryViewMgr.downloadImage(url);
        }

        @JavascriptInterface
        public void downloadNextImages() {
            DoNotShowMenu(true);
            mEntryViewMgr.downloadNextImages();

        }

        @JavascriptInterface
        public void downloadAllImages() {
            DoNotShowMenu(true);
            mEntryViewMgr.downloadAllImages();

        }

        @JavascriptInterface
        public void openTagMenu(String className, String baseUrl, String paramValue) {
            DoNotShowMenu(true);
            mEntryViewMgr.openTagMenu(className, baseUrl, paramValue);
        }
    }

    public static class ImageDownloadObservable extends Observable {
        @Override
        public boolean hasChanged() {
            return true;
        }
    }


    private int GetScrollY() {
        if (mScrollY != 0)
            return mScrollY;
        return GetContentHeight() * mScrollPartY != 0 ? (int) (GetContentHeight() * mScrollPartY) : 0;
    }

    public double GetContentHeight() {
        return getContentHeight() * getScale();
    }

    public double GetViewScrollPartY() {
        return getContentHeight() != 0 ? getScrollY() / GetContentHeight() : 0;
    }

    public void ScrollTo(int y) {
        ObjectAnimator anim = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), y);
        anim.setDuration(PAGE_SCROLL_DURATION_MSEC);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }
    public void PageChange(int delta, StatusText statusText) {
        ScrollTo((int) (getScrollY() + delta * (getHeight() - statusText.GetHeight()) *
                                                       (getBoolean("page_up_down_90_pct", false) ? 0.9 : 0.98)));
        //if ( delta > 0 )
        //    ( (AppBarLayout) findViewById(R.id.appbar) ).setExpanded(false );
    }

    @Override
    public void onPause() {
        super.onPause();
        Dog.d( "WebView.onPause " + mEntryId );
        SaveScrollPos();
        EndStatus();
    }

    public void SaveScrollPos() {
        if ( !mContentWasLoaded )
            return;
        mScrollPartY = GetViewScrollPartY();
        if ( mScrollPartY > 0.0001 ) {
            //Dog.v(TAG, String.format("EnrtyView.SaveScrollPos (entry %d) mScrollPartY = %f getScrollY() = %d, view.getContentHeight() = %f", mEntryId, mScrollPartY, getScrollY(), GetContentHeight()));
//            new Thread() {
//                @Override
//                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(FeedData.EntryColumns.SCROLL_POS, mScrollPartY);
                    //if ( !mIsAutoMarkVisibleAsRead )
                    //    values.put(FeedData.EntryColumns.READ_DATE, new Date().getTime());
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    SetNotifyEnabled(false ); try {
                        //String where = FeedData.EntryColumns.SCROLL_POS + " < " + scrollPart + Constants.DB_OR + FeedData.EntryColumns.SCROLL_POS + Constants.DB_IS_NULL;
                        cr.update(FeedData.EntryColumns.CONTENT_URI(mEntryId), values, null, null);
                    } finally {
                        SetNotifyEnabled( true );
                    }
                    //Dog.v(TAG, String.format("SaveScrollPos (entry %d) update scrollPos = %f", mEntryId, mScrollPartY));
//                }
//            }.start();
        }
    }
    public boolean hasVideo() {
        return PATTERN_VIDEO.matcher(mDataWithWebLinks).find() ||
            PATTERN_IFRAME.matcher(mDataWithWebLinks).find();
    }
}

class ScheduledEntryNotifyObservers implements Runnable {
    private final String mLink;
    private long mId = 0;

    public ScheduledEntryNotifyObservers(long id, String link ) {
        mId = id;
        mLink = link;
    }

    @Override
    public void run() {
        EntryView.mLastNotifyObserversScheduled.remove( mId );
        //Dog.v( EntryView.TAG,"ScheduledNotifyObservers() run");
        if (new Date().getTime() - EntryView.mLastNotifyObserversTime.get( mId ) > EntryView.NOTIFY_OBSERVERS_DELAY_MS)
            EntryView.mImageDownloadObservable.notifyObservers(new Entry(mId, mLink, false) );
        else
            EntryView.ScheduledNotifyObservers( mId, mLink );
    }
}

