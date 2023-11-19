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

import static ru.yanus171.feedexfork.provider.FeedData.EntryColumns.CATEGORY_LIST_SEP;
import static ru.yanus171.feedexfork.provider.FeedData.PutFavorite;
import static ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount;
import static ru.yanus171.feedexfork.utils.HtmlUtils.unescapeTitle;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.HtmlUtils;

public class RssAtomParser extends DefaultHandler {
    private static final String HTML_TEXT = "text/html";

    private static final String TAG_RSS = "rss";
    private static final String TAG_RDF = "rdf";
    private static final String TAG_FEED = "feed";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_ITEM = "item";
    private static final String TAG_UPDATED = "updated";
    private static final String TAG_TITLE = "title";
    private static final String TAG_LINK = "link";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_MEDIA_DESCRIPTION = "media:description";
    private static final String TAG_MEDIA_THUMBNAIL = "media:thumbnail";
    private static final String TAG_CONTENT = "content";
    private static final String TAG_CONTENT_ENCODED = "content:encoded>";
    private static final String TAG_MEDIA_CONTENT = "media:content";
    private static final String TAG_ENCODED_CONTENT = "encoded";
    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_PUBDATE = "pubDate";
    private static final String TAG_PUBLISHED = "published";
    private static final String TAG_DATE = "date";
    private static final String TAG_LAST_BUILD_DATE = "lastBuildDate";
    private static final String TAG_ENCLOSURE = "enclosure";
    private static final String TAG_ALTERNATE = "alternate";
    private static final String TAG_RELATED = "related";
    private static final String TAG_VIA = "via";
    private static final String TAG_GUID = "guid";
    private static final String TAG_AUTHOR = "author";
    private static final String TAG_CREATOR = "creator";
    private static final String TAG_NAME = "name";

    private static final String ATTRIBUTE_URL = "url";
    private static final String ATTRIBUTE_HREF = "href";
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_LENGTH = "length";
    private static final String ATTRIBUTE_REL = "rel";
    private static final String ATTRIBUTE_TERM = "term";

    private static final String[][] TIMEZONES_REPLACE = {
            {"MEST", "+0200"},
            {"EST", "-0500"},
            {"PST", "-0800"},
            {"ICT", "+0700"}};

    private static final DateFormat[] DATE_FORMATS = {
        new SimpleDateFormat("MM/dd/yyyy", Locale.US),
        new SimpleDateFormat("HH:mm' 'dd.MM.yyyy", Locale.US),
        new SimpleDateFormat("EEE,' 'd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US),
        new SimpleDateFormat("EEE,' 'd' 'MMM' 'yy' 'HH:mm:ss' 'Z", Locale.US),
        new SimpleDateFormat("EEE,' 'd' 'MMM' 'yy' 'HH:mm:ss' 'z", Locale.US),
        new SimpleDateFormat("d' 'MMM' 'yy' 'HH:mm:ss' 'Z", Locale.US),
        new SimpleDateFormat("d' 'MMM' 'yy' 'HH:mm:ss' 'z", Locale.US),
        new SimpleDateFormat("d' 'MMM' 'yy' 'HH:mm:ss", Locale.US),
        new SimpleDateFormat("d' 'MMM' 'yy' 'HH:mm", Locale.US),
        new SimpleDateFormat("dd-MM-yyyy' 'HH:mm:ss' 'Z", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss' 'Z", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ssZ", Locale.US),
        new SimpleDateFormat("dd-MM-yyyy' 'HH:mm:ss' 'z", Locale.US),
        new SimpleDateFormat("dd-MM-yyyy' 'HH:mm:ss", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSz", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd", Locale.US),
        new SimpleDateFormat("dd.MM.yyyy", Locale.US),
        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US),
        new SimpleDateFormat("HH:mm", Locale.US),
        new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

    };
    private static final String TAG_CATEGORY = "category";


    private final Date mRealLastUpdateDate;
    private final String mId;
    private final Uri mFeedEntriesUri;
    private final String mFeedName;
    private final String mFeedBaseUrl;
    private final Date mKeepDateBorder;
    private final FeedFilters mFilters;
    private final ArrayList<ContentProviderOperation> mInserts = new ArrayList<>();
    private final ArrayList<ArrayList<String>> mInsertedEntriesImages = new ArrayList<>();
    private long mNewRealLastUpdate;
    private boolean mEntryTagEntered = false;
    private boolean mTitleTagEntered = false;
    private boolean mUpdatedTagEntered = false;
    private boolean mLinkTagEntered = false;
    private boolean mDescriptionTagEntered = false;
    private boolean mPubDateTagEntered = false;
    private boolean mPublishedTagEntered = false;
    private boolean mDateTagEntered = false;
    private boolean mLastBuildDateTagEntered = false;
    private boolean mGuidTagEntered = false;
    private boolean mAuthorTagEntered = false;
    private StringBuilder mTitle;
    private StringBuilder mDateStringBuilder;
    private String mFeedLink;
    private Date mEntryDate;
    private Date mEntryUpdateDate;
    private Date mPreviousEntryDate;
    private Date mPreviousEntryUpdateDate;
    private StringBuilder mEntryLink;
    private StringBuilder mDescription;
    private StringBuilder mDescriptionTemp;
    private StringBuilder mEnclosure;
    private int mNewCount = 0;
    private String mFeedTitle;
    private boolean mDone = false;
    private boolean mFetchImages = false;
    private boolean mRetrieveFullText;
    private boolean mCancelled = false;
    private long mNow = System.currentTimeMillis();
    private StringBuilder mGuid;
    private StringBuilder mAuthor, mTmpAuthor;
    private ArrayList<String> mCategoryList = new ArrayList<>();
    private StringBuilder mTmpCategory = null;
    private boolean mCategoryTagEntered = false;
    private String mMainImageUrl = null;
    private boolean mGuidHasLink = false;

    public RssAtomParser(Date realLastUpdateDate, long keepDateBorderTime, final String id, String feedName, String url, boolean retrieveFullText) {
        mKeepDateBorder = new Date(keepDateBorderTime);
        mRealLastUpdateDate = realLastUpdateDate;
        mNewRealLastUpdate = realLastUpdateDate.getTime();
        mId = id;
        mFeedName = feedName;
        mFeedEntriesUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(id);
        mRetrieveFullText = retrieveFullText;

        mFilters = new FeedFilters(id);

        mFeedBaseUrl = url;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (TAG_UPDATED.equals(localName)) {
            mUpdatedTagEntered = true;
            mDateStringBuilder = new StringBuilder();
        } else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
            mEntryTagEntered = true;
            mDescription = null;
            mDescriptionTemp = null;
            mEntryLink = null;
            mCategoryList.clear();
            mTmpCategory = new StringBuilder();

            // Save the previous (if no date are found for this entry)
            mPreviousEntryDate = mEntryDate;
            mPreviousEntryUpdateDate = mEntryUpdateDate;
            mEntryDate = null;
            mEntryUpdateDate = null;

            // This is the retrieved feed title
            if (mFeedTitle == null && mTitle != null && mTitle.length() > 0) {
                mFeedTitle = mTitle.toString();
            }
            mTitle = null;
        } else if (TAG_TITLE.equals(localName)) {
            if (mTitle == null) {
                mTitleTagEntered = true;
                mTitle = new StringBuilder();
            }
        } else if (TAG_LINK.equals(localName)) {
            if (mAuthorTagEntered) {
                return;
            }
            if (TAG_ENCLOSURE.equals(attributes.getValue("", ATTRIBUTE_REL))) {
                startEnclosure(attributes, attributes.getValue("", ATTRIBUTE_HREF));
                //} else if(TAG_ALTERNATE.equals(attributes.getValue("", ATTRIBUTE_REL))) {
            } else if(TAG_RELATED.equals(attributes.getValue("", ATTRIBUTE_REL))) {
            } else if(TAG_VIA.equals(attributes.getValue("", ATTRIBUTE_REL))) {
            } else {
                // Get the link only if we don't have one or if its the good one (html)
                if (mEntryLink == null || HTML_TEXT.equals(attributes.getValue("", ATTRIBUTE_TYPE))) {
                    mEntryLink = new StringBuilder();

                    boolean foundLink = false;
                    String href = attributes.getValue("", ATTRIBUTE_HREF);
                    if (!TextUtils.isEmpty(href)) {
                        mEntryLink.append(href);
                        foundLink = true;
                        mLinkTagEntered = false;
                    } else {
                        mLinkTagEntered = true;
                    }

                    if (!foundLink) {
                        mLinkTagEntered = true;
                    }
                }
            }
        } else if ( TAG_DESCRIPTION.equals(localName) || TAG_MEDIA_DESCRIPTION.equals(qName)
                || TAG_CONTENT.equals(localName) || TAG_MEDIA_CONTENT.equals(qName) || TAG_CONTENT_ENCODED.equals(qName) ) {
            mDescriptionTagEntered = true;
            mDescriptionTemp = new StringBuilder();
        } else if (TAG_SUMMARY.equals(localName)) {
            if (mDescriptionTemp == null) {
                mDescriptionTagEntered = true;
                mDescriptionTemp = new StringBuilder();
            }
        } else if ( TAG_MEDIA_THUMBNAIL.equals( qName ) ) {
            mMainImageUrl = attributes.getValue( "", "url" );
        } else if (TAG_PUBDATE.equals(localName)) {
            mPubDateTagEntered = true;
            mDateStringBuilder = new StringBuilder();
        } else if (TAG_PUBLISHED.equals(localName)) {
            mPublishedTagEntered = true;
            mDateStringBuilder = new StringBuilder();
        } else if (TAG_DATE.equals(localName)) {
            mDateTagEntered = true;
            mDateStringBuilder = new StringBuilder();
        } else if (TAG_LAST_BUILD_DATE.equals(localName)) {
            mLastBuildDateTagEntered = true;
            mDateStringBuilder = new StringBuilder();
        } else if (TAG_ENCODED_CONTENT.equals(localName)) {
            mDescriptionTagEntered = true;
            mDescriptionTemp = new StringBuilder();
        } else if (TAG_ENCLOSURE.equals(localName)) {
            startEnclosure(attributes, attributes.getValue("", ATTRIBUTE_URL));
        } else if (TAG_GUID.equals(localName)) {
            mGuidTagEntered = true;
            mGuid = new StringBuilder();
            mGuidHasLink = "true".equals( attributes.getValue(  "isPermaLink" ) );

        } else if (TAG_NAME.equals(localName) || TAG_AUTHOR.equals(localName) || TAG_CREATOR.equals(localName)) {
            mAuthorTagEntered = true;
            if (mTmpAuthor == null) {
                mTmpAuthor = new StringBuilder();
            }
        } else if (TAG_CATEGORY.equals(localName)) {
            mCategoryTagEntered = true;
            if (mTmpCategory == null) {
                mTmpCategory = new StringBuilder();
            }
            if ( attributes.getValue("", ATTRIBUTE_TERM) != null )
                mTmpCategory.append( attributes.getValue("", ATTRIBUTE_TERM) );
        }
    }

    private void startEnclosure(Attributes attributes, String url) {
        if (mEnclosure == null && url != null) { // fetch the first enclosure only
            mEnclosure = new StringBuilder(url);
            mEnclosure.append(Constants.ENCLOSURE_SEPARATOR);

            String value = attributes.getValue("", ATTRIBUTE_TYPE);

            if (value != null) {
                mEnclosure.append(value);
            }
            mEnclosure.append(Constants.ENCLOSURE_SEPARATOR);
            value = attributes.getValue("", ATTRIBUTE_LENGTH);
            if (value != null) {
                mEnclosure.append(value);
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
            if (mTitleTagEntered) {
                mTitle.append(ch, start, length);
            } else if (mLinkTagEntered) {
                mEntryLink.append(ch, start, length);
            } else if (mDescriptionTagEntered) {
                mDescriptionTemp.append(ch, start, length);
            } else if (mUpdatedTagEntered || mPubDateTagEntered || mPublishedTagEntered || mDateTagEntered || mLastBuildDateTagEntered) {
                mDateStringBuilder.append(ch, start, length);
            } else if (mGuidTagEntered) {
                mGuid.append(ch, start, length);
            } else if (mAuthorTagEntered) {
                mTmpAuthor.append(ch, start, length);
            } else if (mCategoryTagEntered) {
                mTmpCategory.append(ch, start, length);
            }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
            if (TAG_TITLE.equals(localName)) {
                mTitleTagEntered = false;
            } else if (TAG_DESCRIPTION.equals(localName) || TAG_MEDIA_DESCRIPTION.equals(qName) || TAG_SUMMARY.equals(localName)
                    || TAG_CONTENT.equals(localName) || TAG_MEDIA_CONTENT.equals(qName) || TAG_CONTENT_ENCODED.equals(qName) || TAG_ENCODED_CONTENT.equals(localName)) {
                mDescriptionTagEntered = false;
                if ( mDescription == null || (mDescriptionTemp != null && mDescriptionTemp.length() > mDescription.length() ) )
                    mDescription = mDescriptionTemp;
            } else if (TAG_LINK.equals(localName)) {
                mLinkTagEntered = false;

                if (mFeedLink == null && !mEntryTagEntered && TAG_LINK.equals(qName)) { // Skip <atom10:link> tags
                    mFeedLink = mEntryLink.toString();
                }
            } else if (TAG_UPDATED.equals(localName)) {
                mEntryUpdateDate = parseDate(mDateStringBuilder.toString(), mNow);
                mUpdatedTagEntered = false;
            } else if (TAG_PUBDATE.equals(localName)) {
                mEntryDate = parseDate(mDateStringBuilder.toString(), mNow);
                mPubDateTagEntered = false;
            } else if (TAG_CATEGORY.equals(localName)) {
                mCategoryList.add(mTmpCategory.toString());
                mTmpCategory = new StringBuilder();
                mCategoryTagEntered = false;
            } else if (TAG_PUBLISHED.equals(localName)) {
                mEntryDate = parseDate(mDateStringBuilder.toString(), mNow);
                mPublishedTagEntered = false;
            } else if (TAG_LAST_BUILD_DATE.equals(localName)) {
                mEntryDate = parseDate(mDateStringBuilder.toString(), mNow);
                mLastBuildDateTagEntered = false;
            } else if (TAG_DATE.equals(localName)) {
                mEntryDate = parseDate(mDateStringBuilder.toString(), mNow);
                mDateTagEntered = false;
            } else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
                mEntryTagEntered = false;

                boolean updateOnly = false;
                // Old mEntryDate but recent update date => we need to not insert it!
                if (mEntryUpdateDate != null && mEntryDate != null && (mEntryDate.before(mRealLastUpdateDate) || mEntryDate.before(mKeepDateBorder))) {
                    updateOnly = true;
                    if (mEntryUpdateDate.after(mEntryDate)) {
                        mEntryDate = mEntryUpdateDate;
                    }
                } else if (mEntryDate == null && mEntryUpdateDate != null) { // only one updateDate, copy it into mEntryDate
                    mEntryDate = mEntryUpdateDate;
                } else if (mEntryDate == null && mEntryUpdateDate == null) { // nothing, we need to retrieve the previous date
                    mEntryDate = mPreviousEntryDate;
                    mEntryUpdateDate = mPreviousEntryUpdateDate;
                }
                if ( mTitle == null && mDescription != null )
                    mTitle = new StringBuilder( HtmlUtils.extractTitle(mDescription.toString()) );
                if (mTitle != null && (mEntryDate == null || (mEntryDate.after(mRealLastUpdateDate) && mEntryDate.after(mKeepDateBorder)))) {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.SCROLL_POS, 0);
                    if ( !mCategoryList.isEmpty() )
                        values.put(EntryColumns.CATEGORIES, TextUtils.join(CATEGORY_LIST_SEP, mCategoryList ) );
                    if (mEntryDate != null && mEntryDate.getTime() > mNewRealLastUpdate) {
                        mNewRealLastUpdate = mEntryDate.getTime();
                    }


                    String entryLinkString = ""; // don't set this to null as we need *some* value
                    if (mEntryLink != null && mEntryLink.length() > 0) {
                        entryLinkString = mEntryLink.toString().trim();
                        if (mFeedBaseUrl != null && !entryLinkString.startsWith(Constants.HTTP_SCHEME) && !entryLinkString.startsWith(Constants.HTTPS_SCHEME)) {
                            entryLinkString = mFeedBaseUrl
                                + (entryLinkString.startsWith(Constants.SLASH) ? entryLinkString : Constants.SLASH + entryLinkString);
                        }
                    }
                    if ( entryLinkString.isEmpty() && mGuidHasLink )
                        entryLinkString = mGuid.toString();

                    String improvedTitle = unescapeTitle(mTitle.toString().trim());
                    values.put(EntryColumns.TITLE, improvedTitle);

                    String improvedContent = null;
                    String mainImageUrl = mMainImageUrl;
                    ArrayList<String> imagesUrls = null;
                    if (mDescription != null) {
                        // Improve the description
                        improvedContent = HtmlUtils.improveHtmlContent(mDescription.toString(), entryLinkString, mFilters, mCategoryList, ArticleTextExtractor.MobilizeType.Yes, true);

                        if ( improvedTitle.isEmpty() ) {
                            improvedTitle = HtmlUtils.extractTitle(improvedContent);
                            values.put(EntryColumns.TITLE, improvedTitle);
                        }
                        imagesUrls = new ArrayList<>();
                        if ( mainImageUrl != null ) {
                            imagesUrls.add(mainImageUrl);
                            if ( !improvedContent.contains( mMainImageUrl ) )
                                improvedContent = String.format( "<img src=\"%s\" />", mMainImageUrl ) + improvedContent;
                        }
                        if (mFetchImages) {
                            //imagesUrls = HtmlUtils.getImageURLs(improvedContent);
                            HtmlUtils.replaceImageURLs( improvedContent, "", -1, entryLinkString, true, imagesUrls, null, mMaxImageDownloadCount );
                            if ( mainImageUrl == null && !imagesUrls.isEmpty() ) {
                                mainImageUrl = HtmlUtils.getMainImageURL(imagesUrls);
                            }
                        } else if ( mainImageUrl == null )
                            mainImageUrl = HtmlUtils.getMainImageURL(improvedContent);

                        if (improvedContent != null)
                            values.put(EntryColumns.ABSTRACT, improvedContent);
                    }

                    if ( mainImageUrl != null )
                        values.put(EntryColumns.IMAGE_URL, mainImageUrl);
                    mMainImageUrl = null;

                    String improvedAuthor = "";
                    if ( mAuthor != null )
                        improvedAuthor = mAuthor.toString();

                    // Try to find if the entry is not filtered and need to be processed
                    if (!mFilters.isEntryFiltered(improvedTitle, improvedAuthor, entryLinkString, improvedContent, mCategoryList.toArray(new String[0]))) {

                        if (mAuthor != null) {
                            values.put(EntryColumns.AUTHOR, mAuthor.toString());
                        }

                        String enclosureString = null;
                        StringBuilder existenceStringBuilder = new StringBuilder(EntryColumns.LINK).append(Constants.DB_ARG);

                        if (mEnclosure != null && mEnclosure.length() > 0) {
                            enclosureString = mEnclosure.toString();
                            values.put(EntryColumns.ENCLOSURE, enclosureString);
                            existenceStringBuilder.append(Constants.DB_AND).append(EntryColumns.ENCLOSURE).append(Constants.DB_ARG);
                        }

                        String guidString = null;

                        if (mGuid != null && mGuid.length() > 0) {
                            guidString = mGuid.toString();
                            values.put(EntryColumns.GUID, guidString);
                            existenceStringBuilder.append(Constants.DB_AND).append(EntryColumns.GUID).append(Constants.DB_ARG);
                        }

                        String[] existenceValues = enclosureString != null ? (guidString != null ? new String[]{entryLinkString, enclosureString,
                                guidString} : new String[]{entryLinkString, enclosureString}) : (guidString != null ? new String[]{entryLinkString,
                                guidString} : new String[]{entryLinkString});

                        // First, try to update the feed
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        boolean isUpdated = (!entryLinkString.isEmpty() || guidString != null)
                                && cr.update(mFeedEntriesUri, values, existenceStringBuilder.toString(), existenceValues) != 0;


                        // Insert it only if necessary
                        if (!isUpdated && !updateOnly) {
                            // We put the date only for new entry (no need to change the past, you may already read it)
                            if (mEntryDate != null) {
                                values.put(EntryColumns.DATE, mEntryDate.getTime());
                            } else {
                                values.put(EntryColumns.DATE, mNow--); // -1 to keep the good entries order
                            }

                            values.put(EntryColumns.LINK, entryLinkString);
                            if ( mFilters.isMarkAsStarred(improvedTitle, improvedAuthor, entryLinkString, improvedContent, mCategoryList.toArray(new String[0]) ) )
                                PutFavorite( values, true );

                            // We cannot update, we need to insert it
                            mInsertedEntriesImages.add(imagesUrls);
                            mInserts.add(ContentProviderOperation.newInsert(mFeedEntriesUri).withValues(values).build());
                            mNewCount++;
                        }

                        // No date, but we managed to update an entry => we already parsed the following entries and don't need to continue
                        if (isUpdated && mEntryDate == null) {
                            cancel();
                        }
                    }

                //} else {
                    //cancel();
                }
                mDescription = null;
                mTitle = null;
                mEnclosure = null;
                mGuid = null;
                mAuthor = null;
            } else if (TAG_RSS.equals(localName) || TAG_RDF.equals(localName) || TAG_FEED.equals(localName)) {
                mDone = true;
            } else if (TAG_GUID.equals(localName)) {
                mGuidTagEntered = false;
            } else if (TAG_NAME.equals(localName) || TAG_AUTHOR.equals(localName) || TAG_CREATOR.equals(localName)) {
                mAuthorTagEntered = false;
                mCategoryTagEntered = false;

                if (mTmpAuthor != null && mTmpAuthor.indexOf("@") == -1) { // no email
                    if (mAuthor == null) {
                        mAuthor = new StringBuilder(mTmpAuthor);
                    } else { // this indicates multiple authors
                        boolean found = false;
                        for (String previousAuthor : mAuthor.toString().split(",")) {
                            if (previousAuthor.equals(mTmpAuthor.toString())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            mAuthor.append(Constants.COMMA_SPACE);
                            mAuthor.append(mTmpAuthor);
                        }
                    }
                }

                mTmpAuthor = null;
            }
    }

    public String getFeedLink() {
        return mFeedLink;
    }

    public int getNewCount() {
        return mNewCount;
    }

    public boolean isDone() {
        return mDone;
    }

    public boolean isCancelled() {
        return mCancelled;
    }

    private void cancel() throws SAXException {
        if (!mCancelled) {
            mCancelled = true;
            mDone = true;
            endDocument();

            throw new SAXException("Finished");
        }
    }

    public void setFetchImages(boolean fetchImages) {
        this.mFetchImages = fetchImages;
    }

    public static Date parseDate(String dateStr, long now) {
        Dog.d( "parseDate " + dateStr );
        long dateBorder = 30 * 365 * 24 * 60 * 60 * 1000L; // twenty years ago
        dateStr = improveDateString(dateStr);
        Date result = new Date();
        for (DateFormat format : DATE_FORMATS ) {
            try {
                result = format.parse(dateStr);
                if ( now == 0 )
                    return result;
                if ( now - result.getTime() > dateBorder )
                    continue;
                if ( Math.abs( result.getTime() -  now ) < dateBorder )
                    return (result.getTime() > now ? new Date(now) : result);
            } catch (ParseException ignored) {

            } // just do nothing

        }
        if (  now - result.getTime() > dateBorder ) {
            Calendar today = Calendar.getInstance();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis( result.getTime() );
            cal.set( today.get( Calendar.YEAR ), today.get( Calendar.MONTH ), today.get( Calendar.DATE ) );
            if ( cal.after( today ) )
                cal.add( Calendar.DATE, -1 );
            return new Date( cal.getTimeInMillis() );
        }

        return null;
    }

    static String improveDateString(String dateStr) {
        // We remove the first part if necessary (the day display)
//        int coma = dateStr.indexOf(", ");
//        if (coma != -1) {
//            dateStr = dateStr.substring(coma + 2);
//        }

        dateStr = dateStr.replaceAll("([0-9])T([0-9])", "$1 $2")
                .replaceAll("Z$", "")
                .replaceAll("  ", " ")
                .trim(); // fix useless char

        // Replace bad timezones
        for (String[] timezoneReplace : TIMEZONES_REPLACE) {
            dateStr = dateStr.replace(timezoneReplace[0], timezoneReplace[1]);
        }

        return dateStr;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        // ignore warnings
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        // ignore errors
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        // ignore errors
    }

    @Override
    public void endDocument() throws SAXException {
        ContentResolver cr = MainApplication.getContext().getContentResolver();

        try {
            if (!mInserts.isEmpty()) {
                ContentProviderResult[] results = cr.applyBatch(FeedData.AUTHORITY, mInserts);
                if (mFetchImages) {
                    for (int i = 0; i < results.length; ++i) {
                        ArrayList<String> images = mInsertedEntriesImages.get(i);
                        if (images != null) {
                            FetcherService.addImagesToDownload(results[i].uri.getLastPathSegment(), images);
                        }
                    }
                }
            }
            if (mRetrieveFullText)
                FetcherService.addEntriesToMobilize(mFeedEntriesUri);

        } catch (  Exception e ) {
            FetcherService.Status().SetError( e.getMessage(), String.valueOf( mId ), "", e );
            Dog.e("Error", e);
        }

        ContentValues values = new ContentValues();
        if (mFeedName == null && mFeedTitle != null) {
            values.put(FeedColumns.NAME, mFeedTitle.trim());
        }
        values.putNull(FeedColumns.ERROR);
        values.put(FeedColumns.LAST_UPDATE, System.currentTimeMillis() - 3000); // by precaution to not miss some feeds
        values.put(FeedData.FeedColumns.REAL_LAST_UPDATE, mNewRealLastUpdate);
        cr.update(FeedColumns.CONTENT_URI(mId), values, null, null);

        super.endDocument();
    }


}

