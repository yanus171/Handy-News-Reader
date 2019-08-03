package ru.yanus171.feedexfork.view;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.MainApplication.NOTIFICATION_CHANNEL_ID;

/**
 * Created by Admin on 03.06.2016.
 */


public class StatusText implements Observer {
    private static final String SEP = "__";
    private String mFeedID = "";
    private String mEntryID = "";
    private TextView mView;
    private TextView mErrorView;
    //SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;
    private static int MaxID = 0;

    public StatusText(final TextView view, final TextView errorView, final Observable observable ) {
        //mOnRefreshListener = onRefreshListener;
        observable.addObserver( this );
        mView = view;
        mErrorView = errorView;
        mView.setVisibility(View.GONE);
        mView.setGravity(Gravity.LEFT | Gravity.TOP);
        mView.setBackgroundColor(Color.TRANSPARENT );
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FetcherObservable status = (FetcherObservable)observable;
                status.Clear();
                v.setVisibility(View.GONE);

            }
        });
        mView.setLines( 2 );

        mErrorView.setVisibility(View.GONE);
        mErrorView.setGravity(Gravity.LEFT | Gravity.TOP);
        mErrorView.setBackgroundColor(Color.TRANSPARENT );
        mErrorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FetcherObservable status = (FetcherObservable)observable;
                status.ClearError();
                v.setVisibility(View.GONE);

            }
        });
        mErrorView.setLines( 2 );
    }

    public void SetFeedID( String feedID ) {
        mFeedID = feedID;
        FetcherService.Status().UpdateText();
    }
    public void SetFeedID( Uri uri ) {
        SetFeedID( uri.getPathSegments().size() > 1 ? uri.getPathSegments().get(1) : "" );
    }
    public void SetEntryID( String entryID ) {
        mEntryID = entryID;
        FetcherService.Status().UpdateText();
    }


    @Override
    public void update(Observable observable, Object data) {
        String[] list = TextUtils.split( (String)data, SEP );
        String text = list[0];
        final String errorFeedID = list[2];
        final String errorEntryID = list[3];
        final String error =
            errorFeedID.equals( mFeedID ) && mEntryID.isEmpty() ||
            errorEntryID.equals( mEntryID )  && !mEntryID.isEmpty() ? list[1] : "";

        mView.post (new Runnable() {
            private String mError;
            private String mText;

            @Override
            public void run() {
                if ( !PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ) || mText.trim().isEmpty() )
                    mView.setVisibility(View.GONE);
                else {
                    mView.setText(mText);
                    mView.setVisibility(View.VISIBLE);
                }
                mErrorView.setText(mError);
                mErrorView.setVisibility( mError.isEmpty() ? View.GONE : View.VISIBLE );
            }
            Runnable init( String text, String error ) {
                mText = text;
                mError = error;
                return this;
            }
        }.init(text, error));

    }


    public static class FetcherObservable extends Observable {
        public volatile int mBytesRecievedLast = 0;
        final LinkedHashMap<Integer,String> mList = new LinkedHashMap<>();
        private String mProgressText = "";
        private String mErrorText = "";
        private String mDBText = "";
        private long mLastNotificationUpdateTime = ( new Date() ).getTime();
        private String mNotificationTitle = "";
        private String mErrorFeedID;
        private String mErrorEntryID;
        @Override
        public boolean hasChanged () {
            return true;
        }

        void UpdateText() {
            UiUtils.RunOnGuiThreadInFront(new Runnable() {
                @Override
                public void run() {
                synchronized ( mList ) {
                    String s = "";
                    if ( PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ) )
                        for( java.util.Map.Entry<Integer,String> item: mList.entrySet() )
                                s += item.getValue() + " ";

                    if ( PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ) ) {
                        if (!mProgressText.isEmpty())
                            s += " " + mProgressText;
                        if (!mList.isEmpty() && !mDBText.isEmpty())
                            s += " " + mDBText;
                        if (!mList.isEmpty() && FetcherService.mCancelRefresh)
                            s += "\n cancel Refresh";
                        if (mBytesRecievedLast > 0)
                            s = String.format("(%.2f MB) ", (float) mBytesRecievedLast / 1024 / 1024) + s;
                    }
                    if ( PrefUtils.getBoolean( PrefUtils.IS_REFRESHING, false ) &&
                       ( ( new Date() ).getTime() - mLastNotificationUpdateTime  > 1000 ) ) {

                        Constants.NOTIF_MGR.notify(Constants.NOTIFICATION_ID_REFRESH_SERVICE, GetNotification(s, mNotificationTitle));
                        mLastNotificationUpdateTime = ( new Date() ).getTime();
                    }
                    if ( !mNotificationTitle.isEmpty() )
                        s = mNotificationTitle + " " + s;
                    NotifyObservers(s, mErrorText, mErrorFeedID, mErrorEntryID);
                    Dog.v("Status Update " + s.replace("\n", " "));
                }
                }
            });
        }

        public void NotifyObservers(String text, String error, String errorFeedID, String errorEntryID) {
            notifyObservers(text + SEP + error + SEP + errorFeedID + SEP + errorEntryID);
        }

        void Clear() {
            synchronized ( mList ) {
                mList.clear();
                mProgressText = "";
                mDBText = "";
                //mErrorText = "";
                mBytesRecievedLast = 0;
            }
        }
        public void ClearError() {
            synchronized ( mList ) {
                mErrorText = "";
            }
        }
        public int Start( final String text ) {
            Dog.v("Status Start " + text);
            synchronized ( mList ) {
                if ( mList.isEmpty() )
                    mBytesRecievedLast = 0;
                MaxID++;
                mList.put(MaxID, text );
            }
            UpdateText();
            return MaxID;
        }
        public void End( int id ) {
            Dog.v( "Status End " );
            synchronized ( mList ) {
                mProgressText = "";
                mList.remove( id );
            }
            UpdateText();
        }

        public void ChangeProgress(String text) {
            synchronized ( mList ) {
                mProgressText = text;
            }
            UpdateText();
        }
        public void SetError( String text, String feedID, String entryID, Exception e ) {
            Dog.e( "Error", e );
            if ( e != null )
                e.printStackTrace();
            synchronized ( mList ) {
                mErrorFeedID = feedID;
                mErrorEntryID = entryID;
                mErrorText = ( text == null ? "" : text + ", " ) + e.toString();
            }
            UpdateText();
        }
        public void ChangeProgress(int textID) {
            ChangeProgress(MainApplication.getContext().getString( textID ));
        }
        public void ChangeDB(String text) {
            synchronized ( mList ) {
                mDBText = text;
            }
            UpdateText();
        }
        public void SetNotificationTitle(String text) {
            synchronized ( mList ) {
                mNotificationTitle = text;
            }
            UpdateText();
        }
        public void AddBytes(int bytes) {
            //synchronized ( mList ) {
                mBytesRecievedLast += bytes;
            //}
        }
        public void HideByScroll() {
            UiUtils.RunOnGuiThread( new Runnable() {
                    @Override
                    public void run() {
                synchronized (mList) {
                    if ( mList.isEmpty() ) {
                        mBytesRecievedLast = 0;
                        NotifyObservers( "", "", "", "" );
                    }
                }
                }
            });
        }
    }



    static public Notification GetNotification(final String text, final String title ) {
        final Context context = MainApplication.getContext();
        final PendingIntent pIntent = PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), 0 );

        if (Build.VERSION.SDK_INT >= 26 ) {
            Notification.Builder builder;
            Notification.BigTextStyle bigTextStyle =
                    new Notification.BigTextStyle();
            bigTextStyle.bigText(text);
            bigTextStyle.setBigContentTitle(title);
            builder = new Notification.Builder(MainApplication.getContext(), NOTIFICATION_CHANNEL_ID) //
                    .setSmallIcon(R.drawable.refresh) //
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)) //
                    .setStyle( bigTextStyle )
                    .setContentIntent( pIntent );
            return builder.build();
        } else {
            NotificationCompat.BigTextStyle bigTextStyle =
                    new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText(text);
            bigTextStyle.setBigContentTitle(title);
            android.support.v4.app.NotificationCompat.Builder builder =
                    new android.support.v4.app.NotificationCompat.Builder(MainApplication.getContext()) //
                            .setSmallIcon(R.drawable.refresh) //
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)) //
                            .setStyle(bigTextStyle)
                            .setContentIntent( pIntent );
            return builder.build();
        }
    }


}

