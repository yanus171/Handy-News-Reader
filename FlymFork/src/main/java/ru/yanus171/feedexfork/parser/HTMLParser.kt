/**
 * Flym
 *
 *
 * Copyright (c) 2012-2015 Frederic Julian
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 *
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.yanus171.feedexfork.parser

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import ru.yanus171.feedexfork.Constants
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.CATEGORY_LIST_SEP
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns
import ru.yanus171.feedexfork.service.FetcherService.*
import ru.yanus171.feedexfork.service.MarkItem
import ru.yanus171.feedexfork.utils.*
import ru.yanus171.feedexfork.utils.EntryUrlVoc.remove
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.regex.Matcher
import java.util.regex.Pattern

object HTMLParser {
    private val TOMORROW_YYYY_MM_DD = "{tomorrow YYYY-MM-DD}"
    @SuppressLint("SimpleDateFormat")
    @JvmStatic
    fun Parse( executor: ExecutorService, feedID: String, feedUrlparam: String, jsonOptions: JSONObject, recursionCount: Int): Int {
        //var feedUrl = feedUrl
        val maxRecursionCount = if ( jsonOptions.has( NEXT_PAGE_MAX_COUNT ) ) jsonOptions.getInt(NEXT_PAGE_MAX_COUNT) else 20
        if (recursionCount > maxRecursionCount)
            return 0
        val statusText = if (recursionCount > 0) {(MainApplication.getContext().getString( R.string.parsing_web_page_links ) + ": " + recursionCount.toString() ) } else "";
        val status = Status().Start( statusText, false)
        val urlNextPageClassName = if ( jsonOptions.has( NEXT_PAGE_MAX_COUNT ) ) jsonOptions.getString(NEXT_PAGE_URL_CLASS_NAME) else ""
        val newEntries: Int
        Status().ChangeProgress("Loading main page")

        val cal = Calendar.getInstance()
        val isTomorrow = feedUrlparam.contains(TOMORROW_YYYY_MM_DD) && cal[Calendar.HOUR_OF_DAY] >= 16
        val feedUrl = replaceTomorrow(feedUrlparam)
        /* check and optionally find favicon */
        try {
            NetworkUtils.retrieveFavicon(MainApplication.getContext(), URL(feedUrl), feedID)
        } catch (ignored: Throwable) {
        }
        var connection: Connection? = null
        var doc: Document? = null
        try {
            connection = Connection(feedUrl)
            doc = Jsoup.parse(connection.inputStream, null, "")
        } catch (e: Exception) {
            Status().SetError(e.localizedMessage, feedID, "", e)
        } finally {
            connection?.disconnect()
        }
        if ( doc == null )
            return 0;

        val cr = MainApplication.getContext().contentResolver
        val entryUri = GetEntryUri( feedUrlparam )
        if ( entryUri != null ) {
            cr.delete(entryUri, null, null);
            EntryUrlVoc.remove( feedUrlparam )
        }
        val filters = FeedFilters(feedID)
        val uriMainEntry = LoadLink(feedID, feedUrlparam, "", filters, ForceReload.Yes, true, false, false, IsAutoDownloadImages(feedID), false, false, false).first
        run {
            val cursor = cr.query(uriMainEntry, arrayOf(EntryColumns.TITLE), null, null, null)!!
            if (cursor.moveToFirst()) {
                val values = ContentValues()
                values.put(FeedColumns.NAME, cursor.getString(0))
                cr.update(FeedColumns.CONTENT_URI(feedID), values, FeedColumns.NAME + Constants.DB_IS_NULL, null)
            }
            cursor.close()
        }

        class Item(var mUrl: String, var mCaption: String){
            override fun equals(other: Any?): Boolean {
                val otherItem: Item = other as Item
                return mUrl == otherItem.mUrl
            }
        }

        val urlNextPage = OneWebPageParser.getUrl(doc, urlNextPageClassName, "a", "href", NetworkUtils.getBaseUrl(feedUrl))

        val listItem = ArrayList<Item>()
        val categoryList = ArrayList<String>()
        val content = ArticleTextExtractor.extractContent(doc, feedUrl, null, filters,
                ArticleTextExtractor.MobilizeType.Yes, categoryList ,false, false)
        doc = Jsoup.parse(content)
        run {
            val list: Elements = doc.select("a")
            val BASE_URL: Pattern = Pattern.compile("(http|https).[/]+[^\"]+")
            for (el: Element in list) {
                if (isCancelRefresh()) break
                var link: String = el.attr("href")
                Dog.v("link before = " + link)
                var matcher: Matcher = BASE_URL.matcher(link)
                if (!matcher.find()) {
                    matcher = BASE_URL.matcher(feedUrl)
                    if (matcher.find()) {
                        link = matcher.group() + "/" + link
                        link = link.replace("//", "/")
                    }
                }
                Dog.v("link after = " + link)
                if (!isLinkToLoad( link )) continue
                if (filters.isEntryFiltered(el.text(), "", link, "", null) )
                    continue
                val newItem = Item(link, el.text())
                if ( !listItem.contains( newItem ) )
                    listItem.add( newItem )
            }
        }
        try {
            val futures = ArrayList<Future<DownloadResult>>()
            for (item: Item in listItem) {
                if (isCancelRefresh()) break
                synchronized(executor) {
                    futures.add(executor.submit(Callable {
                        val result = DownloadResult()
                        result.mAttemptNumber = 0
                        result.mTaskID = 0L
                        result.mResultCount = 0
                        try {
                            val load = LoadLink(feedID, item.mUrl, item.mCaption, filters, ForceReload.No, true, false, false, IsAutoDownloadImages(feedID), true, false, false)
                            val uri = load.first
                            if (load.second) {
                                result.mResultCount = 1
                                val cursor = cr.query(uri, arrayOf(EntryColumns.TITLE, EntryColumns.AUTHOR, EntryColumns.CATEGORIES), null, null, null)
                                if ( cursor != null ) {
                                    if ( cursor.moveToFirst() ) {
                                        val title = cursor.getString(0)
                                        val author = cursor.getString(1)
                                        val categoryList = TextUtils.split(if (cursor.isNull(2)) "" else cursor.getString(2), CATEGORY_LIST_SEP);
                                        if (filters.isMarkAsStarred(title, author, item.mUrl, "", categoryList)) {
                                            synchronized(mMarkAsStarredFoundList) {
                                                mMarkAsStarredFoundList.add(MarkItem(feedID, cursor.getString(0), item.mUrl))
                                            }
                                            val values = ContentValues()
                                            values.put(EntryColumns.IS_FAVORITE, 1)
                                            cr.update(uri, values, null, null)
                                        }
                                        if (isTomorrow) {
                                            val values = ContentValues()
                                            values.put(EntryColumns.DATE, System.currentTimeMillis() + Constants.MILLS_IN_DAY)
                                            cr.update(uri, values, null, null)
                                        }
                                        if (filters.isEntryFiltered(title, author, item.mUrl, "", categoryList)) {
                                            FileUtils.deleteMobilizedFile(item.mUrl);
                                            cr.delete(uri, null, null);
                                            remove(item.mUrl);
                                        }
                                    }
                                    cursor.close()
                                }
                            }
                        } catch (e: Exception) {
                            Status().SetError("", "", "", e);
                        }
                        result
                    }))
                }
            }
            newEntries = FinishExecutionService(statusText, status, futures)
        } finally {
            Status().End(status)
        }
        //        synchronized ( FetcherService.mCancelRefresh ) {
//			FetcherService.mCancelRefresh = false;
//		}
        run {
            val values = ContentValues()
            values.put(FeedColumns.LAST_UPDATE, System.currentTimeMillis())
            cr.update(FeedColumns.CONTENT_URI(feedID), values, null, null)
        }
        run {
            val values = ContentValues()
            values.put(EntryColumns.DATE, System.currentTimeMillis() + (if (isTomorrow) Constants.MILLS_IN_DAY else 0))
            values.put(EntryColumns.SCROLL_POS, 0)
            values.putNull(EntryColumns.IS_READ)
            cr.update(uriMainEntry, values, null, null)
        }
        // img in a tag
/*Matcher matcher = Pattern.compile("<a href=[^>]+>(.)+?</a>").matcher(content);
		while ( matcher.find() ) {
			Document doc = Jsoup.Parse(matcher.group(), null, "");
			//String link = matcher.group().replace( "<a href=\"", "" );
			FetcherService.OpenExternalLink( link, intent.getStringExtra( Constants.TITLE_TO_LOAD ), null  );
		}*/
        return if ( urlNextPage.isEmpty() || isCancelRefresh() )
            newEntries
        else
            Parse( executor, feedID, urlNextPage, jsonOptions, recursionCount + 1 )
    }

    fun replaceTomorrow(feedUrl: String): String {
        val cal = Calendar.getInstance()
        var feedUrl1 = feedUrl
        if (feedUrl1.contains(TOMORROW_YYYY_MM_DD)) {
            val date: Calendar = Calendar.getInstance()
            if ( cal[Calendar.HOUR_OF_DAY] >= 16 )
                date.add(Calendar.DATE, 1)
            feedUrl1 = feedUrl1.replace(TOMORROW_YYYY_MM_DD, SimpleDateFormat("yyyy-MM-dd").format(Date(date.timeInMillis)))
        }
        return feedUrl1
    }
}