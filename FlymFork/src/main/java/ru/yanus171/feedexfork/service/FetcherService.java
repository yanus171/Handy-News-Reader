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

package ru.yanus171.feedexfork.service;

import static android.content.Intent.EXTRA_TEXT;
import static android.provider.BaseColumns._ID;
import static java.lang.Thread.MIN_PRIORITY;
import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_IS_NOT_NULL;
import static ru.yanus171.feedexfork.Constants.DB_IS_NULL;
import static ru.yanus171.feedexfork.Constants.DB_OR;
import static ru.yanus171.feedexfork.Constants.EXTRA_FILENAME;
import static ru.yanus171.feedexfork.Constants.EXTRA_ID;
import static ru.yanus171.feedexfork.Constants.EXTRA_URI;
import static ru.yanus171.feedexfork.Constants.GROUP_ID;
import static ru.yanus171.feedexfork.MainApplication.OPERATION_NOTIFICATION_CHANNEL_ID;
import static ru.yanus171.feedexfork.MainApplication.UNREAD_NOTIFICATION_CHANNEL_ID;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.MainApplication.mImageFileVoc;
import static ru.yanus171.feedexfork.adapter.DrawerAdapter.newNumber;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.mCurrentUri;
import static ru.yanus171.feedexfork.fragment.EntryFragment.IsLocalFile;
import static ru.yanus171.feedexfork.fragment.EntryFragment.STATE_RELOAD_WITH_DEBUG;
import static ru.yanus171.feedexfork.fragment.EntryFragment.WHERE_SQL_EXTRA;
import static ru.yanus171.feedexfork.parser.OPML.EXTRA_REMOVE_EXISTING_FEEDS_BEFORE_IMPORT;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.CATEGORY_LIST_SEP;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.FEED_ID;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.IMAGES_SIZE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.IS_FAVORITE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.LINK;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.MOBILIZED_HTML;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_FAVORITE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_NOT_FAVORITE;
import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.WHERE_READ;
import static ru.yanus171.feedexfork.provider.FeedData.PutFavorite;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.SetNotifyEnabled;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.URI_ENTRIES_FOR_FEED;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.notifyChangeOnAllUris;
import static ru.yanus171.feedexfork.service.AutoJobService.DEFAULT_INTERVAL;
import static ru.yanus171.feedexfork.service.AutoJobService.getTimeIntervalInMSecs;
import static ru.yanus171.feedexfork.service.BroadcastActionReciever.Action;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.ClearContentStepToFile;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.SaveContentStepToFile;
import static ru.yanus171.feedexfork.utils.HtmlUtils.convertXMLSymbols;
import static ru.yanus171.feedexfork.utils.HtmlUtils.extractTitle;
import static ru.yanus171.feedexfork.utils.NetworkUtils.NATIVE;
import static ru.yanus171.feedexfork.utils.NetworkUtils.OKHTTP;
import static ru.yanus171.feedexfork.utils.PrefUtils.MAX_IMAGE_DOWNLOAD_COUNT;
import static ru.yanus171.feedexfork.utils.PrefUtils.REFRESH_INTERVAL;
import static ru.yanus171.feedexfork.view.StatusText.GetPendingIntentRequestCode;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Xml;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.parser.HTMLParser;
import ru.yanus171.feedexfork.parser.OPML;
import ru.yanus171.feedexfork.parser.OneWebPageParser;
import ru.yanus171.feedexfork.parser.RssAtomParser;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedData.TaskColumns;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Connection;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.LabelVoc;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;

@SuppressLint("Range")
public class FetcherService extends IntentService {

    public static final String ACTION_REFRESH_FEEDS = FeedData.PACKAGE_NAME + ".REFRESH";
    public static final String ACTION_MOBILIZE_FEEDS = FeedData.PACKAGE_NAME + ".MOBILIZE_FEEDS";
    private static final String ACTION_LOAD_LINK = FeedData.PACKAGE_NAME + ".LOAD_LINK";
    public static final String EXTRA_STAR = "STAR";
    public static final String EXTRA_LABEL_ID_LIST = "LABEL_ID_LIST";

    private static final int THREAD_NUMBER = 10;
    private static final int MAX_TASK_ATTEMPT = 3;

    private static final int FETCHMODE_DIRECT = 1;
    private static final int FETCHMODE_REENCODE = 2;
    public static final int FETCHMODE_EXERNAL_LINK = 3;

    private static final String CHARSET = "charset=";
    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    private static final String HREF = "href=\"";

    private static final String HTML_BODY = "<body";
    private static final String ENCODING = "encoding=\"";
    public static final String CUSTOM_KEEP_TIME = "customKeepTime";
    public static final String CUSTOM_REFRESH_INTERVAL = "customRefreshInterval";
    public static final String IS_ONE_WEB_PAGE = "isOneWebPage";
    public static final String IS_RSS = "isRss";
    public static final String NEXT_PAGE_URL_CLASS_NAME = "UrlNextPageClassName";
    public static final String NEXT_PAGE_MAX_COUNT = "NextPageMaxCount";

    public static final long MILLS_IN_DAY = 86400000L;

    public static Boolean mCancelRefresh = false;
    private static final ArrayList<Long> mActiveEntryIDList = new ArrayList<>();
    private static Boolean mIsDownloadImageCursorNeedsRequery = false;

    //private static volatile Boolean mIsDeletingOld = false;

    public static final ArrayList<MarkItem> mMarkAsStarredFoundList = new ArrayList<>();

    /* Allow different positions of the "rel" attribute w.r.t. the "href" attribute */
    public static final Pattern FEED_LINK_PATTERN = Pattern.compile(
            "[.]*<link[^>]* ((rel=alternate|rel=\"alternate\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* (rel=alternate|rel=\"alternate\"))[^>]*>",
            Pattern.CASE_INSENSITIVE);
    public static int mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();

    public static StatusText.FetcherObservable Status() {
        if ( mStatusText == null ) {
            mStatusText = new StatusText.FetcherObservable();
        }
        return mStatusText;
    }

    private static StatusText.FetcherObservable mStatusText = null;

    public FetcherService() {
        super(FetcherService.class.getSimpleName());
        HttpURLConnection.setFollowRedirects(true);
    }

    public static boolean hasMobilizationTask(long entryId) {
        Cursor cursor = getContext().getContentResolver().query(TaskColumns.CONTENT_URI, TaskColumns.PROJECTION_ID,
                TaskColumns.ENTRY_ID + '=' + entryId + DB_AND + TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NULL, null, null);

        boolean result = cursor.getCount() > 0;
        cursor.close();

        return result;
    }

    public static void addImagesToDownload(String entryId, ArrayList<String> images) {
        if (images != null && !images.isEmpty()) {
            ContentValues[] values = new ContentValues[images.size()];
            for (int i = 0; i < images.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(TaskColumns.ENTRY_ID, entryId);
                values[i].put(TaskColumns.IMG_URL_TO_DL, images.get(i));
            }

            getContext().getContentResolver().bulkInsert(TaskColumns.CONTENT_URI, values);
        }
    }

    public static void addEntriesToMobilize( Uri uri ) {
        ContentResolver cr = MainApplication.getContext().getContentResolver();

        ArrayList<Long> entriesId = new ArrayList<>();
        Cursor c = cr.query(uri, new String[]{EntryColumns._ID, EntryColumns.LINK, MOBILIZED_HTML},
                            MOBILIZED_HTML + DB_IS_NULL + DB_OR + MOBILIZED_HTML + " = '" + FileUtils.EMPTY_MOBILIZED_VALUE + "'", null, EntryColumns.DATE + Constants.DB_DESC);
        while (c.moveToNext()) {
            if (!FileUtils.INSTANCE.isMobilized( c.getString(1), c, 2, 0 ))
                entriesId.add(c.getLong(0));
        }
        c.close();

        ContentValues[] values = new ContentValues[entriesId.size()];
        for (int i = 0; i < entriesId.size(); i++) {
            values[i] = new ContentValues();
            values[i].put(TaskColumns.ENTRY_ID, entriesId.get(i));
        }

        getContext().getContentResolver().bulkInsert(TaskColumns.CONTENT_URI, values);
    }

    static boolean isBatteryLow() {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = getContext().registerReceiver(null, ifilter);
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale * 100;

        long lowLevelPct = 20;
        try {
            lowLevelPct = Math.max(50, Long.parseLong(PrefUtils.getString("refresh.min_update_battery_level", 20)) );
        } catch (Exception ignored) {
        }
        return batteryPct < lowLevelPct;
    }
    private void moveBackupFileVersion( String fileName, int index ) throws IOException {
        final File sourceFile = new File( index == 0 ? fileName : String.format( "%s.%d", fileName, index ) );
        final File destFile = new File( String.format( "%s.%d", fileName, index + 1 ) );
        if ( !sourceFile.exists() )
            return;
        FileUtils.INSTANCE.copy(sourceFile, destFile);
        FileUtils.INSTANCE.copyFileToDownload(destFile.getPath(), false);
    }
    @Override
    public void onHandleIntent(final Intent intent) {
        if (intent == null) // No intent, we quit
            return;
        if ( MainApplication.getContext() == null )
            return;
        Status().ClearError();

        FileUtils.INSTANCE.reloadPrefs();

        if (intent.hasExtra(Constants.FROM_AUTO_BACKUP)) {
            LongOper(R.string.exportingToFile, () -> {
                try {
                    final String sourceFileName = OPML.GetAutoBackupOPMLFileName();
                    moveBackupFileVersion( sourceFileName, 2 );
                    moveBackupFileVersion( sourceFileName, 1 );
                    moveBackupFileVersion( sourceFileName, 0 );
                    OPML.exportToFile( sourceFileName, true );
                    FileUtils.INSTANCE.copyFileToDownload(sourceFileName, true);
                    PrefUtils.putLong( AutoJobService.LAST_JOB_OCCURED + PrefUtils.AUTO_BACKUP_INTERVAL, System.currentTimeMillis() );
                } catch (IOException e) {
                    e.printStackTrace();
                    DebugApp.SendException( e, FetcherService.this );
                }
            });
            return;
        } else if (intent.hasExtra(Constants.FROM_IMPORT)) {
            LongOper(R.string.importingFromFile, () -> {
                try {
                    final boolean isRemoveExistingFeeds = intent.getBooleanExtra( EXTRA_REMOVE_EXISTING_FEEDS_BEFORE_IMPORT, false );
                    if ( intent.hasExtra( EXTRA_FILENAME ) )
                        OPML.importFromFile( intent.getStringExtra( EXTRA_FILENAME ), isRemoveExistingFeeds );
                    else if ( intent.hasExtra( EXTRA_URI ) )
                        OPML.importFromFile( Uri.parse( intent.getStringExtra( EXTRA_URI ) ), isRemoveExistingFeeds );
                } catch (Exception e) {
                    DebugApp.SendException(e, FetcherService.this);
                }
            });
            return;
        } else if (intent.hasExtra( Constants.FROM_DELETE_OLD )) {
            LongOper(R.string.menu_delete_old, new Runnable() {
                @Override
                public void run() {
                    SetNotifyEnabled( false );
                    try {
                        long keepTime = (long) (GetDefaultKeepTime() * MILLS_IN_DAY);
                        long keepDateBorderTime = keepTime > 0 ? System.currentTimeMillis() - keepTime : 0;
                        deleteOldEntries(keepDateBorderTime);
                        deleteGhost();
                        if (PrefUtils.CALCULATE_IMAGES_SIZE())
                            CalculateImageSizes();
                        if (Build.VERSION.SDK_INT >= 21)
                            PrefUtils.putLong(AutoJobService.LAST_JOB_OCCURED + PrefUtils.DELETE_OLD_INTERVAL, System.currentTimeMillis());
                    } finally {
                        SetNotifyEnabled( true );
                        notifyChangeOnAllUris( URI_ENTRIES_FOR_FEED, null );
                    }
                }
            });
            return;
        } else if (intent.hasExtra( Constants.FROM_RELOAD_ALL_TEXT )) {
            LongOper(R.string.reloading_all_texts, () -> {
                SetNotifyEnabled(false);
                try (Cursor cursor = getContext().getContentResolver().query(intent.getData(), new String[]{_ID, LINK}, intent.getStringExtra(WHERE_SQL_EXTRA), null, null)) {
                    ContentValues[] values = new ContentValues[cursor.getCount()];
                    while (cursor.moveToNext()) {
                        final long entryId = cursor.getLong(0);
                        final String link = cursor.getString(1);
                        FileUtils.INSTANCE.deleteMobilized(link, EntryColumns.CONTENT_URI(entryId));
                        values[cursor.getPosition()] = new ContentValues();
                        values[cursor.getPosition()].put(TaskColumns.ENTRY_ID, entryId);
                    }
                    getContext().getContentResolver().bulkInsert(TaskColumns.CONTENT_URI, values);
                } finally {
                    SetNotifyEnabled(true);
                    notifyChangeOnAllUris(URI_ENTRIES_FOR_FEED, null);
                }

                ExecutorService executor = CreateExecutorService(GetThreadCount());
                try {
                    mobilizeAllEntries(executor);
                    downloadAllImages(executor);
                } finally {
                    executor.shutdown();
                }
            });
            return;
        }

        mIsWiFi = GetIsWifi();

        final boolean isFromAutoRefresh = intent.getBooleanExtra(Constants.FROM_AUTO_REFRESH, false);

        if (ACTION_MOBILIZE_FEEDS.equals(intent.getAction())) {
            ExecutorService executor = CreateExecutorService( GetThreadCount() ); try {
                mobilizeAllEntries(executor);
                downloadAllImages(executor);
            } finally {
                executor.shutdown();
            }
        } else if (ACTION_LOAD_LINK.equals(intent.getAction())) {
            LongOper(R.string.loadingLink, new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = CreateExecutorService(GetLoadImageThreadCount()); try {
                        Pair<Uri,Boolean> result = LoadLink(GetExtrenalLinkFeedID(),
                                 intent.getStringExtra(Constants.URL_TO_LOAD),
                                 intent.getStringExtra(Constants.TITLE_TO_LOAD),
                                 null,
                                 FetcherService.ForceReload.No,
                                 true,
                                 true,
                                 intent.getBooleanExtra(EXTRA_STAR, false),
                                 AutoDownloadEntryImages.Yes,
                                 false,
                                 true,
                                                            true);
                        if ( intent.hasExtra( EXTRA_LABEL_ID_LIST ) && intent.getStringArrayListExtra( EXTRA_LABEL_ID_LIST ) != null && result.first != null ) {
                            HashSet<Long> labels = new HashSet<>();
                            for ( String item: intent.getStringArrayListExtra( EXTRA_LABEL_ID_LIST ) )
                                labels.add( Long.parseLong( item ) );
                            LabelVoc.INSTANCE.setEntry(Long.parseLong(result.first.getLastPathSegment() ), labels );
                        }
                        downloadAllImages(executor);
                    } finally { executor.shutdown(); }
                }
            } );
        } else { // == Constants.ACTION_REFRESH_FEEDS
            LongOper(R.string.RefreshFeeds, new Runnable() {
                @Override
                public void run() {
                    long keepTime = (long) (GetDefaultKeepTime() * MILLS_IN_DAY);
                    long keepDateBorderTime = keepTime > 0 ? System.currentTimeMillis() - keepTime : 0;

                    String feedId = intent.getStringExtra(Constants.FEED_ID);
                    String groupId = intent.getStringExtra(Constants.GROUP_ID);

                    mMarkAsStarredFoundList.clear();
                    int newCount = 0;
                    ExecutorService executor = CreateExecutorService(GetThreadCount()); try {
                        if ( isFromAutoRefresh )
                            SetNotifyEnabled( false );
                        try {
                            newCount = (feedId == null ?
                                    refreshFeeds( executor, keepDateBorderTime, groupId, isFromAutoRefresh) :
                                    refreshFeed( executor, feedId, keepDateBorderTime ));
                            if ( newCount > 0 )
                                EntryUrlVoc.INSTANCE.reinit( false );
                        } finally {
                            if (mMarkAsStarredFoundList.size() > 5) {
                                ArrayList<String> list = new ArrayList<>();
                                for (MarkItem item : mMarkAsStarredFoundList)
                                    list.add(item.mCaption);
                                ShowEventNotification(TextUtils.join(", ", list),
                                                      R.string.markedAsStarred,
                                                      new Intent(FetcherService.this, HomeActivity.class),
                                                      Constants.NOTIFICATION_ID_MANY_ITEMS_MARKED_STARRED, null);
                            } else if (mMarkAsStarredFoundList.size() > 0)
                                for (MarkItem item : mMarkAsStarredFoundList) {
                                    Uri entryUri = GetEntryUri(item.mLink);

                                    int ID = -1;
                                    try {
                                        if (entryUri != null)
                                            ID = Integer.parseInt(entryUri.getLastPathSegment());
                                    } catch (Throwable ignored) {

                                    }

                                    ShowEventNotification(item.mCaption,
                                                          R.string.markedAsStarred,
                                                          GetEntryActivityIntent(Intent.ACTION_VIEW, entryUri),
                                                          ID, createCancelStarPI( item.mLink, ID ));
                                }
                            if ( isFromAutoRefresh ) {
                                SetNotifyEnabled( true );
                                notifyChangeOnAllUris( URI_ENTRIES_FOR_FEED, mCurrentUri );
                            }
                        }

                        if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_ENABLED, true) && newCount > 0)
                            ShowEventNotification(getResources().getQuantityString(R.plurals.number_of_new_entries, newCount, newCount),
                                                  R.string.flym_feeds,
                                                  new Intent(FetcherService.this, HomeActivity.class),
                                                  Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT,
                                                  null);
                        else if (Constants.NOTIF_MGR != null)
                            Constants.NOTIF_MGR.cancel(Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT);

                        if ( isFromAutoRefresh || newCount > 0 ) {
                            mobilizeAllEntries(executor);
                            downloadAllImages(executor);
                        }
                    } finally {
                        executor.shutdown();
                    }
                    if ( isFromAutoRefresh && Build.VERSION.SDK_INT >= 21 )
                        PrefUtils.putLong( AutoJobService.LAST_JOB_OCCURED + PrefUtils.REFRESH_INTERVAL, System.currentTimeMillis() );
                }
            } );
        }
    }

    private void deleteGhost() {
        final int status = Status().Start( R.string.deleting_ghost_entries, false );
        final HashSet<String> mapEntryLinkHash = new HashSet<>();
        try (Cursor cursor = getContentResolver().query(EntryColumns.CONTENT_URI, new String[] {LINK}, null, null, null ) ) {
            while (cursor.moveToNext())
                mapEntryLinkHash.add(FileUtils.INSTANCE.getLinkHash(cursor.getString(0)));
        }
        deleteGhostHtmlFiles( mapEntryLinkHash );
        deleteGhostImages( mapEntryLinkHash );
        EntryUrlVoc.INSTANCE.reinit( true );
        Status().End( status );
    }


    private void deleteGhostHtmlFiles( final HashSet<String> mapEntryLink ) {
        if ( isCancelRefresh() )
            return;
        int deletedCount = 0;
        final File folder = FileUtils.INSTANCE.GetHTMLFolder();
        String[] fileNames = folder.list();
        if (fileNames != null  )
            for (String fileName : fileNames) {
                if ( !mapEntryLink.contains( fileName ) ) {
                    if ( new File( folder, fileName ).delete() )
                        deletedCount++;
                    Status().ChangeProgress(getString(R.string.deleteFullTexts) + String.format( " %d", deletedCount ) );
                    if (FetcherService.isCancelRefresh())
                        break;

                }
            }
        Status().ChangeProgress( "" );
        //Status().End( status );
    }

    private void deleteGhostImages(  final HashSet<String> setEntryLinkHash ) {
        if ( isCancelRefresh() )
            return;
        HashSet<String> setEntryLinkHashFavorities = new HashSet<>();
        try (Cursor cursor = getContentResolver().query( EntryColumns.FAVORITES_CONTENT_URI, new String[] {LINK}, null, null, null ) ){
            while (cursor.moveToNext())
                setEntryLinkHashFavorities.add(cursor.getString(0));
        }
        int deletedCount = 0;
        final File folder = FileUtils.INSTANCE.GetImagesFolder();
        File[] files = FileUtils.INSTANCE.GetImagesFolder().listFiles();
        final int status = Status().Start( getString(R.string.image_count) + String.format(": %d", files.length), false ); try {
            final int FIRST_COUNT_TO_DELETE = files.length - 8000;
            if (FIRST_COUNT_TO_DELETE > 500)
                Arrays.sort(files, new Comparator<File>() {

                    @Override
                    public int compare(File f1, File f2) {
                        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });
            if (isCancelRefresh())
                return;
            for (File file : files) {
                final String fileName = file.getName();
                final String[] list = TextUtils.split(fileName, "_");
                if (fileName.equals(".nomedia"))
                    continue;
                String linkHash = list[0];
                if ( deletedCount < FIRST_COUNT_TO_DELETE && !setEntryLinkHashFavorities.contains(linkHash) ||
                     list.length != 3 ||
                     list.length >= 2 && !setEntryLinkHash.contains(linkHash) ){
                    if (mImageFileVoc.removeFile(fileName))
                        deletedCount++;
                    Status().ChangeProgress(getString(R.string.deleteImages) + String.format(" %d", deletedCount));
                    if (FetcherService.isCancelRefresh())
                        break;
                }
            }
        } finally {
            Status().ChangeProgress( "" );
            Status().End( status );
        }
    }

    private void LongOper( int textID, Runnable oper ) {
        LongOper( getString( textID ), oper );
    }

    private void LongOper( String title, Runnable oper ) {
        startForeground(Constants.NOTIFICATION_ID_REFRESH_SERVICE, StatusText.GetNotification("", title, R.drawable.refresh, OPERATION_NOTIFICATION_CHANNEL_ID, createCancelPI()));
        Status().SetNotificationTitle( title, createCancelPI() );
        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true);
        synchronized (mCancelRefresh) {
            mCancelRefresh = false;
        }
        try {
            oper.run();
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText( this, getString( R.string.error ) + ": " + e.getMessage(), Toast.LENGTH_LONG ).show();
            DebugApp.SendException( e, this );
        } finally {
            Status().SetNotificationTitle( "", null );
            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);
            stopForeground(true);
            synchronized (mCancelRefresh) {
                mCancelRefresh = false;
            }
        }
    }

    public static float GetDefaultKeepTime() {
        return Float.parseFloat(PrefUtils.getString(PrefUtils.KEEP_TIME, "4"));
    }
    public static float GetDefaultRefreshInterval() {
        return Float.parseFloat(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, "3"));
    }

    private static boolean mIsWiFi = false;
    private boolean GetIsWifi() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI );
    }
    public static boolean isNotCancelRefresh() {
        return !isCancelRefresh();
    }
    public static boolean isCancelRefresh() {
        synchronized (mCancelRefresh) {
            if ( !mIsWiFi && Status().mBytesRecievedLast > PrefUtils.getMaxSingleRefreshTraffic() * 1024 * 1024 )
                return true;
            //if (mCancelRefresh) {
            //    MainApplication.getContext().getContentResolver().delete( TaskColumns.CONTENT_URI, null, null );
            //}
            return mCancelRefresh;
        }
    }

    public static boolean isEntryIDActive(long id) {
        synchronized (mActiveEntryIDList) {
            return mActiveEntryIDList.contains( id );
        }
    }
    public static void setEntryIDActiveList(ArrayList<Long> list) {
        synchronized (mActiveEntryIDList) {
            mActiveEntryIDList.clear();
            mActiveEntryIDList.addAll( list );
        }
    }
    public static void addActiveEntryID( long value ) {
        synchronized (mActiveEntryIDList) {
            if ( !mActiveEntryIDList.contains( value ) )
                mActiveEntryIDList.add( value );
        }
    }
    public static void removeActiveEntryID( long value ) {
        synchronized (mActiveEntryIDList) {
            if ( mActiveEntryIDList.contains( value ) )
                mActiveEntryIDList.remove( value );
        }
    }
    public static void clearActiveEntryID() {
        synchronized (mActiveEntryIDList) {
            mActiveEntryIDList.clear();
        }
    }
    private static boolean isDownloadImageCursorNeedsRequery() {
        synchronized (mIsDownloadImageCursorNeedsRequery) {
            return mIsDownloadImageCursorNeedsRequery;
        }
    }
    public static void setDownloadImageCursorNeedsRequery( boolean value ) {
        synchronized (mIsDownloadImageCursorNeedsRequery) {
            mIsDownloadImageCursorNeedsRequery = value;
        }
    }

    public static void CancelStarNotification( long entryID ) {
        if ( Constants.NOTIF_MGR != null ) {
            Constants.NOTIF_MGR.cancel((int) entryID);
            Constants.NOTIF_MGR.cancel(Constants.NOTIFICATION_ID_MANY_ITEMS_MARKED_STARRED);
            Constants.NOTIF_MGR.cancel(Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT);
        }
    }

    private void mobilizeAllEntries( ExecutorService executor) {
        final String statusText = getString(R.string.mobilizeAll);
        int status = Status().Start(statusText, false);
        SetNotifyEnabled( false ); try {
            final ContentResolver cr = getContentResolver();
            Status().ChangeProgress("");
            try ( Cursor cursor = cr.query(TaskColumns.CONTENT_URI, new String[]{_ID, TaskColumns.ENTRY_ID, TaskColumns.NUMBER_ATTEMPT},
                    TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NULL, null, null) ) {
                ArrayList<Future<FetcherService.DownloadResult>> futures = new ArrayList<>();
                while (cursor.moveToNext() && !isCancelRefresh()) {
                    final long taskId = cursor.getLong(0);
                    final long entryId = cursor.getLong(1);
                    int attemptCount = 0;
                    if (!cursor.isNull(2)) {
                        attemptCount = cursor.getInt(2);
                    }

                    final int finalAttemptCount = attemptCount;
                    futures.add(executor.submit(new Callable<DownloadResult>() {
                        @Override
                        public DownloadResult call() {
                            DownloadResult result = new DownloadResult();
                            result.mAttemptNumber = finalAttemptCount;
                            result.mTaskID = taskId;
                            result.mResultCount = 0;
                            try {
                                try (Cursor curEntry = cr.query(EntryColumns.CONTENT_URI(entryId), new String[]{EntryColumns.FEED_ID}, null, null, null)) {
                                    if (curEntry.moveToFirst()) {
                                        final String feedID = curEntry.getString(0);
                                        FeedFilters filters = new FeedFilters(feedID);
                                        if (mobilizeEntry(entryId, filters, ArticleTextExtractor.MobilizeType.Yes, IsAutoDownloadImages(feedID), true, false, false, false, false)) {
                                            ContentResolver cr = getContext().getContentResolver();
                                            cr.delete(TaskColumns.CONTENT_URI(taskId), null, null);//operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                                            result.mResultCount = 1;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Status().SetError("", "", "", e);
                            }
                            return result;
                        }
                    }));
                }

                FinishExecutionService(statusText, status, futures);
            }
        } finally {
            SetNotifyEnabled( true );
            notifyChangeOnAllUris( URI_ENTRIES_FOR_FEED, null );
            Status().End( status );
        }


    }

    public static AutoDownloadEntryImages IsAutoDownloadImages(String feedId) {
        final ContentResolver cr = getContext().getContentResolver();
        AutoDownloadEntryImages result = AutoDownloadEntryImages.Yes;
        try ( Cursor curFeed = cr.query( FeedColumns.CONTENT_URI( feedId ), new String[] { FeedColumns.IS_IMAGE_AUTO_LOAD }, null, null, null ) ) {
            if (curFeed.moveToFirst())
                result = curFeed.isNull(0) || curFeed.getInt(0) == 1 ? AutoDownloadEntryImages.Yes : AutoDownloadEntryImages.No;
        }
        return result;
    }

    public enum AutoDownloadEntryImages {Yes, No}

    @SuppressLint("Range")
    public static boolean loadFB2LocalFile(final long entryId, String link ) throws IOException {
        boolean result = false;
        if (!FileUtils.INSTANCE.getFileName(Uri.parse(link)).toLowerCase().contains(".fb2"))
            return result;
        Timer timer = new Timer( "loadFB2LocalFile " + link );
        try (InputStream is = MainApplication.getContext().getContentResolver().openInputStream(Uri.parse(link))) {
            ClearContentStepToFile();
            Status().ChangeProgress( "Jsoup.parse" );
            Document doc = Jsoup.parse(is, null, "");
            SaveContentStepToFile( doc, "Jsoup.parse" );

            Status().ChangeProgress( "images" );
            Elements list = doc.getElementsByTag("image");
            list.addAll( doc.getElementsByTag("img") );
            for (Element el : list ) {
                Elements images = doc.getElementsByAttributeValue( "id", el.attr( "l:href" ).replace( "#", "" ));
                if ( images.isEmpty() )
                    continue;
                el.insertChildren( -1, images.first() );
            }
            SaveContentStepToFile( doc, "binary_move" );

            Status().ChangeProgress( "title" );
            String title = "";
            for ( Element el: doc.getElementsByTag( "title" ) ) {
                title = el.text()
                    .replace( "\n", " " )
                    .replace( "\r", "" )
                    .replace( "<p>", "" )
                    .replace( "</p>", "" );
                break;
            }
            SaveContentStepToFile( doc, "title" );
            for (Element el : doc.getElementsByTag("title"))
                el.tagName( "h1" );
            SaveContentStepToFile( doc, "h1" );

            Status().ChangeProgress( "binary" );
            for (Element el : doc.getElementsByTag("binary")) {
                if (!el.hasAttr("content-type"))
                    continue;
                el.tagName("img");
                el.attr("src", "data:" + el.attr("content-type") + ";base64," + el.ownText().replace( "\n", "" ));
                el.text("");
            }
            SaveContentStepToFile( doc, "binary_to_img" );

            Status().ChangeProgress( "doc.toString()" );
            String content = doc.toString();
            //content = content.replaceAll( "[^\\s]{50,}", "" );

            Status().ChangeProgress( "replace 0" );
            content = content.replace( "&#x0;", "" );
            SaveContentStepToFile( content, "after_remove_0" );

            Status().ChangeProgress( "convertXMLSymbols" );
            content = convertXMLSymbols(content);
            if ( title.isEmpty() ) {
                Status().ChangeProgress( "extractTitle" );
                title = Html.fromHtml(extractTitle(content)).toString();
            }

            content = AddFB2TableOfContent( content );


            ContentValues values = new ContentValues();
            values.put(EntryColumns.TITLE, title );
            Status().ChangeProgress( "saveMobilizedHTML" );
            FileUtils.INSTANCE.saveMobilizedHTML(link, content, values);
            MainApplication.getContext().getContentResolver().update(EntryColumns.CONTENT_URI( entryId ), values, null, null);
            Status().ChangeProgress("");
            result = true;
        }
        timer.End();
        return result;
    }
    private static String AddFB2TableOfContent(String content) {
        final Pattern PATTERN = Pattern.compile("<(h1|title)>((.|\\n|\\t)+?)</(h1|title)>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = PATTERN.matcher(content);
        StringBuilder tc = new StringBuilder();
        int i = 1;
        String TC_START = "TC_START";
        while (matcher.find()) {
            String match = matcher.group();
            String newText = "<div id=\"tc" + i + "\" >" + match + "</div>";
            if ( i == 1 )
                newText = TC_START + newText;
            content = content.replace(match, newText);
            tc.append("<p><a href=\"#tc").append(i).append("\">").append(matcher.group(2)).append("</a></p>");
            i++;
        }
        content = content.replaceFirst( TC_START, tc.toString() );
        return content;
    }
    @SuppressLint("Range")
    public static boolean mobilizeEntry(final long entryId,
                                        FeedFilters filters,
                                        final ArticleTextExtractor.MobilizeType mobilize,
                                        final AutoDownloadEntryImages autoDownloadEntryImages,
                                        final boolean isCorrectTitle,
                                        final boolean isShowError,
                                        final boolean isForceReload,
                                        boolean isParseDateFromHTML,
                                        final boolean withScripts ) {
        boolean success = false;
        ClearContentStepToFile();
        ContentResolver cr = getContext().getContentResolver();
        Uri entryUri = EntryColumns.CONTENT_URI(entryId);
        long feedId = -1;
        try ( Cursor entryCursor = cr.query(entryUri, null, null, null, null) ) {

            if (entryCursor.moveToFirst()) {
                int linkPos = entryCursor.getColumnIndex(LINK);
                String link = entryCursor.getString(linkPos);
                try {
                    feedId = entryCursor.getLong(entryCursor.getColumnIndex(EntryColumns.FEED_ID));
                    if (IsLocalFile(Uri.parse(link)))
                        return loadFB2LocalFile(entryId, link);
                    String linkToLoad = HTMLParser.INSTANCE.replaceTomorrow(link).trim();
                    String contentIndicator = null;
                    {
                        Pattern rx = Pattern.compile("\\{(.+)\\}");
                        final Matcher matcher = rx.matcher(linkToLoad);

                        if (matcher.find()) {
                            Calendar date = Calendar.getInstance();
                            linkToLoad = linkToLoad.replace(matcher.group(0), new SimpleDateFormat(matcher.group(1)).format(new Date(date.getTimeInMillis())));
                        }
                    }

                    if (isLinkToLoad(linkToLoad.toLowerCase()) && (isForceReload || !FileUtils.INSTANCE.isMobilized(link, entryCursor))) { // If we didn't already mobilized it
                        int abstractHtmlPos = entryCursor.getColumnIndex(EntryColumns.ABSTRACT);

                        Connection connection = null;
                        Document doc = null;
                        try {
                            // Try to find a text indicator for better content extraction
                            String text = entryCursor.getString(abstractHtmlPos);
                            if (!TextUtils.isEmpty(text)) {
                                text = Html.fromHtml(text).toString();
                                if (text.length() > 60) {
                                    contentIndicator = text.substring(20, 40);
                                }
                            }

                            connection = new Connection(linkToLoad, OKHTTP);

                            Status().ChangeProgress(R.string.extractContent);

                            if (FetcherService.isCancelRefresh())
                                return false;
                            doc = Jsoup.parse(connection.getInputStream(), null, "");
                            for (Element el : doc.getElementsByTag("meta")) {
                                if (el.hasAttr("content") &&
                                    el.hasAttr("http-equiv") &&
                                    el.attr("http-equiv").equals("refresh")) {
                                    String s = el.attr("content");
                                    link = s.replaceFirst("\\d+;URL=", "");
                                    connection.disconnect();
                                    connection = new Connection(link, OKHTTP);
                                    doc = Jsoup.parse(connection.getInputStream(), null, "");
                                    break;
                                }
                            }
                        } finally {
                            if (connection != null)
                                connection.disconnect();
                        }


                        ClearContentStepToFile();
                        SaveContentStepToFile(doc, "Jsoup.parse.connection.getInputStream");
                        final int titleCol = entryCursor.getColumnIndex(EntryColumns.TITLE);
                        String title = entryCursor.isNull(titleCol) ? null : entryCursor.getString(titleCol);
                        //if ( entryCursor.isNull( titlePos ) || title == null || title.isEmpty() || title.startsWith("http")  ) {
                        if (isCorrectTitle) {
                            Elements titleEls = doc.getElementsByTag("title");
                            if (!titleEls.isEmpty())
                                title = titleEls.first().text();
                        }

                        String dateText = "";
                        if (isParseDateFromHTML) {
                            Element element = ArticleTextExtractor.getDateElementFromPref(doc, link);
                            if (element != null) {
                                for (Element el : element.getAllElements())
                                    if (el.hasText())
                                        dateText += el.ownText() + " ";
                                dateText = dateText.trim();
                            } else {
                                try {
                                    dateText = doc.getElementsByTag("time").first().attr("datetime");
                                } catch (Exception ignored) {

                                }
                            }
                        }
                        Dog.v("date = " + dateText);

                        ArrayList<String> categoryList = new ArrayList<>();
                        String mobilizedHtml = ArticleTextExtractor.extractContent(doc,
                                                                                   link,
                                                                                   contentIndicator,
                                                                                   filters,
                                                                                   mobilize,
                                                                                   categoryList,
                                                                                   !String.valueOf(feedId).equals(GetExtrenalLinkFeedID()),
                                                                                   entryCursor.getInt(entryCursor.getColumnIndex(EntryColumns.IS_WITH_TABLES)) == 1,
                                                                                   withScripts);
                        Status().ChangeProgress("");

                        ContentValues values = new ContentValues();
                        if (mobilizedHtml != null) {
                            if (title.isEmpty())
                                title = extractTitle(mobilizedHtml);

                            if (!categoryList.isEmpty())
                                values.put(EntryColumns.CATEGORIES, TextUtils.join(CATEGORY_LIST_SEP, categoryList));
                            else
                                values.putNull(EntryColumns.CATEGORIES);
                            if (!dateText.isEmpty()) {
                                final String format = ArticleTextExtractor.getDataForUrlFromPref(link, PrefUtils.getString(PrefUtils.DATE_EXTRACT_RULES, ""));
                                Date date = null;
                                if (!format.isEmpty()) {
                                    try {
                                        date = new SimpleDateFormat(format, Locale.getDefault()).parse(dateText);
                                    } catch (ParseException e) {
                                        Status().SetError(format, String.valueOf(feedId), String.valueOf(entryId), e);
                                    }
                                }// else
                                //    date = parseDate( dateText, 0 );
                                if (date != null)
                                    values.put(EntryColumns.DATE, date.getTime());
                            }
                        }
                        FileUtils.INSTANCE.saveMobilizedHTML(link, mobilizedHtml, values);
                        {
                            boolean isOneWebPage = false;
                            final int optionsCol = entryCursor.getColumnIndex(FeedColumns.OPTIONS);
                            final String jsonText = entryCursor.isNull(optionsCol) ? "" : entryCursor.getString(optionsCol);
                            if (!jsonText.isEmpty())
                                try {
                                    JSONObject jsonOptions = new JSONObject(jsonText);
                                    isOneWebPage = jsonOptions.has(IS_ONE_WEB_PAGE) && jsonOptions.getBoolean(IS_ONE_WEB_PAGE);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            final int favCol = entryCursor.getColumnIndex(IS_FAVORITE);
                            if (entryCursor.isNull(titleCol) || (entryCursor.isNull(favCol) || entryCursor.getInt(favCol) == 0) && !isOneWebPage)
                                values.put(EntryColumns.TITLE, title);
                        }
                        ArrayList<String> imgUrlsToDownload = new ArrayList<>();
                        if (autoDownloadEntryImages == AutoDownloadEntryImages.Yes && NetworkUtils.needDownloadPictures())
                            HtmlUtils.replaceImageURLs(mobilizedHtml, "", entryId, link, true, imgUrlsToDownload, null, mMaxImageDownloadCount);

                        String mainImgUrl;
                        if (!imgUrlsToDownload.isEmpty())
                            mainImgUrl = HtmlUtils.getMainImageURL(imgUrlsToDownload);
                        else
                            mainImgUrl = HtmlUtils.getMainImageURL(mobilizedHtml);

                        if (mainImgUrl != null)
                            values.put(EntryColumns.IMAGE_URL, mainImgUrl);

                        if (filters != null && filters.isEntryFiltered(title, "", link, mobilizedHtml, categoryList.toArray(new String[0]))) {
                            cr.delete(entryUri, null, null);
                        } else {
                            if (filters != null && filters.isMarkAsStarred(title, "", link, mobilizedHtml, categoryList.toArray(new String[0])))
                                PutFavorite(values, true);
                            cr.update(entryUri, values, null, null);
                            if (!imgUrlsToDownload.isEmpty())
                                addImagesToDownload(String.valueOf(entryId), imgUrlsToDownload);
                        }
                        success = true;
                    } else { // We already mobilized it
                        success = true;
                        //operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (isShowError && feedId != -1 ) {
                        String title = "";
                        Cursor cursor = cr.query(FeedColumns.CONTENT_URI(feedId), new String[]{FeedColumns.NAME}, null, null, null);
                        if (cursor.moveToFirst() && cursor.isNull(0))
                            title = cursor.getString(0);
                        cursor.close();
                        Status().SetError(title + ": ", String.valueOf(feedId), String.valueOf(entryId), e);
                    }
                }
            }
        }
        return success;
    }

    public static boolean isLinkToLoad( String link ) {
        return  !link.endsWith( "mp3" ) &&
                !link.endsWith( "pdf" ) &&
                !link.endsWith( "avi" ) &&
                !link.endsWith( "mpeg" ) &&
                !link.endsWith( "doc" ) &&
                !link.endsWith( "docx" ) &&
                !link.endsWith( "jpeg" ) &&
                !link.endsWith( "png" );
    }


    public static Intent GetActionIntent( String action, Uri uri, Class<?> class1 ) {
        return new Intent(action, uri).setPackage( getContext().getPackageName() ).setClass( getContext(), class1 );
    }
    public static Intent GetEntryActivityIntent(String action, Uri uri ) {
        return GetActionIntent( action, uri, EntryActivity.class );
    }
    public static Intent GetIntent( String extra ) {
        return new Intent(getContext(), FetcherService.class).putExtra(extra, true );
    }
    public static void StartServiceLoadExternalLink(String url, String title, boolean star, ArrayList<String> labelIDs) {
        FetcherService.StartService( new Intent(getContext(), FetcherService.class )
                .setAction( ACTION_LOAD_LINK )
                .putExtra(Constants.URL_TO_LOAD, url)
                .putExtra(Constants.TITLE_TO_LOAD, title)
                .putExtra(EXTRA_LABEL_ID_LIST, labelIDs)
                .putExtra( EXTRA_STAR, star ), false );
    }
    public PendingIntent createCancelStarPI( String link, int notificationID ) {
        Intent intent = new Intent(this, BroadcastActionReciever.class);
        intent.setAction( Action );
        intent.putExtra("UnstarArticle", true );
        intent.putExtra( EXTRA_TEXT, link );
        intent.putExtra( EXTRA_ID, notificationID );
        return PendingIntent.getBroadcast(this, GetPendingIntentRequestCode(), intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public enum ForceReload {Yes, No}
//    public static void OpenLink( Uri entryUri ) {
//        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, entryUri.toString());
//        Intent intent = new Intent(MainApplication.getContext(), HomeActivity.class);
//        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
//        MainApplication.getContext().startActivity( intent );
//    }

    public static Uri GetEntryUri( final String url ) {
        Long id = EntryUrlVoc.INSTANCE.get( url );
        return id == null ? null : EntryColumns.CONTENT_URI(id);

//        Timer timer = new Timer( "GetEnryUri" );
//        Uri entryUri = null;
//        String url1 = url.replace("https:", "http:");
//        String url2 = url.replace("http:", "https:");
//        ContentResolver cr = MainApplication.getContext().getContentResolver();
//        Cursor cursor = cr.query(EntryColumns.CONTENT_URI,
//                new String[]{_ID, EntryColumns.FEED_ID},
//                LINK + "='" + url1 + "'" + DB_OR + LINK + "='" + url2 + "'",
//                null,
//                null);
//        try {
//            if (cursor.moveToFirst())
//                entryUri = Uri.withAppendedPath( EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( cursor.getString(1) ), cursor.getString(0) );
//        } finally {
//            cursor.close();
//        }
//        timer.End();
//        return entryUri;
    }
    public static Pair<Uri,Boolean> LoadLink(final String feedID,
                                             final String url,
                                             final String title,
                                             FeedFilters filters,
                                             final ForceReload forceReload,
                                             final boolean isCorrectTitle,
                                             final boolean isShowError,
                                             final boolean isStarred,
                                             AutoDownloadEntryImages autoDownloadEntryImages,
                                             boolean isParseDateFromHTML,
                                             boolean isLoadingLinkStatus,
                                             boolean setReadDate ) {
        boolean load;
        Dog.v( "LoadLink " + url );

        final ContentResolver cr = getContext().getContentResolver();
        final int status = isLoadingLinkStatus ? FetcherService.Status().Start(getContext().getString(R.string.loadingLink), false) : -1;
        try {
            Uri entryUri = GetEntryUri( url );
            if ( entryUri != null ) {
                load = (forceReload == ForceReload.Yes);
                if (load) {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.DATE, (new Date()).getTime());
                    if ( setReadDate )
                        values.put(EntryColumns.READ_DATE, (new Date()).getTime());
                    cr.update(entryUri, values, null, null);//operations.add(ContentProviderOperation.newUpdate(entryUri).withValues(values).build());
                }
            } else {

                ContentValues values = new ContentValues();
                values.put(EntryColumns.TITLE, title);
                values.put(EntryColumns.SCROLL_POS, 0);
                //values.put(EntryColumns.ABSTRACT, NULL);
                //values.put(EntryColumns.IMAGE_URL, NULL);
                //values.put(EntryColumns.AUTHOR, NULL);
                //values.put(EntryColumns.ENCLOSURE, NULL);
                values.put(EntryColumns.DATE, (new Date()).getTime());
                if ( setReadDate )
                    values.put(EntryColumns.READ_DATE, (new Date()).getTime());
                values.put(LINK, url);
                values.put(EntryColumns.IS_WITH_TABLES, 0);
                values.put(EntryColumns.IMAGES_SIZE, 0);
                if ( isStarred )
                    PutFavorite(values, true);
                //values.put(EntryColumns.MOBILIZED_HTML, enclosureString);
                //values.put(EntryColumns.ENCLOSURE, enclosureString);
                entryUri = cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), values);
                EntryUrlVoc.INSTANCE.set( url, entryUri );
                load = true;
            }

            if ( forceReload == ForceReload.Yes )
                FileUtils.INSTANCE.deleteMobilized( entryUri );

            if ( load && !FetcherService.isCancelRefresh() ) {
                final long entryId = Long.parseLong(entryUri.getLastPathSegment());
                mobilizeEntry( entryId, filters, ArticleTextExtractor.MobilizeType.Yes, autoDownloadEntryImages, isCorrectTitle, isShowError, false, isParseDateFromHTML, false);
            }
            return new Pair<>(entryUri, load);
        } finally {
            FetcherService.Status().End(status);
        }
        //stopForeground( true );
    }

    private static void downloadAllImages() {
        ExecutorService executor = CreateExecutorService(GetLoadImageThreadCount());
        try {
            downloadAllImages(executor);
        } finally {
            executor.shutdown();
        }
    }

    private static String mExtrenalLinkFeedID = "";
    public static String GetExtrenalLinkFeedID() {
        //Timer timer = new Timer( "GetExtrenalLinkFeedID()" );
        synchronized ( mExtrenalLinkFeedID ) {
            if (mExtrenalLinkFeedID.isEmpty()) {

                ContentResolver cr = getContext().getContentResolver();
                Cursor cursor = cr.query(FeedColumns.CONTENT_URI,
                        FeedColumns.PROJECTION_ID,
                        FeedColumns.FETCH_MODE + "=" + FetcherService.FETCHMODE_EXERNAL_LINK, null, null);
                if (cursor.moveToFirst())
                    mExtrenalLinkFeedID = cursor.getString(0);
                cursor.close();

                if (mExtrenalLinkFeedID.isEmpty()) {
                    ContentValues values = new ContentValues();
                    values.put(FeedColumns.FETCH_MODE, FetcherService.FETCHMODE_EXERNAL_LINK);
                    values.put(FeedColumns.NAME, getContext().getString(R.string.externalLinks));
                    mExtrenalLinkFeedID = cr.insert(FeedColumns.CONTENT_URI, values).getLastPathSegment();
                }
            }
        }
        //timer.End();
        return mExtrenalLinkFeedID;
    }

    public static class DownloadResult{
        public Long mTaskID;
        public Integer mAttemptNumber;
        public Integer mResultCount;

    }
    private static void downloadAllImages( ExecutorService executor ) {
        StatusText.FetcherObservable obs = Status();
        final String statusText = getContext().getString(R.string.AllImages);
        int status = obs.Start(statusText, false); try {

            ArrayList<Future<DownloadResult>> futures = new ArrayList<>();
            ContentResolver cr = getContext().getContentResolver();
            try ( Cursor cursor = cr.query(TaskColumns.CONTENT_URI, new String[]{_ID, TaskColumns.ENTRY_ID, TaskColumns.IMG_URL_TO_DL,
                    TaskColumns.NUMBER_ATTEMPT, LINK}, TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NOT_NULL, null, null) ) {
                while (cursor != null && cursor.moveToNext() && !isCancelRefresh() && !isDownloadImageCursorNeedsRequery()) {
                    final long taskId = cursor.getLong(0);
                    final long entryId = cursor.getLong(1);
                    final String entryLink = cursor.getString(4);
                    final String imgPath = cursor.getString(2);
                    int attemptNum = 0;
                    if (!cursor.isNull(3)) {
                        attemptNum = cursor.getInt(3);
                    }
                    final int finalNbAttempt = attemptNum;
                    futures.add( executor.submit(new Callable<DownloadResult>() {
                        @Override
                        public DownloadResult call() {
                            DownloadResult result = new DownloadResult();
                            result.mAttemptNumber = finalNbAttempt;
                            result.mTaskID = taskId;
                            result.mResultCount = 0;
                            try {
                                if ( NetworkUtils.downloadImage(entryId, entryLink, imgPath, true, false) )
                                    result.mResultCount = 1;
                            } catch ( Exception e ) {
                                Status().SetError( "", "", "", e );
                            }
                            return result;
                        }
                    }) );
                }
            }
            FinishExecutionService( statusText, status, futures);

        } finally { obs.End( status ); }

        if ( isDownloadImageCursorNeedsRequery() ) {
            setDownloadImageCursorNeedsRequery( false );
            downloadAllImages( executor );
        }
    }
    public static int FinishExecutionService( String statusText,
                                              int status,
                                              ArrayList<Future<DownloadResult>> futures) {
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        int countOK = 0;
        Status().Change(status, statusText + String.format(" %d/%d", 0, futures.size()));
        for ( Future<DownloadResult> item: futures ) {
            try {
                if ( isCancelRefresh() ) {
                    item.cancel(false );
                    continue;
                }
                final DownloadResult result = item.get();
                Status().Change(status, statusText + String.format(" %d/%d", futures.indexOf( item ) + 1, futures.size()));
                if (result.mResultCount > 0 ) {
                    countOK += result.mResultCount;// If we are here, everything WAS OK
                    if ( operations != null && result.mTaskID != null )
                        operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(result.mTaskID)).build());
                } else if ( operations != null ) {
                    if (result.mAttemptNumber + 1 > MAX_TASK_ATTEMPT) {
                        operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(result.mTaskID)).build());
                    } else {
                        ContentValues values = new ContentValues();
                        values.put(TaskColumns.NUMBER_ATTEMPT, result.mAttemptNumber + 1);
                        operations.add(ContentProviderOperation.newUpdate(TaskColumns.CONTENT_URI(result.mTaskID)).withValues(values).build());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();//DebugApp.AddErrorToLog( null ,e );
            }
        }
        if (!operations.isEmpty()) {
            new Thread() {
                @Override public void run() {
                    int status = Status().Start( R.string.applyOperations, false );
                    try {
                        MainApplication.getContext().getContentResolver().applyBatch(FeedData.AUTHORITY, operations);
                    } catch (Throwable ignored) {

                    } finally {
                        Status().End( status );
                    }
                }
            }.start();

        }
        return countOK;
    }
    public static void downloadEntryImages( final String feedId, final long entryId, final String entryLink, final ArrayList<String> imageList ) {
        final StatusText.FetcherObservable obs = Status();
        final String statusText = getContext().getString(R.string.article_images_downloading);
        int status = obs.Start( statusText, true); try {
            int downloadedCount = 0;
            ExecutorService executor = CreateExecutorService(GetLoadImageThreadCount()); try {
                ArrayList<Future<DownloadResult>> futures = new ArrayList<>();
                for( final String imgPath: imageList ) {
                    futures.add( executor.submit( new Callable<DownloadResult>() {
                        @Override
                        public DownloadResult call() {
                            DownloadResult result = new DownloadResult();
                            result.mResultCount = 0;
                            if ( !isCancelRefresh() && isEntryIDActive( entryId ) ) {
                                try {
                                    if ( NetworkUtils.downloadImage(entryId, entryLink, imgPath, true, false) )
                                        result.mResultCount = 1;
                                } catch (Exception e) {
                                    obs.SetError(entryLink, feedId, String.valueOf(entryId), e);
                                }
                            }
                            return result;
                        }}));
                }
                downloadedCount = FinishExecutionService(statusText, status, futures );
                //Dog.v( "downloadedCount = " + downloadedCount );
            } finally {
                executor.shutdown();
            }
            if ( downloadedCount > 0 )
                EntryView.NotifyToUpdate( entryId, entryLink, false );
        } catch ( Exception e ) {
            obs.SetError(null, "", String.valueOf(entryId), e);
        } finally {
            obs.ResetBytes();
            obs.End(status);
        }
    }

    public static ExecutorService CreateExecutorService( int threadCount ) {
        return Executors.newFixedThreadPool( threadCount, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread( runnable );
                thread.setPriority( MIN_PRIORITY );
                return thread;
            }
        } );
    }

    private static int GetThreadCount() {
        int threadCount = PrefUtils.getIntFromText("thread_count", 5  );
        if ( threadCount < 1 )
            threadCount = 1;
        else if ( threadCount > 100 )
            threadCount = 100;
        return threadCount;
    }
    private static int GetLoadImageThreadCount() {
        int threadCount = PrefUtils.getIntFromText( MAX_IMAGE_DOWNLOAD_COUNT, GetThreadCount() );
        if ( threadCount < 1 )
            threadCount = GetThreadCount();
        else if ( threadCount > 100 )
            threadCount = 100;
        return threadCount;
    }

    private void deleteOldEntries(final long defaultKeepDateBorderTime) {
        if ( isCancelRefresh() )
            return;
        {
            //int status = Status().Start(MainApplication.getContext().getString(R.string.clearingWebViewChache), false);
            UiUtils.RunOnGuiThread(new Runnable() {
                @Override
                public void run() {
                    new WebView(getContext()).clearCache(true );
                }
            });
            //Status().End( status );
        }
        int status = Status().Start(getContext().getString(R.string.deleteOldEntries), false);
        ContentResolver cr = getContext().getContentResolver();
        try ( Cursor cursor = cr.query(FeedColumns.CONTENT_URI,
                                       new String[]{_ID, FeedColumns.OPTIONS},
                                       FeedColumns.LAST_UPDATE + Constants.DB_IS_NOT_NULL + DB_OR + FeedColumns._ID + "=" + GetExtrenalLinkFeedID(), null, null) ) {
            try {
                //mIsDeletingOld = true;
                int index = 1;
                while (cursor.moveToNext() && !isCancelRefresh()) {
                    index++;
                    Status().ChangeProgress(String.format("%d / %d", index, cursor.getCount()));
                    long keepDateBorderTime = defaultKeepDateBorderTime;
                    final String jsonText = cursor.isNull(1) ? "" : cursor.getString(1);
                    if (!jsonText.isEmpty())
                        try {
                            JSONObject jsonOptions = new JSONObject(jsonText);
                            if (jsonOptions.has(CUSTOM_KEEP_TIME))
                                keepDateBorderTime = jsonOptions.getDouble(CUSTOM_KEEP_TIME) == 0 ? 0 : System.currentTimeMillis() - (long) (jsonOptions.getDouble(CUSTOM_KEEP_TIME) * MILLS_IN_DAY);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    final long feedID = cursor.getLong(0);
                    final boolean isDeleteRead = PrefUtils.getBoolean("delete_read_articles", false);
                    if (keepDateBorderTime > 0 || isDeleteRead) {
                        final String deleteRead = isDeleteRead ? DB_OR + WHERE_READ : "";
                        String where = "(" + EntryColumns.DATE + '<' + keepDateBorderTime + deleteRead + ")" + DB_AND + WHERE_NOT_FAVORITE;
                        // Delete the entries, the cache files will be deleted by the content provider
                        cr.delete(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), where, null);
                    }
                }
                EntryUrlVoc.INSTANCE.reinit(true);
            } finally {
                Status().ChangeProgress("");
                Status().End(status);
            }
        }
    }

    private int refreshFeeds(final ExecutorService executor, final long keepDateBorderTime, String groupID, final boolean isFromAutoRefresh) {
        String statusText = "";
        int status = Status().Start( statusText, false ); try {
            final ExecutorService executorInner = CreateExecutorService(GetThreadCount()); try {
                ContentResolver cr = getContentResolver();
                String where = PrefUtils.getBoolean(PrefUtils.REFRESH_ONLY_SELECTED, false) && isFromAutoRefresh ? FeedColumns.IS_AUTO_REFRESH + Constants.DB_IS_TRUE : null;
                final String[] projection = new String[]{FeedColumns._ID, FeedColumns.LAST_UPDATE, FeedColumns.OPTIONS};
                ArrayList<Future<DownloadResult>> futures = new ArrayList<>();
                try ( final Cursor cursor = groupID != null ?
                    cr.query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupID), projection, null, null, null) :
                    cr.query(FeedColumns.CONTENT_URI, projection, where, null, null) ) {
                    while (cursor.moveToNext()) {
                        if (isFromAutoRefresh) {
                            final JSONObject jsonOptions = getJsonObject(cursor);
                            int pos = cursor.getColumnIndex(FeedColumns.LAST_UPDATE);
                            long lastUpdate = !cursor.isNull(pos) ? cursor.getLong(pos) : 0;
                            long interval = getTimeIntervalInMSecs(REFRESH_INTERVAL, DEFAULT_INTERVAL);
                            if (jsonOptions.has(CUSTOM_REFRESH_INTERVAL)) {
                                try {
                                    interval = jsonOptions.getLong(CUSTOM_REFRESH_INTERVAL);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (lastUpdate != 0 && System.currentTimeMillis() - lastUpdate < interval)
                                continue;
                        }
                        final String feedId = cursor.getString(0);
                        futures.add(executor.submit(() -> {
                            DownloadResult result = new DownloadResult();
                            result.mResultCount = 0;
                            try {
                                if (!isCancelRefresh())
                                    result.mResultCount = refreshFeed(executorInner, feedId, keepDateBorderTime);
                            } catch (Exception e) {
                                Status().SetError("", "", "", e);
                            }
                            return result;
                        }));
                    }
                }
                return FinishExecutionService( statusText, status, futures );
            } finally {
                executorInner.shutdown();
            }
        } finally { Status().End( status ); }
    }

    @NotNull
    private JSONObject getJsonObject(Cursor cursor) {
        JSONObject jsonOptions = new JSONObject();
        try {
            jsonOptions = new JSONObject(cursor.getString(cursor.getColumnIndex(FeedColumns.OPTIONS)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonOptions;
    }

    private int refreshFeed( ExecutorService executor, String feedId, long keepDateBorderTime) {
        PrefUtils.putBoolean( STATE_RELOAD_WITH_DEBUG, false );

        int newCount = 0;

        if ( GetExtrenalLinkFeedID().equals( feedId ) )
            return 0;

        ContentResolver cr = getContentResolver();
        try ( Cursor cursor = cr.query(FeedColumns.CONTENT_URI(feedId), null, null, null, null) ) {

            if (cursor.moveToFirst()) {
                int urlPosition = cursor.getColumnIndex(FeedColumns.URL);
                int idPosition = cursor.getColumnIndex(_ID);
                int titlePosition = cursor.getColumnIndex(FeedColumns.NAME);
                //if ( cursor.isNull( cursor.getColumnIndex(FeedColumns.REAL_LAST_UPDATE) ) ) {
                //    keepDateBorderTime = 0;
                //}
                boolean isRss = true;
                boolean isOneWebPage = false;
                try {

                    JSONObject jsonOptions = getJsonObject(cursor);
                    isRss = !jsonOptions.has(IS_RSS) || jsonOptions.getBoolean(IS_RSS);
                    isOneWebPage = jsonOptions.has(IS_ONE_WEB_PAGE) && jsonOptions.getBoolean(IS_ONE_WEB_PAGE);

                    if (jsonOptions.has(CUSTOM_KEEP_TIME))
                        keepDateBorderTime = jsonOptions.getDouble(CUSTOM_KEEP_TIME) == 0 ? 0 : System.currentTimeMillis() - (long) (jsonOptions.getDouble(CUSTOM_KEEP_TIME) * MILLS_IN_DAY);

                    final String feedID = cursor.getString(idPosition);
                    final String feedUrl = cursor.getString(urlPosition);
                    final boolean isLoadImages = NetworkUtils.needDownloadPictures() && (IsAutoDownloadImages(feedID) == AutoDownloadEntryImages.Yes);
                    final int status = Status().Start(cursor.getString(titlePosition), false);
                    try {
                        if (isRss)
                            newCount = ParseRSSAndAddEntries(feedUrl, cursor, keepDateBorderTime, feedID);
                        else if (isOneWebPage)
                            newCount = OneWebPageParser.INSTANCE.parse(keepDateBorderTime, feedID, feedUrl, jsonOptions, isLoadImages, 0);
                        else {
                            newCount = HTMLParser.Parse(executor, feedID, feedUrl, jsonOptions, 0);
                            FetcherService.addEntriesToMobilize(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedId));
                        }
                    } finally {
                        Status().End(status);
                    }
                    newNumber(feedID, DrawerAdapter.NewNumberOperType.Insert, newCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        return newCount;
    }


    private void ShowEventNotification(String text, int captionID, Intent intent, int ID, PendingIntent cancelPI){
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext()) //
                .setContentIntent(contentIntent) //
                .setSmallIcon(R.mipmap.ic_launcher) //
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)) //
                //.setTicker(text) //
                .setWhen(System.currentTimeMillis()) //
                .setAutoCancel(true) //
                .setContentTitle(getString(captionID)) //
                .setLights(0xffffffff, 0, 0);
        if (Build.VERSION.SDK_INT >= 26 ) {
            if (ID == Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT)
                builder.setChannelId(UNREAD_NOTIFICATION_CHANNEL_ID);
            else
                builder.setChannelId(OPERATION_NOTIFICATION_CHANNEL_ID);
            if ( cancelPI != null )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getContext().getString(android.R.string.cancel), cancelPI);
        }
        if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_VIBRATE, false))
            builder.setVibrate(new long[]{0, 1000});

        String ringtone = PrefUtils.getString(PrefUtils.NOTIFICATIONS_RINGTONE, null);
        if (ringtone != null && ringtone.length() > 0)
            builder.setSound(Uri.parse(ringtone));

        if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_LIGHT, false))
            builder.setLights(0xffffffff, 300, 1000);

        Notification nf;
        if (Build.VERSION.SDK_INT < 16)
            nf = builder.setContentText(text).build();
        else
            nf = new NotificationCompat.BigTextStyle(builder.setContentText(text)).bigText(text).build();

        if (Constants.NOTIF_MGR != null)
            Constants.NOTIF_MGR.notify(ID, nf);
    }

    //private Uri getEntryUri(String entryLink) {
    //    return EntryColumns.CONTENT_URI(EntryUrlVoc.INSTANCE.get( entryLink ));
//        Uri entryUri = null;
//        Cursor cursor = MainApplication.getContext().getContentResolver().query(
//                EntryColumns.CONTENT_URI, //ENTRIES_FOR_FEED_CONTENT_URI(feedID),
//                new String[]{_ID},
//                LINK + "='" + entryLink + "'",
//                null,
//                null);
//        if (cursor.moveToFirst())
//            entryUri = EntryColumns.CONTENT_URI(cursor.getLong(0));
//        cursor.close();
//        return entryUri;
    //}


    public static String ToString (InputStream inputStream, Xml.Encoding encoding ) throws
    IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //InputStream inputStream = connection.getInputStream();

        byte[] byteBuffer = new byte[4096];

        int n;
        while ((n = inputStream.read(byteBuffer)) > 0) {
            Status().AddBytes(n);
            outputStream.write(byteBuffer, 0, n);
        }
        String content = outputStream.toString(encoding.name());
        content = CleanRSS(content);

        return content;
    }

    private static String ToString (Reader reader ) {

        Scanner scanner = new Scanner(reader).useDelimiter("\\A");
        String content = scanner.hasNext() ? scanner.next() : "";
        Status().AddBytes(content.length());
        content = CleanRSS(content);
        return content;
    }

    @NotNull
    private static String CleanRSS(String content) {
        content = content.replace(" & ", " &amp; ");
        content = content.replaceAll( "<[a-z]+?:", "<" );
        content = content.replaceAll( "</[a-z]+?:", "</" );
        content = content.replace( "&mdash;", "-" );
        content = content.replace( "&ndash;", "-" );
        content = content.replace((char) 0x1F, ' ');
        content = content.replace((char) 0x02, ' ');
        content = content.replace(String.valueOf((char)0x00), "");
        return content;
    }


    private int ParseRSSAndAddEntries(String feedUrl, Cursor cursor, long keepDateBorderTime, String feedId) {
        RssAtomParser handler = null;

        int fetchModePosition = cursor.getColumnIndex(FeedColumns.FETCH_MODE);
        int realLastUpdatePosition = cursor.getColumnIndex(FeedColumns.REAL_LAST_UPDATE);
        int retrieveFullscreenPosition = cursor.getColumnIndex(FeedColumns.RETRIEVE_FULLTEXT);
        int autoImageDownloadPosition = cursor.getColumnIndex(FeedColumns.IS_IMAGE_AUTO_LOAD);
        int titlePosition = cursor.getColumnIndex(FeedColumns.NAME);
        int urlPosition = cursor.getColumnIndex(FeedColumns.URL);
        int iconUrlPosition = cursor.getColumnIndex(FeedColumns.ICON_URL);

        Connection connection = null;
        ContentResolver cr = getContext().getContentResolver();
        try {

            connection = new Connection( feedUrl, OKHTTP);
            String contentType = connection.getContentType();
            int fetchMode = cursor.getInt(fetchModePosition);

            boolean autoDownloadImages = cursor.isNull(autoImageDownloadPosition) || cursor.getInt(autoImageDownloadPosition) == 1;

            if (fetchMode == 0) {
                if (contentType != null) {
                    int index = contentType.indexOf(CHARSET);

                    if (index > -1) {
                        int index2 = contentType.indexOf(';', index);

                        try {
                            Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8));
                            fetchMode = FETCHMODE_DIRECT;
                        } catch (UnsupportedEncodingException ignored) {
                            fetchMode = FETCHMODE_REENCODE;
                        }
                    } else {
                        fetchMode = FETCHMODE_REENCODE;
                    }

                } else {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    char[] chars = new char[20];

                    int length = bufferedReader.read(chars);

                    FetcherService.Status().AddBytes(length);

                    String xmlDescription = new String(chars, 0, length);

                    connection.disconnect();
                    connection = new Connection(feedUrl, OKHTTP);

                    int start = xmlDescription.indexOf(ENCODING);

                    if (start > -1) {
                        try {
                            Xml.findEncodingByName(xmlDescription.substring(start + 10, xmlDescription.indexOf('"', start + 11)));
                            fetchMode = FETCHMODE_DIRECT;
                        } catch (UnsupportedEncodingException ignored) {
                            fetchMode = FETCHMODE_REENCODE;
                        }
                    } else {
                        // absolutely no encoding information found
                        fetchMode = FETCHMODE_DIRECT;
                    }
                }

                ContentValues values = new ContentValues();
                values.put(FeedColumns.FETCH_MODE, fetchMode);
                cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);
            }

            handler = new RssAtomParser(new Date(cursor.getLong(realLastUpdatePosition)),
                    keepDateBorderTime,
                    feedId,
                    cursor.getString(titlePosition),
                    feedUrl,
                    cursor.getInt(retrieveFullscreenPosition) == 1);
            handler.setFetchImages(NetworkUtils.needDownloadPictures() && autoDownloadImages);

            InputStream inputStream = connection.getInputStream();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String xmlText = ReadAll(inputStream, outputStream);

            if ( xmlText.isEmpty() ) {
                connection.disconnect();
                connection = new Connection( feedUrl, NATIVE );
                inputStream = connection.getInputStream();

                outputStream = new ByteArrayOutputStream();
                xmlText = ReadAll(inputStream, outputStream);
            }

            switch (fetchMode) {
                default:
                case FETCHMODE_DIRECT: {
                    if (contentType != null) {
                        int index = contentType.indexOf(CHARSET);

                        int index2 = contentType.indexOf(';', index);

                        parseXml(//cursor.getString(urlPosition),
                                inputStream,
                                Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8)),
                                handler);

                    } else {
                        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                        Xml.parse( CleanRSS( xmlText ), handler);
                    }
                    break;
                }

                case FETCHMODE_REENCODE: {
                    int start = xmlText.indexOf(ENCODING);

                    if (start > -1) {
                        parseXml( new StringReader(new String(outputStream.toByteArray(),
                                                   xmlText.substring(start + 10,
                                                                     xmlText.indexOf('"', start + 11))).trim()),
                                  handler );
                    } else {
                        // use content type
                        if (contentType != null) {
                            int index = contentType.indexOf(CHARSET);

                            if (index > -1) {
                                int index2 = contentType.indexOf(';', index);

                                try {
                                    StringReader reader = new StringReader(new String(outputStream.toByteArray(), index2 > -1 ? contentType.substring(
                                            index + 8, index2) : contentType.substring(index + 8)));
                                    parseXml(reader, handler);
                                } catch (Exception ignored) {
                                }
                            } else {
                                StringReader reader = new StringReader(new String(outputStream.toByteArray()));
                                parseXml(reader, handler);
                            }
                        }
                    }
                    break;
                }
            }


            connection.disconnect();
        } catch(FileNotFoundException e){
            if (handler == null || (!handler.isDone() && !handler.isCancelled())) {
                ContentValues values = new ContentValues();

                // resets the fetch mode to determine it again later
                values.put(FeedColumns.FETCH_MODE, 0);

                values.put(FeedColumns.ERROR, getString(R.string.error_feed_error));
                cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);
                FetcherService.Status().SetError( cursor.getString(titlePosition) + ": " + getString(R.string.error_feed_error), feedId, "", e);
            }
        } catch(Exception e){
            if (handler == null || (!handler.isDone() && !handler.isCancelled())) {
                ContentValues values = new ContentValues();

                // resets the fetch mode to determine it again later
                values.put(FeedColumns.FETCH_MODE, 0);

                values.put(FeedColumns.ERROR, e.getMessage() != null ? e.getMessage() : getString(R.string.error_feed_process));
                cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);

                FetcherService.Status().SetError(cursor.getString(titlePosition) + ": " + e.toString(),
                        feedId, "", e);
            }
        } finally{
            /* check and optionally find favicon */
            try {
                if (handler != null ) {
                    if (handler.getFeedLink() != null)
                        NetworkUtils.retrieveFavicon(this, new URL(handler.getFeedLink()), feedId);
                    else
                        NetworkUtils.retrieveFavicon(this, new URL( feedUrl ), feedId);
                }
            } catch (Throwable ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
        return handler != null ? handler.getNewCount() : 0;
    }

    @NotNull
    private String ReadAll(InputStream inputStream, ByteArrayOutputStream outputStream) throws IOException {
        String xmlText;
        byte[] byteBuffer = new byte[4096];
        int n;
        while ((n = inputStream.read(byteBuffer)) > 0) {
            FetcherService.Status().AddBytes(n);
            outputStream.write(byteBuffer, 0, n);
        }
        xmlText = outputStream.toString().trim();
        return xmlText;
    }

    private static void parseXml( InputStream in, Xml.Encoding encoding, ContentHandler contentHandler) throws IOException, SAXException {
        Status().ChangeProgress(R.string.parseXml);
        Xml.parse(ToString(in, encoding).trim(), contentHandler);
        Status().ChangeProgress("");
        Status().AddBytes(contentHandler.toString().length());
    }

    private static void parseXml (Reader reader, ContentHandler contentHandler) throws IOException, SAXException {
        Status().ChangeProgress(R.string.parseXml);
        Xml.parse(ToString( reader ), contentHandler);
        Status().ChangeProgress("");
        Status().AddBytes(contentHandler.toString().length());
    }

    public static void cancelRefresh () {
        synchronized (mCancelRefresh) {
            getContext().getContentResolver().delete( TaskColumns.CONTENT_URI, null, null );
            mCancelRefresh = true;
        }
    }

    public static void deleteAllFeedEntries( Uri entriesUri, String condition ){
        int status = Status().Start("deleteAllFeedEntries", true);
        try {
            final ContentResolver cr = getContext().getContentResolver();
            try (final Cursor cursor = cr.query( entriesUri, new String[] {EntryColumns._ID, EntryColumns.LINK}, condition, null, null ) ) {
                while ( cursor.moveToNext() ) {
                    Status().ChangeProgress(String.format("%d/%d", cursor.getPosition(), cursor.getCount()));
                    FileUtils.INSTANCE.deleteMobilizedFile(cursor.getString(1));
                    EntryUrlVoc.INSTANCE.remove(  cursor.getString(1) );
                }
            }
            Status().ChangeProgress( "" );
            cr.delete(entriesUri, condition, null);
            EntryUrlVoc.INSTANCE.reinit( true );
        } finally {
            Status().End(status);
        }

    }

    public static void unstarAllFeedEntries( Uri entriesUri ){
        int status = Status().Start("unstarAllFeedEntries", true);
        try {

            final ContentResolver cr = getContext().getContentResolver();
            try( final Cursor cursor = cr.query( entriesUri, new String[] {EntryColumns._ID}, WHERE_FAVORITE, null, null ) ) {
                SetNotifyEnabled( false ); try {
                    while (cursor.moveToNext()) {

                        Status().ChangeProgress(String.format("%d/%d", cursor.getPosition(), cursor.getCount()));
                        ContentValues values = new ContentValues();
                        values.putNull(EntryColumns.IS_FAVORITE);
                        final long entryID = cursor.getLong(0);
                        cr.update(EntryColumns.CONTENT_URI(entryID), values, null, null);
                        LabelVoc.INSTANCE.removeLabels(entryID);
                    }
                } finally {
                    SetNotifyEnabled( true );
                    notifyChangeOnAllUris( URI_ENTRIES_FOR_FEED, entriesUri );
                }
            }
            Status().ChangeProgress( "" );
        } finally {
            Status().End(status);
        }

    }



//        public static void createTestData () {
//            int status = Status().Start("createTestData", true);
//            try {
//                {
//                    final String testFeedID = "10000";
//                    final String testAbstract1 = "safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd ";
//                    String testAbstract = "";
//                    for (int i = 0; i < 10; i++)
//                        testAbstract += testAbstract1;
//                    //final String testAbstract2 = "sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff";
//
//                    deleteAllFeedEntries(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( testFeedID) );
//
//                    ContentResolver cr = MainApplication.getContext().getContentResolver();
//                    ContentValues values = new ContentValues();
//                    values.put(_ID, testFeedID);
//                    values.put(FeedColumns.NAME, "testFeed");
//                    values.putNull(FeedColumns.IS_GROUP);
//                    //values.putNull(FeedColumns.GROUP_ID);
//                    values.putNull(FeedColumns.LAST_UPDATE);
//                    values.put(FeedColumns.FETCH_MODE, 0);
//                    cr.insert(FeedColumns.CONTENT_URI, values);
//
//                    for (int i = 0; i < 30; i++) {
//                        values.clear();
//                        values.put(_ID, i);
//                        values.put(EntryColumns.ABSTRACT, testAbstract);
//                        values.put(EntryColumns.TITLE, "testTitle" + i);
//                        cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(testFeedID), values);
//                    }
//                }
//
//                {
//                    // small
//                    final String testFeedID = "10001";
//                    final String testAbstract1 = "safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd ";
//                    String testAbstract = "";
//                    for (int i = 0; i < 1; i++)
//                        testAbstract += testAbstract1;
//                    //final String testAbstract2 = "sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff";
//
//                    deleteAllFeedEntries(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI( testFeedID) );
//
//                    ContentResolver cr = MainApplication.getContext().getContentResolver();
//                    ContentValues values = new ContentValues();
//                    values.put(_ID, testFeedID);
//                    values.put(FeedColumns.NAME, "testFeedSmall");
//                    values.putNull(FeedColumns.IS_GROUP);
//                    //values.putNull(FeedColumns.GROUP_ID);
//                    values.putNull(FeedColumns.LAST_UPDATE);
//                    values.put(FeedColumns.FETCH_MODE, 0);
//                    cr.insert(FeedColumns.CONTENT_URI, values);
//
//                    for (int i = 0; i < 30; i++) {
//                        values.clear();
//                        values.put(_ID, 100 + i);
//                        values.put(EntryColumns.ABSTRACT, testAbstract);
//                        values.put(EntryColumns.TITLE, "testTitleSmall" + i);
//                        cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(testFeedID), values);
//                    }
//                }
//            } finally {
//                Status().End(status);
//            }
//
//        }

    public static void StartService(Intent intent, boolean requiresNetwork) {
        final Context context = getContext();

        final boolean isFromAutoRefresh = intent.getBooleanExtra(Constants.FROM_AUTO_REFRESH, false);
        //boolean isOpenActivity = intent.getBooleanExtra(Constants.OPEN_ACTIVITY, false);

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        // Connectivity issue, we quit
        if (requiresNetwork && (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED) ) {
            if (ACTION_REFRESH_FEEDS.equals(intent.getAction()) && !isFromAutoRefresh) {
                // Display a toast in that case
                UiUtils.RunOnGuiThread( new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return;
        }

        boolean skipFetch = requiresNetwork && isFromAutoRefresh && PrefUtils.getBoolean(PrefUtils.REFRESH_WIFI_ONLY, false)
                && networkInfo.getType() != ConnectivityManager.TYPE_WIFI;
        // We need to skip the fetching process, so we quit
        if (skipFetch)
            return;

        if (isFromAutoRefresh && Build.VERSION.SDK_INT < 26 && isBatteryLow())
            return;

        final boolean foreground = !ACTION_MOBILIZE_FEEDS.equals(intent.getAction());
        if (Build.VERSION.SDK_INT >= 26 && foreground)
            context.startForegroundService(intent);
        else
            context.startService( intent );
    }

    static Intent GetStartIntent() {
        return new Intent(getContext(), FetcherService.class)
                .setAction( FetcherService.ACTION_REFRESH_FEEDS );
    }

    void CalculateImageSizes() {
        final int status = Status().Start(R.string.setting_calculate_image_sizes, false ); try {
            {
                ContentValues values = new ContentValues();
                values.put( IMAGES_SIZE, 0 );
                getContentResolver().update( FeedColumns.CONTENT_URI, values, null, null );
            }

            final HashMap<Long, Long> mapEntryIDToSize = new HashMap<>();
            final HashMap<Long, Long> mapFeedIDToSize = new HashMap<>();

            final HashMap<String, Long> mapEntryLinkHashToID = new HashMap<>();
            final HashMap<String, Long> mapEntryLinkHashToFeedID = new HashMap<>();
            try( Cursor cursor = getContentResolver().query(EntryColumns.CONTENT_URI, new String[]{_ID, LINK, FEED_ID}, null, null, null) ) {
                while (cursor.moveToNext()) {
                    final String linkHash = FileUtils.INSTANCE.getLinkHash(cursor.getString(1));
                    mapEntryLinkHashToID.put(linkHash, cursor.getLong(0));
                    mapEntryLinkHashToFeedID.put(linkHash, cursor.getLong(2));
                }
            }

            final HashMap<Long, Long> mapFeedIDToGroupID = new HashMap<>();
            try( Cursor cursor = getContentResolver().query(FeedColumns.CONTENT_URI, new String[]{_ID, GROUP_ID}, GROUP_ID + DB_IS_NOT_NULL, null, null) ) {
                while (cursor.moveToNext())
                    if (!cursor.isNull(1))
                        mapFeedIDToGroupID.put(cursor.getLong(0), cursor.getLong(1));
            }

            File[] files = FileUtils.INSTANCE.GetImagesFolder().listFiles();
            if (isCancelRefresh())
                return;
            int index = 0;
            for (File file : files) {
                index++;
                if ( index % 71 == 0 ) {
                    Status().ChangeProgress(String.format("%d/%d", index, files.length));
                    if (FetcherService.isCancelRefresh())
                        break;
                }
                final String fileName = file.getName();
                final String[] list = TextUtils.split(fileName, "_");
                if (fileName.equals(".nomedia"))
                    continue;
                if (list.length >= 2) {
                    final String entryLinkHash = list[0];
                    if (!mapEntryLinkHashToID.containsKey(entryLinkHash))
                        continue;
                    final long entryID = mapEntryLinkHashToID.get(entryLinkHash);
                    final long feedID = mapEntryLinkHashToFeedID.get(entryLinkHash);
                    final long groupID = mapFeedIDToGroupID.containsKey(feedID) ? mapFeedIDToGroupID.get(feedID) : -1L;
                    final long size = file.length();

                    if (!mapEntryIDToSize.containsKey(entryID))
                        mapEntryIDToSize.put(entryID, size);
                    else
                        mapEntryIDToSize.put(entryID, mapEntryIDToSize.get(entryID) + size);

                    if (!mapFeedIDToSize.containsKey(feedID))
                        mapFeedIDToSize.put(feedID, size);
                    else
                        mapFeedIDToSize.put(feedID, mapFeedIDToSize.get(feedID) + size);

                    if (groupID != -1) {
                        if (!mapFeedIDToSize.containsKey(groupID))
                            mapFeedIDToSize.put(groupID, size);
                        else
                            mapFeedIDToSize.put(groupID, mapFeedIDToSize.get(groupID) + size);
                    }
                }
            }

            Status().ChangeProgress(R.string.applyOperations);
            if (FetcherService.isCancelRefresh())
                return;
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            for (Map.Entry<Long, Long> item : mapEntryIDToSize.entrySet())
                operations.add(ContentProviderOperation.newUpdate(EntryColumns.CONTENT_URI(item.getKey()))
                                   .withValue(EntryColumns.IMAGES_SIZE, item.getValue()).build());
            if (FetcherService.isCancelRefresh())
                return;
            for (Map.Entry<Long, Long> item : mapFeedIDToSize.entrySet())
                operations.add(ContentProviderOperation.newUpdate(FeedColumns.CONTENT_URI(item.getKey()))
                                   .withValue(FeedColumns.IMAGES_SIZE, item.getValue()).build());
            if (FetcherService.isCancelRefresh())
                return;

            if (!operations.isEmpty())
                try {
                    //SetNotifyEnabled( false );
                    getContentResolver().applyBatch(FeedData.AUTHORITY, operations);
                    //SetNotifyEnabled( true );
                    //getContentResolver().notifyChange(FeedColumns.GROUPED_FEEDS_CONTENT_URI, null);
                } catch (Exception e) {
                    DebugApp.AddErrorToLog(null, e);
                }
        } finally {
            Status().ChangeProgress( "" );
            Status().End( status );
        }

    }
    private PendingIntent createCancelPI() {
        Intent intent = new Intent(this, BroadcastActionReciever.class);
        intent.setAction( Action );
        intent.putExtra("FetchingServiceStart", true );
        return PendingIntent.getBroadcast(this, GetPendingIntentRequestCode(), intent, PendingIntent.FLAG_IMMUTABLE);
    }

}
