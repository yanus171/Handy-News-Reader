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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.GeneralPrefsActivity;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.utils.PrefUtils.DISPLAY_ENTRIES_FULLSCREEN;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;


public class EntryFragment extends /*SwipeRefresh*/Fragment implements LoaderManager.LoaderCallbacks<Cursor>, EntryView.EntryViewManager {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";
    private static final String STATE_LOCK_LAND_ORIENTATION = "STATE_LOCK_LAND_ORIENTATION";
    public static final String NO_DB_EXTRA = "NO_DB_EXTRA";


    private int mTitlePos = -1, mDatePos, mMobilizedHtmlPos, mAbstractPos, mLinkPos, mIsFavoritePos, mIsReadPos, mIsNewPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconPos, mFeedIDPos, mScrollPosPos, mRetrieveFullTextPos;


    private int mCurrentPagerPos = -1, mLastPagerPos = -1;
    private Uri mBaseUri;
    private long mInitialEntryId = -1;
    private long[] mEntriesIds = new long[1];

    private boolean mFavorite, mIsFullTextShown = true;

    public ViewPager mEntryPager;
    public BaseEntryPagerAdapter mEntryPagerAdapter;

    private View mStarFrame;
    private ProgressBar mProgressBar;
    private TextView mLabelClock;

    private StatusText mStatusText = null;

    private boolean mLockLandOrientation = false;

    public boolean mMarkAsUnreadOnFinish = false;
    private boolean mRetrieveFullText = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        if (IsExternalLink( getActivity().getIntent().getData() )) //getBooleanExtra(NO_DB_EXTRA, false ) )
            mEntryPagerAdapter = new SingleEntryPagerAdapter();
        else
            mEntryPagerAdapter = new EntryPagerAdapter();

        super.onCreate(savedInstanceState);
    }

    private boolean IsExternalLink( Uri uri ) {
        return uri == null || uri.toString().startsWith( "http" );
    }

    //@Override
    //public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry, container, true);
        TapZonePreviewPreference.SetupZoneSizes( rootView );

        mStatusText = new StatusText( (TextView)rootView.findViewById( R.id.statusText ),
                                      (TextView)rootView.findViewById( R.id.errorText ),
                                      FetcherService.Status()/*,
                                      this*/);
        rootView.findViewById(R.id.toggleFullscreenBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen( EntryActivity.GetIsStatusBarHidden(), !EntryActivity.GetIsActionBarHidden() );
            }
        });

        mProgressBar = rootView.findViewById(R.id.progressBar);
        mProgressBar.setProgress( 0 );

        mLabelClock = rootView.findViewById(R.id.textClock);
        mLabelClock.setText("");

        rootView.findViewById(R.id.toggleFullScreenStatusBarBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen(!EntryActivity.GetIsStatusBarHidden(), EntryActivity.GetIsActionBarHidden());
            }
        });

        mEntryPager = rootView.findViewById(R.id.pager);
        //mEntryPager.setPageTransformer(true, new DepthPageTransformer());
        mEntryPager.setAdapter(mEntryPagerAdapter);

        if (savedInstanceState != null) {
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            //mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID);
            //mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS);
            mEntryPager.getAdapter().notifyDataSetChanged();
            mEntryPager.setCurrentItem(mCurrentPagerPos);
            mLastPagerPos = mCurrentPagerPos;
        }

        if ( mEntryPagerAdapter instanceof EntryPagerAdapter )
            mEntryPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int i, float v, int i2) {
                }

                @Override
                public void onPageSelected(int i) {
                    mCurrentPagerPos = i;
                    mEntryPagerAdapter.onPause(); // pause all webviews
                    mEntryPagerAdapter.onResume(); // resume the current webview

                    PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID()).toString());

                    CancelStarNotification( getCurrentEntryID() );

                    refreshUI(( ( EntryPagerAdapter )mEntryPagerAdapter ).getCursor(i));
                    mLastPagerPos = i;

                }

                @Override
                public void onPageScrollStateChanged(int i) {
                }
            });


        rootView.findViewById(R.id.entryNextBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() + 1, false );
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

        rootView.findViewById(R.id.pageUpBtn).setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                PageUp();
            }
        });

        //disableSwipe();


        rootView.findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.statusText).setVisibility(View.GONE);

        mLockLandOrientation = PrefUtils.getBoolean(STATE_LOCK_LAND_ORIENTATION, false );

        final Vibrator vibrator = (Vibrator) getContext().getSystemService( Context.VIBRATOR_SERVICE );
        mStarFrame = rootView.findViewById(R.id.frameStar);
        final ImageView frameStarImage  = rootView.findViewById(R.id.frameStarImage);
        final boolean prefVibrate = PrefUtils.getBoolean(VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE, true);
        rootView.findViewById(R.id.pageUpBtn).setOnTouchListener(new View.OnTouchListener() {
            private int initialy = 0;
            private boolean mWasVibrate = false;
            private boolean mWasSwipe = false;
            private final int MAX_HEIGHT = UiUtils.mmToPixel( 12 );
            private final int MIN_HEIGHT = UiUtils.mmToPixel( 1 );
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if ( event.getAction() == MotionEvent.ACTION_DOWN) {
                    Dog.v( "onTouch ACTION_DOWN " );
                    //initialx = (int) event.getX();
                    initialy = (int) event.getY();
                    mWasVibrate = false;
                    mWasSwipe = false;
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_MOVE) {
                    Dog.v( "onTouch ACTION_MOVE " + ( event.getY() - initialy ) );
                    int w = Math.max( 0, (int) (event.getY() - initialy) );
                    SetStarFrameWidth( Math.min( w, MAX_HEIGHT ) );
                    if ( prefVibrate && w >= MAX_HEIGHT && !mWasVibrate ) {
                        mWasVibrate = true;
                        vibrator.vibrate(VIBRATE_DURATION);
                    } else if ( w < MAX_HEIGHT )
                        mWasVibrate = false;
                    if ( w >= MIN_HEIGHT )
                        mWasSwipe = true;

                    frameStarImage.setImageResource( ( w >= MAX_HEIGHT ) == mFavorite ? R.drawable.star_empty_gray : R.drawable.star_yellow );
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_UP) {
                    Dog.v( "onTouch ACTION_UP " );
                    if ( !mWasSwipe ) {
                        PageUp();
                    } else if ( event.getY() - initialy >= MAX_HEIGHT ) {
                        SetIsFavourite(!mFavorite);
                    }
                    SetStarFrameWidth(0);
                    return true;
                } else
                    SetStarFrameWidth(0);
                return false;
            }


        });
        SetStarFrameWidth(0);

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
        mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() ).PageChange(-1);
    }

    public void PageDown() {
        mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem() ).PageChange(1);
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
        outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
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
        super.onDestroy();
    }
    @Override
    public void onResume() {
        super.onResume();
        mEntryPagerAdapter.onResume();
        mMarkAsUnreadOnFinish = false;
        if ( GeneralPrefsFragment.mSetupChanged ) {
            GeneralPrefsFragment.mSetupChanged = false;
            mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);


        }

        final boolean tapZonesVisible = PrefUtils.getBoolean( PrefUtils.TAP_ZONES_VISIBLE, true );
        View rootView = getView().getRootView();
        UiUtils.HideButtonText(rootView, R.id.pageDownBtnVert, true);
        UiUtils.HideButtonText(rootView, R.id.pageDownBtn, true);
        UiUtils.HideButtonText(rootView, R.id.pageUpBtn, true);
        UiUtils.HideButtonText(rootView, R.id.entryNextBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.entryPrevBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.toggleFullScreenStatusBarBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.toggleFullscreenBtn, !tapZonesVisible);

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
            ImageSpan imageSpan = new ImageSpan(drawable);
            builder.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(builder);
        }
    }

    // -------------------------------------------------------------------------
    private static int DpToPx(float dp) {
        Resources r = MainApplication.getContext().getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return (int) px;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry, menu);

        //int color = ContextCompat.getColor(getContext(), R.color.common_google_signin_btn_text_dark);
        //updateMenuWithIcon(menu.findItem(R.id.menu_mark_as_favorite));
        //updateMenuWithIcon(menu.findItem(R.id.menu_mark_as_unfavorite));
        updateMenuWithIcon(menu.findItem(R.id.menu_reload_full_text));
        //updateMenuWithIcon(menu.findItem(R.id.menu_reload_full_text_without_mobilizer));
        //updateMenuWithIcon(menu.findItem(R.id.menu_reload_full_text_with_tags));
        updateMenuWithIcon(menu.findItem(R.id.menu_load_all_images));
        updateMenuWithIcon(menu.findItem(R.id.menu_share_all_text));
        updateMenuWithIcon(menu.findItem(R.id.menu_open_link));
        updateMenuWithIcon(menu.findItem(R.id.menu_cancel_refresh));
        updateMenuWithIcon(menu.findItem(R.id.menu_setting));

        //EntryActivity activity = (EntryActivity) getActivity();
        menu.findItem(R.id.menu_star).setShowAsAction( EntryActivity.GetIsActionBarHidden() ? MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW : MenuItem.SHOW_AS_ACTION_IF_ROOM );

        {
            MenuItem item = menu.findItem(R.id.menu_star);
            if (mFavorite)
                item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
            else
                item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
            updateMenuWithIcon(item);
        }
        //menu.findItem(R.id.menu_mark_as_favorite).setVisible( !mFavorite );
        //menu.findItem(R.id.menu_mark_as_unfavorite).setVisible(mFavorite);

        menu.findItem(R.id.menu_lock_land_orientation).setChecked(mLockLandOrientation);

//        if (mFavorite)
//            menu.findItem(R.id.menu_mark_as_favorite ).setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
//        else
//            menu.findItem(R.id.menu_mark_as_unfavorite).setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mEntriesIds != null) {
            Activity activity = getActivity();

            switch (item.getItemId()) {
                case R.id.menu_star: {

                    SetIsFavourite( !mFavorite );

                    if ( !mFavorite )
                        CloseEntry();
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
                    PrefUtils.ToogleTheme(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID())));
                    return true;
                }

                case R.id.menu_full_screen: {
                    EntryActivity activity1 = (EntryActivity) getActivity();
                    activity1.setFullScreen( true, true );
                    break;
                }

                case R.id.menu_copy_clipboard: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    String link = cursor.getString(mLinkPos);
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Text 1", link);
                    clipboard.setPrimaryClip(clip);

                    UiUtils.toast( getActivity(), R.string.copied_clipboard);
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
                /*case R.id.menu_mark_as_favorite: {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getFavoriteContentValues(true), null, null);
                        }
                    }.start();
                    UiUtils.toast( getActivity(), R.string.entry_marked_favourite );
                    break;
                }
                case R.id.menu_mark_as_unfavorite: {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getFavoriteContentValues(false), null, null);
                        }
                    }.start();

                    UiUtils.toast( getActivity(), R.string.entry_marked_unfavourite );

                    CloseEntry();

                    break;
                }*/
                case R.id.menu_font_bold: {
                    PrefUtils.putBoolean(PrefUtils.ENTRY_FONT_BOLD,
                            !PrefUtils.getBoolean(PrefUtils.ENTRY_FONT_BOLD, false));
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                    break;
                }
                case R.id.menu_load_all_images: {
                    FetcherService.mMaxImageDownloadCount = 0;
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                    break;
                }
                case R.id.menu_share_all_text: {
                    if ( mCurrentPagerPos != -1 ) {
                        Spanned spanned = Html.fromHtml(mEntryPagerAdapter.GetEntryView(mCurrentPagerPos).mData);
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

                case R.id.menu_reload_full_text: {

                    ReloadFullText();
                    break;
                }

                case R.id.menu_reload_full_text_without_mobilizer: {

                    int status = FetcherService.Status().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.MobilizeType.No );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_reload_full_text_with_tags: {

                    int status = FetcherService.Status().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.MobilizeType.Tags );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_lock_land_orientation: {
                    mLockLandOrientation = !mLockLandOrientation;
                    item.setChecked(mLockLandOrientation);
                    SetOrientation();
                    break;
                }
            }
        }

        return true;
    }

    private void ReloadFullText() {
        int status = FetcherService.Status().Start("Reload fulltext");
        try {
            DeleteMobilized();
            LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
        } finally { FetcherService.Status().End( status ); }
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

                /*// Update the cursor
                Cursor updatedCursor = cr.query(uri, null, null, null, null);
                updatedCursor.moveToFirst();
                mEntryPagerAdapter.onsetUpdatedCursor(mCurrentPagerPos, updatedCursor);*/

            }
        }.start();
        getActivity().invalidateOptionsMenu();
        Toast.makeText( getContext(), mFavorite ? R.string.entry_marked_favourite : R.string.entry_marked_unfavourite, Toast.LENGTH_LONG ).show();
    }

    private void DeleteMobilized() {
        ContentValues values = new ContentValues();
        values.putNull(EntryColumns.MOBILIZED_HTML);
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        cr.update(uri, values, null, null);
    }

    private void CloseEntry() {
        PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        getActivity().finish();
    }

    public long getCurrentEntryID() {
        return GetEntryID( mCurrentPagerPos );
    }

    public void setData(final Uri uri) {
        Timer timer = new Timer( "EntryFr.setData" );
        Dog.v( String.format( "EntryFragment.setData( %s )", uri == null ? "" : uri.toString() ) );

        mCurrentPagerPos = -1;

        //PrefUtils.putString( PrefUtils.LAST_URI, uri.toString() );

        mBaseUri = null;
        if ( !IsExternalLink( uri ) ) {
            mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
            Dog.v(String.format("EntryFragment.setData( %s ) baseUri = %s", uri.toString(), mBaseUri));
            try {
                mInitialEntryId = Long.parseLong(uri.getLastPathSegment());
            } catch (Exception unused) {
                mInitialEntryId = -1;
            }


            String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;

            // Load the entriesIds list. Should be in a loader... but I was too lazy to do so
            Cursor entriesCursor = MainApplication.getContext().getContentResolver().query(mBaseUri, EntryColumns.PROJECTION_ID,
                    null, null, EntryColumns.DATE + entriesOrder);

            if (entriesCursor != null && entriesCursor.getCount() > 0) {
                synchronized ( this ) {
                    mEntriesIds = new long[entriesCursor.getCount()];
                }
                int i = 0;
                while (entriesCursor.moveToNext()) {
                    SetEntryID( i, entriesCursor.getLong(0) );
                    if (GetEntryID( i ) == mInitialEntryId) {
                        mCurrentPagerPos = i; // To immediately display the good entry
                        mLastPagerPos = i;
                        CancelStarNotification(getCurrentEntryID());
                    }
                    i++;
                }

                entriesCursor.close();
            }
            if ( mBaseUri != null && mBaseUri.getPathSegments().size() > 1 ) {
                Dog.v( "EntryFragment.setData() mBaseUri.getPathSegments[1] = " + mBaseUri.getPathSegments().get(1) );
                if ( mBaseUri.getPathSegments().get(1).equals( FetcherService.GetExtrenalLinkFeedID() ) ) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Dog.v( "EntryFragment.setData() update time to current" );
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            ContentValues values = new ContentValues();
                            values.put(EntryColumns.DATE, (new Date()).getTime());
                            cr.update(uri, values, null, null);
                        }
                    }).start();
                }
            }
        } else if ( mEntryPagerAdapter instanceof SingleEntryPagerAdapter ) {
            mBaseUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() );
            mCurrentPagerPos = 0;
        }


        mEntryPagerAdapter.notifyDataSetChanged();
        if (mCurrentPagerPos != -1) {
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }

        timer.End();
    }

    private void refreshUI(Cursor entryCursor) {
		try {
			if (entryCursor != null ) {
				//String feedTitle = entryCursor.isNull(mFeedNamePos) ? entryCursor.getString(mFeedUrlPos) : entryCursor.getString(mFeedNamePos);
				EntryActivity activity = (EntryActivity) getActivity();
				activity.setTitle("");//activity.setTitle(feedTitle);

				mFavorite = entryCursor.getInt(mIsFavoritePos) == 1;
				//mRetrieveFullText = entryCursor.getInt( mRetrieveFullTextPos ) == 1;

				activity.invalidateOptionsMenu();

				mStatusText.SetEntryID( String.valueOf( getCurrentEntryID() ) );
				// Listen the mobilizing task

				if (FetcherService.hasMobilizationTask( getCurrentEntryID() )) {
					//--showSwipeProgress();

					// If the service is not started, start it here to avoid an infinite loading
					if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                        FetcherService.StartService( new Intent(MainApplication.getContext(), FetcherService.class)
                                                     .setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
					}
				}

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
                            final Uri uri = ContentUris.withAppendedId(mBaseUri, GetEntryID( mPagerPos ));
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
            startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + DrawerAdapter.FIRST_ENTRY_POS, position2)), 0);
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
        EntryView entryView = mEntryPagerAdapter.GetEntryView( mEntryPager.getCurrentItem());
        if (entryView != null) {
            if ( PrefUtils.getBoolean( "article_text_footer_show_progress", true ) ) {
                mProgressBar.setVisibility( View.VISIBLE );
                int webViewHeight = entryView.getMeasuredHeight();
                int contentHeight = (int) Math.floor(entryView.getContentHeight() * entryView.getScale());
                mProgressBar.setMax(contentHeight - webViewHeight);
                mProgressBar.setProgress(entryView.getScrollY());
                String color = Theme.GetColor( "article_text_footer_progress_color", R.string.default_article_text_footer_color);
                if (Build.VERSION.SDK_INT >= 21 )
                    mProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor( color )));
                mProgressBar.setScaleY( PrefUtils.getIntFromText( "article_text_footer_progress_height", 1 ) );
            } else {
                mProgressBar.setVisibility( View.GONE );
            }
        }

        if ( PrefUtils.getBoolean( "article_text_footer_show_clock", true ) ) {
            mLabelClock.setTextSize(COMPLEX_UNIT_DIP, 8 + PrefUtils.getFontSizeFooterClock() );
            mLabelClock.setText( new SimpleDateFormat("HH:mm").format(new Date()) );
            mLabelClock.setTextColor(Theme.GetColorInt( "article_text_footer_clock_color", R.string.default_article_text_footer_color) );
            mLabelClock.setBackgroundColor( Theme.GetColorInt( "article_text_footer_clock_color_background", R.string.transparent_color) );
        } else {
            mLabelClock.setText( "" );
        }
    }

    @Override
    public void onClickOriginalText() {
        getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                mIsFullTextShown = false;
                mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
            }
        });
    }

    @Override
    public void onClickFullText() {
        final BaseActivity activity = (BaseActivity) getActivity();

        Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
        final boolean alreadyMobilized = !cursor.isNull(mMobilizedHtmlPos);

        if (alreadyMobilized) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIsFullTextShown = true;
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                }
            });
        } else /*--if (!isRefreshing())*/ {
            LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
        }
    }

    private void LoadFullText(final ArticleTextExtractor.MobilizeType mobilize ) {
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
                    int status = FetcherService.Status().Start(getActivity().getString(R.string.loadFullText)); try {
                        FetcherService.mobilizeEntry(getContext().getContentResolver(), getCurrentEntryID(), mobilize, FetcherService.AutoDownloadEntryImages.Yes, true, true);
                    } finally { FetcherService.Status().End( status ); }
                }
            }.start();



            /*activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshSwipeProgress();
                }
            });*/
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
    public void onClickEnclosure() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String enclosure = mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mEnclosurePos);

                final int position1 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + DrawerAdapter.FIRST_ENTRY_POS);

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
            PrefUtils.putString(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, oldPref + "\n" + className);
            DeleteMobilized();
            LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
            Toast.makeText( getContext(), R.string.fullTextReloadStarted, Toast.LENGTH_LONG ).show();
        }
    }

    public void returnClass(String classNameList) {
        final ArrayList<String> list = PrefUtils.GetRemoveClassList();
        boolean needRefresh = false;
        for ( String className: TextUtils.split( classNameList, " " ) )
            if ( list.contains( className ) ) {
                needRefresh = true;
                list.remove( className );
            }
        if ( !needRefresh )
            return;
        PrefUtils.putString(PrefUtils.GLOBAL_CLASS_LIST_TO_REMOVE_FROM_ARTICLE_TEXT, TextUtils.join( "\n", list ) );
        DeleteMobilized();
        LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
        Toast.makeText( getContext(), R.string.fullTextReloadStarted, Toast.LENGTH_LONG ).show();
    }

    @Override
    public void openTagMenu(final String className, final String baseUrl, final String paramValue ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext() );
        builder.setTitle(baseUrl + ":class=" + className)
            .setItems(new CharSequence[]{getString(R.string.setFullTextRoot),
                                         getString(paramValue.equals( "hide" ) ? R.string.hide : R.string.show )},
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0)
                        setFullTextRoot(baseUrl, className);
                    else if ( paramValue.equals( "hide" ) )
                        removeClass( className );
                    else if ( paramValue.equals( "show" ) )
                        returnClass( className );
                }
            });
        builder.show();



    }

    private void setFullTextRoot(String baseUrl, String className) {
        ArrayList<String> ruleList = HtmlUtils.Split( PrefUtils.getString( PrefUtils.CONTENT_EXTRACT_RULES, R.string.full_text_root_default ),
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
        PrefUtils.putString(PrefUtils.CONTENT_EXTRACT_RULES, TextUtils.join( "\n", ruleList ) );
        DeleteMobilized();
        LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
        Toast.makeText( getContext(), R.string.fullTextReloadStarted, Toast.LENGTH_LONG ).show();
    }

    @Override
    public void downloadImage(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FetcherService.mCancelRefresh = false;
                int status = FetcherService.Status().Start( getString(R.string.downloadImage) ); try {
                    NetworkUtils.downloadImage(getCurrentEntryID(), url, false);
                } catch (IOException e) {
                    //FetcherService.Status().End( status );
                    e.printStackTrace();
                } finally {
                    FetcherService.Status().End( status );
                }

            }
        }).start();
        //mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
    }

    @Override
    public void downloadNextImages() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FetcherService.mMaxImageDownloadCount += PrefUtils.getImageDownloadCount();
                mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
            }
        });

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timer.Start( id, "EntryFr.onCreateLoader" );
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(GetEntryID( id )), null, null, null, null);
        cursorLoader.setUpdateThrottle(100);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Timer.End( loader.getId() );
        if (cursor != null) { // can be null if we do a setData(null) before
            try {
                if ( cursor.moveToFirst() ) {

                    if (mTitlePos == -1) {
                        mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                        mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
                        mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
                        mMobilizedHtmlPos = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
                        mLinkPos = cursor.getColumnIndex(EntryColumns.LINK);
                        mIsFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                        mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
                        mIsNewPos = cursor.getColumnIndex(EntryColumns.IS_NEW);
                        mEnclosurePos = cursor.getColumnIndex(EntryColumns.ENCLOSURE);
                        mFeedIDPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
                        mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
                        mScrollPosPos = cursor.getColumnIndex(EntryColumns.SCROLL_POS);
                        mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
                        mFeedUrlPos = cursor.getColumnIndex(FeedColumns.URL);
                        mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
                        mRetrieveFullTextPos = cursor.getColumnIndex(FeedColumns.RETRIEVE_FULLTEXT);
                    }

                    int position = loader.getId();
                    if (position != -1) {
                        FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
                        mEntryPagerAdapter.displayEntry(position, cursor, false);
                        mRetrieveFullText = cursor.getInt(mRetrieveFullTextPos) == 1;
                        EntryActivity activity = (EntryActivity) getActivity();
                        if (getBoolean(DISPLAY_ENTRIES_FULLSCREEN, false))
                            activity.setFullScreen(true, true);

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
        abstract void onResume();
        abstract void onPause();
        public abstract EntryView GetEntryView( int pagerPos );

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
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        void displayEntry(int pagerPos, Cursor newCursor, boolean forceUpdate) {
            Dog.d( "EntryPagerAdapter.displayEntry" + pagerPos);

            EntryView view = GetEntryView( pagerPos );
            if (view != null ) {
                if (newCursor == null) {
                    newCursor = (Cursor) view.getTag(); // get the old one
                }

                if (newCursor != null && newCursor.moveToFirst()  ) {
                    view.setTag(newCursor);

                    String contentText;
                    String author = "";
                    long timestamp = 0;
                    String link = "";
                    String title = "";
                    String feedID = "";
                    String enclosure = "";
                    float scrollPart = 0;
                    try {
                        contentText = newCursor.getString(mMobilizedHtmlPos);
                        feedID = newCursor.getString(mFeedIDPos);
                        if (!feedID.equals(GetExtrenalLinkFeedID()) &&
                            ( contentText == null || (forceUpdate && !mIsFullTextShown)) ) {
                            mIsFullTextShown = false;
                            contentText = newCursor.getString(mAbstractPos);
                        } else {
                            mIsFullTextShown = true;
                        }
                        if (contentText == null) {
                            contentText = "";
                        }

                        author = newCursor.getString(mAuthorPos);
                        timestamp = newCursor.getLong(mDatePos);
                        link = newCursor.getString(mLinkPos);
                        title = newCursor.getString(mTitlePos);
                        enclosure = newCursor.getString(mEnclosurePos);
                        if ( !newCursor.isNull(mScrollPosPos) )
                            scrollPart = newCursor.getFloat(mScrollPosPos);

                    } catch ( IllegalStateException e ) {
                        e.printStackTrace();
                        contentText = "Context too large";
                    }

                    //FetcherService.setCurrentEntryID( getCurrentEntryID() );
                    view.setHtml(GetEntryID( pagerPos ),
                                feedID,
                                title,
                                link,
                                contentText,
                                enclosure,
                                author,
                                timestamp,
                                mIsFullTextShown,
                                (EntryActivity) getActivity());

                    if (pagerPos == mCurrentPagerPos) {
                        refreshUI(newCursor);

                        //if (PrefUtils.getLong(PrefUtils.LAST_ENTRY_ID, 0) == mEntriesIds[pagerPos]) {
                        //int dy = mScrollPosPos;
                        //if (dy > view.getScrollY())
                        //if ( view.GetViewScrollPartY() < scrollPart )
                            view.mScrollPartY = scrollPart;
                        Dog.v( String.format( "displayEntry view.mScrollY  (entry %s) view.mScrollY = %f", getCurrentEntryID(),  view.mScrollPartY ) );
                        //Dog.v( "displayEntry view.mScrollY = " + view.mScrollY );
                        //}


                    }

                    UpdateFooter();

                }
            }
        }

    };

    public class EntryPagerAdapter extends BaseEntryPagerAdapter {

        private final SparseArray<EntryView> mEntryViews = new SparseArray<>();

        EntryPagerAdapter() { }

        @Override
        public int getCount() {
            synchronized ( this ) {
                return mEntriesIds != null ? mEntriesIds.length : 0;
            }
        }

        @Override
        public void destroyItem(ViewGroup container, final int position, Object object) {
            Dog.d( "EntryPagerAdapter.destroyItem " + position );
            FetcherService.removeActiveEntryID( GetEntryID( position ) );
            getLoaderManager().destroyLoader(position);
            container.removeView((View) object);
            EntryView.mImageDownloadObservable.deleteObserver(mEntryViews.get(position));
            GetEntryView( position ).SaveScrollPos();
            mEntryViews.delete(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Dog.d( "EntryPagerAdapter.instantiateItem" + position );
            FetcherService.addActiveEntryID( GetEntryID( position ) );
            final EntryView view = CreateEntryView();
            mEntryViews.put(position, view);
            container.addView(view);
            getLoaderManager().restartLoader(position, null, EntryFragment.this);

            return view;
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

        @Override
        void onResume() {
            if (mEntriesIds != null) {
                EntryView view = mEntryViews.get(mCurrentPagerPos);
                if (view != null) {
                    view.onResume();
                }
            }
        }

        @Override
        void onPause() {
            for (int i = 0; i < mEntryViews.size(); i++) {
                mEntryViews.valueAt(i).onPause();
            }
        }

        @Override
        public EntryView GetEntryView( int pagerPos ) {
            return mEntryViews.get(pagerPos);
        }
    }

    public class SingleEntryPagerAdapter extends BaseEntryPagerAdapter {
        EntryView mEntryView = null;

        SingleEntryPagerAdapter() {

        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public EntryView GetEntryView(int pagerPos) {
            return mEntryView;
        }

        @Override
        public void destroyItem(ViewGroup container, final int position, Object object) {
            Dog.d( "EntryPagerAdapter.destroyItem " + position );
            FetcherService.removeActiveEntryID( GetEntryID( position ) );
            getLoaderManager().destroyLoader(position);
            container.removeView((View) object);
            EntryView.mImageDownloadObservable.deleteObserver(mEntryView);
            mEntryView.SaveScrollPos();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Dog.d( "EntryPagerAdapter.instantiateItem" + position );
            final EntryView view = CreateEntryView();
            container.addView(view);
            long entryID = GetEntryID( position );
            if ( entryID != -1 ) {
                FetcherService.addActiveEntryID(entryID);
                getLoaderManager().restartLoader(position, null, EntryFragment.this);
            }
            mEntryView = view;
            return view;
        }


        @Override
        void onResume() {
            if ( mEntryView != null )
                mEntryView.onResume();
        }

        @Override
        void onPause() {
            mEntryView.onPause();
        }
    }

    @NonNull
    private EntryView CreateEntryView() {
        final EntryView view = new EntryView(getActivity());
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
                if ( view.IsScrollAtBottom() )
                    SetIsFavourite(false);
            }
        };
        return view;
    }

    public void SetEntryID( int position, long entryID )  {
        synchronized ( this ) {
            mEntriesIds[position] = entryID;
        }
    }
    public long GetEntryID( int position )  {
        synchronized ( this ) {
            if ( position >= 0 && position < mEntriesIds.length )
                return mEntriesIds[position];
            else
                return -1;
        }
    }
}

