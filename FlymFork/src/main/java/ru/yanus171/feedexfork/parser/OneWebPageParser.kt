package ru.yanus171.feedexfork.parser

import android.content.ContentValues
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.yanus171.feedexfork.Constants
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.adapter.DrawerAdapter
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns
import ru.yanus171.feedexfork.service.FetcherService
import ru.yanus171.feedexfork.service.FetcherService.NEXT_PAGE_URL_CLASS_NAME
import ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount
import ru.yanus171.feedexfork.service.MarkItem
import ru.yanus171.feedexfork.utils.*
import ru.yanus171.feedexfork.utils.ArticleTextExtractor.RemoveHiddenElements
import ru.yanus171.feedexfork.utils.HtmlUtils.extractTitle
import java.net.URL
import java.util.*
import java.util.regex.Pattern

const val ONE_WEB_PAGE_TEXT_CLASS_NAME = "oneWebPageTextClassName"
const val ONE_WEB_PAGE_IAMGE_URL_CLASS_NAME = "oneWebPageIconClassName"
const val ONE_WEB_PAGE_DATE_CLASS_NAME = "oneWebPageDateClassName"
const val ONE_WEB_PAGE_AUTHOR_CLASS_NAME = "oneWebPageAuthorClassName"
const val ONE_WEB_PAGE_URL_CLASS_NAME = "oneWebPageUrlClassName"
const val ONE_WEB_PAGE_ARTICLE_CLASS_NAME = "oneWebPageArticleClassName"

object OneWebPageParser {
    fun parse(lastUpdateDate: Long,
              feedID: String,
              feedUrl: String,
              jsonOptions: JSONObject,
              fetchImages: Boolean,
              recursionCount: Int): Int {
        val maxRecursionCount = if ( jsonOptions.has(FetcherService.NEXT_PAGE_MAX_COUNT) ) jsonOptions.getInt(FetcherService.NEXT_PAGE_MAX_COUNT) else 20
        if (recursionCount > maxRecursionCount)
            return 0
        var newCount = 0
        val cr = MainApplication.getContext().contentResolver
        val status = FetcherService.Status().Start(if (recursionCount > 0) MainApplication.getContext().getString(R.string.parsing_one_web_page) + ": " + recursionCount.toString() else "", false)
        var urlNextPage = ""
        try { /* check and optionally find favicon */
            try {
                NetworkUtils.retrieveFavicon(MainApplication.getContext(), URL(feedUrl), feedID)
            } catch (ignored: Throwable) {
            }
            var connection: Connection? = null
            val doc: Document
            try {
                connection = Connection(feedUrl)
                doc = Jsoup.parse(connection.inputStream, null, "")
                val articleClassName = jsonOptions.getString(ONE_WEB_PAGE_ARTICLE_CLASS_NAME)
                val textClassName = jsonOptions.getString(ONE_WEB_PAGE_TEXT_CLASS_NAME)
                val authorClassName = jsonOptions.getString(ONE_WEB_PAGE_AUTHOR_CLASS_NAME)
                val dateClassName = jsonOptions.getString(ONE_WEB_PAGE_DATE_CLASS_NAME)
                val imageUrlClassName = jsonOptions.getString(ONE_WEB_PAGE_IAMGE_URL_CLASS_NAME)
                val urlClassName = jsonOptions.getString(ONE_WEB_PAGE_URL_CLASS_NAME)
                val urlNextPageClassName = jsonOptions.getString(NEXT_PAGE_URL_CLASS_NAME)
                if ( articleClassName.isEmpty() )
                    return 1;
                val articleList = doc.getElementsByClass(articleClassName)
                val feedEntriesUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID)
                val feedBaseUrl = NetworkUtils.getBaseUrl(feedUrl)
                val filters = FeedFilters(feedID)
                var now = Date().time
                for (elArticle in articleList) {
                    if ( FetcherService.isCancelRefresh() )
                        return newCount;
                    val author = getValue(authorClassName, elArticle)
                    var date = 0L
                    for ( item in dateClassName.split(" ") ) {
                        val tempDate = getDate(elArticle, item)
                        if ( tempDate > date )
                            date = tempDate
                    }
                    if (date in 1 until lastUpdateDate)
                        continue
                    if ( date == 0L )
                        date = now--

                    var entryUrl = getUrl(elArticle, urlClassName, "div", "data-post", feedBaseUrl)
                    if ( entryUrl.isEmpty() )
                        entryUrl = getUrl(elArticle, urlClassName, "a", "href", feedBaseUrl)
                    val mainImageUrl = getUrl(elArticle, imageUrlClassName, "img", "src", feedBaseUrl)
                    var content = getValueHTML(textClassName, elArticle)
                    //if ( mainImageUrl.isNotEmpty() )
                    //    textHTML = "<img src='$mainImageUrl'/><p>$textHTML"
                    val isAutoFullTextRoot = ArticleTextExtractor.getFullTextRootElementFromPref(doc, entryUrl) == null
                    content = content.replace(">[\\n|\\s|\\.|\\t]+".toRegex(), "> ")
                    val categoryList = ArrayList<String>()
                    content = HtmlUtils.improveHtmlContent(content, feedBaseUrl, filters, categoryList, ArticleTextExtractor.MobilizeType.No, isAutoFullTextRoot)
                    val title = extractTitle( content )
                    content = content.replaceFirst( title, "" )

                    // Try to find if the entry is not filtered and need to be processed
                    if (!filters.isEntryFiltered(title, author, entryUrl, content, null)) {
                        var isUpdated = false
                        var entryID = 0L
                        val cursor = cr.query(feedEntriesUri, arrayOf(EntryColumns.DATE, EntryColumns._ID), EntryColumns.LINK + Constants.DB_ARG, arrayOf(entryUrl), null)
                        if ( cursor != null ) {
                            if (cursor.moveToFirst()) {
                                entryID = cursor.getLong(1)
                                if (cursor.isNull(0) && cursor.getLong(0) > date)
                                    isUpdated = true
                            }
                            cursor.close()
                        }
                        val values = ContentValues()
                        values.put(EntryColumns.SCROLL_POS, 0)
                        values.put(EntryColumns.TITLE, title)
                        values.put(EntryColumns.ABSTRACT, content)
                        values.put(EntryColumns.IMAGE_URL, mainImageUrl)
                        values.put(EntryColumns.AUTHOR, author)
                        values.put(EntryColumns.GUID, entryUrl)
                        values.put(EntryColumns.LINK, entryUrl)
                        values.put(EntryColumns.FETCH_DATE, date)
                        values.put(EntryColumns.DATE, date)
                        if ( categoryList.isNotEmpty() )
                            values.put(EntryColumns.CATEGORIES, categoryList.joinToString(EntryColumns.CATEGORY_LIST_SEP))
                        values.putNull(EntryColumns.MOBILIZED_HTML)

                        if ( isUpdated ) {
                            values.put(EntryColumns.IS_READ, 0)
                            values.put(EntryColumns.IS_NEW, 1)
                            cr.update(EntryColumns.CONTENT_URI(entryID), values, null, null)
                        } else if ( entryID == 0L ) {
                            if (filters.isMarkAsStarred(author, author, entryUrl, content, null)) {
                                values.put(EntryColumns.IS_FAVORITE, 1)
                                values.put(EntryColumns.READ_DATE, Date().time)
                            }
                            entryID = cr.insert(feedEntriesUri, values)!!.lastPathSegment!!.toLong()
                            newCount++
                        }
                        EntryUrlVoc.set(entryUrl, entryID)
                        val imagesToDl = ArrayList<String>()
                        if ( mainImageUrl.isNotEmpty() )
                            imagesToDl.add(mainImageUrl)
                        HtmlUtils.replaceImageURLs(content, "", entryID, entryUrl, true, imagesToDl, null, mMaxImageDownloadCount)
                        FetcherService.addImagesToDownload(entryID.toString(), imagesToDl)
                    }
                }
                urlNextPage = getUrl(doc, urlNextPageClassName, "a", "href", feedBaseUrl)
            } catch (e: Exception) {
                FetcherService.Status().SetError(e.localizedMessage, feedID, "", e)
            } finally {
                connection?.disconnect()
            }

            //        synchronized ( FetcherService.mCancelRefresh ) {
//			FetcherService.mCancelRefresh = false;
//		}

        } finally {
            FetcherService.Status().End(status)
        }
        if ( urlNextPage.isNotEmpty() )
            newCount += parse(lastUpdateDate, feedID, urlNextPage, jsonOptions, fetchImages, recursionCount + 1)
        else {
            val values = ContentValues()
            values.put(FeedColumns.LAST_UPDATE, System.currentTimeMillis())
            cr.update(FeedColumns.CONTENT_URI(feedID), values, null, null)
        }
        return newCount
    }

    private fun getDate(elArticle: Element, dateClassName: String): Long {
        var result = 0L
        if ( dateClassName.isEmpty() )
            return result;
        val list = elArticle.getElementsByClass(dateClassName)
        val now = Calendar.getInstance().timeInMillis
        if ( list.isNotEmpty() )
            for (item in list.first()!!.allElements) {
                val timeTag = item.getElementsByTag("time").first()
                if ( timeTag != null && timeTag.hasAttr("datetime") ) {
                    result = RssAtomParser.parseDate(timeTag.attr("datetime"), now).time
                    break
                } else if (item.hasText()) {
                    try {
                        result = RssAtomParser.parseDate(item.text(), now).time
                        break
                    } catch (ignored: Exception) {
                    }
                }
            }
        return result
    }

    fun getUrl(elArticle: Element, urlClassName: String, tag: String, attrName: String, feedBaseUrl: String): String {
        var result = ""
        if ( urlClassName.isEmpty() )
            return result;
        val list = elArticle.getElementsByClass(urlClassName)
        if (!list.isEmpty()) {
            val listA = list.first()!!.getElementsByTag(tag)
            if (!listA.isEmpty()) {
                result = listA.first()!!.attr(attrName)
                if ( result.startsWith("//") )
                    result = "http:$result"
                else if (!result.startsWith("http") ) {
                    val match = Pattern.compile("\\d+").matcher(result);
                    result = if ( match.find() )
                        feedBaseUrl + "/" + match.group(0);
                    else
                        feedBaseUrl + result
                    if ( result.contains("t.me/s/") )
                        result = result.replace("/s/", "/")
                }
            }

        }
        return result
    }

    private fun getValue(className: String, elArticle: Element): String {
        var result = ""
        if (className.isNotEmpty()) {
            val list = elArticle.getElementsByClass(className)
            if (!list.isEmpty()) result = list.first()!!.text()
        }
        return result
    }
//    private fun getValueOwnText(className: String, elArticle: Element): String {
//        var result = ""
//        if (className.isNotEmpty()) {
//            val list = elArticle.getElementsByClass(className)
//            if (!list.isEmpty()) result = list.first().ownText()
//        }
//        return result
//    }
    private fun getValueHTML(className: String, elArticle: Element): String {
        var result = ""
        if (className.isNotEmpty()) {
            val list = elArticle.getElementsByClass(className)
            if (!list.isEmpty()) {
                RemoveHiddenElements(list.first())
                result = list.first()!!.html()
            }
        }
        return result
    }
}
