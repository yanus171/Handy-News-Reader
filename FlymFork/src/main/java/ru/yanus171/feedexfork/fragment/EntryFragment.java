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
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.GeneralPrefsActivity;
import ru.yanus171.feedexfork.activity.MessageBox;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.adapter.EntriesCursorAdapter;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.utils.WaitDialog;
import ru.yanus171.feedexfork.view.Entry;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

import static ru.yanus171.feedexfork.Constants.MILLS_IN_SECOND;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.EXTRA_WEB_SEARCH;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.mWhereSQL;
import static ru.yanus171.feedexfork.fragment.GeneralPrefsFragment.mSetupChanged;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.service.FetcherService.GetActionIntent;
import static ru.yanus171.feedexfork.utils.PrefUtils.CONTENT_TEXT_ROOT_EXTRACT_RULES;
import static ru.yanus171.feedexfork.utils.PrefUtils.DATE_EXTRACT_RULES;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_PROGRESS_INFO;
import static ru.yanus171.feedexfork.utils.PrefUtils.STATE_IMAGE_WHITE_BACKGROUND;
import static ru.yanus171.feedexfork.utils.PrefUtils.CATEGORY_EXTRACT_RULES;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabled;
import static ru.yanus171.feedexfork.utils.Theme.TEXT_COLOR_READ;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.HideTapZonesText;


public class EntryFragment extends /*SwipeRefresh*/Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
    EntryView.EntryViewManager {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";
    private static final String STATE_LOCK_LAND_ORIENTATION = "STATE_LOCK_LAND_ORIENTATION";

    public static final String NO_DB_EXTRA = "NO_DB_EXTRA";


    private int mTitlePos = -1, mDatePos, mAbstractPos, mLinkPos, mIsFavoritePos, mIsWithTablePos, mIsReadPos, mIsNewPos, mIsWasAutoUnStarPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconUrlPos, mFeedIDPos, mScrollPosPos, mRetrieveFullTextPos;


    private int mCurrentPagerPos = -1, mLastPagerPos = -1;
    private Uri mBaseUri;
    private long mInitialEntryId = -1;
    private Entry[] mEntriesIds = new Entry[1];

    private boolean mFavorite, mIsWithTables, mIsFullTextShown = true;

    private ViewPager mEntryPager;
    public BaseEntryPagerAdapter mEntryPagerAdapter;

    private View mStarFrame = null;
    private StatusText mStatusText = null;

    private boolean mLockLandOrientation = false;

    public boolean mMarkAsUnreadOnFinish = false;
    private boolean mRetrieveFullText = false;
    public boolean mIsFinishing = false;
    private View mBtnEndEditing = null;
    private FeedFilters mFilters = null;
    private static EntryView mLeakEntryView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu( true );
        if ( IsExternalLink( getActivity().getIntent().getData() ) )
            mEntryPagerAdapter = new SingleEntryPagerAdapter();
        else
            mEntryPagerAdapter = new EntryPagerAdapter();

        if ( savedInstanceState != null ) {
            mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS, -1);
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            //outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID, -1);
        }

        super.onCreate(savedInstanceState);
    }

    private boolean IsCreateViewPager( Uri uri ) {
        return PrefUtils.getBoolean( "change_articles_by_swipe", false ) &&
                !IsExternalLink( uri );
    }

    private boolean IsExternalLink( Uri uri ) {
        return uri == null || uri.toString().startsWith( "http" );
    }
    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    //@Override
    //public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        getBaseActivity().mRootView = inflater.inflate(R.layout.fragment_entry, container, true);
        View rootView = getBaseActivity().mRootView;
        TapZonePreviewPreference.SetupZoneSizes( rootView, false );

        mStatusText = new StatusText( (TextView)rootView.findViewById( R.id.statusText ),
                                      (TextView)rootView.findViewById( R.id.errorText ),
                                      (ProgressBar) rootView.findViewById( R.id.progressBarLoader),
                                      (TextView) rootView.findViewById( R.id.progressText),
                                      FetcherService.Status());
        rootView.findViewById(R.id.toggleFullscreenBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen( GetIsStatusBarHidden(), !GetIsActionBarHidden() );
            }
        });
        rootView.findViewById(R.id.toggleFullScreenStatusBarBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen(!GetIsStatusBarHidden(), GetIsActionBarHidden());
            }
        });

        mBtnEndEditing = rootView.findViewById(R.id.btnEndEditing);
        mBtnEndEditing.setVisibility( View.GONE );
        mBtnEndEditing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReloadFullText();
                Toast.makeText(getContext(), R.string.fullTextReloadStarted, Toast.LENGTH_LONG).show();
            }
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
                    mCurrentPagerPos = i;
                    mEntryPagerAdapter.onPause(); // pause all webviews
                    mEntryPagerAdapter.onResume(); // resume the current webview

                    PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID()).toString());

                    CancelStarNotification( getCurrentEntryID() );

                    mLastPagerPos = i;
                    refreshUI( mEntryPagerAdapter.getCursor(i) );

                    EntryView view = mEntryPagerAdapter.GetEntryView( i );
                    if ( view != null ) {
                        if ( view.mLoadTitleOnly )
                            getLoaderManager().restartLoader(i, null, EntryFragment.this);
                        view.mLoadTitleOnly = false;
                    }
                    final String text = String.format( "+%d", mEntryPagerAdapter.getCount() - mLastPagerPos - 1 );
                    Toast toast = Toast.makeText( getContext(), text, Toast.LENGTH_SHORT );
                    TextView textView = new TextView(getContext());
                    textView.setText( text );
                    textView.setPadding( 10, 10, 10, 10 );
                    textView.setBackgroundResource( R.drawable.toast_background );
                    toast.setView( textView );
                    toast.show();
                }

                @Override
                public void onPageScrollStateChanged(int i) {
                }
            });


        rootView.findViewById(R.id.entryNextBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( isArticleTapEnabled() )
                    mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() + 1, false );
                else {
                    PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED, true);
                    TapZonePreviewPreference.SetupZoneSizes(getBaseActivity().mRootView, false);
                    Toast.makeText( MainApplication.getContext(), R.string.tap_actions_were_enabled, Toast.LENGTH_LONG ).show();
                }
            }
        });

        rootView.findViewById(R.id.entryPrevBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() - 1, false );
            }
        });

        TextView.OnClickListener listener = new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                //EntryView entryView = (EntryView) mEntryPager.findViewWithTag("EntryView" + mEntryPager.getCurrentItem());
                PageDown();
            }
        };

        rootView.findViewById(R.id.pageDownBtn).setOnClickListener(listener);
        rootView.findViewById(R.id.pageDownBtnVert).setOnClickListener(listener);
        rootView.findViewById(R.id.pageDownBtn).setOnLongClickListener( new TextView.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final EntryView view = mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() );
                view.ScrollTo( (int) view.GetContentHeight() - view.getHeight() );
                Toast.makeText( v.getContext(), R.string.list_was_scrolled_to_bottom, Toast.LENGTH_SHORT ).show();
                return true;
            }
        });

        rootView.findViewById(R.id.layoutColontitul).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.statusText).setVisibility(View.GONE);

        mLockLandOrientation = getBoolean(STATE_LOCK_LAND_ORIENTATION, false );

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
                    Dog.v( "onTouch ACTION_DOWN " );
                    initialY = (int) event.getY();
                    mWasVibrate = false;
                    mWasSwipe = false;
                    downTime = SystemClock.elapsedRealtime();
                    wasUp = false;
                    UiUtils.RunOnGuiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!wasUp && !mWasSwipe) {
                                mEntryPagerAdapter.GetEntryView(mEntryPager.getCurrentItem()).ScrollTo(0);
                                Toast.makeText(MainApplication.getContext(), R.string.list_was_scrolled_to_top, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, ViewConfiguration.getLongPressTimeout() );
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_MOVE) {
                    Dog.v("onTouch ACTION_MOVE " + (event.getY() - initialY));
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
                    frameStarImage.setImageResource((w >= MAX_HEIGHT) == mFavorite ? R.drawable.ic_star_border_yellow : R.drawable.ic_star_yellow);
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_UP) {
                    Dog.v( "onTouch ACTION_UP " );
                    if ( !mWasSwipe ) {
                        if ( !IsLong() )
                            PageUp();
                    } else if ( event.getY() - initialY >= MAX_HEIGHT ) {
                        SetIsFavourite(!mFavorite);
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
        SetStarFrameWidth(0);
        UpdateFooter();
        SetOrientation();

        return rootView;
    }

    private void SetStarFrameWidth(int w) {
        mStarFrame.setLayoutParams( new FrameLayout.LayoutParams( FrameLayout.LayoutParams.FILL_PARENT, w));
    }



    private void SetOrientation() {
		int or = mLockLandOrientation ?
			   ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE :
			   ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		if ( mLockLandOrientation && or != getActivity().getRequestedOrientation() )
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
            mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true, false);
        }
        HideTapZonesText(getView().getRootView());
        refreshUI( null );
    }


    @Override
    public void onPause() {
        super.onPause();
        EntryView entryView = mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() );
        if (entryView != null) {
            //PrefUtils.putInt(PrefUtils.LAST_ENTRY_SCROLL_Y, entryView.getScrollY());
            entryView.SaveScrollPos();
            PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, getCurrentEntryID());
            PrefUtils.putBoolean(STATE_LOCK_LAND_ORIENTATION, mLockLandOrientation);
        }

        mEntryPagerAdapter.onPause();
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

        {
            MenuItem item = menu.findItem(R.id.menu_star);
            if (mFavorite)
                item.setTitle(R.string.menu_unstar).setIcon(R.drawable.ic_star);
            else
                item.setTitle(R.string.menu_star).setIcon(R.drawable.ic_star_border);
            updateMenuWithIcon(item);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        menu.findItem(R.id.menu_lock_land_orientation).setChecked(mLockLandOrientation);
        menu.findItem(R.id.menu_image_white_background).setChecked(PrefUtils.isImageWhiteBackground());
        menu.findItem(R.id.menu_font_bold).setChecked(PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ));
        menu.findItem(R.id.menu_show_progress_info).setChecked(PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ));
        menu.findItem(R.id.menu_full_screen).setChecked(GetIsStatusBarHidden() );
        menu.findItem(R.id.menu_actionbar_visible).setChecked(!GetIsStatusBarHidden() );
        menu.findItem(R.id.menu_reload_with_tables_toggle).setChecked( mIsWithTables );
        menu.findItem(R.id.menu_menu_by_tap_enabled).setChecked(PrefUtils.isArticleTapEnabled());

        EntryView view = GetSelectedEntryView();
        menu.findItem(R.id.menu_go_back).setVisible( view != null && view.canGoBack() );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mEntriesIds != null) {
            Activity activity = getActivity();

            switch (item.getItemId()) {

                case R.id.menu_close: {
                    onClose();
                    break;
                }

                case R.id.menu_star: {

                    SetIsFavourite( !mFavorite );
                    break;
                }
                case R.id.menu_share: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    if (cursor != null) {
                        String link = cursor.getString(mLinkPos);
                        if (link != null) {
                            String title = cursor.getString(mTitlePos);
                            startActivity(Intent.createChooser(
                                    new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, link)
                                            .setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)
                            ));
                        }
                    }
                    break;
                }

                case R.id.menu_toggle_theme: {
                    mEntryPagerAdapter.GetEntryView( mCurrentPagerPos ).SaveScrollPos();
                    PrefUtils.ToogleTheme(GetActionIntent( Intent.ACTION_VIEW, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID())));
                    return true;
                }

                case R.id.menu_full_screen: {
                    EntryActivity activity1 = (EntryActivity) getActivity();
                    activity1.setFullScreen(!GetIsStatusBarHidden(), GetIsActionBarHidden() );
                    item.setChecked( GetIsStatusBarHidden() );
                    break;
                }
                case R.id.menu_actionbar_visible: {
                    EntryActivity activity1 = (EntryActivity) getActivity();
                    activity1.setFullScreen( GetIsStatusBarHidden(), !GetIsActionBarHidden() );
                    item.setChecked( !GetIsActionBarHidden() );
                    break;
                }

                case R.id.menu_copy_clipboard: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    String link = cursor.getString(mLinkPos);
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Text 1", link);
                    clipboard.setPrimaryClip(clip);

                    UiUtils.toast( getActivity(), R.string.link_was_copied_to_clipboard);
                    break;
                }
                case R.id.menu_mark_as_unread: {
                    mMarkAsUnreadOnFinish = true;
                    CloseEntry();
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getUnreadContentValues(), null, null);
                        }
                    }.start();
                    UiUtils.toast( getActivity(), R.string.entry_marked_unread );
                    break;
                }
                case R.id.menu_load_all_images: {
                    FetcherService.mMaxImageDownloadCount = 0;
                    GetSelectedEntryView().UpdateImages( true );
                    break;
                }
                case R.id.menu_labels: {
                    LabelVoc.INSTANCE.showDialog(getContext(), getCurrentEntryID(), null );
                    break;
                }
                case R.id.menu_go_back: {
                    GetSelectedEntryView().GoBack();
                    break;
                }
                case R.id.menu_go_top: {
                    GetSelectedEntryView().GoTop();
                    break;
                }
                case R.id.menu_share_all_text: {
                    if ( mCurrentPagerPos != -1 ) {
                        Spanned spanned = Html.fromHtml(mEntryPagerAdapter.GetEntryView(mCurrentPagerPos).GetData());
                        char[] chars = new char[spanned.length()];
                        TextUtils.getChars(spanned, 0, spanned.length(), chars, 0);
                        String plainText = new String(chars);
                        plainText = plainText.replaceAll( "body(.)*", "" );
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                        .putExtra(Intent.EXTRA_TEXT, plainText)
                                        .setType(Constants.MIMETYPE_TEXT_PLAIN),
                                getString(R.string.menu_share)));
                    }
                    break;
                }
                case R.id.menu_cancel_refresh: {
                    FetcherService.cancelRefresh();
                    break;
                }
                case R.id.menu_setting: {
                    startActivity(new Intent(getContext(), GeneralPrefsActivity.class));
                    break;
                }
                case R.id.menu_open_link: {
                    Uri uri = Uri.parse( mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mLinkPos) );
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri );
                    getActivity().startActivity(intent);
                    break;
                }

                case R.id.menu_reload_full_text:
                case R.id.menu_reload_full_text_toolbar: {

                    ReloadFullText();
                    break;
                }
                case R.id.menu_reload_full_text_without_mobilizer: {

                    int status = FetcherService.Status().Start("Reload fulltext", true); try {
                        LoadFullText( ArticleTextExtractor.MobilizeType.No, true );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_reload_with_tables_toggle: {
                    int status = FetcherService.Status().Start("Reload with|out tables", true); try {
                        SetIsWithTables( !mIsWithTables );
                        item.setChecked( mIsWithTables );
                        LoadFullText( ArticleTextExtractor.MobilizeType.No, true );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_reload_full_text_with_tags: {

                    int status = FetcherService.Status().Start("Reload fulltext", true); try {
                        GetSelectedEntryView().mIsEditingMode = true;
                        LoadFullText( ArticleTextExtractor.MobilizeType.Tags, true );

                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_lock_land_orientation: {
                    mLockLandOrientation = !mLockLandOrientation;
                    item.setChecked(mLockLandOrientation);
                    SetOrientation();
                    break;
                }
                case R.id.menu_image_white_background: {
                    PrefUtils.toggleBoolean(STATE_IMAGE_WHITE_BACKGROUND, false) ;
                    item.setChecked( PrefUtils.isImageWhiteBackground() );
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true, true);
                    break;
                }
                case R.id.menu_font_bold: {
                    PrefUtils.toggleBoolean(PrefUtils.ENTRY_FONT_BOLD, false);
                    item.setChecked( PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ) );
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true, true);
                    break;
                }
                case R.id.menu_show_progress_info: {
                    PrefUtils.toggleBoolean( SHOW_PROGRESS_INFO, false ) ;
                    item.setChecked( PrefUtils.getBoolean( SHOW_PROGRESS_INFO, false ) );
                    break;
                }
                case R.id.menu_menu_by_tap_enabled: {
                    PrefUtils.toggleBoolean(PREF_ARTICLE_TAP_ENABLED, false);
                    item.setChecked( isArticleTapEnabled() );
                    TapZonePreviewPreference.SetupZoneSizes( getBaseActivity().mRootView, false );
                    if ( !isArticleTapEnabled() )
                        Toast.makeText( getContext(), R.string.tap_actions_were_disabled, Toast.LENGTH_LONG ).show();
                    break;
                }

                case R.id.menu_show_html: {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        final String html = "<root>" + mEntryPagerAdapter.GetEntryView(mCurrentPagerPos).GetData() + "</root>";
                        String htmlFormatted = NetworkUtils.formatXML( html );
                        DebugApp.CreateFileUri("", "html.html", html);
                        MessageBox.Show(htmlFormatted);
                    }
                    break;
                }
                case R.id.menu_add_feed: {
                    startActivity( new Intent( Intent.ACTION_INSERT).setData(FeedColumns.CONTENT_URI ).putExtra(EXTRA_WEB_SEARCH, true) );
                    break;
                }

                case R.id.menu_add_entry_shortcut: {
                    if ( ShortcutManagerCompat.isRequestPinShortcutSupported(getContext()) ) {
                        //Adding shortcut for MainActivity on Home screen
                        Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                        if (cursor != null) {
                            final String name = cursor.getString(mTitlePos);
                            final long entryID = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                            //final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                            final Uri uri = Uri.parse( cursor.getString(mLinkPos) );
                            final String iconUrl = cursor.getString(cursor.getColumnIndex( EntryColumns.IMAGE_URL ));

                            new WaitDialog(getActivity(), R.string.downloadImage, new Runnable() {
                                @Override
                                public void run() {

                                    final Bitmap bitmap = iconUrl != null ? NetworkUtils.downloadImage(iconUrl) : null;
                                    if ( bitmap == null )
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText( getContext(), R.string.unable_to_load_article_icon, Toast.LENGTH_LONG ).show();
                                            }
                                        });
                                    final IconCompat icon = (bitmap == null) ?
                                        IconCompat.createWithResource( getContext(), R.mipmap.ic_launcher ) :
                                        IconCompat.createWithBitmap(bitmap);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(getContext(), String.valueOf(entryID))
                                                    .setIcon(icon)
                                                    .setShortLabel(name)
                                                    .setIntent(new Intent(getContext(), EntryActivity.class).setAction(Intent.ACTION_VIEW).setData(uri))
                                                    .build();

                                            ShortcutManagerCompat.requestPinShortcut(getContext(), pinShortcutInfo, null);
                                            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)
                                                Toast.makeText(getContext(), R.string.new_entry_shortcut_added, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }).execute();
                        }
                    } else
                        Toast.makeText( getContext(), R.string.new_feed_shortcut_add_failed, Toast.LENGTH_LONG ).show();
                    break;
                }

            }
        }

        return true;
    }


    private void ReloadFullText() {
        int status = FetcherService.Status().Start("Reload fulltext", true);
        GetSelectedEntryView().mIsEditingMode = false;
        try {
            LoadFullText( ArticleTextExtractor.MobilizeType.Yes, true );
        } finally { FetcherService.Status().End( status ); }
    }

    private void SetIsWithTables(final boolean withTables) {
        if ( mIsWithTables == withTables )
            return;
        mIsWithTables = withTables;
        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        ContentValues values = new ContentValues();
        values.put(EntryColumns.IS_WITH_TABLES, mIsWithTables? 1 : 0);
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.update(uri, values, null, null);
        getActivity().invalidateOptionsMenu();
    }
    private void SetIsFavourite(final boolean favorite) {
        if ( mFavorite == favorite )
            return;
        mFavorite = favorite;
        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        new Thread() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, mFavorite ? 1 : 0);
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(uri, values, null, null);
            }
        }.start();
        getActivity().invalidateOptionsMenu();
        Toast.makeText( getContext(), mFavorite ? R.string.entry_marked_favourite : R.string.entry_marked_unfavourite, Toast.LENGTH_LONG ).show();
    }

//    private void DeleteMobilized() {
//        FileUtils.INSTANCE.deleteMobilized( ContentUris.withAppendedId(mBaseUri, getCurrentEntryID() ) );
//    }

    private void CloseEntry() {
        PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        getActivity().finish();
    }

    public long getCurrentEntryID() {
        Entry entry = GetEntry( mCurrentPagerPos );
        if ( entry != null )
            return entry.mID;
        else
            return -1;
    }

    private String getCurrentEntryLink() {
        Entry entry = GetEntry( mCurrentPagerPos );
        if ( entry != null )
            return entry.mLink;
        else
            return "";
    }

    @SuppressLint("StaticFieldLeak")
    public void setData(final Uri uri) {
        mCurrentPagerPos = -1;
        mBaseUri = null;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Timer timer = new Timer( "EntryFr.setData" );
                Dog.v( String.format( "EntryFragment.setData( %s )", uri == null ? "" : uri.toString() ) );


                //PrefUtils.putString( PrefUtils.LAST_URI, uri.toString() );

                if ( !IsExternalLink( uri ) ) {
                    mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
                    Dog.v(String.format("EntryFragment.setData( %s ) baseUri = %s", uri.toString(), mBaseUri));
                    try {
                        mInitialEntryId = Long.parseLong(uri.getLastPathSegment());
                    } catch (Exception unused) {
                        mInitialEntryId = -1;
                    }


                    String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;

                    final ContentResolver cr = MainApplication.getContext().getContentResolver();
                    // Load the entriesIds list. Should be in a loader... but I was too lazy to do so
                    Cursor entriesCursor = cr.query( mBaseUri, EntryColumns.PROJECTION_ID, mWhereSQL, null, EntryColumns.DATE + entriesOrder);

                    if (entriesCursor != null && entriesCursor.getCount() > 0) {
                        synchronized ( this ) {
                            mEntriesIds = new Entry[entriesCursor.getCount()];
                        }
                        int i = 0;
                        while (entriesCursor.moveToNext()) {
                            SetEntryID( i, entriesCursor.getLong(0), entriesCursor.getString(1) );
                            if (GetEntry( i ).mID == mInitialEntryId) {
                                mCurrentPagerPos = i; // To immediately display the good entry
                                mLastPagerPos = i;
                                CancelStarNotification(getCurrentEntryID());
                            }
                            i++;
                        }

                        entriesCursor.close();
                    }
                    if ( IsFeedUri( mBaseUri ) ) {
                        Dog.v( "EntryFragment.setData() mBaseUri.getPathSegments[1] = " + mBaseUri.getPathSegments().get(1) );
                        final String feedId = mBaseUri.getPathSegments().get(1);
                        if (feedId.equals(FetcherService.GetExtrenalLinkFeedID())) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Dog.v("EntryFragment.setData() update time to current");
                                    ContentValues values = new ContentValues();
                                    values.put(EntryColumns.DATE, (new Date()).getTime());
                                    cr.update(uri, values, null, null);
                                }
                            }).start();
                        }
                        mFilters = new FeedFilters(feedId);
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
                mEntryPager.setAdapter(mEntryPagerAdapter);
                if (mCurrentPagerPos != -1) {
                    mEntryPager.setCurrentItem(mCurrentPagerPos);
                }
            }
        }.execute();
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
    private void refreshUI(Cursor entryCursor) {
        EntryView view = GetSelectedEntryView();
        if ( view != null )
            mBtnEndEditing.setVisibility( view.mIsEditingMode ? View.VISIBLE : View.GONE );
        mBtnEndEditing.setBackgroundColor( Theme.GetToolBarColorInt() );
        try {
			if (entryCursor != null ) {
				//String feedTitle = entryCursor.isNull(mFeedNamePos) ? entryCursor.getString(mFeedUrlPos) : entryCursor.getString(mFeedNamePos);
				EntryActivity activity = (EntryActivity) getActivity();
				activity.setTitle("");//activity.setTitle(feedTitle);

				mFavorite = entryCursor.getInt(mIsFavoritePos) == 1;
                mIsWithTables = entryCursor.getInt(mIsWithTablePos) == 1;
                //mRetrieveFullText = entryCursor.getInt( mRetrieveFullTextPos ) == 1;

                activity.invalidateOptionsMenu();

				final long currentEntryID = getCurrentEntryID();
				mStatusText.SetEntryID( String.valueOf( currentEntryID ) );
				// Listen the mobilizing task

                new Thread() {
                    long mID;
                    @Override
                    public void run() {
                        if (FetcherService.hasMobilizationTask( currentEntryID )) {
                            //--showSwipeProgress();
                            // If the service is not started, start it here to avoid an infinite loading
                            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false))
                                FetcherService.StartService( new Intent(MainApplication.getContext(), FetcherService.class)
                                        .setAction(FetcherService.ACTION_MOBILIZE_FEEDS), true);
                        }
                    }
                    Thread SetID( long id ) {
                        mID = id;
                        return this;
                    }
                }.SetID( currentEntryID ).start();


				// Mark the previous opened article as read
				//if (entryCursor.getInt(mIsReadPos) != 1) {
				if ( !mMarkAsUnreadOnFinish && mLastPagerPos != -1 ) {
                    class ReadAndOldWriter implements Runnable {
                        private final boolean mSetAsRead;
                        private final int mPagerPos;
                        private ReadAndOldWriter(int pagerPos, boolean setAsRead){
                            mPagerPos = pagerPos;
                            mSetAsRead = setAsRead;
                        }
					    @Override
                        public void run() {
                            final Uri uri = ContentUris.withAppendedId(mBaseUri, GetEntry( mPagerPos ).mID);
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            if ( mSetAsRead )
                                cr.update(uri, FeedData.getReadContentValues(), EntryColumns.WHERE_UNREAD, null);
                            cr.update(uri, FeedData.getOldContentValues(), EntryColumns.WHERE_NEW, null);
                            /*// Update the cursor
                            Cursor updatedCursor = cr.query(uri, null, null, null, null);
                            updatedCursor.moveToFirst();
                            mEntryPagerAdapter.setUpdatedCursor(mPagerPos, updatedCursor);*/
                        }
                    }
                    new Thread(new ReadAndOldWriter( mLastPagerPos, mEntryPagerAdapter.getCursor(mLastPagerPos).getInt(mIsReadPos) != 1 )).start();
                }
            }
		} catch ( IllegalStateException e ) {
			e.printStackTrace();
		}
	}

    private void showEnclosure(Uri uri, String enclosure, int position1, int position2) {
        try {
            startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + DrawerAdapter.FIRST_ENTRY_POS(), position2)), 0);
        } catch (Exception e) {
            try {
                startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode - let the browser handle this
            } catch (Throwable t) {
                UiUtils.showMessage(getActivity(), t.getMessage());
            }
        }
    }

    /*private void setImmersiveFullScreen(boolean fullScreen) {
        BaseActivity activity = (BaseActivity) getActivity();
        if ( fullScreen )
            mToggleStatusBarVisbleBtn.setVisibility(View.VISIBLE);
        else
            mToggleStatusBarVisbleBtn.setVisibility(View.GONE);
        activity.setImmersiveFullScreen(fullScreen);
    }*/

    public void UpdateFooter() {
        EntryView entryView = mEntryPagerAdapter.GetEntryView(mEntryPager.getCurrentItem());
        int webViewHeight = 0;
        int contentHeight = 0;
        if (entryView != null) {
            webViewHeight = entryView.getMeasuredHeight();
            contentHeight = (int) Math.floor(entryView.getContentHeight() * entryView.getScale());
        }
        getBaseActivity().UpdateHeader(contentHeight - webViewHeight,
                                       entryView == null ? 0 : entryView.getScrollY(),
                                       GetIsStatusBarHidden(),
                                       GetIsActionBarHidden() );
    }


    @Override
    public void onClickOriginalText() {
        getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                mIsFullTextShown = false;
                mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true, true);
            }
        });
    }

    @Override
    public void onClickFullText() {
        final BaseActivity activity = (BaseActivity) getActivity();

        Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
        final boolean alreadyMobilized = FileUtils.INSTANCE.isMobilized(cursor.getString( mLinkPos ), cursor );

        if (alreadyMobilized) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIsFullTextShown = true;
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true, true);
                }
            });
        } else /*--if (!isRefreshing())*/ {
            LoadFullText( ArticleTextExtractor.MobilizeType.Yes, false );
        }
    }

    private void LoadFullText(final ArticleTextExtractor.MobilizeType mobilize, final boolean isForceReload ) {
        final BaseActivity activity = (BaseActivity) getActivity();
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        // since we have acquired the networkInfo, we use it for basic checks
        if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            //FetcherService.addEntriesToMobilize(new Long[]{mEntriesIds[mCurrentPagerPos]});
            //activity.startService(new Intent(activity, FetcherService.class)
            //                      .setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
            new Thread() {
                @Override
                public void run() {
                    int status = FetcherService.Status().Start(getActivity().getString(R.string.loadFullText), true); try {
                        FetcherService.mobilizeEntry(getCurrentEntryID(),
                                                     mFilters,
                                                     mobilize,
                                                     FetcherService.AutoDownloadEntryImages.Yes,
                                                     true,
                                                     true,
                                                     isForceReload,
                                                     true );
                    } finally { FetcherService.Status().End( status ); }
                }
            }.start();
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UiUtils.showMessage(getActivity(), R.string.network_error);
                }
            });
        }
    }

    @Override
    public void onReloadFullText() {
        ReloadFullText();
    }

    @Override
    public void onClose() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( getActivity() != null )
                    getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onClickEnclosure() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String enclosure = mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mEnclosurePos);

                final int position1 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + DrawerAdapter.FIRST_ENTRY_POS());

                final Uri uri = Uri.parse(enclosure.substring(0, position1));
                final String filename = uri.getLastPathSegment();

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.open_enclosure)
                        .setMessage(getString(R.string.file) + ": " + filename)
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
                            UiUtils.showMessage(getActivity(), R.string.error);
                        }
                    }
                }).show();
            }
        });
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
        View layout = getView();
        return (layout == null ? null : (FrameLayout) layout.findViewById(R.id.videoLayout));
    }

    public void removeClass(String className) {
        final String oldPref = PrefUtils.getString( PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, "" );
        if ( !PrefUtils.GetRemoveClassList().contains( className ) ) {
            PrefUtils.putStringCommit(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, oldPref + "\n" + className);
        }
        ActionAfterRulesEditing();
    }

    private void ActionAfterRulesEditing() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GetSelectedEntryView().UpdateTags();
            }
        });
    }

    public void returnClass(String classNameList) {
        final ArrayList<String> list = PrefUtils.GetRemoveClassList();
        boolean needRefresh = false;
        for ( String className: TextUtils.split( classNameList, " " ) )
            if ( list.contains( className ) ) {
                needRefresh = true;
                list.remove( className );
            }
        if ( needRefresh )
            PrefUtils.putStringCommit(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, TextUtils.join( "\n", list ) );
        ActionAfterRulesEditing();
    }

    @Override
    public void openTagMenu(final String className, final String baseUrl, final String paramValue ) {
        ScrollView scroll = new ScrollView(getContext() );
        final LinearLayout parent = new LinearLayout(getContext() );
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setGravity(Gravity.CENTER);
        UiUtils.AddText( parent, null, getString( R.string.open_tag_menu_hint ) ).setTextColor( Theme.GetColorInt( TEXT_COLOR_READ, R.string.default_read_color ));
        final RadioGroup groupUrl = new RadioGroup(getContext() );
        //groupUrl.setGravity( Gravity.CENTER );
        parent.addView(groupUrl);
        int id = 0;
        String keyUrl = baseUrl.replaceAll( "http.+?//", "" ).replaceAll( "www.", "" );
        if ( !keyUrl.endsWith( "/" ) )
            keyUrl = keyUrl + "/";//.substring( 0, keyUrl.length() - 1 );
        while( keyUrl.contains( "/" ) ) {
            keyUrl = keyUrl.substring( 0, keyUrl.lastIndexOf( "/" ) );
            id++;
            RadioButton btn = new RadioButton(getContext());
            btn.setText( keyUrl );
            btn.setTag( keyUrl );
            btn.setId( id );
            groupUrl.addView( btn );
            btn.setChecked( true );
        }

        scroll.addView(parent);
        scroll.setPadding( 0, 0, 0, 20 );

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext() );
        builder.setView( scroll );
        builder.setTitle( getString( R.string.open_tag_menu_dialog_title ) + className );

        final AlertDialog dialog = builder.show();
        AddActionButton(parent, R.string.setFullTextRoot, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFullTextRoot(GetSelectedUrlPart(groupUrl), className);
                dialog.dismiss();
            }
        });
        AddActionButton(parent, paramValue.equals("hide") ? R.string.hide : R.string.show, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( paramValue.equals( "hide" ) )
                    removeClass( className );
                else if ( paramValue.equals( "show" ) )
                    returnClass( className );
                dialog.dismiss();
            }
        });
        AddActionButton(parent, R.string.set_category, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCategory(GetSelectedUrlPart(groupUrl), className);
                dialog.dismiss();
            }
        });

        AddActionButton(parent, R.string.set_date, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDate(GetSelectedUrlPart(groupUrl), className);
                dialog.dismiss();
            }
        });

        AddActionButton(parent, R.string.copyClassNameToClipboard, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(className);
                dialog.dismiss();
            }
        });

        AddActionButton(parent, android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private String GetSelectedUrlPart(RadioGroup groupUrl) {
        return (String) groupUrl.findViewById(groupUrl.getCheckedRadioButtonId() ).getTag();
    }

    private void AddActionButton(LinearLayout parent, int captionID, View.OnClickListener listener) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                               LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(20, 20, 20, 20);
        lp.gravity = Gravity.CENTER;
        TextView view = UiUtils.AddText(parent, lp, getString(captionID));
        view.setBackgroundResource( R.drawable.btn_background );
        view.setOnClickListener( listener );
    }

    private void copyToClipboard(String text) {
        ((android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE)).setText(text);
        Toast.makeText(getActivity(), getActivity().getString( R.string.text_was_copied_to_clipboard ) + ": " + text, Toast.LENGTH_LONG).show();
    }
    private void setFullTextRoot(String baseUrl, String className) {
        setClassObject(baseUrl, className, CONTENT_TEXT_ROOT_EXTRACT_RULES, getString(R.string.full_text_root_default ) );
    }
    private void setCategory(String baseUrl, String className) {
        setClassObject(baseUrl, className, CATEGORY_EXTRACT_RULES, "" );
    }
    private void setDate(String baseUrl, String className) {
        setClassObject(baseUrl, className, DATE_EXTRACT_RULES, "" );
    }
    private void setClassObject(String baseUrl, String className, String prefKey, String defaultPrefValue) {
        ArrayList<String> ruleList = HtmlUtils.Split( PrefUtils.getString(prefKey, defaultPrefValue ),
                Pattern.compile( "\\n|\\s" ) );
        int index = -1;
        for( int i = 0; i < ruleList.size(); i++ ) {
            final String line = ruleList.get( i );
            final String[] list1 = line.split(":");
            final String url = list1[0];
            if ( url.equals( baseUrl ) ) {
                index = i;
                break;
            }
        }
        final String newRule = baseUrl + ":class=" + className;
        if ( index != -1 )
            ruleList.remove(index );
        ruleList.add( 0, newRule );
        PrefUtils.putStringCommit(prefKey, TextUtils.join("\n", ruleList ) );
        ActionAfterRulesEditing();
    }


    @Override
    public void downloadImage(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FetcherService.mCancelRefresh = false;
                int status = FetcherService.Status().Start( getString(R.string.downloadImage), true ); try {
                    NetworkUtils.downloadImage(getCurrentEntryID(), getCurrentEntryLink(), url, false, true);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    FetcherService.Status().End( status );
                }

            }
        }).start();
    }

    @Override
    public void downloadNextImages() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
            FetcherService.mMaxImageDownloadCount += PrefUtils.getImageDownloadCount();
            GetSelectedEntryView().UpdateImages( true );
            }
        });

    }

    @Override
    public void downloadAllImages() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FetcherService.mMaxImageDownloadCount = 0;
                GetSelectedEntryView().UpdateImages( true );
            }
        });

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timer.Start( id, "EntryFr.onCreateLoader" );
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(GetEntry( id ).mID), null, null, null, null);
        cursorLoader.setUpdateThrottle(100);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if ( mIsFinishing )
            return;
        Timer.End( loader.getId() );
        if (cursor != null) { // can be null if we do a setData(null) before
            try {
                if ( cursor.moveToFirst() ) {

                    if (mTitlePos == -1) {
                        mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                        mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
                        mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
                        mLinkPos = cursor.getColumnIndex(EntryColumns.LINK);
                        mIsFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                        mIsWithTablePos= cursor.getColumnIndex(EntryColumns.IS_WITH_TABLES);
                        mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
                        mIsNewPos = cursor.getColumnIndex(EntryColumns.IS_NEW);
                        mIsWasAutoUnStarPos = cursor.getColumnIndex(EntryColumns.IS_WAS_AUTO_UNSTAR);
                        mEnclosurePos = cursor.getColumnIndex(EntryColumns.ENCLOSURE);
                        mFeedIDPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
                        mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
                        mScrollPosPos = cursor.getColumnIndex(EntryColumns.SCROLL_POS);
                        mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
                        mFeedUrlPos = cursor.getColumnIndex(FeedColumns.URL);
                        mFeedIconUrlPos = cursor.getColumnIndex(FeedColumns.ICON_URL);
                        mRetrieveFullTextPos = cursor.getColumnIndex(FeedColumns.RETRIEVE_FULLTEXT);
                    }

                    int position = loader.getId();
                    if (position != -1) {
                        FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
                        mEntryPagerAdapter.displayEntry(position, cursor, false, false);
                        mRetrieveFullText = cursor.getInt(mRetrieveFullTextPos) == 1;
                        //if (getBoolean(DISPLAY_ENTRIES_FULLSCREEN, false))
                        //    activity.setFullScreen(true, true);
                        if ( position == mCurrentPagerPos ) {
                            EntryView view = mEntryPagerAdapter.GetEntryView( position );
                            if ( view.mLoadTitleOnly ) {
                                view.mLoadTitleOnly = false;
                                getLoaderManager().restartLoader(position, null, EntryFragment.this);
                            }
                        }
                    }
                }
            } catch ( IllegalStateException e ) {
                FetcherService.Status().SetError( e.getMessage(), "", String.valueOf( getCurrentEntryID() ), e );
                Dog.e("Error", e);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEntryPagerAdapter.setUpdatedCursor(loader.getId(), null);
    }



    /*@Override
    public void onRefresh() {
        // Nothing to do
    }*/

    public abstract class BaseEntryPagerAdapter extends PagerAdapter {
        final SparseArray<EntryView> mEntryViews = new SparseArray<>();

        Cursor getCursor(int pagerPos) {
            EntryView view = GetEntryView( pagerPos );
            if (view != null ) {
                return (Cursor) view.getTag();
            }
            return null;
        }
        void setUpdatedCursor(int pagerPos, Cursor newCursor) {
            EntryView view = GetEntryView( pagerPos );
            if (view != null ) {
                Cursor previousUpdatedOne = (Cursor) view.getTag();
                if (previousUpdatedOne != null) {
                    previousUpdatedOne.close();
                }
                view.setTag(newCursor);
            }
        }
        @Override
        public boolean isViewFromObject(@NotNull View view, Object object) {
            return view == object;
        }

        void displayEntry(int pagerPos, Cursor newCursor, boolean forceUpdate, boolean invalidateCache ) {
            Dog.d( "EntryPagerAdapter.displayEntry" + pagerPos);

            EntryView view = GetEntryView( pagerPos );
            if (view != null ) {
                view.StatusStartPageLoading();
                if ( invalidateCache  )
                    view.InvalidateContentCache();
                if (newCursor == null) {
                    newCursor = (Cursor) view.getTag(); // get the old one
                }

                if (newCursor != null && newCursor.moveToFirst()  ) {
                    view.setTag(newCursor);

                    if ( mSetupChanged )
                        view.InvalidateContentCache();
                    //FetcherService.setCurrentEntryID( getCurrentEntryID() );
                    mIsFullTextShown =  view.setHtml( GetEntry( pagerPos ).mID,
                                                      newCursor,
                                                      mFilters,
                                                      mIsFullTextShown,
                                                      forceUpdate,
                                                      (EntryActivity) getActivity() );

                    if (pagerPos == mCurrentPagerPos) {
                        refreshUI(newCursor);

                        Dog.v( String.format( "displayEntry view.mScrollY  (entry %s) view.mScrollY = %f", getCurrentEntryID(),  view.mScrollPartY ) );

                    }
                    UpdateFooter();
                }
            }
        }

        void onResume() {
            for (int i = 0; i < mEntryViews.size(); i++) {
                mEntryViews.valueAt(i).onResume();
            }
        }

        void onPause() {
            for (int i = 0; i < mEntryViews.size(); i++) {
                mEntryViews.valueAt(i).onPause();
            }
        }

        EntryView GetEntryView(int pagerPos) {
            return mEntryViews.get(pagerPos);
        }
        public EntryView GetEntryView( Entry entry ) {
            for(int i = 0; i < mEntryViews.size(); i++) {
                EntryView view = mEntryViews.get(mEntryViews.keyAt(i));
                if ( view.mEntryId == entry.mID )
                    return view;
            }
            return null;
        }


        @Override
        public void destroyItem(ViewGroup container, final int position, @NotNull Object object) {
            Dog.d( "EntryPagerAdapter.destroyItem " + position );
            FetcherService.removeActiveEntryID( GetEntry( position ).mID );
            getLoaderManager().destroyLoader(position);
            container.removeView((View) object);
            mEntryViews.delete(position);
        }



        @NotNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Dog.d( "EntryPagerAdapter.instantiateItem" + position );
            final EntryView view = CreateEntryView();
            mEntryViews.put(position, view);
            container.addView(view);
            view.mLoadTitleOnly = true;
            Entry entry = GetEntry( position );
            if ( entry != null ) {
                FetcherService.addActiveEntryID(entry.mID);
                getLoaderManager().restartLoader(position, null, EntryFragment.this);
            }
            return view;
        }
    }

    public class EntryPagerAdapter extends BaseEntryPagerAdapter {


        EntryPagerAdapter() { }

        @Override
        public int getCount() {
            synchronized ( this ) {
                return mEntriesIds != null ? mEntriesIds.length : 0;
            }
        }


        void setUpdatedCursor(int pagerPos, Cursor newCursor) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null ) {
                    Cursor previousUpdatedOne = (Cursor) view.getTag();
                    if (previousUpdatedOne != null) {
                        previousUpdatedOne.close();
                    }
                view.setTag(newCursor);
            }
        }

    }

    public class SingleEntryPagerAdapter extends BaseEntryPagerAdapter {
        SingleEntryPagerAdapter() {

        }

        @Override
        public int getCount() {
            return 1;
        }


        @NotNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final EntryView view = (EntryView) super.instantiateItem(container, position);
            view.mLoadTitleOnly = false;
            return view;
        }
    }

    @NonNull
    private EntryView CreateEntryView() {
        final EntryView view = new EntryView(getActivity());
        if ( mLeakEntryView == null )
            mLeakEntryView  = view;
        view.setListener(EntryFragment.this);
        view.setTag(null);

        view.mScrollChangeListener = new Runnable(){
            @Override
            public void run() {
                if ( !mFavorite )
                    return;
                if ( mRetrieveFullText && !mIsFullTextShown )
                    return;
                if ( !PrefUtils.getBoolean("entry_auto_unstart_at_bottom", true) )
                    return;
                if ( view.mWasAutoUnStar )
                    return;
                if ( !view.mContentWasLoaded )
                    return;
                if ( view.IsScrollAtBottom() && new Date().getTime() - view.mLastSetHTMLTime > MILLS_IN_SECOND * 5 ) {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    view.mWasAutoUnStar = true;
                    new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();
                            values.put(EntryColumns.IS_WAS_AUTO_UNSTAR, 1);
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, values, null, null);
                        }
                    }.start();
                    SetIsFavourite(false);
                }
            }
        };
        view.StatusStartPageLoading();
        return view;
    }

    public void SetEntryID( int position, long entryID, String entryLink )  {
        synchronized ( this ) {
            mEntriesIds[position] = new Entry( entryID, entryLink );
        }
    }
    private Entry GetEntry(int position)  {
        synchronized ( this ) {
            if ( position >= 0 && position < mEntriesIds.length )
                return mEntriesIds[position];
            else
                return null;
        }
    }

    private EntryView GetSelectedEntryView()  {
        return mEntryPagerAdapter.GetEntryView(mCurrentPagerPos);
    }

}

