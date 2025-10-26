package ru.yanus171.feedexfork.fragment;

import android.os.Build;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.widget.SearchView;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.view.WebViewExtended;

public class EntryTextSearch {
    private final WebViewExtended mWebView;
    private String mSearchText = "";
    private MenuItem mSearchNextItem = null;
    private MenuItem mSearchPreviousItem = null;

    public EntryTextSearch(WebViewExtended webView) {
        mWebView = webView;
        if (Build.VERSION.SDK_INT >= 16 ) {
            mWebView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                if (mSearchNextItem == null || mSearchPreviousItem == null)
                    return;
                mSearchNextItem.setVisible(numberOfMatches > 1);
                mSearchPreviousItem.setVisible(numberOfMatches > 1);
            });
        }

    }

    public void onCreateOptionsMenu(Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        mSearchNextItem = menu.findItem(R.id.menu_search_next);
        mSearchPreviousItem = menu.findItem(R.id.menu_search_previous);
        mSearchNextItem.setVisible( false );
        mSearchPreviousItem.setVisible( false );
        if (!mSearchText.isEmpty()) {
            searchItem.expandActionView();
            // Without that, it just does not work
            searchView.post(() -> {
                searchView.setQuery(mSearchText, false);
                searchView.clearFocus();
            });
        }searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSearchText = newText;
                mSearchNextItem.setVisible( false );
                mSearchPreviousItem.setVisible( false );

                if (!TextUtils.isEmpty(newText)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                        mWebView.findAllAsync( newText );
                    else
                        mWebView.findAll( newText );
                }
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            mSearchText = "";
            mSearchNextItem.setVisible( false );
            mSearchPreviousItem.setVisible( false );
            mWebView.clearMatches();
            return false;
        });
        // Use a custom search icon for the SearchView in AppBar
        int searchImgId = androidx.appcompat.R.id.search_button;
        ImageView v = searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search);

    }
}
