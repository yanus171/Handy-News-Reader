package ru.yanus171.feedexfork.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.utils.Theme;

import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;

public class MenuItem {
    public final String text;
    public final int icon;
    public final Intent mIntent;
    public final DialogInterface.OnClickListener mListener;

    public MenuItem(int textID, Integer icon, DialogInterface.OnClickListener listener) {
        this.text = MainApplication.getContext().getString(textID);
        this.icon = icon;
        this.mListener = listener;
        this.mIntent = null;
    }
    public MenuItem(int textID, Integer icon, Intent intent ) {
        this.text = MainApplication.getContext().getString(textID);
        this.icon = icon;
        this.mListener = null;
        this.mIntent = intent;
    }
    MenuItem(String text ) {
        this.text = text;
        this.icon = 0;
        this.mListener = null;
        this.mIntent = null;
    }

    @Override
    public String toString() {
        return text;
    }

    public static void ShowMenu(MenuItem[] items, String title, Context context) {
        final int textColor = Color.parseColor( Theme.GetTextColor() );
        int dp50 = (int) (50 * context.getResources().getDisplayMetrics().density + 0.5f);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setAdapter(new ArrayAdapter<MenuItem>(
            context,
            android.R.layout.select_dialog_item,
            android.R.id.text1,
            items) {
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                //Use super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = SetupTextView(v, android.R.id.text1);
                tv.setTextColor( textColor );
                if (items[position].icon > 0) {
                    //Put the image on the TextView
                    Drawable dr = context.getResources().getDrawable(items[position].icon);
                    if ( dr instanceof BitmapDrawable) {
                        Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                        Drawable d = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, dp50, dp50, true));
                        d.setBounds(0, 0, dp50, dp50);
                        tv.setCompoundDrawables(d, null, null, null);
                    } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && dr instanceof VectorDrawable) {
                        VectorDrawable vd = (VectorDrawable) dr;
                        DrawableCompat.setTint(vd, textColor );
                        vd.setBounds(0, 0, dp50, dp50);
                        tv.setCompoundDrawables(vd, null, null, null);
                    }
                    //Add margin between image and text (support various screen densities)
                    int dp5 = (int) (5 * context.getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);
                } else {
                    tv = SetupSmallTextView(v, android.R.id.text1);
                    tv.setCompoundDrawables(null, null, null, null);
                }
                return v;
            }
        }, (dialogInterface, i) -> {
            if ( items[i].mListener != null )
                items[i].mListener.onClick(dialogInterface, i);
            else if ( items[i].mIntent != null )
                context.startActivity( items[i].mIntent );
        });
        if ( title != null ) {
            TextView view = new TextView( context );
            SetupSmallTextView( view );
            view.setTextColor( textColor );
            view.setText( title );
            view.setGravity(Gravity.CENTER);
            final int pad = dp50 / 3;
            view.setPadding( pad, pad, pad, 0 );
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
//                view.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );
            builder.setCustomTitle( view );
        }
        builder.show();
    }

}
