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
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.CategoriesToOutput;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.IsFeedUri;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_CATEGORY;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_DATE;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_HIDDEN;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_FULL_TEXT_ROOT_CLASS;
import static ru.yanus171.feedexfork.utils.PrefUtils.ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR_BACKGROUND;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_BACKGROUND_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_LEFT_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_BORDER_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_COLOR;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetShowInBrowserIntent;
import static ru.yanus171.feedexfork.view.EntryView.TAP_TIMEOUT;
import static ru.yanus171.feedexfork.view.FontSelectPreference.DefaultFontFamily;
import static ru.yanus171.feedexfork.view.FontSelectPreference.GetTypeFaceLocalUrl;
import static ru.yanus171.feedexfork.view.WebEntryView.OpenImage;
import static ru.yanus171.feedexfork.view.WebEntryView.ShowImageMenu;
import static ru.yanus171.feedexfork.view.WebEntryView.ShowLinkMenu;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class WebViewExtended extends WebView implements Handler.Callback {

    static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";
    private static final String NO_MENU = "NO_MENU_";
    public static final String BASE_URL = "";
    private static final int CLICK_ON_WEBVIEW = 1;
    private static final int CLICK_ON_URL = 2;
    private static final int TOGGLE_TAP_ZONE_VISIBIILTY = 3;
    public static final int TOUCH_PRESS_POS_DELTA = 5;

    private final Handler mHandler = new Handler(this);
    public double mLastContentHeight = 0;
    private long mLastTimeScrolled = 0;
    private boolean mIsScrollScheduled = false;
    public WebEntryView mEntryView = null;
    public Runnable mScrollChangeListener = null;

    public WebViewExtended( Context context, WebEntryView entryView ) {
        super(context);
        mEntryView = entryView;
        init();
    }

    public WebViewExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WebViewExtended(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {

        mEntryView.StatusStartPageLoading();
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
        
        setupWebChromeClient();

        setOnLongClickListener(view -> {
            HitTestResult hitTestResult = ((WebView) view).getHitTestResult();
            if (hitTestResult.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE ) {
                String link = hitTestResult.getExtra();
                ShowImageMenu( link, mEntryView.mTitle, view.getContext() );
                return true;
            }
            return false;
        });
        
        setupWebViewClient();

        setupOnTouchListener();

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

    private void setupOnTouchListener() {
        setOnTouchListener(new OnTouchListener() {
            private float mPressedY;
            private float mPressedX;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ( mEntryView.mActivity == null || mEntryView.mActivity.mEntryFragment == null )
                    return false;
                mEntryView.mActivity.mEntryFragment.mAnchor = "";
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
                            !mEntryView.mActivity.mHasSelection) {
                        //final HitTestResult hr = getHitTestResult();
                        //Log.v( TAG, "HitTestResult type=" + hr.getType() + ", extra=" + hr.getExtra()  );
                        mHandler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 0);
                        mHandler.sendEmptyMessageDelayed(TOGGLE_TAP_ZONE_VISIBIILTY, 150);
                    }
                }
                return false;
            }
        });
    }

    private void setupWebViewClient() {
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
                    else if (!anchor.isEmpty() && url.replace( "#" + anchor, "" ).equals( mEntryView.mEntryLink ) || !url.contains( "http" ) ) {
                        if ( anchor.isEmpty() )
                            ScrollSmoothTo( 0 );
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mEntryView.moveToAnchor(view, anchor);
                            mEntryView.AddNavigationHistoryStep();
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
                        Matcher matcher = REGEX.matcher(mEntryView.mData);
                        String title = matcher.find() ? Jsoup.parse(matcher.group(1 ) ).text() : url;
                        title = url.equals( title )  ? "" : title;

                        ShowLinkMenu(url, title, context);
                    }
                } catch ( ActivityNotFoundException e ) {
                    UiUtils.toast( R.string.cant_open_link);
                }
                return true;
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Status().ChangeProgress( "started..." );
                mEntryView.mContentWasLoaded = false;
                mEntryView.StatusStartPageLoading();
                super.onPageStarted( view, url, favicon );
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished( view, url );
                //Dog.v( "EntryView.onPageFinished url = " + url );
                if ( url.equals( "about:blank" ) ) {
                    Status().ChangeProgress("finished.");
                    if (!mEntryView.mLoadTitleOnly)
                        mEntryView.mContentWasLoaded = true;
                    if (mEntryView.mActivity.mEntryFragment != null)
                        mEntryView.DisableTapActionsIfVideo( mEntryView );
                    if ( !mIsScrollScheduled ) {
                        if (mEntryView.mContentWasLoaded)
                            mEntryView.DownLoadImages();
                        ScheduleScrollTo(view, new Date().getTime());
                    }
                } else
                    DoNotShowMenu(false);
            }

            private void ScheduleScrollTo(final WebView view, long startTime) {
                //Dog.v(TAG, "ScheduleScrollTo() mEntryID = " + mEntryId + ", mScrollPartY=" + mScrollPartY + ", GetScrollY() = " + GetScrollY() + ", GetContentHeight()=" + GetContentHeight() );
                double newContentHeight = GetContentHeight();
                final String searchText = mEntryView.mActivity.getIntent().getStringExtra( "SCROLL_TEXT" );
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
                            if (mEntryView.mActivity.mEntryFragment != null)
                                mEntryView.mActivity.mEntryFragment.UpdateHeader();
                            if ( mEntryView.mActivity.mEntryFragment != null && !mEntryView.mActivity.mEntryFragment.mAnchor.isEmpty() )
                                mEntryView.moveToAnchor( view, mEntryView.mActivity.mEntryFragment.mAnchor );
                            else
                                ScrollToY();
                        });
                    if ( new Date().getTime() - startTime < 1000 )
                        PostDelayed( view, startTime );
                    mEntryView.EndStatus();
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
    }

    private void setupWebChromeClient() {
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
                if ( mEntryView.mContentWasLoaded )
                    return;
                Status().ChangeProgress( String.format( "%d %% ...", progress ) );
            }


        });
    }

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
    private static final long MOVE_TIMEOUT = 800;

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();
    private final ImageDownloadJavaScriptObject mImageDownloadObject = new ImageDownloadJavaScriptObject();
    public static final ImageDownloadObservable mImageDownloadObservable = new ImageDownloadObservable();
    private EntryViewManager mEntryViewMgr;
    private long mPressedTime = 0;
    private long mMovedTime = 0;

    public void setListener(EntryViewManager manager) {
        mEntryViewMgr = manager;
    }

    public String generateHtmlContent(String feedID, Uri articleListUri, String title, String link, String contentText, String categories,
                                       String enclosure, String author,
                                       long timestamp, boolean canSwitchToFullText, boolean hasOriginalText) {
        Timer timer = new Timer("EntryView.generateHtmlContent");

        StringBuilder content = new StringBuilder(GetCSS(title, link, mEntryView.mIsEditingMode))
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



    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == CLICK_ON_URL) {
            mHandler.removeMessages(CLICK_ON_WEBVIEW);
            mEntryView.mActivity.closeContextMenu();
            return true;
        }
        if ( msg.what == TOGGLE_TAP_ZONE_VISIBIILTY ) {
            mEntryView.toggleTapZoneVisibility();
            return true;
        }
        if (msg.what == CLICK_ON_WEBVIEW)
            return true;
        return false;
    }

    void ScrollToY() {
        Dog.v(EntryView.TAG, "EntryView.ScrollToY() mEntryID = " + mEntryView.mEntryId + ", mScrollPartY=" + mEntryView.mScrollPartY + ", GetScrollY() = " + mEntryView.GetScrollY());
        if ( mEntryView.GetScrollY() > 0 )
            scrollTo( 0, mEntryView.GetScrollY() );
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        Status().HideByScroll();
        if ( mEntryView.mActivity != null && mEntryView.mActivity.mEntryFragment != null )
            mEntryView.mActivity.mEntryFragment.UpdateHeader();
        mLastTimeScrolled = System.currentTimeMillis();
        if (mScrollChangeListener != null)
            mScrollChangeListener.run();
    }


    static int NOTIFY_OBSERVERS_DELAY_MS = 1000;


    static HashMap<Long, Long> mLastNotifyObserversTime = new HashMap<>();
    static HashMap<Long, Boolean> mLastNotifyObserversScheduled = new HashMap<>();

    static void ScheduledNotifyObservers(final long entryId, final String entryLink) {
        mLastNotifyObserversTime.put(entryId, new Date().getTime());
        if (!mLastNotifyObserversScheduled.containsKey(entryId)) {
            mLastNotifyObserversScheduled.put(entryId, true);
            UiUtils.RunOnGuiThread(new ScheduledEntryNotifyObservers(entryId, entryLink), NOTIFY_OBSERVERS_DELAY_MS);
        }
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
        mEntryView.mActivity.closeOptionsMenu();
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


    public double GetContentHeight() {
        return getContentHeight() * getScale();
    }


    public void ScrollSmoothTo(int y) {
        ObjectAnimator anim = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), y);
        anim.setDuration(PAGE_SCROLL_DURATION_MSEC);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Dog.d( "WebView.onPause " + mEntryView.mEntryId );
        mEntryView.SaveScrollPos();
        mEntryView.EndStatus();
    }
    int getPageHeight() {
        return getHeight() - mEntryView.mActivity.mEntryFragment.mStatusText.GetHeight();
    }
    int getPageCount() {
        return (int) (GetContentHeight() / getPageHeight());
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
        WebViewExtended.mLastNotifyObserversScheduled.remove( mId );
        //Dog.v( EntryView.TAG,"ScheduledNotifyObservers() run");
        if (new Date().getTime() - WebViewExtended.mLastNotifyObserversTime.get( mId ) > WebViewExtended.NOTIFY_OBSERVERS_DELAY_MS)
            WebViewExtended.mImageDownloadObservable.notifyObservers(new Entry(mId, mLink, false) );
        else
            WebViewExtended.ScheduledNotifyObservers( mId, mLink );
    }

}

