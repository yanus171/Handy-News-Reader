/**
 * Flym
 * <p/>
 * Copyri`ht (c) 2012-2015 Frederic Julian
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

package ru.yanus171.feedexfork.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Date;

import ru.yanus171.feedexfork.BuildConfig;
import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.EntryLabelColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns;
import ru.yanus171.feedexfork.provider.FeedData.LabelColumns;
import ru.yanus171.feedexfork.provider.FeedData.TaskColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.LabelSelectPreference;

import static android.provider.BaseColumns._ID;
import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_DESC;
import static ru.yanus171.feedexfork.provider.FeedData.ENTRY_LABELS_WITH_ENTRIES;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.READ_DATE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_LAST_READ;
import static ru.yanus171.feedexfork.utils.PrefUtils.IsFeedsABCSort;
import static ru.yanus171.feedexfork.view.ListSelectPreference.DefaultSeparator;

public class FeedDataContentProvider extends ContentProvider {

    private static final int URI_GROUPED_FEEDS = 1;
    private static final int URI_GROUPS = 2;
    private static final int URI_GROUP = 3;
    private static final int URI_FEEDS_FOR_GROUPS = 4;
    private static final int URI_FEEDS = 5;
    private static final int URI_FEED = 6;
    private static final int URI_FILTERS = 7;
    private static final int URI_FILTERS_FOR_FEED = 8;
    public static final int URI_ENTRIES_FOR_FEED = 9;
    private static final int URI_ENTRY_FOR_FEED = 10;
    public static final int URI_ENTRIES_FOR_GROUP = 11;

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        return super.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    private static final int URI_ENTRY_FOR_GROUP = 12;
    public static final int URI_ENTRIES = 13;
    private static final int URI_ENTRY = 14;
    private static final int URI_UNREAD_ENTRY = 16;
    public static final int URI_UNREAD_ENTRIES = 15;
    public static final int URI_FAVORITES = 17;
    private static final int URI_FAVORITES_ENTRY = 18;
    private static final int URI_TASKS = 19;
    private static final int URI_TASK = 20;
    private static final int URI_SEARCH = 21;
    private static final int URI_SEARCH_ENTRY = 22;
    private static final int URI_GROUPS_AND_ROOT_FEEDS = 27;
    private static final int URI_LABELS = 29;
    private static final int URI_LABEL = 30;
    private static final int URI_ENTRIES_LABELS = 31;
    private static final int URI_ENTRY_LABELS = 32;
    private static final int URI_ENTRIES_LABELS_WITH_ENTRIES = 33;
    public static final int URI_LAST_READ = 34;
    private static final int URI_LAST_READ_ENTRY = 35;

    public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    public static int mNotifyBlockCount = 0;

    static {
        URI_MATCHER.addURI(FeedData.AUTHORITY, "grouped_feeds", URI_GROUPED_FEEDS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups", URI_GROUPS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups_and_root", URI_GROUPS_AND_ROOT_FEEDS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#", URI_GROUP);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#/feeds", URI_FEEDS_FOR_GROUPS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds", URI_FEEDS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#", URI_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries", URI_ENTRIES_FOR_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries/#", URI_ENTRY_FOR_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#/entries", URI_ENTRIES_FOR_GROUP);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#/entries/#", URI_ENTRY_FOR_GROUP);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "filters", URI_FILTERS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/filters", URI_FILTERS_FOR_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries", URI_ENTRIES);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/#", URI_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "unread_entries/#", URI_UNREAD_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "unread_entries", URI_UNREAD_ENTRIES);

        URI_MATCHER.addURI(FeedData.AUTHORITY, "last_read", URI_LAST_READ);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "last_read/#", URI_LAST_READ_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites", URI_FAVORITES);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites/#", URI_FAVORITES_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "tasks", URI_TASKS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "tasks/#", URI_TASK);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "labels/#", URI_LABEL);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "labels", URI_LABELS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entrylabels/#", URI_ENTRY_LABELS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entrylabels", URI_ENTRIES_LABELS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entrylabels/with_entries", URI_ENTRIES_LABELS_WITH_ENTRIES);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/search/*", URI_SEARCH);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/search/*/#", URI_SEARCH_ENTRY);
    }

    private static final String[] MAX_PRIORITY = new String[]{"MAX(" + FeedColumns.PRIORITY + ")"};

    private DatabaseHelper mDatabaseHelper;

    public static Pair<Uri, Boolean> addFeed(Context context, String url, String name, Long groupID,
                                             boolean retrieveFullText,
                                             boolean showTextInEntryList,
                                             boolean imageAutoLoad,
                                             String options) {
        boolean added = false;
        ContentResolver cr = context.getContentResolver();

        if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
            url = Constants.HTTP_SCHEME + url;
        }

        Cursor cursor = cr.query(FeedColumns.CONTENT_URI, new String[] {_ID}, FeedColumns.URL + Constants.DB_ARG,
                new String[]{url}, null);

        Uri result = Uri.EMPTY;
        if (cursor.moveToFirst()) {
            Toast.makeText(context, R.string.error_feed_url_exists, Toast.LENGTH_SHORT).show();
            final long feedId = cursor.getLong(0);
            result = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( feedId );
            context.startActivity( new Intent( context, HomeActivity.class )
                    .setAction(Intent.ACTION_MAIN)
                    .setData( result ) );
            cursor.close();
        } else {
            cursor.close();
            ContentValues values = new ContentValues();

            values.put(FeedColumns.URL, url);
            values.putNull(FeedColumns.ERROR);

            if (name.trim().length() > 0) {
                values.put(FeedColumns.NAME, name);
            }
            values.put(FeedColumns.RETRIEVE_FULLTEXT, retrieveFullText ? 1 : null);
            values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, showTextInEntryList ? 1 : null);
            if ( groupID != null ) {
                values.put(FeedColumns.GROUP_ID, groupID);
                values.put(FeedColumns.IS_GROUP_EXPANDED, 1);
            } else
                values.putNull( FeedColumns.GROUP_ID );
            values.put(FeedColumns.IS_IMAGE_AUTO_LOAD, imageAutoLoad);
            values.put(FeedColumns.OPTIONS, options);

            result = cr.insert(FeedColumns.CONTENT_URI, values);
            added = true;
        }
        return new Pair<>(result, added);
    }

    private static String getSearchWhereClause(String uriSearchParam) {
        uriSearchParam = Uri.decode(uriSearchParam).trim();

        if (!uriSearchParam.isEmpty()) {
            uriSearchParam = DatabaseUtils.sqlEscapeString("%" + Uri.decode(uriSearchParam) + "%");
            return EntryColumns.TITLE + " LIKE " + uriSearchParam; //+ Constants.DB_OR + EntryColumns.ABSTRACT + " LIKE " + uriSearchParam + Constants.DB_OR + EntryColumns.MOBILIZED_HTML + " LIKE " + uriSearchParam;
        } else {
            return "1 = 2"; // to have 0 result with an empty search
        }
    }

    @Override
    public String getType(Uri uri) {
        int matchCode = URI_MATCHER.match(uri);

        switch (matchCode) {
            case URI_GROUPED_FEEDS:
            case URI_GROUPS:
            case URI_FEEDS_FOR_GROUPS:
            case URI_GROUPS_AND_ROOT_FEEDS:
            case URI_FEEDS:
                return "vnd.android.cursor.dir/vnd.flymfork.feed";
            case URI_GROUP:
            case URI_FEED:
                return "vnd.android.cursor.item/vnd.flymfork.feed";
            case URI_FILTERS:
            case URI_FILTERS_FOR_FEED:
                return "vnd.android.cursor.dir/vnd.flymfork.filter";
            case URI_FAVORITES:
            case URI_LAST_READ:
            case URI_UNREAD_ENTRIES:
            case URI_ENTRIES:
            case URI_ENTRIES_FOR_FEED:
            case URI_ENTRIES_FOR_GROUP:
            case URI_SEARCH:
                return "vnd.android.cursor.dir/vnd.flymfork.entry";
            case URI_FAVORITES_ENTRY:
            case URI_LAST_READ_ENTRY:
            case URI_ENTRY:
            case URI_UNREAD_ENTRY:
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP:
            case URI_SEARCH_ENTRY:
                return "vnd.android.cursor.item/vnd.flymfork.entry";
            case URI_TASKS:
                return "vnd.android.cursor.dir/vnd.flymfork.task";
            case URI_TASK:
                return "vnd.android.cursor.item/vnd.flymfork.task";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(new Handler(), getContext());

        return true;
    }

    static private String getSortOrder() {
        return IsFeedsABCSort() ? FeedColumns.NAME : FeedColumns.PRIORITY;
    }
    public static String FEEDS_TABLE_WITH_GROUP_PRIORITY() {
        return FeedColumns.TABLE_NAME +
            " LEFT JOIN " +
            "(SELECT " + FeedColumns._ID + " AS joined_feed_id, " + getSortOrder() + " AS group_priority FROM " + FeedColumns.TABLE_NAME + ") AS f " +
            "ON (" + FeedColumns.TABLE_NAME + '.' + FeedColumns.GROUP_ID + " = f.joined_feed_id)";
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        FetcherService.Status().ChangeDB("query DB");

        long time = new Date().getTime();
        // This is a debug code to allow to visualize the task with the ContentProviderHelper app
        if (uri != null && BuildConfig.DEBUG && FeedData.CONTENT_AUTHORITY.equals(uri.toString())) {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(TaskColumns.TABLE_NAME);
            SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
            FetcherService.Status().ChangeDB("");
            return queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
        }

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int matchCode = URI_MATCHER.match(uri);

        if ((matchCode == URI_FEEDS || matchCode == URI_GROUPS || matchCode == URI_GROUPS_AND_ROOT_FEEDS || matchCode == URI_FEEDS_FOR_GROUPS) && sortOrder == null) {
            sortOrder = FeedColumns.PRIORITY;
        }

        switch (matchCode) {
            case URI_GROUPED_FEEDS: {
                queryBuilder.setTables(FEEDS_TABLE_WITH_GROUP_PRIORITY());
                if ( IsFeedsABCSort() )
                    sortOrder = "(CASE WHEN " + FeedColumns.IS_GROUP + " = 1 OR group_priority IS NOT NULL THEN 0 ELSE 1 END), IFNULL(group_priority, " + FeedColumns.NAME + " ), " + FeedColumns.IS_GROUP + " DESC, " + FeedColumns.NAME;
                else
                    sortOrder =  "IFNULL(group_priority, " + FeedColumns.PRIORITY + "), IFNULL(" + FeedColumns.GROUP_ID + ", " + _ID + "), " + FeedColumns.IS_GROUP + " DESC, " + FeedColumns.PRIORITY;
                break;
            }
            case URI_GROUPS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_TRUE) );//.append(Constants.DB_OR)
                        //.append(FeedColumns.GROUP_ID).append(Constants.DB_IS_NULL));
                break;
            }
            case URI_GROUPS_AND_ROOT_FEEDS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_TRUE).append(Constants.DB_OR)
                .append(FeedColumns.GROUP_ID).append(Constants.DB_IS_NULL).append(FeedData.getWhereNotExternal()));
                break;
            }
            case URI_FEEDS_FOR_GROUPS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_GROUP:
            case URI_FEED: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_FEEDS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_NULL));
                break;
            }
            case URI_FILTERS: {
                queryBuilder.setTables(FilterColumns.TABLE_NAME);
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                queryBuilder.setTables(FilterColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP:
            case URI_SEARCH_ENTRY: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(3)));
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRIES_FOR_GROUP: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRIES:{
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                break;
            }
            case URI_LAST_READ:{
                // later
                break;
            }
            case URI_UNREAD_ENTRIES: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(EntryColumns.WHERE_UNREAD);
                break;
            }
            case URI_SEARCH: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(getSearchWhereClause(uri.getPathSegments().get(2)));
                break;
            }
            case URI_FAVORITES_ENTRY:
            case URI_LAST_READ_ENTRY:
            case URI_UNREAD_ENTRY:
            case URI_ENTRY: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_FAVORITES: {
                queryBuilder.setTables(FeedData.ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE));
                break;
            }
            case URI_TASKS: {
                queryBuilder.setTables(FeedData.TASKS_WITH_FEED_INFO);
                break;
            }
            case URI_TASK: {
                queryBuilder.setTables(TaskColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_LABELS: {
                queryBuilder.setTables(LabelColumns.TABLE_NAME);
                break;
            }
            case URI_LABEL: {
                queryBuilder.setTables(LabelColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRIES_LABELS: {
                queryBuilder.setTables(EntryLabelColumns.TABLE_NAME);
                break;
            }
            case URI_ENTRY_LABELS: {
                queryBuilder.setTables(LabelColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryLabelColumns.ENTRY_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRIES_LABELS_WITH_ENTRIES: {
                queryBuilder.setTables(ENTRY_LABELS_WITH_ENTRIES( selection ) );
                SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
                return queryBuilder.query(database, projection, null, null, null, null, sortOrder);

            }
            default:
                throw new IllegalArgumentException("Illegal query. Match code=" + matchCode + "; uri=" + uri);
        }

        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        if ( matchCode == URI_LAST_READ ) {
            queryBuilder.setTables("(SELECT * FROM ( " + FeedData.ENTRIES_TABLE_WITH_FEED_INFO + ") ORDER BY " + READ_DATE + DB_DESC + " LIMIT " + PrefUtils.getIntFromText("last_read_count",  20) + ")" );
            queryBuilder.appendWhere( WHERE_LAST_READ + DB_AND + EntryColumns._ID + " NOT IN ( SELECT " + EntryLabelColumns.ENTRY_ID + " FROM " + EntryLabelColumns.TABLE_NAME +
                                       " WHERE " + EntryLabelColumns.LABEL_ID + " IN ( " + LabelSelectPreference.GetIDList( "lastReadHideLabelList", DefaultSeparator) + "))" );
        }
        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        Dog.v("query " + (new Date().getTime() - time) + " uri = " + uri);

        FetcherService.Status().ChangeDB("");
        return cursor;
    }

    public static int getNewGroupPriority( long groupID ) {
        int result = 1;
        Cursor cursor = MainApplication.getContext().getContentResolver().query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupID), MAX_PRIORITY, null, null, null);
        if ( cursor.moveToFirst() && !cursor.isNull( 0 ) )
            result = cursor.getInt( 0 ) + 1;
        cursor.close();
        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        FetcherService.Status().ChangeDB("insert DB");
        long newId;

        int matchCode = URI_MATCHER.match(uri);

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        switch (matchCode) {
            case URI_GROUPS:
            case URI_FEEDS: {
                Cursor cursor;
                if ( matchCode == URI_GROUPS  ) {
                    cursor = query(FeedColumns.GROUPS_AND_ROOT_CONTENT_URI, MAX_PRIORITY, null, null, null);
                } else if ( matchCode == URI_FEEDS && values.getAsInteger( FeedColumns.GROUP_ID ) != null ) {//values.containsKey(FeedColumns.GROUP_ID)  ) {
                    String groupId = values.getAsString(FeedColumns.GROUP_ID);
                    cursor = query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupId), MAX_PRIORITY, null, null, null);
                } else
                    cursor = query(FeedColumns.GROUPS_AND_ROOT_CONTENT_URI, MAX_PRIORITY, FeedColumns.GROUP_ID + Constants.DB_IS_NULL, null, null);

                if (cursor.moveToFirst()) { // normally this is always the case with MAX()
                    values.put(FeedColumns.PRIORITY, cursor.getInt(0) + 1);
                } else {
                    values.put(FeedColumns.PRIORITY, 1);
                }
                cursor.close();

                newId = database.insert(FeedColumns.TABLE_NAME, null, values);
                //mDatabaseHelper.exportToOPML();

                break;
            }
            case URI_FILTERS: {
                newId = database.insert(FilterColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                values.put(FilterColumns.FEED_ID, uri.getPathSegments().get(1));
                newId = database.insert(FilterColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                final long feedID = Long.parseLong( uri.getPathSegments().get(1) );
                values.put(EntryColumns.FEED_ID, feedID);
                values.put(EntryColumns.FETCH_DATE, new Date().getTime());
                newId = database.insert(EntryColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_TASKS: {
                newId = database.insert(TaskColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_LABELS: {
                newId = database.insert(LabelColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_ENTRIES_LABELS: {
                newId = database.insert(EntryLabelColumns.TABLE_NAME, null, values);
                break;
            }

            default:
                throw new IllegalArgumentException("Illegal insert. Match code=" + matchCode + "; uri=" + uri);
        }
        FetcherService.Status().ChangeDB("");
        if (newId > -1) {
            notifyChangeOnAllUris(matchCode, uri);
            return ContentUris.withAppendedId(uri, newId);
        } else { // This can happen when an insert failed with "ON CONFLICT IGNORE", this is not an error
            return uri;
        }
    }

    private static boolean mPriorityManagement = true;
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (uri == null || values == null) {
            throw new IllegalArgumentException("Illegal update. Uri=" + uri + "; values=" + values);
        }

        FetcherService.Status().ChangeDB("update DB");
        int matchCode = URI_MATCHER.match(uri);

        String table;

        StringBuilder where = new StringBuilder();

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        switch (matchCode) {
            case URI_FEED: {
                table = FeedColumns.TABLE_NAME;

                long feedId = Long.parseLong(uri.getPathSegments().get(1));
                where.append(_ID).append('=').append(feedId);

                if (values.containsKey(FeedColumns.PRIORITY) && mPriorityManagement ) {
                    Cursor priorityCursor = database.query(FeedColumns.TABLE_NAME, new String[]{FeedColumns.PRIORITY, FeedColumns.GROUP_ID},
                            _ID + "=" + feedId, null, null, null, null);
                    if (priorityCursor.moveToNext()) {
                        int oldPriority = priorityCursor.getInt(0);
                        String oldGroupId = priorityCursor.getString(1);
                        int newPriority = values.getAsInteger(FeedColumns.PRIORITY);
                        String newGroupId = values.getAsString(FeedColumns.GROUP_ID);

                        priorityCursor.close();

                        String oldGroupWhere = '(' + (oldGroupId != null ? FeedColumns.GROUP_ID + '=' + oldGroupId : FeedColumns.IS_GROUP
                                + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';

                        // If the group has changed, it is not only a +1 or -1 for priority...
                        if ((oldGroupId == null && newGroupId != null) || (oldGroupId != null && newGroupId == null)
                                || (oldGroupId != null && newGroupId != null && !oldGroupId.equals(newGroupId))) {

                            String priorityValue = FeedColumns.PRIORITY + "-1";
                            String priorityWhere = FeedColumns.PRIORITY + '>' + oldPriority;
                            database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                    + oldGroupWhere + Constants.DB_AND + priorityWhere);

                            priorityValue = FeedColumns.PRIORITY + "+1";
                            priorityWhere = FeedColumns.PRIORITY + '>' + (newPriority - 1);
                            String newGroupWhere = '(' + (newGroupId != null ? FeedColumns.GROUP_ID + '=' + newGroupId : FeedColumns.IS_GROUP
                                    + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';
                            database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                    + newGroupWhere + Constants.DB_AND + priorityWhere);

                        } else { // We move the item into the same group
                            if (newPriority > oldPriority) {
                                String priorityValue = FeedColumns.PRIORITY + "-1";
                                String priorityWhere = '(' + FeedColumns.PRIORITY + " BETWEEN " + (oldPriority + 1) + " AND " + newPriority + ')';
                                database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                        + oldGroupWhere + Constants.DB_AND + priorityWhere);

                            } else if (newPriority < oldPriority) {
                                String priorityValue = FeedColumns.PRIORITY + "+1";
                                String priorityWhere = '(' + FeedColumns.PRIORITY + " BETWEEN " + newPriority + " AND " + (oldPriority - 1) + ')';
                                database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                        + oldGroupWhere + Constants.DB_AND + priorityWhere);
                            }
                        }
                    } else {
                        priorityCursor.close();
                    }
                }
                break;
            }
            case URI_GROUPS:
            case URI_FEEDS_FOR_GROUPS:
            case URI_FEEDS: {
                table = FeedColumns.TABLE_NAME;
                break;
            }
            case URI_FILTERS: {
                table = FilterColumns.TABLE_NAME;
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                table = FilterColumns.TABLE_NAME;
                where.append(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP:
            case URI_SEARCH_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(3));
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                //TODO also remove tasks
                break;
            }
            case URI_ENTRIES_FOR_GROUP: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append(" IN (SELECT ").append(_ID).append(" FROM ").append(FeedColumns.TABLE_NAME).append(" WHERE ").append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)).append(')');
                break;
            }
            case URI_ENTRIES: {
                table = EntryColumns.TABLE_NAME;
                break;
            }
            case URI_UNREAD_ENTRIES: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.WHERE_UNREAD);
                break;
            }
            case URI_SEARCH: {
                table = EntryColumns.TABLE_NAME;
                where.append(getSearchWhereClause(uri.getPathSegments().get(2)));
                break;
            }
            case URI_FAVORITES_ENTRY:
            case URI_LAST_READ_ENTRY:
            case URI_UNREAD_ENTRY:
            case URI_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_FAVORITES: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE);
                break;
            }
            case URI_TASKS: {
                table = TaskColumns.TABLE_NAME;
                break;
            }
            case URI_TASK: {
                table = TaskColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_LABEL: {
                table = LabelColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal update. Match code=" + matchCode + "; uri=" + uri);
        }

        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(Constants.DB_AND).append(selection);
            } else {
                where.append(selection);
            }
        }

        int count = database.update(table, values, where.toString(), selectionArgs);

//        if ( mPriorityManagement && ( FeedColumns.TABLE_NAME.equals(table)
//                && (values.containsKey(FeedColumns.NAME) || values.containsKey(FeedColumns.URL) || values.containsKey(FeedColumns.PRIORITY)) ) ) {
//            mDatabaseHelper.exportToOPML();
//        }
        FetcherService.Status().ChangeDB("");
        if (count > 0 ) {
            notifyChangeOnAllUris(matchCode, uri);
        }
        return count;
    }

    static synchronized boolean IsNotifyEnabled() {
        return mNotifyBlockCount == 0;
    }

    public static synchronized void SetNotifyEnabled(boolean value) {
        if ( value )
            mNotifyBlockCount--;
        else
            mNotifyBlockCount++;
        if ( mNotifyBlockCount < 0 )
            mNotifyBlockCount = 0;
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        FetcherService.Status().ChangeDB("delete DB");
        int matchCode = URI_MATCHER.match(uri);

        String table;

        StringBuilder where = new StringBuilder();

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        switch (matchCode) {
            case URI_GROUP: {
                table = FeedColumns.TABLE_NAME;

                String groupId = uri.getPathSegments().get(1);

                where.append(_ID).append('=').append(groupId);

                // Delete the sub feeds & their entries
                Cursor subFeedsCursor = database.query(FeedColumns.TABLE_NAME, FeedColumns.PROJECTION_ID, FeedColumns.GROUP_ID + "=" + groupId, null,
                        null, null, null);
                while (subFeedsCursor.moveToNext()) {
                    String feedId = subFeedsCursor.getString(0);
                    delete(FeedColumns.CONTENT_URI(feedId), null, null);
                }
                subFeedsCursor.close();

                // Update the priorities
                Cursor priorityCursor = database.query(FeedColumns.TABLE_NAME, FeedColumns.PROJECTION_PRIORITY, _ID + "=" + groupId, null,
                        null, null, null);

                if (priorityCursor.moveToNext()) {
                    int priority = priorityCursor.getInt(0);
                    String priorityWhere = FeedColumns.PRIORITY + " > " + priority;
                    String groupWhere = '(' + FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL
                            + ')';
                    database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + " = " + FeedColumns.PRIORITY + "-1 WHERE "
                            + groupWhere + Constants.DB_AND + priorityWhere);
                }
                priorityCursor.close();
                break;
            }
            case URI_FEEDS: {
                table = FeedColumns.TABLE_NAME;

                delete(TaskColumns.CONTENT_URI, null, null);
                delete(FilterColumns.CONTENT_URI, null, null);
                delete(EntryColumns.CONTENT_URI, null, null);
                break;
            }
            case URI_FEED: {
                table = FeedColumns.TABLE_NAME;

                final String feedId = uri.getPathSegments().get(1);

                // Remove also the feed entries & filters
                new Thread() {
                    @Override
                    public void run() {
                        Uri entriesUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedId);
                        delete(entriesUri, null, null);
                        delete(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), null, null);
                    }
                }.start();

                where.append(_ID).append('=').append(feedId);

                // Update the priorities
                Cursor priorityCursor = database.query(FeedColumns.TABLE_NAME, new String[]{FeedColumns.PRIORITY, FeedColumns.GROUP_ID},
                                                       _ID + '=' + feedId, null, null, null, null);

                if (priorityCursor.moveToNext()) {
                    int priority = priorityCursor.getInt(0);
                    String groupId = priorityCursor.getString(1);

                    String groupWhere = '(' + (groupId != null ? FeedColumns.GROUP_ID + '=' + groupId : FeedColumns.IS_GROUP + Constants.DB_IS_TRUE
                        + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';
                    String priorityWhere = FeedColumns.PRIORITY + " > " + priority;

                    database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + " = " + FeedColumns.PRIORITY + "-1 WHERE "
                                         + groupWhere + Constants.DB_AND + priorityWhere);
                }
                priorityCursor.close();
                break;
            }
            case URI_FEEDS_FOR_GROUPS: {
                table = FeedColumns.TABLE_NAME;
                where.append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_FILTERS: {
                table = FilterColumns.TABLE_NAME;
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                table = FilterColumns.TABLE_NAME;
                where.append(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP:
            case URI_SEARCH_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                final String entryId = uri.getPathSegments().get(3);
                where.append(_ID).append('=').append(entryId);
                // Also remove the associated tasks
                new Thread() {
                    @Override
                    public void run() {
                        delete(TaskColumns.CONTENT_URI, TaskColumns.ENTRY_ID + '=' + entryId, null);
                    }
                }.start();
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));

                //TODO also remove tasks

                break;
            }
            case URI_UNREAD_ENTRIES: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.WHERE_UNREAD);
                //TODO also remove tasks

                break;
            }
            case URI_ENTRIES_FOR_GROUP: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID)
                        .append(" IN (SELECT ").append(_ID)
                        .append(" FROM ").append(FeedColumns.TABLE_NAME)
                        .append(" WHERE ").append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)).append(')');

                //TODO also remove tasks

                break;
            } case URI_ENTRIES: {
                table = EntryColumns.TABLE_NAME;

                // Also remove all tasks
                new Thread() {
                    @Override
                    public void run() {
                        delete(TaskColumns.CONTENT_URI, null, null);
                    }
                }.start();
                break;
            }
            case URI_FAVORITES_ENTRY:
            case URI_LAST_READ_ENTRY:
            case URI_UNREAD_ENTRY:
            case URI_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_FAVORITES: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE);
                break;
            }
            case URI_TASKS: {
                table = TaskColumns.TABLE_NAME;
                break;
            }
            case URI_TASK: {
                table = TaskColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_LABELS: {
                table = LabelColumns.TABLE_NAME;
                break;
            }
            case URI_LABEL: {
                table = LabelColumns.TABLE_NAME;
                where.append(_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRIES_LABELS: {
                table = EntryLabelColumns.TABLE_NAME;
                break;
            }
            case URI_ENTRY_LABELS: {
                table = EntryLabelColumns.TABLE_NAME;
                where.append(EntryLabelColumns.ENTRY_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            default:
                throw new IllegalArgumentException("Illegal delete. Match code=" + matchCode + "; uri=" + uri);
        }

        if (!TextUtils.isEmpty(selection)) {
            if (where.length() > 0) {
                where.append(Constants.DB_AND);
            }
            where.append(selection);
        }

        // If it's an entry deletion, delete associated cache files
        // Need to be done before the real entry deletion
        if (EntryColumns.TABLE_NAME.equals(table)) {
//            Cursor cursor = getContext().getContentResolver().query( uri, new String[]{ _ID, LINK }, where.toString(), null, null );
//            while( cursor.moveToNext() )
//                FileUtils.INSTANCE.deleteMobilized( cursor.getString( 1 ), EntryColumns.CONTENT_URI( cursor.getLong( 0 ) ) );


            //NetworkUtils.deleteEntriesImagesCache(uri, where.toString(), selectionArgs);
        }

        int count = database.delete(table, where.toString(), selectionArgs);

        if (count > 0  ) {
//            if (FeedColumns.TABLE_NAME.equals(table)) {
//                mDatabaseHelper.exportToOPML();
//            }

            notifyChangeOnAllUris(matchCode, uri);
        }
        FetcherService.Status().ChangeDB("");
        return count;
    }

    public static void notifyChangeOnAllUris(int matchCode, Uri uri) {
        synchronized (DrawerAdapter.class) {
            DrawerAdapter.mIsNeedUpdateNumbers = true;
        }

        if ( !IsNotifyEnabled() )
            return;

        ContentResolver cr = MainApplication.getContext().getContentResolver();
        if ( uri != null )
            cr.notifyChange(uri, null);

        if (matchCode != URI_FILTERS && matchCode != URI_FILTERS_FOR_FEED && matchCode != URI_TASKS && matchCode != URI_TASK) {
            // Notify everything else (except EntryColumns.CONTENT_URI to not update the
            // entry WebView when clicking on "favorite" button)
            cr.notifyChange(FeedColumns.GROUPED_FEEDS_CONTENT_URI, null);
            cr.notifyChange(EntryColumns.UNREAD_ENTRIES_CONTENT_URI, null);
            cr.notifyChange(EntryColumns.FAVORITES_CONTENT_URI, null);
            cr.notifyChange(EntryColumns.LAST_READ_CONTENT_URI, null);
            cr.notifyChange(FeedColumns.CONTENT_URI, null);
            cr.notifyChange(FeedColumns.GROUPS_CONTENT_URI, null);
            cr.notifyChange(FeedColumns.GROUPS_AND_ROOT_CONTENT_URI, null);
        }
        if ( EntriesListFragment.mSearchQueryUri != null )
            cr.notifyChange(EntriesListFragment.mSearchQueryUri, null);

    }


}
