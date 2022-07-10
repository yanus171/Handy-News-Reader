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

package ru.yanus171.feedexfork.utils;

import androidx.annotation.NonNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;

import static android.provider.BaseColumns._ID;
import static ru.yanus171.feedexfork.MainApplication.mImageFileVoc;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.service.FetcherService.mMaxImageDownloadCount;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.HANDY_NEWS_READER_ROOT_CLASS;
import static ru.yanus171.feedexfork.utils.RegexUtils.createMatcherWithTimeout;

public class HtmlUtils {

    private static final Whitelist JSOUP_WHITELIST = Whitelist.relaxed()
            .addTags("iframe", "video", "audio", "source", "track", "hr", "ruby", "rp", "rt" )
            .addAttributes("iframe", "src", "frameborder", "height", "width")
            .addAttributes("video", "src", "controls", "height", "width", "poster")
            .addAttributes("audio", "src", "controls")
            .addAttributes("source", "src", "type")
            .addAttributes("img", "data-original")
            .addAttributes("a", "data-fancybox-href")
            //.addAttributes("math", "xmlns")
            //.addTags( "math", "mglyph", "mi", "mn", "mo", "mtext", "mspace", "ms", "mrow", "mfrac", "msqrt", "mroot", "mstyle", "msub", "msup", "munder", "mover", "semantics" )
            .addAttributes( ":all", "id", "name", "class", "displaystyle", "scriptlevel" )
            .addAttributes( "textroot", HANDY_NEWS_READER_ROOT_CLASS )
            .addTags("s")
            .addTags("time")
            .addAttributes("track", "src", "kind", "srclang", "label");

    public static final String URL_SPACE = "%20";

    private static final Pattern IMG_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern A_IMG_PATTERN = Pattern.compile("<a[^>]+href([^>]+)>([^<]?)<img(.)*?</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADS_PATTERN = Pattern.compile("<div class=('|\")mf-viral('|\")><table border=('|\")0('|\")>.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("<template(.|\\s)+?/template>", Pattern.CASE_INSENSITIVE);
    //private static final Pattern LAZY_LOADING_PATTERN = Pattern.compile("\\s+src=[^>]+\\s+original[-]*src=(\"|')", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAZY_LOADING_PATTERN2 = Pattern.compile("src=\"[^\"]+?lazy[^\"]+\"", Pattern.CASE_INSENSITIVE);
    //private static final Pattern DATA_SRC_PATTERN = Pattern.compile("data-src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPTY_IMAGE_PATTERN = Pattern.compile("<img\\s+(height=['\"]1['\"]\\s+width=['\"]1['\"]|width=['\"]1['\"]\\s+height=['\"]1['\"])\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_HTTP_IMAGE_PATTERN = Pattern.compile("\\s+(href|src)=(\"|')//", Pattern.CASE_INSENSITIVE);
    private static final Pattern BAD_IMAGE_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)\\.img['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern START_BR_PATTERN = Pattern.compile("^(\\s*<br\\s*[/]*>\\s*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern END_BR_PATTERN = Pattern.compile("(\\s*<br\\s*[/]*>\\s*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTIPLE_BR_PATTERN = Pattern.compile("(\\s*<br\\s*[/]*>\\s*){3,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPTY_LINK_PATTERN = Pattern.compile("<a\\s+[^>]*></a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG_USER_PATTERN = Pattern.compile("<img[^>]+(user)[^>]+(.)*?>", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_VIDEO = Pattern.compile("<video.+?</video>", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_IFRAME = Pattern.compile("<iframe(.|\\n|\\s|\\t)+?/iframe>", Pattern.CASE_INSENSITIVE);

    public static final Pattern HTTP_PATTERN = Pattern.compile("(http.?:[/][/]|www.)([a-z]|[-_%]|[A-Z]|[0-9]|[?]|[=]|[:]|[/.]|[~])*");//Pattern.compile("(?<![\\>https?://|href=\"'])(?<http>(https?:[/][/]|www.)([a-z]|[-_%]|[A-Z]|[0-9]|[/.]|[~])*)");

    //public static boolean mIsDownloadingImagesForEntryView = false;

    static final Pattern VIDEO_CHANNEL_URL = Pattern.compile("ube.com.watch.v=([^&]+)" );

    public static String improveHtmlContent(String content, String baseUri, FeedFilters filters, ArrayList<String> categoryList, ArticleTextExtractor.MobilizeType mobType, boolean isAutoFullTextRoot ) {

        if (content == null )
            return content;

        content = TEMPLATE_PATTERN.matcher(content).replaceAll( "" );
        content = ADS_PATTERN.matcher(content).replaceAll("");
        // remove some ads
        content = ADS_PATTERN.matcher(content).replaceAll("");
        // remove lazy loading images stuff
        //content = LAZY_LOADING_PATTERN.matcher(content).replaceAll(" src=$1");
        content = LAZY_LOADING_PATTERN2.matcher(content).replaceAll("");
        //content = DATA_SRC_PATTERN.matcher(content).replaceAll(" src=$1");
        //content = ReplaceImagesWithDataOriginal(content, "<span[^>]+class..lazy-image-placeholder[^>]+src=\"([^\"]+)\"[^>]+>");
        content = ReplaceImagesWithDataOriginal(content, "<img[^>]+data-src=\"([^\"]+)\"([^>]+)?>");
        content = ReplaceImagesWithDataOriginal(content, "<a[^>]+><img[^>]+srcset=\"[^\"]+,(.+?)\\s.+\"+[^>]+></a>" );
        content = ReplaceImagesWithALink(content);
        content = ReplaceImagesWithDataOriginal(content, "<img[^>]+data-original=\"([^\"]+)\"([^>]+)?>");
        //content = ReplaceImagesWithDataOriginal(content, "<a+?background-image.+?url.+?quot;(.+?).quot;(.|\\n|\\t|\\s)+?a>");
        //content = ReplaceImagesWithDataOriginal(content, "<a+?background-image.+?url.+?'(.+?)'(.|\\n|\\t|\\s)+?a>");
        //content = ReplaceImagesWithDataOriginal(content, "<i+?background-image.+?url.+?quot;(.+?).quot;(.|\\n|\\t|\\s)+?i>");

        content = extractVideos(content);
        content = ReplaceImagesWithDataOriginal(content, "<a.+?background-image.+?url.+?(http.+?)('|.quot)(.|\\n|\\t|\\s)+?a>");
        content = ReplaceImagesWithDataOriginal(content, "<a.+?href=\"([^\"]+?(jpg|png))\"(.|\\n|\\t|\\s)+?a>");
        // clean by JSoup
        final Safelist whiteList =
                ( mobType == ArticleTextExtractor.MobilizeType.Tags ) ?
                JSOUP_WHITELIST.addAttributes( "i", "onclick" )
                               .addAttributes( "span", "class" )
                               .addTags( "s" )
                :
                JSOUP_WHITELIST;
        content = Jsoup.clean(content, baseUri, whiteList  );


        if ( isAutoFullTextRoot ) {
            // remove empty or bad images
            content = EMPTY_IMAGE_PATTERN.matcher(content).replaceAll("");
            content = BAD_IMAGE_PATTERN.matcher(content).replaceAll("");
            //// remove empty links
            //content = EMPTY_LINK_PATTERN.matcher(content).replaceAll("");
            // fix non http image paths
            content = NON_HTTP_IMAGE_PATTERN.matcher(content).replaceAll(" $1=$2http://");
            // remove trailing BR & too much BR
            //content = START_BR_PATTERN.matcher(content).replaceAll("");
            //content = END_BR_PATTERN.matcher(content).replaceAll("");
            content = MULTIPLE_BR_PATTERN.matcher(content).replaceAll("<br><br>");
            if ( PrefUtils.getBoolean( "setting_convert_xml_symbols_before_parsing", true ) ) {
                // xml
                content = content.replace("&lt;", "<");
                content = content.replace("&gt;", ">");
                content = content.replace("&amp;", "&");
                content = content.replace("&quot;", "\"");
            }

        }
        content = content.replace( "&#39;", "'" );

        if ( filters != null )
            content = filters.removeText(content, DB_APPLIED_TO_CONTENT );

        content = content.replaceAll( "<p>([\\n\\s\\t]*?)</p>", "" );

        Matcher m = VIDEO_CHANNEL_URL.matcher( baseUri );
        if ( m.find() ) {
            String channelID = m.group(1);
            content += String.format( "<iframe src=\"https://www.1111222.com/embed/%s?rel=0\" frameborder=\"0\" allowfullscreen ></iframe>", channelID )
                .replace( "222", "ube" )
                .replace( "1111", "yout" );
        }

        content = extractCategoryList( content, categoryList );
        return content;
    }

    public static ArrayList<String> getImageURLs1(String content) {
        ArrayList<String> images = new ArrayList<>();

        if (!TextUtils.isEmpty(content)) {
            Matcher matcher = IMG_PATTERN.matcher(content);
            int index = 0;
            while (matcher.find() && ( ( index < FetcherService.mMaxImageDownloadCount ) || ( FetcherService.mMaxImageDownloadCount == 0 ) ) ) {
                images.add(matcher.group(1).replace(" ", URL_SPACE));
                index++;
            }
            FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
        }


        return images;
    }

    private static final String LINK_TAG_END = "</a>";

    private static String GetLinkStartTag(String imgPath) {
        return "<a href=\"" + Constants.FILE_SCHEME + imgPath + "\" >";
    }
    public static String replaceImageURLs(String content, final long entryId, final String entryLink, boolean isDownLoadImages) {
        final ArrayList<String> imagesToDl = new ArrayList<>();
        return replaceImageURLs(content, "", entryId, entryLink, isDownLoadImages, imagesToDl, null, mMaxImageDownloadCount );

    }
    public static String replaceImageURLs(String content,
                                          final String feedId,
                                          final long entryId,
                                          final String entryLink,
                                          boolean isDownLoadImages,
                                          final ArrayList<String> imagesToDl,
                                          final ArrayList<Uri> externalImageList,
                                          final int maxImageDownloadCount) {
        final int status = Status().Start("Reading images", true); try {
            // TODO <a href([^>]+)>([^<]+)<img(.)*?</a>
            Timer timer = new Timer( "replaceImageURLs" );
            if (!TextUtils.isEmpty(content)) {
                Matcher matcher;

                final boolean isShowImages = maxImageDownloadCount == 0 || PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true);
                //final ArrayList<String> imagesToDl = new ArrayList<>();
                String firstImageUrl = "";
                matcher = IMG_PATTERN.matcher(content);
                int index = 0;
                while ( matcher.find() ) {
                    index++;
                    String srcUrl = matcher.group(1);
                    srcUrl = srcUrl.replace(" ", URL_SPACE);
                    final String imgWebTag = matcher.group(0);
                    final String imgFilePath = NetworkUtils.getDownloadedImagePath(entryLink, srcUrl);
                    final File file = new File( imgFilePath );
                    final boolean isImageToAdd = maxImageDownloadCount == 0 || ( index <= maxImageDownloadCount );
                    final String imgFileTag =
                        GetLinkStartTag(imgFilePath) +
                        imgWebTag.replace(srcUrl, Constants.FILE_SCHEME + imgFilePath) +
                        LINK_TAG_END;
                    if ( isShowImages ) {
                        final boolean isFileExists = mImageFileVoc.isExists( file.getPath() );
                        if ( !isFileExists && isImageToAdd )
                            imagesToDl.add(srcUrl);
                        if (firstImageUrl.isEmpty() )
                            firstImageUrl = srcUrl;
                        String btnLoadNext = "";
                        if ( index == maxImageDownloadCount + 1 ) {
                            btnLoadNext = getButtonHtml("downloadNextImages()", getString(R.string.downloadNext) + PrefUtils.getImageDownloadCount(), "download_next");
                            btnLoadNext += getButtonHtml("downloadAllImages()", getString(R.string.downloadAll), "download_all");
                        }
                        final boolean isSmallForExternalList = false;// = file.length() < 1024 * 7;
                        if ( externalImageList != null && externalImageList.size() <= maxImageDownloadCount &&
                            ( !isSmallForExternalList || !isFileExists ) ) {
                            externalImageList.add(Uri.fromFile(file));
                            content = content.replace(imgWebTag, "");
                        } else if ( externalImageList != null && isSmallForExternalList )
                            content = content.replace(imgWebTag, "");
                        else if ( isImageToAdd || isFileExists )
                            content = content.replace(imgWebTag, imgFileTag + btnLoadNext);
                        else if ( !isFileExists ) {
                            String htmlButtons = getDownloadImageHtml(srcUrl) + "<br/>";
                            content = content.replace(imgWebTag, htmlButtons + btnLoadNext + imgWebTag.replace(srcUrl, Constants.FILE_SCHEME + imgFilePath));
                        }
                    } else
                        content = content.replace(imgWebTag, "");
                }
                content = content.replaceAll("width=\"\\d+\"", "");
                content = content.replaceAll("height=\"\\d+\"", "");

                // Download the images if needed
                if ( isDownLoadImages && !imagesToDl.isEmpty() && entryId != -1 )
                    new Thread(() -> FetcherService.downloadEntryImages(feedId, entryId, entryLink, imagesToDl)).start();

                if ( entryId != -1 && !firstImageUrl.isEmpty() ) {
                    String finalFirstImageUrl = firstImageUrl;
                    new Thread(() -> {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        Uri uri = FeedData.EntryColumns.CONTENT_URI(entryId);
                        Cursor cur = cr.query(uri, new String[]{_ID}, FeedData.EntryColumns.IMAGE_URL + Constants.DB_IS_NOT_NULL + Constants.DB_AND + FeedData.EntryColumns.IMAGE_URL + "<> ''" , null, null);
                        if (!cur.moveToFirst()) {
                            ContentValues values = new ContentValues();
                            values.put(FeedData.EntryColumns.IMAGE_URL, finalFirstImageUrl);
                            cr.update(uri, values, null, null);
                        }
                        cur.close();
                    }).start();
                }

            }
            timer.End();
        } catch ( Exception e ) {
            Status().SetError( null, "", String.valueOf(entryId), e  );
        } finally {
            Status().End( status );
        }
        return content;
    }

    private static String ReplaceImagesWithALink(String content) {
        // <img> in <a> tag
        final Pattern A_HREF_WITH_IMG = Pattern.compile("href=(.[^>]+\\.(jpeg|jpg|png).)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = A_IMG_PATTERN.matcher(content);
        while (matcher.find()) {
            String match = matcher.group();
            Matcher matcherHrefImg = A_HREF_WITH_IMG.matcher(match);
            if (matcherHrefImg.find()) {
                String newText = "<img src=" + matcherHrefImg.group(1) + " />";
                content = content.replace(match, newText);
            } else {
                String replace = match.replaceAll("<a([^>]+)>", "").replaceAll("</a>", "");
                content = content.replace(match, replace);
            }
        }
        return content;
    }

    private static String ReplaceImagesWithDataOriginal(String content, String regex) {
        final Pattern IMG_DATA_ORIGINAL = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = IMG_DATA_ORIGINAL.matcher( content );//createMatcherWithTimeout( content, IMG_DATA_ORIGINAL, 2000, 100 );
        while (matcher.find()) {
            String match = matcher.group();
            String newText = "<img src=\"" + matcher.group(1) + "\" />";
            content = content.replace(match, newText);
        }
        return content;
    }
    private static String extractVideos(String content) {
        final Pattern patternSrc = Pattern.compile("src=\"([^\"]+?)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = PATTERN_VIDEO.matcher(content);
        ArrayList<String> list = new ArrayList<>();
        String result = content;
        while (matcher.find()) {
            String video = matcher.group();
            result = result.replace(video, "");
            video = video.replace( "<video ", "<video controls muted preload='metadata'" );
            final Matcher m = patternSrc.matcher( video );
            if ( m.find() && m.groupCount() >= 1 && !result.contains( m.group(1) ) )
                list.add( video );
        }
        result += TextUtils.join( "\n", list );
        return result;
    }

    private static String getDownloadImageHtml(String match) {
        return getButtonHtml("downloadImage('" + match + "')" , getString( R.string.downloadOneImage ), "download_image" );
    }

    private static String getString( int id ) {
        return MainApplication.getContext().getString(id);

    }
    @NonNull
    static String getButtonHtml(String methodName, String caption, String divClass) {
        final String BUTTON_START = "<span class=\"" + divClass +"\"><i onclick=\"";
        final String BUTTON_END = "\" align=\"left\" class=\"" + divClass + "\" >" + caption + "</i></span>";
        final String result = BUTTON_START + "ImageDownloadJavaScriptObject." + methodName + ";" + BUTTON_END;
        return result;
    }

    public static String getMainImageURL(String content) {
        if (!TextUtils.isEmpty(content)) {
            content = ReplaceImagesWithALink(content);
            Matcher matcher = IMG_PATTERN.matcher(content);

            while (matcher.find()) {
                String imgUrl = matcher.group(1).replace(" ", URL_SPACE);
                if (isCorrectImage(imgUrl)) {
                    return imgUrl;
                }
            }
        }

        return null;
    }

    public static String getMainImageURL(ArrayList<String> imgUrls) {
        for (String imgUrl : imgUrls) {
            if (isCorrectImage(imgUrl)) {
                return imgUrl;
            }
        }

        return null;
    }

    private static boolean isCorrectImage(String imgUrl) {
        return !imgUrl.endsWith(".gif") && !imgUrl.endsWith(".GIF") && !imgUrl.endsWith(".img") && !imgUrl.endsWith(".IMG");
    }

//    static public ArrayList<String> Split( String text, String sep ) {
//        final ArrayList<String> result = new ArrayList<>();
//        for( String item: TextUtils.split(text, sep) )
//            result.add( item );
//        return result;
//
//    }
    static public ArrayList<String> Split( String text, Pattern sep ) {
        final ArrayList<String> result = new ArrayList<>();
        Collections.addAll(result, TextUtils.split(text, sep));
        return result;
    }

    private static final Pattern CATEGORY_PATTERN = Pattern.compile( "[>\\s]#(\\w+)" );

    private static String extractCategoryList(String content, ArrayList<String> categoryList) {
        if ( !categoryList.isEmpty() )
            return content;
        Matcher match = CATEGORY_PATTERN.matcher(content);
        while (match.find())
            categoryList.add(match.group(1));
        return match.replaceAll("");
    }


}