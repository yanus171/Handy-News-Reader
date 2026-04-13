package ru.yanus171.feedexfork.fragment;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import androidx.appcompat.widget.SearchView;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.view.WebViewExtended;

public class EntryTextSearch {
    private final WebViewExtended mWebView;
    private String mSearchText = "";
    private MenuItem mSearchNextItem = null;
    private MenuItem mSearchPreviousItem = null;
    private MenuItem mSearchItem = null;
    private SearchView mSearchView = null;

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
        mSearchItem = menu.findItem(R.id.menu_search);
        if ( mSearchItem == null )
            return;
        mSearchNextItem = menu.findItem(R.id.menu_search_next);
        mSearchPreviousItem = menu.findItem(R.id.menu_search_previous);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        int searchCloseButtonId = mSearchView.findViewById(androidx.appcompat.R.id.search_close_btn).getId();
        ImageView closeButton = mSearchView.findViewById(searchCloseButtonId);
        closeButton.setOnClickListener(view -> {
            mSearchText = "";
            mSearchView.clearFocus();
            mSearchView.setQuery("", false);
            mSearchNextItem.setVisible( false );
            mSearchPreviousItem.setVisible( false );
            mWebView.clearMatches();
            hideKeyboard( mSearchView );
        });
        // Use a custom search icon for the SearchView in AppBar
        int searchImgId = androidx.appcompat.R.id.search_button;
        ImageView v = mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search);
    }
    private void hideKeyboard( SearchView searchView ) {
        InputMethodManager imm = (InputMethodManager) MainApplication.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
    }
    public void onPrepareOptionsMenu() {
        if ( mSearchItem == null )
            return;
        mSearchNextItem.setVisible( false );
        mSearchPreviousItem.setVisible( false );
        if (!mSearchText.isEmpty()) {
            final SearchView searchView = (SearchView) mSearchItem.getActionView();
            mSearchItem.expandActionView();
            // Without that, it just does not work
            searchView.post(() -> {
                searchView.setQuery(mSearchText, false);
                searchView.clearFocus();
            });
        }
    }

}
