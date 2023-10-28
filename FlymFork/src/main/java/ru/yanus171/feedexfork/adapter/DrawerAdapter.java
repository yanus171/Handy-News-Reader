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

import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_COUNT;
import static ru.yanus171.feedexfork.Constants.DB_IS_NULL;
import static ru.yanus171.feedexfork.Constants.DB_OR;
import static ru.yanus171.feedexfork.Constants.DB_SUM;
import static ru.yanus171.feedexfork.Constants.EMPTY_WHERE_SQL;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.TakeMarkAsReadList;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_READ;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_UNREAD;
import static ru.yanus171.feedexfork.utils.NetworkUtils.GetImageFileUri;
import static ru.yanus171.feedexfork.utils.UiUtils.SetFont;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.EntryLabelColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.Label;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class DrawerAdapter extends BaseAdapter {

    private HomeActivity mActivity = null;

    private static final int POS_ID = 0;
    private static final int POS_URL = 1;
    private static final int POS_NAME = 2;
    private static final int POS_IS_GROUP = 3;
    private static final int POS_ICON_URL = 4;
    private static final int POS_LAST_UPDATE = 5;
    private static final int POS_ERROR = 6;
    private static final int POS_IS_SHOW_TEXT_IN_ENTRY_LIST = 7;
    private static final int POS_IS_GROUP_EXPANDED = 8;
    private static final int POS_IS_AUTO_RESRESH = 9;
    private static final int POS_OPTIONS = 10;
    private static final int POS_IMAGESIZE = 11;


    public static final int FAVORITES_DRAWER_PAS = 2;
    public static final int LAST_READ_DRAWER_POS = 4;
    public static final int ALL_DRAWER_POS = 0;
    public static final int UNREAD_DRAWER_POS = 1;
    public static final int STARRED_DRAWER_POS = 2;
    public static final int EXTERNAL_DRAWER_POS = 3;
    public static final int LABEL_GROUP_POS = 5;
    public static final String PREF_IS_LABEL_GROUP_EXPANDED = "label_group_expanded";
    public static final String PREF_LABEL_ID_EXPANDED = "LABEL_ID_EXPANDED_";
    public static final String CHILD_LABEL_KEY = "CHILD_LABEL_";
    public static final String CHILD_LABEL_KEY_READ = "CHILD_LABEL_READ_";

    private final ProgressBar mProgressBar;
    public static boolean mIsNeedUpdateNumbers = true;

    public static boolean isLabelExpanded(long parentLabelID ) {
        return PrefUtils.getBoolean( PREF_LABEL_ID_EXPANDED + parentLabelID, false );
    }

    public static int FIRST_ENTRY_POS() {
        if ( !PrefUtils.getBoolean( PREF_IS_LABEL_GROUP_EXPANDED, false ) )
            return LABEL_GROUP_POS + 1;
        int result = LABEL_GROUP_POS;
        for ( Label label : LabelVoc.INSTANCE.getList() )
            result += 1 + (isLabelExpanded(label.mID ) ? LabelVoc.INSTANCE.getChildrenIDs(label.mID ).size() : 0);
        return result + 1;//LABEL_GROUP_POS + (? LabelVoc.INSTANCE.getList().size() : 0) + 1;
    }
    public static boolean isChildLabelPosition( int position ) {
        int result = LABEL_GROUP_POS;
        for ( Label parentLabel : LabelVoc.INSTANCE.getList() ) {
            result += 1;
            if ( position == result )
                return false;
            if ( isLabelExpanded(parentLabel.mID ) )
                for ( long ignored : LabelVoc.INSTANCE.getChildrenIDs(parentLabel.mID ) )  {
                    result++;
                    if ( position == result )
                        return true;
                }
        }
        return false;
    }
    public static Long getParentLabelID(int position) {
        int result = LABEL_GROUP_POS;
        for ( Label parentLabel : LabelVoc.INSTANCE.getList() ) {
            result += 1;
            if ( position == result )
                return parentLabel.mID;
            if ( isLabelExpanded(parentLabel.mID ) )
                for ( long ignored : LabelVoc.INSTANCE.getChildrenIDs(parentLabel.mID ) )  {
                    result++;
                    if ( position == result )
                        return parentLabel.mID;
                }
        }
        return -1L;
    }
    public static long getLabelIDByPosition( int position ) {
        int result = LABEL_GROUP_POS;
        for ( Label parentLabel : LabelVoc.INSTANCE.getList() ) {
            result += 1;
            if ( position == result )
                return parentLabel.mID;
            if ( isLabelExpanded(parentLabel.mID ) )
                for ( long childLabelID : LabelVoc.INSTANCE.getChildrenIDs( parentLabel.mID ) )  {
                    result++;
                    if ( position == result )
                        return childLabelID;
                }
        }
        return -1;
    }
    static public int getParentLabelPositionByID( long labelID ) {
        if ( !PrefUtils.getBoolean( PREF_IS_LABEL_GROUP_EXPANDED, false ) )
            return -1;
        int result = LABEL_GROUP_POS;
        for ( Label label : LabelVoc.INSTANCE.getList() ) {
            if ( label.mID == labelID )
                return  result;
            result += 1 + (isLabelExpanded(label.mID) ? LabelVoc.INSTANCE.getChildrenIDs(label.mID).size() : 0);
        }
        return -1;
    }



    private static final int NORMAL_TEXT_COLOR = Color.parseColor("#EEEEEE");
    private static final int GROUP_TEXT_COLOR = Color.parseColor("#BBBBBB");

    private static final String COLON = getContext().getString(R.string.colon);

    private static final int CACHE_MAX_ENTRIES = 100;
    public final Map<Long, String> mFormattedDateCache = new LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
        @Override
        public boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    private final Context mContext;
    private Cursor mFeedsCursor;
    private final String KEY_AllUnreadNumber = "AllUnreadNumber";
    private final String KEY_FavoritesUnreadNumber = "FavoritesUnreadNumber";
    private final String KEY_FavoritesReadNumber = "FavoritesReadNumber";
    private final String KEY_LastReadUnreadNumber = "LastReadUnreadNumber";
    private final String KEY_LastReadReadNumber = "LastReadReadNumber";
    private final String KEY_AllNumber = "AllNumber";
    private final String KEY_ExternalUnreadNumber = "ExternalUnreadNumber";
    private final String KEY_ExternalReadNumber = "ExternalReadNumber";
    private final String KEY_AllImagesSize = "AllImagesSize";
    private final String KEY_AllUnreadImagesSize = "AllUnreadImagesSize";
    private final String KEY_FavoritiesImagesSize = "FavoritiesImagesSize";
    private final String KEY_ExternalImagesSize = "ExternalImagesSize";
    private final String KEY_TextTaskCount = "TextTaskCount";
    private final String KEY_ImageTaskCount = "ImageTaskCount";
    private final String KEY_LabelEntriesUnreadCount = "LabelEntriesUnreadCount_";
    private final String KEY_LabelEntriesImagesSize = "LabelEntriesImagesSize1_";
    private final String KEY_LabelEntriesReadCount = "LabelEntriesReadCount_";
    private static final String KEY_FeedAllArticleCountVoc = "FeedAllArticleCountVoc_";
    private static final String KEY_FeedUnreadArticleCountVoc = "FeedUnreadArticleCountVoc_";

    public enum NewNumberOperType { Insert, Update }
    public static void newNumber( String feedID, NewNumberOperType type, boolean isRead ) {
        newNumber( feedID, type, isRead ? -1 : +1 );
    }
    static boolean isNumeric(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
    public static void newNumber( String feedID, NewNumberOperType type, int newUnreadCount ) {
        Dog.v(String.format("newNumber( %s, %d)", feedID, newUnreadCount ) );
        if ( !isNumeric( feedID ) )
            return;
        {
            final String key = getFeedUnreadArticleCountKey(Long.parseLong(feedID));
            int oldCount = PrefUtils.getInt(key, 0);
            if ( newUnreadCount > 0 && type == NewNumberOperType.Insert)
                PrefUtils.putInt( key, oldCount + newUnreadCount);
            //else if ( isRead && type == NewNumberOperType.Delete )
            //    PrefUtils.putInt( key, oldCount - 1);
            else if ( type == NewNumberOperType.Update )
                PrefUtils.putInt( key, oldCount + newUnreadCount);
        }
        {
            final String key = getFeedAllArticleCountKey(Long.parseLong(feedID));
            int oldCount = PrefUtils.getInt(key, 0);
            if (type == NewNumberOperType.Insert)
                PrefUtils.putInt(key, oldCount + newUnreadCount);
            //else if ( type == NewNumberOperType.Delete )
            //    PrefUtils.putInt(key, oldCount - newUnreadCount);
        }
        synchronized (DrawerAdapter.class) {
            mIsNeedUpdateNumbers = true;
        }
    }

    public static String getFeedAllArticleCountKey( long feedID ) {
        return KEY_FeedAllArticleCountVoc + feedID;
    }
    public static String getFeedUnreadArticleCountKey( long feedID ) {
        return KEY_FeedUnreadArticleCountVoc + feedID;
    }


    private String EXPR_NUMBER (final String where ) {
        return "CASE WHEN " + FeedData.FeedColumns.WHERE_GROUP + " THEN " + GROUP_NUMBER(where ) +
            " ELSE " + FEED_NUMBER( where ) + " END";
    }
    private static String FEED_NUMBER(final String where ) {
        return "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
            where + DB_AND  + EntryColumns.FEED_ID + '=' + FeedData.FeedColumns.TABLE_NAME + '.' + FeedData.FeedColumns._ID + ')';
    }
    private static String GROUP_NUMBER(final String where ) {
        if ( PrefUtils.getBoolean( "show_group_entries_count", false ) )
            return "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
                where + DB_AND + EntryColumns.FEED_ID + " IN ( SELECT " + FeedData.FeedColumns._ID +  " FROM " +
                FeedData.FeedColumns.TABLE_NAME + " AS t1"+ " WHERE " +
                FeedData.FeedColumns.GROUP_ID +  " = " + FeedData.FeedColumns.TABLE_NAME + "." + FeedData.FeedColumns._ID  + ") " + " )";
        else
            return "0";

    }

    final String EXPR_FEED_ALL_NUMBER = PrefUtils.getBoolean(PrefUtils.SHOW_READ_ARTICLE_COUNT, false ) ? EXPR_NUMBER("1=1" ) : "0";

    public DrawerAdapter(HomeActivity activity, Cursor feedCursor, ProgressBar progressBar) {
        mActivity = activity;
        mContext = activity;
        mFeedsCursor = feedCursor;
        mProgressBar = progressBar;
    }

    public void setCursor(Cursor feedCursor) {
        mFeedsCursor = feedCursor;
        notifyDataSetChanged();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_drawer_list, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = convertView.findViewById(android.R.id.icon);
            holder.iconViewSmall = convertView.findViewById(R.id.icon_small);
            holder.childExpandBtn = convertView.findViewById(R.id.icon_expand);
            holder.titleTxt = SetupTextView(convertView, android.R.id.text1);
            holder.stateTxt = SetupSmallTextView(convertView, android.R.id.text2);
            holder.imageSizeTxt = SetupSmallTextView(convertView, R.id.imageSize);
            holder.unreadTxt = SetupTextView(convertView, R.id.unread_count);
            holder.readTxt = SetupTextView(convertView, R.id.read_count);
            holder.tasksTxt = SetupSmallTextView(convertView, R.id.tasks);
            holder.autoRefreshIcon = convertView.findViewById(R.id.auto_refresh_icon);
            holder.separator = convertView.findViewById(R.id.separator);
            holder.layoutSize = convertView.findViewById(R.id.layout_size);

            convertView.setTag(R.id.holder, holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag(R.id.holder);

        // default init
        holder.iconView.setImageDrawable(null);
        holder.iconView.setVisibility( View.VISIBLE );
        holder.iconViewSmall.setImageDrawable(null);
        holder.iconViewSmall.setVisibility( View.GONE );
        holder.childExpandBtn.setVisibility( View.GONE );
        holder.childExpandBtn.setImageDrawable(null);
        holder.titleTxt.setText("");
        holder.titleTxt.setTextColor(NORMAL_TEXT_COLOR);
        holder.titleTxt.setAllCaps(false);
        SetFont(holder.titleTxt, 1);
        holder.stateTxt.setVisibility(View.GONE);
        holder.unreadTxt.setText("");
        SetFont(holder.unreadTxt, 1);
        holder.readTxt.setText("");
        SetFont(holder.readTxt, 1);
        convertView.setPadding(0, 0, 0, 0);
        holder.separator.setVisibility(View.GONE);
        holder.autoRefreshIcon.setVisibility(View.GONE);
        holder.imageSizeTxt.setVisibility(View.GONE);
        holder.tasksTxt.setVisibility(View.GONE);
        holder.layoutSize.setVisibility(View.VISIBLE);

        if ( position == ALL_DRAWER_POS ||
             position == UNREAD_DRAWER_POS ||
             position == STARRED_DRAWER_POS ||
             position == EXTERNAL_DRAWER_POS ||
             position == LAST_READ_DRAWER_POS) {
            switch (position) {
                case ALL_DRAWER_POS:
                    holder.titleTxt.setText(R.string.unread_entries);
                    holder.iconView.setImageResource(R.drawable.cup_new_unread);
                    SetCount(KEY_AllUnreadNumber, holder.unreadTxt);
                    SetImageSizeText(holder, KEY_AllUnreadImagesSize);
                    break;
                case UNREAD_DRAWER_POS:
                    holder.titleTxt.setText(R.string.all_entries);
                    holder.iconView.setImageResource(R.drawable.cup_new_pot);
                    SetCount(KEY_AllNumber, holder.unreadTxt);
                    holder.readTxt.setText("");
                    SetImageSizeText(holder, KEY_AllImagesSize);
                    String taskInfo = "";
                    final int textTaskCount = PrefUtils.getInt(KEY_TextTaskCount, 0);
                    if (textTaskCount != 0)
                        taskInfo += String.format(" T:%d", textTaskCount);
                    final int imageTaskCount = PrefUtils.getInt(KEY_ImageTaskCount, 0);
                    if (imageTaskCount != 0)
                        taskInfo += String.format(" I:%d", imageTaskCount);
                    if (!taskInfo.isEmpty()) {
                        holder.tasksTxt.setVisibility(View.VISIBLE);
                        holder.tasksTxt.setText(getContext().getString(R.string.tasks_to_download) + ": " + taskInfo);
                    }
                    break;
                case STARRED_DRAWER_POS:
                    holder.titleTxt.setText(R.string.favorites);
                    holder.iconView.setImageResource(R.drawable.star_yellow);
                    SetCount(KEY_FavoritesUnreadNumber, holder.unreadTxt);
                    SetCount(KEY_FavoritesReadNumber, holder.readTxt);
                    SetImageSizeText(holder, KEY_FavoritiesImagesSize);
                    break;
                case EXTERNAL_DRAWER_POS:
                    holder.titleTxt.setText(R.string.externalLinks);
                    holder.iconView.setImageResource(R.drawable.download_gray);
                    SetCount(KEY_ExternalUnreadNumber, holder.unreadTxt);
                    SetCount(KEY_ExternalReadNumber, holder.readTxt);
                    SetImageSizeText(holder, KEY_ExternalImagesSize);
                    break;
                case LAST_READ_DRAWER_POS:
                    holder.titleTxt.setText(R.string.last_read);
                    holder.iconView.setImageResource(R.drawable.clock_green_filled);
                    SetCount(KEY_LastReadUnreadNumber, holder.unreadTxt);
                    SetCount(KEY_LastReadReadNumber, holder.readTxt);
                    //SetImageSizeText(holder, KEY_ExternalImagesSize);
                    break;
            }
        } else if (position == LABEL_GROUP_POS) {
            holder.iconView.setImageResource(isGroupExpanded(position) ? R.drawable.ic_group_expanded_gray : R.drawable.ic_group_collapsed_gray);
            holder.iconView.setOnClickListener(v -> {
                PrefUtils.putBoolean(PREF_IS_LABEL_GROUP_EXPANDED, !isGroupExpanded(LABEL_GROUP_POS));
                notifyDataSetChanged();
            });
            holder.titleTxt.setText(R.string.labels_group_title);
            holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
            holder.titleTxt.setAllCaps(true);
            holder.separator.setVisibility(View.VISIBLE);
        } else if (isLabelPos(position)) {
            Label label = LabelVoc.INSTANCE.get(getLabelIDByPosition( position ));
            holder.titleTxt.setText(label.mName);
            holder.titleTxt.setTextColor(label.colorInt());
            holder.iconView.setImageResource(R.drawable.label_brown);
            SetCount( KEY_LabelEntriesUnreadCount + label.mID, holder.unreadTxt);
            SetCount( KEY_LabelEntriesReadCount + label.mID, holder.readTxt);
            SetImageSizeText(holder, KEY_LabelEntriesImagesSize + label.mID);
            holder.childExpandBtn.setVisibility( View.INVISIBLE );
            if ( !isChildLabelPosition( position ) && !LabelVoc.INSTANCE.getChildrenIDs( label.mID ).isEmpty() ) {
                holder.iconView.setVisibility( View.VISIBLE );
                holder.iconViewSmall.setVisibility(View.GONE);
                holder.childExpandBtn.setVisibility( View.VISIBLE );
                holder.childExpandBtn.setImageResource(isLabelExpanded(label.mID) ? R.drawable.ic_group_expanded_gray : R.drawable.ic_group_collapsed_gray);
                holder.childExpandBtn.setOnClickListener(v -> {
                    PrefUtils.putBoolean(PREF_LABEL_ID_EXPANDED + label.mID, !isLabelExpanded( label.mID));
                    notifyDataSetChanged();
                });
            } else if ( isChildLabelPosition( position ) ) {
                holder.iconView.setVisibility(View.GONE);
                holder.iconViewSmall.setVisibility(View.VISIBLE);
                holder.iconViewSmall.setImageResource( R.drawable.label_brown );
                holder.childExpandBtn.setVisibility( View.GONE );
                holder.tasksTxt.setVisibility(View.GONE);
                holder.layoutSize.setVisibility(View.GONE);
                long parentLabelID = getParentLabelID( position );
                SetCount( CHILD_LABEL_KEY + parentLabelID + "_" + label.mID, holder.unreadTxt);
                SetCount( CHILD_LABEL_KEY_READ + parentLabelID + "_" + label.mID, holder.readTxt);
                holder.imageSizeTxt.setVisibility( View.GONE );
                //convertView.setMinHeight(0); // Min Height
                //convertView.setMinimumHeight(0); // Min Height
                //convertView.setHeight(44); // Height
//                SetSmallFont(holder.titleTxt);
//                SetSmallFont(holder.unreadTxt);
//                SetSmallFont(holder.readTxt);
            }


        } else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS())) {
            holder.titleTxt.setText((mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME)));

            if (mFeedsCursor.getInt(POS_IS_GROUP) == 1) {
                holder.iconView.setImageResource(isGroupExpanded(position) ? R.drawable.ic_group_expanded_gray : R.drawable.ic_group_collapsed_gray);
                holder.iconView.setOnClickListener(v -> {
                    ContentResolver cr = getContext().getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(FeedData.FeedColumns.IS_GROUP_EXPANDED, isGroupExpanded(position) ? null : 1);
                    cr.update(FeedData.FeedColumns.CONTENT_URI(getItemId(position)), values, null, null);
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

                Uri iconUri = mFeedsCursor.isNull(POS_ICON_URL) ? Uri.EMPTY : GetImageFileUri(mFeedsCursor.getString(POS_ICON_URL), mFeedsCursor.getString(POS_ICON_URL));
                //final int dim = UiUtils.dpToPixel(35);
                Glide.with(parent.getContext())
                    .load(iconUri)
                    //.override( dim, dim )
                    //.centerCrop()
                    .placeholder(R.drawable.cup_new_empty)
                    .into(holder.iconView);

                holder.autoRefreshIcon.setVisibility(isAutoRefresh(position) ? View.VISIBLE : View.GONE);
            }
            final long feedOrGroupID = mFeedsCursor.getLong(POS_ID);
            int unread = PrefUtils.getInt( getFeedUnreadArticleCountKey( feedOrGroupID ), 0 );
            int all = PrefUtils.getInt( getFeedAllArticleCountKey(feedOrGroupID ), 0 );
            SetCount(getFeedUnreadArticleCountKey( feedOrGroupID ), holder.unreadTxt);
            int read = all - unread;
            if (read > 0)
                holder.readTxt.setText(String.valueOf(read));

            SetImageSizeText(holder, mFeedsCursor.getLong(POS_IMAGESIZE));
        }

        convertView.setBackgroundColor(position == mActivity.mDrawerList.getCheckedItemPosition() ? Theme.GetToolBarColorInt() : Color.TRANSPARENT );
        return convertView;
    }

    private void SetCount(String key, TextView view) {
        int count = PrefUtils.getInt( key, 0 );
        view.setText(count != 0 ? String.valueOf(count) : "");
    }

    @NotNull
    static public ArrayList<Label> getLabelList() {
        return PrefUtils.getBoolean( PREF_IS_LABEL_GROUP_EXPANDED, false ) ? LabelVoc.INSTANCE.getList() : new ArrayList<>();
    }

    static public boolean isLabelPos( int position ) {
        return position > LABEL_GROUP_POS && position < FIRST_ENTRY_POS();
    }

    static private void SetImageSizeText(ViewHolder holder, long size) {
        if ( PrefUtils.CALCULATE_IMAGES_SIZE() && size != 0 ) {
            holder.imageSizeTxt.setVisibility( View.VISIBLE );
            holder.imageSizeTxt.setText(GetImageSizeText(size));
        } else
            holder.imageSizeTxt.setVisibility( View.GONE );
    }

    static private void SetImageSizeText(ViewHolder holder, String key) {
        SetImageSizeText(holder, PrefUtils.getLong(key, 0));
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
        if ( position < FIRST_ENTRY_POS() )
            return Long.parseLong(FetcherService.GetExtrenalLinkFeedID());
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
        if ( position == LABEL_GROUP_POS )
            return getContext().getString( R.string.labels_group_title );
        else if ( isLabelPos( position ) )
            return LabelVoc.INSTANCE.get( getLabelIDByPosition( position ) ).mName;
        else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS()))
            return mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME);

        return null;
    }

//    private int getLabelPosition(int position) {
//        return position - LABEL_GROUP_POS - 1;
//    }

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

    public interface OnLabelReturnedFromCursor {
        void run(Label label, Cursor cur);
    }


    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    public void updateNumbersAsync() {
        synchronized (DrawerAdapter.class) {
            if (!mIsNeedUpdateNumbers)
                return;
            mIsNeedUpdateNumbers = false;
        }
        mProgressBar.setVisibility( View.VISIBLE );

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                updateNumbers();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                notifyDataSetChanged();
                mProgressBar.setVisibility( View.GONE );
            }

        }.execute();
    }
    private void updateNumbers() {
        EntriesListFragment.SetItemsAsRead( TakeMarkAsReadList( false ) );
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        ContentResolver cr = mContext.getContentResolver();
        Timer timer = new Timer("updateNumbers()");
        final boolean showRead = PrefUtils.getBoolean(PrefUtils.SHOW_READ_ARTICLE_COUNT, false);
        // Gets the numbers of entries (should be in a thread, but it's way easier like this and it shouldn't be so slow)
        Cursor numbers = cr.query(EntryColumns.CONTENT_URI,
                                  new String[]{FeedData.ALL_UNREAD_NUMBER,
                                      showRead ? FeedData.FAVORITES_READ_NUMBER : "0",
                                      showRead ? FeedData.FAVORITES_UNREAD_NUMBER : FeedData.FAVORITES_NUMBER,
                                      FeedData.ALL_NUMBER,
                                      showRead ? FeedData.EXTERNAL_UNREAD_NUMBER : FeedData.EXTERNAL_NUMBER,
                                      showRead ? FeedData.EXTERNAL_READ_NUMBER : "0",
                                      FeedData.ALL_IMAGESSIZE_NUMBER(),
                                      FeedData.ALL_UNREAD_IMAGESSIZE_NUMBER(),
                                      FeedData.FAVORITES_IMAGESSIZE_NUMBER(),
                                      FeedData.EXTERNAL_IMAGESSIZE_NUMBER(),
                                      showRead ? FeedData.LAST_READ_READ_NUMBER : "0",
                                      showRead ? FeedData.LAST_READ_UNREAD_NUMBER : FeedData.LAST_READ_NUMBER,
                                  },
                                  null, null, null);
        if (numbers != null) {
            if (numbers.moveToFirst()) {
                editor.putInt( KEY_AllUnreadNumber, numbers.getInt(0) );
                editor.putInt( KEY_FavoritesReadNumber, numbers.getInt(1) );
                editor.putInt( KEY_FavoritesUnreadNumber, numbers.getInt(2) );
                editor.putInt( KEY_AllNumber, numbers.getInt(3) );
                editor.putInt( KEY_ExternalUnreadNumber, numbers.getInt(4) );
                editor.putInt( KEY_ExternalReadNumber, numbers.getInt(5) );
                editor.putLong( KEY_AllImagesSize, numbers.getLong(6) );
                editor.putLong( KEY_AllUnreadImagesSize, numbers.getLong(7) );
                editor.putLong( KEY_FavoritiesImagesSize, numbers.getLong(8) );
                editor.putLong( KEY_ExternalImagesSize, numbers.getLong(9) );
                editor.putInt( KEY_LastReadReadNumber, numbers.getInt(10) );
                editor.putInt( KEY_LastReadUnreadNumber, numbers.getInt(11) );

            }
            numbers.close();
        }

        {
            Cursor cur = cr.query(FeedData.TaskColumns.CONTENT_URI,
                                  new String[]{FeedData.TaskColumns.TEXT_COUNT, FeedData.TaskColumns.IMAGE_COUNT},
                                  null, null, null);
            if (cur.moveToFirst()) {
                editor.putInt(KEY_TextTaskCount, cur.getInt(0) );
                editor.putInt(KEY_ImageTaskCount, cur.getInt(1) );
            }
            cur.close();
        }

        SetLabelsNumber(editor, DB_COUNT(EntryLabelColumns.ENTRY_ID), WHERE_UNREAD, KEY_LabelEntriesUnreadCount );
        SetLabelsNumber(editor, DB_COUNT(EntryLabelColumns.ENTRY_ID), WHERE_READ, KEY_LabelEntriesReadCount );
        SetLabelsNumber(editor, DB_SUM( EntryColumns.IMAGES_SIZE ), EMPTY_WHERE_SQL, KEY_LabelEntriesImagesSize );

        {
            Cursor cur = cr.query( FeedData.FeedColumns.GROUPED_FEEDS_CONTENT_URI,
                                   new String[]{FeedData.FeedColumns._ID, EXPR_FEED_ALL_NUMBER, EXPR_NUMBER( WHERE_UNREAD )},
                                   "(" + FeedData.FeedColumns.WHERE_GROUP + DB_OR +
                                       FeedData.FeedColumns.GROUP_ID + DB_IS_NULL + DB_OR +
                                       FeedData.FeedColumns.GROUP_ID + "=0" + DB_OR +
                                       FeedData.FeedColumns.GROUP_ID + " IN (SELECT " + FeedData.FeedColumns._ID +
                                       " FROM " + FeedData.FeedColumns.TABLE_NAME +
                                       " WHERE " + FeedData.FeedColumns.IS_GROUP_EXPANDED + Constants.DB_IS_TRUE + "))" + FeedData.getWhereNotExternal(),
                                   null,
                                   null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    editor.putInt(getFeedAllArticleCountKey( cur.getLong(0 ) ), cur.getInt(1));
                    editor.putInt(getFeedUnreadArticleCountKey( cur.getLong(0 ) ), cur.getInt(2));
                }
                cur.close();
            }
        }

        SetChildLabelsNumbers(editor);
        editor.apply();

        timer.End();
    }
    private void SetChildLabelsNumbers(SharedPreferences.Editor editor) {
        HashSet<Long> readEntriesMap = new HashSet<>();
        {
            ContentResolver cr = mContext.getContentResolver();
            Cursor cur = cr.query(FeedData.EntryColumns.CONTENT_URI, new String[]{FeedData.TaskColumns._ID}, WHERE_READ, null, null);
            while (cur.moveToNext())
                readEntriesMap.add(cur.getLong(0));
            cur.close();
        }
        {
            HashMap<String, Integer> map = LabelVoc.INSTANCE.GetChildLabelsNumbers(false, readEntriesMap);
            for (String key : map.keySet())
                editor.putInt(CHILD_LABEL_KEY + key, map.get(key));
        }
        {
            HashMap<String, Integer> map = LabelVoc.INSTANCE.GetChildLabelsNumbers(true, readEntriesMap);
            for (String key : map.keySet())
                editor.putInt(CHILD_LABEL_KEY_READ + key, map.get(key));
        }
    }
    private void SetLabelsNumber(SharedPreferences.Editor editor, String columnSQLFunc, String whereSQL, String key ) {
        for ( Label label: LabelVoc.INSTANCE.getList() )
            editor.putLong(key + label.mID, 0 );

        ContentResolver cr = mContext.getContentResolver();
        Cursor cur = cr.query(EntryLabelColumns.WITH_ENTRIES_URI,
                              new String[]{EntryLabelColumns.LABEL_ID, columnSQLFunc},
                              whereSQL, null, null);
        if (cur != null) {
            while (cur.moveToNext()) {
                Label label = LabelVoc.INSTANCE.get(cur.getLong(0));
                editor.putInt(key + label.mID, cur.isNull(1 ) ? 0 : cur.getInt(1));
                LabelVoc.INSTANCE.set(label);
            }
            cur.close();
        }
    }

    private static class ViewHolder {
        LinearLayout layoutSize;
        ImageView iconView;
        ImageView iconViewSmall;
        ImageView childExpandBtn;
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
