package ru.yanus171.feedexfork.service;

import static ru.yanus171.feedexfork.MainApplication.mImageFileVoc;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.ClearContentStepToFile;
import static ru.yanus171.feedexfork.utils.NetworkUtils.getDownloadedImagePath;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.Timer;

public class EPUB {
    public static boolean Is(String uri ) {
        return Is( Uri.parse(uri) );
    }
    public static boolean Is(Uri uri ) {
        final String fileName = FileUtils.INSTANCE.getFileName(uri).toLowerCase();
        return fileName.contains(".epub");
    }
    static private String getTitle(Document doc) {
        return getElementText( doc, "dc:title" ) + " " +
                getElementText( doc, "dc:title" ) + " " +
                getElementText( doc, "dc:creator" );
    }
    static private String getElementText(Document doc, String tagName) {
        Elements els = doc.getElementsByTag(tagName);
        if ( !els.isEmpty() )
            return els.first().text();
        return "";

    }
    @SuppressLint("Range")
    public static boolean loadLocalFile(final long entryId, String entryLink) throws IOException {
        if (!Is(entryLink))
            return false;
        Timer timer = new Timer( "loadEPUBLocalFile " + entryLink );
        ClearContentStepToFile();
        ZIP zip = new ZIP( Uri.parse( entryLink ) );
        final String rootFileName = getAttributeValue( zip.GetDoc( "META-INF/container.xml" ), "rootfile", "full-path");
        final String parentFolder = getParentPath(rootFileName);
        Document doc = zip.GetDoc( rootFileName );
        StringBuilder content = new StringBuilder();
        final String title = getTitle( doc );
        for( Element item: doc.getElementsByTag( "item" ) ) {
            Status().ChangeProgress( "Reading " + item.attr( "href" ) );
            final String type = item.attr("media-type");
            final String href = item.attr( "href" );
            final String relativeFileName = parentFolder + href;
            if (type.contains("xhtml"))
                content.append( zip.GetDoc( relativeFileName ).getElementsByTag("body").first() );
            else if ( type.contains("image") )
                saveImage( zip.GetFile( relativeFileName ), href, entryLink );
        }
        Status().ChangeProgress("");
        saveToDB(entryId, entryLink, title, content.toString());
        Status().ChangeProgress("");
        timer.End();
        return true;
    }

    private static void saveImage( File file, String relativeFileName, String entryLink ) throws IOException {
        final String downloadedFilePath = getDownloadedImagePath( entryLink, relativeFileName );
        FileUtils.INSTANCE.copy( file, new File( downloadedFilePath ) );
        mImageFileVoc.addFile( downloadedFilePath );
    }

    private static String getParentPath(String rootFileName) {
        String result = new File(rootFileName).getParent();
        if ( result == null )
            result = "";
        else
            result += "/";
        return result;
    }

    private static String getAttributeValue(Document doc, String tagName, String attributeName) {
        final Elements els = doc.getElementsByTag(tagName);
        if ( els.isEmpty() )
            throw new IllegalStateException( String.format( "Tag %s not found in doc", tagName ) );
        final String result = els.first().attr( attributeName );
        if ( result.isEmpty() )
            throw new IllegalStateException( String.format( "Attribute %s not found in tag %s", attributeName, tagName ) );
        return result;
    }

    private static void saveToDB(long entryId, String link, String title, String content) {
        ContentValues values = new ContentValues();
        values.put(FeedData.EntryColumns.TITLE, title);
        Status().ChangeProgress( "saveMobilizedHTML" );
        FileUtils.INSTANCE.saveMobilizedHTML(link, content, values);
        contentResolver().update(FeedData.EntryColumns.CONTENT_URI(entryId), values, null, null);
    }

    private static ContentResolver contentResolver() {
        return MainApplication.getContext().getContentResolver();
    }
}
