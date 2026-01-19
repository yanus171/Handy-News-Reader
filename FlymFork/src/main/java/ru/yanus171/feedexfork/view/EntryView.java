package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.fragment.EntryFragment.addArticleShortcut;
import static ru.yanus171.feedexfork.fragment.EntryMenu.setItemChecked;
import static ru.yanus171.feedexfork.fragment.EntryMenu.setItemVisible;
import static ru.yanus171.feedexfork.fragment.EntryMenu.setVisible;
import static ru.yanus171.feedexfork.fragment.EntryMenu.updateMenuWithIcon;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.SCROLL_POS;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.TITLE;
import static ru.yanus171.feedexfork.provider.FeedData.PutFavorite;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.STATE_IMAGE_WHITE_BACKGROUND;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Stack;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.GeneralPrefsActivity;
import ru.yanus171.feedexfork.activity.LocalFile;
import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.EPUB;
import ru.yanus171.feedexfork.service.FB2;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

public abstract class EntryView {
    protected double mScrollPartY = -1;
    public Cursor mCursor = null;
    public String mTitle = "";
    public static final String TAG = "EntryView";
    public boolean mFavorite;
    protected int mTitlePos = -1;
    private int mFeedIDPos, mLinkPos, mIsFavoritePos, mIsLandscapePos, mIsReadPos;
    public long mEntryId = -1;
    public String mEntryLink = "";
    public View mView = null;
    public static final long TAP_TIMEOUT = 1000;
    public boolean mContentWasLoaded = false;

    protected EntryFragment mEntryFragment = null;
    protected final int mPosition;

    private int mStatus = 0;
    private final Stack<Integer> mHistoryAnchorScrollY = new Stack<>();

    protected EntryView(EntryFragment fragment, long entryId, int position) {
        mEntryFragment = fragment;
        mEntryId = entryId;
        mPosition = position;
    }

    protected Context getContext() {
        return mEntryFragment.getContext();
    }
    public boolean CanGoBack() {
        return !mHistoryAnchorScrollY.isEmpty();
    }
    public void ClearHistoryAnchor() {
        mHistoryAnchorScrollY.clear();
        if ( mEntryFragment.mTapZones != null )
            mEntryFragment.mTapZones.Update();
    }

    public void GoBack() {
        if (CanGoBack())
            ScrollTo(mHistoryAnchorScrollY.pop(), false);
        if ( mEntryFragment.mTapZones != null )
            mEntryFragment.mTapZones.Update();
    }

    public void GoTop() {
        AddNavigationHistoryStep();
        ScrollTo(0, false );
    }

    public void AddNavigationHistoryStep() {
        mHistoryAnchorScrollY.push(GetScrollY());
        if ( mEntryFragment.mTapZones != null )
            mEntryFragment.mTapZones.Update();
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

    public void update(boolean invalidateContent ) {
        if ( invalidateContent )
            InvalidateContentCache();
        if ( mEntryFragment.mTapZones != null )
            mEntryFragment.mTapZones.Hide();
        mEntryFragment.mControlPanel.hide();
    }

    public void onStart() {
    }
    public abstract void ScrollToPage(int page);

    public void onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_star);
        if ( item == null )
            return;
        if (mFavorite)
            item.setTitle(R.string.menu_unstar).setIcon(R.drawable.ic_star);
        else
            item.setTitle(R.string.menu_star).setIcon(R.drawable.ic_star_border);
        updateMenuWithIcon(item);
    }

    public void onPageSelected() {

    }

    static public class ProgressInfo {
        public int max;
        public int progress;
        public int step;
    }

    public String getFeedID() {
        if (mCursor != null)
            return mCursor.getString(mFeedIDPos);
        else
            return "";
    }
    public boolean getIsUnReadFromCursor() {
        if (mCursor != null)
            return mCursor.getInt(mIsReadPos) != 1;
        else
            return false;
    }
    public int getForceOrientationFromCursor() {
        if (mCursor != null)
            return mCursor.getInt(mIsLandscapePos);
        else
            return 0;
    }
    public void StatusStartPageLoading() {
        if ( !mContentWasLoaded )
            synchronized (this) {
                if (mStatus == 0)
                    mStatus = Status().Start(R.string.web_page_loading, true);
            }
    }
    public void InvalidateContentCache() {
        if ( mContentWasLoaded )
            SaveScrollPos();
        mContentWasLoaded = false;
    }
    public void onResume() {

    }
    public void onPause() {

    }

    public void setCursor( Cursor cursor ) {
        mCursor = cursor;
        if ( mCursor != null && mCursor.moveToFirst() && !mContentWasLoaded )
            readDataFromDB();
    }
    @SuppressLint("Range")
    public void generateArticleContent( boolean forceUpdate ) {
        Dog.v(String.format("displayEntry view.mScrollY  (entry %s) view.mScrollY = %f", mEntryId, mScrollPartY));
        mEntryFragment.UpdateHeader();
    }

    @SuppressLint("Range")
    public void loadingDataFinished(){
        //Timer.End( loader.getId() );
        //readDataFromDB();
    }

    protected void readDataFromDB() {
        if (mTitlePos == -1) {
            mTitlePos = mCursor.getColumnIndex(TITLE);
            mLinkPos = mCursor.getColumnIndex(FeedData.EntryColumns.LINK);
            mIsFavoritePos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_FAVORITE);
            mIsLandscapePos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_LANDSCAPE);
            mIsReadPos = mCursor.getColumnIndex(FeedData.EntryColumns.IS_READ);
            mFeedIDPos = mCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
        }
        mEntryLink = mCursor.getString(mLinkPos);
        mScrollPartY = readDouble( SCROLL_POS, 0);
        mFavorite = mCursor.getInt(mIsFavoritePos) == 1;
        mTitle = mCursor.getString(mTitlePos);
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
    protected boolean readBooleanWithNullTrue( String fieldName) {
        final int fieldPos = mCursor.getColumnIndex(fieldName);
        return mCursor.getInt(fieldPos) == 1 || mCursor.isNull(fieldPos);
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
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
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
                final EditText editText = new EditText(getContext());
                editText.setText( mTitle );
                final AlertDialog d = new AlertDialog.Builder( getContext() )
                        .setView( editText )
                        .setTitle( R.string.menu_edit_article_title )
                        .setIcon( R.drawable.ic_edit )
                        .setNegativeButton( android.R.string.cancel, null )
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            final Uri uri = getUri();
                            if ( !mEntryFragment.getEntryActivity().mIsNewTask )
                                PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, getUri().toString());
                            new Thread() {
                                @Override
                                public void run() {
                                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                                    ContentValues values = new ContentValues();
                                    final String newTitle = editText.getText().toString();
                                    values.put( TITLE, newTitle );
                                    cr.update(uri, values, null, null);
                                    InvalidateContentCache();
                                }


                            }.start();
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
                mEntryFragment.mTapZones.Update();
                UiUtils.toast( R.string.tap_actions_were_disabled );
                update(true);
                break;
            }


            case R.id.menu_add_entry_shortcut: {
                if (mCursor != null)
                    addArticleShortcut( mEntryFragment.getActivity(),
                                        mCursor.getString(mTitlePos),
                                        mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID)),
                                        Uri.parse( mCursor.getString(mLinkPos) ),
                                        mCursor.getString(mCursor.getColumnIndex( FeedData.EntryColumns.IMAGE_URL )) );
                break;
            }

        }
    }

    private void OpenSettings() {
        getContext().startActivity(new Intent(getContext(), GeneralPrefsActivity.class));
    }

    protected void toggleImageWhiteBackground() {
        PrefUtils.toggleBoolean(STATE_IMAGE_WHITE_BACKGROUND, false) ;
        update(true);
        generateArticleContent(true);
    }

    @NotNull
    public static IconCompat LoadIcon(String iconUrl) {
        Bitmap bitmap =
                iconUrl == null ? null :
                LocalFile.Is( iconUrl ) ? FileUtils.INSTANCE.loadBitmapFromUri( Uri.parse(iconUrl) ) :
                NetworkUtils.downloadImage(iconUrl);
        if ( bitmap == null )
            UiUtils.RunOnGuiThread(() -> UiUtils.toast( R.string.unable_to_load_article_icon ));
        else
            bitmap = UiUtils.getScaledBitmap( bitmap, 32 );
        return (bitmap == null) ?
                IconCompat.createWithResource( MainApplication.getContext(), R.mipmap.ic_launcher ) :
                IconCompat.createWithBitmap(bitmap);
    }

    public void OpenLabelSetup() {
        LabelVoc.INSTANCE.showDialogToSetArticleLabels(getContext(), mEntryId, null);
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
                if ( !mFavorite ) {
                    LabelVoc.INSTANCE.removeLabels(mEntryId);
                    InvalidateContentCache();
                }
            }
        }.start();
        mEntryFragment.getActivity().invalidateOptionsMenu();
        if ( mFavorite ) {
            if ( showToast )
                UiUtils.toast( R.string.entry_marked_favourite );
        } else {
            Snackbar snackbar = Snackbar.make(mEntryFragment.getView().getRootView().findViewById(R.id.pageDownBtn), R.string.removed_from_favorites, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(getContext(), R.color.light_theme_color_primary))
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
        return ContentUris.withAppendedId(mEntryFragment.mBaseUri, mEntryId);
    }
    public void onPrepareOptionsMenu (Menu menu) {
        setItemChecked( menu, R.id.menu_image_white_background, PrefUtils.isImageWhiteBackground());
        setItemChecked( menu, R.id.menu_show_progress_info, PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ));

        setItemChecked( menu, R.id.menu_full_screen, GetIsStatusBarHidden() );
        setItemChecked( menu, R.id.menu_actionbar_visible, !GetIsStatusBarHidden() );
        setItemChecked( menu, R.id.menu_go_back, CanGoBack() );
        setItemChecked( menu, R.id.menu_zoom_shift_enabled, false );
        setItemChecked( menu, R.id.menu_disable_all_tap_actions, mEntryFragment.mTapZones != null );

        hideAllMenuItems(menu);

        setVisible( menu, R.id.menu_star );
        setVisible( menu, R.id.menu_share );
        setVisible( menu, R.id.menu_mark_as_unread );
        setVisible( menu, R.id.menu_article_web_search );
        //showItem( menu, R.id.menu_share_group );
        //showItem( menu, R.id.menu_display_options_group );
        setVisible( menu, R.id.menu_labels );
        //showItem( menu, R.id.menu_reload_group );
        setVisible( menu, R.id.menu_setting );
        setVisible( menu, R.id.menu_close );
        setVisible( menu, R.id.menu_toggle_theme );
        setVisible( menu, R.id.menu_copy_clipboard );
        setVisible( menu, R.id.menu_setting );
        setVisible( menu, R.id.menu_edit_article_title );
        setVisible( menu, R.id.menu_image_white_background );
        setVisible( menu, R.id.menu_disable_all_tap_actions );
        setVisible( menu, R.id.menu_add_entry_shortcut );
    }

    private static void hideAllMenuItems(Menu menu) {
        for (int i = 0; i < menu.size(); i++ )
            if ( menu.getItem(i).hasSubMenu() )
                hideAllMenuItems( menu.getItem(i).getSubMenu() );
            else
                setItemVisible(menu, menu.getItem(i).getItemId(), false );
    }

    protected static float getPageChangeMultiplier() {
        return PrefUtils.isPageUpDown90Pct() ? 0.9F : 0.98F;
    }


    public void setupControlPanelButtonActions() {
        setupButtonAction(R.id.btn_label_setup, false, v -> OpenLabelSetup());
        setupButtonAction(R.id.btn_settings, false, v -> OpenSettings());
        setupButtonAction(R.id.btn_image_white_background, PrefUtils.isImageWhiteBackground(), v -> toggleImageWhiteBackground() );
        setupButtonLongClickAction( R.id.btn_share, v -> mEntryFragment.mMenu.openShare());
        setupButtonLongClickAction( R.id.btn_force_orientation_by_sensor, v -> mEntryFragment.mMenu.openDisplay());
        setupButtonLongClickAction( R.id.btn_force_portrait_orientation_toggle, v -> mEntryFragment.mMenu.openDisplay());
        setupButtonLongClickAction( R.id.btn_force_landscape_orientation_toggle, v -> mEntryFragment.mMenu.openDisplay());
        setupButtonLongClickAction( R.id.btn_reload, v -> mEntryFragment.mMenu.openReload());
    }
    protected void setupButtonAction(int viewId, boolean checked, View.OnClickListener click ) {
        mEntryFragment.mControlPanel.setupButtonAction(viewId, checked, click );
    }
    protected void setupButtonLongClickAction(int viewId, View.OnClickListener click ) {
        mEntryFragment.mControlPanel.setupButtonLongClickAction( viewId, click );
    }
    protected void EndStatus() {
        synchronized (this) {
            if (mStatus != 0)
                Status().End(mStatus);
            mStatus = 0;
        }
    }

    public static void share( Context context, Uri entryUri, String title ) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.setType( Constants.MIMETYPE_TEXT_PLAIN);
        if ( LocalFile.Is( entryUri ) ) {
            intent.putExtra(Intent.EXTRA_STREAM, entryUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (entryUri.toString().toLowerCase().endsWith("pdf"))
                intent.setType(Constants.MIMETYPE_PDF);
            else if (EPUB.Is(entryUri))
                intent.setType(Constants.MIMETYPE_EPUB);
            else if (FB2.IsFB2(entryUri))
                intent.setType(Constants.MIMETYPE_FB2);
        } else
            intent.putExtra( Intent.EXTRA_TEXT, entryUri.toString() );

        context.startActivity(
                Intent.createChooser( intent, MainApplication.getContext().getString(R.string.menu_share) ) );
    }
}

