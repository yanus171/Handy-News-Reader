package ru.yanus171.feedexfork.fragment;

import android.database.DatabaseUtils;
import android.net.Uri;
import android.view.MenuItem;

import androidx.appcompat.widget.SearchView;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;

public class EntriesSearch {
    private String mSearchText = "";

    @NotNull
    public String getWhereClause() {
        if (mSearchText.isEmpty()) {
            return "";
        }
        return getSearchWhereClause(mSearchText);
    }

    public boolean hasSearch() {
        return !mSearchText.isEmpty();
    }

    public void clearSearch(androidx.appcompat.widget.SearchView searchView) {
        mSearchText = "";
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }
    }

    public interface OnSearchChangedListener {
        void onSearchChanged(String newText);
    }

    public void setupSearchView(@NotNull SearchView searchView,
                                @NotNull MenuItem searchItem,
                                @NotNull OnSearchChangedListener onSearchChanged,
                                @NotNull Runnable onSearchClosed) {
        int searchImgId = androidx.appcompat.R.id.search_button;
        android.widget.ImageView v = searchView.findViewById(searchImgId);
        if (v != null) {
            v.setImageResource(R.drawable.ic_search);
        }

        if (!mSearchText.isEmpty()) {
            searchItem.expandActionView();
            searchView.post(() -> {
                searchView.setQuery(mSearchText, false);
                searchView.clearFocus();
            });
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!android.text.TextUtils.isEmpty(newText)) {
                    mSearchText = newText;
                }
                onSearchChanged.onSearchChanged(newText);
                return false;
            }
        });

        searchView.setOnCloseListener(() -> {
            clearSearch(searchView);
            onSearchClosed.run();
            return false;
        });
    }

    @NotNull
    private static String getSearchWhereClause(String uriSearchParam) {
        uriSearchParam = Uri.decode(uriSearchParam).trim();
        Pattern regex = Pattern.compile("\\b(?:AND|OR)\\b");
        Matcher matcher = regex.matcher(uriSearchParam);
        int prevIndex = 0;
        String where = "";
        while (matcher.find()) {
            final String word = uriSearchParam.substring(prevIndex, matcher.start()).trim();
            prevIndex = Math.min(uriSearchParam.length() - 1, matcher.end() + 1);
            if (word.isEmpty()) {
                continue;
            }
            where += EntryColumns.TITLE + " LIKE " + DatabaseUtils.sqlEscapeString("%" + word + "%")
                    + " " + matcher.group() + " ";
        }
        final String word = uriSearchParam.substring(prevIndex).trim();
        if (!word.isEmpty()) {
            where += EntryColumns.TITLE + " LIKE " + DatabaseUtils.sqlEscapeString("%" + word + "%");
        } else if (!where.isEmpty()) {
            where += "(1 = 2)";
        }
        return where;
    }
}
