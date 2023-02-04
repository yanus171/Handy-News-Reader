package ru.yanus171.feedexfork.utils;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

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
	static final String STYLE_THEME = "style_theme";
	static final String PREF_STYLE_THEME = "pref_style_theme";
	public static final String NEW_ARTICLE_INDICATOR_RES_ID = "NEW_ARTICLE_INDICATOR";
	public static final String STARRED_ARTICLE_INDICATOR_RES_ID = "STARRED_ARTICLE_INDICATOR";
	private static final String TOOL_BAR_COLOR = "toolBarColor";
	private static HashMap<String, HashMap<String, String>> ThemeList = null;
	static final String THEME_CUSTOM = "Custom";
	private static String mTheme = DARK;

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

			{   // Light theme
				HashMap<String, String> theme = new HashMap<>();
				theme.put(TEXT_COLOR, GetResourceColor(R.color.light_theme_color_unread));
				theme.put(TEXT_COLOR_BACKGROUND, GetResourceColor(R.color.light_theme_background));
				theme.put(TEXT_COLOR_READ, GetResourceColor(R.color.light_theme_color_read));
				theme.put(MENU_FONT_COLOR, "#000000");
				theme.put(MENU_BACKGROUND_COLOR, "#CCCCCC");
				theme.put(QUOTE_BACKGROUND_COLOR, "#e6e6e6");
				theme.put(QUOTE_LEFT_COLOR, "#FFA500");
				theme.put(SUBTITLE_COLOR, "#666666");
				theme.put(SUBTITLE_BORDER_COLOR, "#dddddd");
				theme.put(STYLE_THEME, String.valueOf( R.style.Theme_Light) );
				theme.put(PREF_STYLE_THEME, String.valueOf( R.style.Theme_Light_Pref) );
				theme.put(TOOL_BAR_COLOR, GetResourceColor( R.color.light_theme_color_primary));
				theme.put(NEW_ARTICLE_INDICATOR_RES_ID, String.valueOf(R.drawable.ic_indicator_new_article_light) );
				theme.put(STARRED_ARTICLE_INDICATOR_RES_ID, String.valueOf(R.drawable.ic_indicator_star_light) );
				ThemeList.put(LIGHT, theme);
			}	

			{   // Dark theme
				HashMap<String, String> theme = new HashMap<>();
				theme.put(TEXT_COLOR, GetResourceColor( R.color.dark_theme_color_unread));
				theme.put(TEXT_COLOR_BACKGROUND, GetResourceColor(R.color.dark_theme_background));
				theme.put(TEXT_COLOR_READ, GetResourceColor( R.color.dark_theme_color_read));
				theme.put(MENU_FONT_COLOR, "#FFFFFF");
				theme.put(MENU_BACKGROUND_COLOR, "#222222");
				theme.put(QUOTE_BACKGROUND_COLOR, "#000000");
				theme.put(QUOTE_LEFT_COLOR, "#FFA500");
				theme.put(SUBTITLE_COLOR, "#8c8c8c");
				theme.put(SUBTITLE_BORDER_COLOR, "#303030");
				theme.put(STYLE_THEME, String.valueOf( R.style.Theme_Dark) );
				theme.put(PREF_STYLE_THEME, String.valueOf( R.style.Theme_Dark_Pref) );
				theme.put(TOOL_BAR_COLOR, GetResourceColor(R.color.dark_theme_color_primary));
				theme.put(NEW_ARTICLE_INDICATOR_RES_ID, String.valueOf(R.drawable.ic_indicator_new_article_dark) );
				theme.put(STARRED_ARTICLE_INDICATOR_RES_ID, String.valueOf(R.drawable.ic_indicator_star_dark) );
				ThemeList.put(DARK, theme);
			}

			{   // Black theme
				HashMap<String, String> theme = (HashMap<String, String>) ThemeList.get(DARK).clone(); // clone from dark theme, so we need not repeat equal settings
				theme.put(TEXT_COLOR, GetResourceColor(R.color.black_theme_color_unread));
				theme.put(TEXT_COLOR_BACKGROUND, GetResourceColor( R.color.black_theme_background));
				theme.put(TEXT_COLOR_READ, GetResourceColor(R.color.black_theme_color_read));
				theme.put(STYLE_THEME, String.valueOf( R.style.Theme_Black) );
				theme.put(TOOL_BAR_COLOR, GetResourceColor(R.color.black_theme_color_primary));
				theme.put(PREF_STYLE_THEME, String.valueOf( R.style.Theme_Black_Pref) );
				ThemeList.put(BLACK, theme);
			}
			// for all themes
			for ( HashMap<String, String> theme: ThemeList.values() ) {
				theme.put("notificationBackgroundColor", theme.get(MENU_BACKGROUND_COLOR) );
				theme.put(LINK_COLOR, MainApplication.getContext().getString( R.string.default_link_color ) );
				theme.put(LINK_COLOR_BACKGROUND, theme.get( TEXT_COLOR_BACKGROUND ) );
				theme.put(TEXT_COLOR_READ_BACKGROUND, theme.get( TEXT_COLOR_BACKGROUND ) );
			}
			mTheme = PrefUtils.getString( THEME, DARK);
		}

	}

	private static String GetResourceColor( int resID ) {
		return "#"+Integer.toHexString(ContextCompat.getColor(MainApplication.getContext(), resID )).substring(2);
	}
	private static String GetTheme() { return PrefUtils.getString( THEME, DARK ); }
	public static String GetTextColor() {
		//if ( IsCustom() )
			return GetColor( TEXT_COLOR, R.string.default_text_color );
		//else
		//	return IsLight() ? getTextColorLightTheme() : getTextColorDarkTheme();
	}
	public static int GetTextColorInt() {
		return GetColorInt(TEXT_COLOR, R.string.default_text_color);
	}

	public static String GetBackgroundColor() {
		return Theme.GetColor( TEXT_COLOR_BACKGROUND, R.string.default_text_color_background );
	}
	public static int GetBackgroundColorInt() {
		return Theme.GetColorInt( TEXT_COLOR_BACKGROUND, R.string.default_text_color_background );
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
		return Theme.GetColorInt(TOOL_BAR_COLOR, R.string.default_toolbar_color );
	}
	//-------------------------------------------------------------------
	public static String GetToolBarColor() {
		return Theme.GetColor(TOOL_BAR_COLOR, R.string.default_toolbar_color );
	}
	//-------------------------------------------------------------------
	@SuppressLint("DefaultLocale")
	public static String GetToolBarColorRGBA() {
		final int color = GetToolBarColorInt();
		return String.format( "%d, %d, %d, %d", Color.red( color ), Color.green( color ), Color.blue( color ), Color.alpha( color ) );
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
	public static String GetTextColorRead() {
		return GetColor( TEXT_COLOR_READ, R.string.default_read_color );
	}
	public static int GetTextColorReadInt() {
		return GetColorInt( TEXT_COLOR_READ, R.string.default_read_color );
	}
}
