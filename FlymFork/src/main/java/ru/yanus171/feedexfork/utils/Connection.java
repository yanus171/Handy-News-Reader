package ru.yanus171.feedexfork.utils;

import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Connection {
    HttpURLConnection mConnection = null;
    Response mResponse = null;
    public Connection( String url ) {
        try {

            if (IsOkHttp()) {
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Call call = new OkHttpClient().newCall(request);
                mResponse = call.execute();
            } else
                mConnection = NetworkUtils.setupConnection1(url);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public InputStream getInputStream() throws IOException {
        if (IsOkHttp())
            return mResponse.body().byteStream();
        else
            return mConnection.getInputStream();
    }

    public static boolean IsOkHttp() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public long getContentLength() {
        if (IsOkHttp()) {
            return mResponse.body().contentLength();
        } else {
            return mConnection.getContentLength();
        }
    }

    public String getContentType() {
        if (IsOkHttp()) {
            return mResponse.body().contentType().type();
        } else {
            return mConnection.getContentType();
        }
    }

    public void disconnect() {
        if (IsOkHttp()) {

        } else {
            mConnection.disconnect();
            mConnection = null;
        }
    }

}
