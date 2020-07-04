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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;

import static android.view.View.TEXT_DIRECTION_ANY_RTL;
import static android.view.View.TEXT_DIRECTION_RTL;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.CATEGORY_LIST_SEP;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.service.FetcherService.GetActionIntent;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.RemoveHeaders;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.RemoveTables;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_CATEGORY;
import static ru.yanus171.feedexfork.utils.PrefUtils.SHOW_ARTICLE_URL;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR_BACKGROUND;
import static ru.yanus171.feedexfork.utils.Theme.NEW_ARTICLE_INDICATOR_RES_ID;
import static ru.yanus171.feedexfork.utils.Theme.STARRED_ARTICLE_INDICATOR_RES_ID;
import static ru.yanus171.feedexfork.utils.Theme.TEXT_COLOR_READ;
import static ru.yanus171.feedexfork.utils.UiUtils.SetFont;
import static ru.yanus171.feedexfork.utils.UiUtils.SetSmallFont;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;
import static ru.yanus171.feedexfork.view.EntryView.getAlign;
import static ru.yanus171.feedexfork.view.EntryView.isTextRTL;

public class EntriesCursorAdapter extends ResourceCursorAdapter {


    public static final String STATE_TEXTSHOWN_ENTRY_ID = "STATE_TEXTSHOWN_ENTRY_ID";
    private HashMap<Long, EntryContent> mContentVoc = new HashMap<>();
    private final Uri mUri;
    private final Context mContext;
    private final boolean mShowFeedInfo;
    private final boolean mShowEntryText;
    private final boolean mShowUnread;
    private boolean mIsLoadImages;
    private boolean mBackgroundColorLight = false;
    private final static int MAX_TEXT_LEN = 2500;
    FeedFilters mFilters = null;
    public static final ArrayList<Uri> mMarkAsReadList = new ArrayList<>();

    private int mIdPos, mTitlePos, mFeedTitlePos, mUrlPos, mMainImgPos, mDatePos, mIsReadPos,
        mAuthorPos, mImageSizePos, mFavoritePos, mMobilizedPos, mFeedIdPos, mFeedNamePos,
        mAbstractPos, mIsNewPos, mTextLenPos, mCategoriesPos;
    public static int mEntryActivityStartingStatus = 0;
    public boolean mIgnoreClearContentVocOnCursorChange = false;

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo, boolean showEntryText, boolean showUnread) {
        super(context, R.layout.item_entry_list, cursor, 0);
        //Dog.v( String.format( "new EntriesCursorAdapter( %s, showUnread = %b )", uri.toString() ,showUnread ) );
        mContext = context;
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        mShowEntryText = showEntryText;
        mShowUnread = showUnread;
        mIsLoadImages = true;
        //SetIsReadMakredList();

        mMarkAsReadList.clear();

        reinit(cursor);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    public Uri EntryUri( long id ) {
        return EntryColumns.CONTENT_URI( id ); //ContentUris.withAppendedId(mUri, id);
    }


    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {


        final Vibrator vibrator = (Vibrator) view.getContext().getSystemService( Context.VIBRATOR_SERVICE );

        view.findViewById(R.id.textDate).setVisibility(View.GONE);
        view.findViewById(android.R.id.text2).setVisibility(View.GONE);

        final String feedId = cursor.getString(mFeedIdPos);
        //final long entryID = cursor.getLong(mIdPos);

        if (view.getTag(R.id.holder) == null) {
            final ViewHolder holder = new ViewHolder();
            holder.titleTextView = SetupTextView( view, android.R.id.text1);
            holder.urlTextView = SetupSmallTextView( view, R.id.textUrl);
            holder.textTextView = SetupTextView( view, R.id.textSource);
            if ( mShowEntryText ) {
                holder.dateTextView = SetupSmallTextView( view, R.id.textDate);
            }
            else {
                holder.dateTextView = SetupSmallTextView( view, android.R.id.text2);
            }
            holder.authorTextView = SetupSmallTextView( view, R.id.textAuthor);
            holder.imageSizeTextView = SetupSmallTextView( view, R.id.imageSize);
            holder.mainImgView = view.findViewById(R.id.main_icon);
            //holder.mainImgLayout = view.findViewById(R.id.main_icon_layout);
            holder.starImgView = view.findViewById(R.id.favorite_icon);
            holder.mobilizedImgView = view.findViewById(R.id.mobilized_icon);
            holder.readImgView = view.findViewById(R.id.read_icon);
            holder.readImgView.setVisibility( PrefUtils.IsShowReadCheckbox() ? View.VISIBLE : View.GONE ); //
            holder.textLayout = view.findViewById(R.id.textLayout);
            holder.readToggleSwypeBtnView = view.findViewById(R.id.swype_btn_toggle_read);
            holder.starToggleSwypeBtnView = view.findViewById(R.id.swype_btn_toggle_star);
            holder.newImgView = view.findViewById(R.id.new_icon);
            holder.contentImgView1 = view.findViewById(R.id.image1);
            holder.contentImgView2 = view.findViewById(R.id.image2);
            holder.contentImgView3 = view.findViewById(R.id.image3);
            holder.readMore = SetupTextView( view, R.id.textSourceReadMore);
            holder.collapsedBtn = view.findViewById(R.id.collapsed_btn);
            holder.categoriesTextView = SetupSmallTextView( view, R.id.textCategories);

            view.setTag(R.id.holder, holder);

            holder.readMore.setTextColor(Theme.GetColorInt(LINK_COLOR, R.string.default_link_color));
            holder.readMore.setBackgroundColor( Theme.GetColorInt(LINK_COLOR_BACKGROUND, R.string.default_text_color_background));
            holder.readMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OpenArticle(view.getContext(), holder.entryID, holder.isTextShown());
                }
            });

            holder.collapsedBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final long shownId = PrefUtils.getLong(STATE_TEXTSHOWN_ENTRY_ID, 0);
                    if (shownId == holder.entryID) {
                        PrefUtils.putLong( STATE_TEXTSHOWN_ENTRY_ID, 0 );
                    } else {
                        SetIsRead( EntryUri(shownId), true, true );
                        PrefUtils.putLong( STATE_TEXTSHOWN_ENTRY_ID, holder.entryID );
                    }
                    MainApplication.getContext().getContentResolver().notifyChange( mUri, null );//notifyDataSetChanged();
                    EntryView.mImageDownloadObservable.notifyObservers(new ListViewTopPos(GetPosByID( holder.entryID ) ) );
                }
            });
            view.findViewById( R.id.layout_ontouch ).setOnTouchListener( new View.OnTouchListener() {
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
                    final int MIN_X_TO_VIEW_ARTICLE = UiUtils.mmToPixel( 5 );
                    final ViewHolder holder = (ViewHolder) ( (ViewGroup)v.getParent().getParent() ).getTag(R.id.holder);
                    if ( event.getAction() == MotionEvent.ACTION_DOWN) {
                        Dog.v( "onTouch ACTION_DOWN" );
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
                        UiUtils.RunOnGuiThread( new Runnable() {
                            @Override
                            public void run() {
                                if (isPress) {
                                    //wasMove = true;
                                    EntriesListFragment.ShowDeleteDialog(view.getContext(), holder.titleTextView.getText().toString(), holder.entryID, holder.entryLink);
                                }
                            }
                        }, ViewConfiguration.getLongPressTimeout());
                    }
                    if ( event.getAction() == MotionEvent.ACTION_MOVE) {

                        currentx = (int) event.getX();
                        currenty = (int) event.getY();
                        paddingX = currentx - initialx;
                        paddingY = currenty - initialy;
                        Dog.v( "onTouch ACTION_MOVE " + paddingX + ", " + paddingY );

                        //allow vertical scrolling
                        if ( ( initialx < minX * 2 || Math.abs( paddingY ) > Math.abs( paddingX ) ) &&
                             Math.abs( initialy - event.getY() ) > minY &&
                             view.getParent() != null )
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        if ( Math.abs( initialy - event.getY() ) > minY  )
                            isPress = false;
                        holder.readToggleSwypeBtnView.setVisibility( View.VISIBLE );
                        holder.starToggleSwypeBtnView.setVisibility( View.VISIBLE );
                    }


                    int overlap = holder.readToggleSwypeBtnView.getWidth() / 2;
                    int threshold = holder.readToggleSwypeBtnView.getWidth();
                    if ( threshold < minX )
                        threshold = minX + 5;
                    int max = threshold + overlap;

                    if ( event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ) {
                        isPress = false;
                        if ( event.getAction() == MotionEvent.ACTION_UP ) {
                            Dog.v("onTouch ACTION_UP" );
                            if (!mShowEntryText &&
                                mEntryActivityStartingStatus == 0 &&
                                currentx > MIN_X_TO_VIEW_ARTICLE &&
                                Math.abs(paddingX) < minX &&
                                Math.abs(paddingY) < minY &&
                                ( IsUnderView(event, holder.titleTextView, v) || IsUnderView(event, holder.dateTextView, v) || IsUnderView(event, holder.authorTextView, v) ) &&
                                SystemClock.elapsedRealtime() - downTime < ViewConfiguration.getLongPressTimeout()) {
                                mEntryActivityStartingStatus = Status().Start(R.string.article_opening, true);
                                OpenArticle(v.getContext(), holder.entryID, holder.isTextShown());
                            } else if ( Math.abs( paddingX ) > Math.abs( paddingY ) && paddingX >= threshold)
                                toggleReadState(holder, view);
                            else if ( Math.abs( paddingX ) > Math.abs( paddingY ) && paddingX <= -threshold)
                                toggleFavoriteState( holder.entryID, view );
                        } else {
                            Dog.v("onTouch ACTION_CANCEL");
                        }
                        paddingX = 0;
                        paddingY = 0;
                        initialx = 0;
                        initialx = 0;
                        currentx = 0;
                        wasVibrateRead = false;
                        wasVibrateStar = false;

                        if ( view.getParent() != null )
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        holder.readToggleSwypeBtnView.setVisibility( View.GONE );
                        holder.starToggleSwypeBtnView.setVisibility( View.GONE );

                    }

                    if ( paddingX > max )
                        paddingX = max;
                    if ( paddingX < -max )
                        paddingX = -max;

                    // block left drawable area
                    if ( initialx < minX * 2 ) {
                        isPress = false;
                        paddingX = 0;
                    }

                    if ( Math.abs( paddingX ) < minX )
                        paddingX = 0;

                    if ( Math.abs( paddingY ) < minY )
                        paddingY = 0;

                    // no long tap when large move
                    if( Math.abs( paddingX ) > minX || Math.abs( paddingY ) > minY )
                        isPress = false;

                    final boolean prefVibrate = PrefUtils.getBoolean(VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE, true);
                    if( prefVibrate && Math.abs( paddingX ) > Math.abs( paddingY ) && paddingX >= threshold ) {
                        if ( !wasVibrateRead ) {
                            vibrator.vibrate( VIBRATE_DURATION );
                            wasVibrateRead = true;
                        }
                        //holder.readToggleSwypeBtnView.setVisibility(View.VISIBLE);
                    } else
                        wasVibrateRead = false;

                    if( prefVibrate && Math.abs( paddingX ) > Math.abs( paddingY ) && paddingX <= -threshold ) {
                        if ( !wasVibrateStar ) {
                            vibrator.vibrate( VIBRATE_DURATION );
                            wasVibrateStar = true;
                        }
                        //holder.starToggleSwypeBtnView.setVisibility( View.VISIBLE );
                    } else
                        wasVibrateStar = false;

                    v.setPadding(Math.max(paddingX, 0), 0, paddingX < 0 ? -paddingX : 0, 0);

                    Dog.v(" onTouch paddingX = " + paddingX + ", paddingY= " + paddingY + ", minX= " + minX + ", minY= " + minY + ", isPress = " + isPress + ", threshold = " + threshold );
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
            } );
        }

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        holder.entryID = cursor.getLong(mIdPos);
        holder.entryLink = cursor.getString(mUrlPos);

        holder.isRead = EntryColumns.IsRead( cursor, mIsReadPos ) || mMarkAsReadList.contains( EntryUri( holder.entryID ) );

        //mBackgroundColorLight =  daysTo % 2 == 1; //mShowEntryText && cursor.getPosition() % 2 == 1;

        holder.readToggleSwypeBtnView.setVisibility( View.GONE );
        holder.starToggleSwypeBtnView.setVisibility( View.GONE );

        holder.dateTextView.setVisibility(View.VISIBLE);

        final String feedTitle = cursor.getString(mFeedTitlePos);
        String titleText = GetTitle(cursor);
        holder.titleTextView.setVisibility( titleText.isEmpty() ? View.GONE : View.VISIBLE );
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(cursor.getLong(mDatePos));
        Calendar currentDate = Calendar.getInstance();
        boolean isToday = currentDate.get( Calendar.DAY_OF_YEAR ) == date.get( Calendar.DAY_OF_YEAR );
        if ( PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ) || isToday )
            holder.titleTextView.setText( Html.fromHtml( "<b>" + titleText + "</b>" ) );
        else
            holder.titleTextView.setText(titleText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            holder.titleTextView.setTextDirection( isTextRTL( titleText ) ? TEXT_DIRECTION_RTL : TEXT_DIRECTION_ANY_RTL );

        holder.urlTextView.setText(cursor.getString(mUrlPos));

        String feedName = cursor.getString(mFeedNamePos);

        final boolean isTextShown = mShowEntryText || holder.isTextShown();
        holder.collapsedBtn.setVisibility( mShowEntryText ? View.GONE : View.VISIBLE );
        holder.collapsedBtn.setImageResource( holder.isTextShown() ? R.drawable.ic_keyboard_arrow_down_gray : R.drawable.ic_keyboard_arrow_right_gray );
        SetFont(holder.titleTextView, isTextShown ? 1.4F : 1 );
        holder.mainImgView.setVisibility( mIsLoadImages ? View.VISIBLE : View.GONE  );
        if ( !isTextShown && PrefUtils.getBoolean( "setting_show_article_icon", true ) ) {
            String mainImgUrl = cursor.getString(mMainImgPos);
            mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(holder.entryLink, mainImgUrl);

            ColorGenerator generator = ColorGenerator.DEFAULT;
            int color = generator.getColor(feedId); // The color is specific to the feedId (which shouldn't change)
            if ( mainImgUrl != null ) {
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

        holder.isFavorite = cursor.getInt(mFavoritePos) == 1;

        holder.isMobilized = FileUtils.INSTANCE.isMobilized( cursor.getString(mUrlPos), cursor, mMobilizedPos, mIdPos );

        String sizeText = " " + GetTextSizeText( cursor.getInt( mTextLenPos) );
        if (mShowFeedInfo && mFeedNamePos > -1) {
            if (feedName != null) {
                holder.dateTextView.setText(Html.fromHtml("<font color='#247ab0'>" + feedName + "</font>" + Constants.COMMA_SPACE + StringUtils.getDateTimeString(cursor.getLong(mDatePos))) + sizeText);
            } else {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)) + sizeText);
            }
        } else {
            holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)) + sizeText);
        }

        final int imageSize = cursor.getInt( mImageSizePos );
        if ( PrefUtils.CALCULATE_IMAGES_SIZE() && imageSize  != 0 ) {
            holder.imageSizeTextView.setVisibility( View.VISIBLE );
            holder.imageSizeTextView.setText(GetImageSizeText(imageSize));
        } else
            holder.imageSizeTextView.setVisibility( View.GONE );

        holder.authorTextView.setText( cursor.getString( mAuthorPos ) );
        UpdateReadView( holder, view );

        final boolean showUrl = PrefUtils.getBoolean( SHOW_ARTICLE_URL, false ) || feedId.equals( FetcherService.GetExtrenalLinkFeedID() );
        holder.urlTextView.setVisibility( showUrl ? View.VISIBLE : View.GONE );


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
                toggleFavoriteState(holder.entryID, view);
            }
        });

        holder.categoriesTextView.setVisibility(View.GONE);
        if ( PrefUtils.getBoolean( SHOW_ARTICLE_CATEGORY, true ) ) {
            final String categories = cursor.isNull(mCategoriesPos) ? "" : cursor.getString(mCategoriesPos);
            if (!categories.isEmpty()) {
                holder.categoriesTextView.setVisibility(View.VISIBLE);
                holder.categoriesTextView.setText(CategoriesToOutput(categories));
            }
        }

        holder.contentImgView1.setVisibility( View.GONE );
        holder.contentImgView2.setVisibility( View.GONE );
        holder.contentImgView3.setVisibility( View.GONE );
        holder.readMore.setVisibility( View.GONE );
        if ( isTextShown ) {
            holder.textTextView.setVisibility(View.VISIBLE);
            final String html = cursor.getString(mAbstractPos) == null ? "" : GetHtmlAligned(cursor.getString(mAbstractPos));
            holder.textTextView.setLinkTextColor( Theme.GetColorInt(LINK_COLOR, R.string.default_link_color) );
            //holder.textTextView.setTextIsSelectable( true );
            SetupEntryText(holder, getBoldText(html), IsReadMore(html ) );
            //holder.textTextView.setMovementMethod(LinkMovementMethod.getInstance());
            //final boolean isMobilized = FileUtils.INSTANCE.isMobilized( holder.entryLink, cursor );
            //if ( html.contains( "<img" ) /*|| isMobilized*/ ) {
                EntryContent content = mContentVoc.get(holder.entryID);
                if (content != null && content.GetIsLoaded() ) {
                    SetContentImage(context, holder.contentImgView1, 0, content.mImageUrlList);
                    SetContentImage(context, holder.contentImgView2, 1, content.mImageUrlList);
                    SetContentImage(context, holder.contentImgView3, 2, content.mImageUrlList);
                    SetupEntryText(holder, content.mText, content.mIsReadMore);
                } else if ( content == null ) {
                    content = new EntryContent();
                    content.mID = holder.entryID;
                    content.mHTML = html;
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
        holder.newImgView.setVisibility( PrefUtils.getBoolean( "show_new_icon", true ) && EntryColumns.IsNew( cursor, mIsNewPos ) ? View.VISIBLE : View.GONE );
        holder.newImgView.setImageResource(Theme.GetResID( NEW_ARTICLE_INDICATOR_RES_ID ) );


    }

    private Spanned getBoldText(String html) {
        return Html.fromHtml(PrefUtils.getBoolean(PrefUtils.ENTRY_FONT_BOLD, false ) ? "<b>" + html + "</b>" : html );
    }

    public static String CategoriesToOutput(String categories) {
        return TextUtils.join(", ", TextUtils.split(categories, CATEGORY_LIST_SEP) );
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
        ArrayList<Uri> mImageUrlList = new ArrayList<>();
        Spanned mText;
        String mHTML;
        boolean mIsLoaded = false;
        boolean mIsMobilized = false;
        boolean mIsReadMore = false;
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
            temp = temp.replaceAll( "<br>", "" );
            temp = temp.replaceAll( "\n", " " );
            temp = HtmlUtils.replaceImageURLs( temp,
                                               "",
                                               mID,
                                               mLink,
                                               true,
                                               imagesToDl,
                                               mImageUrlList,
                                               3 );
            mText = getBoldText( GetHtmlAligned( temp ));
            mIsReadMore = IsReadMore(temp);
            synchronized ( this ) {
                mIsLoaded = true;
            }
            EntryView.NotifyToUpdate( mID, mLink );
        }
    }

    private static boolean IsReadMore(String temp) {
        return temp.length() > MAX_TEXT_LEN || temp.contains( "<img" );
    }

    private void OpenArticle(Context context, long entryID, boolean isExpanded) {
        context.startActivity( GetActionIntent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mUri, entryID)));
        if( isExpanded ) {
            EntryView.mImageDownloadObservable.notifyObservers(new ListViewTopPos(GetPosByID( entryID ) ) );
            PrefUtils.putLong( STATE_TEXTSHOWN_ENTRY_ID, 0 );
        }
    }

    private void SetupEntryText(ViewHolder holder, Spanned text, boolean isReadMore) {
        //final String s = text.toString();
//        holder.textTextView.setText( text.length() > MAX_TEXT_LEN ? text.subSequence( 0, MAX_TEXT_LEN ) + " ..." : text );
        holder.textTextView.setText( text );
        //holder.textTextView.setText( s.length() > MAX_TEXT_LEN ? s.substring( 0, MAX_TEXT_LEN ) + " ..." : s );
        //Linkify.addLinks(holder.textTextView, Linkify.ALL);
        //holder.readMore.setVisibility( isReadMore ? View.VISIBLE : View.GONE );
        //holder.readMore.setText( isReadMore ? R.string.read_more : R.string.open_article );
        holder.readMore.setText( R.string.read_more );
        holder.readMore.setVisibility( isReadMore ? View.VISIBLE : View.GONE );
    }

    private static void SetContentImage(Context context, ImageView imageView, int index, ArrayList<Uri> allImages) {
        if ( allImages.size() > index ) {
            final Uri uri = allImages.get( index );
            imageView.setVisibility(View.VISIBLE);
                Glide.with(context).load( uri )
                    .fitCenter()
                    //.override(dim)
                    .into(imageView);
            //imageView.setImageURI( uri );
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        EntryView.OpenImage( uri.toString(), view.getContext() );
                    } catch (IOException e) {
                        UiUtils.toast( view.getContext(), view.getContext().getString( R.string.cant_open_image ) + ": " + e.getLocalizedMessage() );
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    static private String GetImageSizeText(int imageSize) {
        final double MEGABYTE = 1024.0 * 1024.0;
        return PrefUtils.CALCULATE_IMAGES_SIZE() && imageSize > 1024 * 100 ?
            String.format( "%.1f\u00A0M", imageSize / MEGABYTE ).replace( ",", "." ) : "";
    }

    static private String GetTextSizeText(int imageSize) {
        final int KBYTE = 1024;
        return imageSize > KBYTE ? String.format( "%d\u00A0K", imageSize / KBYTE ) : "";
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
        holder.titleTextView.setEnabled(!holder.isRead);
        holder.dateTextView.setEnabled(!holder.isRead);
        holder.textTextView.setEnabled(!holder.isRead);
        holder.authorTextView.setEnabled(!holder.isRead);
        holder.categoriesTextView.setEnabled(!holder.isRead);
        holder.urlTextView.setEnabled(!holder.isRead);
//        holder.readImgView.setVisibility( PrefUtils.IsShowReadCheckbox() && !mShowEntryText && !holder.isTextShown() ? View.VISIBLE : View.GONE );
        holder.readImgView.setVisibility( PrefUtils.IsShowReadCheckbox() && !mShowEntryText && !holder.isTextShown() ? View.VISIBLE : View.GONE );
        holder.readImgView.setImageResource(holder.isRead ? R.drawable.ic_indicator_read : R.drawable.ic_indicator_unread);
        holder.readToggleSwypeBtnView.setImageResource(holder.isRead ? R.drawable.ic_check_box_outline_blank_gray : R.drawable.ic_check_box_gray);
        final int backgroundColor;
        backgroundColor = Color.parseColor( !holder.isRead ? Theme.GetColor( Theme.TEXT_COLOR_BACKGROUND, R.string.default_text_color_background ) : Theme.GetColor( Theme.TEXT_COLOR_READ_BACKGROUND, R.string.default_text_color_background )  );
        parentView.findViewById(R.id.layout_vertval).setBackgroundColor(backgroundColor );
        parentView.findViewById( R.id.entry_list_layout_root_root ).setBackgroundColor( backgroundColor );
        {
            final int color = Color.parseColor( !holder.isRead ? Theme.GetTextColor() : Theme.GetColor( TEXT_COLOR_READ, R.string.default_read_color ) );
            holder.imageSizeTextView.setTextColor( color );
            holder.authorTextView.setTextColor( color );
            holder.categoriesTextView.setTextColor(color );
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

            UpdateReadView(holder, parentView);
            SetIsRead(EntryUri(holder.entryID), holder.isRead, true);
            if ( holder.isRead && mShowUnread ) {
                Snackbar snackbar = Snackbar.make(parentView.getRootView().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                        .setActionTextColor(ContextCompat.getColor(parentView.getContext(), R.color.light_theme_color_primary))
                        .setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SetIsRead(EntryUri(holder.entryID), false, false);
                            }
                        });
                snackbar.getView().setBackgroundResource(R.color.material_grey_900);
                snackbar.show();
            }
        }
    }

    private static void SetIsRead(final Uri entryUri, final boolean isRead, final boolean isSilent ) {
        new Thread() {
            @Override
            public void run() {
                if ( isSilent )
                    FeedDataContentProvider.mNotifyEnabled = false;
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(entryUri, isRead ? FeedData.getReadContentValues() : FeedData.getUnreadContentValues(), null, null);
                CancelStarNotification( Long.parseLong(entryUri.getLastPathSegment()) );
                if ( isSilent )
                    FeedDataContentProvider.mNotifyEnabled = true;

            }
        }.start();
    }


    private void toggleFavoriteState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isFavorite = !holder.isFavorite;
            UpdateStarImgView(holder);
            SetIsFavorite(EntryUri(holder.entryID), holder.isFavorite, true);
        }
    }
    private static void SetIsFavorite(final Uri entryUri, final boolean isFavorite, final boolean isSilent ) {
        new Thread() {
            @Override
            public void run() {
                if ( isSilent )
                    FeedDataContentProvider.mNotifyEnabled = false;
                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, isFavorite ? 1 : 0);

                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(entryUri, values, null, null);
                if ( !isFavorite )
                    CancelStarNotification( Long.parseLong(entryUri.getLastPathSegment()) );
                if ( isSilent )
                    FeedDataContentProvider.mNotifyEnabled = true;

            }
        }.start();
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
        else
            mContentVoc.clear();
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
    private static class ViewHolder {
        ImageView collapsedBtn;
        TextView titleTextView;
        TextView urlTextView;
        TextView textTextView;
        TextView categoriesTextView;
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
        boolean isRead;
        boolean isFavorite;
        boolean isMobilized;
        long entryID = -1;
        String entryLink;
        ImageView contentImgView1;
        ImageView contentImgView2;
        ImageView contentImgView3;
        TextView readMore;

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
        return getCount();
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
            final int fiID = ((Cursor) getItem(0)).getColumnIndex(EntryColumns._ID);
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
