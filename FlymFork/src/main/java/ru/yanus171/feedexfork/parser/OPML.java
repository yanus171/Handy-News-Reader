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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Map;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.FileUtils;

import static ru.yanus171.feedexfork.Constants.FALSE;
import static ru.yanus171.feedexfork.Constants.TRUE;
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

public class OPML {

    public static String GetAutoBackupOPMLFileName() { return  FileUtils.GetFolder() + "/HandyNewsReader_auto_backup.opml"; }

    private static final String START = "<?xml version='1.0' encoding='utf-8'?>\n<opml version='1.1'>\n<head>\n<title>Handy News Reader export</title>\n<dateCreated>";
    private static final String AFTER_DATE = "</dateCreated>\n</head>\n<body>\n";
    private static final String OUTLINE_TITLE = "\t<outline title='";
    private static final String OUTLINE_XMLURL = "' type='rss' xmlUrl='";
    private static final String OUTLINE_RETRIEVE_FULLTEXT = "' retrieveFullText='";
    //private static final String OUTLINE_INLINE_CLOSING = "'/>\n";
    private static final String OUTLINE_NORMAL_CLOSING = "'>\n";
    private static final String OUTLINE_END = "\t</outline>\n";

    private static final String FILTER_TEXT = "\t\t<filter text='";
    private static final String FILTER_IS_REGEX = "' isRegex='";
    private static final String FILTER_IS_APPLIED_TO_TITLE = "' isAppliedToTitle='";
    private static final String FILTER_IS_ACCEPT_RULE = "' isAcceptRule='";
    private static final String FILTER_IS_MARK_AS_STARRED = "' isMarkAsStarred='";
    private static final String FILTER_CLOSING = "'/>\n";

    private static final String TAG_ENTRY = "entry";

    private static final String TAG_START = "\t\t<%s %s='";
    private static final String ATTR_VALUE = "' %s='";
    private static final String TAG_CLOSING = "'/>\n";

    private static final String TAG_PREF = "pref";
    private static final String ATTR_PREF_CLASSNAME = "classname";
    private static final String ATTR_PREF_VALUE = "value";
    private static final String ATTR_PREF_KEY = "key";

    private static final String CLOSING = "</body>\n</opml>\n";

    //private static final OPMLParser mParser = new OPMLParser();
    private static Boolean mAutoBackupEnabled = true;

    public static
    void importFromFile(String filename) throws IOException, SAXException {
        importFromFile(new FileInputStream(filename));
    }

    private static void importFromFile(InputStream input) throws IOException, SAXException {
        SetAutoBackupEnabled(false); // Do not write the auto backup file while reading it...
        try {
            final OPMLParser parser = new OPMLParser();
            Xml.parse(new InputStreamReader(input), parser);
            //parser.mEditor.commit();
        } finally {
            SetAutoBackupEnabled(true);
        }
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

    public static void exportToFile(String filename) throws IOException {
        if ( GetAutoBackupOPMLFileName().equals(filename) && !IsAutoBackupEnabled() )
            return;

        final Context context = MainApplication.getContext();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        //final int status = FetcherService.Status().Start( context.getString( R.string.exportingToFile ) );
        try {
            Cursor cursorGroupsAndRoot = context.getContentResolver()
                    .query(GROUPS_AND_ROOT_CONTENT_URI, FEEDS_PROJECTION, null, null, null);

            writer.write( START );
            writer.write( String.valueOf( System.currentTimeMillis() ) );
            writer.write( AFTER_DATE);
            SaveSettings( writer, "\t\t");

            {
                Cursor cursor = context.getContentResolver().query(CONTENT_URI( FetcherService.GetExtrenalLinkFeedID() ), FEEDS_PROJECTION, null, null, null);
                if ( cursor.moveToFirst() )
                    ExportFeed(writer, cursor);
                cursor.close();
            }

            while (cursorGroupsAndRoot.moveToNext()) {
                if (cursorGroupsAndRoot.getInt(1) == 1) { // If it is a group
                    writer.write( OUTLINE_TITLE);
                    writer.write( cursorGroupsAndRoot.isNull(2) ? "" : TextUtils.htmlEncode(cursorGroupsAndRoot.getString(2)));
                    WriteLongValue(writer, cursorGroupsAndRoot, PRIORITY, 11);
                    writer.write( OUTLINE_NORMAL_CLOSING);
                    Cursor cursorFeeds = context.getContentResolver()
                            .query(FEEDS_FOR_GROUPS_CONTENT_URI(cursorGroupsAndRoot.getString(0)), FEEDS_PROJECTION, null, null, null);
                    while (cursorFeeds.moveToNext()) {
                        ExportFeed(writer, cursorFeeds);
                    }
                    cursorFeeds.close();

                    writer.write( OUTLINE_END);
                } else
                    ExportFeed(writer, cursorGroupsAndRoot);
            }
            writer.write( CLOSING );

            cursorGroupsAndRoot.close();
        } finally {
            writer.close();
        }
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

    private static void ExportFeed(Writer writer, Cursor cursor) throws IOException {
        final String feedID = cursor.getString(0);
        writer.write( "\t");
        writer.write( OUTLINE_TITLE);
        writer.write(GetEncoded( cursor ,2) );
        writer.write(OUTLINE_XMLURL);
        writer.write(GetEncoded( cursor, 3));
        writer.write(OUTLINE_RETRIEVE_FULLTEXT);
        writer.write(GetBoolText( cursor, 4 ));
        WriteBoolValue(writer, cursor, SHOW_TEXT_IN_ENTRY_LIST, 5);
        WriteBoolValue(writer, cursor, IS_AUTO_REFRESH, 6);
        WriteBoolValue(writer, cursor, IS_IMAGE_AUTO_LOAD, 7);
        WriteEncodedText( writer, cursor, OPTIONS, 8 );
        WriteLongValue(writer, cursor, LAST_UPDATE, 9);
        WriteLongValue(writer, cursor, REAL_LAST_UPDATE, 10);
        WriteLongValue(writer, cursor, PRIORITY, 11);
        WriteLongValue(writer, cursor, FETCH_MODE, 12);
        writer.write(OUTLINE_NORMAL_CLOSING);

        ExportFilters(writer, feedID);
        final boolean saveAbstract = !TRUE.equals( GetBoolText( cursor, 4 ) );
        ExportEntries(writer, feedID, saveAbstract);
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
            EntryColumns.IS_FAVORITE, EntryColumns._ID, EntryColumns.GUID };

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
        Cursor cur = MainApplication.getContext().getContentResolver()
                .query(ENTRIES_FOR_FEED_CONTENT_URI( feedID ), ENTRIES_PROJECTION, null, null, null);
        if (cur.getCount() != 0) {
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
//                if ( !saveAbstract && cur.getInt(10) == 1 ) {
//                    //writer.write(String.format(ATTR_VALUE, EntryColumns.MOBILIZED_HTML));
//                    long entryID  = cur.getLong(11 );
//                    final String text = GetMobilizedText( entryID );
//                    writer.write(text == null ? "" : TextUtils.htmlEncode(text));
//                }
                WriteEncodedText(writer, cur, EntryColumns.GUID, 12);
                writer.write(TAG_CLOSING);
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
            FilterColumns.IS_APPLIED_TO_TITLE, FilterColumns.IS_ACCEPT_RULE, FilterColumns.IS_MARK_STARRED};

    private static void ExportFilters(Writer writer, String feedID) throws IOException {
        Cursor cur = MainApplication.getContext().getContentResolver()
                .query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedID), FILTERS_PROJECTION, null, null, null);
        if (cur.getCount() != 0) {
            while (cur.moveToNext()) {
                writer.write("\t");
                writer.write(FILTER_TEXT);
                writer.write(TextUtils.htmlEncode(cur.getString(0)));
                writer.write(FILTER_IS_REGEX);
                writer.write(GetBoolText( cur, 1));
                writer.write(FILTER_IS_APPLIED_TO_TITLE);
                writer.write(GetBoolText( cur, 2) );
                writer.write(FILTER_IS_ACCEPT_RULE);
                writer.write(GetBoolText( cur, 3) );
                writer.write(FILTER_IS_MARK_AS_STARRED);
                writer.write(GetBoolText( cur, 4) );
                writer.write(FILTER_CLOSING);
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
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
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
        private final SharedPreferences.Editor mEditor;

        private boolean mBodyTagEntered = false;
        private boolean mFeedEntered = false;
        private boolean mProbablyValidElement = false;
        private String mGroupId = null;
        private String mFeedId = null;

        @SuppressLint("CommitPrefEdits")
        OPMLParser() {
            mEditor = PreferenceManager.getDefaultSharedPreferences( MainApplication.getContext() ).edit();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
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

                ContentResolver cr = MainApplication.getContext().getContentResolver();

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
                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_APPLIED_TO_TITLE)));
                    values.put(FilterColumns.IS_ACCEPT_RULE, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_ACCEPT_RULE)));
                    values.put(FilterColumns.IS_MARK_STARRED, TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_MARK_AS_STARRED)));

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(mFeedId), values);
                }
            } else if (TAG_ENTRY.equals(localName)) {
                if (mFeedEntered && mFeedId != null) {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_NEW, GetBool( attributes, EntryColumns.IS_NEW));
                    values.put(EntryColumns.IS_READ, GetBool( attributes, EntryColumns.IS_READ));
                    values.put(EntryColumns.IS_FAVORITE, GetBool( attributes, EntryColumns.IS_FAVORITE));
                    values.put(EntryColumns.LINK, GetText( attributes, EntryColumns.LINK ));
                    values.put(EntryColumns.ABSTRACT, GetText( attributes, EntryColumns.ABSTRACT));
                    values.put(EntryColumns.MOBILIZED_HTML, GetText( attributes, EntryColumns.MOBILIZED_HTML));
                    values.put(EntryColumns.FETCH_DATE, GetText( attributes, EntryColumns.FETCH_DATE));
                    values.put(EntryColumns.DATE, GetText( attributes, EntryColumns.DATE));
                    values.put(EntryColumns.TITLE, GetText( attributes, EntryColumns.TITLE));
                    values.put(EntryColumns.SCROLL_POS, GetText( attributes, EntryColumns.SCROLL_POS ));
                    values.put(EntryColumns.AUTHOR, GetText( attributes, EntryColumns.AUTHOR) );
                    values.put(EntryColumns.IMAGE_URL, GetText( attributes, EntryColumns.IMAGE_URL));
                    values.put(EntryColumns.GUID, GetText( attributes, EntryColumns.GUID));

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( mFeedId ), values);
                }
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
                } else {
                    // throw new ClassNotFoundException("Unknown type: "
                    // + prefClass);
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
}
