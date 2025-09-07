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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
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
import androidx.appcompat.widget.SearchView;
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
import ru.yanus171.feedexfork.view.Entry;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;
import ru.yanus171.feedexfork.view.WebEntryView;
import ru.yanus171.feedexfork.view.WebViewExtended;

import static ru.yanus171.feedexfork.Constants.CONTENT_SCHEME;
import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.FILE_SCHEME;
import static ru.yanus171.feedexfork.Constants.HTTPS_SCHEME;
import static ru.yanus171.feedexfork.Constants.HTTP_SCHEME;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.activity.GeneralPrefsActivity.mActivity;
import static ru.yanus171.feedexfork.adapter.DrawerAdapter.newNumber;
import static ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.LANDSCAPE;
import static ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.NONE;
import static ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.PORTRAIT;
import static ru.yanus171.feedexfork.fragment.GeneralPrefsFragment.mSetupChanged;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_FORCE_ORIENTATION_BY_SENSOR;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_PROGRESS_INFO;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabled;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.view.EntryView.TAG;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.UpdateTapZonesTextAndVisibility;


public class EntryFragment extends /*SwipeRefresh*/Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    //private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";
    private static final String STATE_LOCK_LAND_ORIENTATION = "STATE_LOCK_LAND_ORIENTATION";

    public static final String NO_DB_EXTRA = "NO_DB_EXTRA";
    public static final String NEW_TASK_EXTRA = "NEW_TASK_EXTRA";

    public static final String STATE_RELOAD_IMG_WITH_A_LINK = "STATE_REPLACE_IMG_WITH_A_LINK";
    public static final String STATE_RELOAD_WITH_DEBUG = "STATE_RELOAD_WITH_DEBUG";
    public boolean mIgnoreNextLoading = false;



    public Uri mBaseUri;
    public int mCurrentPagerPos = -1;
    private int mLastPagerPos = -1;
    private long mInitialEntryId = -1;

    private boolean mIsTapZoneVisible = false;

    public enum ForceOrientation {NONE, LANDSCAPE, PORTRAIT}
    public ForceOrientation ForceOrientationFromInt(int code) {
        return code == 1 ? LANDSCAPE : code == 2 ? PORTRAIT : NONE;
    }
    int ForceOrientationToInt( ForceOrientation fo ) {
        return fo == LANDSCAPE ? 1 : fo == PORTRAIT ? 2 : 0;
    }
    public ForceOrientation mForceOrientation = NONE;

    private ViewPager mEntryPager;
    public BaseEntryPagerAdapter mEntryPagerAdapter;

    private View mStarFrame = null;
    public StatusText mStatusText = null;

    public boolean mMarkAsUnreadOnFinish = false;
    public boolean mIsFinishing = false;
    public View mBtnEndEditing = null;
    public FeedFilters mFilters = null;
    private static EntryView mLeakEntryView = null;
    private String mWhereSQL;
    static public final String WHERE_SQL_EXTRA = "WHERE_SQL_EXTRA";
    public int mLastScreenState = -1;
    private String mSearchText = "";
    MenuItem mSearchNextItem = null;
    MenuItem mSearchPreviousItem = null;
    MenuItem mForceLandscapeOrientationMenuItem = null;
    public MenuItem mForcePortraitOrientationMenuItem = null;
    public String mAnchor = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu( true );
        if ( IsExternalLink( getActivity().getIntent().getData() ) )
            mEntryPagerAdapter = new SingleEntryPagerAdapter( this );
        else
            mEntryPagerAdapter = new EntryPagerAdapter( this );

        mWhereSQL = getActivity().getIntent().getStringExtra( WHERE_SQL_EXTRA );
        if ( savedInstanceState != null ) {
            mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS, -1);
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            //outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID, -1);
        }

        super.onCreate(savedInstanceState);
    }

    private void UpdateTapZoneButton( int viewID, boolean visible ) {
        UiUtils.UpdateTapZoneButton( getBaseActivity().mRootView, viewID, visible );
    }

    public void SetupZones() {
        final boolean visible = mIsTapZoneVisible;
        UpdateTapZoneButton( R.id.pageUpBtn, false );
        UpdateTapZoneButton( R.id.pageDownBtn, false );
        UpdateTapZoneButton( R.id.brightnessSliderLeft, false );
        UpdateTapZoneButton( R.id.brightnessSliderRight, false );
        UpdateTapZoneButton( R.id.entryLeftBottomBtn, visible );
        UpdateTapZoneButton( R.id.entryRightBottomBtn, visible );
        UpdateTapZoneButton( R.id.leftTopBtn, visible );
        UpdateTapZoneButton( R.id.rightTopBtn, visible );
        UpdateTapZoneButton( R.id.backBtn, visible );
        UpdateTapZoneButton( R.id.leftTopBtnFS, visible );
        UpdateTapZoneButton( R.id.rightTopBtnFS, visible );
        UpdateTapZoneButton( R.id.entryCenterBtn, visible );

        final EntryView view = GetSelectedEntryView();
        final boolean isBackBtnVisible = view != null && view.CanGoBack() && visible; //&& IsZoneEnabled( R.id.backBtn, false, false );
        getBaseActivity().mRootView.findViewById( R.id.backBtn ).setVisibility(isBackBtnVisible ? View.VISIBLE : View.GONE );

        if ( !isArticleTapEnabledTemp() ) {
            UpdateTapZoneButton(R.id.rightTopBtn, true);
            getBaseActivity().mRootView.findViewById(R.id.rightTopBtn).setVisibility( View.VISIBLE );
        }
    }

    private boolean IsCreateViewPager( Uri uri ) {
        return PrefUtils.getBoolean( "change_articles_by_swipe", false ) &&
                !IsExternalLink( uri );
    }

    public static boolean IsExternalLink( Uri uri ) {
        return uri == null ||
            uri.toString().startsWith( HTTP_SCHEME ) ||
            uri.toString().startsWith( HTTPS_SCHEME ) ||
            IsLocalFile( uri );
    }
    public static boolean IsLocalFile(Uri uri ) {
        return uri.toString().startsWith( CONTENT_SCHEME ) &&
            ( uri.toString().contains( "document" ) || uri.toString().contains( "media" )  || uri.toString().contains( "storage" ) ) ||
            uri.toString().startsWith( FILE_SCHEME ) || uri.toString().contains( "cache_root" );
    }

    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getBaseActivity().mRootView = inflater.inflate(R.layout.fragment_entry, container, true);
        View rootView = getBaseActivity().mRootView;
        SetupZones();

        mStatusText = new StatusText( rootView.findViewById(R.id.statusText ),
                                      rootView.findViewById(R.id.errorText ),
                                      rootView.findViewById(R.id.progressBarLoader),
                                      rootView.findViewById(R.id.progressText),
                                      FetcherService.Status() );

        rootView.findViewById(R.id.backBtn).setOnClickListener(v -> GetSelectedEntryView().GoBack() );
        rootView.findViewById(R.id.backBtn).setOnLongClickListener( v -> {
            GetSelectedEntryView().ClearHistoryAnchor();
            return true;
        });

        rootView.findViewById(R.id.rightTopBtn).setOnClickListener(v -> {
            if ( isArticleTapEnabled() ) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen( GetIsStatusBarHidden(), !GetIsActionBarHidden() );
            } else
                EnableTapActions();
        });
        rootView.findViewById(R.id.rightTopBtn).setOnLongClickListener(view -> {
            if ( isArticleTapEnabled() ) {
                PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, false);
                SetupZones();
                GetSelectedEntryView().refreshUI( true );
                Toast.makeText(MainApplication.getContext(), R.string.tap_actions_were_disabled, Toast.LENGTH_LONG).show();
            } else
                EnableTapActions();
            return true;
        });

        rootView.findViewById(R.id.leftTopBtn).setOnClickListener(v -> {
            EntryActivity activity = (EntryActivity) getActivity();
            activity.setFullScreen(!GetIsStatusBarHidden(), GetIsActionBarHidden());
        });
        rootView.findViewById(R.id.leftTopBtn).setOnLongClickListener(view -> {
            if ( !isArticleTapEnabled() )
                return true;
            GetSelectedEntryView().OpenLabelSetup();
            return true;
        });

        rootView.findViewById(R.id.entryLeftBottomBtn).setOnClickListener(v -> mEntryPager.setCurrentItem(mEntryPager.getCurrentItem() - 1, true ));
        rootView.findViewById(R.id.entryLeftBottomBtn).setOnLongClickListener(view -> {
            if ( !isArticleTapEnabled() )
                return true;
            if ( GetSelectedEntryWebView() == null )
                return true;
            GetSelectedEntryWebView().DownloadAllImages();
            Toast.makeText(getContext(), R.string.downloadAllImagesStarted, Toast.LENGTH_LONG).show();
            return true;
        });

        rootView.findViewById(R.id.entryRightBottomBtn).setOnClickListener(v -> mEntryPager.setCurrentItem(mEntryPager.getCurrentItem() + 1, true ));
        rootView.findViewById(R.id.entryRightBottomBtn).setOnLongClickListener(view -> {
            if ( !isArticleTapEnabled() )
                return true;
            if ( GetSelectedEntryWebView() == null )
                return true;
            GetSelectedEntryWebView().ReloadFullText();
            Toast.makeText(getContext(), R.string.fullTextReloadStarted, Toast.LENGTH_LONG).show();
            return true;
        });

        mBtnEndEditing = rootView.findViewById(R.id.btnEndEditing);
        mBtnEndEditing.setVisibility( View.GONE );
        mBtnEndEditing.setOnClickListener(view -> {
            GetSelectedEntryWebView().ReloadFullText();
            Toast.makeText(getContext(), R.string.fullTextReloadStarted, Toast.LENGTH_LONG).show();
        });

        mEntryPager = rootView.findViewById(R.id.pager);
        //mEntryPager.setPageTransformer(true, new DepthPageTransformer());
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        //    mEntryPager.setNestedScrollingEnabled( true );
        if (savedInstanceState != null) {
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            //mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID);
            //mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS);
            mLastPagerPos = mCurrentPagerPos;
        }

        if ( mEntryPagerAdapter instanceof EntryPagerAdapter )
            mEntryPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrolled(int i, float v, int i2) {
                }

                @SuppressLint("DefaultLocale")
                @Override
                public void onPageSelected(int i) {
                    final boolean isForward = mCurrentPagerPos < i;
                    mCurrentPagerPos = i;
                    mEntryPagerAdapter.onPause(); // pause all webviews
                    mEntryPagerAdapter.onResume(); // resume the current webview
                    if ( !getEntryActivity().mIsNewTask )
                        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID()).toString());

                    CancelStarNotification( getCurrentEntryID() );

                    mLastPagerPos = i;
                    refreshUI(false);

                    EntryView view = mEntryPagerAdapter.GetEntryView( i );
                    if ( view != null && GetSelectedEntryWebView() != null ) {
                        if ( view.mLoadTitleOnly )
                            getLoaderManager().restartLoader(i, null, EntryFragment.this);
                        else
                            GetSelectedEntryWebView().DisableTapActionsIfVideo( view );
                        view.mLoadTitleOnly = false;
                    }
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

        TextView.OnClickListener listener = view -> PageDown();

        rootView.findViewById(R.id.pageDownBtn).setOnClickListener(listener);
        //rootView.findViewById(R.id.pageDownBtnVert).setOnClickListener(listener);
        rootView.findViewById(R.id.pageDownBtn).setOnLongClickListener(v -> {
            if ( !isArticleTapEnabled() )
                return true;
            final EntryView view = mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() );
            view.ScrollToBottom();
            Toast.makeText( v.getContext(), R.string.list_was_scrolled_to_bottom, Toast.LENGTH_SHORT ).show();
            return true;
        });

        rootView.findViewById(R.id.layoutColontitul).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.statusText).setVisibility(View.GONE);

        final Vibrator vibrator = (Vibrator) getContext().getSystemService( Context.VIBRATOR_SERVICE );
        mStarFrame = rootView.findViewById(R.id.frameStar);
        final ImageView frameStarImage  = rootView.findViewById(R.id.frameStarImage);
        final boolean prefVibrate = getBoolean(VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE, true);
        rootView.findViewById(R.id.pageUpBtn).setOnTouchListener(new View.OnTouchListener() {
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
                                Toast.makeText(MainApplication.getContext(), R.string.list_was_scrolled_to_top, Toast.LENGTH_SHORT).show();
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

        rootView.findViewById(R.id.entryCenterBtn).setOnClickListener(v -> getEntryActivity().openOptionsMenu());

            SetStarFrameWidth(0);
        UpdateHeader();

        return rootView;
    }

    private void EnableTapActions() {
        PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, true );
        SetupZones();
        GetSelectedEntryView().refreshUI( true );
        Toast.makeText(MainApplication.getContext(), R.string.tap_actions_were_enabled, Toast.LENGTH_LONG ).show();
    }


    private void SetStarFrameWidth(int w) {
        mStarFrame.setLayoutParams( new FrameLayout.LayoutParams( FrameLayout.LayoutParams.FILL_PARENT, w));
    }


    public void applyOrientation() {
        int or = mForceOrientation == LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE :
                 mForceOrientation == PORTRAIT ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT :
			     ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        if ( or != getActivity().getRequestedOrientation() )
			getActivity().setRequestedOrientation( or );
    }

    public void PageUp() {
        mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() ).PageChange(-1, mStatusText);
    }

    public void PageDown() {
        mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() ).PageChange(1, mStatusText);
    }

    public void NextEntry() {
        if ( mEntryPager.getCurrentItem() < mEntryPager.getAdapter().getCount() - 1  )
            mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() + 1 );
    }
    public void PreviousEntry() {
        if ( mEntryPager.getCurrentItem() > 0  )
            mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() - 1 );
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
            mEntryPagerAdapter.generateArticleContent(mCurrentPagerPos, true, false);
        }
        mLastScreenState = getActivity().getResources().getConfiguration().orientation;
        UpdateTapZonesTextAndVisibility(getView().getRootView(), mIsTapZoneVisible );
        refreshUI(false);
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
        if ( newConfig.orientation != mLastScreenState && mCurrentPagerPos != -1) {
            EntryView entryView = GetSelectedEntryView();
            if ( entryView != null && mForceOrientation != LANDSCAPE &&
                 entryView instanceof WebEntryView && ((WebEntryView)entryView).mHasScripts ) {
                mEntryPager.setAdapter(mEntryPagerAdapter);
                mEntryPager.setCurrentItem(mCurrentPagerPos);
            }
        }
        mLastScreenState = newConfig.orientation;
    }


    @Override
    public void onPause() {
        EntryView entryView = mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() );
        if (entryView != null) {
            //PrefUtils.putInt(PrefUtils.LAST_ENTRY_SCROLL_Y, entryView.getScrollY());
            entryView.SaveScrollPos();
        }

        mEntryPagerAdapter.onPause();
        super.onPause();
    }


    /**
     * Updates a menu item in the dropdown to show it's icon that was declared in XML.
     *
     * @param item
     *         the item to update
     */
    private static void updateMenuWithIcon(@NonNull final MenuItem item) {
        SpannableStringBuilder builder = new SpannableStringBuilder()
                .append("*") // the * will be replaced with the icon via ImageSpan
                .append("    ") // This extra space acts as padding. Adjust as you wish
                .append(item.getTitle());



        // Retrieve the icon that was declared in XML and assigned during inflation
        if (item.getIcon() != null && item.getIcon().getConstantState() != null) {
            Drawable drawable = item.getIcon().getConstantState().newDrawable();

            // Mutate this drawable so the tint only applies here
            // drawable.mutate().setTint(color);

            // Needs bounds, or else it won't show up (doesn't know how big to be)
            //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.setBounds(0, 0, DpToPx( 30 ), DpToPx( 30 ) );
            //drawable.setBounds(-DpToPx( 30 ), 0, 0, 0 );
            ImageSpan imageSpan = new ImageSpan(drawable);
            builder.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(builder);
        }
    }

    // -------------------------------------------------------------------------
    public static int DpToPx(float dp) {
        Resources r = MainApplication.getContext().getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return (int) px;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            menu.setGroupDividerEnabled( true );
        for ( int i = 0; i < menu.size(); i++ )
            updateMenuWithIcon( menu.getItem( i ) );

        menu.findItem(R.id.menu_star).setShowAsAction( GetIsActionBarHidden() ? MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW : MenuItem.SHOW_AS_ACTION_IF_ROOM );

        final EntryView view = GetSelectedEntryView();
        if ( view != null ){
            MenuItem item = menu.findItem(R.id.menu_star);
            if (view.mFavorite)
                item.setTitle(R.string.menu_unstar).setIcon(R.drawable.ic_star);
            else
                item.setTitle(R.string.menu_star).setIcon(R.drawable.ic_star_border);
            updateMenuWithIcon(item);
        }

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        mSearchNextItem = menu.findItem(R.id.menu_search_next);
        mSearchPreviousItem = menu.findItem(R.id.menu_search_previous);
        mSearchNextItem.setVisible( false );
        mSearchPreviousItem.setVisible( false );

        mForceLandscapeOrientationMenuItem = menu.findItem(R.id.menu_force_landscape_orientation_toggle);
        mForcePortraitOrientationMenuItem = menu.findItem(R.id.menu_force_portrait_orientation_toggle);

        // Use a custom search icon for the SearchView in AppBar
        int searchImgId = androidx.appcompat.R.id.search_button;
        ImageView v = searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search);

        if (!mSearchText.isEmpty()) {
            searchItem.expandActionView();
            // Without that, it just does not work
            searchView.post(() -> {
                searchView.setQuery(mSearchText, false);
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
                mSearchText = newText;
                mSearchNextItem.setVisible( false );
                mSearchPreviousItem.setVisible( false );

                if (!TextUtils.isEmpty(newText)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                        GetSelectedEntryWebViewExtended().findAllAsync( newText );
                    else
                        GetSelectedEntryWebViewExtended().findAll( newText );
                }
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            mSearchText = "";
            mSearchNextItem.setVisible( false );
            mSearchPreviousItem.setVisible( false );
            GetSelectedEntryWebViewExtended().clearMatches();
            return false;
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        if ( GetSelectedEntryView() != null )
            GetSelectedEntryView().onPrepareOptionsMenu( menu );
    }

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
                UiUtils.toast( mActivity, R.string.entry_marked_unread );
                break;
            }

            case R.id.menu_force_orientation_by_sensor: {
                item.setChecked( !item.isChecked() );
                PrefUtils.putBoolean( PREF_FORCE_ORIENTATION_BY_SENSOR, item.isChecked() );
                if ( mForceOrientation == NONE )
                    getEntryActivity().applyBaseOrientation();
                break;
            }
            case R.id.menu_force_landscape_orientation_toggle: {
                item.setChecked( !item.isChecked() );
                changeOrientation(item.isChecked() ? LANDSCAPE : NONE);
                break;
            }
            case R.id.menu_force_portrait_orientation_toggle: {
                item.setChecked( !item.isChecked() );
                changeOrientation(item.isChecked() ? PORTRAIT : NONE );
                break;
            }
            case R.id.menu_show_progress_info: {
                PrefUtils.toggleBoolean( SHOW_PROGRESS_INFO, false ) ;
                item.setChecked( PrefUtils.getBoolean( SHOW_PROGRESS_INFO, false ) );
                break;
            }
            case R.id.menu_article_web_search: {
                mActivity.startActivity( new Intent(Intent.ACTION_WEB_SEARCH)
                        .setPackage(mActivity.getPackageName())
                        .setClass(mActivity, ArticleWebSearchActivity.class) );
                break;
            }


        }
        GetSelectedEntryView().onOptionsItemSelected(item);

        return true;
    }



    public void close() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( getActivity() != null )
                    getActivity().onBackPressed();
            }
        });
    }




    public void SetForceOrientation(final ForceOrientation forceOrientation) {
        if ( mForceOrientation == forceOrientation )
            return;
        mForceOrientation = forceOrientation;
        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        ContentValues values = new ContentValues();
        values.put( EntryColumns.IS_LANDSCAPE, ForceOrientationToInt( mForceOrientation ) );
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.update(uri, values, null, null);
        GetSelectedEntryView().InvalidateContentCache();
        UiUtils.RunOnGuiThread( () -> getLoaderManager().restartLoader(mCurrentPagerPos, null, EntryFragment.this) );
        getActivity().invalidateOptionsMenu();
    }


//    private void DeleteMobilized() {
//        FileUtils.INSTANCE.deleteMobilized( ContentUris.withAppendedId(mBaseUri, getCurrentEntryID() ) );
//    }

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
        Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
        if (cursor != null)
            return cursor.getString(GetSelectedEntryView().mFeedIDPos);
        else
            return "";
    }

    private String getCurrentEntryLink() {
        Entry entry = mEntryPagerAdapter.GetEntry( mCurrentPagerPos );
        if ( entry != null )
            return entry.mLink;
        else
            return "";
    }

    @SuppressLint("StaticFieldLeak")
    public void setData(final Uri uri) {
        mCurrentPagerPos = -1;
        mBaseUri = null;
        Dog.v( TAG, "setData " + uri );

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Timer timer = new Timer( "EntryFr.setData" );
                Dog.v( String.format( "EntryFragment.setData( %s )", uri == null ? "" : uri.toString() ) );

                //PrefUtils.putString( PrefUtils.LAST_URI, uri.toString() );
                if ( !IsExternalLink( uri ) ) {
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
                    {
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
        ContentValues values = new ContentValues();
        values.put(EntryColumns.READ_DATE, new Date().getTime());
        final ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.update( entryUri, values, null, null );
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
                    view.mCursor.getInt(view.mIsReadPos) != 1,
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


    /*private void setImmersiveFullScreen(boolean fullScreen) {
        BaseActivity activity = (BaseActivity) getActivity();
        if ( fullScreen )
            mToggleStatusBarVisbleBtn.setVisibility(View.VISIBLE);
        else
            mToggleStatusBarVisbleBtn.setVisibility(View.GONE);
        activity.setImmersiveFullScreen(fullScreen);
    }*/

    public void UpdateHeader() {
        EntryView entryView = GetSelectedEntryView();
        EntryView.ProgressInfo info = new EntryView.ProgressInfo();
        if (entryView != null)
            info = entryView.getProgressInfo( mStatusText.GetHeight() );
        getBaseActivity().UpdateHeader(info.max,
                                       info.progress,
                                       info.step,
                                       GetIsStatusBarHidden(),
                                       GetIsActionBarHidden());
    }




    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timer.Start( id, "EntryFr.onCreateLoader" );
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(mEntryPagerAdapter.GetEntry( id ).mID), null, null, null, null);
        cursorLoader.setUpdateThrottle(100);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
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
        }
        //refreshUI();
    }



    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        final EntryView view = mEntryPagerAdapter.GetEntryView( loader.getId() );
        if (view != null )
            view.setCursor(null);
    }

    private boolean hasEntryView() {
        return GetSelectedEntryView() != null;
    }

    /*@Override
    public void onRefresh() {
        // Nothing to do
    }*/


    @NonNull
    public EntryView CreateWebEntryView(EntryActivity activity, int position, ViewGroup container ) {
        final Entry entry = mEntryPagerAdapter.GetEntry(position);
        final EntryView view = EntryView.Create( entry.mLink, entry.mID, activity, container );
        view.mView.setTag(view);

        if ( mLeakEntryView == null )
            mLeakEntryView  = view;

        final WebViewExtended webView = GetSelectedEntryWebViewExtended();
        if (Build.VERSION.SDK_INT >= 16 && webView != null ) {
            webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                if (mSearchNextItem == null || mSearchPreviousItem == null)
                    return;
                mSearchNextItem.setVisible(numberOfMatches > 1);
                mSearchPreviousItem.setVisible(numberOfMatches > 1);
            });
        }
        view.StatusStartPageLoading();
        return view;
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

    public void toggleTapZoneVisibility() {
        mIsTapZoneVisible = !mIsTapZoneVisible;
        SetupZones();
    }
    public boolean hasVideo() {
        final WebEntryView view = GetSelectedEntryWebView();
        if ( view != null )
            return view.hasVideo();
        return false;
    }
    public void changeOrientation(EntryFragment.ForceOrientation orientation) {
        SetForceOrientation( orientation );
        if ( orientation == LANDSCAPE && mForcePortraitOrientationMenuItem != null)
            mForcePortraitOrientationMenuItem.setChecked( false );
        else if ( orientation == PORTRAIT && mForceLandscapeOrientationMenuItem != null)
            mForceLandscapeOrientationMenuItem.setChecked( false );
    }
    public void refreshUI( boolean invalidateContent ) {
        mBtnEndEditing.setVisibility( View.GONE );
        mBtnEndEditing.setBackgroundColor( Theme.GetToolBarColorInt() );
        EntryView view = GetSelectedEntryView();
        if (view != null && view.mCursor != null ) {
            getEntryActivity().SetTaskTitle( view.mTitle );
            SetForceOrientation(ForceOrientationFromInt(view.mCursor.getInt(view.mIsLandscapePos)));
            applyOrientation();
            getEntryActivity().invalidateOptionsMenu();
            view.refreshUI( invalidateContent );

            mStatusText.SetEntryID(String.valueOf(view.mEntryId));

            startMobilizationTask(view.mEntryId);
        }
        applyOrientation();
        markPrevArticleAsRead();
    }
}

