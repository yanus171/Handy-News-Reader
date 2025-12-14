package ru.yanus171.feedexfork.activity;

import static ru.yanus171.feedexfork.Constants.CONTENT_SCHEME;
import static ru.yanus171.feedexfork.Constants.FILE_SCHEME;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.parser.FileSelectDialog;
import ru.yanus171.feedexfork.service.FB2;
import ru.yanus171.feedexfork.utils.FileUtils;

public class LocalFile {
    @NonNull
    static public Uri processOnFirstLoad(Uri uri) {
        if (FB2.Is( uri ) )
            uri = FB2.processOnFirstLoad( uri );
        final String cacheDir = MainApplication.getContext().getCacheDir().getAbsolutePath();
        final File fileInCache = new File( cacheDir, FileSelectDialog.Companion.getFileName(uri));
        if ( !fileInCache.exists() || fileInCache.length() == 0 )
            FileSelectDialog.Companion.copyFile(uri, fileInCache.getAbsolutePath(), MainApplication.getContext(), false);
        uri = FileUtils.INSTANCE.getUriForFile( fileInCache );
        return uri;
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
    public static Uri extractFileFromZIP(Uri sourceUri, String targetFileName) {
        final String cacheDir = MainApplication.getContext().getCacheDir().getAbsolutePath();
        try {
            return extractFileFromZIP( sourceUri, cacheDir, targetFileName, null );
        } catch ( IllegalArgumentException e ) {
            e.printStackTrace();
            return extractFileFromZIP( sourceUri, cacheDir, targetFileName, Charset.forName("CP1251") );
        }
    }
    private static Uri extractFileFromZIP(Uri sourceUri, String destFolder, String targetFileName, Charset charset)  {
        InputStream is;
        ZipInputStream inputStream;
        try {
            String filename;
            is = MainApplication.getContext().getContentResolver().openInputStream(sourceUri);
            if (Build.VERSION.SDK_INT >= 24 && charset != null)
                inputStream = new ZipInputStream(new BufferedInputStream(is), charset);
            else
                inputStream = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry entry;

            while ((entry = inputStream.getNextEntry()) != null)
            {
                filename = entry.getName();
                if (entry.isDirectory() || (!targetFileName.isEmpty() && filename != targetFileName) )
                    continue;
                return createFileFromZIP(destFolder, filename, inputStream);
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.EMPTY;
    }

    @NonNull
    private static Uri createFileFromZIP(String destFolder, String filename, ZipInputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int count;
        final File destFile = new File(destFolder, filename);
        FileOutputStream fout = new FileOutputStream(destFile );
        while ((count = inputStream.read(buffer)) != -1)
            fout.write(buffer, 0, count);
        fout.close();
        inputStream.closeEntry();
        return FileUtils.INSTANCE.getUriForFile(destFile);
    }

}
