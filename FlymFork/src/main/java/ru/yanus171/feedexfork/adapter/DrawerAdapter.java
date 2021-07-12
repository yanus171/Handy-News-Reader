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

package ru.yanus171.feedexfork.adapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.utils.Label;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.utils.NetworkUtils.GetImageFileUri;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;

public class DrawerAdapter extends BaseAdapter {

    private static final int POS_ID = 0;
    private static final int POS_URL = 1;
    private static final int POS_NAME = 2;
    private static final int POS_IS_GROUP = 3;
    private static final int POS_ICON_URL = 4;
    private static final int POS_LAST_UPDATE = 5;
    private static final int POS_ERROR = 6;
    private static final int POS_UNREAD = 7;
    private static final int POS_ALL = 8;
    private static final int POS_IS_SHOW_TEXT_IN_ENTRY_LIST = 9;
    private static final int POS_IS_GROUP_EXPANDED = 10;
    private static final int POS_IS_AUTO_RESRESH = 11;
    private static final int POS_OPTIONS = 12;
    private static final int POS_IMAGESIZE = 13;

    public static final int EXTERNAL_ENTRY_POS = 3;
    public static final int LABEL_GROUP_POS = 4;
    private static final String PREF_IS_LABEL_GROUP_EXPANDED = "label_group_expanded";

    public static final int FIRST_ENTRY_POS() { return LABEL_GROUP_POS + (PrefUtils.getBoolean( PREF_IS_LABEL_GROUP_EXPANDED, false ) ? LabelVoc.INSTANCE.getList().size() : 0) + 1; }

    private static final int NORMAL_TEXT_COLOR = Color.parseColor("#EEEEEE");
    private static final int GROUP_TEXT_COLOR = Color.parseColor("#BBBBBB");

    private static final String COLON = getContext().getString(R.string.colon);

    private static final int CACHE_MAX_ENTRIES = 100;
    private final Map<Long, String> mFormattedDateCache = new LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
        @Override
        public boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    private final Context mContext;
    private Cursor mFeedsCursor;
    private int mAllUnreadNumber, mFavoritesUnreadNumber, mFavoritesReadNumber, mAllNumber, mExternalUnreadNumber, mExternalReadNumber;
    private long mAllImagesSize, mAllUnreadImagesSize, mFavoritiesImagesSize, mExternalImagesSize;
    private int mTextTaskCount, mImageTaskCount;

    public DrawerAdapter(Context context, Cursor feedCursor) {
        mContext = context;
        mFeedsCursor = feedCursor;
        //clearFaviconCache();
        updateNumbers();
    }

    public void setCursor(Cursor feedCursor) {
        mFeedsCursor = feedCursor;

        updateNumbers();
        notifyDataSetChanged();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_drawer_list, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = convertView.findViewById(android.R.id.icon);
            holder.titleTxt = SetupTextView( convertView, android.R.id.text1);
            holder.stateTxt = SetupSmallTextView( convertView, android.R.id.text2);
            holder.imageSizeTxt = SetupSmallTextView( convertView, R.id.imageSize);
            holder.unreadTxt = SetupTextView( convertView, R.id.unread_count);
            holder.readTxt = SetupTextView( convertView, R.id.read_count);
            holder.tasksTxt = SetupSmallTextView( convertView, R.id.tasks);
            holder.autoRefreshIcon = convertView.findViewById(R.id.auto_refresh_icon);
            holder.separator = convertView.findViewById(R.id.separator);
            convertView.setTag(R.id.holder, holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag(R.id.holder);

        // default init
        holder.iconView.setImageDrawable(null);
        holder.titleTxt.setText("");
        holder.titleTxt.setTextColor(NORMAL_TEXT_COLOR);
        holder.titleTxt.setAllCaps(false);
        UiUtils.SetFont(holder.titleTxt, 1);
        holder.stateTxt.setVisibility(View.GONE);
        holder.unreadTxt.setText("");
        UiUtils.SetFont(holder.unreadTxt, 1);
        holder.readTxt.setText("");
        UiUtils.SetFont(holder.readTxt, 1);
        convertView.setPadding(0, 0, 0, 0);
        holder.separator.setVisibility(View.GONE);
        holder.autoRefreshIcon.setVisibility( View.GONE );
        holder.imageSizeTxt.setVisibility( View.GONE );
        holder.tasksTxt.setVisibility( View.GONE );

        ArrayList<Label> labelList = getLabelList();

        if (position == 0 || position == 1 || position == 2 || position == EXTERNAL_ENTRY_POS ) {
            switch (position) {
                case 0:
                    holder.titleTxt.setText(R.string.unread_entries);
                    holder.iconView.setImageResource(R.drawable.cup_new_unread);
                    if (mAllUnreadNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mAllUnreadNumber));
                    SetImageSizeText(holder, mAllUnreadImagesSize);
                    break;
                case 1:
                    holder.titleTxt.setText(R.string.all_entries);
                    holder.iconView.setImageResource(R.drawable.cup_new_pot);
                    if (mAllNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mAllNumber));
                    holder.unreadTxt.setText(String.valueOf(mAllNumber));
                    holder.readTxt.setText("");
                    SetImageSizeText(holder, mAllImagesSize);
                    String taskInfo = "";
                    if (mTextTaskCount != 0)
                        taskInfo += String.format(" T:%d", mTextTaskCount);
                    if (mImageTaskCount != 0)
                        taskInfo += String.format(" I:%d", mImageTaskCount);
                    if (!taskInfo.isEmpty()) {
                        holder.tasksTxt.setVisibility(View.VISIBLE);
                        holder.tasksTxt.setText(getContext().getString(R.string.tasks_to_download) + ": " + taskInfo);
                    }
                    break;
                case 2:
                    holder.titleTxt.setText(R.string.favorites);
                    holder.iconView.setImageResource(R.drawable.cup_new_star);
                    if (mFavoritesUnreadNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mFavoritesUnreadNumber));
                    if (mFavoritesReadNumber != 0)
                        holder.readTxt.setText(String.valueOf(mFavoritesReadNumber));
                    SetImageSizeText(holder, mFavoritiesImagesSize);
                    break;
                case EXTERNAL_ENTRY_POS:
                    holder.titleTxt.setText(R.string.externalLinks);
                    holder.iconView.setImageResource(R.drawable.cup_new_load_later);
                    if (mExternalUnreadNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mExternalUnreadNumber));
                    if (mExternalReadNumber != 0)
                        holder.readTxt.setText(String.valueOf(mExternalReadNumber));
                    SetImageSizeText(holder, mExternalImagesSize);
                    break;
            }
        } else if ( position == LABEL_GROUP_POS ) {
            holder.iconView.setImageResource(isGroupExpanded(position) ? R.drawable.ic_group_expanded_gray : R.drawable.ic_group_collapsed_gray);
            holder.iconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PrefUtils.putBoolean( PREF_IS_LABEL_GROUP_EXPANDED, !isGroupExpanded(LABEL_GROUP_POS) );
                    notifyDataSetChanged();
                }
            });
            holder.titleTxt.setText( R.string.labels_group_title);
            holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
            holder.titleTxt.setAllCaps(true);
            holder.separator.setVisibility(View.VISIBLE);
        } else if ( isLabelPos(position, labelList) ) {
            Label label = labelList.get(getLabelPosition(position));
            holder.titleTxt.setText( label.mName );
            holder.titleTxt.setTextColor( label.colorInt() );
            holder.iconView.setImageResource(R.drawable.cup_empty);
            holder.unreadTxt.setText("");
            holder.readTxt.setText("");
            //SetImageSizeText(holder, mExternalImagesSize);
         } else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS())) {
            holder.titleTxt.setText((mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME)));

            if (mFeedsCursor.getInt(POS_IS_GROUP) == 1) {
                holder.iconView.setImageResource(isGroupExpanded(position) ? R.drawable.ic_group_expanded_gray : R.drawable.ic_group_collapsed_gray);
                holder.iconView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ContentResolver cr = getContext().getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(FeedData.FeedColumns.IS_GROUP_EXPANDED, isGroupExpanded(position) ? null : 1 );
                        cr.update(FeedData.FeedColumns.CONTENT_URI(getItemId(position)), values, null, null);
                    }
                });
                holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
                holder.titleTxt.setAllCaps(true);
                holder.separator.setVisibility(View.VISIBLE);
            } else {
                holder.stateTxt.setVisibility(View.VISIBLE);

                if (mFeedsCursor.isNull(POS_ERROR)) {
                    long timestamp = mFeedsCursor.getLong(POS_LAST_UPDATE);

                    // Date formatting is expensive, look at the cache
                    String formattedDate = mFormattedDateCache.get(timestamp);
                    if (formattedDate == null) {

                        formattedDate = "";//mContext.getString(R.string.last_update) + COLON;

                        if (timestamp == 0) {
                            formattedDate += mContext.getString(R.string.never);
                        } else {
                            formattedDate += StringUtils.getDateTimeString(timestamp);
                        }

                        mFormattedDateCache.put(timestamp, formattedDate);
                    }

                    holder.stateTxt.setText(formattedDate);
                } else {
                    holder.stateTxt.setText(new StringBuilder(mContext.getString(R.string.error)).append(COLON).append(mFeedsCursor.getString(POS_ERROR)));
                }

                Uri iconUri = mFeedsCursor.isNull(POS_ICON_URL) ? Uri.EMPTY : GetImageFileUri(mFeedsCursor.getString(POS_ICON_URL), mFeedsCursor.getString(POS_ICON_URL) );
                final int dim = UiUtils.dpToPixel(35);
                Glide.with(parent.getContext())
                    .load(iconUri)
                    //.override( dim, dim )
                    //.centerCrop()
                    .placeholder( R.drawable.cup_new_empty )
                    .into(holder.iconView);

                holder.autoRefreshIcon.setVisibility( isAutoRefresh( position )  ? View.VISIBLE : View.GONE );
            }
            int unread = mFeedsCursor.getInt(POS_UNREAD);
            if (unread != 0) {
                holder.unreadTxt.setText(String.valueOf(unread));
            }
            int read = mFeedsCursor.getInt(POS_ALL) - mFeedsCursor.getInt(POS_UNREAD);
            if ( read > 0 )
                holder.readTxt.setText( String.valueOf(read) );

            SetImageSizeText(holder, mFeedsCursor.getInt( POS_IMAGESIZE ));
        }

        return convertView;
    }

    @NotNull
    public ArrayList<Label> getLabelList() {
        return isGroupExpanded(LABEL_GROUP_POS) ? LabelVoc.INSTANCE.getList() : new ArrayList<Label>();
    }

    public boolean isLabelPos(int position, ArrayList<Label> labelList) {
        return position > LABEL_GROUP_POS && position <= LABEL_GROUP_POS + labelList.size();
    }

    static private void SetImageSizeText(ViewHolder holder, long size) {
        if ( PrefUtils.CALCULATE_IMAGES_SIZE() && size != 0 ) {
            holder.imageSizeTxt.setVisibility( View.VISIBLE );
            holder.imageSizeTxt.setText(GetImageSizeText(size));
        } else
            holder.imageSizeTxt.setVisibility( View.GONE );
    }

    static private String GetImageSizeText(long imageSize) {
        final int MEGABYTE = 1024 * 1024;
        return PrefUtils.CALCULATE_IMAGES_SIZE() && imageSize > MEGABYTE ?
            EntriesCursorAdapter.GetImageSizeText( imageSize ).replace( ",", "." ) : "";
    }

    @Override
    public int getCount() {
        if (mFeedsCursor != null) {
            return mFeedsCursor.getCount() + FIRST_ENTRY_POS();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS())) {
            return mFeedsCursor.getLong(POS_ID);
        }

        return -1;
    }

    public int getItemPosition( long feedID ) {
        for( int i = 0; i < getCount(); i++ )
            if ( getItemId( i ) == feedID )
                return i;
        return -1;
    }

    public Bitmap getItemIcon(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()))
            return UiUtils.getFaviconBitmap( mFeedsCursor.getString( POS_ICON_URL ) );
        return null;
    }

    public String getItemName(int position) {
        if ( isLabelPos( position, getLabelList() ) )
            return getLabelList().get( getLabelPosition( position ) ).mName;
        else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()))
            return mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME);

        return null;
    }

    private int getLabelPosition(int position) {
        return position - LABEL_GROUP_POS - 1;
    }

    public boolean isItemAGroup(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()) && mFeedsCursor.getInt(POS_IS_GROUP) == 1;

    }

    public boolean isShowTextInEntryList(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()) && mFeedsCursor.getInt(POS_IS_SHOW_TEXT_IN_ENTRY_LIST) == 1;
    }

    public JSONObject getOptions(int position) {
        try {
            return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()) ? new JSONObject( mFeedsCursor.getString(POS_OPTIONS) ) : new JSONObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private boolean isGroupExpanded(int position) {
        if ( position == LABEL_GROUP_POS )
            return PrefUtils.getBoolean( PREF_IS_LABEL_GROUP_EXPANDED, false );
        else
            return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()) && mFeedsCursor.getInt(POS_IS_GROUP_EXPANDED) == 1;

    }

    private boolean isAutoRefresh(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()) && mFeedsCursor.getInt(POS_IS_AUTO_RESRESH) == 1;

    }

    private void updateNumbers() {
        ContentResolver cr = mContext.getContentResolver();
        mAllUnreadNumber = mFavoritesUnreadNumber = mFavoritesReadNumber = mAllNumber = mExternalReadNumber = mExternalUnreadNumber = 0;
        mAllImagesSize = 0;
        Timer timer = new Timer("updateNumbers()");
        // Gets the numbers of entries (should be in a thread, but it's way easier like this and it shouldn't be so slow)
        Cursor numbers = cr.query(EntryColumns.CONTENT_URI,
                                  new String[]{FeedData.ALL_UNREAD_NUMBER,
                                      PrefUtils.getBoolean(PrefUtils.SHOW_READ_ARTICLE_COUNT, false) ? FeedData.FAVORITES_READ_NUMBER : "0",
                                      PrefUtils.getBoolean(PrefUtils.SHOW_READ_ARTICLE_COUNT, false) ? FeedData.FAVORITES_UNREAD_NUMBER : FeedData.FAVORITES_NUMBER,
                                      FeedData.ALL_NUMBER,
                                      PrefUtils.getBoolean(PrefUtils.SHOW_READ_ARTICLE_COUNT, false) ? FeedData.EXTERNAL_UNREAD_NUMBER : FeedData.EXTERNAL_NUMBER,
                                      PrefUtils.getBoolean(PrefUtils.SHOW_READ_ARTICLE_COUNT, false) ? FeedData.EXTERNAL_READ_NUMBER : "0",
                                      FeedData.ALL_IMAGESSIZE_NUMBER(),
                                      FeedData.ALL_UNREAD_IMAGESSIZE_NUMBER(),
                                      FeedData.FAVORITES_IMAGESSIZE_NUMBER(),
                                      FeedData.EXTERNAL_IMAGESSIZE_NUMBER()},
                                  null, null, null);
        if (numbers != null) {
            if (numbers.moveToFirst()) {
                mAllUnreadNumber = numbers.getInt(0);
                mFavoritesReadNumber = numbers.getInt(1);
                mFavoritesUnreadNumber = numbers.getInt(2);
                mAllNumber = numbers.getInt(3);
                mExternalUnreadNumber = numbers.getInt(4);
                mExternalReadNumber = numbers.getInt(5);
                mAllImagesSize = numbers.getLong(6);
                mAllUnreadImagesSize = numbers.getLong(7);
                mFavoritiesImagesSize = numbers.getLong(8);
                mExternalImagesSize = numbers.getLong(9);
            }
            numbers.close();
        }
        timer.End();

        timer = new Timer("updateTaskNumbers()");

        mTextTaskCount = 0;
        mImageTaskCount = 0;
        Cursor tasks = cr.query(FeedData.TaskColumns.CONTENT_URI,
                                new String[]{FeedData.TaskColumns.TEXT_COUNT, FeedData.TaskColumns.IMAGE_COUNT},
                                null, null, null);
        if (tasks != null) {
            if (tasks.moveToFirst()) {
                mTextTaskCount = tasks.getInt(0);
                mImageTaskCount = tasks.getInt(1);
            }
            tasks.close();
        }
        timer.End();
    }
    private static class ViewHolder {
        ImageView iconView;
        TextView titleTxt;
        TextView stateTxt;
        TextView imageSizeTxt;
        TextView unreadTxt;
        TextView readTxt;
        TextView tasksTxt;
        ImageView autoRefreshIcon;

        View separator;
    }
}
