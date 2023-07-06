package ru.yanus171.feedexfork.parser;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.service.MarkItem;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.LabelVoc;

import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_AUTHOR;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CATEGORY;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_CONTENT;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_TITLE;
import static ru.yanus171.feedexfork.provider.FeedData.FilterColumns.DB_APPLIED_TO_URL;
import static ru.yanus171.feedexfork.service.FetcherService.mMarkAsStarredFoundList;

public class FeedFilters {

    private final ArrayList<Rule> mFilters = new ArrayList<>();
    public FeedFilters(String feedId) {
        init( GetCursor(feedId), true );
        if ( !feedId.equals( FetcherService.GetExtrenalLinkFeedID() ) )
            init( GetCursor(FetcherService.GetExtrenalLinkFeedID() ), true );
    }
    public FeedFilters(Cursor c) {
        init( c, false );
        init( GetCursor(FetcherService.GetExtrenalLinkFeedID() ), true );
    }
    public void init( Cursor cursor, boolean closeCursor ) {
        if ( cursor.moveToFirst() )
            do {
                Rule r = new Rule();
                r.filterText = cursor.getString(0);
                r.isRegex = cursor.getInt(1) == 1;
                r.mApplyType = cursor.getInt(2);
                r.isAcceptRule = cursor.getInt(3) == 1;
                r.isMarkAsStarred = cursor.getInt(4) == 1;
                r.isRemoveText = cursor.getInt(5) == 1;
                if ( !cursor.isNull( 6 ) )
                    r.labelIDList = LabelVoc.INSTANCE.stringToList(cursor.getString(6) );
                mFilters.add(r);
            } while ( cursor.moveToNext() );
        if ( closeCursor )
            cursor.close();
    }

    public static Cursor GetCursor(String feedId) {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        return cr.query(getCursorUri(feedId), getCursorProjection(), null, null, null);
    }

    @NotNull
    public static String[] getCursorProjection() {
        return new String[]{FeedData.FilterColumns.FILTER_TEXT, FeedData.FilterColumns.IS_REGEX,
                FeedData.FilterColumns.APPLY_TYPE, FeedData.FilterColumns.IS_ACCEPT_RULE, FeedData.FilterColumns.IS_MARK_STARRED,
                FeedData.FilterColumns.IS_REMOVE_TEXT, FeedData.FilterColumns.LABEL_ID_LIST};
    }

    public static Uri getCursorUri(String feedId) {
        return FeedData.FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId);
    }

    public boolean isEntryFiltered(String title, String author, String url, String content, String[] categoryList) {
        final String categories = categoryList == null ? "" : TextUtils.join( ", ", categoryList );
        boolean isFiltered = false;

        for (Rule r : mFilters) {
            if ( r.isMarkAsStarred || r.isRemoveText )
                continue;

            if ( r.isAcceptRule && r.mApplyType == DB_APPLIED_TO_CATEGORY && categories.isEmpty() )
                continue;

            boolean isMatch = r.isMatch( title, author, url, content, categories );

            if (r.isAcceptRule) {
                if (isMatch) {

                    isFiltered = false;
                    break; // accept rules override reject rules, the rest of the rules must be ignored
                } else {
                    isFiltered = true;
                    //break;
                }
            } else if ( isMatch ) {
                isFiltered = true;
                //break; // no break, there might be an accept rule later
            }
        }

        return isFiltered;
    }

    public boolean isMarkAsStarred(String title, String author, String url, String content, String[] categoryList) {
        final String categories = categoryList == null ? "" : TextUtils.join( ", ", categoryList );
        for (Rule r : mFilters)
            if ( r.isMarkAsStarred && r.isMatch( title, author, url, content, categories ) ) {
                synchronized(mMarkAsStarredFoundList) {
                    mMarkAsStarredFoundList.add( new MarkItem( title, url, r.labelIDList ));
                }
                return true;
            }
        return false;
    }
    public String removeText( String text, int applyType ) {
        if ( text == null )
            return text;
        String result = text;
        for (Rule r : mFilters)
            if ( r.isRemoveText && applyType == r.mApplyType) {
                if (r.isRegex) {
                    try {
                        result = result.replaceAll(r.filterText, "");
                    } catch ( PatternSyntaxException e ) {
                        DebugApp.AddErrorToLog( r.filterText, e );
                    }
                } else
                    result = result.replace( r.filterText, "" );
            }
        return result;
    }

    private class Rule {
        String filterText;
        boolean isRegex;
        int mApplyType;
        boolean isAcceptRule;
        boolean isMarkAsStarred = false;
        public HashSet<Long> labelIDList;
        boolean isRemoveText = false;

        boolean isMatch(String title, String author, String url, String content, String categories) {
            boolean result = false;
            author = author == null ? "" : author;
            title = title == null ? "" : title;
            url = url == null ? "" : url;
            categories = categories == null ? "" : categories;
            content = content == null ? "" : content;

            if (isRegex) {
                try {
                    Pattern p = Pattern.compile(filterText);
                    if ( mApplyType == DB_APPLIED_TO_TITLE)
                        result = p.matcher(title).find();
                    else if ( mApplyType == DB_APPLIED_TO_AUTHOR)
                        result = p.matcher(author).find();
                    else if ( mApplyType == DB_APPLIED_TO_URL)
                        result = p.matcher(url).find();
                    else if ( mApplyType == DB_APPLIED_TO_CATEGORY)
                        result = p.matcher(categories).find();
                    else if ( mApplyType == DB_APPLIED_TO_CONTENT)
                        result = p.matcher(content).find();
                } catch ( PatternSyntaxException e ) {
                    DebugApp.AddErrorToLog( null, e  );
                }
            } else {
                final String filterTextLow = filterText.toLowerCase();
                result =
                    ( mApplyType == DB_APPLIED_TO_TITLE && title.toLowerCase().contains(filterTextLow ) ) ||
                    ( mApplyType == DB_APPLIED_TO_CATEGORY && categories.toLowerCase().contains(filterTextLow ) ) ||
                    ( mApplyType == DB_APPLIED_TO_CONTENT && content.toLowerCase().contains(filterTextLow ) ) ||
                    ( mApplyType == DB_APPLIED_TO_URL && url.toLowerCase().contains(filterTextLow ) ) ||
                    ( mApplyType == DB_APPLIED_TO_AUTHOR && author.toLowerCase().contains(filterTextLow ) );
            }
            return result;
        }
    }
}
