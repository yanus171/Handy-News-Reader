package ru.yanus171.feedexfork.utils;

import static ru.yanus171.feedexfork.parser.OPML.FILENAME_DATETIME_FORMAT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.activity.MessageBox;

public class Log {
    private static Queue<String> mList = new ArrayDeque<>();
    private static final String TIME_FORMAT = "HH:mm:ss.SSS";

    @SuppressLint("SimpleDateFormat")
    static public void Add(String text ) {
        mList.add(String.format( "%s %s\n", new SimpleDateFormat(TIME_FORMAT).format(new Date(System.currentTimeMillis() )), text ) );
        if (mList.size() > 1000)
            mList.poll();
    }
    // --------------------------------------------------------------------------
    public static void Show( Context context ) {
        Uri fileUri = DebugApp.CreateFileUri(context.getCacheDir().getAbsolutePath(), getFileName(), mList.toString());
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(fileUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch ( ActivityNotFoundException ignored ) {
            MessageBox.Show(mList.toString());
        }
    }

    @SuppressLint("SimpleDateFormat")
    private static String getFileName() {
        return String.format( "log_%s", new SimpleDateFormat(FILENAME_DATETIME_FORMAT).format(new Date(System.currentTimeMillis() ) ) );
    }
}
