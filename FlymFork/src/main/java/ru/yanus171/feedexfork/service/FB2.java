package ru.yanus171.feedexfork.service;

import static ru.yanus171.feedexfork.fragment.EntryFragment.IsLocalFile;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.ClearContentStepToFile;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.SaveContentStepToFile;
import static ru.yanus171.feedexfork.utils.HtmlUtils.convertXMLSymbols;
import static ru.yanus171.feedexfork.utils.NetworkUtils.getDownloadedImageLocaLPath;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.Timer;

class FB2 {
    public static boolean TypeIs( String link ) {
        return IsLocalFile(Uri.parse(link)) && FileUtils.INSTANCE.getFileName(Uri.parse(link)).toLowerCase().contains(".fb2");
    }
    @SuppressLint("Range")
    public boolean loadLocalFile(final long entryId, String link ) throws IOException {
        if (!TypeIs(link))
            return false;
        Timer timer = new Timer( "loadFB2LocalFile " + link );
        ClearContentStepToFile();
        Document doc;
        try (InputStream is = contentResolver().openInputStream(Uri.parse(link))) {
            doc = loadDoc(is);
        }
        convertImages(doc);
        convertTitle(doc);
        createImageFiles(link, doc);
        final String title = doc.getElementsByTag( "author" ).first().text() + ". " + doc.getElementsByTag( "book-title" ).first().text();
        removeElements(doc);
        String content = getContent(doc);
        content = removeTags(content);
        content = removeWrongChars(content);
        content = convertXMLSymbols(content);
        content = AddFB2TableOfContent( content );
        saveToDB(entryId, link, title, content);
        Status().ChangeProgress("");
        timer.End();
        return true;
    }

    @NonNull
    private static String getContent(Document doc) {
        Status().ChangeProgress( "doc.toString()" );
        String content = doc.toString();
        return content;
    }

    @NonNull
    private static Document loadDoc(InputStream is) throws IOException {
        Status().ChangeProgress( "Jsoup.parse" );
        Document doc = Jsoup.parse(is, null, "");
        SaveContentStepToFile( doc, "Jsoup.parse" );
        return doc;
    }

    private static void convertTitle(Document doc) {
        for (Element el : doc.getElementsByTag("title"))
            el.tagName( "h1" );
        SaveContentStepToFile(doc, "h1" );
    }

    @NonNull
    private static String removeWrongChars(String content) {
        Status().ChangeProgress( "replace 0" );
        content = content.replace( "&#x0;", "" );
        SaveContentStepToFile(content, "after_remove_0" );
        return content;
    }

    private static void saveToDB(long entryId, String link, String title, String content) {
        ContentValues values = new ContentValues();
        values.put(FeedData.EntryColumns.TITLE, title);
        Status().ChangeProgress( "saveMobilizedHTML" );
        FileUtils.INSTANCE.saveMobilizedHTML(link, content, values);
        contentResolver().update(FeedData.EntryColumns.CONTENT_URI(entryId), values, null, null);
    }

    private static void convertImages(Document doc) {
        Status().ChangeProgress( "images" );
        Elements list = doc.getElementsByTag("image");
        list.addAll( doc.getElementsByTag("img") );
        for (Element el : list ) {
            final String id = el.attr( "l:href" ).replace( "#", "" );
            if ( id.isEmpty() )
                continue;
            Elements images = doc.getElementsByAttributeValue( "id", id );
            if ( images.isEmpty() )
                continue;
            el.insertChildren( -1, images.first() );
            el.attr( "align", "middle" );
        }
        SaveContentStepToFile( doc, "binary_move" );
    }

    private static void createImageFiles(String link, Document doc) throws IOException {
        Status().ChangeProgress( "binary" );
        int imageIndex = 0;
        for (Element el : doc.getElementsByTag("binary")) {
            if (!el.hasAttr("content-type"))
                continue;
            el.tagName("img");
            imageIndex++;
            final String imgFilePath = getDownloadedImageLocaLPath(link, imageIndex );
            final String imageData = el.ownText().replace( "\n", "" ).replace( " ", "" );
            if ( imageData.isEmpty() )
                continue;
            Status().ChangeProgress( String.format( "image file %d", imageIndex ) ) ;
            byte[] data = Base64.decode(imageData, Base64.DEFAULT );
            try (OutputStream stream = new FileOutputStream(imgFilePath)) {
                stream.write(data);
            }
            el.attr("src", Constants.FILE_SCHEME + imgFilePath);
            el.text("");
        }
        SaveContentStepToFile( doc, "binary_to_img" );
    }

    @NonNull
    private static String removeTags(String content) {
        //content = content.replaceAll( "[^\\s]{50,}", "" );
        content = content.replace( "<style>", "" );
        content = content.replace( "</style>", "" );
        content = content.replace( "<empty-line", "<br" );
        content = content.replace( "emphasis>", "i>" );
        //content = content.replace( "</i>, ", ",</i>" );
        content = content.replace( "strong>", "b>" );
        content = content.replace( "strikethrough>.", "del>" );
        content = content.replace( "xlink:href", "href" );
        content = content.replaceAll( "\\n\\s+<", "<" );
        //content = content.replace( "\n\r<", "<" );
        //content = content.replace( "\r\n<", "<" );
        return content;
    }

    private void removeElements(Document doc) {
        removeElementsWithTag(doc, "id");
        removeElementsWithTag(doc, "genre");
        removeElementsWithTag(doc, "lang" );
        removeElementsWithTag(doc, "src-lang" );
        removeElementsWithTag(doc, "translator" );
        removeElementsWithTag(doc, "document-info" );
        removeElementsWithTag(doc, "publish-info" );
        removeElementsWithTag(doc, "custom-info" );
        removeElementsWithTag(doc, "home-page" );
        removeElementsWithTag(doc, "first-name" );
        removeElementsWithTag(doc, "last-name" );
        removeElementsWithTag(doc, "author" );
        removeElementsWithTag(doc, "book-title" );
        removeElementsWithTag(doc, "stylesheet" );
    }

    private static ContentResolver contentResolver() {
        return MainApplication.getContext().getContentResolver();
    }

    private void removeElementsWithTag(Document doc, String tag) {
        for ( Element item: doc.getElementsByTag( tag ) )
            item.remove();
    }

    private String AddFB2TableOfContent(String content) {
        final Pattern PATTERN = Pattern.compile("<(h1|title)>((.|\\n|\\t)+?)</(h1|title)>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = PATTERN.matcher(content);
        StringBuilder tc = new StringBuilder();
        int i = 1;
        String TC_START = "TC_START";
        while (matcher.find()) {
            String match = matcher.group();
            String newText = "<div id=\"tc" + i + "\" >" + match.replaceAll( "<?p>", "") + "</div>";
            if ( i == 1 )
                newText = TC_START + newText;
            content = content.replaceFirst(match, newText);
            String caption = matcher.group(2).replaceAll( "<.*?>", "");
            tc.append("<p class=\"toc\"><a href=\"#tc").append(i).append("\">").append(caption).append("</a></p>");
            i++;
        }
        if ( tc.length() > 0 )
            tc.insert( 0, String.format("<h2>%s</h2>", MainApplication.getContext().getString( R.string.tableOfContent )) );
        content = content.replaceFirst( TC_START, "<div class=\"toc\">" + tc + "</div>" );
        return content;
    }
}
