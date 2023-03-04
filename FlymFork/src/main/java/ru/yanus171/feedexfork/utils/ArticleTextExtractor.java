package ru.yanus171.feedexfork.utils;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.parser.FeedFilters;
import ru.yanus171.feedexfork.service.FetcherService;

import static ru.yanus171.feedexfork.fragment.EntryFragment.STATE_RELOAD_WITH_DEBUG;
import static ru.yanus171.feedexfork.parser.RssAtomParser.parseDate;
import static ru.yanus171.feedexfork.utils.DebugApp.CreateFileUri;
import static ru.yanus171.feedexfork.utils.NetworkUtils.OKHTTP;
import static ru.yanus171.feedexfork.utils.PrefUtils.CONTENT_TEXT_ROOT_EXTRACT_RULES;
import static ru.yanus171.feedexfork.view.EntryView.TAG;

/**
 * This class is thread safe.
 *
 * @author Alex P (ifesdjeen from jreadability)
 * @author Peter Karich
 */
public class ArticleTextExtractor {

    // Interesting nodes
    private static final Pattern NODES = Pattern.compile("p|div|td|h1|h2|article|section");

    // Unlikely candidates
    private static final Pattern UNLIKELY = Pattern.compile("com(bx|ment|munity)|dis(qus|cuss)|e(xtra|[-]?mail)|foot|"
            + "header|menu|re(mark|ply)|rss|sh(are|outbox)|sponsor"
            + "a(d|ll|gegate|rchive|ttachment)|(pag(er|ination))|popup|print|"
            + "login|si(debar|gn|ngle)");

    // Most likely positive candidates
    private static final Pattern POSITIVE = Pattern.compile("(^(body|content|h?entry|main|page|post|text|blog|story|haupt))"
            + "|arti(cle|kel)|instapaper_body");

    // Most likely negative candidates
    //private static final Pattern NEGATIVE = Pattern.compile("nav($|igation)|user|com(ment|bx)|(^com-)|contact|"
    private static final Pattern NEGATIVE = Pattern.compile("nav($|igation)|user|combx|(^com-)|contact|"
            + "foot|masthead|(me(dia|ta))|outbrain|promo|related|scroll|(sho(utbox|pping))|"
            + "sidebar|sponsor|tags|tool|widget|player|disclaimer|toc|infobox|vcard");

    private static final Pattern NEGATIVE_STYLE =
            Pattern.compile("hidden|display: ?none|font-size: ?small");
    public static final String TAG_BUTTON_CLASS = "tag_button";
    public static final String TAG_BUTTON_CLASS_HIDDEN = "tag_button_hidden";
    public static final String TAG_BUTTON_CLASS_CATEGORY = "tag_button_category";
    public static final String TAG_BUTTON_CLASS_DATE = "tag_button_date";
    public static final String TAG_BUTTON_FULL_TEXT_ROOT_CLASS = "tag_button_full_text";
    static final String CLASS_ATTRIBUTE = "class";
    public static final String HANDY_NEWS_READER_ROOT_CLASS = "Handy_News_Reader_root";
    public static final String HANDY_NEWS_READER_CATEGORY_CLASS = "Handy_News_Reader_tag";
    public static final String HANDY_NEWS_READER_DATE_CLASS = "Handy_News_Reader_date";
    public static final String HANDY_NEWS_READER_MAIN_IMAGE_CLASS = "Handy_News_Reader_main_image";
    public static final String HANDY_NEWS_READER_COMMENTS_CLASS = "Handy_News_Reader_comments";
    public static final String P_HR = "</p><hr>";
    public static final String EMPTY_TAG = "EMPTY_TAG";
    public static final String CONTENT_STEP_TO_FILE_SUBDIR = "content";
    private static int mContentStepFileIndex = 0;

    public enum MobilizeType {Yes, No, Tags}
    //public static final String BEST_ELEMENT_ATTR = "BEST_ELEMENT";

    public static String mLastLoadedAllDoc = "";

    /**
     * @param input            extracts article text from given html string. wasn't tested
     *                         with improper HTML, although jSoup should be able to handle minor stuff.
     * @param contentIndicator a text which should be included into the extracted content, or null
     * @return extracted article, all HTML tags stripped
     */
    public static String extractContent(InputStream input,
                                        final String url,
                                        String contentIndicator,
                                        FeedFilters filters,
                                        MobilizeType mobilize,
                                        ArrayList<String> tagList,
                                        boolean isFindBestElement,
                                        boolean isWithTables,
                                        boolean withScripts) throws Exception {
        return extractContent(Jsoup.parse(input, null, ""),
                              url,
                              contentIndicator,
                              filters,
                              mobilize,
                              tagList,
                              isFindBestElement,
                              isWithTables,
                              withScripts);
    }

    public static void ClearContentStepToFile() {
        if (!PrefUtils.getBoolean( STATE_RELOAD_WITH_DEBUG, false ))
            return;
        mContentStepFileIndex = 0;
        File dir = new File(MainApplication.getContext().getCacheDir() + "/" + CONTENT_STEP_TO_FILE_SUBDIR);
        dir.mkdirs();
        for( File file: dir.listFiles() )
            file.delete();
    }
    public static void SaveContentStepToFile( Element content, String stepName ) {
        if (!PrefUtils.getBoolean( STATE_RELOAD_WITH_DEBUG, false ))
            return;
        SaveContentStepToFile( content.toString(), stepName );
    }
    public static void SaveContentStepToFile( StringBuilder content, String stepName ) {
        if (!PrefUtils.getBoolean( STATE_RELOAD_WITH_DEBUG, false ))
            return;
        SaveContentStepToFile( content.toString(), stepName );
    }
    public static void SaveContentStepToFile( String content, String stepName ) {
        if (!PrefUtils.getBoolean( STATE_RELOAD_WITH_DEBUG, false ))
            return;
        mContentStepFileIndex++;
        CreateFileUri(MainApplication.getContext().getCacheDir().getAbsolutePath() + "/" + CONTENT_STEP_TO_FILE_SUBDIR, mContentStepFileIndex + "_" + stepName + ".html", content );
    }
    public static String extractContent(Document doc,
                                        final String url,
                                        String contentIndicator,
                                        FeedFilters filters,
                                        MobilizeType mobilize,
                                        ArrayList<String> categoryList,
                                        boolean isFindBestElement,
                                        boolean isWithTables,
                                        boolean withScripts ) {
        if (doc == null)
            throw new NullPointerException("missing document");

        FetcherService.Status().AddBytes(doc.html().length());

        // now remove the clutter
        prepareDocument(doc, mobilize, withScripts);
        {
            Elements elList = doc.getElementsByAttribute( "id" );
            for (Element item : elList)
                if ( !item.hasAttr( "class" ) )
                    item.attr( "class", item.attr( "id") );

        }

        {
            Elements elList = doc.getElementsByClass( "turbo-image" );
            for (Element item : elList)
                if ( item.hasAttr( "data-src" ) ) {
                    item.attr("src", item.attr("data-src"));
                    item.tagName( "img" );
                }
        }

        {
            Elements elList = doc.getElementsByClass( "lazy" );
            for (Element item : elList)
                if (item.tagName().equals("a") && item.hasAttr("data-src" ) ) {
                    item.removeAttr("data-src");
                }
        }

        {
            categoryList.clear();
            Element tagsElements = getCategoriesElementFromPref(doc, url);
            if (tagsElements != null) {
                for (Element item : tagsElements.getAllElements())
                    if (!item.ownText().isEmpty())
                        categoryList.add(item.ownText());
            }
        }
        Dog.v( "tagList = " + TextUtils.join( " ", categoryList ));

        String ogImage = null;
        Element mainImageElement = getMainImageElementFromPref(doc, url);
        if (mainImageElement != null ) {
            for (Element item : mainImageElement.getAllElements())
                if ( item.tagName().equals( "img" ) )
                    ogImage = item.attr( "src" );
        } else {
            Collection<Element> metas = getMetas(doc);
            for (Element entry : metas) {
                if (entry.hasAttr("property") && "og:image".equals(entry.attr("property"))) {
                    ogImage = entry.attr("content");
                    break;
                }
            }
        }
        SaveContentStepToFile( doc, "before FindBestElement RemoveHiddenElements" );

        AddColumnClassToTableCells(doc, url);

        Element rootElement = doc;
        if ( mobilize == MobilizeType.Yes) {
            rootElement = FindBestElement(doc, url, contentIndicator, isFindBestElement);
            RemoveHiddenElements(rootElement);
        }

        if ( rootElement == null )
            rootElement = doc;


        SaveContentStepToFile( doc, "before remove title" );

        Elements title = rootElement.getElementsByClass("title");
        for (Element entry : title)
            if (entry.tagName().equalsIgnoreCase("h1")) {
                title.first().remove();
                break;
            }

        StringBuilder ret = new StringBuilder(rootElement.toString());
        SaveContentStepToFile( ret, "after toString" );

        if (ogImage != null && !ret.toString().contains(ogImage)) {
            ret.insert(0, "<img src=\"" + ogImage + "\"><br>\n");
        }

        if ( mobilize == MobilizeType.Yes && PrefUtils.getBoolean(PrefUtils.LOAD_COMMENTS, false) ) {

            Element commentsElement = getCommentsElementFromPref(doc, url);
            if ( commentsElement == null )
                commentsElement = doc.getElementById( "comments" );
            if (commentsElement != null ) {
                for (Element item : commentsElement.children()) {
                    Elements li = item.getElementsByTag("li");
                    for (Element entry : li) {
                        entry.tagName("p");
                    }
                    Elements ul = item.getElementsByTag("ul");
                    for (Element entry : ul) {
                        entry.tagName("p");
                    }
                    RemoveHiddenElements(item);
                    ret.append(item);
                }
            }

            ret.append(AddHabrComments(url));
            SaveContentStepToFile( ret, "after load comments" );
        }

        if ( !isWithTables ) {
            ret = new StringBuilder(RemoveTables(ret.toString()));
            SaveContentStepToFile( ret, "after RemoveTables" );
        }

        final boolean isAutoFullTextRoot = getFullTextRootElementFromPref(doc, url) == null;
        ret = new StringBuilder(HtmlUtils.improveHtmlContent(ret.toString(), url, filters, categoryList, mobilize, isAutoFullTextRoot));

        if ( mobilize == MobilizeType.Tags ) {
            mLastLoadedAllDoc = ret.toString();
            Document tempDoc = Jsoup.parse(ret.toString(), NetworkUtils.getUrlDomain(url));
            //rootElement = FindBestElement(doc, url, contentIndicator, isFindBestElement);
            AddTagButtons(tempDoc, url, null);
            ret = new StringBuilder(tempDoc.toString());
            SaveContentStepToFile( ret, "improveHtmlContent TAGS" );
        }

        return ret.toString();
    }

    private static void AddColumnClassToTableCells(Element rootElement, String url) {
        int colNumber = 0;
        final String prefix = url //NetworkUtils.getBaseUrl_(url)
            .replace( "https://", "" )
            .replace( "http://", "" )
            .replace( "/", "_" );
        for ( Element el: rootElement.getAllElements() ) {
            if (el.tagName().equals("tr"))
                colNumber = 0;
            else if (el.tagName().equals("td")) {
                colNumber++;
                el.attr("class", prefix + "_" + colNumber);
            }
        }
    }

    private static String AddHabrComments(String url) {
        StringBuilder result = new StringBuilder();
        if ( url.contains( "habr.com" ) ) {
            Matcher matcher = Pattern.compile("/(\\d+)/").matcher(url);
            if ( matcher.find() ) {
                String id = matcher.group(1);
                final Connection conn = new Connection("https://habr.com/kek/v2/articles/ARTICLE_ID/comments/?fl=ru&hl=ru".replace("ARTICLE_ID", id ), OKHTTP);
                try {
                    final String s = conn.getText();
                    Dog.d(TAG, s);
                    JSONObject json = new JSONObject(s );
                    JSONObject list = json.getJSONObject( "comments");
                    result.append( String.format("<h2> %s </h2>", MainApplication.getContext().getString( R.string.comments )) );
                    for (int i = 0; i < list.names().length(); i++) {
                        JSONObject item = list.getJSONObject( list.names().getString( i ) );
                        JSONObject author = item.getJSONObject( "author" );
                        int score = item.getInt( "score" );
                        result.append( String.format("<p class='subtitle'>%s<i>%s</i>, %s</p>%s<br/>",
                                       score == 0 ? "" : String.format("[%s] ", score),
                                       GetValue(author, "alias" ) + " " + GetValue(author, "fullname" ) + " " + GetValue(author, "speciality" ),
                                       StringUtils.getDateTimeString(parseDate(item.getString("timePublished"), 0L).getTime() ),
                                       item.getString("message")) );
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    result.append( e.getLocalizedMessage() );
                } catch (JSONException e) {
                    e.printStackTrace();
                    result.append( e.getLocalizedMessage() );
                }
                conn.disconnect();
            }

        }
        return result.toString();
    }

    private static String GetValue( JSONObject json, String name ) throws JSONException {
        String s = json.getString( name );
        if ("null".equals( s ))
            return "";
        else
            return s;
    }
    public static void RemoveHiddenElements(Element rootElement) {
        final ArrayList<String> removeClassList = PrefUtils.GetRemoveClassList();
        for (String classItem : removeClassList) {
            if ( !classItem.isEmpty() ) {
                Elements list = rootElement.getElementsByClass(classItem);
                for (Element item : list)
                    item.remove();
            }
        }
    }

    @NotNull
    public static String RemoveTables(String ret) {
        ret = ret.replaceAll("<table(.)*?>", "<p>");
        ret = ret.replaceAll("</table>", "</p>");

        ret = ret.replaceAll("<tr(.)*?>", "<p>");
        ret = ret.replaceAll("</tr>", P_HR);

        ret = ret.replaceAll("<td(.)*?>", "<p>");
        ret = ret.replaceAll("</td>", "</p>");

        ret = ret.replaceAll("<th(.)*?>", "<p>");
        ret = ret.replaceAll("</th>", P_HR);
        return ret;
    }

    @NotNull
    public static String RemoveHeaders(String ret) {
        ret = ret.replaceAll("<h1(.)*?>", "");
        ret = ret.replaceAll("</h1>", "");

        ret = ret.replaceAll("<h2(.)*?>", "");
        ret = ret.replaceAll("</h2>", "");

        ret = ret.replaceAll("<h3(.)*?>", "");
        ret = ret.replaceAll("</h3>", "");

        return ret;
    }

    public static Element FindBestElement(Document doc, String url, String contentIndicator, boolean isFindBestElement) {

        // init elements
        Collection<Element> nodes = getNodes(doc);
        int maxWeight = 0;

        Element bestMatchElement = getFullTextRootElementFromPref(doc, url);
        if (bestMatchElement == null && isFindBestElement) {
            for (Element entry : nodes) {
                int currentWeight = getWeight(entry, contentIndicator);
                if (currentWeight > maxWeight) {
                    maxWeight = currentWeight;
                    bestMatchElement = entry;

                    if (maxWeight > 300) {
                        break;
                    }
                }
            }
        }
        if ( bestMatchElement == null )
            bestMatchElement = doc;


        return bestMatchElement;
    }

    public static void AddTagButtons(Document doc, String url, Element bestMatchElement) {
        doc.addClass( HANDY_NEWS_READER_ROOT_CLASS );

        String fullTextRootElementFromPrefClassName = getUrlClassNameFromPref( url, CONTENT_TEXT_ROOT_EXTRACT_RULES );

        final ArrayList<String> removeClassList = PrefUtils.GetRemoveClassList();

        final Element categoriesElement = getCategoriesElementFromPref(doc, url);
        final Elements categoriesAllElements = categoriesElement != null ? categoriesElement.getAllElements() : null;

        final Element dateElement = getDateElementFromPref(doc, url);

        for (Element el : doc.getElementsByAttribute(CLASS_ATTRIBUTE))
            if (el.hasText()) {
                //final String classNameList =
                //       el.attr(CLASS_ATTRIBUTE).trim().replaceAll("\\r|\\n", " ").replaceAll(" +", " ");
                final Set<String> classNameList = el.classNames();
                if ( classNameList.contains( TAG_BUTTON_CLASS) )
                    continue;
                for ( String className: classNameList ) {
                    if ( className.trim().isEmpty() )
                        continue;
                    final boolean isHidden = removeClassList.contains(className);
                    final boolean isCategory = categoriesAllElements != null && categoriesAllElements.contains( el );
                    final boolean isDate = dateElement != null && dateElement.equals( el );
                    final boolean isTextRoot = el.equals( bestMatchElement ) ||
                        el.hasAttr( HANDY_NEWS_READER_ROOT_CLASS ) ||
                        el.classNames().contains( fullTextRootElementFromPrefClassName );
                    AddTagButton(el, className, url, isHidden, isTextRoot, isCategory, isDate );
                    if ( isHidden && el.parent() != null ) {
                        Element elementS = doc.createElement("s");
                        elementS.addClass( TAG_BUTTON_CLASS_HIDDEN );
                        el.replaceWith(elementS);
                        elementS.insertChildren(0, el);
                    }
                }
            }
    }

    private static void AddTagButton(Element el, String className, String baseUrl,
                                     boolean isHidden, boolean isFullTextRoot, boolean isCategory, boolean isDate ) {
        final String paramValue = isHidden ? "show" : "hide";
        final String methodText = "openTagMenu('" + className + "', '" + baseUrl + "', '" + paramValue + "')";
        //final String fullTextRoot = isFullTextRoot ? " !!! " + MainApplication.getContext().getString( R.string.fullTextRoot ).toUpperCase() + " !!! " : "";
        final String tagClass;
        if ( isFullTextRoot )
            tagClass = TAG_BUTTON_FULL_TEXT_ROOT_CLASS;
        else if ( isCategory )
            tagClass = TAG_BUTTON_CLASS_CATEGORY;
        else if ( isDate )
            tagClass = TAG_BUTTON_CLASS_DATE;
        else if ( isHidden )
            tagClass = TAG_BUTTON_CLASS_HIDDEN;
        else
            tagClass = TAG_BUTTON_CLASS;
        el.append(HtmlUtils.getButtonHtml(methodText,  " " + className + " ]", tagClass));
        el.prepend(HtmlUtils.getButtonHtml(methodText,  "[ " + className + " ", tagClass));
    }


    public static Element getFullTextRootElementFromPref(Element doc, final String url ) {
        return getElementWithClassNameFromPref( doc, url, PrefUtils.getString(CONTENT_TEXT_ROOT_EXTRACT_RULES, R.string.full_text_root_default),
                                                HANDY_NEWS_READER_ROOT_CLASS, false);
    }
    private static Element getCategoriesElementFromPref(Element doc, final String url) {
        return getElementWithClassNameFromPref(doc, url, PrefUtils.getString(PrefUtils.CATEGORY_EXTRACT_RULES, ""), HANDY_NEWS_READER_CATEGORY_CLASS, false);
    }
    private static Element getMainImageElementFromPref(Element doc, final String url) {
        return getElementWithClassNameFromPref(doc, url, PrefUtils.getString(PrefUtils.MAIN_IMAGE_EXTRACT_RULES, ""), HANDY_NEWS_READER_MAIN_IMAGE_CLASS, true);
    }
    private static Element getCommentsElementFromPref(Element doc, final String url) {
        return getElementWithClassNameFromPref(doc, url, PrefUtils.getString(PrefUtils.COMMENTS_EXTRACT_RULES, ""), HANDY_NEWS_READER_COMMENTS_CLASS, true);
    }
    public static Element getDateElementFromPref(Element doc, final String url) {
        return getElementWithClassNameFromPref(doc, url, PrefUtils.getString(PrefUtils.DATE_EXTRACT_RULES, ""), HANDY_NEWS_READER_DATE_CLASS, true);
    }
    private static Element getElementWithClassNameFromPref(Element doc,
                                                           final String url,
                                                           final String pref,
                                                           final String className,
                                                           boolean canReturnEmptyElement ) {
        Element result = null;
        for( String line: pref.split( "\\n|\\s" ) ) {   //while ( result == null ) {
            if ( ( line == null ) || line.isEmpty() )
                continue;
            try {
                String[] list1 = line.split(":");
                String keyWord = list1[0];
                String[] list2 = list1[1].split("=");
                String elementType = list2[0].toLowerCase();
                String elementValue = list2[1];
                //if (doc.head().html().contains(keyWord)) {
                if (url.contains(keyWord)) {
                    if ( elementValue.equals( HANDY_NEWS_READER_ROOT_CLASS ) )
                        return doc;
                    if (elementType.equals("id"))
                        result = doc.getElementById(elementValue);
                    else if (elementType.equals("class")) {
                        final Elements elements = doc.getElementsByClass(elementValue);
                        if ( canReturnEmptyElement && elements.isEmpty() )
                            result = new Element(EMPTY_TAG);
                        if (!elements.isEmpty()) {
                            if ( elements.size() == 1 )
                                result = elements.first();
                            else {
                                for ( Element el: elements )
                                    el.attr(className, "1");
                                result = new Element("p");
                                result.insertChildren(0, elements.clone());
                                elements.remove();
                            }
                        } else if ( HANDY_NEWS_READER_ROOT_CLASS.equals( className ) )
                            result = doc;
                    }
                    break;
                }
            } catch ( Exception e ) {
                Dog.e( e.getMessage() );
            }

        }
        return result;
    }
    public static String getDataForUrlFromPref(final String url, final String pref ) {
        String result = "";
        for( String line: pref.split( "\\n" ) ) {   //while ( result == null ) {
            if ( ( line == null ) || line.isEmpty() )
                continue;
            try {
                String[] list1 = line.split(":");
                String keyWord = list1[0];
                if (url.contains(keyWord)) {
                    String[] list2 = list1[1].split("=");
                    //String elementType = list2[0].toLowerCase();
                    //String elementValue = list2[1];
                    if (list2.length >= 3)
                        result = list2[2];
                    break;
                }
            } catch ( Exception e ) {
                Dog.e( e.getMessage() );
            }

        }
        return result;
    }
    private static String getUrlClassNameFromPref(final String url, final String prefKey ) {
        String result = "";
        final String pref = PrefUtils.getString( prefKey, "" );
        for( String line: pref.split( "\\n|\\s" ) ) {
            if ( ( line == null ) || line.isEmpty() )
                continue;
            try {
                String[] list1 = line.split(":");
                String keyWord = list1[0];
                String[] list2 = list1[1].split("=");
                String  elementType = list2[0].toLowerCase();
                String elementValue = list2[1];
                if (url.contains(keyWord))
                    return elementValue;
            } catch ( Exception e ) {
                Dog.e( e.getMessage() );
            }

        }
        return result;
    }

    /**
     * Weights current element. By matching it with positive candidates and
     * weighting child nodes. Since it's impossible to predict which exactly
     * names, ids or class names will be used in HTML, major role is played by
     * child nodes
     *
     * @param e                Element to weight, along with child nodes
     * @param contentIndicator a text which should be included into the extracted content, or null
     */
    private static int getWeight(Element e, String contentIndicator) {
        int weight = calcWeight(e);
        weight += (int) Math.round(e.ownText().length() / 100.0 * 10);
        weight += weightChildNodes(e, contentIndicator);
        return weight;
    }

    /**
     * Weights a child nodes of given Element. During tests some difficulties
     * were met. For instance, not every single document has nested paragraph
     * tags inside of the major article tag. Sometimes people are adding one
     * more nesting level. So, we're adding 4 points for every 100 symbols
     * contained in tag nested inside of the current weighted element, but only
     * 3 points for every element that's nested 2 levels deep. This way we give
     * more chances to extract the element that has less nested levels,
     * increasing probability of the correct extraction.
     *
     * @param rootEl           Element, who's child nodes will be weighted
     * @param contentIndicator a text which should be included into the extracted content, or null
     */
    private static int weightChildNodes(Element rootEl, String contentIndicator) {
        int weight = 0;
        Element caption = null;
        List<Element> pEls = new ArrayList<>(5);
        for (Element child : rootEl.children()) {
            String text = child.text();
            int textLength = text.length();
            if (textLength < 20) {
                continue;
            }

            if (contentIndicator != null && text.contains(contentIndicator)) {
                weight += 100; // We certainly found the item
            }

            String ownText = child.ownText();
            int ownTextLength = ownText.length();
            if (ownTextLength > 200) {
                weight += Math.max(50, ownTextLength / 10);
            }

            if (child.tagName().equals("h1") || child.tagName().equals("h2")) {
                weight += 30;
            } else if (child.tagName().equals("div") || child.tagName().equals("p")) {
                weight += calcWeightForChild(ownText);
                if (child.tagName().equals("p") && textLength > 50)
                    pEls.add(child);

                if (child.className().toLowerCase().equals("caption"))
                    caption = child;
            }
        }

        // use caption and image
        if (caption != null)
            weight += 30;

        if (pEls.size() >= 2) {
            for (Element subEl : rootEl.children()) {
                if ("h1;h2;h3;h4;h5;h6".contains(subEl.tagName())) {
                    weight += 20;
                    // headerEls.add(subEl);
                }
            }
        }
        return weight;
    }

    private static int calcWeightForChild(String text) {
        return text.length() / 25;
//		return Math.min(100, text.length() / ((child.getAllElements().size()+1)*5));
    }

    private static int calcWeight(Element e) {
        int weight = 0;
        if (POSITIVE.matcher(e.className()).find())
            weight += 35;

        if (POSITIVE.matcher(e.id()).find())
            weight += 40;

        if (UNLIKELY.matcher(e.className()).find())
            weight -= 20;

        if (UNLIKELY.matcher(e.id()).find())
            weight -= 20;

        if (NEGATIVE.matcher(e.className()).find())
            weight -= 50;

        if (NEGATIVE.matcher(e.id()).find())
            weight -= 50;

        String style = e.attr("style");
        if (style != null && !style.isEmpty() && NEGATIVE_STYLE.matcher(style).find())
            weight -= 50;
        return weight;
    }

    /**
     * Prepares document. Currently only stipping unlikely candidates, since
     * from time to time they're getting more score than good ones especially in
     * cases when major text is short.
     *
     * @param doc document to prepare. Passed as reference, and changed inside
     *            of function
     */
    private static void prepareDocument(Element doc, MobilizeType mobilize, boolean withScripts) {
        // stripUnlikelyCandidates(doc);
        if ( mobilize == MobilizeType.Yes )
            removeSelectsAndOptions(doc);
        removeScriptsAndStyles(doc, withScripts);
        SaveContentStepToFile( doc, "after prepareDocument" );
    }

    /**
     * Removes unlikely candidates from HTML. Currently takes id and class name
     * and matches them against list of patterns
     *
     * @param doc document to strip unlikely candidates from
     */
//    protected void stripUnlikelyCandidates(Document doc) {
//        for (Element child : doc.select("body").select("*")) {
//            String className = child.className().toLowerCase();
//            String id = child.id().toLowerCase();
//
//            if (NEGATIVE.matcher(className).find()
//                    || NEGATIVE.matcher(id).find()) {
//                child.remove();
//            }
//        }
//    }
    private static void removeScriptsAndStyles(Element doc, boolean withScripts) {
        Elements scripts = doc.getElementsByTag("script");
        if ( !withScripts )
            for (Element item : scripts)
                item.remove();

        if (FetcherService.isCancelRefresh())
            return;

        Elements noscripts = doc.getElementsByTag("noscript");
        for (Element item : noscripts)
            item.remove();

        if (FetcherService.isCancelRefresh())
            return;

        Elements styles = doc.getElementsByTag("style");
        for (Element style : styles)
            style.remove();
    }

    private static void removeSelectsAndOptions(Element doc) {
        Elements selects = doc.getElementsByTag("select");
        for (Element item : selects)
            item.remove();

        if (FetcherService.isCancelRefresh())
            return;

        Elements noscripts = doc.getElementsByTag("option");
        for (Element item : noscripts)
            item.remove();
    }

    /**
     * @return a set of all meta nodes
     */
    private static Collection<Element> getMetas(Element doc) {
        Collection<Element> nodes = new HashSet<>(64);
        nodes.addAll(doc.select("head").select("meta"));
        return nodes;
    }

    /**
     * @return a set of all important nodes
     */
    private static Collection<Element> getNodes(Element doc) {
        Collection<Element> nodes = new HashSet<>(64);
        for (Element el : doc.select("body").select("*")) {
            if (NODES.matcher(el.tagName()).matches()) {
                nodes.add(el);
            }
        }
        return nodes;
    }
}
