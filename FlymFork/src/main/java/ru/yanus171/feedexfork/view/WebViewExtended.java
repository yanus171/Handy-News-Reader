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
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.view.AppSelectPreference.GetShowInBrowserIntent;
import static ru.yanus171.feedexfork.view.EntryView.TAP_TIMEOUT;
import static ru.yanus171.feedexfork.view.WebEntryView.OpenImage;
import static ru.yanus171.feedexfork.view.WebEntryView.ShowImageMenu;
import static ru.yanus171.feedexfork.view.WebEntryView.ShowLinkMenu;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
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

import org.jsoup.Jsoup;

import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class WebViewExtended extends WebView implements Handler.Callback {

    static final String TEXT_HTML = "text/html";
    public static final String NO_MENU = "NO_MENU_";
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
                if ( mEntryView.mEntryFragment == null )
                    return false;
                mEntryView.mEntryFragment.mAnchor = "";
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
                            !mEntryView.mEntryFragment.getEntryActivity().mHasSelection) {
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

            private boolean mIsPageFinished = false;

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
                        String title = "";
                        try {
                            final Pattern REGEX = Pattern.compile("<a[^>]+?href=.url.+?>(.+?)</a>".replace("url", urlWithoutRegexSymbols), Pattern.CASE_INSENSITIVE);
                            Matcher matcher = REGEX.matcher(mEntryView.mData);
                            title = matcher.find() ? Jsoup.parse(matcher.group(1)).text() : url;
                            title = url.equals(title) ? "" : title;
                        } catch ( PatternSyntaxException e ){
                            DebugApp.AddErrorToLog( null, e );
                        }
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
                mIsPageFinished = false;
                mEntryView.mContentWasLoaded = false;
                mEntryView.StatusStartPageLoading();
                super.onPageStarted( view, url, favicon );
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished( view, url );
                mIsPageFinished = true;
                //Dog.v( "EntryView.onPageFinished url = " + url );
                if ( url.equals( "about:blank" ) ) {
                    Status().ChangeProgress("finished.");
                    mEntryView.DisableTapActionsIfVideo();
                    if ( !mIsScrollScheduled ) {
                        if (mEntryView.mContentWasLoaded)
                            mEntryView.DownLoadImages();
                        ScheduleScrollTo(view, new Date().getTime());
                    }
                } else
                    DoNotShowMenu(false);
                if ( !mEntryView.mEntryFragment.isCurrentPage( mEntryView.mPosition ) )
                    mEntryView.EndStatus();
            }

            private void ScheduleScrollTo(final WebView view, long startTime) {
                //Dog.v(TAG, "ScheduleScrollTo() mEntryID = " + mEntryId + ", mScrollPartY=" + mScrollPartY + ", GetScrollY() = " + GetScrollY() + ", GetContentHeight()=" + GetContentHeight() );
                double newContentHeight = GetContentHeight();
                final String searchText = mEntryView.mEntryFragment.getActivity() != null ? mEntryView.mEntryFragment.getActivity().getIntent().getStringExtra( "SCROLL_TEXT" ) : null;
                final boolean isSearch = searchText != null && !searchText.isEmpty();
                if ( mIsPageFinished && !mIsScrollScheduled && newContentHeight > 0 && newContentHeight == mLastContentHeight) {
                    if (!mEntryView.mLoadTitleOnly) {
                        mEntryView.mContentWasLoaded = true;
                        mEntryView.EndStatus();
                    }
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
                            if (mEntryView.mEntryFragment != null)
                                mEntryView.mEntryFragment.UpdateHeader();
                            if ( mEntryView.mEntryFragment != null && !mEntryView.mEntryFragment.mAnchor.isEmpty() )
                                mEntryView.moveToAnchor( view, mEntryView.mEntryFragment.mAnchor );
                            else
                                ScrollToY();
                        });
                    if ( new Date().getTime() - startTime < 1000 )
                        PostDelayed( view, startTime );
                } else
                    PostDelayed( view, startTime );
                mLastContentHeight = newContentHeight;
            }

            void PostDelayed( final WebView view, long startTime ) {
                mEntryView.StatusStartPageLoading();
                if ( !mIsScrollScheduled ) {
                    mIsScrollScheduled = true;
                    view.postDelayed(() -> {
                        mIsScrollScheduled = false;
                        ScheduleScrollTo(view, startTime);
                    }, 550);
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


    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == CLICK_ON_URL) {
            mHandler.removeMessages(CLICK_ON_WEBVIEW);
            mEntryView.mEntryFragment.getActivity().closeContextMenu();
            return true;
        }
        if ( msg.what == TOGGLE_TAP_ZONE_VISIBIILTY ) {
            if ( mEntryView.mEntryFragment.mTapZones != null )
                mEntryView.mEntryFragment.mTapZones.toggleVisibility();
            return true;
        }
        if (msg.what == CLICK_ON_WEBVIEW)
            return true;
        return false;
    }

    void ScrollToY() {
        //Dog.v(EntryView.TAG, "EntryView.SaveScrollPos.ScrollToY() before mEntryID = " + mEntryView.mEntryId + ", mScrollPartY=" + mEntryView.mScrollPartY + ", GetScrollY() = " + mEntryView.GetScrollY());
        if ( mEntryView.GetScrollY() > 0 )
            scrollTo( 0, mEntryView.GetScrollY() );
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        Status().HideByScroll();
        if ( mEntryView.mEntryFragment != null )
            mEntryView.mEntryFragment.UpdateHeader();
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
            DoNotShowMenu(true);
            mEntryViewMgr.onClose();
        }
    }

    private void DoNotShowMenu( boolean hideTapZones  ) {
        mHandler.sendEmptyMessage(CLICK_ON_URL);
        if ( hideTapZones )
            mHandler.removeMessages( TOGGLE_TAP_ZONE_VISIBIILTY );
        mEntryView.mEntryFragment.getActivity().closeOptionsMenu();
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
        Dog.d( "WebView.onPause " + mEntryView.mEntryId );
        mEntryView.SaveScrollPos();
        mEntryView.EndStatus();
        super.onPause();
    }
    int getPageHeight() {
        return getHeight() - mEntryView.mEntryFragment.mStatusText.GetHeight();
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


