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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

import java.io.FileNotFoundException;
import java.io.InputStream;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.view.FontSelectPreference;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.utils.NetworkUtils.GetImageFileUri;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;

public class UiUtils {
    private static Typeface mCachedTypeFace = null;

    //static private final HashMap<String, Bitmap> FAVICON_CACHE = new HashMap<>();

    static public void setTheme(Activity a) {
        a.setTheme( Theme.GetResID( Theme.STYLE_THEME ) );
    }

    static public void setPreferenceTheme(Activity a) {
        a.setTheme( Theme.GetResID( Theme.PREF_STYLE_THEME ) );
    }

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }
    static public int mmToPixel(int mm) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, getContext().getResources().getDisplayMetrics());
    }
//    static public void clearFaviconCache() {
//        FAVICON_CACHE.clear();
//    }
    static public void addEmptyFooterView(ListView listView, int dp) {
        View view = new View(listView.getContext());
        view.setMinimumHeight(dpToPixel(dp));
        view.setClickable(true);
        listView.addFooterView(view);
    }

    static public void showMessage(@NonNull Activity activity, @StringRes int messageId) {
        showMessage(activity, activity.getString(messageId));
    }

    static public void toast( @StringRes int messageId) {
        Toast.makeText(MainApplication.getContext(), messageId, Toast.LENGTH_LONG).show();
    }
    static public void toastShort( @StringRes int messageId) {
        Toast.makeText(MainApplication.getContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    static public void toast( String message) {
        Toast.makeText(MainApplication.getContext(), message, Toast.LENGTH_LONG).show();
    }

    public static void SetTypeFace(TextView textView) {
        if ( mCachedTypeFace == null )
            mCachedTypeFace = FontSelectPreference.GetTypeFace(FontSelectPreference.KEY );
        textView.setTypeface( mCachedTypeFace );
    }

    static public void SetFont(TextView textView, float sizeCoeff) {
        SetTypeFace( textView );
        textView.setTextSize(COMPLEX_UNIT_DIP, ( 18 + PrefUtils.getFontSizeEntryList() ) * sizeCoeff );
    }

    public static void SetSmallFont(TextView textView) {
        SetTypeFace( textView );
        textView.setTextSize(COMPLEX_UNIT_DIP, 14 + PrefUtils.getFontSizeEntryList() );
    }

    static public void showMessage(@NonNull Activity activity, @NonNull String message) {
        View coordinatorLayout = activity.findViewById(R.id.coordinator_layout);
        Snackbar snackbar = Snackbar.make((coordinatorLayout != null ? coordinatorLayout : activity.findViewById(android.R.id.content)), message, Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundResource(R.color.material_grey_900);
        snackbar.show();
    }

    static public Bitmap getFaviconBitmap( String iconUrl ){
        Bitmap bitmap = null;
        if ( iconUrl != null )
            try {
                InputStream imageStream = getContext().getContentResolver().openInputStream( GetImageFileUri( iconUrl, iconUrl ));
                bitmap = BitmapFactory.decodeStream(imageStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        return bitmap;
    }

    static public Bitmap getScaledBitmap(Bitmap bitmap, int sizeInDp) {
        if (bitmap != null ) {
            //Bitmap bitmap = Bitmap.createBitmap( sourceBitmap );
            if (bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(sizeInDp);
                if (bitmap.getHeight() != bitmapSizeInDip) {
                    Bitmap tmp = bitmap;
                    bitmap = Bitmap.createScaledBitmap(tmp, bitmapSizeInDip, bitmapSizeInDip, false);
                    tmp.recycle();
                }

                return bitmap;
            }
        }

        return null;
    }

    private static Handler mHandler = null;
    public static void RunOnGuiThread( final Runnable r ) {
        if ( mHandler == null )
            mHandler  = new Handler(Looper.getMainLooper());
        //synchronized ( mHandler ) {
            mHandler.post( r );
        //}
    }
    public static void RunOnGuiThread( final Runnable r, int delayMills ) {
        if ( mHandler == null )
            mHandler  = new Handler(Looper.getMainLooper());
        //synchronized ( mHandler ) {
            mHandler.postDelayed( r, delayMills );
        //}

    }
    public static void RemoveFromGuiThread( final Runnable r ) {
        if ( mHandler != null )
            mHandler.removeCallbacks( r );
    }

    public static void UpdateButtonVisibility(View rootView, int ID, boolean visible) {
        TextView btn = rootView.findViewById(ID);
        if ( btn != null ) {
            if (visible)
                btn.setBackgroundResource(ID == R.id.backBtn ? R.drawable.arrow_back_tap_zone : R.drawable.round_background);
            else
                btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setText("");
        }
    }

    public static void UpdateTapZoneButton(View rootView, int ID, boolean visible) {
        TextView btn = rootView.findViewById(ID);
        if ( btn != null ) {
            if (!isArticleTapEnabledTemp())
                btn.setVisibility( View.GONE );
            else {
                btn.setVisibility( View.VISIBLE );
                if (visible)
                    btn.setBackgroundResource( ID == R.id.backBtn ? R.drawable.arrow_back_tap_zone : R.drawable.round_background);
                else
                    btn.setBackgroundColor(Color.TRANSPARENT);
            }
            btn.setText("");
        }
    }



    public static void SetSize( View parent, int ID, int width, int height ) {
        View view = parent.findViewById( ID );
        if ( view == null )
            return;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width;
        lp.height = height;
    }

    // -------------------------------------------------------------------
    static TextView AddSmallText(LinearLayout layout, int textID) {
        return AddSmallText(layout, null, Gravity.LEFT, null, getContext().getString(textID));
    }
    public static TextView AddSmallText(LinearLayout layout, LinearLayout.LayoutParams lp, int gravity, ColorTB color, String text) {
        TextView result = CreateSmallText( layout.getContext(), gravity, color, text );
        if (lp != null) {
            layout.addView(result, lp);
        } else {
            layout.addView(result);
        }
        return result;
    }
    public static TextView CreateSmallText(Context context, int gravity, ColorTB color, String text) {
        TextView result = new TextView(context);
        result.setAutoLinkMask(Linkify.ALL);
        result.setLinkTextColor(Color.LTGRAY);
        result.setText(text);
        result.setTextIsSelectable(true);
        result.setFocusable(false);
        result.setFocusableInTouchMode(true);

        //result.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        result.setGravity(gravity);
        result.setPadding(10, 0, 10, 0);

        result.setTextColor(color != null ? color.Text : Theme.GetMenuFontColor());
        SetSmallFont(result );
        return result;
    }

    // -------------------------------------------------------------------
    public static TextView AddText(LinearLayout layout, LinearLayout.LayoutParams lp, String text) {
        TextView result = CreateTextView(layout.getContext());
        if (lp != null) {
            layout.addView(result, lp);
        } else {
            layout.addView(result);
        }
        result.setText(text);
        result.setTextIsSelectable(true);
        result.setFocusable(false);
        result.setFocusableInTouchMode(false);

        result.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18 + PrefUtils.getFontSizeEntryList());
        result.setTextColor(Theme.GetMenuFontColor());
        result.setGravity(Gravity.CENTER);
        result.setPadding(10, 10, 10, 0);
        return result;
    }


    public static void InvalidateTypeFace() {
        mCachedTypeFace = null;
    }

    public static TextView SetupTextView(View rootView, int id) {
        TextView result = rootView.findViewById(id);
        SetupTextView(result);
        return result;
    }
    public static TextView SetupSmallTextView(View rootView, int id) {
        TextView result = rootView.findViewById(id);
        SetupSmallTextView(result);
        return result;
    }

    public static void SetupSmallTextView(TextView result) {
        //result.setText("");
        SetSmallFont(result);
    }
    public static void SetupTextView(TextView result) {
        //result.setText("");
        SetFont(result, 1);
    }
    public static TextView CreateTextView(Context context) {
        TextView result = new TextView( context );
        SetupTextView( result );
        return result;
    }
}
