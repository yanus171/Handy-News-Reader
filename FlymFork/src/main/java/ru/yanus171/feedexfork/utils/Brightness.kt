@file:Suppress("FunctionName")

package ru.yanus171.feedexfork.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.TextView

import java.util.Date

import ru.yanus171.feedexfork.R

import android.provider.Settings.System.SCREEN_BRIGHTNESS
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import ru.yanus171.feedexfork.utils.PrefUtils.GetTapZoneSize
import ru.yanus171.feedexfork.utils.UiUtils.SetSize

class Brightness(private val mActivity: Activity, rootView: View) {
    private val mDimFrame: View = rootView.findViewById(R.id.dimFrame)
    private val mInfo: TextView = rootView.findViewById(R.id.brightnessInfo)
    var mCurrentAlpha = 128

    init {
        mInfo.visibility = View.GONE
        mCurrentAlpha = PrefUtils.getInt(PrefUtils.LAST_BRIGHTNESS, mCurrentAlpha)
        UiUtils.HideButtonText(rootView, R.id.brightnessSlider, true)
        SetSize(rootView, R.id.brightnessSlider, GetTapZoneSize(), MATCH_PARENT)

        if (PrefUtils.getBoolean(PrefUtils.BRIGHTNESS_GESTURE_ENABLED, false))
            rootView.findViewById<View>(R.id.brightnessSlider).setOnTouchListener(object : View.OnTouchListener {
                private var paddingX = 0
                private var paddingY = 0
                private var initialX = 0
                private var initialY = 0
                private var currentX = 0
                private var currentY = 0
                private var mInitialAlpha = 0

                @SuppressLint("ClickableViewAccessibility", "DefaultLocale")
                override fun onTouch(view1: View, event: MotionEvent): Boolean {

                    when {
                        event.action == MotionEvent.ACTION_DOWN -> {
                            paddingX = 0
                            paddingY = 0
                            initialX = event.x.toInt()
                            initialY = event.y.toInt()
                            currentX = event.x.toInt()
                            currentY = event.y.toInt()
                            mInitialAlpha = mCurrentAlpha
                            Dog.v("onTouch ACTION_DOWN")
                            return true
                        }
                        event.action == MotionEvent.ACTION_MOVE -> {

                            currentX = event.x.toInt()
                            currentY = event.y.toInt()
                            paddingX = currentX - initialX
                            paddingY = currentY - initialY

                            if (Math.abs(paddingY) > Math.abs(paddingX) && Math.abs(initialY - event.y) > view1.width) {
                                Dog.v("onTouch ACTION_MOVE $paddingX, $paddingY")
                                var currentAlpha = mInitialAlpha + 255 * paddingY / mDimFrame.height / 5
                                if (currentAlpha > 255)
                                    currentAlpha = 255
                                else if (currentAlpha < 1)
                                    currentAlpha = 1
                                setBrightness(currentAlpha)
                                mInfo.visibility = View.VISIBLE
                                mInfo.text = String.format("%s: %d %%",
                                        mInfo.context.getString(R.string.brightness),
                                        ((255 - currentAlpha) / 255.toFloat() * 100).toInt())
                            }
                            return true
                        }
                        event.action == MotionEvent.ACTION_UP -> {
                            mInfo.visibility = View.GONE
                            return false
                        }
                        else -> return false
                    }

                }
            })

    }

    fun OnResume() {
        if (!PrefUtils.getBoolean(PrefUtils.BRIGHTNESS_GESTURE_ENABLED, false))
            return
        val period = (PrefUtils.getIntFromText("settings_brightness_read_from_system_period_min", 10) * 1000 * 60).toLong()
        val now = Date().time
        var brightness = PrefUtils.getInt(PrefUtils.LAST_BRIGHTNESS, 0)
        if (period != 0L && now - PrefUtils.getLong(PrefUtils.LAST_BRIGHTNESS_ONPAUSE_TIME, now) > period)
        //brightness = 255 - (int) (mActivity.getWindow().getAttributes().screenBrightness / (float)255 * 100);
            brightness = 255 - android.provider.Settings.System.getInt(mActivity.contentResolver, SCREEN_BRIGHTNESS, brightness)
        setBrightness(brightness)
    }

    fun OnPause() {
        PrefUtils.putInt(PrefUtils.LAST_BRIGHTNESS, mCurrentAlpha)
        PrefUtils.putLong(PrefUtils.LAST_BRIGHTNESS_ONPAUSE_TIME, Date().time)
    }
    //    private int GetAlpha() {
    //        if ( PrefUtils.getBoolean( "brightness_with_dim_activity", false ) )
    //            return mColor.alpha( ( (ColorDrawable)mDimFrame.getBackground() ).getColor() );
    //        else
    //            return 255 - (int) (255 * mActivity.getWindow().getAttributes().screenBrightness);
    //    }

    @SuppressLint("DefaultLocale")
    private fun setBrightness(currentAlpha: Int) {
        mCurrentAlpha = currentAlpha
        Dog.d(String.format("setBrightness currentAlpha=%d", currentAlpha))
        if (PrefUtils.getBoolean("brightness_with_dim_activity", false)) {
            SetWindowBrightness(0)
            val newColor = Color.argb(currentAlpha, 0, 0, 0)
            mDimFrame.setBackgroundColor(newColor)
        } else {
            mDimFrame.setBackgroundColor(Color.TRANSPARENT)
            SetWindowBrightness(currentAlpha)
        }
    }

    private fun SetWindowBrightness(currentAlpha: Int) {
        setBrightness(currentAlpha, mActivity.window)
    }

    companion object {

        fun setBrightness(currentAlpha: Int, window: Window) {
            val brightness = if ( currentAlpha == 255 ) { 1  } else { 255 - currentAlpha }
            //                    if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite( getContext() )) {
            //                        android.provider.Settings.System.putInt(getContext().getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            //                        android.provider.Settings.System.putInt(getContext().getContentResolver(), SCREEN_BRIGHTNESS, currentAlpha);
            //                    }
            val lp = window.attributes
            lp.screenBrightness = brightness / 255.toFloat()
            window.attributes = lp
            //getActivity().getWindow().addFlags( WindowManager.LayoutParams.FLAGS_CHANGED );
        }
    }

}
