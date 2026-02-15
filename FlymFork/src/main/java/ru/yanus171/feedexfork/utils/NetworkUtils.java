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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.Html;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.WebEntryView;

import static ru.yanus171.feedexfork.MainApplication.mImageFileVoc;

public class NetworkUtils {

    public static final boolean OKHTTP = true;
    public static final boolean NATIVE = false;
    //public static final File IMAGE_FOLDER_FILE = new File(MainApplication.getContext().getCacheDir(), "images/");
    //public static final String IMAGE_FOLDER = IMAGE_FOLDER_FILE.getAbsolutePath() + '/';
    private static final String TEMP_PREFIX = "TEMP__";
    //public static final File FOLDER = new File(Environment.getExternalStorageDirectory(), "feedex/");
    private static final String ID_SEPARATOR = "__";

    private static final String FILE_FAVICON = "/favicon.ico";
    private static final String PROTOCOL_SEPARATOR = "://";

    private static final CookieManager COOKIE_MANAGER = new CookieManager() {{
        CookieHandler.setDefault(this);
    }};

    public static String getDownloadedOrDistantImageUrl(String entryLink, String imgUrl) {
        File dlImgFile = new File(NetworkUtils.getDownloadedImagePath(entryLink, imgUrl));
        return Uri.fromFile(dlImgFile).toString();
    }

    public static String getDownloadedImagePath(String entryLink, String imgUrl) {
        return getDownloadedImagePath(entryLink, "", imgUrl);
    }

    public static String getDownloadedImageLocaLPath( String entryLink, int index ) {
        return FileUtils.INSTANCE.GetImagesFolder().getAbsolutePath() + "/" + FileUtils.INSTANCE.getLinkHash( entryLink ) + ID_SEPARATOR + index;
    }

    private static String getDownloadedImagePath( String entryLink, String prefix, String imgUrl ) {
        final String lastSegment = imgUrl.contains( "/" ) ? imgUrl.substring(imgUrl.lastIndexOf("/")) : imgUrl;
        String fileExtension = lastSegment.contains(".") ? lastSegment.substring(lastSegment.lastIndexOf(".")) : "";
        fileExtension = fileExtension.replace( ".", "" );
        if ( fileExtension.isEmpty() && imgUrl.contains("/svg/") )
            fileExtension = "svg";
        if ( fileExtension.contains( "?" ) )
            fileExtension = fileExtension.replace( fileExtension.substring(fileExtension.lastIndexOf("?") + 1), "" );
        fileExtension = fileExtension.replace( ";", "" );
        fileExtension = fileExtension.replace( "&", "" );
        fileExtension = fileExtension.replace( "?", "" );

        return FileUtils.INSTANCE.GetImagesFolder().getAbsolutePath() + "/" + prefix + FileUtils.INSTANCE.getLinkHash( entryLink ) + ID_SEPARATOR +
               FileUtils.INSTANCE.getLinkHash( imgUrl ) + (fileExtension.isEmpty()  ? "" : "." + fileExtension);
    }

    private static String getTempDownloadedImagePath(String entryLink, String imgUrl) {
        return getDownloadedImagePath(entryLink, TEMP_PREFIX, imgUrl);
    }

    public static boolean downloadImage(final long entryId, String entryUrl, String imgUrl, boolean isSizeLimit, boolean notify ) throws IOException {
        boolean result = false;
        if ( FetcherService.isCancelRefresh() )
            return result;
        String tempImgPath = getTempDownloadedImagePath(entryUrl, imgUrl);
        String finalImgPath = getDownloadedImagePath(entryUrl, imgUrl);

        if (!new File(tempImgPath).exists() && !new File(finalImgPath).exists()) {
            boolean abort = false;
            Connection imgURLConnection = null;
            try {
                //IMAGE_FOLDER_FILE.mkdir(); // create images dir

                // Compute the real URL (without "&eacute;", ...)
                String realUrl = Html.fromHtml(imgUrl).toString();
                imgURLConnection = new Connection( realUrl, OKHTTP );

                long size = imgURLConnection.getContentLength();
                int maxImageDownloadSize = PrefUtils.getImageMaxDownloadSizeInKb() * 1024;
                if ( !isSizeLimit || size <= maxImageDownloadSize ) {

                    FileOutputStream fileOutput = new FileOutputStream(tempImgPath); try {
                        InputStream inputStream = imgURLConnection.getInputStream(); try {

                            int bytesRecieved = 0;
                            int progressBytes = 0;
                            final int cStep = 1024 * 10;
                            byte[] buffer = new byte[2048];
                            int bufferLength;
                            //FetcherService.Status().ChangeProgress(getProgressText(bytesRecieved));
                            while (!FetcherService.isCancelRefresh() && ( bufferLength = inputStream.read(buffer) ) > 0) {
                                if (isSizeLimit && size > maxImageDownloadSize) {
                                    abort = true;
                                    break;
                                }
                                fileOutput.write(buffer, 0, bufferLength);
                                bytesRecieved += bufferLength;
                                progressBytes += bufferLength;
                                if (progressBytes >= cStep) {
                                    progressBytes = 0;
                                    //FetcherService.Status().ChangeProgress(getProgressText(bytesRecieved));
                                }
                            }
                            result = true;
                            FetcherService.Status().AddBytes(bytesRecieved);

                        } finally {
                            inputStream.close();
                        }
                    } finally {
                        fileOutput.flush();
                        fileOutput.close();
                    }

                    if ( !abort ) {
                        new File(tempImgPath).renameTo(new File(finalImgPath));
                        mImageFileVoc.addFile(finalImgPath );
                    } else
                        new File(tempImgPath).delete();
                }
            } catch (IOException e) {
                new File(tempImgPath).delete();
                throw e;
            } finally {
                if (imgURLConnection != null) {
                    imgURLConnection.disconnect();
                }
            }

            if ( result && !abort && notify && entryId > 0 )
                WebEntryView.NotifyToUpdate( entryId, entryUrl, true );
        }
        return result;
    }

    private static String getProgressText(int bytesRecieved) {
        return String.format("%d KB ...", bytesRecieved / 1024);
    }

    public static synchronized void deleteEntriesImagesCache(Uri entriesUri, String selection, String[] selectionArgs) {
        if (FileUtils.INSTANCE.GetImagesFolder().exists()) {
            Context context = MainApplication.getContext();
            PictureFilenameFilter filenameFilter = new PictureFilenameFilter();

            Cursor cursor = MainApplication.getContext().getContentResolver().query(entriesUri, FeedData.EntryColumns.PROJECTION_ID, selection, selectionArgs, null);

            while (cursor.moveToNext() && !FetcherService.isCancelRefresh()) {
                filenameFilter.setEntryId(cursor.getString(0));

                File[] files = FileUtils.INSTANCE.GetImagesFolder().listFiles(filenameFilter);
                if (files != null && files.length > 0 ) {
                    for (File file : files) {
                        file.delete();
                        if ( FetcherService.isCancelRefresh() )
                            break;
                    }
                    //FetcherService.mDeletedImageCount += files.length;
                    //FetcherService.Status().ChangeProgress(context.getString(R.string.deleteImages) + String.format( " %d", FetcherService.mDeletedImageCount ) );
                }
            }
            cursor.close();
        }
    }

    public static boolean needDownloadPictures() {
        String fetchPictureMode = PrefUtils.getPreloadImagesMode();

        boolean downloadPictures = false;
        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
            if (Constants.FETCH_PICTURE_MODE_ALWAYS_PRELOAD.equals(fetchPictureMode)) {
                downloadPictures = true;
            } else if (Constants.FETCH_PICTURE_MODE_WIFI_ONLY_PRELOAD.equals(fetchPictureMode)) {
                ConnectivityManager cm = (ConnectivityManager) MainApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI ) {
                    downloadPictures = true;
                }
            }
        }
        return downloadPictures;
    }

    public static String getBaseUrl(String link) {
        String baseUrl = link;
        Pattern p = Pattern.compile("(http?.://[^/]+)");
        Matcher m = p.matcher(baseUrl);
        if (m.find())
            baseUrl = m.group(1);  // The matched substring
        else {
            if ( link.endsWith( "/" ) )
                link = link.substring(0, link.length() - 1 );
            int index = link.lastIndexOf('/'); // this also covers https://
            if (index > -1) {
                baseUrl = link.substring(0, index + 1);
            }
        }
        return baseUrl;
    }

    public static String getUrlDomain(String link) {
        String result = link;
        result = result.replaceAll( "http.+?//", "" );
        result = result.replaceAll( "http.+?/", "" );
        if ( result.endsWith( "/" ) )
            result = result.substring(0, result.length() - 1 );

        int index = result.lastIndexOf('/'); // this also covers https://
        if (index > -1) {
            result = result.substring(0, index + 1);
        }
        result = result.replace( "www.", "" );
        return result;
    }
    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            FetcherService.Status().AddBytes( n );
            output.write(buffer, 0, n);
        }

        byte[] result = output.toByteArray();

        output.close();
        inputStream.close();
        return result;
    }

    public static void retrieveFavicon(Context context, URL url, String feedID) {
        final String imageUrl = url.getProtocol() + PROTOCOL_SEPARATOR + url.getHost() + FILE_FAVICON;
        ContentResolver cr = context.getContentResolver();
        try (Cursor cursor = cr.query(FeedData.FeedColumns.CONTENT_URI(feedID), new String[]{FeedData.FeedColumns.ICON_URL}, null, null, null)) {
            if (!cursor.moveToFirst() || (!cursor.isNull(0) && mImageFileVoc.isExists(GetImageFile(imageUrl, imageUrl).getPath())))
                return;
        }
        try {
            downloadImage( -1, imageUrl, imageUrl, true, false );
            ContentValues values = new ContentValues();
            values.put(FeedData.FeedColumns.ICON_URL, imageUrl);
            cr.update(FeedData.FeedColumns.CONTENT_URI(feedID), values, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HttpURLConnection setupConnection(String url, int timeout) throws IOException {
        return setupConnection(new URL(url), timeout);
    }

    public static Bitmap downloadImage(String url) {
        Bitmap bitmap = null;
        try {
            Connection connection = new Connection( url, OKHTTP ); try {

                byte[] iconBytes = getBytes(connection.getInputStream());
                if (iconBytes != null && iconBytes.length > 0) {
                    bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                    if (bitmap != null) {
                        if (bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {

                        }
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            DebugApp.AddErrorToLog(null, e);
        }
        return bitmap;
    }

    public static HttpURLConnection setupConnection(URL url, int timeout) throws IOException {
        FetcherService.Status().ChangeProgress(R.string.setupConnection);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();//; new OkUrlFactory(new OkHttpClient()).open(url);


        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setRequestProperty("User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari"); // some feeds need this to work properly
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("accept", "*/*");

        COOKIE_MANAGER.getCookieStore().removeAll(); // Cookie is important for some sites, but we clean them each times
        connection.connect();
        FetcherService.Status().ChangeProgress("");
        return connection;
    }

    static public String ToString( InputStream inputStream ) throws IOException {
        int ch;
        StringBuilder sb = new StringBuilder();
        while((ch = inputStream.read()) != -1)
                sb.append((char)ch);
        if ( inputStream.markSupported() )
            inputStream.reset();
        return sb.toString();
    }

    private static class PictureFilenameFilter implements FilenameFilter {
        private static final String REGEX = "__.*";

        private Pattern mPattern;

        public PictureFilenameFilter(String entryId) {
            setEntryId(entryId);
        }

        PictureFilenameFilter() {
        }

        void setEntryId(String entryId) {
            mPattern = Pattern.compile(entryId + REGEX);
        }

        @Override
        public boolean accept(File dir, String filename) {
            return mPattern.matcher(filename).find();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String formatXML(final String unformattedXML) {
        final int length = unformattedXML.length();
        final int indentSpace = 3;
        final StringBuilder newString = new StringBuilder(length + length / 10);
        final char space = ' ';
        int i = 0;
        int indentCount = 0;
        char currentChar = unformattedXML.charAt(i++);
        char previousChar = currentChar;
        boolean nodeStarted = true;
        newString.append(currentChar);
        for (; i < length - 1;) {
            currentChar = unformattedXML.charAt(i++);

            final String TAG = "<br>";
            if ( i < unformattedXML.length() - TAG.length() && unformattedXML.substring( i, i + TAG.length() ).equals( TAG ) ) {
                newString.append( TAG );
                i += TAG.length();
                continue;
            }

            if(((int) currentChar < 32 && currentChar != 13 ) && !nodeStarted) {
                //continue;
            }

            switch (currentChar) {
                case '<':
                    if ('>' == previousChar && '/' != unformattedXML.charAt(i - 1) && '/' != unformattedXML.charAt(i) &&
                            '!' != unformattedXML.charAt(i)) {
                        indentCount++;
                    }
                    newString.append(System.lineSeparator());
                    for (int j = indentCount * indentSpace; j > 0; j--) {
                        newString.append(space);
                    }
                    newString.append(currentChar);
                    nodeStarted = true;
                    break;
                case '>':
                    newString.append(currentChar);
                    nodeStarted = false;
                    break;
                case '/':
                    if ('<' == previousChar || '>' == unformattedXML.charAt(i)) {
                        indentCount--;
                    }
                    newString.append(currentChar);
                    break;
                default:
                    newString.append(currentChar);
            }
            previousChar = currentChar;
        }
        newString.append(unformattedXML.charAt(length - 1));
        return newString.toString();
        //System.out.println(newString.toString());
    }

    public static Uri GetImageFileUri( String entryUrl, String imageUrl ) {
        return Uri.fromFile( GetImageFile( entryUrl, imageUrl ) );
    }

    public static File GetImageFile( String entryUrl, String imageUrl ) {
        final String imgFilePath = NetworkUtils.getDownloadedImagePath(entryUrl, imageUrl);
        return new File(imgFilePath );
    }
}

;