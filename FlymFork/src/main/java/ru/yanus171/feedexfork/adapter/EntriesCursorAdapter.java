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

package ru.yanus171.feedexfork.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.MenuItem;

import static android.view.View.TEXT_DIRECTION_ANY_RTL;
import static android.view.View.TEXT_DIRECTION_RTL;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.MainApplication.mImageFileVoc;
import static ru.yanus171.feedexfork.fragment.EntryFragment.NEW_TASK_EXTRA;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.CATEGORY_LIST_SEP;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.RemoveHeaders;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.RemoveTables;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_CATEGORY;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_TEXT_PREVIEW;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_URL;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.NEW_ARTICLE_INDICATOR_RES_ID;
import static ru.yanus171.feedexfork.utils.Theme.STARRED_ARTICLE_INDICATOR_RES_ID;
import static ru.yanus171.feedexfork.utils.UiUtils.SetFont;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;
import static ru.yanus171.feedexfork.view.EntryView.ShowLinkMenu;
import static ru.yanus171.feedexfork.view.EntryView.getAlign;
import static ru.yanus171.feedexfork.view.EntryView.isTextRTL;
import static ru.yanus171.feedexfork.view.MenuItem.ShowMenu;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private static final String STATE_TEXT_LINE_COUNT_ENTRY_ID = "TEXT_LINE_COUNT_ENTRY_ID";
    private static final String STATE_TEXT_LINE_COUNT = "TEXT_LINE_COUNT";
    private final int MAX_LINES_STEP = 50;
    public static final String STATE_TEXTSHOWN_ENTRY_ID = "STATE_TEXTSHOWN_ENTRY_ID";
    private final HashMap<Long, EntryContent> mContentVoc = new HashMap<>();
    private final Uri mUri;
    private final Context mContext;
    private final boolean mShowFeedInfo;
    private final boolean mShowEntryText;
    private final boolean mShowUnread;
    private final HomeActivity mActivity;
    private boolean mIsLoadImages;
    private boolean mBackgroundColorLight = false;
    FeedFilters mFilters = null;
    public static final HashSet<String> mMarkAsReadList = new HashSet<>();

    private int mIdPos, mTitlePos, mFeedTitlePos, mUrlPos, mMainImgPos, mDatePos, mIsReadPos,
        mAuthorPos, mImageSizePos, mFavoritePos, mMobilizedPos, mFeedIdPos, mFeedNamePos,
        mAbstractPos, mIsNewPos, mTextLenPos, mCategoriesPos;
    public static int mEntryActivityStartingStatus = 0;
    public boolean mIgnoreClearContentVocOnCursorChange = false;
    public boolean mIsNewTask = false;
    private boolean mNeedScrollToTopExpandedArticle = false;

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo, boolean showEntryText, boolean showUnread, HomeActivity activity) {
        super(context, R.layout.item_entry_list, cursor, 0);
        //Dog.v( String.format( "new EntriesCursorAdapter( %s, showUnread = %b )", uri.toString() ,showUnread ) );
        mContext = context;
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        mShowEntryText = showEntryText;
        mShowUnread = showUnread;
        mIsLoadImages = true;
        mActivity = activity;
        //SetIsReadMakredList();

        mMarkAsReadList.clear();

        reinit(cursor);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }
    public int getItemPosition( long feedID ) {
        for( int i = 0; i < getCount(); i++ )
            if ( getItemId( i ) == feedID )
                return i;
        return -1;
    }
    public static Uri EntryUri( long id ) {
        return EntryColumns.CONTENT_URI( id ); //ContentUris.withAppendedId(mUri, id);
    }

    private static boolean HasMoreText( ViewHolder holder) {
        return Build.VERSION.SDK_INT >= 16 && holder.textTextView.getLineCount() > holder.textTextView.getMaxLines();
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {

        final Vibrator vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);

        view.findViewById(R.id.textDate).setVisibility(View.GONE);
        view.findViewById(android.R.id.text2).setVisibility(View.GONE);
        final int cursorPosition = cursor.getPosition();
        final String feedId = cursor.getString(mFeedIdPos);
        //final long entryID = cursor.getLong(mIdPos);
        final String feedTitle = cursor.getString(mFeedTitlePos);

        if (view.getTag(R.id.holder) == null) {
            final ViewHolder holder = new ViewHolder();
            holder.titleTextView = SetupTextView(view, android.R.id.text1);
            holder.urlTextView = SetupSmallTextView(view, R.id.textUrl);
            holder.textPreviewTextView = SetupSmallTextView(view, R.id.textTextPreview);
            holder.textTextView = SetupTextView(view, R.id.textSource);

            if (mShowEntryText) {
                holder.dateTextView = SetupSmallTextView(view, R.id.textDate);
            } else {
                holder.dateTextView = SetupSmallTextView(view, android.R.id.text2);
            }
            holder.authorTextView = SetupSmallTextView(view, R.id.textAuthor);
            holder.imageSizeTextView = SetupSmallTextView(view, R.id.imageSize);
            holder.mainImgView = view.findViewById(R.id.main_icon);
            //holder.mainImgLayout = view.findViewById(R.id.main_icon_layout);
            holder.layoutControls = view.findViewById(R.id.layout_controls);
            holder.starImgView = view.findViewById(R.id.favorite_icon);
            holder.mobilizedImgView = view.findViewById(R.id.mobilized_icon);
            holder.readImgView = view.findViewById(R.id.read_icon);
            holder.readImgView.setVisibility(PrefUtils.IsShowReadCheckbox() ? View.VISIBLE : View.GONE); //
            holder.textLayout = view.findViewById(R.id.textLayout);
            holder.readToggleSwypeBtnView = view.findViewById(R.id.swype_btn_toggle_read);
            holder.starToggleSwypeBtnView = view.findViewById(R.id.swype_btn_toggle_star);
            holder.newImgView = view.findViewById(R.id.new_icon);
            holder.contentImgView1 = view.findViewById(R.id.image1);
            holder.contentImgView2 = view.findViewById(R.id.image2);
            holder.contentImgView3 = view.findViewById(R.id.image3);
            holder.openArticle = SetupSmallTextView(view, R.id.textSourceOpenArticle);
            holder.showMore = SetupSmallTextView(view, R.id.textSourceShowMore);
            holder.collapsedBtn = view.findViewById(R.id.collapsed_btn);
            holder.categoriesTextView = SetupSmallTextView(view, R.id.textCategories);
            holder.labelTextView = SetupSmallTextView(view, R.id.textLabel);
            holder.textSizeProgressBar = view.findViewById(R.id.progressBar);
            holder.bottomEmptyPage = view.findViewById(R.id.bottomEmptyPage);
            holder.collapseBtnBottom = view.findViewById(R.id.collapse_btn_bottom);
            view.setTag(R.id.holder, holder);

            //final View.OnClickListener openArticle = view12 -> OpenArticle(view12.getContext(), holder.entryID, holder.isTextShown(), "");

            holder.openArticle.setTextColor( Theme.GetTextColorReadInt() );
            holder.openArticle.setOnClickListener(view12 -> {
                String searchText = "";
                if ( holder.isTextShown() ) {
                    CharSequence text = holder.textTextView.getText();
                    String[] list = TextUtils.split( text.toString(), "[\\.|\\,|\\?|\\!|\\:|\\;|\\-]" );
                    if ( list.length > 1 ) {
                        searchText = list[list.length - 2].replace( "\n", "" ).trim();
                    }
                }
                OpenArticle(view12.getContext(), holder.entryID, holder.isTextShown(), searchText);


            });

            if ( Build.VERSION.SDK_INT >= 16 ) {
                holder.showMore.setTextColor(Theme.GetTextColorReadInt());
                holder.showMore.setOnClickListener(v -> {
                    holder.textTextView.setMaxLines(holder.textTextView.getMaxLines() + MAX_LINES_STEP);
                    PrefUtils.putInt( STATE_TEXT_LINE_COUNT, holder.textTextView.getMaxLines() );
                    PrefUtils.putLong( STATE_TEXT_LINE_COUNT_ENTRY_ID, holder.entryID );
                });
                holder.showMore.getViewTreeObserver().addOnGlobalLayoutListener( () -> {
                    final boolean visible = holder.isTextShown() && HasMoreText( holder );
                    holder.showMore.setVisibility( visible ? View.VISIBLE : View.GONE );
                    if ( visible ) {
                        final int count = holder.textTextView.getLineCount() / MAX_LINES_STEP;
                        final int page = holder.textTextView.getMaxLines() / MAX_LINES_STEP;
                        holder.showMore.setText( MainApplication.getContext().getString( R.string.show_more ) +
                                                         String.format( " (%d/%d)", page, count ) );
                    }
                } );
            }

            final View.OnClickListener collapseListener = view13 -> {
                final long shownId = PrefUtils.getLong(STATE_TEXTSHOWN_ENTRY_ID, 0);
                holder.textTextView.setMaxLines( MAX_LINES_STEP );
                if (shownId == holder.entryID) {
                    PrefUtils.putLong(STATE_TEXTSHOWN_ENTRY_ID, 0);
                } else {
                    SetIsRead(holder.entryID, true, true);
                    PrefUtils.putLong(STATE_TEXTSHOWN_ENTRY_ID, holder.entryID);
                    EntryView.mImageDownloadObservable.notifyObservers(new ListViewTopPos(GetPosByID(holder.entryID)));
                    mNeedScrollToTopExpandedArticle = true;
                }
                MainApplication.getContext().getContentResolver().notifyChange(mUri, null );
            };

            holder.collapsedBtn.setOnClickListener( collapseListener );
            holder.collapseBtnBottom.setOnClickListener( collapseListener );
            holder.categoriesTextView.setOnClickListener( collapseListener );
            holder.layoutControls.setOnClickListener( collapseListener );

            view.findViewById(R.id.layout_ontouch).setOnTouchListener(new View.OnTouchListener() {
                private int paddingX = 0;
                private int paddingY = 0;
                private int initialx = 0;
                private int initialy = 0;
                private int currentx = 0;
                private int currenty = 0;
                private boolean wasVibrateRead = false, wasVibrateStar = false;
                private boolean isPress = false;
                private long downTime = 0;
                //private boolean wasMove = false;
                //private  ViewHolder viewHolder;

                public boolean onTouch(View v, MotionEvent event) {
                    final int minX = 40;
                    final int minY = 20;
                    final int MIN_X_TO_VIEW_ARTICLE = UiUtils.mmToPixel(5);
                    final ViewHolder holder = (ViewHolder) ((ViewGroup) v.getParent().getParent()).getTag(R.id.holder);
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //Dog.v("onTouch ACTION_DOWN");
                        paddingX = 0;
                        paddingY = 0;
                        initialx = (int) event.getX();
                        initialy = (int) event.getY();
                        currentx = (int) event.getX();
                        currenty = (int) event.getY();
                        downTime = SystemClock.elapsedRealtime();
                        //wasMove = false;
                        view.getParent().requestDisallowInterceptTouchEvent(true);

                        isPress = true;
                        UiUtils.RunOnGuiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isPress) {
                                    final int pos = getItemPosition(holder.entryID);
                                    ArrayList<Integer> posList = new ArrayList<>();

                                    final MenuItem[] items = {
                                        new MenuItem(R.string.context_menu_delete, R.drawable.delete, (_1, _2) ->
                                            EntriesListFragment.ShowDeleteDialog(view.getContext(), holder.titleTextView.getText().toString(), holder.entryID, holder.entryLink) ),
                                        new MenuItem(R.string.menu_mark_upper_as_read, R.drawable.ic_arrow_drop_up, (_1, _2) -> {
                                            for (int i = 0; i < pos; i++)
                                                posList.add(i);
                                            ShowMarkPosListAsReadDialog(context, R.string.question_mark_upper_as_read, posList);
                                        } ),
                                        new MenuItem(R.string.menu_mark_lower_as_read, R.drawable.arrow_drop_down, (_1, _2) -> {
                                            for (int i = pos + 1; i < getCount(); i++)
                                                posList.add(i);
                                            ShowMarkPosListAsReadDialog(context, R.string.question_mark_lower_as_read, posList);
                                            }),
                                        new MenuItem(R.string.menu_edit_labels, R.drawable.ic_label, (_1, _2) ->
                                            LabelVoc.INSTANCE.showDialogToSetArticleLabels(context, holder.entryID, EntriesCursorAdapter.this)),
                                        new MenuItem(R.string.menu_share, R.drawable.ic_share, (_1, _2) ->
                                            context.startActivity(Intent.createChooser( new Intent(Intent.ACTION_SEND)
                                                                                        .putExtra(Intent.EXTRA_TEXT, holder.entryLink )
                                                                                        .putExtra(Intent.EXTRA_SUBJECT, holder.titleTextView.getText().toString())
                                                                                        .setType(Constants.MIMETYPE_TEXT_PLAIN),
                                                                                        context.getString(R.string.menu_share))))
                                    };
                                    ShowMenu(items, String.valueOf(holder.titleTextView.getText()), context );
                                    //wasMove = true;
                                }
                            }
                        }, ViewConfiguration.getLongPressTimeout());
                    }
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {

                        currentx = (int) event.getX();
                        currenty = (int) event.getY();
                        paddingX = currentx - initialx;
                        paddingY = currenty - initialy;
                        //Dog.v("onTouch ACTION_MOVE " + paddingX + ", " + paddingY);

                        //allow vertical scrolling
                        if ((initialx < minX * 2 || Math.abs(paddingY) > Math.abs(paddingX)) &&
                            Math.abs(initialy - event.getY()) > minY &&
                            view.getParent() != null)
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        if (Math.abs(initialy - event.getY()) > minY)
                            isPress = false;
                        holder.readToggleSwypeBtnView.setVisibility(View.VISIBLE);
                        holder.starToggleSwypeBtnView.setVisibility(View.VISIBLE);
                    }


                    int overlap = holder.readToggleSwypeBtnView.getWidth() / 2;
                    int threshold = holder.readToggleSwypeBtnView.getWidth();
                    if (threshold < minX)
                        threshold = minX + 5;
                    int max = threshold + overlap;

                    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        isPress = false;
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            //Dog.v("onTouch ACTION_UP");
                            if (mEntryActivityStartingStatus == 0 &&
                                currentx > MIN_X_TO_VIEW_ARTICLE &&
                                Math.abs(paddingX) < minX &&
                                Math.abs(paddingY) < minY &&
                                (IsUnderView(event, holder.titleTextView, v) || IsUnderView(event, holder.mainImgView, v) || IsUnderView(event, holder.dateTextView, v) || IsUnderView(event, holder.authorTextView, v)) &&
                                SystemClock.elapsedRealtime() - downTime < ViewConfiguration.getLongPressTimeout()) {
                                mEntryActivityStartingStatus = Status().Start(R.string.article_opening, true);
                                OpenArticle(v.getContext(), holder.entryID, holder.isTextShown(), "");
                            } else if (Math.abs(paddingX) > Math.abs(paddingY) && paddingX >= threshold)
                                toggleReadState(holder, view);
                            else if (Math.abs(paddingX) > Math.abs(paddingY) && paddingX <= -threshold)
                                toggleFavoriteState(view);
                        } else {
                            //Dog.v("onTouch ACTION_CANCEL");
                        }
                        paddingX = 0;
                        paddingY = 0;
                        initialx = 0;
                        initialx = 0;
                        currentx = 0;
                        wasVibrateRead = false;
                        wasVibrateStar = false;

                        if (view.getParent() != null)
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        holder.readToggleSwypeBtnView.setVisibility(View.GONE);
                        holder.starToggleSwypeBtnView.setVisibility(View.GONE);

                    }

                    if (paddingX > max)
                        paddingX = max;
                    if (paddingX < -max)
                        paddingX = -max;

                    // block left drawable area
                    if (initialx < minX * 2) {
                        isPress = false;
                        paddingX = 0;
                    }

                    if (Math.abs(paddingX) < minX)
                        paddingX = 0;

                    if (Math.abs(paddingY) < minY)
                        paddingY = 0;

                    // no long tap when large move
                    if (Math.abs(paddingX) > minX || Math.abs(paddingY) > minY)
                        isPress = false;

                    final boolean prefVibrate = PrefUtils.getBoolean(VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE, true);
                    if (prefVibrate && Math.abs(paddingX) > Math.abs(paddingY) && paddingX >= threshold) {
                        if (!wasVibrateRead) {
                            vibrator.vibrate(VIBRATE_DURATION);
                            wasVibrateRead = true;
                        }
                        //holder.readToggleSwypeBtnView.setVisibility(View.VISIBLE);
                    } else
                        wasVibrateRead = false;

                    if (prefVibrate && Math.abs(paddingX) > Math.abs(paddingY) && paddingX <= -threshold) {
                        if (!wasVibrateStar) {
                            vibrator.vibrate(VIBRATE_DURATION);
                            wasVibrateStar = true;
                        }
                        //holder.starToggleSwypeBtnView.setVisibility( View.VISIBLE );
                    } else
                        wasVibrateStar = false;

                    v.setPadding(Math.max(paddingX, 0), 0, paddingX < 0 ? -paddingX : 0, 0);

                    //Dog.v(" onTouch paddingX = " + paddingX + ", paddingY= " + paddingY + ", minX= " + minX + ", minY= " + minY + ", isPress = " + isPress + ", threshold = " + threshold);
                    return true;
                }

                private boolean IsUnderView(MotionEvent event, View view, View rootView) {
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();
                    int[] location = new int[2];
                    view.getLocationInWindow(location);
                    int[] locationRoot = new int[2];
                    rootView.getLocationInWindow(locationRoot);
                    final int left = location[0] - locationRoot[0];//view.getLeft();
                    final int top = location[1] - locationRoot[1];//view.getTop();
                    final int right = left + view.getWidth();
                    final int bottom = top + view.getHeight();
                    return x > left && x < right && y > top && y < bottom;
                }
            });
        }

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        holder.entryID = cursor.getLong(mIdPos);
        holder.entryLink = cursor.getString(mUrlPos);

        final View.OnClickListener manageLabels = PrefUtils.getBoolean( "label_setup_by_tap_on_date", false ) ?
            view1 -> LabelVoc.INSTANCE.showDialogToSetArticleLabels(context, holder.entryID, EntriesCursorAdapter.this ) :
            null;
        holder.urlTextView.setOnClickListener( manageLabels );
        holder.dateTextView.setOnClickListener( manageLabels );
        //holder.authorTextView.setOnClickListener( manageLabels );

        final int lineCount = holder.isTextShown() && PrefUtils.getLong(STATE_TEXT_LINE_COUNT_ENTRY_ID, 0 ) == holder.entryID ? PrefUtils.getInt( STATE_TEXT_LINE_COUNT, MAX_LINES_STEP ) : MAX_LINES_STEP;
        holder.textTextView.setMaxLines( lineCount );

        holder.isRead = isInMarkAsReadList(EntryUri(holder.entryID).toString()) || EntryColumns.IsRead(cursor, mIsReadPos);

        if ( mMapFavourite.containsKey(holder.entryID) )
            holder.isFavorite = mMapFavourite.get( holder.entryID );
        else
            holder.isFavorite = cursor.getInt(mFavoritePos) == 1;

        //mBackgroundColorLight =  daysTo % 2 == 1; //mShowEntryText && cursor.getPosition() % 2 == 1;

        holder.readToggleSwypeBtnView.setVisibility( View.GONE );
        holder.starToggleSwypeBtnView.setVisibility( View.GONE );

        holder.dateTextView.setVisibility(View.VISIBLE);

        String titleText = GetTitle(cursor);
        holder.titleTextView.setVisibility( titleText.isEmpty() ? View.GONE : View.VISIBLE );
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(cursor.getLong(mDatePos));
        Calendar currentDate = Calendar.getInstance();
        final boolean isTextShown = mShowEntryText || holder.isTextShown();
        boolean isToday = currentDate.get( Calendar.DAY_OF_YEAR ) == date.get( Calendar.DAY_OF_YEAR );
        if ( PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ) || isToday )
            holder.titleTextView.setText( Html.fromHtml( "<b>" + titleText + "</b>" ) );
        else
            holder.titleTextView.setText(titleText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            holder.titleTextView.setTextDirection( isTextRTL( titleText ) ? TEXT_DIRECTION_RTL : TEXT_DIRECTION_ANY_RTL );
        holder.titleTextView.setMaxLines( isTextShown ? 20 : 5 );

        holder.urlTextView.setText(cursor.getString(mUrlPos));

        String feedName = cursor.getString(mFeedNamePos);

        holder.collapsedBtn.setVisibility( mShowEntryText ? View.GONE : View.VISIBLE );
        holder.collapsedBtn.setImageResource( holder.isTextShown() ? R.drawable.ic_keyboard_arrow_down_gray : R.drawable.ic_keyboard_arrow_right_gray );
        SetFont(holder.titleTextView, 1 );
        holder.mainImgView.setVisibility( mIsLoadImages ? View.VISIBLE : View.GONE  );
        if ( !isTextShown && PrefUtils.getBoolean( "setting_show_article_icon", true ) ) {
            String mainImgUrl = cursor.getString(mMainImgPos);
            mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(holder.entryLink, mainImgUrl);

            ColorGenerator generator = ColorGenerator.DEFAULT;
            int color = generator.getColor(feedId); // The color is specific to the feedId (which shouldn't change)
            if ( mainImgUrl != null && mImageFileVoc.isExists(mainImgUrl) ) {
                final int dim = UiUtils.dpToPixel(70);
                //String lettersForName = feedName != null ? (feedName.length() < 2 ? feedName.toUpperCase() : feedName.substring(0, 2).toUpperCase()) : "";
                //TextDrawable letterDrawable = TextDrawable.builder().buildRect(lettersForName, color);

                Glide.with(context).load(mainImgUrl)
                    .override(dim, dim)
                    .centerCrop()
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if ( mIsLoadImages ) {
                                holder.mainImgView.setVisibility(View.GONE);
                                return true;
                            } else
                                return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.mainImgView.setVisibility( View.VISIBLE );
                            return false;
                        }
                    })
                    //.placeholder(R.drawable.cup_new_empty)
                    //.placeholder(letterDrawable)
                    //.error(letterDrawable)
                    .into(holder.mainImgView);
            } else {
                Glide.with(context).clear(holder.mainImgView);
                //holder.mainImgView.setImageDrawable(letterDrawable);
            }
        } else
            holder.mainImgView.setVisibility( View.GONE );


        holder.isMobilized = FileUtils.INSTANCE.isMobilized( cursor.getString(mUrlPos), cursor, mMobilizedPos, mIdPos );

        final long textSize = cursor.isNull( mTextLenPos ) ? FileUtils.INSTANCE.LinkToFile( cursor.getString( mUrlPos ) ).length() : cursor.getInt( mTextLenPos );
        String textSizeText = " " + GetTextSizeText( textSize );
        if (mShowFeedInfo && mFeedNamePos > -1) {
            if (feedName != null) {
                holder.dateTextView.setText(Html.fromHtml("<font color='#247ab0'>" + feedName + "</font>" + Constants.COMMA_SPACE + StringUtils.getDateTimeString(cursor.getLong(mDatePos))) + textSizeText);
            } else {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)) + textSizeText);
            }
        } else {
            holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)) + textSizeText);
        }

        {
            final int max = PrefUtils.getIntFromText( "atricle_list_size_progressbar_maxsize", 50 ) * KBYTE;
            if ( max > 0 ) {
                holder.textSizeProgressBar.setMax( max );
                holder.textSizeProgressBar.setProgress( (int)textSize );
                holder.textSizeProgressBar.setVisibility(View.VISIBLE );
            } else
                holder.textSizeProgressBar.setVisibility(View.GONE );
        }

        final int imageSize = cursor.getInt( mImageSizePos );
        if ( PrefUtils.CALCULATE_IMAGES_SIZE() && imageSize  != 0 ) {
            holder.imageSizeTextView.setVisibility( View.VISIBLE );
            holder.imageSizeTextView.setText(GetImageSizeText(imageSize));
        } else
            holder.imageSizeTextView.setVisibility( View.GONE );

        holder.authorTextView.setText( cursor.getString( mAuthorPos ) );
        UpdateReadView( holder, view );

        final boolean showUrl = PrefUtils.getBoolean( SHOW_ARTICLE_URL, false ) ;
        holder.urlTextView.setVisibility( showUrl ? View.VISIBLE : View.GONE );

        final boolean showTextPreview = PrefUtils.getBoolean( SHOW_ARTICLE_TEXT_PREVIEW, false ) &&
            cursor.getString(mAbstractPos) != null && !holder.isTextShown();
        holder.textPreviewTextView.setVisibility( showTextPreview ? View.VISIBLE : View.GONE );
        if ( showTextPreview ) {
            String s = getBoldText(GetHtmlAligned(cursor.getString(mAbstractPos))).toString();
            s = s.replace( "\n", " " );
            s = s.replace( holder.titleTextView.getText().toString(), "" );
            s = s.replace( "￼", "" );
            s = s.replace( "\t", " " );
            for ( int i = 0; i < 2; i++ )
                s = s.replace( "  ", " " );
            s = s.trim();
            if ( s.startsWith( "." ) )
                s = s.substring( 1 );
            s = s.trim();
            if ( !s.isEmpty() ) {
                holder.textPreviewTextView.setText(getBoldText(s));
                holder.textPreviewTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        OpenArticle(holder.textPreviewTextView.getContext(), holder.entryID, false, "");
                    }
                });
            } else
                holder.textPreviewTextView.setVisibility( View.GONE );
        }


        UpdateStarImgView(holder);
        holder.mobilizedImgView.setVisibility(PrefUtils.getBoolean( "show_full_text_indicator", false ) && holder.isMobilized? View.VISIBLE : View.GONE);

        UpdateReadView(holder, view);
        holder.readImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReadState(holder, view);
            }
        });
        holder.starImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavoriteState(view);
            }
        });

        holder.categoriesTextView.setVisibility(View.GONE);
        if ( PrefUtils.getBoolean( SHOW_ARTICLE_CATEGORY, true ) ) {
            final String categories = cursor.isNull(mCategoriesPos) ? "" : cursor.getString(mCategoriesPos);
            if (!categories.isEmpty()) {
                holder.categoriesTextView.setVisibility(View.VISIBLE);
                holder.categoriesTextView.setText(CategoriesToOutput(categories) );
            }
        }
        //view.findViewById(R.id.text2Layout).setOnClickListener(manageLabels);

        UpdateLabelText(holder);
        holder.labelTextView.setOnClickListener(manageLabels);

        holder.contentImgView1.setVisibility( View.GONE );
        holder.contentImgView2.setVisibility( View.GONE );
        holder.contentImgView3.setVisibility( View.GONE );
        holder.openArticle.setVisibility(View.GONE );
        holder.collapseBtnBottom.setVisibility(View.GONE );
        holder.showMore.setVisibility( View.GONE );
        if ( isTextShown ) {
            holder.openArticle.setVisibility(View.VISIBLE );
            holder.collapseBtnBottom.setVisibility(View.VISIBLE );
            holder.textTextView.setVisibility(View.VISIBLE);
            final String html = cursor.getString(mAbstractPos) == null ? "" : GetHtmlAligned(cursor.getString(mAbstractPos));
            holder.textTextView.setLinkTextColor( Theme.GetColorInt(LINK_COLOR, R.string.default_link_color) );
            //holder.textTextView.setTextIsSelectable( true );
            SetupEntryText(holder, getBoldText(html), NeedToOpenArticle( html ) || HasMoreText( holder ) );
            //holder.textTextView.setMovementMethod(LinkMovementMethod.getInstance());
            //final boolean isMobilized = FileUtils.INSTANCE.isMobilized( holder.entryLink, cursor );
            //if ( html.contains( "<img" ) /*|| isMobilized*/ ) {
                EntryContent content = mContentVoc.get(holder.entryID);
                if (content != null && content.GetIsLoaded() ) {
                    SetContentImage(context, holder.contentImgView1, 0, content.mImageUrlList);
                    SetContentImage(context, holder.contentImgView2, 1, content.mImageUrlList);
                    SetContentImage(context, holder.contentImgView3, 2, content.mImageUrlList);
                    SetupEntryText(holder, content.mText, content.mNeedToOpenArticle  || HasMoreText( holder ));
                    if ( mNeedScrollToTopExpandedArticle ) {
                        mNeedScrollToTopExpandedArticle = false;
                        UiUtils.RunOnGuiThread(new Runnable() {
                            @Override
                            public void run() {
                                EntryView.mImageDownloadObservable.notifyObservers(new ListViewTopPos(GetPosByID(holder.entryID)));
                            }
                        }, 100);
                    }
                } else if ( content == null ) {
                    content = new EntryContent();
                    content.mID = holder.entryID;
                    content.mHTML = html;
                    content.mTitle = holder.titleTextView.getText().toString();
                    if ( mFilters != null )
                        content.mHTML = mFilters.removeText(content.mHTML, DB_APPLIED_TO_CONTENT );
                    content.mIsMobilized = false;//isMobilized;
                    content.mLink = holder.entryLink;
                    mContentVoc.put( holder.entryID, content );
                    final EntryContent contentFinal = content;
                    new Thread() {
                        @Override
                        public void run() {
                            contentFinal.Load();
                        }
                    }.start();
                }
            //}

        } else
            holder.textTextView.setVisibility(View.GONE);

        /*Display display = ((WindowManager) context.getSystemService( Context.WINDOW_SERVICE ) ).getDefaultDisplay();
        int or = display.getOrientation();
        if (or == Configuration.ORIENTATION_LANDSCAPE) {
            holder.titleTextView.setSingleLine();
            holder.mainImgView.setMaxHeight(  );
        }*/
        final boolean isNew = PrefUtils.getBoolean( "show_new_icon", true ) && EntryColumns.IsNew( cursor, mIsNewPos ) && !isInWasVisibleList(EntryUri(holder.entryID).toString());
        holder.newImgView.setVisibility( isNew ? View.VISIBLE : View.GONE );
        holder.newImgView.setImageResource(Theme.GetResID( NEW_ARTICLE_INDICATOR_RES_ID ) );

        holder.bottomEmptyPage.setVisibility( View.GONE );
        if ( cursor.getPosition() == getCount() - 1  ) {
            holder.bottomEmptyPage.setVisibility( View.VISIBLE );
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity)holder.bottomEmptyPage.getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            holder.bottomEmptyPage.setMinHeight((int) (displayMetrics.heightPixels * 0.7));
        }
    }

    private boolean isInMarkAsReadList(String entryUri)  {
        return mMarkAsReadList.contains(entryUri);
    }
    private boolean isInWasVisibleList(String entryUri)  {
        return mMarkAsReadList.contains(entryUri);
    }

    private void UpdateLabelText(ViewHolder holder) {
        boolean visible = holder.isFavorite && !LabelVoc.INSTANCE.getLabelIDs(holder.entryID ).isEmpty();
        holder.labelTextView.setVisibility( visible ? View.VISIBLE : View.GONE );
        if ( visible )
            holder.labelTextView.setText(Html.fromHtml(LabelVoc.INSTANCE.getStringList(holder.entryID )) );
    }

    private Spanned getBoldText(String html) {
        String result = PrefUtils.getBoolean(PrefUtils.ENTRY_FONT_BOLD, false ) ? "<b>" + html + "</b>" : html;
        return Html.fromHtml(result);
    }

    public static String CategoriesToOutput(String categories) {
        String[] list = TextUtils.split(categories, CATEGORY_LIST_SEP);
        for ( int i = 0; i < list.length; i++ )
            list[i] = "#" + list[i].replace( "#", "" );
        return TextUtils.join(", ", list );
    }

    @NotNull
    private String GetTitle(Cursor cursor) {
        String text = cursor.isNull( mTitlePos ) ? "" : cursor.getString(mTitlePos);
        if ( mFilters != null )
            text = mFilters.removeText(text, DB_APPLIED_TO_TITLE );
        return text;
    }

    @NotNull
    private String GetHtmlAligned(String html) {
        return "<p align='" + getAlign(html) + "'>" +  html + "</p>";
    }

    public void setFilter( FeedFilters filters ) {
        mFilters = filters;
    }

    class EntryContent {
        long mID = 0L;
        String mLink;
        String mTitle;
        ArrayList<Uri> mImageUrlList = new ArrayList<>();
        Spanned mText;
        String mHTML;
        boolean mIsLoaded = false;
        boolean mIsMobilized = false;
        boolean mNeedToOpenArticle = false;
        boolean GetIsLoaded() {
            synchronized ( this ) {
                return mIsLoaded;
            }
        }
        void Load() {
            //if ( !isEntryIDActive( holder.entryID ) )
            //    return;
            final ArrayList<String> imagesToDl = new ArrayList<>();
            final ArrayList<Uri> allImages = new ArrayList<>();
            String temp = mHTML;
            mIsMobilized = FileUtils.INSTANCE.isMobilized( mLink, null, 0, 0 );
            if ( mIsMobilized )
                temp = FileUtils.INSTANCE.loadMobilizedHTML( mLink, null );
            temp = RemoveTables( temp );
            temp = RemoveHeaders( temp );
            temp = temp.replaceAll( "<iframe(.|\\n)*?/iframe>", "" );
            temp = temp.replaceAll( "<div(.|\\n)*?>", "" );
            temp = temp.replaceAll( "</div>", "" );
            //temp = temp.replaceAll( "<br>", " " );
            temp = temp.replaceAll( "\n", " " );
            temp = HtmlUtils.replaceImageURLs( temp,
                                               "",
                                               mID,
                                               mLink,
                                               true,
                                               imagesToDl,
                                               mImageUrlList,
                                               3 );
            temp = temp.replace(mTitle, "");

            mText = getBoldText( GetHtmlAligned( temp ));
            mNeedToOpenArticle = NeedToOpenArticle(temp);
            synchronized ( this ) {
                mIsLoaded = true;
            }
            EntryView.NotifyToUpdate( mID, mLink, false );
        }
    }

    private static boolean NeedToOpenArticle(String temp ) {
        return temp.contains( "<img" );
    }

    private void OpenArticle(Context context, long entryID, boolean isExpanded, String searchText ) {
        context.startActivity(FetcherService.GetEntryActivityIntent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mUri, entryID))
                                   .putExtra( "SCROLL_TEXT", searchText )
                                   .putExtra( NEW_TASK_EXTRA, mActivity.mIsNewTask )
                                   .putExtra( EntryFragment.WHERE_SQL_EXTRA, mActivity.mEntriesFragment.GetWhereSQL() ));
        if( isExpanded ) {
            EntryView.mImageDownloadObservable.notifyObservers(new ListViewTopPos(GetPosByID( entryID ) ) );
            PrefUtils.putLong( STATE_TEXTSHOWN_ENTRY_ID, 0 );
        }
    }

    static public void ShowDialog(Context context, int messageID, final Runnable action ) {
        AlertDialog dialog = new AlertDialog.Builder(context) //
            .setIcon(android.R.drawable.ic_dialog_alert) //
            .setTitle( R.string.confirmation ) //
            .setMessage( messageID ) //
            .setPositiveButton(android.R.string.yes, (dialog1, which) -> new Thread() {
                @Override
                public void run() {
                    action.run();
                }
            }.start()).setNegativeButton(android.R.string.no, null).create();
        dialog.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }

    public void ShowMarkPosListAsReadDialog(Context context, int confirmID, ArrayList<Integer> posList) {
        ShowDialog(context, confirmID, () -> {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            ArrayList<Long> ids = new ArrayList<>();
            for ( int pos: posList)
                ids.add(getItemId(pos));
            String where = BaseColumns._ID + " IN (" + TextUtils.join(",", ids) + ')';
            cr.update(EntryColumns.CONTENT_URI, FeedData.getReadContentValues(), where, null);
        });
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span, final Context context)
    {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                ShowLinkMenu(span.getURL(), strBuilder.subSequence(start, end ).toString(), context);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    protected void SetTextViewHTMLWithLinks(TextView textView, Spanned spanned)
    {
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(spanned);
        URLSpan[] urls = strBuilder.getSpans(0, spanned.length(), URLSpan.class);
        for(URLSpan span : urls)
            makeLinkClickable(strBuilder, span, textView.getContext());
        textView.setText(strBuilder);
        //textView.setMovementMethod(LinkMovementMethod.getInstance());
        //justify( textView );
    }

    public static void justify(final TextView textView) {

        final AtomicBoolean isJustify = new AtomicBoolean(false);

        final String textString = textView.getText().toString();

        final TextPaint textPaint = textView.getPaint();

        final SpannableStringBuilder builder = new SpannableStringBuilder();

        textView.post(new Runnable() {
            @Override
            public void run() {

                if (!isJustify.get()) {

                    final int lineCount = textView.getLineCount();
                    final int textViewWidth = textView.getWidth();

                    for (int i = 0; i < lineCount; i++) {

                        int lineStart = textView.getLayout().getLineStart(i);
                        int lineEnd = textView.getLayout().getLineEnd(i);

                        String lineString = textString.substring(lineStart, lineEnd);

                        if (i == lineCount - 1) {
                            builder.append(new SpannableString(lineString));
                            break;
                        }

                        String trimSpaceText = lineString.trim();
                        String removeSpaceText = lineString.replaceAll(" ", "");

                        float removeSpaceWidth = textPaint.measureText(removeSpaceText);
                        float spaceCount = trimSpaceText.length() - removeSpaceText.length();

                        float eachSpaceWidth = (textViewWidth - removeSpaceWidth) / spaceCount;

                        SpannableString spannableString = new SpannableString(lineString);
                        for (int j = 0; j < trimSpaceText.length(); j++) {
                            char c = trimSpaceText.charAt(j);
                            if (c == ' ') {
                                Drawable drawable = new ColorDrawable(0x00ffffff);
                                drawable.setBounds(0, 0, (int) eachSpaceWidth, 0);
                                ImageSpan span = new ImageSpan(drawable);
                                spannableString.setSpan(span, j, j + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }

                        builder.append(spannableString);
                    }

                    textView.setText(builder);
                    isJustify.set(true);
                }
            }
        });
    }

    private void SetupEntryText(ViewHolder holder, Spanned text, boolean isReadMore) {
        //SetTextViewHTMLWithLinks(holder.textTextView, text );
        holder.textTextView.setText( text );
        //holder.openArticle.setText( R.string.open_article );
        //holder.openArticle.setVisibility(isReadMore ? View.VISIBLE : View.GONE );
    }

    private static void SetContentImage(final Context context, ImageView imageView, int index, ArrayList<Uri> allImages) {
        if ( allImages.size() > index ) {
            final Uri uri = allImages.get( index );
            imageView.setVisibility(View.VISIBLE);
                Glide.with(context).load( uri )
                    .fitCenter()
                    //.override(dim)
                    .into(imageView);
            //imageView.setImageURI( uri );
            imageView.setOnClickListener(view -> EntryView.ShowImageMenu(uri.toString(), view.getContext() ));

//            new AsyncTask<Pair<Uri,ImageView>, Void, Void>() {
//                private Bitmap uriToBitmap(Uri selectedFileUri) {
//                    Bitmap image = null;
//                    try {
//                        ParcelFileDescriptor parcelFileDescriptor =
//                            context.getContentResolver().openFileDescriptor(selectedFileUri, "r");
//                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
//                        image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
//
//                        parcelFileDescriptor.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    return image;
//                }
//                Bitmap mBitmap = null;
//                ImageView mInageView = null;
//                @Override
//                protected Void doInBackground(Pair<Uri, ImageView>... pairs) {
//                    mBitmap = uriToBitmap( pairs[0].first );
//                    mInageView = pairs[0].second;
//                    return null;
//                }
//
//
//                @Override
//                protected void onPostExecute(Void result) {
//                    if ( mBitmap != null )
//                        mInageView.setImageBitmap( mBitmap );
//                }
//            }.execute(new Pair<>(uri, imageView) );
        }
    }

    public static final int KBYTE = 1024;

    static String GetImageSizeText(long imageSize) {
        final double MEGABYTE = KBYTE * KBYTE;
        return PrefUtils.CALCULATE_IMAGES_SIZE() && imageSize > KBYTE * 100 ?
            String.format( "%.1f\u00A0%s", imageSize / MEGABYTE, MainApplication.getContext().getString( R.string.megabytes ) ).replace( ",", "." ) : "";
    }

    static private String GetTextSizeText(long imageSize) {
        return imageSize > KBYTE ? String.format( "%d\u00A0%s", imageSize / KBYTE, MainApplication.getContext().getString( R.string.kilobytes ) ) : "";
    }

    private void UpdateStarImgView(ViewHolder holder) {
        int startID = Theme.GetResID( STARRED_ARTICLE_INDICATOR_RES_ID );
//        if ( holder.isFavorite )
            holder.starImgView.setImageResource(holder.isFavorite ? startID : R.drawable.ic_indicator_nonstar);
            holder.starToggleSwypeBtnView.setImageResource(holder.isFavorite ? R.drawable.ic_star_border_grey : R.drawable.ic_star_grey);
//        else
//            holder.starImgView.setImageResource(R.drawable.ic_star_border_grey);
//        holder.starImgView.setVisibility( holder.isFavorite ? View.VISIBLE : View.GONE );
    }
    private void UpdateReadView(ViewHolder holder, View parentView) {
        if ( holder.isTextShown() )
            holder.isRead = false;
        {
            boolean v = PrefUtils.IsShowReadCheckbox() && !mShowEntryText && !holder.isTextShown();
            holder.readImgView.setVisibility( v ? View.VISIBLE : View.GONE );
            if ( v )
                holder.readImgView.setImageResource(holder.isRead ? R.drawable.ic_indicator_read : R.drawable.ic_indicator_unread);
        }
        holder.readToggleSwypeBtnView.setImageResource(holder.isRead ? R.drawable.ic_check_box_outline_blank_gray : R.drawable.ic_check_box_gray);
        final int backgroundColor;
        backgroundColor = Color.parseColor( !holder.isRead ? Theme.GetColor( Theme.TEXT_COLOR_BACKGROUND, R.string.default_text_color_background ) : Theme.GetColor( Theme.TEXT_COLOR_READ_BACKGROUND, R.string.default_text_color_background )  );
        parentView.findViewById(R.id.layout_vertval).setBackgroundColor(backgroundColor );
        parentView.findViewById( R.id.entry_list_layout_root_root ).setBackgroundColor( backgroundColor );
        {
            final int color = Color.parseColor( !holder.isRead ? Theme.GetTextColor() : Theme.GetTextColorRead() );
            holder.imageSizeTextView.setTextColor( color );
            holder.authorTextView.setTextColor( color );
            holder.categoriesTextView.setTextColor(color );
            holder.labelTextView.setTextColor(color );
            holder.textPreviewTextView.setTextColor(color );
            holder.dateTextView.setTextColor( color );
            holder.textTextView.setTextColor( color );
            //holder.readMore.setTextColor( color );
            holder.titleTextView.setTextColor( color );
            holder.urlTextView.setTextColor( color );
            holder.authorTextView.setTextColor( color );
        }
    }

    private void toggleReadState(final ViewHolder holder, View parentView) {
        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isRead = !holder.isRead;

            if (holder.isRead && PrefUtils.getLong(STATE_TEXTSHOWN_ENTRY_ID, 0) == holder.entryID ) {
                PrefUtils.putLong(STATE_TEXTSHOWN_ENTRY_ID, 0);
                MainApplication.getContext().getContentResolver().notifyChange(mUri, null);//notifyDataSetChanged();
            } else
                UpdateReadView(holder, parentView);
            SetIsRead(holder.entryID, holder.isRead, true);
            if ( holder.isRead && mShowUnread ) {
                Snackbar snackbar = Snackbar.make(parentView.getRootView().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                        .setActionTextColor(ContextCompat.getColor(parentView.getContext(), R.color.light_theme_color_primary))
                        .setAction(R.string.undo, v -> SetIsRead(holder.entryID, false, false));
                snackbar.getView().setBackgroundResource(R.color.material_grey_900);
                snackbar.show();
            }


        }
    }

    private void SetIsRead(final long entryId, final boolean isRead, final boolean isSilent ) {
        final Uri entryUri = EntryUri( entryId );
        if (isRead)
            mMarkAsReadList.add(entryUri.toString());
        else
            mMarkAsReadList.remove(entryUri.toString());
        if ( isRead && isSilent )
            return;
        new Thread() {
            @Override
            public void run() {
                if (isSilent)
                    SetNotifyEnabled(false);
                try {
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    cr.update(entryUri, isRead ? FeedData.getReadContentValues() : FeedData.getUnreadContentValues(), null, null);
                    CancelStarNotification(Long.parseLong(entryUri.getLastPathSegment()));
                } finally {
                    if ( isSilent )
                        SetNotifyEnabled( true );
                }
            }
        }.start();
    }


    private void toggleFavoriteState(View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isFavorite = !holder.isFavorite;
            UpdateStarImgView(holder);
            UpdateLabelText(holder);
            SetIsFavorite( view, holder.entryID, holder.isFavorite, true );
        }
    }
    private void SetIsFavorite( View parentView, final long entryId, final boolean isFavorite, final boolean isSilent ) {
        final Uri entryUri = EntryUri( entryId );
        mMapFavourite.put( entryId, isFavorite );
        final HashSet<Long> oldLabels = LabelVoc.INSTANCE.getLabelIDs(entryId);
        new Thread() {
            @Override
            public void run() {
                if ( isSilent )
                    SetNotifyEnabled( false );
                try {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_FAVORITE, isFavorite ? 1 : 0);

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    cr.update(entryUri, values, null, null);
                    if (!isFavorite) {
                        CancelStarNotification( entryId );
                        LabelVoc.INSTANCE.removeLabels( entryId );
                    }
                } finally {
                    if ( isSilent )
                        SetNotifyEnabled( true );
                }
            }
        }.start();
        if ( !isFavorite ) {
            Snackbar snackbar = Snackbar.make( parentView.getRootView().findViewById(R.id.pageDownBtn), R.string.removed_from_favorites, Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(parentView.getContext(), R.color.light_theme_color_primary))
                .setAction(R.string.undo, v -> {
                    SetIsFavorite( parentView, entryId, true, false);
                    LabelVoc.INSTANCE.setEntry( entryId, oldLabels );
                    mMapFavourite.put( entryId, isFavorite );
                    MainApplication.getContext().getContentResolver().notifyChange(mUri, null);
                });
            snackbar.getView().setBackgroundResource(R.color.material_grey_900);
            snackbar.show();
        }
    }

    @Override
    public void changeCursor(Cursor cursor) {
        //SetIsReadMakredList();
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if ( mIgnoreClearContentVocOnCursorChange )
            mIgnoreClearContentVocOnCursorChange = false;
        else {
            mContentVoc.clear();
            mMapFavourite.clear();
        }
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        Status().mIsHideByScrollEnabled = false;
        super.notifyDataSetChanged();
        Status().mIsHideByScrollEnabled = true;
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mFeedTitlePos = cursor.getColumnIndex(FeedColumns.NAME);
            mUrlPos = cursor.getColumnIndex(EntryColumns.LINK);
            mMainImgPos = cursor.getColumnIndex(EntryColumns.IMAGE_URL);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
            mImageSizePos = cursor.getColumnIndex(EntryColumns.IMAGES_SIZE);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mIsNewPos = cursor.getColumnIndex(EntryColumns.IS_NEW);
            mMobilizedPos = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
            mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
            mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
            mFeedIdPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
            mCategoriesPos = cursor.getColumnIndex(EntryColumns.CATEGORIES);
            mTextLenPos = cursor.getColumnIndex("TEXT_LEN");
            {
                int col = cursor.getColumnIndex(FeedColumns.IS_IMAGE_AUTO_LOAD);
                if ( col != -1 && cursor.moveToFirst() )
                    mIsLoadImages = !cursor.isNull( col ) && cursor.getInt( col ) == 1;
            }
        }
    }

    public class ListViewTopPos {
        public int mPos = 0;

        ListViewTopPos(int pos) {
            mPos = pos;
        }
    }

    HashMap<Long, Boolean> mMapFavourite = new HashMap<>();

    private static class ViewHolder {
        ProgressBar textSizeProgressBar;
        View layoutControls;
        ImageView collapsedBtn;
        TextView titleTextView;
        TextView urlTextView;
        TextView textPreviewTextView;
        TextView textTextView;
        TextView categoriesTextView;
        TextView labelTextView;
        TextView dateTextView;
        TextView authorTextView;
        TextView imageSizeTextView;
        ImageView mainImgView;
        //View mainImgLayout;
        ImageView starImgView;
        ImageView mobilizedImgView;
        ImageView readImgView;
        ImageView newImgView;
        ImageView readToggleSwypeBtnView;
        ImageView starToggleSwypeBtnView;
        LinearLayout textLayout;
        boolean isMobilized;
        long entryID = -1;
        String entryLink;
        ImageView contentImgView1;
        ImageView contentImgView2;
        ImageView contentImgView3;
        TextView openArticle;
        TextView showMore;
        View collapseBtnBottom;
        TextView bottomEmptyPage;
        boolean isRead;
        boolean isFavorite;

        boolean isTextShown() {
            return entryID == PrefUtils.getLong( STATE_TEXTSHOWN_ENTRY_ID, 0 );
        }
    }

    public int GetFirstUnReadPos() {
        for (int i = 0; i < getCount(); i++) {
            Cursor cursor = (Cursor) getItem(i);
            if ( !EntryColumns.IsRead( cursor, mIsReadPos ) )
                return i;
        }
        return getCount();
    }
    public int GetTopNewPos() {
        for (int i = 0; i < getCount(); i++) {
            Cursor cursor = (Cursor) getItem(i);
            if ( EntryColumns.IsNew( cursor, mIsNewPos ) )
                return i;
        }
        return getCount() - 1;
    }
    public int GetBottomNewPos() {
        for (int i = getCount() - 1; i >= 0; i--) {
            Cursor cursor = (Cursor) getItem(i);
            if ( EntryColumns.IsNew( cursor, mIsNewPos ) )
                return i;
        }
        return 0;
    }
    public int GetPosByID( long id ) {
        if ( !isEmpty() ) {
            final int fiID = mIdPos;//((Cursor) getItem(0)).getColumnIndex(EntryColumns._ID);
            for (int i = 0; i < getCount(); i++) {
                Cursor cursor = (Cursor) getItem(i);
                if (cursor.getLong(fiID) == id)
                    return i;
            }
        }
        return -1;
    }

}

/*class MarkAsRadThread extends Thread  {
    //private final Uri mFeedUri;

    @Override
    public void run() {
        synchronized (EntriesCursorAdapter.mMarkAsReadList) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            for (Uri uri : EntriesCursorAdapter.mMarkAsReadList) {
                //Uri entryUri = ContentUris.withAppendedId(mFeedUri, id);
                cr.update(uri, FeedData.getReadContentValues(), null, null);
            }
            EntriesCursorAdapter.mMarkAsReadList.clear();
        }
    }

}*/
