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

package ru.yanus171.feedexfork.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.PrefUtils;

import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_COUNT;
import static ru.yanus171.feedexfork.Constants.DB_IS_FALSE;
import static ru.yanus171.feedexfork.Constants.DB_IS_NOT_NULL;
import static ru.yanus171.feedexfork.Constants.DB_IS_NULL;
import static ru.yanus171.feedexfork.Constants.DB_IS_TRUE;
import static ru.yanus171.feedexfork.Constants.DB_OR;
import static ru.yanus171.feedexfork.Constants.DB_SUM;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.FEED_ID;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.IMAGES_SIZE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_FAVORITE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_LAST_READ;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_READ;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_UNREAD;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;

public class FeedData {
    public static final String PACKAGE_NAME = "ru.yanus171.feedexfork";
    private static final String CONTENT = "content://";
    public static final String AUTHORITY = PACKAGE_NAME + ".provider.FeedData";
    public static final String CONTENT_AUTHORITY = CONTENT + AUTHORITY;
    public static final String ENTRY_LABELS_WITH_ENTRIES(String where) {
        return
            "( SELECT * FROM " +
            EntryLabelColumns.TABLE_NAME + " AS el" +
            " LEFT JOIN " +
            "( SELECT " + EntryColumns._ID + ", " + IMAGES_SIZE + ", " + EntryColumns.IS_READ + ", " + EntryColumns.IMAGES_SIZE + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + where + ") AS e " +
            " ON (el." + EntryLabelColumns.ENTRY_ID + " = e." + EntryColumns._ID + ") " +
            " WHERE e." + EntryColumns._ID + DB_IS_NOT_NULL +
            ") AS f1 GROUP BY " + EntryLabelColumns.LABEL_ID;
    }
    public static final String ENTRIES_TABLE_WITH_FEED_INFO = EntryColumns.TABLE_NAME + " JOIN (SELECT " + FeedColumns._ID + " AS joined_feed_id, " + FeedColumns.RETRIEVE_FULLTEXT + ", " + FeedColumns.NAME + ", " + FeedColumns.URL + ", " +
            FeedColumns.ICON_URL + ", " + FeedColumns.IS_IMAGE_AUTO_LOAD + ", " + FeedColumns.OPTIONS + ", " + FeedColumns.GROUP_ID + " FROM " + FeedColumns.TABLE_NAME + ") AS f ON (" + EntryColumns.TABLE_NAME + '.' + FEED_ID + " = f.joined_feed_id)";
    public static final String TASKS_WITH_FEED_INFO = TaskColumns.TABLE_NAME  + " LEFT JOIN (SELECT " + EntryColumns._ID + " AS entry_id, " + EntryColumns.LINK + " FROM " + EntryColumns.TABLE_NAME + ") AS f " +
            "ON (" + TaskColumns.TABLE_NAME + '.' + TaskColumns.ENTRY_ID + " = f.entry_id )";
    public static final String ALL_UNREAD_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_UNREAD + ")";
    public static final String ALL_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + ")";
    public static final String FAVORITES_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_FAVORITE  + ')';
    public static final String FAVORITES_UNREAD_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_FAVORITE + DB_AND + WHERE_UNREAD + ')';
    public static final String FAVORITES_READ_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_FAVORITE + DB_AND + WHERE_READ + ')';
    public static final String LAST_READ_UNREAD_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_LAST_READ + DB_AND + WHERE_UNREAD + ')';
    public static final String LAST_READ_READ_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_LAST_READ + DB_AND + WHERE_READ + ')';
    public static final String LAST_READ_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_LAST_READ  + ')';
    public static final String EXTERNAL_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + FEED_ID + "=" + GetExtrenalLinkFeedID() + ")";
    public static final String EXTERNAL_UNREAD_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_UNREAD + DB_AND + FEED_ID + "=" + GetExtrenalLinkFeedID() + ")";
    public static final String EXTERNAL_READ_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_READ + DB_AND + FEED_ID + "=" + GetExtrenalLinkFeedID() + ")";
    public static String ALL_IMAGESSIZE_NUMBER() {
        return PrefUtils.CALCULATE_IMAGES_SIZE() ? "(SELECT " + DB_SUM(IMAGES_SIZE) + " FROM " + EntryColumns.TABLE_NAME + ")" : "0";
    }
    public static String ALL_UNREAD_IMAGESSIZE_NUMBER() {
        return PrefUtils.CALCULATE_IMAGES_SIZE() ? "(SELECT " + DB_SUM(IMAGES_SIZE) + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_UNREAD + ")" : "0";
    }
    public static String FAVORITES_IMAGESSIZE_NUMBER() {
        return PrefUtils.CALCULATE_IMAGES_SIZE() ? "(SELECT " + DB_SUM(IMAGES_SIZE) + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_FAVORITE + ")" : "0";
    }
    public static String EXTERNAL_IMAGESSIZE_NUMBER() {
        return PrefUtils.CALCULATE_IMAGES_SIZE() ? "(SELECT " + DB_SUM(IMAGES_SIZE) + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + FEED_ID + "=" + GetExtrenalLinkFeedID() + ")" : "0";
    }
    private static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT";
    private static final String TYPE_EXTERNAL_ID = "INTEGER(7)";
    static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_TEXT_UNIQUE = "TEXT UNIQUE";
    static final String TYPE_DATE_TIME = "DATETIME";
    static final String TYPE_INT = "INT";
    static final String TYPE_BOOLEAN = "INTEGER(1)";

    public static ContentValues getReadContentValues() {
        ContentValues values = new ContentValues();
        values.put(EntryColumns.IS_READ, true);
        values.put(EntryColumns.IS_NEW, false);
        return values;
    }

    public static ContentValues getOldContentValues() {
        ContentValues values = new ContentValues();
        values.put(EntryColumns.IS_NEW, false);
        return values;
    }

    public static ContentValues getUnreadContentValues() {
        ContentValues values = new ContentValues();
        values.putNull(EntryColumns.IS_READ);
        return values;
    }
    public static ContentValues getUnstarContentValues() {
        ContentValues values = new ContentValues();
        values.putNull(EntryColumns.IS_FAVORITE);
        return values;
    }

    public static ContentValues getFavoriteContentValues( boolean favorite ) {
        ContentValues values = new ContentValues();
        if ( favorite )
            values.put(EntryColumns.IS_FAVORITE, 1);
        else
            values.putNull(EntryColumns.IS_FAVORITE);
        return values;
    }

    public static class FeedColumns implements BaseColumns {
        public static final String TABLE_NAME = "feeds";

        public static final String URL = "url";
        public static final String NAME = "name";
        public static final String IS_GROUP = "isgroup";
        public static final String GROUP_ID = "groupid";
        public static final String LAST_UPDATE = "lastupdate";
        public static final String REAL_LAST_UPDATE = "reallastupdate";
        public static final String RETRIEVE_FULLTEXT = "retrievefulltext";
        public static final String ICON_URL = "icon_url";
        public static final String ERROR = "error";
        public static final String PRIORITY = "priority";
        public static final String FETCH_MODE = "fetchmode";
        public static final String SHOW_TEXT_IN_ENTRY_LIST = "show_text_in_entry_list";
        public static final String IS_GROUP_EXPANDED = "is_group_expanded";
        public static final String IS_AUTO_REFRESH = "is_auto_refresh";
        public static final String IS_IMAGE_AUTO_LOAD = "is_image_auto_load";
        public static final String IMAGES_SIZE = "images_size";
        public static final String OPTIONS = "options";
        public static final String[] PROJECTION_ID = new String[]{FeedColumns._ID};
        public static final String[] PROJECTION_GROUP_ID = new String[]{FeedColumns.GROUP_ID};
        public static final String[] PROJECTION_PRIORITY = new String[]{FeedColumns.PRIORITY};

        public static String WHERE_GROUP = "(" + FeedColumns.IS_GROUP + DB_IS_TRUE + ")";

        public static Uri CONTENT_URI(String feedId) {
            return Uri.parse(CONTENT_AUTHORITY + "/feeds/" + feedId);
        }

        public static Uri CONTENT_URI(long feedId) {
            return Uri.parse(CONTENT_AUTHORITY + "/feeds/" + feedId);
        }

        public static final String[][] COLUMNS = new String[][]{{_ID, TYPE_PRIMARY_KEY}, {URL, TYPE_TEXT_UNIQUE}, {NAME, TYPE_TEXT}, {IS_GROUP, TYPE_BOOLEAN},
                {GROUP_ID, TYPE_EXTERNAL_ID}, {LAST_UPDATE, TYPE_DATE_TIME}, {REAL_LAST_UPDATE, TYPE_DATE_TIME}, {RETRIEVE_FULLTEXT, TYPE_BOOLEAN},
                {ICON_URL, "BLOB"}, {ERROR, TYPE_TEXT}, {PRIORITY, TYPE_INT}, {FETCH_MODE, TYPE_INT}, {SHOW_TEXT_IN_ENTRY_LIST, TYPE_BOOLEAN},
                {IS_GROUP_EXPANDED, TYPE_BOOLEAN}, {IS_AUTO_REFRESH, TYPE_BOOLEAN}, {IS_IMAGE_AUTO_LOAD, TYPE_BOOLEAN}, {IMAGES_SIZE, TYPE_INT},
                {OPTIONS, TYPE_TEXT}};

        public static Uri GROUPS_CONTENT_URI(String groupId) {
            return Uri.parse(CONTENT_AUTHORITY + "/groups/" + groupId);
        }

        public static Uri GROUPS_CONTENT_URI(long groupId) {
            return Uri.parse(CONTENT_AUTHORITY + "/groups/" + groupId);
        }

        public static Uri FEEDS_FOR_GROUPS_CONTENT_URI(String groupId) {
            return Uri.parse(CONTENT_AUTHORITY + "/groups/" + groupId + "/feeds");
        }

        public static Uri FEEDS_FOR_GROUPS_CONTENT_URI(long groupId) {
            return Uri.parse(CONTENT_AUTHORITY + "/groups/" + groupId + "/feeds");
        }

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/feeds");


        public static final Uri GROUPED_FEEDS_CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/grouped_feeds");

        public static final Uri GROUPS_CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/groups");

        public static final Uri GROUPS_AND_ROOT_CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/groups_and_root");
    }

    public static class FilterColumns implements BaseColumns {
        static final String TABLE_NAME = "filters";

        public static final String FEED_ID = "feedid";
        public static final String FILTER_TEXT = "filtertext";
        public static final String IS_REGEX = "isregex";
        public static final String APPLY_TYPE = "isappliedtotitle";
        public static final String IS_ACCEPT_RULE = "isacceptrule";
        public static final String IS_MARK_STARRED = "ismarkstarred";
        public static final String IS_REMOVE_TEXT = "isremovetext";

        public static final int DB_APPLIED_TO_CONTENT = 0;
        public static final int DB_APPLIED_TO_TITLE = 1;
        public static final int DB_APPLIED_TO_AUTHOR = 2;
        public static final int DB_APPLIED_TO_CATEGORY = 3;
        public static final int DB_APPLIED_TO_URL = 4;

        public static Uri FILTERS_FOR_FEED_CONTENT_URI(String feedId) {
            return Uri.parse(CONTENT_AUTHORITY + "/feeds/" + feedId + "/filters");
        }

        public static Uri FILTERS_FOR_FEED_CONTENT_URI(long feedId) {
            return Uri.parse(CONTENT_AUTHORITY + "/feeds/" + feedId + "/filters");
        }

        public static final String[][] COLUMNS = new String[][]{{_ID, TYPE_PRIMARY_KEY}, {FEED_ID, TYPE_EXTERNAL_ID}, {FILTER_TEXT, TYPE_TEXT},
                {IS_REGEX, TYPE_BOOLEAN}, {APPLY_TYPE, TYPE_BOOLEAN}, {IS_ACCEPT_RULE, TYPE_BOOLEAN}, {IS_MARK_STARRED, TYPE_BOOLEAN},
            {IS_REMOVE_TEXT, TYPE_BOOLEAN}};

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/filters");
    }

    public static class EntryColumns implements BaseColumns {
        public static final String TABLE_NAME = "entries";

        public static final String FEED_ID = "feedid";
        public static final String TITLE = "title";
        public static final String ABSTRACT = "abstract";
        public static final String MOBILIZED_HTML = "mobilized";
        public static final String DATE = "date";
        public static final String FETCH_DATE = "fetch_date";
        public static final String READ_DATE = "read_date";
        public static final String IS_READ = "isread";
        public static final String LINK = "link";
        public static final String IS_FAVORITE = "favorite";
        public static final String ENCLOSURE = "enclosure";
        public static final String GUID = "guid";
        public static final String AUTHOR = "author";
        public static final String IMAGE_URL = "image_url";
        public static final String SCROLL_POS = "scroll_pos";
        public static final String IS_NEW = "new";
        public static final String IS_WAS_AUTO_UNSTAR = "was_auto_unstar";
        public static final String IS_WITH_TABLES = "with_tables";
        public static final String IMAGES_SIZE = "images_size";
        public static final String CATEGORIES = "categories";

        public static final String CATEGORY_LIST_SEP = ";";

        private static final String MOB_LENGTH_EXPR( String fieldName ) {
            return String.format( "CASE WHEN length(%s) > 10 THEN length(%s) ELSE %s END", fieldName, fieldName, fieldName );
        }
        private static final String TEXT_LEN_EXPR = String.format( "CASE WHEN %s IS NULL THEN length(%s) ELSE %s END AS TEXT_LEN",
                EntryColumns.MOBILIZED_HTML, EntryColumns.ABSTRACT, MOB_LENGTH_EXPR( EntryColumns.MOBILIZED_HTML ) );
        public static final String[] PROJECTION_ID = new String[]{EntryColumns._ID, EntryColumns.LINK};
        public static final String[] PROJECTION_WITHOUT_TEXT =
                new String[]{EntryColumns._ID,
                             EntryColumns.AUTHOR,
                             EntryColumns.DATE,
                             FEED_ID,
                             EntryColumns.FETCH_DATE,
                             EntryColumns.GUID,
                             EntryColumns.IMAGE_URL,
                             EntryColumns.IS_FAVORITE,
                             EntryColumns.IS_READ,
                             EntryColumns.LINK,
                             EntryColumns.SCROLL_POS,
                             EntryColumns.IS_NEW,
                             EntryColumns.IS_WAS_AUTO_UNSTAR,
                             EntryColumns.IS_WITH_TABLES,
                             EntryColumns.IMAGES_SIZE,
                             EntryColumns.TITLE,
                             EntryColumns.DATE,
                             String.format( "substr( %s, 1, 10 ) AS %s", EntryColumns.MOBILIZED_HTML, EntryColumns.MOBILIZED_HTML ),
                             FeedColumns.NAME,
                             FeedColumns.OPTIONS,
                             TEXT_LEN_EXPR,
                             EntryColumns.READ_DATE };
        public static final String[] PROJECTION_WITH_TEXT =
                new String[]{EntryColumns._ID,
                        EntryColumns.AUTHOR,
                        EntryColumns.DATE,
                        FEED_ID,
                        EntryColumns.FETCH_DATE,
                        EntryColumns.GUID,
                        EntryColumns.IMAGE_URL,
                        EntryColumns.IS_FAVORITE,
                        EntryColumns.IS_READ,
                        EntryColumns.LINK,
                        EntryColumns.SCROLL_POS,
                        EntryColumns.IS_NEW,
                        EntryColumns.IS_WAS_AUTO_UNSTAR,
                        EntryColumns.IS_WITH_TABLES,
                        EntryColumns.IMAGES_SIZE,
                        EntryColumns.TITLE,
                        EntryColumns.DATE,
                        EntryColumns.ABSTRACT,
                        String.format( "substr( %s, 1, 10 ) AS %s", EntryColumns.MOBILIZED_HTML, EntryColumns.MOBILIZED_HTML ),
                        FeedColumns.NAME,
                        FeedColumns.OPTIONS,
                        TEXT_LEN_EXPR,
                        EntryColumns.CATEGORIES,
                        FeedColumns.IS_IMAGE_AUTO_LOAD,
                        EntryColumns.READ_DATE};
        public static final String WHERE_READ = IS_READ + DB_IS_TRUE;
        public static final String WHERE_UNREAD = "(" + IS_READ + DB_IS_NULL + DB_OR + IS_READ + DB_IS_FALSE + ')';
        public static final String WHERE_FAVORITE = "(" + IS_FAVORITE + DB_IS_TRUE + ')';
        public static final String WHERE_LAST_READ = "(" + READ_DATE + DB_IS_NOT_NULL + ')';
        public static final String WHERE_NOT_FAVORITE = "(" + IS_FAVORITE + DB_IS_NULL + DB_OR + IS_FAVORITE + DB_IS_FALSE + ')';
        public static final String WHERE_NEW = "(" + EntryColumns.IS_NEW + DB_IS_NULL + DB_OR + IS_NEW + DB_IS_TRUE  + ")";

        public static boolean IsNew( Cursor cursor, int fieldPos ) {
            return cursor.isNull( fieldPos ) || cursor.getInt( fieldPos ) == 1;
        }
        public static boolean IsRead( Cursor cursor, int fieldPos ) {
            return !cursor.isNull( fieldPos ) && cursor.getInt( fieldPos ) == 1;
        }
        public static Uri ENTRIES_FOR_FEED_CONTENT_URI(String feedId) {
            return Uri.parse(CONTENT_AUTHORITY + "/feeds/" + feedId + "/entries");
        }

        public static final String[][] COLUMNS = new String[][]{{_ID, TYPE_PRIMARY_KEY}, {FEED_ID, TYPE_EXTERNAL_ID}, {TITLE, TYPE_TEXT},
                {ABSTRACT, TYPE_TEXT}, {MOBILIZED_HTML, TYPE_TEXT}, {DATE, TYPE_DATE_TIME}, {FETCH_DATE, TYPE_DATE_TIME}, {IS_READ, TYPE_BOOLEAN}, {LINK, TYPE_TEXT},
                {IS_FAVORITE, TYPE_BOOLEAN}, {IS_NEW, TYPE_BOOLEAN}, {ENCLOSURE, TYPE_TEXT}, {GUID, TYPE_TEXT}, {AUTHOR, TYPE_TEXT},
                {IMAGE_URL, TYPE_TEXT}, {SCROLL_POS, TYPE_INT}, {IS_WAS_AUTO_UNSTAR, TYPE_BOOLEAN}, {IS_WITH_TABLES, TYPE_BOOLEAN},
                {IMAGES_SIZE, TYPE_INT}, {CATEGORIES, TYPE_TEXT},  {READ_DATE, TYPE_DATE_TIME} };

        public static Uri ENTRIES_FOR_FEED_CONTENT_URI(long feedId) {
            return Uri.parse(CONTENT_AUTHORITY + "/feeds/" + feedId + "/entries");
        }

        public static Uri ENTRIES_FOR_GROUP_CONTENT_URI(String groupId) {
            return Uri.parse(CONTENT_AUTHORITY + "/groups/" + groupId + "/entries");
        }

        public static Uri ENTRIES_FOR_GROUP_CONTENT_URI(long groupId) {
            return Uri.parse(CONTENT_AUTHORITY + "/groups/" + groupId + "/entries");
        }

        public static Uri CONTENT_URI(String entryId) {
            return Uri.parse(CONTENT_AUTHORITY + "/entries/" + entryId);
        }

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/entries");

        public static Uri CONTENT_URI(long entryId) {
            return Uri.parse(CONTENT_AUTHORITY + "/entries/" + entryId);
        }

        public static Uri PARENT_URI(String path) {
            return Uri.parse(CONTENT_AUTHORITY + path.substring(0, path.lastIndexOf('/')));
        }

        public static Uri SEARCH_URI(String search) {
            return Uri.parse(CONTENT_AUTHORITY + "/entries/search/" + (TextUtils.isEmpty(search) ? " " : Uri.encode(search))); // The space is mandatory here with empty search
        }

        public static final Uri UNREAD_ENTRIES_CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/unread_entries");

        public static final Uri FAVORITES_CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/favorites");
        public static final Uri LAST_READ_CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/last_read");

        public static boolean isSearchUri(Uri uri) {
            return uri != null && uri.toString().startsWith(CONTENT_AUTHORITY + "/entries/search/");
        }
    }

    public static class LabelColumns implements BaseColumns {
        public static final String TABLE_NAME = "label";
        public static final String NAME = "name";
        public static final String COLOR = "color";
        public static final String ORDER = "order_";
        public static final String[][] COLUMNS =
            new String[][]{
                {_ID, TYPE_PRIMARY_KEY},
                {NAME, TYPE_TEXT},
                {COLOR, TYPE_TEXT},
                {ORDER, TYPE_INT}
        };

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/labels");
        public static Uri CONTENT_URI(long ID) { return Uri.parse(CONTENT_AUTHORITY + "/labels/" + ID);}
    }

    public static class EntryLabelColumns {
        public static final String TABLE_NAME = "entrylabel";
        public static final String LABEL_ID = "label_id";
        public static final String ENTRY_ID = "entry_id";
        public static final String[][] COLUMNS = new String[][]{
            {LABEL_ID, TYPE_EXTERNAL_ID + " REFERENCES " + LabelColumns.TABLE_NAME + "(" + LabelColumns._ID + ") ON DELETE CASCADE"},
            {ENTRY_ID, TYPE_EXTERNAL_ID + " REFERENCES " + EntryColumns.TABLE_NAME + "(" + EntryColumns._ID + ") ON DELETE CASCADE" },
            {"UNIQUE", "(" + LABEL_ID + ", " + ENTRY_ID + ") "}};

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/entrylabels");
        public static Uri CONTENT_URI(long ID) { return Uri.parse(CONTENT_AUTHORITY + "/entrylabels/" + ID);}
        public static Uri CONTENT_URI(String ID) { return Uri.parse(CONTENT_AUTHORITY + "/entrylabels/" + ID);}

        public static final Uri WITH_ENTRIES_URI = Uri.parse(CONTENT_AUTHORITY + "/entrylabels/with_entries");

        public static final String UNREAD_NUMBER = "(SELECT " + DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " + WHERE_UNREAD + ")";

    }

    public static class TaskColumns implements BaseColumns {
        public static final String TABLE_NAME = "tasks";

        public static final String ENTRY_ID = "entryid";
        public static final String IMG_URL_TO_DL = "imgurl_to_dl";
        public static final String NUMBER_ATTEMPT = "number_attempt";
        public static final String[] PROJECTION_ID = new String[]{EntryColumns._ID};

        public static Uri CONTENT_URI(String taskId) {
            return Uri.parse(CONTENT_AUTHORITY + "/tasks/" + taskId);
        }

        public static Uri CONTENT_URI(long taskId) {
            return Uri.parse(CONTENT_AUTHORITY + "/tasks/" + taskId);
        }

        public static final String[][] COLUMNS = new String[][]{{_ID, TYPE_PRIMARY_KEY}, {ENTRY_ID, TYPE_EXTERNAL_ID}, {IMG_URL_TO_DL, TYPE_TEXT},
                {NUMBER_ATTEMPT, TYPE_INT}, {"UNIQUE", "(" + ENTRY_ID + ", " + IMG_URL_TO_DL + ") ON CONFLICT IGNORE"}};

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY + "/tasks");

        public static final String TEXT_COUNT = "(SELECT " + DB_COUNT + " FROM " + TaskColumns.TABLE_NAME + " WHERE " + IMG_URL_TO_DL + DB_IS_NULL + ")";
        public static final String IMAGE_COUNT = "(SELECT " + DB_COUNT + " FROM " + TaskColumns.TABLE_NAME + " WHERE " + IMG_URL_TO_DL + DB_IS_NOT_NULL + ")";


    }

    public static String getWhereNotExternal() {
        return Constants.DB_AND + "(" + FeedColumns.FETCH_MODE + "<>" + FetcherService.FETCHMODE_EXERNAL_LINK + DB_OR + FeedColumns.FETCH_MODE + DB_IS_NULL + ")";
    }

}
