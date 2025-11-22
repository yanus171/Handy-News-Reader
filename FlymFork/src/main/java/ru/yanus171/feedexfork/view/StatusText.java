package ru.yanus171.feedexfork.view;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
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
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.MainApplication.OPERATION_NOTIFICATION_CHANNEL_ID;

/**
 * Created by Admin on 03.06.2016.
 */


public class StatusText implements Observer {
    private static final String SEP = "__#__";
    private static final String DELIMITER = " ";
    private static int PendingIntentRequestCode = 0;
    private final TextView mProgressText;
    static private Date mLastClearStatusTime = null;
    private String mFeedID = "";
    private String mEntryID = "";
    private TextView mView;
    private TextView mErrorView;
    //SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;
    private static int MaxID = 0;
    private ProgressBar mProgressBar;

    public StatusText(final TextView view,
                      final TextView errorView,
                      final ProgressBar progressBar,
                      final TextView progressText,
                      final Observable observable ) {
        //mOnRefreshListener = onRefreshListener;
        observable.addObserver( this );
        mView = view;
        mErrorView = errorView;
        mView.setVisibility(View.GONE);
        mView.setGravity(Gravity.START | Gravity.TOP);
        mView.setTextColor( Theme.GetTextColorReadInt() );
        mView.setBackgroundColor(Color.parseColor( Theme.GetBackgroundColor() ) );
        mView.setOnClickListener(v -> {
            FetcherObservable status = (FetcherObservable)observable;
            status.Clear();
            v.setVisibility(View.GONE);
            mLastClearStatusTime = new Date();
        });
        mView.setLines( 2 );
        UiUtils.SetSmallFont( mView );

        mErrorView.setVisibility(View.GONE);
        mErrorView.setGravity(Gravity.START | Gravity.TOP);
        mErrorView.setBackgroundColor( Theme.GetErrorTextBackroundColorInt() );
        mErrorView.setTextColor( Theme.GetErrorTextColorInt() );
        mErrorView.setOnClickListener(v -> {
            FetcherObservable status = (FetcherObservable)observable;
            status.ClearError();
            v.setVisibility(View.GONE);
        });
        UiUtils.SetSmallFont( mErrorView );

        mProgressBar = progressBar;
        mProgressBar.setOnClickListener(v -> {
            FetcherObservable status = (FetcherObservable)observable;
            status.ToggleProgressTextVisibility();
        });

        mProgressText = progressText;
    }

    private void SetFeedID(String feedID) {
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

    public int GetHeight() {
        int result = 0;
        if ( mView.getVisibility() == View.VISIBLE )
            result += mView.getHeight();
        if ( mErrorView.getVisibility() == View.VISIBLE )
            result += mErrorView.getHeight();
        return result;
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
        final boolean showProgress = PrefUtils.getBoolean( "settings_show_circle_progress", true ) && Boolean.valueOf( list[4] );
        final String progressText = list[5];
        final long CLEAR_STATUS_PERIOD = 1000 * 60;
        if ( !PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false )
            || text.trim().isEmpty()
            || ( mLastClearStatusTime != null && new Date().getTime() - mLastClearStatusTime.getTime() < CLEAR_STATUS_PERIOD ) )
            mView.setVisibility(View.GONE);
        else {
            mView.setText(text);
            mView.setVisibility(View.VISIBLE);
        }
        mErrorView.setText(error);
        mErrorView.setVisibility( error.isEmpty() ? View.GONE : View.VISIBLE );
        mProgressBar.setVisibility( showProgress ? View.VISIBLE : View.GONE  );

        if ( mProgressText != null ) {
            mProgressText.setVisibility(!progressText.isEmpty() ? View.VISIBLE : View.GONE);
            mProgressText.setText(progressText);
        }
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
        ArrayList<Integer> mProgressBarStatusList = new ArrayList<>();
        private boolean mIsProgressTextVisible = false;
        public volatile boolean mIsHideByScrollEnabled = true;
        private PendingIntent mCancelPI = null;

        @Override
        public boolean hasChanged () {
            return true;
        }

        public void UpdateText() {
            UiUtils.RunOnGuiThread(() -> {
                synchronized ( mList ) {
                    ArrayList<String> s = new ArrayList<>();
                    for( java.util.Map.Entry<Integer,String> item: mList.entrySet() )
                            s.add( item.getValue() );

                    if (!mProgressText.isEmpty())
                        s.add( mProgressText );
                    if (!mList.isEmpty() && !mDBText.isEmpty())
                        s.add( mDBText );
                    if (!mList.isEmpty() && FetcherService.mCancelRefresh)
                        s.add( "\n cancel Refresh" );
                    if (mBytesRecievedLast > 0)
                        s.add(0, String.format("(%.2f MB) ", (float) mBytesRecievedLast / 1024 / 1024) );
                    if ( PrefUtils.getBoolean( PrefUtils.IS_REFRESHING, false ) &&
                       ( ( new Date() ).getTime() - mLastNotificationUpdateTime  > 1000 ) ) {

                        Constants.NOTIF_MGR.notify(Constants.NOTIFICATION_ID_REFRESH_SERVICE, GetNotification(TextUtils.join(DELIMITER, s ), mNotificationTitle, R.drawable.ic_sync, OPERATION_NOTIFICATION_CHANNEL_ID, mCancelPI));
                        mLastNotificationUpdateTime = ( new Date() ).getTime();
                    }
                    if ( !mNotificationTitle.isEmpty() )
                        s.add( 0, mNotificationTitle );
                    HashSet<Integer> temp = new HashSet<>( mList.keySet() );
                    temp.retainAll( mProgressBarStatusList );
                    final boolean showProgress = !temp.isEmpty();
                    if ( !showProgress )
                        mIsProgressTextVisible = false;
                    final String progressText = mIsProgressTextVisible && !mProgressBarStatusList.isEmpty() ?
                                    mList.get( mProgressBarStatusList.get(0) ) + " " + mProgressText  : "";
                    NotifyObservers( TextUtils.join( DELIMITER, s ), mErrorText, mErrorFeedID, mErrorEntryID, showProgress, progressText );
                    Dog.v("Status Update " + TextUtils.join( " ", s ).replace("\n", " "));


                }
            });
        }

        void NotifyObservers(String text, String error, String errorFeedID, String errorEntryID, boolean showProgressBar, String progressText ) {
            notifyObservers(text + SEP + error + SEP + errorFeedID + SEP + errorEntryID + SEP + showProgressBar + SEP + progressText );
        }

        void Clear() {
            synchronized ( mList ) {
                mList.clear();
                mProgressBarStatusList.clear();
                mProgressText = "";
                mDBText = "";
                //mErrorText = "";
                mBytesRecievedLast = 0;
            }
            UpdateText();
        }
        public void ClearError() {
            synchronized ( mList ) {
                mErrorText = "";
            }
            UpdateText();
        }
        public int Start( final int textId, boolean startProgress ) {
            return Start( MainApplication.getContext().getString( textId ), startProgress );
        }
        public int Start( final String text, boolean startProgress ) {
            synchronized ( mList ) {
                if ( mList.isEmpty() ) {
                    mBytesRecievedLast = 0;
                }
                MaxID++;
                mList.put(MaxID, text );
                if ( startProgress )
                    mProgressBarStatusList.add( MaxID );
                Dog.v("Status Start " + text + " id = " + MaxID );

                UpdateText();
                return MaxID;
            }
        }
        public void End( int id ) {
            Dog.v( "Status End " + id );
            synchronized ( mList ) {
                mProgressText = "";
                mList.remove( id );
                {
                    int index = mProgressBarStatusList.indexOf(id);
                    if (index >= 0)
                        mProgressBarStatusList.remove(index);
                }
            }
            UpdateText();
        }

        public void Change( int id, String newText ) {
            Dog.v( "Status change " + newText + " id = " + id );
            synchronized ( mList ) {
                mProgressText = "";
                mList.put( id, newText );
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
            return;
//            synchronized ( mList ) {
//                mDBText = text;
//            }
//            UpdateText();
        }
        public void SetNotificationTitle(String text, PendingIntent cancelPI) {
            synchronized ( mList ) {
                mCancelPI = cancelPI;
                mNotificationTitle = text;
            }
            UpdateText();
        }
        public void AddBytes(int bytes) {
            synchronized ( mList ) {
                mBytesRecievedLast += bytes;
            }
        }
        public void ResetBytes() {
            synchronized ( mList ) {
                mBytesRecievedLast = 0;
            }
        }
        public void HideByScroll() {
            if ( mIsHideByScrollEnabled )
                UiUtils.RunOnGuiThread( new Runnable() {
                        @Override
                        public void run() {
                    synchronized (mList) {
                        if ( mList.isEmpty() ) {
                            mBytesRecievedLast = 0;
                            NotifyObservers( "", mErrorText, mErrorFeedID, mErrorEntryID, false, "" );
                        }
                    }
                    }
                });
        }

        public void ClearProgress() {
            synchronized ( mList ) {
                for ( int id: mProgressBarStatusList )
                    mList.remove( id );
                mProgressBarStatusList.clear();
            }
            UpdateText();
        }

        void ToggleProgressTextVisibility() {
            mIsProgressTextVisible = ! mIsProgressTextVisible;
            UpdateText();
        }
    }



    public static int GetPendingIntentRequestCode() {
        PendingIntentRequestCode++;
        if (PendingIntentRequestCode > 10000) {
            PendingIntentRequestCode = 1;
        }
        return PendingIntentRequestCode;
    }
    static public Notification GetNotification(final String text, final String title, int iconResID, String channelID, PendingIntent cancelPI ) {
        final Context context = MainApplication.getContext();
        final PendingIntent pIntent = PendingIntent.getActivity(context, GetPendingIntentRequestCode(), new Intent(context, HomeActivity.class), PendingIntent.FLAG_IMMUTABLE );

        if (Build.VERSION.SDK_INT >= 26 ) {
            Notification.Builder builder;
            Notification.BigTextStyle bigTextStyle =
                    new Notification.BigTextStyle();
            bigTextStyle.bigText(text);
            bigTextStyle.setBigContentTitle(title);
            builder = new Notification.Builder(MainApplication.getContext(), channelID )
                    .setSmallIcon( iconResID )
                    .setLargeIcon( BitmapFactory.decodeResource(context.getResources(), iconResID))
                    .setStyle( bigTextStyle )
                    .setContentIntent( pIntent );
            if ( cancelPI != null )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString( android.R.string.cancel ), cancelPI );
            return builder.build();
        } else {
            NotificationCompat.BigTextStyle bigTextStyle =
                    new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText(text);
            bigTextStyle.setBigContentTitle(title);
            androidx.core.app.NotificationCompat.Builder builder =
                    new androidx.core.app.NotificationCompat.Builder(MainApplication.getContext())
                            .setSmallIcon( iconResID )
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), iconResID))
                            .setStyle(bigTextStyle)
                            .setContentIntent( pIntent );
            return builder.build();
        }
    }


}

