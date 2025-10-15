package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.SCROLL_POS;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.TITLE;
import static ru.yanus171.feedexfork.provider.FeedData.PutFavorite;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.STATE_IMAGE_WHITE_BACKGROUND;
import static ru.yanus171.feedexfork.fragment.EntryFragment.NEW_TASK_EXTRA;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Stack;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.EntryActivityNewTask;
import ru.yanus171.feedexfork.activity.GeneralPrefsActivity;
import ru.yanus171.feedexfork.fragment.PDFViewEntryView;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.utils.WaitDialog;

public abstract class EntryView {
    private final View mRootView;
    public EntryActivity mActivity = null;
    public boolean mLoadTitleOnly = false;
    public boolean mContentWasLoaded = false;
    public double mScrollPartY = -1;
    public Cursor mCursor = null;
    protected int mStatus = 0;
    public String mTitle = "";
    public static final String TAG = "EntryView";
    public boolean mWasAutoUnStar = false;
    public boolean mFavorite;
    public int mTitlePos = -1, mDatePos, mAbstractPos, mLinkPos, mIsFavoritePos,
            mIsWithTablePos, mIsLandscapePos, mIsReadPos, mIsNewPos, mIsWasAutoUnStarPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconUrlPos, mFeedIDPos, mScrollPosPos, mRetrieveFullTextPos;


    private final Stack<Integer> mHistoryAnchorScrollY = new Stack<>();
    public long mEntryId = -1;
    public String mEntryLink = "";
    public View mView = null;
    public Runnable mScrollChangeListener = null;

    public static final long TAP_TIMEOUT = 1000;

    protected EntryView(EntryActivity activity, long entryId) {
        mActivity = activity;
        mEntryId = entryId;
        mRootView = mActivity.mEntryFragment.mRootView;
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
            ScrollTo(mHistoryAnchorScrollY.pop(), false);
        mActivity.mEntryFragment.SetupZones();
    }

    public void GoTop() {
        AddNavigationHistoryStep();
        ScrollTo(0, false );
    }

    public void AddNavigationHistoryStep() {
        mHistoryAnchorScrollY.push(GetScrollY());
        mActivity.mEntryFragment.SetupZones();
    }

    protected abstract int GetScrollY();
    protected abstract void ScrollTo( int y, boolean smooth );
    public abstract void LongClickOnBottom();
    public abstract void ScrollOneScreen(int direction);
    protected abstract double GetViewScrollPartY();

    public void SaveScrollPos() {
        if ( !mContentWasLoaded )
            return;
        mScrollPartY = GetViewScrollPartY();
        if ( mScrollPartY > 0.0001 ) {
            //Dog.v(TAG, String.format("EnrtyView.SaveScrollPos (entry %d) mScrollPartY = %f getScrollY() = %d, view.getContentHeight() = %f", mEntryId, mScrollPartY, getScrollY(), GetContentHeight()));
            ContentValues values = new ContentValues();
            values.put(SCROLL_POS, mScrollPartY);
            SaveStateToDB( values );
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            SetNotifyEnabled(false ); try {
                cr.update(FeedData.EntryColumns.CONTENT_URI(mEntryId), values, null, null);
            } finally {
                SetNotifyEnabled( true );
            }
        }
    }
    protected void SaveStateToDB( ContentValues values ) {

    }

    public abstract boolean IsScrollAtBottom();
    public abstract ProgressInfo getProgressInfo( );

    public void Destroy() {

    }

    public void refreshUI( boolean invalidateContent ) {
        if ( invalidateContent )
            InvalidateContentCache();
        mActivity.mEntryFragment.hideTapZones();
        mActivity.mEntryFragment.mControlPanel.hide();
    }

    public void onStart() {
    }
    public abstract void ScrollToPage(int page);

    static public class ProgressInfo {
        public int max;
        public int progress;
        public int step;
    }

    public void StatusStartPageLoading() {
        if ( !mContentWasLoaded )
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
        mContentWasLoaded = false;
    }
    public void onResume() {

    }
    public void onPause() {

    }

    public static EntryView Create(String link, long entryId, EntryActivity activity, ViewGroup container) {
        Dog.v( TAG, "EntryView.Create link = " + link);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && link.endsWith( "pdf" ) )
            return new PDFViewEntryView( activity, container, entryId);//PDFEntryView
        else
            return new WebEntryView( activity, container, entryId );

    }

    protected void toggleTapZoneVisibility() {
        if ( mActivity != null && mActivity.mEntryFragment != null )
            mActivity.mEntryFragment.toggleTapZoneVisibility();
    }
    public void setCursor( Cursor cursor ) {
        mCursor = cursor;
    }
    @SuppressLint("Range")
    public void generateArticleContent( boolean forceUpdate ) {
        //refreshUI();
        Dog.v(String.format("displayEntry view.mScrollY  (entry %s) view.mScrollY = %f", mEntryId, mScrollPartY));
        mActivity.mEntryFragment.UpdateHeader();
    }

    @SuppressLint("Range")
    public void loadingDataFinished(){
        //Timer.End( loader.getId() );
        if (mCursor != null && mCursor.moveToFirst() ) {
            if (mTitlePos == -1) {
                mTitlePos = mCursor.getColumnIndex(TITLE);
                mDatePos = mCursor.getColumnIndex(FeedData.EntryColumns.DATE);
                mAbstractPos = mCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
                mLinkPos = mCursor.getColumnIndex(FeedData.EntryColumns.LINK);
                mIsFavoritePos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_FAVORITE);
                mIsWithTablePos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_WITH_TABLES);
                mIsLandscapePos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_LANDSCAPE);
                mIsReadPos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_READ);
                mIsNewPos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_NEW);
                mIsWasAutoUnStarPos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_WAS_AUTO_UNSTAR);
                mEnclosurePos = mCursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
                mFeedIDPos = mCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
                mAuthorPos = mCursor.getColumnIndex(FeedData.EntryColumns.AUTHOR);
                mScrollPosPos = mCursor.getColumnIndex(SCROLL_POS);
                mFeedNamePos = mCursor.getColumnIndex(FeedData.FeedColumns.NAME);
                mFeedUrlPos = mCursor.getColumnIndex(FeedData.FeedColumns.URL);
                mFeedIconUrlPos = mCursor.getColumnIndex(FeedData.FeedColumns.ICON_URL);
                mRetrieveFullTextPos = mCursor.getColumnIndex(FeedData.FeedColumns.RETRIEVE_FULLTEXT);
            }
            mEntryLink = mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.LINK));
            mScrollPartY = readDouble( SCROLL_POS, 0);
            mFavorite = mCursor.getInt(mIsFavoritePos) == 1;
            mTitle = mCursor.getString(mCursor.getColumnIndex(TITLE));
        }
    }

    @SuppressLint("Range")
    protected float readFloat( String fieldName, float defaultValue) {
        return !mCursor.isNull(mCursor.getColumnIndex(fieldName)) ?
            mCursor.getFloat(mCursor.getColumnIndex(fieldName)) :
            defaultValue;
    }
    @SuppressLint("Range")
    protected double readDouble( String fieldName, double defaultValue) {
        return !mCursor.isNull(mCursor.getColumnIndex(fieldName)) ?
                mCursor.getDouble( mCursor.getColumnIndex(fieldName)) :
                defaultValue;
    }

    @SuppressLint("Range")
    public void onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_star: {

                SetIsFavorite( !mFavorite, true );
                break;
            }

            case R.id.menu_toggle_theme: {
                SaveScrollPos();
                PrefUtils.ToogleTheme(FetcherService.GetEntryActivityIntent(Intent.ACTION_VIEW, getUri()));
            }

            case R.id.menu_copy_clipboard: {
                ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text 1", mEntryLink);
                clipboard.setPrimaryClip(clip);

                UiUtils.toast( R.string.link_was_copied_to_clipboard);
                break;
            }
            case R.id.menu_go_back: {
                GoBack();
                break;
            }
            case R.id.menu_setting: {
                OpenSettings();
                break;
            }

            case R.id.menu_edit_article_title: {
                final EditText editText = new EditText(mActivity);
                editText.setText( mTitle );
                final AlertDialog d = new AlertDialog.Builder( mActivity )
                        .setView( editText )
                        .setTitle( R.string.menu_edit_article_title )
                        .setIcon( R.drawable.ic_edit )
                        .setNegativeButton( android.R.string.cancel, null )
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final Uri uri = getUri();
                                if ( !mActivity.mIsNewTask )
                                    PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, getUri().toString());
                                new Thread() {
                                    @Override
                                    public void run() {
                                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                                        ContentValues values = new ContentValues();
                                        final String newTitle = editText.getText().toString();
                                        values.put( TITLE, newTitle );
                                        cr.update(uri, values, null, null);
                                        mActivity.mEntryFragment.restartCurrentEntryLoader();
                                    }


                                }.start();
                            }
                        }).create();
                d.show();
                break;
            }

            case R.id.menu_image_white_background: {
                toggleImageWhiteBackground();
                item.setChecked( PrefUtils.isImageWhiteBackground() );
                break;
            }
            case R.id.menu_disable_all_tap_actions: {
                PrefUtils.putBoolean( PREF_ARTICLE_TAP_ENABLED_TEMP, false);
                mActivity.mEntryFragment.SetupZones();
                UiUtils.toast( R.string.tap_actions_were_disabled );
                refreshUI(true);
                break;
            }


            case R.id.menu_add_entry_shortcut: {
                if ( ShortcutManagerCompat.isRequestPinShortcutSupported(mActivity) ) {
                    //Adding shortcut for MainActivity on Home screen
                    if (mCursor != null) {
                        final String name = mCursor.getString(mTitlePos);
                        final long entryID = mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));
                        final Uri uri = Uri.parse( mCursor.getString(mLinkPos) );
                        final String iconUrl = mCursor.getString(mCursor.getColumnIndex( FeedData.EntryColumns.IMAGE_URL ));

                        new WaitDialog(mActivity, R.string.downloadImage, new Runnable() {
                            @Override
                            public void run() {

                                final IconCompat icon = LoadIcon(iconUrl);
                                mActivity.runOnUiThread(() -> {
                                    final Intent intent = new Intent(mActivity, EntryActivityNewTask.class)
                                            .setAction(Intent.ACTION_VIEW)
                                            .setData(uri)
                                            .putExtra( NEW_TASK_EXTRA, true );
                                    ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(mActivity, String.valueOf(entryID))
                                            .setIcon(icon)
                                            .setShortLabel(name)
                                            .setIntent( intent )
                                            .build();


                                    ShortcutManagerCompat.requestPinShortcut(mActivity, pinShortcutInfo, null);
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                                        UiUtils.toast( R.string.new_entry_shortcut_added );
                                });
                            }
                        }).execute();
                    }
                } else
                    UiUtils.toast( R.string.new_feed_shortcut_add_failed );
                break;
            }

        }

    }

    private void OpenSettings() {
        mActivity.startActivity(new Intent(mActivity, GeneralPrefsActivity.class));
    }

    protected void toggleImageWhiteBackground() {
        PrefUtils.toggleBoolean(STATE_IMAGE_WHITE_BACKGROUND, false) ;
        refreshUI(true);
        generateArticleContent(true);
    }

    @NotNull
    public static IconCompat LoadIcon(String iconUrl) {
        Bitmap bitmap = iconUrl != null ? NetworkUtils.downloadImage(iconUrl) : null;
        if ( bitmap == null )
            UiUtils.RunOnGuiThread(() -> UiUtils.toast( R.string.unable_to_load_article_icon ));
        else
            bitmap = UiUtils.getScaledBitmap( bitmap, 32 );
        return (bitmap == null) ?
                IconCompat.createWithResource( MainApplication.getContext(), R.mipmap.ic_launcher ) :
                IconCompat.createWithBitmap(bitmap);
    }


    public void OpenLabelSetup() {
        LabelVoc.INSTANCE.showDialogToSetArticleLabels(mActivity, mEntryId, null);
    }

    public abstract void leftBottomBtnClick();
    public abstract void rightBottomBtnClick();

    public void SetIsFavorite(final boolean favorite, boolean showToast) {
        if ( mFavorite == favorite )
            return;
        mFavorite = favorite;
        final HashSet<Long> oldLabels = LabelVoc.INSTANCE.getLabelIDs(mEntryId);
        final Uri uri = getUri();
        new Thread() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                PutFavorite( values, mFavorite );
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(uri, values, null, null);
                if ( !mFavorite )
                    LabelVoc.INSTANCE.removeLabels( mEntryId );
                mActivity.mEntryFragment.restartCurrentEntryLoader();
            }
        }.start();
        mActivity.invalidateOptionsMenu();
        if ( mFavorite ) {
            if ( showToast )
                UiUtils.toast( R.string.entry_marked_favourite );
        } else {
            Snackbar snackbar = Snackbar.make(mActivity.mEntryFragment.getView().getRootView().findViewById(R.id.pageDownBtn), R.string.removed_from_favorites, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(mActivity.mEntryFragment.getView().getContext(), R.color.light_theme_color_primary))
                    .setAction(R.string.undo, v -> {
                        SetIsFavorite( true, false );
                        LabelVoc.INSTANCE.setEntry( mEntryId, oldLabels );
                        mFavorite = true;
                    });
            snackbar.getView().setBackgroundResource(R.color.material_grey_900);
            snackbar.show();
        }
    }
    @NonNull
    public Uri getUri() {
        return ContentUris.withAppendedId(mActivity.mEntryFragment.mBaseUri, mEntryId);
    }
    public void onPrepareOptionsMenu (Menu menu) {
        menu.findItem(R.id.menu_image_white_background).setChecked(PrefUtils.isImageWhiteBackground());
        menu.findItem(R.id.menu_show_progress_info).setChecked(PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ));
        menu.findItem(R.id.menu_force_orientation_by_sensor).setChecked( PrefUtils.isForceOrientationBySensor() );

        menu.findItem(R.id.menu_full_screen).setChecked(GetIsStatusBarHidden() );
        menu.findItem(R.id.menu_actionbar_visible).setChecked(!GetIsStatusBarHidden() );

        menu.findItem(R.id.menu_go_back).setVisible( CanGoBack() );
        menu.findItem( R.id.menu_zoom_shift_enabled).setVisible( false );
    }

//    private String getTitle() {
//        if (GetExtrenalLinkFeedID().equals(mCursor.getString(mFeedIDPos))) {
//            if (!mCursor.isNull(mTitlePos))
//                return mCursor.getString(mTitlePos);
//            else
//                return "";
//        }
//        if ( !mCursor.isNull(mFeedNamePos) )
//            return mCursor.getString(mFeedNamePos);
//        if ( !mCursor.isNull(mFeedUrlPos) )
//            return mCursor.getString(mFeedUrlPos);
//        return "";
//    }

    protected static float getPageChangeMultiplier() {
        return PrefUtils.isPageUpDown90Pct() ? 0.9F : 0.98F;
    }


    public void setupControlPanelButtonActions() {
        setupButtonAction(R.id.btn_label_setup, false, v -> OpenLabelSetup());
        setupButtonAction(R.id.btn_settings, false, v -> OpenSettings());
    }
    protected void setupButtonAction(int viewId, boolean checked, View.OnClickListener click ) {
        mActivity.mEntryFragment.mControlPanel.setupButtonAction(viewId, checked, click );
    }
}

