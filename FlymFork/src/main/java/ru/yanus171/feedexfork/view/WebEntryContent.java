package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.MainApplication.getContext;
import static ru.yanus171.feedexfork.adapter.EntriesCursorAdapter.CategoriesToOutput;
import static ru.yanus171.feedexfork.fragment.EntriesListFragment.IsFeedUri;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedID;
import static ru.yanus171.feedexfork.service.FetcherService.IS_ONE_WEB_PAGE;
import static ru.yanus171.feedexfork.service.FetcherService.IS_RSS;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.AddTagButtons;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_CATEGORY;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_DATE;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_CLASS_HIDDEN;
import static ru.yanus171.feedexfork.utils.ArticleTextExtractor.TAG_BUTTON_FULL_TEXT_ROOT_CLASS;
import static ru.yanus171.feedexfork.utils.PrefUtils.ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LINK_COLOR_BACKGROUND;
import static ru.yanus171.feedexfork.utils.Theme.LOADED_LINK_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.LOADED_LINK_COLOR_BACKGROUND;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_BACKGROUND_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.QUOTE_LEFT_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_BORDER_COLOR;
import static ru.yanus171.feedexfork.utils.Theme.SUBTITLE_COLOR;
import static ru.yanus171.feedexfork.view.FontSelectPreference.DefaultFontFamily;
import static ru.yanus171.feedexfork.view.FontSelectPreference.GetTypeFaceLocalUrl;
import static ru.yanus171.feedexfork.view.WebViewExtended.NO_MENU;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateFormat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Date;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.Timer;

public class WebEntryContent {
    String mTitle;
    private final boolean mIsFullTextShown;
    private final String mFeedID;
    private Uri mArticleListUri = Uri.EMPTY;
    private String mLink;
    private String mCategories;
    private String mAuthor;
    private JSONObject mOptions = null;
    private long mDate = 0;
    private String mContentText;
    boolean mIsEditingMode = false;

    private static final String BODY_START = "<body dir=\"%s\">";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START_WITH_LINK = "<h1><a class='no_draw_link' href=\"%s\">";
    private static final String TITLE_START = "<h1>";
    private static final String TITLE_END = "</h1>";
    private static final String TITLE_END_WITH_LINK = "</a></h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    private static final String CATEGORIES_START = "<p class='categories'>";
    private static final String CATEGORIES_END = "</p>";
    private static final String BUTTON_SECTION_START = "<div class='button-section'>";
    private static final String BOTTOM_PAGE = "<div class='bottom-page'/>";
    private static final String BUTTON_SECTION_END = "</div>";
    private static String BUTTON_START( String layout ) {
        return ( layout.equals( ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL ) ? "" : "<p class='button'>" ) + "<input type='button' value='";
    }
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static String BUTTON_END( String layout ) {
        return "'/>" + (layout.equals(ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL) ? "" : "</p>");
    }
    @SuppressLint("Range")
    WebEntryContent(Cursor cursor, Uri articleLiastUri, FeedFilters filters, boolean loadTitleOnly, boolean isFullTextShown) {
        mIsEditingMode = false;
        mFeedID = cursor.getString(cursor.getColumnIndex(FeedData.EntryColumns.FEED_ID));
        mAuthor = cursor.getString(cursor.getColumnIndex(FeedData.EntryColumns.AUTHOR));
        mCategories = cursor.getString(cursor.getColumnIndex(FeedData.EntryColumns.CATEGORIES));
        mDate = cursor.getLong(cursor.getColumnIndex(FeedData.EntryColumns.DATE));
        mTitle = cursor.getString(cursor.getColumnIndex(FeedData.EntryColumns.TITLE));
        mLink = cursor.getString(cursor.getColumnIndex(FeedData.EntryColumns.LINK));
        if (mLink == null)
            mLink = "";

        if (filters != null)
            mTitle = filters.removeText(mTitle, DB_APPLIED_TO_TITLE);
        mArticleListUri = articleLiastUri;
        try {
            mOptions = new JSONObject(cursor.getString(cursor.getColumnIndex(FeedData.FeedColumns.OPTIONS)));
        } catch (Exception ignored) {
        }
        mIsFullTextShown = isFullTextShown;
        setContent(cursor, filters, loadTitleOnly);
    }

    public WebEntryContent(String html, String baseUri, String link ) {
        mIsEditingMode = false;
        mIsFullTextShown = true;
        mFeedID = "-1";
        mLink = link;
        Document doc = Jsoup.parse(html, baseUri);
        AddTagButtons(doc, link);
        mContentText = doc.toString();
    }

    public String generateHtml() {
        Timer timer = new Timer("EntryView.generateHtmlContent");

        StringBuilder content = new StringBuilder(GetCSS(mTitle, mLink, mIsEditingMode))
                .append(String.format(BODY_START, isTextRTL(mTitle) ? "rtl" : "inherit"));

        if (getBoolean("entry_text_title_link", true))
            content.append(String.format(TITLE_START_WITH_LINK, mLink + NO_MENU)).append(mTitle).append(TITLE_END_WITH_LINK);
        else
            content.append(TITLE_START).append(mTitle).append(TITLE_END);

        content.append(SUBTITLE_START);
        Date date = new Date(mDate);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));
        if (mAuthor != null && !mAuthor.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(mAuthor);
        }
        content.append(dateStringBuilder);
        content.append(SUBTITLE_END);
        if ( mArticleListUri != Uri.EMPTY && !IsFeedUri(mArticleListUri) )
            content.append(SUBTITLE_START).append( getFeedTitle( mFeedID) ).append(SUBTITLE_END);

        if (mCategories != null && !mCategories.isEmpty())
            content.append( CATEGORIES_START ).append( CategoriesToOutput( mCategories) ).append( CATEGORIES_END );

        content.append(mContentText);


        final String layout = PrefUtils.getString( "setting_article_text_buttons_layout", ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL );
        if ( !layout.equals( "Hidden" ) ) {
            content.append(BUTTON_SECTION_START);
            if (canShowSwitchToFullText())
                addButtonHtml(content, R.string.get_full_text, "onClickFullText");
            if (canShowSwitchToOriginal())
                addButtonHtml(content, R.string.original_text, "onClickOriginalText");
            if (canShowReloadFullText())
                addButtonHtml(content, R.string.btn_reload_full_text, "onReloadFullText");
            addButtonHtml(content, R.string.menu_go_back, "onClose");

            content.append(BUTTON_SECTION_END).append(BOTTOM_PAGE).append(BODY_END);
        }

        timer.End();
        return content.toString();
    }

    private static String getFeedTitle( String feedID ) {
        final ContentResolver cr = MainApplication.getContext().getContentResolver();
        try ( Cursor cursor = cr.query(FeedData.FeedColumns.CONTENT_URI(feedID), new String[]{FeedData.FeedColumns.NAME}, null, null, null ) ) {
            if (cursor.moveToFirst())
                return cursor.getString(0);
        }
        return "";
    }

    private static void addButtonHtml(StringBuilder content, int captionID, String methodName) {
        final String layout = PrefUtils.getString( "setting_article_text_buttons_layout", ARTICLE_TEXT_BUTTON_LAYOUT_HORIZONTAL );
        content.append(BUTTON_START(layout));
        content.append(MainApplication.getContext().getString(captionID))
                .append(BUTTON_MIDDLE).append(String.format("injectedJSObject.%s();", methodName) );
        content.append(BUTTON_END(layout));
    }


    private static String GetCSS(final String text, final String url, boolean isEditingMode) {
        String mainFontLocalUrl = GetTypeFaceLocalUrl(PrefUtils.getString("fontFamily", DefaultFontFamily), isEditingMode);
        final CustomClassFontInfo customFontInfo = GetCustomClassAndFontName("font_rules", url);
        if ( !customFontInfo.mKeyword.isEmpty() && customFontInfo.mClassName.isEmpty() )
            mainFontLocalUrl = GetTypeFaceLocalUrl( customFontInfo.mFontName, isEditingMode );
        String mainFontSize = PrefUtils.getFontSizeText(0 );
        String textAlign = getAlign(text);
        return "<head><style type='text/css'> "
                + "@font-face { font-family:\"MainFont\"; src: url(\"" + mainFontLocalUrl + "\");" + "} \n"
                + "@font-face { font-family:\"CustomFont\"; src: url(\"" + GetTypeFaceLocalUrl(customFontInfo.mFontName, isEditingMode) + "\");}\n"
                + "body { font-family: \"MainFont\"; font-size: " + mainFontSize + "; text-align:" + textAlign + "; font-weight: " + getFontBold() + "; "
                + "font-size: " + mainFontSize + "; color: " + Theme.GetTextColor() + "; background-color:" + Theme.GetBackgroundColor() + "; "
                + "max-width: 100%; margin: " + getMargins() + "; " +  PrefUtils.getString( "main_font_css_text", "" ) + "}\n "
                + "* {word-break: break-word}\n"//+ "* {max-width: 100%; word-break: break-word}\n"
                + "title, h1, h2 {font-weight: normal; text-align:center; line-height: 120%}\n "
                + "title, h1 {font-size: " + PrefUtils.getFontSizeText(4 ) + "; margin-top: 1.0cm; margin-bottom: 0.1em}\n "
                + "h2 {font-size: " + PrefUtils.getFontSizeText(2 ) + "}\n "
                + "}body{color: #000; text-align: justify; background-color: #fff;}\n"
                + "a.loaded_link {color: " + Theme.GetColor(LOADED_LINK_COLOR, R.string.default_loaded_link_color) + "; background: " + Theme.GetColor(LOADED_LINK_COLOR_BACKGROUND, R.string.default_text_color_background) + "}\n"
                + "a.no_draw_link {color: " + Theme.GetTextColor() + "; background: " + Theme.GetBackgroundColor() + "; text-decoration: none" + "}\n"
                + "a {color: " + Theme.GetColor(LINK_COLOR, R.string.default_link_color) + "; background: " + Theme.GetColor(LINK_COLOR_BACKGROUND, R.string.default_text_color_background) +
                (getBoolean("underline_links", true) ? "" : "; text-decoration: none") + "}\n"
                + "h1 {color: inherit; text-decoration: none}\n"
                + "img {display: inline;max-width: 100%;height: auto; " + (PrefUtils.isImageWhiteBackground() ? "background: white" : "") + "}\n"
                //+ ".inverted .math {color: white; background-color: black; } "
                + "iframe {allowfullscreen; position:relative;top:0;left:0;width:100%;height:100%;}\n"
                + "pre {white-space: pre-wrap;}\n "
                + "blockquote {border-left: thick solid " + Theme.GetColor(QUOTE_LEFT_COLOR, android.R.color.black) + "; background-color:" + Theme.GetColor(QUOTE_BACKGROUND_COLOR, android.R.color.black) + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em}\n "
                + "td {font-weight: " + getFontBold() + "; text-align:" + textAlign + "}\n "
                + "hr {width: 100%; color: #777777; align: center; size: 1}\n "
                + "p.button {text-align: center}\n "
                + "math {color: " + Theme.GetTextColor() + "; background-color:" + Theme.GetBackgroundColor() + "}\n "
                + "p {font-family: \"MainFont\"; font-size: " + mainFontSize + "; margin: 0.8em 0 0.8em 0; text-align:" + textAlign + "}\n "
                + getCustomFontClassStyle("p", customFontInfo)
                + getCustomFontClassStyle("span", customFontInfo)
                + getCustomFontClassStyle("div", customFontInfo)
                + "p.subtitle { text-align: center; color: " + Theme.GetColor(SUBTITLE_COLOR, android.R.color.black) + "; border-top:1px " + Theme.GetColor(SUBTITLE_BORDER_COLOR, android.R.color.black) + "; border-bottom:1px " + Theme.GetColor(SUBTITLE_BORDER_COLOR, android.R.color.black) + "; padding-top:2px; padding-bottom:2px; font-weight:800 }\n "
                + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em}\n "
                + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0}\n "
                + "div.bottom-page {display: block; min-height: 80vh}\n "
                + "div.button-section {padding: 0.8cm 0; margin: 0; text-align: center}\n "
                + "div {text-align:" + textAlign + "}\n "
                + "div.toc {text-align: center}\n "
                + "p.toc {text-align: center}\n "
                //+ "* { -webkit-tap-highlight-color: rgba(" + Theme.GetToolBarColorRGBA() + "); } "
                + ".categories {font-style: italic; color: " + Theme.GetColor(SUBTITLE_COLOR, android.R.color.black) + "}\n "
                + ".button-section p {font-family: \"MainFont\"; font-size: " + mainFontSize + "; margin: 0.1cm 0 0.2cm 0}\n "
                + ".button-section p.marginfix {margin: 0.2cm 0 0.2cm 0}\n"
                + ".button-section input, .button-section a {font-family: \"MainFont\"; font-size: " + mainFontSize + "; color: #FFFFFF; background-color: " + Theme.GetToolBarColor() + "; text-decoration: none; border: none; border-radius:0.2cm; margin: 0.2cm}\n "
                + "." + TAG_BUTTON_CLASS + ", ." + TAG_BUTTON_CLASS_CATEGORY + ", ." + TAG_BUTTON_CLASS_DATE + ", ." + TAG_BUTTON_CLASS_CATEGORY + ", ." + TAG_BUTTON_FULL_TEXT_ROOT_CLASS + ", ." + TAG_BUTTON_CLASS_HIDDEN
                + " { font-size: " + mainFontSize + "; color: #FFFFFF; background-color: " + Theme.GetToolBarColor() + "; text-decoration: none; border: none; border-radius:0.2cm;  margin-right: 0.2cm; padding-top: 0.0cm; padding-bottom: 0.0cm; padding-left: 0.2cm; padding-right: 0.2cm}\n "
                + "." + TAG_BUTTON_CLASS + " i { background-color: " + Theme.GetToolBarColor() + "}\n "
                + "." + TAG_BUTTON_CLASS_CATEGORY + " i {background-color: #00AAAA}\n "
                + "." + TAG_BUTTON_CLASS_DATE + " i {background-color: #0000AA}\n "
                + "." + TAG_BUTTON_FULL_TEXT_ROOT_CLASS + " i {background-color: #00AA00}\n "
                + "." + TAG_BUTTON_CLASS_HIDDEN + " i {background-color: #888888}\n "
                + PrefUtils.getString( "custom_css_text", "" )
                + "</style><meta name='viewport' content='width=device-width'/></head>";
    }

    @NotNull
    private static String getCustomFontClassStyle(String tag, CustomClassFontInfo info) {
        if ( info.mKeyword.isEmpty() )
            return "";
        else
            return tag + ( info.mClassName.isEmpty() ? "" : "." + info.mClassName)
                    +   "{font-family: \"CustomFont\"; " + info.mStyleText + "} ";
    }

    public int getContentHash() {
        return mContentText.hashCode();
    }

    static class CustomClassFontInfo {
        String mClassName = "";
        String mKeyword = "";
        String mFontName = "";
        String mStyleText = "";
        CustomClassFontInfo( String line ) {
            String[] list1 = line.split(":");
            if ( list1.length >= 1 ) {
                mKeyword = list1[0];
                if (list1.length >= 2) {
                    String[] list2 = list1[1].split("=");
                    if (list2.length >= 2) {
                        mClassName = list2[0].toLowerCase();
                        mFontName = list2[1];
                    }
                    if (list2.length >= 3)
                        mStyleText = list2[2];
                }
            }
            for ( int i = 0; i < list1.length; i++ )
                if (i >= 2)
                    mStyleText += ":" + list1[i];
        }

    }
    private static CustomClassFontInfo GetCustomClassAndFontName(final String key, final String url ) {
        final String pref = PrefUtils.getString( key, "" );
        for( String line: pref.split( "\\n" ) ) {
            if ((line == null) || line.isEmpty())
                continue;
            try {
                CustomClassFontInfo info = new CustomClassFontInfo( line );
                if (url.contains(info.mKeyword)) {
                    return info;
                }
            } catch (Exception e) {
                Dog.e(e.getMessage());
            }
        }
        return new CustomClassFontInfo("");
    }


    private static String getFontBold() {
        if (getBoolean(PrefUtils.ENTRY_FONT_BOLD, false))
            return "bold;";
        else
            return "normal;";
    }

    private static String getMargins() {
        if (getBoolean(PrefUtils.ENTRY_MAGRINS, true))
            return "4%";
        else
            return "0.1cm";
    }

    public static String getAlign(String text) {
        if (isTextRTL(text))
            return "right";
        else if (getBoolean(PrefUtils.ENTRY_TEXT_ALIGN_JUSTIFY, false))
            return "justify";
        else
            return "left";
    }

    private static boolean isWordRTL(String s) {
        if (s.isEmpty()) {
            return false;
        }
        char c = s.charAt(0);
        return c >= 0x590 && c <= 0x6ff;
    }

    public static boolean isTextRTL(String text) {
        if ( text == null )
            return false;
        String[] list = TextUtils.split(text, " ");
        for (String item : list)
            if (isWordRTL(item))
                return true;
        return false;
    }

    private boolean hasOriginal() {
        try {
            return !mFeedID.equals(GetExtrenalLinkFeedID()) && (mOptions == null || isOption(IS_RSS) || isOption(IS_ONE_WEB_PAGE));
        } catch (Exception ignored) {
            return false;
        }
    }
    private boolean isOption( String name ) throws JSONException {
        return mOptions.has(name) && mOptions.getBoolean(name);
    }

    public boolean canShowSwitchToFullText() {
        return !mIsFullTextShown && hasOriginal();
    }

    public boolean canShowSwitchToOriginal() {
        return mIsFullTextShown && hasOriginal();
    }

    public boolean canShowReloadFullText() {
        return mIsFullTextShown;
    }

    @SuppressLint("Range")
    private void setContent(Cursor cursor, FeedFilters filters, boolean loadTitleOnly) {
        if (loadTitleOnly)
            mContentText = getContext().getString(R.string.loading);
        else {
            try {
                if (hasOriginal() && (!mIsFullTextShown || !FileUtils.INSTANCE.isMobilized(mLink, cursor))) {
                    mContentText = cursor.getString(cursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT));
                    if (filters != null)
                        mContentText = filters.removeText(mContentText, DB_APPLIED_TO_CONTENT);
                } else {
                    mContentText = FileUtils.INSTANCE.loadMobilizedHTML(mLink, cursor);
                }
                if (mContentText == null)
                    mContentText = "";
            } catch (IllegalStateException e) {
                e.printStackTrace();
                mContentText = "Context too large";
            }
        }
    }

}
