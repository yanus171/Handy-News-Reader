package ru.yanus171.feedexfork.utils;

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.MailTo;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;

import static ru.yanus171.feedexfork.utils.PrefUtils.THEME;

public class Theme {
	private static final String MENU_BACKGROUND_COLOR = "menuBackgroundColor";
	private static final String MENU_FONT_COLOR = "menuFontColor";
	static final String BACKGROUND_RES_ID = "BackgroundResID";
	static final String BUTTON_BACKGROUND_RES_ID = "ButtonBackgroundResID";
	static final String DARK = "Dark";
	static final String LIGHT = "Light";
	static final String BLACK = "Black";
	public static final String TEXT_COLOR_BACKGROUND = "textColor_background";
	public static final String TEXT_COLOR = "textColor";
	public static final String TEXT_COLOR_READ = "textColorRead";
	static final String BACKGROUND = "_background";
	public static final String TEXT_COLOR_READ_BACKGROUND = TEXT_COLOR_READ + BACKGROUND;
	public static final String LINK_COLOR = "linkColor";
	public static final String LINK_COLOR_BACKGROUND = LINK_COLOR + BACKGROUND;
	public static final String QUOTE_BACKGROUND_COLOR = "quote_background_color";
	public static final String QUOTE_LEFT_COLOR = "quote_left_color";
	//public static final String BUTTON_COLOR = "button_color";
	public static final String SUBTITLE_COLOR = "subtitle_color";
	public static final String SUBTITLE_BORDER_COLOR = "subtitle_border_color";
	//public static final String ENTRY_LIST_BACKGROUND = "entry_list_background";
	public static final String STYLE_THEME = "style_theme";
	private static HashMap<String, HashMap<String, String>> ThemeList = null;
	static final String THEME_CUSTOM = "Custom";
	public static String mTheme = DARK;

	// -------------------------------------------------------------------
	public static boolean IsCustom() {
		return PrefUtils.getBoolean("customColors", false);
	}
	public static void ReInit() {
		ThemeList = null;
	}
	//-------------------------------------------------------------------

	private static void CheckInit() {
		if (ThemeList == null) {
			ThemeList = new HashMap<>();

			{
				HashMap<String, String> theme = new HashMap<String, String>();
//				theme.put(TEXT_COLOR, "#555555");
				theme.put(TEXT_COLOR, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.light_theme_color_unread)).substring(2));
//				theme.put(TEXT_COLOR_BACKGROUND, "#f6f6f6");
				theme.put(TEXT_COLOR_BACKGROUND, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.light_theme_background)).substring(2));
				theme.put(TEXT_COLOR_READ, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.light_theme_color_read)).substring(2));
				theme.put(MENU_FONT_COLOR, "#000000");
				theme.put(MENU_BACKGROUND_COLOR, "#CCCCCC");
				theme.put(QUOTE_BACKGROUND_COLOR, "#e6e6e6");
				theme.put(QUOTE_LEFT_COLOR, "#FFA500");
				//theme.put(BUTTON_COLOR, "#52A7DF");
				theme.put(SUBTITLE_COLOR, "#666666");
				theme.put(SUBTITLE_BORDER_COLOR, "#dddddd");

                //theme.put(ENTRY_LIST_BACKGROUND, "#ffeeeeee");
				theme.put(STYLE_THEME, String.valueOf( R.style.Theme_Light) );
				theme.put("toolBarColor", "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.light_theme_color_primary)).substring(2));

//				theme.put("themeDialog", String.valueOf( R.style.styleLightDialog) );
				ThemeList.put(LIGHT, theme);
			}	

			{
				HashMap<String, String> theme = new HashMap<String, String>();
//				theme.put(TEXT_COLOR, "#FFFFFF");
				theme.put(TEXT_COLOR, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.dark_theme_color_unread)).substring(2));
//				theme.put(TEXT_COLOR_BACKGROUND, "#181b1f");
				theme.put(TEXT_COLOR_BACKGROUND, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.dark_theme_background)).substring(2));
				theme.put(TEXT_COLOR_READ, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.dark_theme_color_read)).substring(2));
				theme.put(MENU_FONT_COLOR, "#FFFFFF");
				theme.put(MENU_BACKGROUND_COLOR, "#222222");
				theme.put(QUOTE_BACKGROUND_COLOR, "#000000");
				theme.put(QUOTE_LEFT_COLOR, "#FFA500");
				//theme.put(BUTTON_COLOR, "#1A5A81");
				theme.put(SUBTITLE_COLOR, "#8c8c8c");
				theme.put(SUBTITLE_BORDER_COLOR, "#303030");
				//theme.put(ENTRY_LIST_BACKGROUND, "#ff202020");
				theme.put(STYLE_THEME, String.valueOf( R.style.Theme_Dark) );
				theme.put("toolBarColor", "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.dark_theme_color_primary)).substring(2));


//				theme.put("themeDialog", String.valueOf( R.style.styleDarkDialog) );
				ThemeList.put(DARK, theme);
			}

			{
				HashMap<String, String> theme = new HashMap<String, String>();
//				theme.put(TEXT_COLOR, "#FFFFFF");
				theme.put(TEXT_COLOR, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.black_theme_color_unread)).substring(2));
//				theme.put(TEXT_COLOR_BACKGROUND, "#181b1f");
				theme.put(TEXT_COLOR_BACKGROUND, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.black_theme_background)).substring(2));
				theme.put(TEXT_COLOR_READ, "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.black_theme_color_read)).substring(2));
				theme.put(MENU_FONT_COLOR, "#FFFFFF");
				theme.put(MENU_BACKGROUND_COLOR, "#222222");
				theme.put(QUOTE_BACKGROUND_COLOR, "#000000");
				theme.put(QUOTE_LEFT_COLOR, "#FFA500");
				//theme.put(BUTTON_COLOR, "#1A5A81");
				theme.put(SUBTITLE_COLOR, "#8c8c8c");
				theme.put(SUBTITLE_BORDER_COLOR, "#303030");
				//theme.put(ENTRY_LIST_BACKGROUND, "#ff202020");
				theme.put(STYLE_THEME, String.valueOf( R.style.Theme_Black) );
				theme.put("toolBarColor", "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), R.color.black_theme_color_primary)).substring(2));


//				theme.put("themeDialog", String.valueOf( R.style.styleDarkDialog) );
				ThemeList.put(BLACK, theme);
			}

			for ( HashMap<String, String> theme: ThemeList.values() ) {
				theme.put("notificationBackgroundColor", theme.get(MENU_BACKGROUND_COLOR) );
//				theme.put(TEXT_COLOR_READ, MainApplication.getContext().getString( R.string.default_read_color ));
				theme.put(LINK_COLOR, MainApplication.getContext().getString( R.string.default_link_color ) );
				theme.put(LINK_COLOR_BACKGROUND, theme.get( TEXT_COLOR_BACKGROUND ) );
				theme.put(TEXT_COLOR_READ_BACKGROUND, theme.get( TEXT_COLOR_BACKGROUND ) );
//				theme.put("toolBarColor", MainApplication.getContext().getString( R.string.default_toolbar_color));
			}
			mTheme = PrefUtils.getString( THEME, DARK);
		}

	}

	private static String GetTheme() { return PrefUtils.getString( THEME, DARK ); }
	public static String GetTextColor() {
		//if ( IsCustom() )
			return GetColor( TEXT_COLOR, R.string.default_text_color );
		//else
		//	return IsLight() ? getTextColorLightTheme() : getTextColorDarkTheme();
	}

	public static String GetBackgroundColor() {
		return Theme.GetColor( TEXT_COLOR_BACKGROUND, R.string.default_text_color_background );
	}


	public static boolean IsLight() {
		return GetTheme().equals( LIGHT );
	}

//	private static String getTextColorDarkTheme() {
//
//		int b = 200;
//		try {
//			b = Integer.parseInt( PrefUtils.getString(PrefUtils.TEXT_COLOR_BRIGHTNESS, "200") );
//		} catch (NumberFormatException e) {
//
//		}
//		return "#" + Integer.toHexString( Color.argb( 255, b, b, b ) ).substring( 2 );
//	}
//
//	private static String getTextColorLightTheme() {
//
//		int b = 200;
//		try {
//			b = Integer.parseInt( PrefUtils.getString(PrefUtils.TEXT_COLOR_BRIGHTNESS, "200") );
//		} catch (NumberFormatException e) {
//
//		}
//		b = 255 - b;
//		return "#" + Integer.toHexString( Color.argb( 255, b, b, b ) ).substring( 2 );
//	}

	//-------------------------------------------------------------------
	public static int GetMenuFontColor() {
		return GetColorInt (MENU_FONT_COLOR, R.color.light_theme_color_primary);
	}
	//-------------------------------------------------------------------
	public static int GetToolBarColorInt() {
		return Theme.GetColorInt("toolBarColor", R.string.default_toolbar_color );
	}
	//-------------------------------------------------------------------
	public static String GetToolBarColor() {
		return Theme.GetColor("toolBarColor", R.string.default_toolbar_color );
	}
	//-------------------------------------------------------------------
	public static int GetMenuBackgroundColor() {
		return GetColorInt(MENU_BACKGROUND_COLOR, android.R.color.background_dark);
	}
//	static int GetTheme() {
//		return Theme.GetResID("theme");
//	}
	static int GetThemeDialog() {
		CheckInit();
		//return Theme.GetResID("themeDialog");
		return Theme.GetResID(STYLE_THEME );
	}
	//-------------------------------------------------------------------
	public static int GetColorInt(String key, int defID) {
		int result = Color.BLACK;
		try {
			result = Color.parseColor(GetColor(key, defID));
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return result;
	}
	//-------------------------------------------------------------------
	public static String GetColor(String key, int defID) {
		CheckInit();		
		
		HashMap<String, String> map = ThemeList.get(mTheme);
		
		if ( map == null )
			map = ThemeList.get(DARK);
		
		String result = MainApplication.getContext().getString(defID);
		if ( PrefUtils.contains( key ) && ( !map.containsKey(key) || IsCustom() ) )
			result = PrefUtils.getString( key, result );
		else if ( map.containsKey( key ) )
			result = map.get( key);

		return result;
	}


	//-------------------------------------------------------------------
	public static String GetColor(String key, String defaultColor) {
		CheckInit();

		HashMap<String, String> map = ThemeList.get(mTheme);

		if ( map == null )
			map = ThemeList.get(DARK);

		String result = defaultColor;
		if ( IsCustom() || !map.containsKey(key) )
			result = PrefUtils.getString( key, result );
		else
			result = map.get(key);

		return result;
	}
	public static int GetResID( String key ) {
		CheckInit();

		HashMap<String, String> map = ThemeList.get(mTheme);
		if ( map.containsKey( key ) )
			return Integer.parseInt( map.get( key ) );
		else
			return android.R.drawable.screen_background_dark;
	}
	
	public static boolean GetBoolean( String key, boolean def ) {
		CheckInit();
		
		HashMap<String, String> map = ThemeList.get(mTheme);
		if ( map.containsKey( key ) )
			return Boolean.parseBoolean(map.get( key ) );
		else 
			return def;
	}

	public static AlertDialog.Builder CreateDialog(Context context) {
		return new AlertDialog.Builder(context, GetThemeDialog());
	}
}
