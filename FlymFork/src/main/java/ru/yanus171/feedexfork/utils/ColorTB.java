package ru.yanus171.feedexfork.utils;

import java.io.Serializable;
import java.util.HashMap;

import android.graphics.Color;

import ru.yanus171.feedexfork.view.ColorPreference;

//************************************************************************
@SuppressWarnings("serial")
public class ColorTB implements Serializable {

	public int Text = android.graphics.Color.WHITE;
	public int Background = android.graphics.Color.TRANSPARENT;
	
	// -------------------------------------------------------------------
	public ColorTB() {
	}

	// -------------------------------------------------------------------
	static public ColorTB Create(int text, int background) {
		return new ColorTB( text, background );
	}
	/*// -------------------------------------------------------------------
	static ColorTB CreateFromPref(String key, int defTextID, int defBackgroundID) {
		return Create( Global.GetPrefColorDefID(key, defTextID), 
					   Global.GetPrefColorDefID(ColorPreference.GetBackgroundKey(key), defBackgroundID) );
	}*/
	// -------------------------------------------------------------------
	private ColorTB(int text, int background) {
		Text = text;
		Background = background;
	}
	// -------------------------------------------------------------------
	public ColorTB(String key, int defTextID, int defBackgroundID) {
		
		if (!Theme.IsCustom() ) {
			Text = Theme.GetColor( key, defTextID );
			Background = Theme.GetColor( ColorPreference.GetBackgroundKey(key), defBackgroundID );
		} else {
			Text = PrefUtils.GetPrefColorDefID(key, defTextID);
			Background = PrefUtils.GetPrefColorDefID(ColorPreference.GetBackgroundKey(key), defBackgroundID);
			
		}
	}

		// --------------------------------------------------------------------
	void SetTransparency(int value) {
		Text = Color.argb(value, Color.red(Text), Color.green(Text), Color.blue(Text));
		if (Background != Color.TRANSPARENT) {
			Background = Color.argb(value, Color.red(Background), Color.green(Background), Color.blue(Background));
		}
	}

	// --------------------------------------------------------------------
	@Override
	public Object clone() {
		return new ColorTB(Text, Background);
	}

	// -----------------------------------------------------------------------------
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		boolean result = false;
		if (object instanceof ColorTB) {
			ColorTB color = (ColorTB) object;
			result = (color.Text == Text) && (color.Background == Background);
		}
		return result;
	}

	// --------------------------------------------------------------------
	public boolean IsEmpty() {
		ColorTB empty = new ColorTB();
		return (empty.Text == Text) && (empty.Background == Background);
	}

	// --------------------------------------------------------------------
	boolean HasBackground() {
		return (Color.alpha(Background) != 0);
	}

	// --------------------------------------------------------------------
	static int FromPrefs(String key, int defaultID) {
		return PrefUtils.GetPrefColorDefID(key, defaultID);
	}
	// --------------------------------------------------------------------
	static String ToHex(int color) {
		return String.format( "#%02X%02X%02X", Color.red( color ), Color.green( color ), Color.blue( color ) );
	}
}
