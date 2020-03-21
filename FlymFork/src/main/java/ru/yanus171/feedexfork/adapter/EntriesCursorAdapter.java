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
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.service.FetcherService.GetActionIntent;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.Theme.TEXT_COLOR_READ;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private final Uri mUri;
    private final Context mContext;
    private final boolean mShowFeedInfo;
    private final boolean mShowEntryText, mShowUnread;
    private boolean mBackgroundColorLight = false;

    public static final ArrayList<Uri> mMarkAsReadList = new ArrayList<>();

    private int mIdPos, mTitlePos, mFeedTitlePos, mUrlPos, mMainImgPos, mDatePos, mIsReadPos, mAuthorPos, mImageSizePos, mFavoritePos, mMobilizedPos, mFeedIdPos, mFeedNamePos, mAbstractPos, mIsNewPos, mTextLenPos;
    public static int mEntryActivityStartingStatus = 0;

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo, boolean showEntryText, boolean showUnread) {
        super(context, R.layout.item_entry_list, cursor, 0);
        //Dog.v( String.format( "new EntriesCursorAdapter( %s, showUnread = %b )", uri.toString() ,showUnread ) );
        mContext = context;
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        mShowEntryText = showEntryText;
        mShowUnread = showUnread;
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
            holder.titleTextView = view.findViewById(android.R.id.text1);
            holder.urlTextView = view.findViewById(R.id.textUrl);
            holder.textTextView = view.findViewById(R.id.textSource);
            if ( mShowEntryText )
                holder.dateTextView = view.findViewById(R.id.textDate);
            else
                holder.dateTextView = view.findViewById(android.R.id.text2);
            holder.authorTextView = view.findViewById(R.id.textAuthor);
            holder.imageSizeTextView = view.findViewById(R.id.imageSize);
            holder.mainImgView = view.findViewById(R.id.main_icon);
            holder.mainImgLayout = view.findViewById(R.id.main_icon_layout);
            holder.starImgView = view.findViewById(R.id.favorite_icon);
            holder.mobilizedImgView = view.findViewById(R.id.mobilized_icon);
            holder.readImgView = view.findViewById(R.id.read_icon);
            holder.readImgView.setVisibility( PrefUtils.IsShowReadCheckbox() ? View.VISIBLE : View.GONE ); //
            holder.textLayout = view.findViewById(R.id.textLayout);
            holder.readToggleSwypeBtnView = view.findViewById(R.id.swype_btn_toggle_read);
            holder.starToggleSwypeBtnView = view.findViewById(R.id.swype_btn_toggle_star);
            holder.newImgView = view.findViewById(R.id.new_icon);

            UiUtils.SetFontSize(holder.titleTextView);

            view.setTag(R.id.holder, holder);

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
                    final int MIN_X_TO_VIEW_ARTICLE = UiUtils.mmToPixel( 10 );
                    final ViewHolder holder = (ViewHolder) ( (ViewGroup)v.getParent() ).getTag(R.id.holder);
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
                                    EntriesListFragment.ShowDeleteDialog(view.getContext(), holder.titleTextView.getText().toString(), holder.entryID);
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
                            if ( mEntryActivityStartingStatus == 0 &&
                                 currentx > MIN_X_TO_VIEW_ARTICLE &&
                                 Math.abs( paddingX ) < minX &&
                                 Math.abs( paddingY ) < minY &&
                                 SystemClock.elapsedRealtime() - downTime < ViewConfiguration.getLongPressTimeout() ) {
                                mEntryActivityStartingStatus = Status().Start(R.string.article_opening, true);
                                v.getContext().startActivity(
                                        GetActionIntent(Intent.ACTION_VIEW,
                                                ContentUris.withAppendedId(mUri, holder.entryID)));
                            } else if ( Math.abs( paddingX ) > Math.abs( paddingY ) && paddingX >= threshold)
                                toggleReadState(holder.entryID, view);
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

                    v.setPadding(paddingX > 0 ? paddingX : 0, 0, paddingX < 0 ? -paddingX : 0, 0);

                    Dog.v(" onTouch paddingX = " + paddingX + ", paddingY= " + paddingY + ", minX= " + minX + ", minY= " + minY + ", isPress = " + isPress + ", threshold = " + threshold );
                    return true;
                }
            } );
        }

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        holder.entryID = cursor.getLong(mIdPos);
        holder.entryLink = cursor.getString(mUrlPos);

        final boolean isUnread = !EntryColumns.IsRead( cursor, mIsReadPos );

        //mBackgroundColorLight =  daysTo % 2 == 1; //mShowEntryText && cursor.getPosition() % 2 == 1;
        final int backgroundColor;
        backgroundColor = Color.parseColor( isUnread ? Theme.GetColor( Theme.TEXT_COLOR_BACKGROUND, R.string.default_text_color_background ) : Theme.GetColor( Theme.TEXT_COLOR_READ_BACKGROUND, R.string.default_text_color_background )  );
        view.findViewById(R.id.layout_vertval).setBackgroundColor(backgroundColor );
        view.findViewById( R.id.layout_root ).setBackgroundColor( backgroundColor );

        holder.readToggleSwypeBtnView.setVisibility( View.GONE );
        holder.starToggleSwypeBtnView.setVisibility( View.GONE );

        holder.dateTextView.setVisibility(View.VISIBLE);

        final String feedTitle = cursor.getString(mFeedTitlePos);
        String titleText = cursor.getString(mTitlePos).replace( feedTitle == null ? "" : feedTitle, "" );
        holder.titleTextView.setText(titleText);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(cursor.getLong(mDatePos));
        Calendar currentDate = Calendar.getInstance();
        boolean isToday = currentDate.get( Calendar.DAY_OF_YEAR ) == date.get( Calendar.DAY_OF_YEAR );
        holder.titleTextView.setTypeface( isToday ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT );

        holder.urlTextView.setText(cursor.getString(mUrlPos));

        String feedName = cursor.getString(mFeedNamePos);

        if ( /*!mShowEntryText && */PrefUtils.getBoolean( "setting_show_article_icon", true ) ) {
            holder.mainImgLayout.setVisibility( View.VISIBLE );
            String mainImgUrl = cursor.getString(mMainImgPos);
            mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(holder.entryLink, mainImgUrl);

            ColorGenerator generator = ColorGenerator.DEFAULT;
            int color = generator.getColor(feedId); // The color is specific to the feedId (which shouldn't change)
            String lettersForName = feedName != null ? (feedName.length() < 2 ? feedName.toUpperCase() : feedName.substring(0, 2).toUpperCase()) : "";
            TextDrawable letterDrawable = TextDrawable.builder().buildRect(lettersForName, color);
            if (mainImgUrl != null) {
                final int dim = UiUtils.dpToPixel(50);
                Glide.with(context).load(mainImgUrl)
                    .override(dim, dim)
                    .centerCrop().placeholder(letterDrawable).error(letterDrawable).into(holder.mainImgView);
            } else {
                Glide.with(context).clear(holder.mainImgView);
                holder.mainImgView.setImageDrawable(letterDrawable);
            }
        } else
            holder.mainImgLayout.setVisibility( View.GONE );

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
        holder.titleTextView.setEnabled(isUnread);
        holder.dateTextView.setEnabled(isUnread);
        holder.authorTextView.setEnabled(isUnread);
        holder.urlTextView.setEnabled(isUnread);

        final boolean showUrl = PrefUtils.getBoolean( "settings_show_article_url", false ) || feedId.equals( FetcherService.GetExtrenalLinkFeedID() );
        holder.urlTextView.setVisibility( showUrl ? View.VISIBLE : View.GONE );

        holder.isRead = !isUnread;

        /*View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (feedId >= 0) { // should not happen, but I had a crash with this on PlayStore...
                    mContext.startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mUri, entryID)));
                }
            }
        };

        holder.textLayout.setOnClickListener( listener );
        holder.textTextView.setOnClickListener( listener );*/

        UpdateStarImgView(holder);
        /*holder.starImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavoriteState(holder.entryID, view);
            }
        });*/

        holder.mobilizedImgView.setVisibility(holder.isMobilized && PrefUtils.getBoolean( "show_full_text_indicator", false ) ? View.VISIBLE : View.GONE);

        UpdateReadImgView(holder);
        holder.readImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReadState(holder.entryID, view);
            }
        });

        if ( mShowEntryText ) {
            holder.textTextView.setVisibility(View.VISIBLE);
            holder.textTextView.setText(Html.fromHtml( cursor.getString(mAbstractPos) == null ? "" : cursor.getString(mAbstractPos) ));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                holder.textTextView.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD );
            holder.textTextView.setEnabled(!holder.isRead);
        } else
            holder.textTextView.setVisibility(View.GONE);

        /*Display display = ((WindowManager) context.getSystemService( Context.WINDOW_SERVICE ) ).getDefaultDisplay();
        int or = display.getOrientation();
        if (or == Configuration.ORIENTATION_LANDSCAPE) {
            holder.titleTextView.setSingleLine();
            holder.mainImgView.setMaxHeight(  );
        }*/
        holder.newImgView.setVisibility( PrefUtils.getBoolean( "show_new_icon", true ) && EntryColumns.IsNew( cursor, mIsNewPos ) ? View.VISIBLE : View.GONE );

        {
            final int color = Color.parseColor( isUnread ? Theme.GetTextColor() : Theme.GetColor( TEXT_COLOR_READ, R.string.default_read_color ) );
            holder.imageSizeTextView.setTextColor( color );
            holder.authorTextView.setTextColor( color );
            holder.dateTextView.setTextColor( color );
            holder.textTextView.setTextColor( color );
            holder.titleTextView.setTextColor( color );
            holder.urlTextView.setTextColor( color );
            holder.authorTextView.setTextColor( color );
        }
    }

    static private String GetImageSizeText(int imageSize) {
        final double MEGABYTE = 1024.0 * 1024.0;
        return PrefUtils.CALCULATE_IMAGES_SIZE() && imageSize > 1024 * 100 ?
            String.format( "%.1f M", imageSize / MEGABYTE ).replace( ",", "." ) : "";
    }

    static private String GetTextSizeText(int imageSize) {
        final int KBYTE = 1024;
        return imageSize > KBYTE ? String.format( "%d K", imageSize / KBYTE ) : "";
    }

    private void UpdateStarImgView(ViewHolder holder) {
        int startID = Theme.IsLight() ? R.drawable.star_gray_solid : R.drawable.star_yellow;
        if ( holder.isFavorite )
            holder.starImgView.setImageResource(startID );
        holder.starImgView.setVisibility( holder.isFavorite ? View.VISIBLE : View.GONE );
    }
    private void UpdateReadImgView(ViewHolder holder) {
        holder.readImgView.setVisibility( PrefUtils.IsShowReadCheckbox() && !mShowEntryText ? View.VISIBLE : View.GONE );
        holder.readImgView.setImageResource(holder.isRead ? R.drawable.rounded_checbox_gray : R.drawable.rounded_empty_gray);
    }

    private void toggleReadState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isRead = !holder.isRead;

            if (holder.isRead) {
                holder.titleTextView.setEnabled(false);
                holder.dateTextView.setEnabled(false);
            } else {
                holder.titleTextView.setEnabled(true);
                holder.dateTextView.setEnabled(true);
            }
            UpdateReadImgView( holder );
            UpdateStarImgView( holder );

            SetIsRead(EntryUri(id), holder.isRead, 0);
            if ( holder.isRead && mShowUnread ) {
                Snackbar snackbar = Snackbar.make(view.getRootView().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                        .setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.light_theme_color_primary))
                        .setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SetIsRead(EntryUri(id), false, 0);
                            }
                        });
                snackbar.getView().setBackgroundResource(R.color.material_grey_900);
                snackbar.show();
            }
        }
    }

//    public String GetTitle( AbsListView lv, int position ) {
//        return ( ( Cursor )lv.getItemAtPosition( position ) ).getString( mTitlePos );
//    }

    private static void SetIsRead(final Uri entryUri, final boolean isRead, final int sleepMsec) {
        new Thread() {
            @Override
            public void run() {

                try {
                    if (sleepMsec > 0)
                        sleep(sleepMsec);
                } catch( InterruptedException e ) {

                }
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(entryUri, isRead ? FeedData.getReadContentValues() : FeedData.getUnreadContentValues(), null, null);
                CancelStarNotification( Long.parseLong(entryUri.getLastPathSegment()) );
            }
        }.start();
    }

    /*public static void SetIsReadMakredList() {
        if ( !mMarkAsReadList.isEmpty() ) {
            Dog.d("SetIsReadMakredList()");
            new MarkAsRadThread().start();
        }
    }*/


    private void toggleFavoriteState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isFavorite = !holder.isFavorite;

            UpdateStarImgView(holder);

            new Thread() {
                @Override
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_FAVORITE, holder.isFavorite ? 1 : 0);

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    Uri entryUri = ContentUris.withAppendedId(mUri, id);
                    cr.update(entryUri, values, null, null);
                    if ( !holder.isFavorite )
                        CancelStarNotification( id );
                }
            }.start();
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
        //SetIsReadMakredList();
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
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
            mTextLenPos = cursor.getColumnIndex("TEXT_LEN");
        }

    }

    private static class ViewHolder {
        TextView titleTextView;
        TextView urlTextView;
        TextView textTextView;
        TextView dateTextView;
        TextView authorTextView;
        TextView imageSizeTextView;
        ImageView mainImgView;
        View mainImgLayout;
        ImageView starImgView;
        ImageView mobilizedImgView;
        ImageView readImgView;
        View newImgView;
        View readToggleSwypeBtnView;
        View starToggleSwypeBtnView;
        LinearLayout textLayout;
        boolean isRead;
        boolean isFavorite;
        boolean isMobilized;
        long entryID = -1;
        String entryLink;
    }

    public int GetFirstUnReadPos() {
        for (int i = 0; i < getCount(); i++) {
            Cursor cursor = (Cursor) getItem(i);
            if ( !EntryColumns.IsRead( cursor, mIsReadPos ) )
                return i;
        }
        return -1;
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
