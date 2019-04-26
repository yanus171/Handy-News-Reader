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
package ru.yanus171.feedexfork.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static ru.yanus171.feedexfork.utils.PrefUtils.GetTapZoneSize;
import static ru.yanus171.feedexfork.utils.UiUtils.SetSize;


public abstract class BaseActivity extends AppCompatActivity {

    View mDecorView;
    private Brightness mBrightness = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( mBrightness == null && findViewById( R.id.dimFrame ) != null )
            mBrightness = new Brightness( this, findViewById( R.id.dimFrame ).getRootView() );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(Constants.NOTIFICATION_ID_REFRESH_SERVICE);
        }
        if ( mBrightness != null ) {
            mBrightness.OnResume();
        }
    }

    @Override
    public void onPause() {
        if ( mBrightness != null ) {
            mBrightness.OnPause();
        }
        super.onPause();
    }
}
class Brightness {
    final private View mDimFrame;
    final private TextView mInfo;
    final private Activity mActivity;
    private int mCurrentAlpha = 128;
    Brightness(Activity activity, View rootView) {
        mActivity = activity;
        mDimFrame = rootView.findViewById( R.id.dimFrame );
        mInfo = rootView.findViewById( R.id.brightnessInfo );
        mInfo.setVisibility( View.GONE );
        mCurrentAlpha = PrefUtils.getInt( PrefUtils.LAST_BRIGHTNESS, mCurrentAlpha );
        UiUtils.HideButtonText( rootView, R.id.brightnessSlider, true );
        SetSize( rootView, R.id.brightnessSlider, GetTapZoneSize(), MATCH_PARENT );

        if ( PrefUtils.getBoolean( "brightness_gesture_enabled", false ) )
            rootView.findViewById( R.id.brightnessSlider ).setOnTouchListener(new View.OnTouchListener() {
                private int paddingX = 0;
                private int paddingY = 0;
                private int initialX = 0;
                private int initialY = 0;
                private int currentX = 0;
                private int currentY = 0;
                private int mInitialAlpha = 0;

                @SuppressLint({"ClickableViewAccessibility", "DefaultLocale"})
                @Override
                public boolean onTouch(View view1, MotionEvent event) {

                    if ( event.getAction() == MotionEvent.ACTION_DOWN) {
                        paddingX = 0;
                        paddingY = 0;
                        initialX = (int) event.getX();
                        initialY = (int) event.getY();
                        currentX = (int) event.getX();
                        currentY = (int) event.getY();
                        mInitialAlpha = mCurrentAlpha;
                        Dog.v( "onTouch ACTION_DOWN" );
                        return true;
                    } else  if ( event.getAction() == MotionEvent.ACTION_MOVE ) {

                        currentX = (int) event.getX();
                        currentY = (int) event.getY();
                        paddingX = currentX - initialX;
                        paddingY = currentY - initialY;

                        if ( Math.abs( paddingY ) > Math.abs( paddingX ) &&
                                Math.abs( initialY - event.getY() ) > view1.getWidth()  ) {
                            Dog.v( "onTouch ACTION_MOVE " + paddingX + ", " + paddingY );
                            int currentAlpha = mInitialAlpha + 255 * paddingY / mDimFrame.getHeight() / 2;
                            if ( currentAlpha > 255 )
                                currentAlpha = 255;
                            else if ( currentAlpha < 1 )
                                currentAlpha = 1;
                            SetBrightness( currentAlpha );
                            mInfo.setVisibility(View.VISIBLE);
                            mInfo.setText(String.format("%s: %d %%",
                                    mInfo.getContext().getString(R.string.brightness),
                                    (int)( ( 255 - currentAlpha ) / (float) 255 * 100)));
                        }
                        return true;
                    } else  if ( event.getAction() == MotionEvent.ACTION_UP) {
                        if ( mInfo != null )
                            mInfo.setVisibility( View.GONE );
                        return false;
                    }

                    return false;
                }
            });

    }
    void OnResume() {
        if ( PrefUtils.getBoolean( "brightness_gesture_enabled", false ) )
            SetBrightness( PrefUtils.getInt( PrefUtils.LAST_BRIGHTNESS, 0 ) );
    }
//    private int GetAlpha() {
//        if ( PrefUtils.getBoolean( "brightness_with_dim_activity", false ) )
//            return Color.alpha( ( (ColorDrawable)mDimFrame.getBackground() ).getColor() );
//        else
//            return 255 - (int) (255 * mActivity.getWindow().getAttributes().screenBrightness);
//    }

    @SuppressLint("DefaultLocale")
    private void SetBrightness(int currentAlpha) {
        mCurrentAlpha = currentAlpha;
        Dog.d( String.format( "SetBrightness currentAlpha=%d", currentAlpha) );
        if ( PrefUtils.getBoolean( "brightness_with_dim_activity", false ) ) {
            SetWindowBrightness(0);
            int newColor = Color.argb( currentAlpha, 0,  0,  0 );
            mDimFrame.setBackgroundColor( newColor );
        } else {
            mDimFrame.setBackgroundColor( Color.TRANSPARENT );
            SetWindowBrightness(currentAlpha);
        }
    }

    private void SetWindowBrightness(int currentAlpha) {
        final int brightness = 255 - currentAlpha;
//                    if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite( getContext() )) {
//                        android.provider.Settings.System.putInt(getContext().getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
//                        android.provider.Settings.System.putInt(getContext().getContentResolver(), SCREEN_BRIGHTNESS, currentAlpha);
//                    }
        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
        lp.screenBrightness = brightness / (float) 255;
        mActivity.getWindow().setAttributes(lp);
        //getActivity().getWindow().addFlags( WindowManager.LayoutParams.FLAGS_CHANGED );
    }

    void OnPause() {
        PrefUtils.putInt( PrefUtils.LAST_BRIGHTNESS, mCurrentAlpha);
    }
}
