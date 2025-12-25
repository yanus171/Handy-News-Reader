package ru.yanus171.feedexfork.service;

import static ru.yanus171.feedexfork.activity.LocalFile.loadDoc;
import static ru.yanus171.feedexfork.service.FetcherService.Status;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ru.yanus171.feedexfork.MainApplication;

public class ZIP {

    public ZIP(Uri uri ) throws IOException {
        mUri = uri;
        init();
    }

    
    public File GetFile(String relativeFileName) throws FileNotFoundException {
        if ( !mFileMap.containsKey( relativeFileName ) )
            throw new FileNotFoundException( String.format("Zip: %s file not found", relativeFileName) );
        return mFileMap.get( relativeFileName );
    }
    private Uri mUri = null;
    private HashMap<String, File> mFileMap = new HashMap<>();

    private void init() throws IOException {
        Status().ChangeProgress( "extracting zip" );
        final File dir = new File( MainApplication.getContext().getCacheDir().getAbsolutePath(), "tempZip" );
        deleteDir(dir);
        if ( !dir.mkdirs() )
            Status().SetError( "Cant make dir " + dir.getPath(), "", "", null );
        else
            try {
                final InputStream is = MainApplication.getContext().getContentResolver().openInputStream(mUri);
                final Charset charset = null;
                ZipInputStream inputStream;
                if (Build.VERSION.SDK_INT >= 24 && charset != null) {
                    inputStream = new ZipInputStream(new BufferedInputStream(is), charset);
                }
                else
                    inputStream = new ZipInputStream(new BufferedInputStream(is));
                try {
                    ZipEntry entry;
                    while ((entry = inputStream.getNextEntry()) != null) {
                        if (entry.isDirectory() )
                            continue;
                        mFileMap.put( entry.getName(), createFileFromZIP(dir.getPath(), entry, inputStream) );
                    }
                } finally {
                    inputStream.close();
                }
            } catch (FileNotFoundException e) {
                Status().SetError( e );
            } catch (IOException e) {
                Status().SetError( e );
            } finally {
                Status().ChangeProgress( "" );
            }
    }

    void deleteDir(File f) throws IOException {
        if ( !f.exists() )
            return;
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                deleteDir(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }
    public static File extractFileFromZIP(Uri sourceUri, String targetFileName) {
        final String cacheDir = MainApplication.getContext().getCacheDir().getAbsolutePath();
        try {
            return extractFileFromZIP( sourceUri, cacheDir, targetFileName, null );
        } catch ( IllegalArgumentException e ) {
            e.printStackTrace();
            return extractFileFromZIP( sourceUri, cacheDir, targetFileName, Charset.forName("CP1251") );
        }
    }
    private static File extractFileFromZIP(Uri sourceUri, String destFolder, String targetFileName, Charset charset)  {
        Status().ChangeProgress( "extracting zip" );
        try {
            String filename;
            ZipEntry entry;
            InputStream is = MainApplication.getContext().getContentResolver().openInputStream(sourceUri);
            ZipInputStream inputStream;
            if (Build.VERSION.SDK_INT >= 24 && charset != null)
                inputStream = new ZipInputStream(new BufferedInputStream(is), charset);
            else
                inputStream = new ZipInputStream(new BufferedInputStream(is));
            try {
                while ((entry = inputStream.getNextEntry()) != null) {
                    filename = entry.getName();
                    if (entry.isDirectory() || (!targetFileName.isEmpty() && !filename.contains(targetFileName)))
                        continue;
                    return createFileFromZIP(destFolder, entry, inputStream);
                }
            } finally {
                inputStream.close();
            }
            throw new FileNotFoundException( "File not found is zip archive " + targetFileName );
        } catch (FileNotFoundException e) {
            Status().SetError( e );
        } catch (IOException e) {
            Status().SetError( e );
        } finally {
            Status().ChangeProgress( "" );
        }
        return null;
    }

    @NonNull
    private static File createFileFromZIP(String destFolder, ZipEntry entry, ZipInputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int count;
        final File destFile = new File(destFolder, entry.getName());
        if ( !destFile.getParentFile().getPath().equals( destFolder ) ) {
            if ( !destFile.getParentFile().exists() && !destFile.getParentFile().mkdirs() )
                throw new FileNotFoundException( "Cannot create dir " + destFile.getParentFile());
        }
        FileOutputStream fout = new FileOutputStream( destFile );
        while ((count = inputStream.read(buffer)) != -1)
            fout.write(buffer, 0, count);
        fout.close();
        return destFile;
    }


}
