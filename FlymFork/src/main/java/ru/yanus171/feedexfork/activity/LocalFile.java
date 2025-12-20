package ru.yanus171.feedexfork.activity;

import static ru.yanus171.feedexfork.Constants.CONTENT_SCHEME;
import static ru.yanus171.feedexfork.Constants.FILE_SCHEME;
import static ru.yanus171.feedexfork.service.FetcherService.Status;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.SaveContentStepToFile;

import android.content.ContentValues;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.parser.FileSelectDialog;
import ru.yanus171.feedexfork.utils.FileUtils;

public class LocalFile {
    @NonNull
    static public Uri processOnFirstLoad(Uri uri, ContentValues values) {
        final String cacheDir = MainApplication.getContext().getCacheDir().getAbsolutePath();
        final File fileInCache = new File( cacheDir, FileSelectDialog.Companion.getFileName(uri));
        if ( !fileInCache.exists() || fileInCache.length() == 0 )
            FileSelectDialog.Companion.copyFile(uri, fileInCache.getAbsolutePath(), MainApplication.getContext(), false);
        uri = FileUtils.INSTANCE.getUriForFile( fileInCache );
        return uri;
    }
    public static boolean Is(String uri ) {
        return Is( Uri.parse( uri ) );
    }
    public static boolean Is(Uri uri ) {
        return uri.toString().startsWith( CONTENT_SCHEME ) &&
                ( uri.toString().contains( "document" ) || uri.toString().contains( "media" )  || uri.toString().contains( "storage" ) ) ||
                uri.toString().startsWith( FILE_SCHEME ) || uri.toString().contains( "cache_root" );
    }

    public static boolean IsZIP(Uri uri ) {
        if ( !Is(uri) )
            return false;
        final String fileName = FileUtils.INSTANCE.getFileName(uri).toLowerCase();
        return fileName.endsWith(".zip");
    }

    @NonNull
    public static Document loadDoc(File file) throws IOException {
        Status().ChangeProgress( "Jsoup.parse" );
        Document doc = Jsoup.parse(file, null, "");
        SaveContentStepToFile( doc, "Jsoup.parse" );
        return doc;
    }

}
