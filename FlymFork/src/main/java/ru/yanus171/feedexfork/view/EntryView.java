package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.LANDSCAPE;
import static ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.NONE;
import static ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.PORTRAIT;
import static ru.yanus171.feedexfork.fragment.EntryFragment.STATE_RELOAD_IMG_WITH_A_LINK;
import static ru.yanus171.feedexfork.fragment.EntryFragment.STATE_RELOAD_WITH_DEBUG;
import static ru.yanus171.feedexfork.provider.FeedData.PutFavorite;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_FORCE_ORIENTATION_BY_SENSOR;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_PROGRESS_INFO;
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
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.loader.content.Loader;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Stack;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.ArticleWebSearchActivity;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.EntryActivityNewTask;
import ru.yanus171.feedexfork.activity.GeneralPrefsActivity;
import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.fragment.PDFViewEntryView;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.utils.WaitDialog;

public abstract class EntryView {
    public EntryActivity mActivity = null;
    public boolean mLoadTitleOnly = false;
    public boolean mContentWasLoaded = false;
    public double mScrollPartY = -1;
    public Cursor mCursor = null;
    protected int mStatus = 0;
    public String mTitle;
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

    protected EntryView(EntryActivity activity, long entryId) {
        mActivity = activity;
        mEntryId = entryId;
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
    public abstract void ScrollToBottom();
    public abstract void PageChange(int delta, StatusText statusText);
    protected abstract double GetViewScrollPartY();

    public void SaveScrollPos() {
        if ( !mContentWasLoaded )
            return;
        mScrollPartY = GetViewScrollPartY();
        if ( mScrollPartY > 0.0001 ) {
            //Dog.v(TAG, String.format("EnrtyView.SaveScrollPos (entry %d) mScrollPartY = %f getScrollY() = %d, view.getContentHeight() = %f", mEntryId, mScrollPartY, getScrollY(), GetContentHeight()));
            ContentValues values = new ContentValues();
            values.put(FeedData.EntryColumns.SCROLL_POS, mScrollPartY);
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
    public abstract ProgressInfo getProgressInfo( int statusHeight );

    public void Destroy() {

    }

    public void refreshUI() {
        mFavorite = mCursor.getInt(mIsFavoritePos) == 1;
    }

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
    @SuppressLint("Range")
    public void generateArticleContent( Cursor cursor, boolean forceUpdate ) {
        mCursor = cursor;
        mEntryLink = mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.LINK));
        if ( mScrollPartY == -1 )
            mScrollPartY = !mCursor.isNull(mCursor.getColumnIndex(FeedData.EntryColumns.SCROLL_POS)) ?
                    mCursor.getDouble(mCursor.getColumnIndex(FeedData.EntryColumns.SCROLL_POS)) : 0;

        //refreshUI();
        Dog.v(String.format("displayEntry view.mScrollY  (entry %s) view.mScrollY = %f", mEntryId, mScrollPartY));
        mActivity.mEntryFragment.UpdateHeader();
    }

    public void loadingDataFinished(Loader<Cursor> loader, Cursor cursor){
        mCursor = cursor;
        Timer.End( loader.getId() );
        if (cursor != null) { // can be null if we do a setData(null) before
            try {
                if ( cursor.moveToFirst() ) {

                    if (mTitlePos == -1) {
                        mTitlePos = cursor.getColumnIndex(FeedData.EntryColumns.TITLE);
                        mDatePos = cursor.getColumnIndex(FeedData.EntryColumns.DATE);
                        mAbstractPos = cursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
                        mLinkPos = cursor.getColumnIndex(FeedData.EntryColumns.LINK);
                        mIsFavoritePos = cursor.getColumnIndex(FeedData.EntryColumns.IS_FAVORITE);
                        mIsWithTablePos= cursor.getColumnIndex(FeedData.EntryColumns.IS_WITH_TABLES);
                        mIsLandscapePos= cursor.getColumnIndex(FeedData.EntryColumns.IS_LANDSCAPE);
                        mIsReadPos = cursor.getColumnIndex(FeedData.EntryColumns.IS_READ);
                        mIsNewPos = cursor.getColumnIndex(FeedData.EntryColumns.IS_NEW);
                        mIsWasAutoUnStarPos = cursor.getColumnIndex(FeedData.EntryColumns.IS_WAS_AUTO_UNSTAR);
                        mEnclosurePos = cursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
                        mFeedIDPos = cursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
                        mAuthorPos = cursor.getColumnIndex(FeedData.EntryColumns.AUTHOR);
                        mScrollPosPos = cursor.getColumnIndex(FeedData.EntryColumns.SCROLL_POS);
                        mFeedNamePos = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
                        mFeedUrlPos = cursor.getColumnIndex(FeedData.FeedColumns.URL);
                        mFeedIconUrlPos = cursor.getColumnIndex(FeedData.FeedColumns.ICON_URL);
                        mRetrieveFullTextPos = cursor.getColumnIndex(FeedData.FeedColumns.RETRIEVE_FULLTEXT);
                    }
                }
            } catch ( IllegalStateException e ) {
                FetcherService.Status().SetError( e.getMessage(), "", String.valueOf( mEntryId ), e );
                Dog.e("Error", e);
            }
        }
        //refreshUI();
    }

    @SuppressLint("Range")
    public void onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_star: {

                SetIsFavorite( !mFavorite, true );
                break;
            }
            case R.id.menu_share: {

                if (mEntryLink != null) {
                    String title = mCursor.getString(mTitlePos);

                    mActivity.startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, mEntryLink)
                                    .setType(Constants.MIMETYPE_TEXT_PLAIN), mActivity.getString(R.string.menu_share)));
                }
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

                UiUtils.toast( mActivity, R.string.link_was_copied_to_clipboard);
                break;
            }
            case R.id.menu_go_back: {
                GoBack();
                break;
            }
            case R.id.menu_setting: {
                mActivity.startActivity(new Intent(mActivity, GeneralPrefsActivity.class));
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
                                        values.put( FeedData.EntryColumns.TITLE, newTitle );
                                        cr.update(uri, values, null, null);
                                        UiUtils.RunOnGuiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mActivity.mEntryFragment.getLoaderManager().restartLoader(mActivity.mEntryFragment.mCurrentPagerPos, null, mActivity.mEntryFragment);
                                                InvalidateContentCache();
                                                //mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true, true);
                                            }
                                        });
                                    }
                                }.start();
                            }
                        }).create();
                d.show();
                break;
            }

            case R.id.menu_image_white_background: {
                PrefUtils.toggleBoolean(STATE_IMAGE_WHITE_BACKGROUND, false) ;
                item.setChecked( PrefUtils.isImageWhiteBackground() );
                refreshUI();
                generateArticleContent( mCursor, true);
                break;
            }
            case R.id.menu_disable_all_tap_actions: {
                PrefUtils.putBoolean( PREF_ARTICLE_TAP_ENABLED_TEMP, false);
                mActivity.mEntryFragment.SetupZones();
                Toast.makeText( mActivity, R.string.tap_actions_were_disabled, Toast.LENGTH_LONG ).show();
                refreshUI();
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
                                        Toast.makeText(mActivity, R.string.new_entry_shortcut_added, Toast.LENGTH_LONG).show();
                                });
                            }
                        }).execute();
                    }
                } else
                    Toast.makeText( mActivity, R.string.new_feed_shortcut_add_failed, Toast.LENGTH_LONG ).show();
                break;
            }

        }

    }

    @NotNull
    public static IconCompat LoadIcon(String iconUrl) {
        Bitmap bitmap = iconUrl != null ? NetworkUtils.downloadImage(iconUrl) : null;
        if ( bitmap == null )
            UiUtils.RunOnGuiThread(() -> Toast.makeText(MainApplication.getContext(), R.string.unable_to_load_article_icon, Toast.LENGTH_LONG ).show());
        else
            bitmap = UiUtils.getScaledBitmap( bitmap, 32 );
        return (bitmap == null) ?
                IconCompat.createWithResource( MainApplication.getContext(), R.mipmap.ic_launcher ) :
                IconCompat.createWithBitmap(bitmap);
    }


    public void OpenLabelSetup() {
        LabelVoc.INSTANCE.showDialogToSetArticleLabels(mActivity, mEntryId, null);
    }

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
                UiUtils.RunOnGuiThread( ()-> mActivity.mEntryFragment.getLoaderManager().restartLoader(mActivity.mEntryFragment.mCurrentPagerPos, null, mActivity.mEntryFragment) );
            }
        }.start();
        mActivity.invalidateOptionsMenu();
        if ( mFavorite ) {
            if ( showToast )
                Toast.makeText(mActivity, R.string.entry_marked_favourite, Toast.LENGTH_LONG).show();
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
        menu.findItem(R.id.menu_force_landscape_orientation_toggle).setChecked( mActivity.mEntryFragment.mForceOrientation == LANDSCAPE );
        menu.findItem(R.id.menu_force_portrait_orientation_toggle).setChecked( mActivity.mEntryFragment.mForceOrientation == PORTRAIT );
        menu.findItem(R.id.menu_force_orientation_by_sensor).setChecked( PrefUtils.isForceOrientationBySensor() );

        menu.findItem(R.id.menu_full_screen).setChecked(GetIsStatusBarHidden() );
        menu.findItem(R.id.menu_actionbar_visible).setChecked(!GetIsStatusBarHidden() );

        menu.findItem(R.id.menu_go_back).setVisible( CanGoBack() );
    }

    private String getTitle() {
        if (GetExtrenalLinkFeedID().equals(mCursor.getString(mFeedIDPos))) {
            if (!mCursor.isNull(mTitlePos))
                return mCursor.getString(mTitlePos);
            else
                return "";
        }
        if ( !mCursor.isNull(mFeedNamePos) )
            return mCursor.getString(mFeedNamePos);
        if ( !mCursor.isNull(mFeedUrlPos) )
            return mCursor.getString(mFeedUrlPos);
        return "";
    }


}

