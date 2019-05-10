package ru.yanus171.feedexfork.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import ru.yanus171.feedexfork.utils.ColorTB;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.ColorDialog;

//***********************************************************************************
public class ColorPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
	private final String NS = "ru.yanus171.feedexfork.view.ColorPreference";
	private final int cColorViewHeight = 50;

	private int DefaultTextColor;
	private TextView mviewColor = null;
	private boolean IsTransparency = false;
	private int DefaultBackgroundColor;
	private boolean IsText;
	private boolean IsBackGround;

	// -------------------------------------
	// ------------------------------------
	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		IsText = false;
		DefaultTextColor = Theme.GetMenuFontColor();
		String s = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue");
		if (s != null) {
			DefaultTextColor = GetDefaultColorFromPrefString(s);
			IsText = true;
		} else {
			s = attrs.getAttributeValue(NS, "text");
			if (s != null) {
				DefaultTextColor = GetDefaultColorFromPrefString(s);
				IsText = true;
			}
		}

		IsBackGround = false;
		DefaultBackgroundColor = Color.TRANSPARENT;
		s = attrs.getAttributeValue(NS, "backgroundColor");
		if (s != null) {
			DefaultBackgroundColor = GetDefaultColorFromPrefString(s);
			IsBackGround = true;
		}

		IsTransparency = attrs.getAttributeBooleanValue(NS, "istransparency", false);
	}

	// -------------------------------------------------------------------------
	private int GetDefaultColorFromPrefString(String str) {
		int result = Color.TRANSPARENT;
		if (str.substring(0, 1).equals("@")) {
			str = str.substring(1, str.length());
			int resID = getContext().getResources().getIdentifier(str, "string", getContext().getPackageName());
			str = getContext().getString(resID);
		}
		result = Color.parseColor(str);
		return result;
	}

    public static String ToHex(int color, boolean isTransparent) {
        if ( isTransparent )
            return String.format( "#%02X%02X%02X%02X",
                    Color.alpha( color ),
                    Color.red( color ),
                    Color.green( color ),
                    Color.blue( color ));
        else
            return String.format( "#%02X%02X%02X",
                    Color.red( color ),
                    Color.green( color ),
                    Color.blue( color ));
    }

    // -------------------------------------------------------------------------
	@Override
	protected void onClick() {
		final ColorDialog colorDialog = new ColorDialog(getContext(),
				ColorTB.Create(GetPersistedTextColor(), GetPersistedBackgroundColor()), IsTransparency, IsText, IsBackGround,
				(String) getTitle());
		AlertDialog.Builder builder = colorDialog.CreateBuilder();

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@SuppressLint("CommitPrefEdits")
			public void onClick(DialogInterface dialog, int which) {
				// persistInt(colorDialog.mColor.Text);
				PrefUtils.putString( getKey(), ToHex( colorDialog.mColor.Text, IsTransparency ) );
                PrefUtils.putString( GetBackgroundKey(getKey()), ToHex( colorDialog.mColor.Background, IsTransparency ) );
				UpdateViewColor();
				callChangeListener(null);
			}
		});

		builder.show();
	}

	// ------------------------------------------------------------------------------------
	private static int GetPrefString(String key, String defaultValue) {
		return Color.parseColor( PrefUtils.getString( key, defaultValue ) );
	}

	// -------------------------------------------------------------------------
	private int GetPersistedTextColor() {
		return GetPrefString(getKey(), ToHex( DefaultTextColor, IsTransparency ));
	}

	// -------------------------------------------------------------------------
	private int GetPersistedBackgroundColor() {
		return GetPrefString(GetBackgroundKey(getKey()), ToHex( DefaultBackgroundColor, IsTransparency ));
	}

	// -------------------------------------------------------------------------
	public static String GetBackgroundKey(String key) {
		return key + "_background";
	}

	// -------------------------------------------------------------------------
	@Override
	protected View onCreateView(ViewGroup parent) {
		ViewGroup result = (ViewGroup) super.onCreateView(parent);
        mviewColor = ColorDialog.CreateDialogColor( result, IsText, IsBackGround );
//		mviewColor = new TextView(getContext());
//		mviewColor.setMinimumHeight(cColorViewHeight);
//		mviewColor.setMinimumWidth(cColorViewHeight);
//		mviewColor.setGravity(Gravity.CENTER);
//		mviewColor.setTypeface(Typeface.DEFAULT_BOLD);
//		mviewColor.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
//		result.addView(mviewColor);
        UpdateViewColor();

		return result;
	}

	private void UpdateViewColor() {
		if (mviewColor != null) {
		    if ( !isEnabled() )
                mviewColor.setVisibility( View.GONE );
		    else {
                mviewColor.setBackgroundColor(GetPersistedTextColor());
                if (IsBackGround && IsText) {
                    mviewColor.setText(ColorDialog.cTextLetter);
                    mviewColor.setTextColor(GetPersistedTextColor());
                    mviewColor.setBackgroundColor(GetPersistedBackgroundColor());
                }
            }
		}
	}

	// ----------------------------------------------------------------------------
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub

	}

	// ----------------------------------------------------------------------------
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub

	}

	// ----------------------------------------------------------------------------
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub

	}
}
