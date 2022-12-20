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

package ru.yanus171.feedexfork.parser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Xml;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.EntryLabelColumns;
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns;
import ru.yanus171.feedexfork.provider.FeedData.LabelColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.Label;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.utils.WaitDialog;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.util.Xml.Encoding.UTF_8;
import static androidx.core.content.FileProvider.getUriForFile;
import static ru.yanus171.feedexfork.Constants.FALSE;
import static ru.yanus171.feedexfork.Constants.TRUE;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.FETCH_MODE;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.GROUPS_AND_ROOT_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.GROUPS_CONTENT_URI;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.GROUP_ID;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.IS_AUTO_REFRESH;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.IS_GROUP;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.IS_IMAGE_AUTO_LOAD;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.LAST_UPDATE;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.NAME;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.OPTIONS;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.PRIORITY;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.REAL_LAST_UPDATE;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.RETRIEVE_FULLTEXT;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.SHOW_TEXT_IN_ENTRY_LIST;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.URL;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns._ID;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.URI_ENTRIES_FOR_FEED;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.notifyChangeOnAllUris;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.isCancelRefresh;
import static ru.yanus171.feedexfork.service.FetcherService.isNotCancelRefresh;

public class OPML {

    public static final String AUTO_BACKUP_OPML_FILENAME = "HandyNewsReader_auto.backup";
    public static final int REQUEST_PICK_OPML_FILE = 1;
    private static final int PERMISSIONS_REQUEST_IMPORT_FROM_OPML = 1;
    private static final int PERMISSIONS_REQUEST_EXPORT_TO_OPML = 2;
    private static final int PERMISSIONS_REQUEST_BACKUP = 3;
    public static final String EXTRA_REMOVE_EXISTING_FEEDS_BEFORE_IMPORT = "EXTRA_REMOVE_EXISTING_FEEDS_BEFORE_IMPORT";
    public static final String FILENAME_DATETIME_FORMAT = "yyyyMMdd_HHmmss";

    public static String GetAutoBackupOPMLFileName() { return getContext().getCacheDir().getAbsolutePath() + "/" + AUTO_BACKUP_OPML_FILENAME; }

    private static final String START = "<?xml version='1.0' encoding='utf-8'?>\n<opml version='1.1'>\n<head>\n<title>Handy Reading export</title>\n<dateCreated>";
    private static final String AFTER_DATE = "</dateCreated>\n</head>\n<body>\n";
    private static final String OUTLINE_TITLE = "\t<outline title='";
    private static final String OUTLINE_XMLURL = "' type='rss' xmlUrl='";
    private static final String OUTLINE_RETRIEVE_FULLTEXT = "' retrieveFullText='";
    private static final String CLOSING = "'/>\n";
    private static final String CLOSING_TEMP = "'>\n";
    private static final String OUTLINE_END = "\t</outline>\n";

    private static final String FILTER_TEXT = "\t\t<filter text='";
    private static final String FILTER_IS_REGEX = "' isRegex='";
    private static final String FILTER_IS_APPLIED_TO_TITLE = "' isAppliedToTitle='";
    private static final String FILTER_IS_ACCEPT_RULE = "' isAcceptRule='";
    private static final String FILTER_IS_MARK_AS_STARRED = "' isMarkAsStarred='";
    private static final String FILTER_IS_REMOVE_TEXT = "' isRemoveText='";

    private static final String LABEL_NAME = "<label name='";
    private static final String LABEL_COLOR = "' color='";
    private static final String LABEL_CLOSING = "</label>\n";

    private static final String LABEL_ENTRY_ID = "<entry entry_id='";

    private static final String TAG_ENTRY = "entry";

    private static final String TAG_LABEL = "label";

    private static final String TAG_LABEL_ENTRY = "entry";

    private static final String TAG_START = "\t\t<%s %s='";
    private static final String ATTR_VALUE = "' %s='";

    private static final String TAG_PREF = "pref";
    private static final String ATTR_PREF_CLASSNAME = "classname";
    private static final String ATTR_PREF_VALUE = "value";
    private static final String ATTR_PREF_KEY = "key";

    private static final String END_CLOSING = "</body>\n</opml>\n";

    //private static final OPMLParser mParser = new OPMLParser();
    private static Boolean mAutoBackupEnabled = true;

    public static
    void importFromFile(String filename, boolean isRemoveExistingFeeds) throws IOException, SAXException {
        importFromFile(new FileInputStream(filename), isRemoveExistingFeeds);
    }
    public static
    void importFromFile(Uri fileUri, boolean isRemoveExistingFeeds) throws IOException, SAXException {
        importFromFile( getContext().getContentResolver().openInputStream(fileUri), isRemoveExistingFeeds);
    }

    private static void importFromFile(InputStream input, boolean isRemoveExistingFeeds) throws IOException, SAXException {
        SetAutoBackupEnabled(false); // Do not write the auto backup file while reading it...
        try {
            ContentResolver cr = getContext().getContentResolver();

            if ( isRemoveExistingFeeds ) {
                cr.delete(FeedData.FeedColumns.CONTENT_URI, _ID + "<> " + FetcherService.GetExtrenalLinkFeedID(), null);
                cr.delete(LabelColumns.CONTENT_URI, null, null);
            }
            EntryUrlVoc.INSTANCE.reinit( false );
            LabelVoc.INSTANCE.reinit( false );

            final OPMLParser parser = new OPMLParser();
            //Xml.parse(FetcherService.ToString(input, UTF_8), parser);
            Xml.parse(input, UTF_8, parser);

            EntryUrlVoc.INSTANCE.reinit( false );
            LabelVoc.INSTANCE.reinit( false );
            notifyChangeOnAllUris( URI_ENTRIES_FOR_FEED, null );
            //MainApplication.mHTMLFileVoc.init1();
        } finally {
            SetAutoBackupEnabled(true);
        }
        UiUtils.RunOnGuiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText( getContext(), R.string.import_completed, Toast.LENGTH_LONG ).show();
            }
        });
    }

    private static void SetAutoBackupEnabled(boolean b) {
        synchronized (mAutoBackupEnabled) {
            mAutoBackupEnabled = b;
        }
    }

    private static boolean IsAutoBackupEnabled() {
        synchronized (mAutoBackupEnabled) {
            return mAutoBackupEnabled;
        }
    }

    public static void exportToFile(String filename, boolean isBackup) throws IOException {

        if ( GetAutoBackupOPMLFileName().equals(filename) && !IsAutoBackupEnabled() )
            return;


        final Context context = getContext();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        //final int status = FetcherService.Status().Start( context.getString( R.string.exportingToFile ) );
        try {
            Cursor cursorGroupsAndRoot = context.getContentResolver()
                    .query(GROUPS_AND_ROOT_CONTENT_URI, FEEDS_PROJECTION, null, null, null);

            writer.write( START );
            writer.write( String.valueOf( System.currentTimeMillis() ) );
            writer.write( AFTER_DATE);

            if ( isBackup ) {
                SaveSettings( writer, "\t\t");
                Cursor cursor = context.getContentResolver().query(CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() ), FEEDS_PROJECTION, null, null, null);
                if ( cursor.moveToFirst() && isNotCancelRefresh() )
                    ExportFeed(writer, cursor, true);
                cursor.close();
            }

            while (cursorGroupsAndRoot.moveToNext()) {
                if ( isCancelRefresh() )
                    break;
                if (cursorGroupsAndRoot.getInt(1) == 1) { // If it is a group
                    writer.write( OUTLINE_TITLE);
                    writer.write( cursorGroupsAndRoot.isNull(2) ? "" : TextUtils.htmlEncode(cursorGroupsAndRoot.getString(2)));
                    WriteLongValue(writer, cursorGroupsAndRoot, PRIORITY, 11);
                    writer.write( CLOSING_TEMP);
                    Cursor cursorFeeds = context.getContentResolver()
                            .query(FEEDS_FOR_GROUPS_CONTENT_URI(cursorGroupsAndRoot.getString(0)), FEEDS_PROJECTION, null, null, null);
                    while (cursorFeeds.moveToNext()) {
                        ExportFeed(writer, cursorFeeds, isBackup);
                    }
                    cursorFeeds.close();

                    writer.write( OUTLINE_END);
                } else
                    ExportFeed(writer, cursorGroupsAndRoot, isBackup);
            }

            if ( isBackup )
                for ( Label label: LabelVoc.INSTANCE.getList() )
                    Export( writer, label );

            cursorGroupsAndRoot.close();
            writer.write( END_CLOSING );
        } finally {
            writer.close();
        }
    }

    private static void Export(Writer writer, Label label) throws IOException {
        writer.write(LABEL_NAME);
        writer.write(TextUtils.htmlEncode(label.mName));
        writer.write(LABEL_COLOR);
        writer.write(label.getMColor());
        writer.write(CLOSING_TEMP);
        for ( Long entryID: LabelVoc.INSTANCE.getLabelEntriesID( label.mID ) ) {
            writer.write("\t");
            writer.write(LABEL_ENTRY_ID);
            writer.write(String.valueOf(entryID));
            writer.write(CLOSING);
        }

        writer.write(LABEL_CLOSING);
    }

    private static String GetLong( final Cursor cursor, final int col ) {
        return cursor.isNull( col ) ? "0" : cursor.getString(col );
    }
    private static String GetEncoded( final Cursor cursor, final int col ) {
        return cursor.isNull( col ) ? "" : TextUtils.htmlEncode(cursor.getString(col ) );
    }
    private static String GetText( Attributes attr, String attrName  ) {
        return attr.getValue( attrName );
    }


    private static final String[] FEEDS_PROJECTION = new String[]{_ID, IS_GROUP, NAME,
            URL, RETRIEVE_FULLTEXT, SHOW_TEXT_IN_ENTRY_LIST, IS_AUTO_REFRESH,
            IS_IMAGE_AUTO_LOAD, OPTIONS, LAST_UPDATE, REAL_LAST_UPDATE,
            PRIORITY, FETCH_MODE };

    private static void ExportFeed(Writer writer, Cursor cursor, boolean isBackup) throws IOException {
        final String feedID = cursor.getString(0);
        writer.write( "\t");
        writer.write( OUTLINE_TITLE );
        writer.write(GetEncoded( cursor ,2) );
        writer.write(OUTLINE_XMLURL);
        writer.write(GetEncoded( cursor, 3));
        if ( isBackup ) {
            writer.write(OUTLINE_RETRIEVE_FULLTEXT);
            writer.write(GetBoolText(cursor, 4));
            WriteBoolValue(writer, cursor, SHOW_TEXT_IN_ENTRY_LIST, 5);
            WriteBoolValue(writer, cursor, IS_AUTO_REFRESH, 6);
            WriteBoolValue(writer, cursor, IS_IMAGE_AUTO_LOAD, 7);
            WriteEncodedText(writer, cursor, OPTIONS, 8);
            WriteLongValue(writer, cursor, LAST_UPDATE, 9);
            WriteLongValue(writer, cursor, REAL_LAST_UPDATE, 10);
            WriteLongValue(writer, cursor, PRIORITY, 11);
            WriteLongValue(writer, cursor, FETCH_MODE, 12);
        }
        writer.write(CLOSING_TEMP);

        if ( isBackup ) {
            ExportFilters(writer, feedID);
            final boolean saveAbstract = !TRUE.equals(GetBoolText(cursor, 4));
            ExportEntries(writer, feedID, saveAbstract);
        }
        writer.write(OUTLINE_END);
    }



    private static String GetBoolText(Cursor cur, int col) {
        return cur.getInt(col) == 1 ? TRUE : FALSE;
    }
    private static boolean GetBool(Attributes attr, String attrName) {
        return TRUE.equals( attr.getValue( "", attrName ) );
    }

    private static final String[] ENTRIES_PROJECTION = new String[]{EntryColumns.TITLE, EntryColumns.LINK,
            EntryColumns.IS_NEW, EntryColumns.IS_READ, EntryColumns.SCROLL_POS, EntryColumns.ABSTRACT,
            EntryColumns.AUTHOR, EntryColumns.DATE, EntryColumns.FETCH_DATE, EntryColumns.IMAGE_URL,
            EntryColumns.IS_FAVORITE, EntryColumns._ID, EntryColumns.GUID, EntryColumns.IS_WAS_AUTO_UNSTAR,
            EntryColumns.IS_WITH_TABLES, EntryColumns.READ_DATE, EntryColumns.IS_LANDSCAPE };

//    private static String GetMobilizedText(long entryID ) {
//        String result = "";
//        try {
//            Cursor cur = MainApplication.getContext().getContentResolver()
//                    .query( EntryColumns.CONTENT_URI( entryID ), new String[]{ EntryColumns.MOBILIZED_HTML }, null, null, null );
//            if ( cur.moveToFirst() )
//                result = cur.getString( 0 );
//        } catch (  Exception ignored ) {
//            ignored.printStackTrace();
//        }
//        return result;
//    }
    private static void ExportEntries(Writer writer, String feedID, boolean saveAbstract) throws IOException {
        Cursor cur = getContext().getContentResolver()
                .query(ENTRIES_FOR_FEED_CONTENT_URI( feedID ), ENTRIES_PROJECTION, null, null, null);
        if (cur != null && cur.getCount() != 0) {
            while (cur.moveToNext()) {
                writer.write("\t");
                writer.write(String.format( TAG_START, TAG_ENTRY, EntryColumns.TITLE) );
                writer.write(cur.isNull( 0 ) ? "" : TextUtils.htmlEncode(cur.getString(0)));
                WriteEncodedText(writer, cur, EntryColumns.LINK, 1);
                WriteBoolValue(writer, cur, EntryColumns.IS_NEW, 2);
                WriteBoolValue(writer, cur, EntryColumns.IS_READ, 3);
                WriteText(writer, cur, EntryColumns.SCROLL_POS, 4);
                if ( saveAbstract ) {
                    WriteEncodedText(writer, cur, EntryColumns.ABSTRACT, 5);
                }
                WriteEncodedText(writer, cur, EntryColumns.AUTHOR, 6);
                WriteText(writer, cur, EntryColumns.DATE, 7);
                WriteText(writer, cur, EntryColumns.FETCH_DATE, 8);
                WriteEncodedText(writer, cur, EntryColumns.IMAGE_URL, 9);
                WriteBoolValue(writer, cur, EntryColumns.IS_FAVORITE, 10);
                WriteLongValue(writer, cur, EntryColumns._ID, 11);
                //                if ( !saveAbstract && cur.getInt(10) == 1 ) {
//                    //writer.write(String.format(ATTR_VALUE, EntryColumns.MOBILIZED_HTML));
//                    long entryID  = cur.getLong(11 );
//                    final String text = GetMobilizedText( entryID );
//                    writer.write(text == null ? "" : TextUtils.htmlEncode(text));
//                }
                WriteEncodedText(writer, cur, EntryColumns.GUID, 12);
                WriteBoolValue(writer, cur, EntryColumns.IS_WAS_AUTO_UNSTAR, 13);
                WriteBoolValue(writer, cur, EntryColumns.IS_WITH_TABLES, 14);
                WriteText(writer, cur, EntryColumns.READ_DATE, 15);
                WriteBoolValue(writer, cur, EntryColumns.IS_LANDSCAPE, 16);
                writer.write(CLOSING);
            }
            writer.write("\t");
        }
        cur.close();
    }

    private static void WriteText(Writer writer, Cursor cur, String fieldName, int col) throws IOException {
        writer.write(String.format(ATTR_VALUE, fieldName));
        writer.write(cur.isNull(col) ? "" : cur.getString(col));
    }

    private static void WriteEncodedText(Writer writer, Cursor cur, String fieldName, int col) throws IOException {
        writer.write(String.format(ATTR_VALUE, fieldName));
        writer.write(cur.isNull(col) ? "" : TextUtils.htmlEncode(cur.getString(col)));
    }

    private static void WriteBoolValue(Writer writer, Cursor cur, String fieldName, int col) throws IOException {
        writer.write(String.format( ATTR_VALUE, fieldName) );
        writer.write(GetBoolText( cur, col));
    }
    private static void WriteLongValue(Writer writer, Cursor cursor, String fieldName, int col) throws IOException {
        writer.write(String.format(ATTR_VALUE, fieldName));
        writer.write(GetLong(cursor, col));
    }

    private static final String[] FILTERS_PROJECTION = new String[]{FilterColumns.FILTER_TEXT, FilterColumns.IS_REGEX,
            FilterColumns.APPLY_TYPE, FilterColumns.IS_ACCEPT_RULE, FilterColumns.IS_MARK_STARRED, FilterColumns.IS_REMOVE_TEXT,};

    private static void ExportFilters(Writer writer, String feedID) throws IOException {
        Cursor cur = getContext().getContentResolver()
                .query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedID), FILTERS_PROJECTION, null, null, null);
        if (cur.getCount() != 0) {
            while (cur.moveToNext()) {
                writer.write("\t");
                writer.write(FILTER_TEXT);
                writer.write(TextUtils.htmlEncode(cur.getString(0)));
                writer.write(FILTER_IS_REGEX);
                writer.write(GetBoolText( cur, 1));
                writer.write(FILTER_IS_APPLIED_TO_TITLE);
                writer.write(cur.getString( 2 ));
                writer.write(FILTER_IS_ACCEPT_RULE);
                writer.write(GetBoolText( cur, 3) );
                writer.write(FILTER_IS_MARK_AS_STARRED);
                writer.write(GetBoolText( cur, 4) );
                writer.write(FILTER_IS_REMOVE_TEXT);
                writer.write(GetBoolText( cur, 5) );
                writer.write(CLOSING);
            }
            writer.write("\t");
        }
        cur.close();
    }

    private static final String PREF_CLASS_FLOAT = "Float";
    private static final String PREF_CLASS_LONG = "Long";
    private static final String PREF_CLASS_INTEGER = "Integer";
    private static final String PREF_CLASS_BOOLEAN = "Boolean";
    private static final String PREF_CLASS_STRING = "String";

    private static void SaveSettings(Writer writer, final String prefix) throws IOException {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (final Map.Entry<String, ?> entry : settings.getAll().entrySet()) {
            String prefClass = entry.getValue().getClass().getName();
            String prefValue;
            if (prefClass.contains(PREF_CLASS_STRING)) {
                prefClass = PREF_CLASS_STRING;
                prefValue = ((String) entry.getValue()).replace("\n", "\\n");
            } else if (prefClass.contains(PREF_CLASS_BOOLEAN)) {
                prefClass = PREF_CLASS_BOOLEAN;
                prefValue = String.valueOf(entry.getValue());
            } else if (prefClass.contains(PREF_CLASS_INTEGER)) {
                prefClass = PREF_CLASS_INTEGER;
                prefValue = String.valueOf(entry.getValue());
            } else if (prefClass.contains(PREF_CLASS_LONG)) {
                prefClass = PREF_CLASS_LONG;
                prefValue = String.valueOf(entry.getValue());
            } else if (prefClass.contains(PREF_CLASS_FLOAT)) {
                prefClass = PREF_CLASS_FLOAT;
                prefValue = String.valueOf(entry.getValue());
            } else
                continue;
            writer.write( prefix + String.format( "<%s %s='%s' %s='%s' %s='%s'/>\n", TAG_PREF,
                    ATTR_PREF_CLASSNAME, prefClass,
                    ATTR_PREF_KEY, entry.getKey(),
                    ATTR_PREF_VALUE, TextUtils.htmlEncode( prefValue ) ) );
        }

    }

    private static class OPMLParser extends DefaultHandler {
        private static final String TAG_BODY = "body";
        private static final String TAG_OUTLINE = "outline";
        private static final String ATTRIBUTE_TITLE = "title";
        private static final String ATTRIBUTE_XMLURL = "xmlUrl";
        private static final String ATTRIBUTE_RETRIEVE_FULLTEXT = "retrieveFullText";
        private static final String TAG_FILTER = "filter";
        private static final String ATTRIBUTE_TEXT = "text";
        private static final String ATTRIBUTE_IS_REGEX = "isRegex";
        private static final String ATTRIBUTE_IS_APPLIED_TO_TITLE = "isAppliedToTitle";
        private static final String ATTRIBUTE_IS_ACCEPT_RULE = "isAcceptRule";
        private static final String ATTRIBUTE_IS_MARK_AS_STARRED = "isMarkAsStarred";
        private static final String ATTRIBUTE_IS_REMOVE_TEXT = "isRemoveText";
        private final SharedPreferences.Editor mEditor;

        private boolean mBodyTagEntered = false;
        private boolean mFeedEntered = false;
        private boolean mProbablyValidElement = false;
        private String mGroupId = null;
        private String mFeedId = null;
        private Long mLabelID = null;
        private HashMap<Long, Long> mEntryFileIDToIDVoc = new HashMap<Long, Long>();
        private Integer mLabelOrder = 0;

        @SuppressLint("CommitPrefEdits")
        OPMLParser() {
            mEditor = PreferenceManager.getDefaultSharedPreferences( getContext() ).edit();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            ContentResolver cr = getContext().getContentResolver();
            if (!mBodyTagEntered) {
                if (TAG_BODY.equals(localName)) {
                    mBodyTagEntered = true;
                    mProbablyValidElement = true;
                }
            } else if (TAG_OUTLINE.equals(localName)) {
                String url = attributes.getValue("", ATTRIBUTE_XMLURL);
                String title = attributes.getValue("", ATTRIBUTE_TITLE);
                if(title == null) {
                    title = attributes.getValue("", ATTRIBUTE_TEXT);
                }

                if (url == null) { // No url => this is a group
                    if (title != null) {
                        ContentValues values = new ContentValues();
                        values.put(IS_GROUP, true);
                        values.put(NAME, title);
                        values.put(PRIORITY, GetText( attributes, PRIORITY));

                        Cursor cursor = cr.query(GROUPS_CONTENT_URI, null, NAME + Constants.DB_ARG, new String[]{title}, null);

                        if (!cursor.moveToFirst()) {
                            mGroupId = cr.insert(GROUPS_CONTENT_URI, values).getLastPathSegment();
                        }
                        cursor.close();
                    }

                } else { // Url => this is a feed
                    mFeedEntered = true;
                    ContentValues values = new ContentValues();

                    values.put(URL, url);
                    values.put(NAME, title != null && title.length() > 0 ? title : null);
                    if (mGroupId != null) {
                        values.put(GROUP_ID, mGroupId);
                    }

                    values.put(RETRIEVE_FULLTEXT, GetBool( attributes, ATTRIBUTE_RETRIEVE_FULLTEXT));
                    values.put(SHOW_TEXT_IN_ENTRY_LIST, GetBool( attributes, SHOW_TEXT_IN_ENTRY_LIST));
                    values.put(IS_AUTO_REFRESH, GetBool( attributes, IS_AUTO_REFRESH));
                    values.put(IS_IMAGE_AUTO_LOAD, GetBool( attributes, IS_IMAGE_AUTO_LOAD));
                    values.put(OPTIONS, GetText( attributes, OPTIONS));
                    values.put(LAST_UPDATE, GetText( attributes, LAST_UPDATE));
                    values.put(REAL_LAST_UPDATE, GetText( attributes, REAL_LAST_UPDATE));
                    values.put(PRIORITY, GetText( attributes, PRIORITY));
                    values.put(FETCH_MODE, GetText( attributes, FETCH_MODE));

                    if ( String.valueOf( FetcherService.FETCHMODE_EXERNAL_LINK ).equals( attributes.getValue( FETCH_MODE ) ) )
                        mFeedId = FetcherService.GetExtrenalLinkFeedID();
                    else {
                        Cursor cursor = cr.query(CONTENT_URI, null, URL + Constants.DB_ARG,
                                new String[]{url}, null);
                        mFeedId = null;
                        if (!cursor.moveToFirst()) {
                            mFeedId = cr.insert(CONTENT_URI, values).getLastPathSegment();
                        }
                        cursor.close();
                    }
                }
            } else if (TAG_FILTER.equals(localName)) {
                if (mFeedEntered && mFeedId != null) {
                    ContentValues values = new ContentValues();
                    values.put(FilterColumns.FILTER_TEXT, attributes.getValue("", ATTRIBUTE_TEXT));
                    values.put(FilterColumns.IS_REGEX, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_REGEX)));
                    {
                        final String text = attributes.getValue("", ATTRIBUTE_IS_APPLIED_TO_TITLE);
                        final int value;
                        if ( text.equals( "true" ) )
                            value = DB_APPLIED_TO_TITLE;
                        else if ( text.equals( "false" ) )
                            value = DB_APPLIED_TO_CONTENT;
                        else
                            value = Integer.parseInt( text );
                        values.put(FilterColumns.APPLY_TYPE, value );
                    }
                    values.put(FilterColumns.IS_ACCEPT_RULE, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_ACCEPT_RULE)));
                    values.put(FilterColumns.IS_MARK_STARRED, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_MARK_AS_STARRED)));
                    values.put(FilterColumns.IS_REMOVE_TEXT, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_REMOVE_TEXT)));

                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(mFeedId), values);
                }
            } else if (TAG_ENTRY.equals(localName) && mFeedEntered && mFeedId != null) {
                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_NEW, GetBool( attributes, EntryColumns.IS_NEW));
                values.put(EntryColumns.IS_READ, GetBool( attributes, EntryColumns.IS_READ));
                values.put(EntryColumns.IS_FAVORITE, GetBool( attributes, EntryColumns.IS_FAVORITE));
                values.put(EntryColumns.ABSTRACT, GetText( attributes, EntryColumns.ABSTRACT));
                final String link = GetText( attributes, EntryColumns.LINK );
                final String mobHtml = GetText( attributes, EntryColumns.MOBILIZED_HTML);
                FileUtils.INSTANCE.saveMobilizedHTML( link, mobHtml, values );
                values.put(EntryColumns.FETCH_DATE, GetText( attributes, EntryColumns.FETCH_DATE));
                values.put(EntryColumns.READ_DATE, GetText( attributes, EntryColumns.READ_DATE));
                values.put(EntryColumns.DATE, GetText( attributes, EntryColumns.DATE));
                values.put(EntryColumns.TITLE, GetText( attributes, EntryColumns.TITLE));
                values.put(EntryColumns.SCROLL_POS, GetText( attributes, EntryColumns.SCROLL_POS ));
                values.put(EntryColumns.AUTHOR, GetText( attributes, EntryColumns.AUTHOR) );
                values.put(EntryColumns.IMAGE_URL, GetText( attributes, EntryColumns.IMAGE_URL));
                values.put(EntryColumns.GUID, GetText( attributes, EntryColumns.GUID));
                values.put(EntryColumns.IS_WAS_AUTO_UNSTAR, GetText( attributes, EntryColumns.IS_WAS_AUTO_UNSTAR));
                values.put(EntryColumns.IS_WITH_TABLES, GetText( attributes, EntryColumns.IS_WITH_TABLES));
                values.put(EntryColumns.IS_LANDSCAPE, GetText( attributes, EntryColumns.IS_LANDSCAPE));

                final Long id = Long.valueOf(cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(mFeedId ), values).getLastPathSegment());
                if ( attributes.getIndex(_ID) != -1 )
                    mEntryFileIDToIDVoc.put(Long.valueOf(GetText(attributes, _ID)), id);
            } else if (TAG_LABEL.equals(localName)) {
                mLabelOrder++;
                ContentValues values = new ContentValues();
                values.put(LabelColumns.NAME, GetText( attributes, LabelColumns.NAME));
                values.put(LabelColumns.COLOR, GetText( attributes, LabelColumns.COLOR));
                values.put(LabelColumns.ORDER, mLabelOrder);
                Uri newUri = cr.insert(LabelColumns.CONTENT_URI, values);
                mLabelID = newUri.getPathSegments().size() == 2 ? Long.valueOf( newUri.getLastPathSegment() ) : null;
                LabelVoc.INSTANCE.addLabel( values.getAsString(LabelColumns.NAME), mLabelID, values.getAsString(LabelColumns.COLOR) );
            } else if (TAG_LABEL_ENTRY.equals(localName) && mLabelID != null ) {
                ContentValues values = new ContentValues();
                values.put(EntryLabelColumns.ENTRY_ID, mEntryFileIDToIDVoc.get(Long.parseLong(GetText(attributes, EntryLabelColumns.ENTRY_ID))));
                values.put(EntryLabelColumns.LABEL_ID, mLabelID);
                cr.insert(EntryLabelColumns.CONTENT_URI, values);
            } else if (TAG_PREF.equals(localName)) {
                final String className = attributes.getValue( ATTR_PREF_CLASSNAME );
                final String value = attributes.getValue( ATTR_PREF_VALUE);
                final String key = attributes.getValue( ATTR_PREF_KEY );
                if (className.contains(PREF_CLASS_STRING)) {
                    mEditor.putString(key, value.replace("\\n", "\n"));
                } else if (className.contains(PREF_CLASS_BOOLEAN)) {
                    mEditor.putBoolean(key, Boolean.parseBoolean(value));
                } else if (className.contains(PREF_CLASS_INTEGER)) {
                    mEditor.putInt(key, Integer.parseInt(value));
                } else if (className.contains(PREF_CLASS_LONG)) {
                    mEditor.putLong(key, Long.parseLong(value));
                } else if (className.contains(PREF_CLASS_FLOAT)) {
                    mEditor.putFloat(key, Float.parseFloat(value));
                }
                mEditor.apply();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (mBodyTagEntered && TAG_BODY.equals(localName)) {
                mBodyTagEntered = false;
            } else if (TAG_OUTLINE.equals(localName)) {
                if (mFeedEntered) {
                    mFeedEntered = false;
                } else {
                    mGroupId = null;
                }
            } else if (TAG_LABEL.equals(localName)) {
                mLabelID = null;
            }
        }

        @Override
        public void warning(SAXParseException e) {
            // ignore warnings
        }

        @Override
        public void error(SAXParseException e) {
            // ignore small errors
        }

        @Override
        public void endDocument() throws SAXException {
            if (!mProbablyValidElement) {
                throw new SAXException();
            } else {
                super.endDocument();
            }
        }
    }
    private static void exportToOpml(final Activity activity, final boolean isBackup) {
        new WaitDialog(activity, R.string.exportingToFile, () -> {
            try {
                final String dateTimeStr = new SimpleDateFormat(FILENAME_DATETIME_FORMAT).format(new Date(System.currentTimeMillis() ) );
                final String name = "/HandyNewsReader_" + dateTimeStr + ( isBackup ? ".backup" : ".opml" );
                final String fileName =  getContext().getCacheDir().getAbsolutePath() + "/" + name;
                OPML.exportToFile( fileName, isBackup );
                FileUtils.INSTANCE.copyFileToDownload( fileName, true );
            } catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> UiUtils.showMessage(activity, R.string.error_feed_export));
            }
        }).execute();
    }

    static private void importFromOpml( final Activity activity ) {
        FileSelectDialog.Companion.startFilePickerIntent(activity, "*/*", REQUEST_PICK_OPML_FILE);
    }
    private static void RequestPermissions(Activity activity, ExportImport operType) {
        if ( operType == ExportImport.ExportToOPML )
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXPORT_TO_OPML);
        else if ( operType == ExportImport.Backup )
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXPORT_TO_OPML);
        else if ( operType == ExportImport.Import )
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_IMPORT_FROM_OPML);
    }
    public enum ExportImport { ExportToOPML, Backup, Import };
    static public void OnMenuExportImportClick(final Activity activity, final ExportImport operType) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.storage_request_explanation).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RequestPermissions( activity, operType );
                    }


                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
                builder.show();
            } else {
                // No explanation needed, we can request the permission.
                RequestPermissions( activity, operType );
            }
        } else {
            if ( operType == ExportImport.ExportToOPML )
                exportToOpml( activity, false );
            else if ( operType == ExportImport.Backup )
                exportToOpml( activity, true );
            else if ( operType == ExportImport.Import )
                importFromOpml( activity );
        }
    }
    static public void OnRequestPermissionResult(Activity activity, int requestCode, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_EXPORT_TO_OPML: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportToOpml( activity, false );
                }
            }
            case PERMISSIONS_REQUEST_BACKUP: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportToOpml( activity, true );
                }
            }
            case PERMISSIONS_REQUEST_IMPORT_FROM_OPML: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    importFromOpml( activity );
                }
            }
        }
    }

    public static final FileSelectDialog mImportFileSelectDialog =
        new FileSelectDialog(OPML::AskQuestionForImport, "backup", REQUEST_PICK_OPML_FILE, R.string.error_feed_import  );

    private static void StartServiceForImport(String fileName, boolean isRemoveExistingFeeds, boolean isFileNameUri) {
        FetcherService.StartService( FetcherService.GetIntent(Constants.FROM_IMPORT )
                                     .putExtra( isFileNameUri ? Constants.EXTRA_URI : Constants.EXTRA_FILENAME, fileName )
                                     .putExtra( EXTRA_REMOVE_EXISTING_FEEDS_BEFORE_IMPORT, isRemoveExistingFeeds ), false);
    }
    public static void AskQuestionForImport(final Activity activity, final String fileName, final boolean isFileNameUri ) {
        Cursor cursor = getContext().getContentResolver().query( FeedData.FeedColumns.CONTENT_URI, null, _ID + "<>" + GetExtrenalLinkFeedID(), null, null );
        if ( cursor.moveToFirst() ) {
            new AlertDialog.Builder( activity )
                .setTitle( activity.getString( R.string.remove_existing_feeds_question ) )
                .setItems(new CharSequence[]{
                    activity.getString(R.string.yes),
                    activity.getString(R.string.no),
                    activity.getString(android.R.string.cancel),}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if ( i == 0 ) //yes
                            new AlertDialog.Builder( activity )
                                .setTitle( activity.getString( R.string.remove_existing_feeds_confirmation ) )
                                .setPositiveButton(R.string.yes_I_realize_btn_caption, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        StartServiceForImport( fileName, true, isFileNameUri );
                                    }
                                })
                                .setNegativeButton( R.string.sorry_I_was_wrong_btn_caption, null ).show();

                        else if ( i == 1  ) //no
                            StartServiceForImport( fileName, false, isFileNameUri);
                        dialogInterface.dismiss();
                    }


                }).show();
        } else
            StartServiceForImport( fileName, false, isFileNameUri);
        cursor.close();

    }

}
