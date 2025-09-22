/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
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

package ru.yanus171.feedexfork;

import android.app.NotificationManager;
import android.content.Context;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

public final class Constants {

  public static final NotificationManager NOTIF_MGR = (NotificationManager) MainApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

  public static final String INTENT_FROM_WIDGET = "fromWidget";

  public static final String FEED_ID = "feedid";
  public static final String GROUP_ID = "groupid";

  public static final String DB_COUNT = "COUNT(*)";
  public static final String DB_COUNT(String fieldName) { return "COUNT(" + fieldName + ")"; }
  public static final String DB_IS_TRUE = "=1";
  public static final String DB_IS_FALSE = "=0";
  public static final String DB_IS_NULL = " IS NULL";
  public static final String DB_IS_NOT_NULL = " IS NOT NULL";
  public static final String DB_DESC = " DESC";
  public static final String DB_ASC = " ASC";
  public static final String DB_ARG = "=?";
  public static final String DB_AND = " AND ";
  public static final String DB_OR = " OR ";
  public static final String EMPTY_WHERE_SQL = "(1 = 1)";
  public static String DB_SUM( String fieldName ) { return " SUM(" + fieldName + ") "; }

  public static final String HTTP_SCHEME = "http://";
  public static final String HTTPS_SCHEME = "https://";
  public static final String FILE_SCHEME = "file://";
  public static final String CONTENT_SCHEME = "content://";

  public static final String HTML_LT = "&lt;";
  public static final String HTML_GT = "&gt;";
  public static final String LT = "<";
  public static final String GT = ">";

  public static final String TRUE = "true";
  public static final String FALSE = "false";

  public static final String ENCLOSURE_SEPARATOR = "[@]"; // exactly three characters!

  public static final String HTML_QUOT = "&quot;";
  public static final String QUOT = "\"";
  public static final String HTML_APOSTROPHE = "&#39;";
  public static final String HTML_APOS = "&apos;";
  public static final String APOSTROPHE = "'";
  public static final String AMP = "&";
  public static final String AMP_SG = "&amp;";
  public static final String SLASH = "/";
  public static final String COMMA_SPACE = ", ";

  public static final String UTF8 = "UTF-8";

  public static final String FROM_AUTO_REFRESH = "from_auto_refresh";
  public static final String FROM_AUTO_BACKUP = "from_auto_backup";
  public static final String FROM_DELETE_OLD = "from_delete_old";
  public static final String FROM_RELOAD_ALL_TEXT = "reload_all_texts";
  public static final String FROM_IMPORT = "from_import";
  public static final String URL_LIST = "url_list";
  //public static final String OPEN_ACTIVITY = "open_activity";
  public static final String URL_TO_LOAD = "url_to_load";
  public static final String TITLE_TO_LOAD = "title_to_load";


  public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
  public static final String MIMETYPE_PDF = "application/pdf";

  public static final int UPDATE_THROTTLE_DELAY = 200;

  public static final String FETCH_PICTURE_MODE_WIFI_ONLY_PRELOAD = "WIFI_ONLY_PRELOAD";
  public static final String FETCH_PICTURE_MODE_ALWAYS_PRELOAD = "ALWAYS_PRELOAD";

  public static final MatrixCursor EMPTY_CURSOR = new MatrixCursor(new String[]{BaseColumns._ID});

  public static final int NOTIFICATION_ID_READING_SERVICE = -4;
  public static final int NOTIFICATION_ID_REFRESH_SERVICE = -3;
  public static final int NOTIFICATION_ID_MANY_ITEMS_MARKED_STARRED = -1;
  public static final int NOTIFICATION_ID_NEW_ITEMS_COUNT = -2;
  public static final long MILLS_IN_DAY = 1000 * 60 * 60 * 24;
  public static final long MILLS_IN_SECOND = 1000;

  public static final int VIBRATE_DURATION = 25;

  public static final String EXTRA_FILENAME = "FileName";
  public static final String EXTRA_URI = "Uri";
  public static final String EXTRA_LINK = "Link";
  public static final String EXTRA_ID = "ID";
  public static final String CALCULATE_IMAGE_SIZES = "CalculateImageSizes";
}
