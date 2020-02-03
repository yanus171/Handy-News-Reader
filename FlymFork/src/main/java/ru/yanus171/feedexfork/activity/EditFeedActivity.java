/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.activity;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.adapter.FiltersCursorAdapter;
import ru.yanus171.feedexfork.fragment.EditFeedsListFragment;
import ru.yanus171.feedexfork.fragment.GeneralPrefsFragment;
import ru.yanus171.feedexfork.loader.BaseLoader;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Connection;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.activity.EditFeedActivity.IS_READ_STUMB;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.ITEM_DESC;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.ITEM_ICON;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.ITEM_TITLE;
import static ru.yanus171.feedexfork.activity.EditFeedActivity.ITEM_URL;

public class EditFeedActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_WEB_SEARCH = "EXTRA_WEB_SEARCH";
    static final String ITEM_TITLE = "title";
    static final String ITEM_URL = "feedId";
    static final String ITEM_DESC = "description";
    static final String ITEM_ISREAD = "read";
    static final String ITEM_ICON = "icon";
    private static final String STATE_CURRENT_TAB = "STATE_CURRENT_TAB";
    private static final String STATE_FEED_EDIT_LOAD_TYPE_ID = "STATE_FEED_EDIT_LOAD_TYPE_ID";
    public static final String DUCKDUCKGO_SEARCH_URL = "http://duckduckgo.com/html/?q=";
    private static final String[] FEED_PROJECTION =
        new String[]{FeedColumns.NAME, FeedColumns.URL, FeedColumns.RETRIEVE_FULLTEXT, FeedColumns.IS_GROUP, FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, FeedColumns.IS_AUTO_REFRESH, FeedColumns.GROUP_ID, FeedColumns.IS_IMAGE_AUTO_LOAD, FeedColumns.OPTIONS  };
    public static final String STATE_WEB_SEARCH_TEXT = "WEB_SEARCH_TEXT";
    public static final String DIALOG_IS_SHOWN = "EDIT_FEED_USER_SELECTION_DIALOG_IS_SHOWN";
    static final String IS_READ_STUMB = "[IS_READ]";
    private String[] mKeepTimeValues;


    private final ActionMode.Callback mFilterActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_edit:
                    EditFilter();

                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_delete:
                    final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                    new AlertDialog.Builder(EditFeedActivity.this) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle(R.string.filter_delete_title) //
                            .setMessage(R.string.question_delete_filter) //
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            ContentResolver cr = getContentResolver();
                                            if (cr.delete(FilterColumns.CONTENT_URI, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                cr.notifyChange(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                        null);
                                            }
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null).show();

                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFiltersCursorAdapter.setSelectedFilter(-1);
            mFiltersListView.invalidateViews();
        }
    };
    private Spinner mKeepTimeSpinner = null;
    private CheckBox mKeepTimeCB = null;
    private AlertDialog mUserSelectionDialog = null;

    private void EditFilter() {
        Cursor c = mFiltersCursorAdapter.getCursor();
        if (c.moveToPosition(mFiltersCursorAdapter.getSelectedFilter())) {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);
            final EditText filterText = dialogView.findViewById(R.id.filterText);
            final CheckBox regexCheckBox = dialogView.findViewById(R.id.regexCheckBox);
            final RadioButton applyTitleRadio = dialogView.findViewById(R.id.applyTitleRadio);
            final RadioButton applyContentRadio = dialogView.findViewById(R.id.applyContentRadio);
            final RadioButton acceptRadio = dialogView.findViewById(R.id.acceptRadio);
            final RadioButton markAsStarredRadio = dialogView.findViewById(R.id.markAsStarredRadio);
            final RadioButton rejectRadio = dialogView.findViewById(R.id.rejectRadio);

            filterText.setText(c.getString(c.getColumnIndex(FilterColumns.FILTER_TEXT)));
            regexCheckBox.setChecked(c.getInt(c.getColumnIndex(FilterColumns.IS_REGEX)) == 1);
            if (c.getInt(c.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE)) == 1) {
                applyTitleRadio.setChecked(true);
            } else {
                applyContentRadio.setChecked(true);
            }
            if (c.getInt(c.getColumnIndex(FilterColumns.IS_MARK_STARRED)) == 1) {
                markAsStarredRadio.setChecked(true);
            } else if (c.getInt(c.getColumnIndex(FilterColumns.IS_ACCEPT_RULE)) == 1) {
                acceptRadio.setChecked(true);
            } else {
                rejectRadio.setChecked(true);
            }

            final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
            new AlertDialog.Builder(EditFeedActivity.this) //
                    .setTitle(R.string.filter_edit_title) //
                    .setView(dialogView) //
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread() {
                                @Override
                                public void run() {
                                    String filter = filterText.getText().toString();
                                    if (!filter.isEmpty()) {
                                        ContentResolver cr = getContentResolver();
                                        ContentValues values = new ContentValues();
                                        values.put(FilterColumns.FILTER_TEXT, filter);
                                        values.put(FilterColumns.IS_REGEX, regexCheckBox.isChecked());
                                        values.put(FilterColumns.IS_APPLIED_TO_TITLE, applyTitleRadio.isChecked());
                                        values.put(FilterColumns.IS_ACCEPT_RULE, acceptRadio.isChecked());
                                        values.put(FilterColumns.IS_MARK_STARRED, markAsStarredRadio.isChecked());
                                        if (cr.update(FilterColumns.CONTENT_URI, values, FilterColumns._ID + '=' + filterId, null) > 0) {
                                            cr.notifyChange(
                                                    FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                    null);
                                        }
                                    }
                                }
                            }.start();
                        }
                    }).setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private TabHost mTabHost;
    private EditText mNameEditText, mUrlEditText;
    private CheckBox mRetrieveFulltextCb;
    private CheckBox mShowTextInEntryListCb;
    private CheckBox mIsAutoRefreshCb, mIsAutoImageLoadCb;
    private ListView mFiltersListView;
    private Spinner mGroupSpinner;
    private CheckBox mHasGroupCb;
    private FiltersCursorAdapter mFiltersCursorAdapter;
    private RadioGroup mLoadTypeRG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_feed_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();

        mTabHost = findViewById(R.id.tabHost);
        mNameEditText = findViewById(R.id.feed_title);
        mUrlEditText = findViewById(R.id.feed_url);
        mRetrieveFulltextCb = findViewById(R.id.retrieve_fulltext);
        mShowTextInEntryListCb = findViewById(R.id.show_text_in_entry_list);
        mIsAutoRefreshCb = findViewById(R.id.auto_refresh);
        mIsAutoImageLoadCb = findViewById(R.id.auto_image_load);
        mFiltersListView = findViewById(android.R.id.list);
        mGroupSpinner = findViewById(R.id.spin_group);

        mKeepTimeCB = findViewById(R.id.cbCustomKeepTime);
        mKeepTimeCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UpdateSpinnerKeepTime();
            }
        });
        mKeepTimeValues = getResources().getStringArray(R.array.settings_keep_time_values);
        mKeepTimeSpinner = findViewById(R.id.spin_keeptime);
        mKeepTimeSpinner.setSelection( 4 );
        for ( int i = 0; i < mKeepTimeValues.length; i ++ )
            if ( Double.parseDouble( mKeepTimeValues[i] ) == FetcherService.GetDefaultKeepTime() ) {
                mKeepTimeSpinner.setSelection( i );
                break;
            }

        mHasGroupCb = findViewById(R.id.has_group);
        mHasGroupCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UpdateSpinnerGroup();
            }
        });
        mLoadTypeRG = findViewById(R.id.rgLoadType);
        View tabWidget = findViewById(android.R.id.tabs);


        mIsAutoRefreshCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mIsAutoImageLoadCb.setEnabled( b );
            }
        });
        mIsAutoRefreshCb.setChecked( false );
        mIsAutoRefreshCb.setVisibility(  PrefUtils.getBoolean( PrefUtils.REFRESH_ONLY_SELECTED, false ) ? View.VISIBLE : View.GONE );

        mLoadTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                ShowControls();

            }
        });

        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("feedTab").setIndicator(getString(R.string.tab_feed_title)).setContent(R.id.feed_tab));
        mTabHost.addTab(mTabHost.newTabSpec("filtersTab").setIndicator(getString(R.string.tab_filters_title)).setContent(R.id.filters_tab));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                invalidateOptionsMenu();
            }
        });

        if (savedInstanceState != null) {
            mTabHost.setCurrentTab(savedInstanceState.getInt(STATE_CURRENT_TAB));
        }

        ResourceCursorAdapter adapter =
            new ResourceCursorAdapter( this,
                                       android.R.layout.simple_spinner_item,
                                       getContentResolver().query( FeedColumns.GROUPS_CONTENT_URI, new String[] { FeedColumns._ID, FeedColumns.NAME }, null, null, FeedColumns.NAME ),
                                       0 ) {
                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    TextView nameTextView = view.findViewById(android.R.id.text1);
                    nameTextView.setText(cursor.getString(1));
                }
            };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGroupSpinner.setAdapter( adapter );

        mIsAutoImageLoadCb.setVisibility( PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true) ? View.VISIBLE : View.GONE );
        PrefUtils.putBoolean( DIALOG_IS_SHOWN, false );

        setTitle( R.string.new_feed_title );
        tabWidget.setVisibility(View.GONE);

        if ( intent.getAction().equals(Intent.ACTION_INSERT) ||
             intent.getAction().equals(Intent.ACTION_SEND) ) {
            if (intent.hasExtra(Intent.EXTRA_TEXT))
                mUrlEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            mHasGroupCb.setChecked(false);
            mIsAutoImageLoadCb.setChecked( true );
            mKeepTimeCB.setChecked( false );

        } else if ( intent.getAction().equals(Intent.ACTION_VIEW) ) {
            tabWidget.setVisibility(View.GONE);
            mUrlEditText.setText(intent.getDataString());
            mLoadTypeRG.check( R.id.rbRss );

        } else if (intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            mLoadTypeRG.check( R.id.rbWebPageSearch );

        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            setTitle(R.string.edit_feed_title);
            tabWidget.setVisibility(View.VISIBLE);

            mFiltersCursorAdapter = new FiltersCursorAdapter(this, Constants.EMPTY_CURSOR);
            mFiltersListView.setAdapter(mFiltersCursorAdapter);
            mFiltersListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    startSupportActionMode(mFilterActionModeCallback);
                    mFiltersCursorAdapter.setSelectedFilter(position);
                    mFiltersListView.invalidateViews();
                    return true;
                }
            });

            mFiltersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mFiltersCursorAdapter.setSelectedFilter(position);
                    mFiltersListView.invalidateViews();
                    EditFilter();
                    //return true;
                }
            });

            getLoaderManager().initLoader(0, null, this);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            if (savedInstanceState == null) {
                Cursor cursor = getContentResolver().query(intent.getData(), FEED_PROJECTION, null, null, null);
                if (cursor != null && cursor.moveToNext()) {
                    mNameEditText.setText(cursor.getString(0));
                    mUrlEditText.setText(cursor.getString(1));
                    mRetrieveFulltextCb.setChecked(cursor.getInt(2) == 1);
                    mShowTextInEntryListCb.setChecked(cursor.getInt(4) == 1);
                    mIsAutoRefreshCb.setChecked(cursor.getInt(5) == 1);
                    mIsAutoImageLoadCb.setChecked(cursor.isNull(7) || cursor.getInt(7) == 1);
                    mGroupSpinner.setSelection( -1);
                    //mKeepTimeSpinner.setSelection( -1);
                    mHasGroupCb.setChecked(!cursor.isNull(6));
                    UpdateSpinnerGroup();
                    if ( !cursor.isNull(6) )
                        for ( int i = 0; i < mGroupSpinner.getCount(); i ++ )
                            if ( mGroupSpinner.getItemIdAtPosition( i ) == cursor.getInt(6) ) {
                                mGroupSpinner.setSelection( i );
                                break;
                            }

                    try {
                        JSONObject jsonOptions  = new JSONObject( cursor.getString( cursor.getColumnIndex(FeedColumns.OPTIONS) ) );
                        mLoadTypeRG.check( jsonOptions.getBoolean( "isRss" ) ? R.id.rbRss : R.id.rbWeb );
                        mKeepTimeCB.setChecked( jsonOptions.has( FetcherService.CUSTOM_KEEP_TIME ) );
                        UpdateSpinnerKeepTime();
                        if ( mKeepTimeCB.isChecked() ) {
                            for ( int i = 0; i < mKeepTimeValues.length; i ++ )
                                if ( Double.parseDouble( mKeepTimeValues[i] ) == jsonOptions.getDouble( FetcherService.CUSTOM_KEEP_TIME ) ) {
                                    mKeepTimeSpinner.setSelection( i );
                                    break;
                                }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //mKeepTimeSpinner.setVisibility( mKeepTimeCB.isChecked() ? View.VISIBLE : View.GONE );

                    if (cursor.getInt(3) == 1) { // if it's a group, we cannot edit it
                        finish();
                    }
                } else {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.error);
                    finish();
                }

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        mUrlEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handled = true;
                    Validate();
                }
                return handled;
            }
        });
        ShowControls();
        findViewById( R.id.brightnessSlider ).setVisibility( View.GONE );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ( getIntent().hasExtra( EXTRA_WEB_SEARCH ) )
            mLoadTypeRG.check(R.id.rbWebPageSearch);

        if ( mLoadTypeRG.getCheckedRadioButtonId() == R.id.rbWebPageSearch )
            mUrlEditText.setText( PrefUtils.getString( STATE_WEB_SEARCH_TEXT, "" ) );

        if ( IsAdd() ) {
            mUrlEditText.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        if ( PrefUtils.getBoolean(DIALOG_IS_SHOWN, false )  &&
                ( mUserSelectionDialog == null || !mUserSelectionDialog.isShowing() ) &&
             mLoadTypeRG.getCheckedRadioButtonId() == R.id.rbWebPageSearch ) {
            final String urlOrSearch = mUrlEditText.getText().toString().trim();
            GetWebSearchDuckDuckGoResultsLoader loader = new GetWebSearchDuckDuckGoResultsLoader(EditFeedActivity.this, urlOrSearch );
            AddFeedFromUserSelection("", getString(R.string.web_page_search_duckduckgo) + "\n" + loader.mUrl, loader );
        }
    }

    private void ShowControls() {
        int i = mLoadTypeRG.getCheckedRadioButtonId();
        final boolean isRss = ( i == R.id.rbRss );
        final boolean isWebPageSearch = ( i == R.id.rbWebPageSearch );
        final int visibility = isWebPageSearch ? View.GONE : View.VISIBLE;
        mRetrieveFulltextCb.setEnabled( isRss && !isWebPageSearch );

        mRetrieveFulltextCb.setVisibility( visibility );
        mHasGroupCb.setVisibility( visibility );
        mGroupSpinner.setVisibility( visibility );
        mIsAutoImageLoadCb.setVisibility( visibility );
        mIsAutoRefreshCb.setVisibility( visibility );
        mKeepTimeCB.setVisibility( visibility );
        mKeepTimeSpinner.setVisibility( visibility );
        mShowTextInEntryListCb.setVisibility( visibility );
        mNameEditText.setVisibility( visibility );
        findViewById( R.id.name_textview ).setVisibility( visibility );
        findViewById( R.id.rbWebPageSearch ).setVisibility( IsAdd() ? View.VISIBLE : View.GONE );
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        UpdateSpinnerGroup();
        UpdateSpinnerKeepTime();
    }

    private boolean IsAdd() {
        return getIntent().getAction().equals(Intent.ACTION_INSERT) ||
            getIntent().getAction().equals(Intent.ACTION_SEND) ||
            getIntent().getAction().equals(Intent.ACTION_VIEW) ||
            getIntent().getAction().equals(Intent.ACTION_WEB_SEARCH );
    }

    private void UpdateSpinnerGroup() {
        mGroupSpinner.setVisibility( mHasGroupCb.isChecked() ? View.VISIBLE : View.GONE );
    }
    private void UpdateSpinnerKeepTime() {
        mKeepTimeSpinner.setVisibility( mKeepTimeCB.isChecked() ? View.VISIBLE : View.GONE );
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_TAB, mTabHost.getCurrentTab());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if ( IsAdd() )
            PrefUtils.putInt( STATE_FEED_EDIT_LOAD_TYPE_ID, mLoadTypeRG.getCheckedRadioButtonId() );
        else if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            String url = mUrlEditText.getText().toString();
            ContentResolver cr = getContentResolver();

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID,
                        FeedColumns.URL + Constants.DB_ARG, new String[]{url}, null);

                if (cursor != null && cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.error_feed_url_exists);
                } else {
                    ContentValues values = new ContentValues();

                    if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME))
                        url = Constants.HTTP_SCHEME + url;

                    values.put(FeedColumns.URL, url);

                    String name = mNameEditText.getText().toString();


                    values.put(FeedColumns.NAME, name.trim().length() > 0 ? name : null);
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, mRetrieveFulltextCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryListCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.IS_AUTO_REFRESH, mIsAutoRefreshCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.IS_IMAGE_AUTO_LOAD, mIsAutoImageLoadCb.isChecked() ? 1 : 0);
                    values.put(FeedColumns.OPTIONS, getOptionsJsonString());
                    values.put(FeedColumns.FETCH_MODE, 0);
                    if ( mHasGroupCb.isChecked() && mGroupSpinner.getSelectedItemId() != AdapterView.INVALID_ROW_ID )
                        values.put(FeedColumns.GROUP_ID, mGroupSpinner.getSelectedItemId() );
                    else
                        values.putNull(FeedColumns.GROUP_ID);

                    values.putNull(FeedColumns.ERROR);

                    GeneralPrefsFragment.mSetupChanged = true;
                    cr.update(getIntent().getData(), values, null, null);

                }
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

        }

        super.onDestroy();
    }

    private String getOptionsJsonString() {
        JSONObject jsonOptions = new JSONObject();
        try {
            jsonOptions.put( "isRss", mLoadTypeRG.getCheckedRadioButtonId() == R.id.rbRss );
            if ( mKeepTimeCB.isChecked()  )
                jsonOptions.put( FetcherService.CUSTOM_KEEP_TIME, mKeepTimeValues[mKeepTimeSpinner.getSelectedItemPosition()] );
            else
                jsonOptions.remove( FetcherService.CUSTOM_KEEP_TIME );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonOptions.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_feed, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mTabHost.getCurrentTab() == 0) {
            menu.findItem(R.id.menu_add_filter).setVisible(false);
        } else {
            menu.findItem(R.id.menu_add_filter).setVisible(true);
        }

        boolean edit = getIntent() != null && getIntent().getAction().equals(Intent.ACTION_EDIT);
        boolean insert = getIntent() != null &&
                ( getIntent().getAction().equals(Intent.ACTION_INSERT ) ||
                  getIntent().getAction().equals(Intent.ACTION_VIEW ) ||
                  getIntent().getAction().equals(Intent.ACTION_SEND) );
        menu.findItem(R.id.menu_validate).setVisible(insert);
        menu.findItem(R.id.menu_delete_feed).setVisible(edit && mTabHost.getCurrentTab() == 0 );

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_validate: // only in insert mode
                Validate();
                return true;
            case R.id.menu_add_filter: {
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);

                new AlertDialog.Builder(this) //
                        .setTitle(R.string.filter_add_title) //
                        .setView(dialogView) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String filterText = ((EditText) dialogView.findViewById(R.id.filterText)).getText().toString();
                                if (filterText.length() != 0) {
                                    String feedId = getIntent().getData().getLastPathSegment();

                                    ContentValues values = new ContentValues();
                                    values.put(FilterColumns.FILTER_TEXT, filterText);
                                    values.put(FilterColumns.IS_REGEX, ((CheckBox) dialogView.findViewById(R.id.regexCheckBox)).isChecked());
                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, ((RadioButton) dialogView.findViewById(R.id.applyTitleRadio)).isChecked());
                                    values.put(FilterColumns.IS_ACCEPT_RULE, ((RadioButton) dialogView.findViewById(R.id.acceptRadio)).isChecked());
                                    values.put(FilterColumns.IS_MARK_STARRED, ((RadioButton) dialogView.findViewById(R.id.markAsStarredRadio)).isChecked());

                                    ContentResolver cr = getContentResolver();
                                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), values);
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();
                return true;
            }
            case R.id.menu_delete_feed:
                EditFeedsListFragment.DeleteFeed( this, getIntent().getData(), "" );
                return true;
            default:

                return super.onOptionsItemSelected(item);
        }
    }

    public void Validate() {
        final String urlOrSearch = mUrlEditText.getText().toString().trim();
        if (urlOrSearch.isEmpty()) {
            UiUtils.showMessage(EditFeedActivity.this, R.string.error_feed_error);
        }

        if ( mLoadTypeRG.getCheckedRadioButtonId() == R.id.rbWebPageSearch ) {
            PrefUtils.putString( STATE_WEB_SEARCH_TEXT, mUrlEditText.getText().toString() );
            GetWebSearchDuckDuckGoResultsLoader loader = new GetWebSearchDuckDuckGoResultsLoader(EditFeedActivity.this, urlOrSearch );
            AddFeedFromUserSelection("", getString(R.string.web_page_search_duckduckgo) + "\n" + loader.mUrl, loader );
        } else {
            final String name = mNameEditText.getText().toString().trim();
            if (!urlOrSearch.toLowerCase().contains("www") &&
                    (!urlOrSearch.contains(".") || !urlOrSearch.contains("/") || urlOrSearch.contains(" "))) {
                AddFeedFromUserSelection(name, getString(R.string.feed_search), new GetFeedSearchResultsLoader(EditFeedActivity.this, urlOrSearch));
            } else {
                AddFeedFromUserSelection(name, getString(R.string.feed_search), new GetSiteAlternateListLoader(EditFeedActivity.this, urlOrSearch));
            }
        }
    }

    private void AddFeedFromUserSelection(final String name, final String dialogCaption, final Loader<ArrayList<HashMap<String, String>>> loader) {
        final ProgressDialog pd = new ProgressDialog(EditFeedActivity.this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(true);
        pd.setIndeterminate(true);
        pd.show();

        getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<ArrayList<HashMap<String, String>>>() {

            @Override
            public Loader<ArrayList<HashMap<String, String>>> onCreateLoader(int id, Bundle args) {
                return loader;
            }

            @Override
            public void onLoadFinished(final Loader<ArrayList<HashMap<String, String>>> loader,
                                       final ArrayList<HashMap<String, String>> data) {
                pd.cancel();

                if (data == null) {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.error);
                } else if (data.isEmpty()) {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.no_result);
                } else if ( data.size() == 1 ) {
                    AddFeed(data.get(0));
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(EditFeedActivity.this);
                    //builder.setTitle(dialogCaption);
                    {
                        TextView textView = UiUtils.CreateSmallText(builder.getContext(),Gravity.CENTER, null, dialogCaption);
                        textView.setMaxLines( 3 );
                        builder.setCustomTitle( textView );
                    }
                    // create the grid item mapping
                    String[] from = new String[]{ITEM_TITLE, ITEM_DESC, ITEM_URL, ITEM_ICON};
                    int[] to = new int[]{R.id.search_item_title, R.id.search_item_descr, R.id.search_item_url, R.id.search_item_icon};

                    // fill in the grid_item layout
                    SimpleAdapter adapter = new SimpleAdapter(EditFeedActivity.this, data, R.layout.item_search_result, from, to);

                    adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Object data, String s) {
                            String value = (String) data;
                            if ( view instanceof TextView && value.startsWith(IS_READ_STUMB) ) {
                                String text = value;
                                ( ( TextView )view ).setTextColor(Color.DKGRAY);
                                text = text.replace( IS_READ_STUMB, "" );
                                ( ( TextView )view ).setText( text );
                                return true;
                            } else if ( view.getId() == R.id.search_item_title ) {
                                ( ( TextView )view ).setTextColor( EditFeedActivity.this.getResources().getColor( android.R.color.primary_text_dark ) );
                                return false;
                            } else if ( view.getId() == R.id.search_item_descr ) {
                                ( ( TextView )view ).setTextColor( EditFeedActivity.this.getResources().getColor( android.R.color.secondary_text_dark ) );
                                return false;
                            } else if ( view.getId() == R.id.search_item_url ) {
                                ( ( TextView )view ).setTextColor( EditFeedActivity.this.getResources().getColor( android.R.color.tertiary_text_dark ) );
                                return false;
                            } else if ( view.getId() == R.id.search_item_icon ) {
                                if (value != null) {
                                    Glide.with(EditFeedActivity.this ).load(value).centerCrop().into((ImageView) view);
                                } else {
                                    Glide.with(EditFeedActivity.this ).clear(view);
                                    (( ImageView)view ).setImageResource( R.drawable.cup_empty );
                                }
                                return true;
                            } else
                                return false;
                        }
                    });

                    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AddFeed(data.get(which));
                        }


                    });
                    builder.setOnCancelListener( new AlertDialog.OnCancelListener(){
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            mUserSelectionDialog = null;
                            PrefUtils.putBoolean( DIALOG_IS_SHOWN, false );
                        }
                    } );
                    if (Build.VERSION.SDK_INT >= 17 )
                        builder.setOnDismissListener( new AlertDialog.OnDismissListener(){
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                mUserSelectionDialog = null;
                            }
                        } );
                    PrefUtils.putBoolean( DIALOG_IS_SHOWN, true );
                    mUserSelectionDialog = builder.show();
                }
            }

            @Override
            public void onLoaderReset(Loader<ArrayList<HashMap<String, String>>> loader) {
            }

            private void AddFeed(HashMap<String, String> dataItem) {
                if ( mLoadTypeRG.getCheckedRadioButtonId() == R.id.rbWebPageSearch ) {
                    Intent intent = new Intent(EditFeedActivity.this, EntryActivity.class );
                    EditFeedActivity.this.startActivity(intent.setData( Uri.parse(dataItem.get(ITEM_URL)) ));
                    return;
                }
                Pair<Uri, Boolean> result =
                    FeedDataContentProvider.addFeed(EditFeedActivity.this,
                        dataItem.get(ITEM_URL),
                        name.isEmpty() ? dataItem.get(ITEM_TITLE) : name,
                        mHasGroupCb.isChecked() ? mGroupSpinner.getSelectedItemId() : null,
                        mRetrieveFulltextCb.isChecked(),
                        mShowTextInEntryListCb.isChecked(),
                        mIsAutoImageLoadCb.isChecked(),
                        getOptionsJsonString());
                if (result.second ) {
                    UiUtils.toast(EditFeedActivity.this, R.string.new_feed_was_added);

                    FetcherService.StartService(new Intent(EditFeedActivity.this, FetcherService.class)
                            .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                            .putExtra(Constants.FEED_ID, result.first.getLastPathSegment())
                            .putExtra(Constants.EXTRA_DELETE_OLD, false));
                    HomeActivity.mNewFeedUri = FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(result.first.getLastPathSegment());
                    setResult(RESULT_OK);
                    startActivity( new Intent( EditFeedActivity.this, HomeActivity.class )
                            .setAction(Intent.ACTION_MAIN)
                            .setData( HomeActivity.mNewFeedUri ) );

                }
                finish();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(this, FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                null, null, null, FilterColumns.IS_ACCEPT_RULE + Constants.DB_DESC);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFiltersCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFiltersCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
    }
}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
class GetFeedSearchResultsLoader extends BaseLoader<ArrayList<HashMap<String, String>>> {
    private String mSearchText;

    public GetFeedSearchResultsLoader(Context context, String searchText) {
        super(context);
        mSearchText = searchText;
        try {
            mSearchText = URLEncoder.encode(searchText, Constants.UTF8);
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    /**
     * This is where the bulk of our work is done. This function is called in a background thread and should generate a new set of data to be
     * published by the loader.
     */
    @Override
    public ArrayList<HashMap<String, String>> loadInBackground() {
        try {
            //    HttpURLConnection conn = NetworkUtils.setupConnection("https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q=" + mSearchText);
            Connection conn = new Connection("http://cloud.feedly.com/v3/search/feeds/?count=20&query=" + mSearchText);
            try {
                String jsonStr = new String(NetworkUtils.getBytes(conn.getInputStream()));
                //String jsonStr = "{\"results\":[{\"deliciousTags\":[\"история\",\"science\",\"education\",\"culture\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1.rss\",\"title\":\"Arzamas | Всё\",\"language\":\"ru\",\"lastUpdated\":1493701920000,\"subscribers\":1947,\"velocity\":3.5,\"website\":\"http://arzamas.academy/?utm_campaign=main_rss&utm_medium=rss&utm_source=rss_link\",\"score\":1947.0,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":1507,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"Новые курсы каждый четверг и дополнительные материалы в течение недели\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_cover-15b8675fdfa\",\"iconUrl\":\"http://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_icon-15414d05afc\",\"partial\":false,\"visualUrl\":\"http://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_visual-15414d05afc\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"magazines\",\"история\",\"журналы\",\"блоги\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1/mag.rss\",\"title\":\"Arzamas | Журнал\",\"language\":\"ru\",\"lastUpdated\":1493701980000,\"subscribers\":249,\"velocity\":1.2,\"website\":\"http://arzamas.academy/mag\",\"score\":1871.93,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":1390,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"??????? ???????? ? ?????, ??????? ? ???????????\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_cover-15b8e8b3d47\",\"iconUrl\":\"http://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_icon-1542d1e8523\",\"partial\":false,\"visualUrl\":\"http://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_visual-1542d1e8523\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"science\",\"история\",\"образование\",\"education\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1/courses.rss\",\"title\":\"Arzamas | Курсы\",\"language\":\"ru\",\"lastUpdated\":1435214100000,\"subscribers\":245,\"velocity\":0.01,\"website\":\"http://arzamas.academy/courses?utm_campaign=courses_rss&utm_medium=rss&utm_source=rss_link\",\"score\":24.5,\"coverage\":0.0,\"coverageScore\":0.0,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"Новые курсы каждый четверг\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_cover-15b870310a2\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_icon-15b870310a2\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_visual-15b870310a2\",\"coverColor\":\"000000\",\"art\":0.0},{\"feedId\":\"feed/http://arzamas.academy/feed_v1/podcast.rss\",\"title\":\"История культуры | Курсы | Arzamas\",\"language\":\"ru\",\"lastUpdated\":1492671600000,\"subscribers\":17,\"velocity\":10.5,\"website\":\"http://arzamas.academy/courses?utm_campaign=episodes_podcast_rss&utm_medium=rss&utm_source=rss_link\",\"score\":17.0,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":366,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"audio\",\"description\":\"Arzamas — это просветительский проект, посвященный гуманитарному знанию. В основе Arzamas лежат курсы, или «гуманитарные сериалы», — каждый на свою тему. Мы записываем выдающихся ученых-гуманитариев и снабжаем их лекции дополнительными материалами. Получается своебразный университет, каждую неделю, по четвергам, выпускающий новый курс по истории, литературе, искусству, антропологии, философии — о культуре и человеке. В этом подкасте представлены аудио-версии наших курсов. Полные версии доступы на нашем сайте (http://arzamas.academy).\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_cover-15b99c01c32\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_icon-15b99c01c32\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_visual-15b99c01c32\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"youtube\"],\"feedId\":\"feed/https://www.youtube.com/playlist?list=UUVgvnGSFU41kIhEc09aztEg\",\"title\":\"Arzamas (uploads) on YouTube\",\"language\":\"ru\",\"lastUpdated\":1491832800000,\"subscribers\":28,\"velocity\":1.2,\"website\":\"https://youtube.com/playlist?list=UUVgvnGSFU41kIhEc09aztEg\",\"score\":152.78,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":149,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"video\",\"description\":\"Официальный канал Arzamas.academy\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/jGwxZEqqmeCambMrgz-PZPVTczhBEEZ48IdO6thk3Ww_sicon-15b6534aa74\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/jGwxZEqqmeCambMrgz-PZPVTczhBEEZ48IdO6thk3Ww_svisual-15b6534aa74\",\"art\":0.0}],\"queryType\":\"term\",\"related\":[\"история\"],\"scheme\":\"subs.0\"}";

                // Parse results
                final ArrayList<HashMap<String, String>> results = new ArrayList<>();
                JSONArray entries = new JSONObject(jsonStr).getJSONArray("results");
                for (int i = 0; i < entries.length(); i++) {
                    try {
                        JSONObject entry = (JSONObject) entries.get(i);
                        String url = entry.get(ITEM_URL).toString().replaceFirst("feed/http", "http"  );
                        if (!url.isEmpty()) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put(ITEM_TITLE, Html.fromHtml(entry.get(ITEM_TITLE).toString())
                                    .toString());
                            map.put(ITEM_URL, url);
                            map.put(ITEM_DESC, Html.fromHtml(entry.get(ITEM_DESC).toString()).toString());

                            results.add(map);
                        }
                    } catch (Exception ignored) {
                    }
                }

                return results;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Dog.e("Error", e);
            return null;
        }
    }
}

class GetSiteAlternateListLoader extends BaseLoader<ArrayList<HashMap<String, String>>> {
    private final String mUrl;

    public GetSiteAlternateListLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    /**
     * This is where the bulk of our work is done. This function is called in a background thread and should generate a new set of data to be
     * published by the loader.
     */
    @Override
    public ArrayList<HashMap<String, String>> loadInBackground() {
        final ArrayList<HashMap<String, String>> results = new ArrayList<>();

        try {
            Connection conn = new Connection(mUrl);
            try {
                String content = new String(NetworkUtils.getBytes(conn.getInputStream()));

                final Pattern TITLE_PATTERN = Pattern.compile("title=\"(.+?)\"", Pattern.CASE_INSENSITIVE);
                final Pattern HREF_PATTERN = Pattern.compile("href=\"(.+?)\"", Pattern.CASE_INSENSITIVE);

                final Matcher matcher = FetcherService.FEED_LINK_PATTERN.matcher(content);
                //final String HREF = "href=\"";

                while (matcher.find()) { // not "while" as only one link is needed
                    final String line = matcher.group();
                    Matcher urlMatcher = HREF_PATTERN.matcher( line );
                    if ( urlMatcher.find() ) {
                        String url = urlMatcher.group( 1 );
                        if ( !url.toLowerCase().contains( "rss" ) &&
                             !url.toLowerCase().contains( "feed" ) &&
                             !url.toLowerCase().contains( "atom" ) )
                            continue;
                        if (url.startsWith(Constants.SLASH)) {
                            int index = mUrl.indexOf('/', 8);
                            if (index > -1) {
                                url = mUrl.substring(0, index) + url;
                            } else {
                                url = mUrl + url;
                            }
                        } else if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                            url = mUrl + '/' + url;
                        }
                        final Matcher titleMatcher = TITLE_PATTERN.matcher( line );
                        final String title = titleMatcher.find() ?  titleMatcher.group( 1 ) : url;
                        HashMap<String, String> map = new HashMap<>();
                        map.put(ITEM_TITLE, title);
                        map.put(ITEM_DESC, url);
                        map.put(ITEM_URL, url);
                        results.add(map);
                    }
                }
                if ( results.isEmpty() ) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put(ITEM_TITLE, "");
                    map.put(ITEM_URL, mUrl);
                    results.add(map);
                }
                return results;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Dog.e("Error", e);
            return null;
        }
    }

}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
class GetWebSearchDuckDuckGoResultsLoader extends BaseLoader<ArrayList<HashMap<String, String>>> {
    private static final String CLASS_ATTRIBUTE = "result__snippet";
    final String mUrl;
    private String mSearchText;

    public GetWebSearchDuckDuckGoResultsLoader(Context context, String searchText) {
        super(context);
        mSearchText = searchText;
        try {
            mSearchText = URLEncoder.encode(searchText, Constants.UTF8);
        } catch (UnsupportedEncodingException ignored) {
        }
        mUrl = EditFeedActivity.DUCKDUCKGO_SEARCH_URL + mSearchText + "&kl=" + Locale.getDefault().getLanguage();
    }

    @Override
    public ArrayList<HashMap<String, String>> loadInBackground() {
        try {
            Connection conn = new Connection(mUrl) ;
            try {
                final ArrayList<HashMap<String, String>> results = new ArrayList<>();
                Document doc = conn.getParse();
                for (Element el : doc.getElementsByClass( "results_links")) {
                    try {
                        final String title = el.getElementsByClass( "result__title" ).text();
                        String url = el.getElementsByClass( "result__title" ).first().getElementsByTag( "a" ).first().attr( "href" );
                        url = URLDecoder.decode( url.substring( url.indexOf( "http" ) ) );
                        final String descr = el.getElementsByClass( "result__snippet" ).text();
                        String icon = el.getElementsByClass( "result__icon__img" ).first().attr( "src" );
                        if ( !icon.startsWith( "https:" ) )
                            icon = "https:" + icon;
                        HashMap<String, String> map = new HashMap<>();
                        final String read = ( FetcherService.GetEnryUri(url ) != null ? IS_READ_STUMB : "" );
                        map.put(ITEM_TITLE, read + title );
                        map.put(ITEM_URL, read + url);
                        map.put(ITEM_DESC, read + descr);
                        map.put(ITEM_ICON, icon);
                        results.add(map);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return results;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Dog.e("Error", e);
            return null;
        }
    }


}
