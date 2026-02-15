/**
 * Flym
 *
 *
 * Copyright (c) 2012-2015 Frederic Julian
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 *
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.yanus171.feedexfork.activity

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import org.json.JSONException
import org.json.JSONObject
import ru.yanus171.feedexfork.Constants
import ru.yanus171.feedexfork.Constants.HTTPS_SCHEME
import ru.yanus171.feedexfork.Constants.HTTP_SCHEME
import ru.yanus171.feedexfork.MainApplication.getContext
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.adapter.FiltersCursorAdapter
import ru.yanus171.feedexfork.fragment.EditFeedsListFragment
import ru.yanus171.feedexfork.fragment.EntryFragment.NEW_TASK_EXTRA
import ru.yanus171.feedexfork.fragment.GeneralPrefsFragment
import ru.yanus171.feedexfork.loader.BaseLoader
import ru.yanus171.feedexfork.parser.*
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns
import ru.yanus171.feedexfork.provider.FeedDataContentProvider
import ru.yanus171.feedexfork.service.FetcherService
import ru.yanus171.feedexfork.utils.*
import ru.yanus171.feedexfork.utils.HtmlUtils.unescapeTitle
import ru.yanus171.feedexfork.view.AppSelectPreference.GetShowInBrowserIntent
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern

@Suppress("DEPRECATION")
open class EditFeedActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var mKeepTimeValues: Array<String>
    private lateinit var mRefreshIntervalValues: Array<String>
    private val mFilterActionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.edit_context_menu, menu)
            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_edit -> {
                    FilterEdit( getContext(), intent.data!!.lastPathSegment!! ).edit( mFiltersCursorAdapter)
                    mode.finish() // Action picked, so close the CAB
                    true
                }
                R.id.menu_delete -> {
                    val filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.selectedFilter)
                    AlertDialog.Builder(this@EditFeedActivity) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle(R.string.filter_delete_title) //
                            .setMessage(R.string.question_delete_filter) //
                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                object : Thread() {
                                    override fun run() {
                                        val cr = contentResolver
                                        if (cr.delete(FilterColumns.CONTENT_URI, FilterColumns._ID + '=' + filterId, null) > 0) {
                                            cr.notifyChange(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(intent.data!!.lastPathSegment),
                                                    null)
                                        }
                                    }
                                }.start()
                            }.setNegativeButton(android.R.string.no, null).show()
                    mode.finish() // Action picked, so close the CAB
                    true
                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: ActionMode) {
            mFiltersCursorAdapter.selectedFilter = -1
            mFiltersListView.invalidateViews()
        }
    }
    private lateinit var mKeepTimeSpinner: Spinner
    private lateinit var mKeepTimeCB: CheckBox
    private lateinit var mRefreshIntervalSpinner: Spinner
    private lateinit var mRefreshIntervalCB: CheckBox
    private var mUserSelectionDialog: AlertDialog? = null
    private lateinit var mOneWebPageArticleClassName: EditText
    private lateinit var mOneWebPageUrlClassName: EditText
    private lateinit var mOneWebPageTextClassName: EditText
    private lateinit var mOneWebPageAuthorClassName: EditText
    private lateinit var mOneWebPageDateClassName: EditText
    private lateinit var mOneWebPageImageUrlClassName: EditText
    private lateinit var mNextPageClassName: EditText
    private lateinit var mNextPageMaxCount: EditText
    private lateinit var mOneWebPageLayout: LinearLayout
    private lateinit var mIsAutoSetAsRead: CheckBox

    private lateinit var mTabHost: TabHost
    private lateinit var mNameEditText: EditText
    private lateinit var mUrlEditText: EditText
    private lateinit var mRetrieveFulltextCb: CheckBox
    private lateinit var mShowTextInEntryListCb: CheckBox
    private lateinit var mIsAutoRefreshCb: CheckBox
    private lateinit var mIsAutoImageLoadCb: CheckBox
    private lateinit var mFiltersListView: ListView
    private lateinit var mGroupSpinner: Spinner
    private lateinit var mHasGroupCb: CheckBox
    private lateinit var mFiltersCursorAdapter: FiltersCursorAdapter
    private lateinit var mLoadTypeRG: RadioGroup

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_edit)
        SetupFont(findViewById(R.id.root))
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setResult(RESULT_CANCELED)
        val intent = intent
        mTabHost = findViewById(R.id.tabHost)
        mNameEditText = findViewById(R.id.feed_title)
        mUrlEditText = findViewById(R.id.feed_url)
        mRetrieveFulltextCb = SetupSmallTextView(R.id.retrieve_fulltext) as CheckBox
        mShowTextInEntryListCb = SetupSmallTextView(R.id.show_text_in_entry_list) as CheckBox
        mIsAutoRefreshCb = SetupSmallTextView(R.id.auto_refresh) as CheckBox
        mIsAutoImageLoadCb = SetupSmallTextView(R.id.auto_image_load) as CheckBox
        mIsAutoSetAsRead = SetupSmallTextView(R.id.auto_set_as_read) as CheckBox
        mIsAutoSetAsRead.isChecked = false
        mFiltersListView = findViewById(android.R.id.list)
        mGroupSpinner = findViewById(R.id.spin_group)

        mKeepTimeCB = SetupSmallTextView(R.id.cbCustomKeepTime) as CheckBox
        mKeepTimeCB.setOnCheckedChangeListener { _, _ -> UpdateSpinnerKeepTime() }
        mKeepTimeValues = resources.getStringArray(R.array.settings_keep_time_values)
        mKeepTimeSpinner = findViewById(R.id.spin_keeptime)
        mKeepTimeSpinner.setSelection(3)
        for (i in mKeepTimeValues.indices) if (mKeepTimeValues[i].toDouble() == FetcherService.GetDefaultKeepTime().toDouble()) {
            mKeepTimeSpinner.setSelection(i)
            break
        }

        mRefreshIntervalCB = SetupSmallTextView(R.id.cbCustomRefreshInterval) as CheckBox
        mRefreshIntervalCB.setOnCheckedChangeListener { _, _ -> UpdateSpinnerRefreshInterval() }
        mRefreshIntervalValues = resources.getStringArray(R.array.settings_interval_values)
        mRefreshIntervalSpinner = findViewById(R.id.spin_RefreshInterval)
        mRefreshIntervalSpinner.setSelection(4)
        for (i in mRefreshIntervalValues.indices) if (mRefreshIntervalValues[i].toDouble() == FetcherService.GetDefaultRefreshInterval().toDouble()) {
            mRefreshIntervalSpinner.setSelection(i)
            break
        }

        mHasGroupCb = SetupSmallTextView(R.id.has_group) as CheckBox
        mHasGroupCb.setOnCheckedChangeListener { buttonView, isChecked -> UpdateSpinnerGroup() }
        mLoadTypeRG = findViewById(R.id.rgLoadType)
        val tabWidget = findViewById<View>(android.R.id.tabs)
        mIsAutoRefreshCb.setOnCheckedChangeListener { compoundButton, b -> mIsAutoImageLoadCb.isEnabled = b }
        mIsAutoRefreshCb.isChecked = false
        mIsAutoRefreshCb.visibility = if (PrefUtils.getBoolean(PrefUtils.REFRESH_ONLY_SELECTED, false)) View.VISIBLE else View.GONE
        mLoadTypeRG.setOnCheckedChangeListener { _, _ -> ShowControls() }
        val isExternal = this !is ArticleWebSearchActivity && intent.data != null && intent.data!!.lastPathSegment == FetcherService.GetExtrenalLinkFeedID()
        mTabHost.setup()
        mTabHost.addTab(mTabHost.newTabSpec("feedTab").setIndicator(getString(R.string.tab_feed_title)).setContent(R.id.feed_tab))
        mTabHost.addTab(mTabHost.newTabSpec("filtersTab").setIndicator(if (isExternal) "" else getString( R.string.tab_filters_title) ).setContent(R.id.filters_tab))
        SetupFont(findViewById(R.id.feed_tab))
        mTabHost.setOnTabChangedListener { invalidateOptionsMenu() }
        if (savedInstanceState != null) {
            mTabHost.currentTab = savedInstanceState.getInt(STATE_CURRENT_TAB)
        }
        val adapter: ResourceCursorAdapter = object : ResourceCursorAdapter(this,
                android.R.layout.simple_spinner_item,
                contentResolver.query(FeedColumns.GROUPS_CONTENT_URI, arrayOf(FeedColumns._ID, FeedColumns.NAME), null, null, FeedColumns.NAME),
                0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                val nameTextView = view.findViewById<TextView>(android.R.id.text1)
                nameTextView.text = cursor.getString(1)
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mGroupSpinner.adapter = adapter
        mIsAutoImageLoadCb.visibility = if (PrefUtils.getPreloadImagesMode().equals(Constants.FETCH_PICTURE_MODE_NEVER )) View.GONE else View.VISIBLE
        PrefUtils.putBoolean(DIALOG_IS_SHOWN, false)
        mNextPageClassName = findViewById(R.id.next_page_classname)
        mNextPageMaxCount = findViewById(R.id.next_page_max_count)
        mOneWebPageLayout = findViewById(R.id.one_webpage_layout)
        mOneWebPageArticleClassName = findViewById(R.id.one_webpage_article_classname)
        mOneWebPageAuthorClassName = findViewById(R.id.one_webpage_author_classname)
        mOneWebPageDateClassName = findViewById(R.id.one_webpage_date_classname)
        mOneWebPageImageUrlClassName = findViewById(R.id.one_webpage_image_url_classname)
        mOneWebPageTextClassName = findViewById(R.id.one_webpage_text_classname)
        mOneWebPageUrlClassName = findViewById(R.id.one_webpage_url_classname)
        setTitle(R.string.new_feed_title)
        if ( this is ArticleWebSearchActivity ) {
            findViewById<TextView>(R.id.url_textview).text = getString(R.string.web_search_keyword);
            setTitle(R.string.web_search_title)
            SetTaskTitle(getString(R.string.web_search_title))
        }
        tabWidget.visibility = View.GONE
        if (Intent.ACTION_INSERT == intent.action) {
            mHasGroupCb.isChecked = false
            mIsAutoImageLoadCb.isChecked = true
            mKeepTimeCB.isChecked = false
            mRefreshIntervalCB.isChecked = false
            mLoadTypeRG.check(PrefUtils.getInt(STATE_LAST_LOAD_TYPE, R.id.rbRss))
        } else if (Intent.ACTION_SEND == intent.action || Intent.ACTION_VIEW == intent.action) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) mUrlEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT)) else if (intent.dataString != null) mUrlEditText.setText(intent.dataString)
            mLoadTypeRG.check(R.id.rbRss)
        } else if (Intent.ACTION_WEB_SEARCH == intent.action) {
            if (intent.hasExtra(SearchManager.QUERY)) mUrlEditText.setText(intent.getStringExtra(SearchManager.QUERY))
        } else if (Intent.ACTION_EDIT == intent.action) {
            setTitle(if (isExternal) R.string.global_filter else R.string.edit_feed_title)
            tabWidget.visibility = View.VISIBLE
            mFiltersCursorAdapter = FiltersCursorAdapter(this, Constants.EMPTY_CURSOR)
            mFiltersListView.setAdapter(mFiltersCursorAdapter)
            mFiltersListView.setOnItemLongClickListener { _, _, position, _ ->
                startSupportActionMode(mFilterActionModeCallback)
                mFiltersCursorAdapter.selectedFilter = position
                mFiltersListView.invalidateViews()
                true
            }
            mFiltersListView.setOnItemClickListener { _, _, position, _ ->
                mFiltersCursorAdapter.selectedFilter = position
                mFiltersListView.invalidateViews()
                FilterEdit( this, intent.data!!.lastPathSegment!! ).edit( mFiltersCursorAdapter)
                //return true;
            }
            loaderManager.initLoader(0, Bundle(), this)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            if (savedInstanceState == null) {
                val cursor = contentResolver.query(intent.data!!, FEED_PROJECTION, null, null, null)
                if (cursor != null && cursor.moveToNext()) {
                    mNameEditText.setText(cursor.getString(0))
                    mUrlEditText.setText(cursor.getString(1))
                    mRetrieveFulltextCb.isChecked = cursor.getInt(2) == 1
                    mShowTextInEntryListCb.isChecked = cursor.getInt(4) == 1
                    mIsAutoRefreshCb.isChecked = cursor.getInt(5) == 1
                    mIsAutoImageLoadCb.isChecked = cursor.isNull(7) || cursor.getInt(7) == 1
                    mGroupSpinner.setSelection(-1)
                    //mKeepTimeSpinner.setSelection( -1);
                    mHasGroupCb.isChecked = !cursor.isNull(6)
                    UpdateSpinnerGroup()
                    if (!cursor.isNull(6)) for (i in 0 until mGroupSpinner.getCount()) if (mGroupSpinner.getItemIdAtPosition(i) == cursor.getInt(6).toLong()) {
                        mGroupSpinner.setSelection(i)
                        break
                    }
                    try {
                        var jsonOptions = JSONObject()
                        try {
                            jsonOptions = JSONObject(cursor.getString(cursor.getColumnIndex(FeedColumns.OPTIONS)))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val isRss = !jsonOptions.has(FetcherService.IS_RSS) || jsonOptions.getBoolean(FetcherService.IS_RSS)
                        val isOneWebPage = jsonOptions.has(FetcherService.IS_ONE_WEB_PAGE) && jsonOptions.getBoolean(FetcherService.IS_ONE_WEB_PAGE)
                        if (isRss) mLoadTypeRG.check(R.id.rbRss) else if (isOneWebPage) mLoadTypeRG.check(R.id.rbOneWebPage) else mLoadTypeRG.check(R.id.rbWebLinks)
                        mKeepTimeCB.isChecked = jsonOptions.has(FetcherService.CUSTOM_KEEP_TIME)
                        UpdateSpinnerKeepTime()
                        if (mKeepTimeCB.isChecked) {
                            for (i in mKeepTimeValues.indices) if (mKeepTimeValues[i].toDouble() == jsonOptions.getDouble(FetcherService.CUSTOM_KEEP_TIME)) {
                                mKeepTimeSpinner.setSelection(i)
                                break
                            }
                        }
                        mRefreshIntervalCB.isChecked = jsonOptions.has(FetcherService.CUSTOM_REFRESH_INTERVAL)
                        UpdateSpinnerRefreshInterval()
                        if (mRefreshIntervalCB.isChecked) {
                            for (i in mRefreshIntervalValues.indices) if (mRefreshIntervalValues[i].toLong() == jsonOptions.getLong(FetcherService.CUSTOM_REFRESH_INTERVAL)) {
                                mRefreshIntervalSpinner.setSelection(i)
                                break
                            }
                        }
                        if (jsonOptions.has(AUTO_SET_AS_READ) && jsonOptions.getBoolean(AUTO_SET_AS_READ)) mIsAutoSetAsRead.isChecked = true
                        mNextPageClassName.setText(jsonOptions.getString(FetcherService.NEXT_PAGE_URL_CLASS_NAME))
                        mNextPageMaxCount.setText(jsonOptions.getString(FetcherService.NEXT_PAGE_MAX_COUNT))
                        if (jsonOptions.has(FetcherService.IS_ONE_WEB_PAGE) && jsonOptions.getBoolean(FetcherService.IS_ONE_WEB_PAGE)) {
                            mOneWebPageArticleClassName.setText(jsonOptions.getString(ONE_WEB_PAGE_ARTICLE_CLASS_NAME))
                            mOneWebPageTextClassName.setText(jsonOptions.getString(ONE_WEB_PAGE_TEXT_CLASS_NAME))
                            mOneWebPageImageUrlClassName.setText(jsonOptions.getString(ONE_WEB_PAGE_IAMGE_URL_CLASS_NAME))
                            mOneWebPageDateClassName.setText(jsonOptions.getString(ONE_WEB_PAGE_DATE_CLASS_NAME))
                            mOneWebPageAuthorClassName.setText(jsonOptions.getString(ONE_WEB_PAGE_AUTHOR_CLASS_NAME))
                            mOneWebPageUrlClassName.setText(jsonOptions.getString(ONE_WEB_PAGE_URL_CLASS_NAME))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    //mKeepTimeSpinner.setVisibility( mKeepTimeCB.isChecked() ? View.VISIBLE : View.GONE );
                    if (cursor.getInt(3) == 1) { // if it's a group, we cannot edit it
                        finish()
                    }
                } else {
                    UiUtils.showMessage(this@EditFeedActivity, R.string.error)
                    finish()
                }
                cursor?.close()
            }
        }
        mUrlEditText.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handled = true
                Validate()
            }
            handled
        }
        ShowControls()
        //findViewById( R.id.brightnessSlider ).setVisibility( View.GONE );
        if (intent.hasExtra(SearchManager.QUERY))
            Validate()
        else if ( IsAdd() )
            checkIfTelegram()
        if (PrefUtils.getBoolean("setting_edit_feed_force_portrait_orientation", false)) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        if ( isExternal ) {
            mTabHost.tabWidget.getChildTabViewAt(0).visibility = View.GONE;
            mTabHost.currentTab = 1
            val text = findViewById<TextView>(R.id.filter_hint).text.toString()
            findViewById<TextView>(R.id.filter_hint).text = text + "\n" + getString( R.string.filter_hint_external )
        }

    }
    @SuppressLint("SetTextI18n")
    private fun checkIfTelegram() {
        val url = mUrlEditText.text.toString()
        val m = Pattern.compile("((t.me)|(telegram.im))\\/([^\\/]+)").matcher(url)
        if ( m.find() ) {
            val name = m.group(4)
            Theme.CreateDialog(this)
                    .setMessage(R.string.addFeedTelegramPatternFound)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.yes) { dialog, _ ->
                        mLoadTypeRG.check(R.id.rbOneWebPage)
                        mUrlEditText.setText("https://t.me/s/$name")
                        mNameEditText.setText(name)
                        val BASE = "tgme_widget_message"
                        mOneWebPageArticleClassName.setText("${BASE}_wrap")
                        mOneWebPageUrlClassName.setText(BASE)
                        mOneWebPageTextClassName.setText("${BASE}_bubble")
                        mOneWebPageDateClassName.setText("${BASE}_date")
                        mOneWebPageAuthorClassName.setText("${BASE}_forwarded")

                        mIsAutoImageLoadCb.isChecked = true
                        mIsAutoSetAsRead.isChecked = true

                        Toast.makeText(this@EditFeedActivity, R.string.feedWasAutoConfigured, Toast.LENGTH_LONG).show()
                        dialog.dismiss()
            }.create().show()
        }
    }
    override fun onResume() {
        super.onResume()
        if ( this is ArticleWebSearchActivity ) mUrlEditText.setText(PrefUtils.getString(STATE_WEB_SEARCH_TEXT, ""))
        if (IsAdd()) {
            mUrlEditText.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        if (mUserSelectionDialog?.isShowing == true) mUserSelectionDialog?.dismiss()
        if (PrefUtils.getBoolean(DIALOG_IS_SHOWN, false) &&
                this is ArticleWebSearchActivity ) {
            val urlOrSearch = mUrlEditText.text.toString().trim { it <= ' ' }
            val loader = GetWebSearchDuckDuckGoResultsLoader(this@EditFeedActivity, urlOrSearch)
            AddFeedFromUserSelection("", "${getString(R.string.web_page_search_duckduckgo)}\n${loader.mUrl}".trimIndent(), loader)
        }
    }

    private fun ShowControls() {
        val i = mLoadTypeRG.checkedRadioButtonId
        val isRss = i == R.id.rbRss
        val isOneWebPage = i == R.id.rbOneWebPage
        val isWebLinks = i == R.id.rbWebLinks
        val isWebPageSearch = this is ArticleWebSearchActivity
        mRetrieveFulltextCb.isEnabled = (isRss || isOneWebPage) && !isWebPageSearch
        findViewById<LinearLayout>(R.id.feed_edit_controls).visibility = if (isWebPageSearch) View.GONE else View.VISIBLE
        findViewById<View>(R.id.layout_next_page).visibility = if (isRss) View.GONE else View.VISIBLE
        //window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        UpdateSpinnerGroup()
        UpdateSpinnerKeepTime()
        mOneWebPageLayout.visibility = if (isOneWebPage) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layout_next_page).visibility = if (isWebLinks || isOneWebPage) View.VISIBLE else View.GONE
    }

    private fun IsAdd(): Boolean {
        return intent.action == Intent.ACTION_INSERT || intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_WEB_SEARCH
    }

    private fun UpdateSpinnerGroup() {
        mGroupSpinner.visibility = if (mHasGroupCb.isChecked) View.VISIBLE else View.GONE
    }

    private fun UpdateSpinnerKeepTime() {
        mKeepTimeSpinner.visibility = if (mKeepTimeCB.isChecked) View.VISIBLE else View.GONE
    }

    private fun UpdateSpinnerRefreshInterval() {
        mRefreshIntervalSpinner.visibility = if (mRefreshIntervalCB.isChecked) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_CURRENT_TAB, mTabHost.currentTab)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (IsAdd())
            PrefUtils.putInt(STATE_FEED_EDIT_LOAD_TYPE_ID, mLoadTypeRG.checkedRadioButtonId)
        else if (intent.action == Intent.ACTION_EDIT) {
            var url = mUrlEditText.text.toString()
            val cr = contentResolver
            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID,
                        FeedColumns.URL + Constants.DB_ARG, arrayOf(url), null)
                if (cursor != null && cursor.moveToFirst() && intent.data!!.lastPathSegment != cursor.getString(0)) {
                    UiUtils.showMessage(this@EditFeedActivity, R.string.error_feed_url_exists)
                } else {
                    val values = ContentValues()
                    if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) url = Constants.HTTP_SCHEME + url
                    values.put(FeedColumns.URL, url)
                    val name = mNameEditText.text.toString()
                    values.put(FeedColumns.NAME, if (name.trim { it <= ' ' }.isNotEmpty()) name else null)
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, if (mRetrieveFulltextCb.isChecked) 1 else null)
                    values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, if (mShowTextInEntryListCb.isChecked) 1 else null)
                    values.put(FeedColumns.IS_AUTO_REFRESH, if (mIsAutoRefreshCb.isChecked) 1 else null)
                    values.put(FeedColumns.IS_IMAGE_AUTO_LOAD, if (mIsAutoImageLoadCb.isChecked) 1 else 0)
                    values.put(FeedColumns.OPTIONS, optionsJsonString)
                    values.put(FeedColumns.FETCH_MODE, 0)
                    values.put(FeedColumns.REAL_LAST_UPDATE, 0)
                    if (mHasGroupCb.isChecked && mGroupSpinner.selectedItemId != AdapterView.INVALID_ROW_ID) values.put(FeedColumns.GROUP_ID, mGroupSpinner.selectedItemId) else values.putNull(FeedColumns.GROUP_ID)
                    values.putNull(FeedColumns.ERROR)
                    GeneralPrefsFragment.SetupChanged()
                    cr.update(intent.data!!, values, null, null)
                }
            } catch (ignored: Exception) {
            } finally {
                cursor?.close()
            }
        }
        super.onDestroy()
    }

    private val optionsJsonString: String
        get() {
            val jsonOptions = JSONObject()
            try {
                jsonOptions.put("isRss", mLoadTypeRG.checkedRadioButtonId == R.id.rbRss)
                jsonOptions.put(FetcherService.IS_ONE_WEB_PAGE, mLoadTypeRG.checkedRadioButtonId == R.id.rbOneWebPage)
                jsonOptions.put(FetcherService.NEXT_PAGE_URL_CLASS_NAME, mNextPageClassName.text.toString())
                jsonOptions.put(FetcherService.NEXT_PAGE_MAX_COUNT, mNextPageMaxCount.text.toString())
                jsonOptions.put(AUTO_SET_AS_READ, mIsAutoSetAsRead.isChecked)
                if (mLoadTypeRG.checkedRadioButtonId == R.id.rbOneWebPage) {
                    jsonOptions.put(ONE_WEB_PAGE_ARTICLE_CLASS_NAME, mOneWebPageArticleClassName.text.toString())
                    jsonOptions.put(ONE_WEB_PAGE_URL_CLASS_NAME, mOneWebPageUrlClassName.text.toString())
                    jsonOptions.put(ONE_WEB_PAGE_AUTHOR_CLASS_NAME, mOneWebPageAuthorClassName.text.toString())
                    jsonOptions.put(ONE_WEB_PAGE_DATE_CLASS_NAME, mOneWebPageDateClassName.text.toString())
                    jsonOptions.put(ONE_WEB_PAGE_IAMGE_URL_CLASS_NAME, mOneWebPageImageUrlClassName.text.toString())
                    jsonOptions.put(ONE_WEB_PAGE_TEXT_CLASS_NAME, mOneWebPageTextClassName.text.toString())
                }
                if (mKeepTimeCB.isChecked)
                    jsonOptions.put(FetcherService.CUSTOM_KEEP_TIME, mKeepTimeValues[mKeepTimeSpinner.selectedItemPosition]) else jsonOptions.remove(FetcherService.CUSTOM_KEEP_TIME)
                if (mRefreshIntervalCB.isChecked)
                    jsonOptions.put(FetcherService.CUSTOM_REFRESH_INTERVAL, mRefreshIntervalValues[mRefreshIntervalSpinner.selectedItemPosition]) else jsonOptions.remove(FetcherService.CUSTOM_REFRESH_INTERVAL)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return jsonOptions.toString()
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit_feed, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (mTabHost.currentTab == 0) {
            menu.findItem(R.id.menu_add_filter).isVisible = false
        } else {
            menu.findItem(R.id.menu_add_filter).isVisible = true
        }
        val edit = intent != null && intent.action == Intent.ACTION_EDIT
        val insert = intent != null &&
                (intent.action == Intent.ACTION_INSERT || intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND)
        menu.findItem(R.id.menu_validate).isVisible = insert
        menu.findItem(R.id.menu_delete_feed).isVisible = edit && mTabHost.currentTab == 0
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_validate -> {
                Validate()
                true
            }
            R.id.menu_add_filter -> {
                FilterEdit( this, intent.data!!.lastPathSegment!! ).add()
                true
            }
            R.id.menu_delete_feed -> {
                EditFeedsListFragment.DeleteFeed(this, intent.data, "")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun Validate() {
        val urlOrSearch = mUrlEditText.text.toString().trim { it <= ' ' }
        if (urlOrSearch.isEmpty()) {
            UiUtils.showMessage(this@EditFeedActivity, R.string.error_feed_error)
        }
        if (IsAdd()) PrefUtils.putInt(STATE_LAST_LOAD_TYPE, mLoadTypeRG.checkedRadioButtonId)
        if ( this is ArticleWebSearchActivity ) {
            PrefUtils.putString(STATE_WEB_SEARCH_TEXT, mUrlEditText.text.toString())
            if ( urlOrSearch.startsWith( HTTP_SCHEME ) || urlOrSearch.startsWith( HTTPS_SCHEME ) ) {
                intent = Intent(this@EditFeedActivity, EntryActivity::class.java)
                intent.data = Uri.parse(urlOrSearch)
                intent.putExtra( NEW_TASK_EXTRA, true )
                this@EditFeedActivity.startActivity(intent)
            } else {
                val loader = GetWebSearchDuckDuckGoResultsLoader(this@EditFeedActivity, urlOrSearch)
                AddFeedFromUserSelection("", "${getString(R.string.web_page_search_duckduckgo)}\n${loader.mUrl}".trimIndent(), loader)
            }
        } else {
            val name = mNameEditText.text.toString().trim { it <= ' ' }
            if (!urlOrSearch.toLowerCase().contains("www") &&
                    (!urlOrSearch.contains(".") || !urlOrSearch.contains("/") || urlOrSearch.contains(" "))) {
                AddFeedFromUserSelection(name, getString(R.string.feed_search), GetFeedSearchResultsLoader(this@EditFeedActivity, urlOrSearch))
            } else {
                AddFeedFromUserSelection(name, getString(R.string.feed_search), GetSiteAlternateListLoader(this@EditFeedActivity, urlOrSearch))
            }
        }
    }

    private fun AddFeedFromUserSelection(name: String, dialogCaption: String, loader: BaseLoader<ArrayList<HashMap<String, String>>>) {
        val pd = ProgressDialog(this@EditFeedActivity)
        pd.setMessage(getString(R.string.loading))
        pd.setCancelable(true)
        pd.isIndeterminate = true
        pd.show()
        loaderManager.restartLoader(1, Bundle(), object : LoaderManager.LoaderCallbacks<ArrayList<HashMap<String, String>>> {
            override fun onCreateLoader(id: Int, args: Bundle): Loader<ArrayList<HashMap<String, String>>> {
                return loader
            }

            override fun onLoadFinished(loader: Loader<ArrayList<HashMap<String, String>>>,
                                        data: ArrayList<HashMap<String, String>>) {
                pd.cancel()
                if (data == null) {
                    UiUtils.showMessage(this@EditFeedActivity, R.string.error)
                } else if (data.isEmpty()) {
                    UiUtils.showMessage(this@EditFeedActivity, R.string.no_result)
                } else if (data.size == 1) {
                    AddFeed(data[0])
                } else {
                    val builder = AlertDialog.Builder(this@EditFeedActivity)
                    //builder.setTitle(dialogCaption);
                    run {
                        val textView = UiUtils.CreateSmallText(builder.context, Gravity.CENTER, null, dialogCaption)
                        textView.maxLines = 3
                        builder.setCustomTitle(textView)
                    }
                    // create the grid item mapping
                    val from = arrayOf(ITEM_TITLE, ITEM_DESC, ITEM_URL, ITEM_ICON)
                    val to = intArrayOf(R.id.search_item_title, R.id.search_item_descr, R.id.search_item_url, R.id.search_item_icon)

                    // fill in the grid_item layout
                    val adapter = SimpleAdapter(this@EditFeedActivity, data, R.layout.item_search_result, from, to)
                    adapter.viewBinder = SimpleAdapter.ViewBinder { view, data_, _ ->
                        if (view is TextView) UiUtils.SetTypeFace(view)
                        if (data_ == null)
                            return@ViewBinder false
                        val value = data_ as String
                        if (view is TextView && value.startsWith(IS_READ_STUMB)) {
                            var text = value
                            view.setTextColor(Color.DKGRAY)
                            text = text.replace(IS_READ_STUMB, "")
                            view.text = text
                            true
                        } else if (view.id == R.id.search_item_title) {
                            (view as TextView).setTextColor(this@EditFeedActivity.resources.getColor(android.R.color.primary_text_dark))
                            false
                        } else if (view.id == R.id.search_item_descr) {
                            (view as TextView).setTextColor(this@EditFeedActivity.resources.getColor(android.R.color.secondary_text_dark))
                            false
                        } else if (view.id == R.id.search_item_url) {
                            (view as TextView).setTextColor(this@EditFeedActivity.resources.getColor(android.R.color.tertiary_text_dark))
                            false
                        } else if (view.id == R.id.search_item_icon) {
                            if (value != null) {
                                Glide.with(this@EditFeedActivity).load(value).centerCrop().into((view as ImageView))
                            } else {
                                Glide.with(this@EditFeedActivity).clear(view)
                                (view as ImageView).setImageResource(R.drawable.cup_new_empty)
                            }
                            true
                        } else false
                    }
                    val lv = ListView(baseContext)
                    builder.setView(lv)
                    lv.adapter = adapter
                    lv.setOnScrollListener(object : AbsListView.OnScrollListener {
                        override fun onScrollStateChanged(absListView: AbsListView, i: Int) {}
                        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                            if (PrefUtils.getBoolean(DIALOG_IS_SHOWN, false) && visibleItemCount > 0) {
                                PrefUtils.putInt(SEARCH_RESULTS_FIRST_VISISBLE_ITEM, firstVisibleItem)
                                val v = view.getChildAt(0)
                                PrefUtils.putInt(SEARCH_RESULTS_Y_OFFSET, v.top - view.paddingTop)
                            }
                        }
                    })
                    lv.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
                        mUserSelectionDialog?.dismiss()
                        AddFeed(data[i])
                    }
                    if (Build.VERSION.SDK_INT >= 17) builder.setOnDismissListener { mUserSelectionDialog = null }
                    PrefUtils.putBoolean(DIALOG_IS_SHOWN, true)
                    mUserSelectionDialog = builder.show()
                    lv.setSelectionFromTop(PrefUtils.getInt(SEARCH_RESULTS_FIRST_VISISBLE_ITEM, 0),
                            PrefUtils.getInt(SEARCH_RESULTS_Y_OFFSET, 0))
                }
            }

            override fun onLoaderReset(loader: Loader<ArrayList<HashMap<String, String>>>) {}
            private fun AddFeed(dataItem: HashMap<String, String>) {
                if (this@EditFeedActivity is ArticleWebSearchActivity) {
                    val url = dataItem[ITEM_URL]!!.replace(IS_READ_STUMB, "")
                    val intent: Intent
                    if (Intent.ACTION_WEB_SEARCH == dataItem[ITEM_URL]) {
                        intent = Intent()
                        intent.action = Intent.ACTION_WEB_SEARCH
                        intent.putExtra(SearchManager.QUERY, mUrlEditText.text)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } else if (Intent.ACTION_VIEW == dataItem[ITEM_ACTION]) {
                        intent = GetShowInBrowserIntent( url );
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } else {
                        intent = Intent(this@EditFeedActivity, EntryActivity::class.java)
                        intent.data = Uri.parse(url)
                        intent.putExtra( NEW_TASK_EXTRA, true )
                    }
                    this@EditFeedActivity.startActivity(intent)
                    return
                }
                val result = FeedDataContentProvider.addFeed(this@EditFeedActivity,
                        dataItem[ITEM_URL],
                        if (name.isEmpty()) dataItem[ITEM_TITLE] else name,
                        if (mHasGroupCb.isChecked) mGroupSpinner.selectedItemId else null,
                        mRetrieveFulltextCb.isChecked,
                        mShowTextInEntryListCb.isChecked,
                        mIsAutoImageLoadCb.isChecked,
                        optionsJsonString)
                if (result.second) {
                    UiUtils.toast( R.string.new_feed_was_added )
                    FetcherService.Start(Intent(this@EditFeedActivity, FetcherService::class.java)
                            .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                            .putExtra(Constants.FEED_ID, result.first.lastPathSegment), true)
                    HomeActivity.mNewFeedUri = FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(result.first.lastPathSegment)
                    setResult(RESULT_OK)
                    startActivity(Intent(this@EditFeedActivity, HomeActivity::class.java)
                            .setAction(Intent.ACTION_MAIN)
                            .setData(HomeActivity.mNewFeedUri))
                }
                finish()
            }

        })
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        val cursorLoader = CursorLoader(this, FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(intent.data!!.lastPathSegment),
                null, null, null, FilterColumns.IS_ACCEPT_RULE + Constants.DB_DESC)
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY.toLong())
        return cursorLoader
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        mFiltersCursorAdapter.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mFiltersCursorAdapter.swapCursor(Constants.EMPTY_CURSOR)
    }

    companion object {
        const val EXTRA_WEB_SEARCH = "EXTRA_WEB_SEARCH"
        const val ITEM_TITLE = "title"
        const val ITEM_URL = "feedId"
        const val ITEM_DESC = "description"
        const val ITEM_ICON = "icon"
        const val ITEM_ACTION = "action"
        private const val STATE_CURRENT_TAB = "STATE_CURRENT_TAB"
        private const val STATE_FEED_EDIT_LOAD_TYPE_ID = "STATE_FEED_EDIT_LOAD_TYPE_ID"
        const val DUCKDUCKGO_SEARCH_URL = "https://duckduckgo.com/html/?q="
        private val FEED_PROJECTION = arrayOf(FeedColumns.NAME, FeedColumns.URL, FeedColumns.RETRIEVE_FULLTEXT, FeedColumns.IS_GROUP, FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, FeedColumns.IS_AUTO_REFRESH, FeedColumns.GROUP_ID, FeedColumns.IS_IMAGE_AUTO_LOAD, FeedColumns.OPTIONS)
        const val STATE_WEB_SEARCH_TEXT = "WEB_SEARCH_TEXT"
        private const val STATE_LAST_LOAD_TYPE = "STATE_LAST_LOAD_TYPE"
        const val DIALOG_IS_SHOWN = "EDIT_FEED_USER_SELECTION_DIALOG_IS_SHOWN"
        const val SEARCH_RESULTS_FIRST_VISISBLE_ITEM = "SEARCH_RESULTS_FIRST_VISISBLE_ITEM"
        const val SEARCH_RESULTS_Y_OFFSET = "SEARCH_RESULTS_Y_OFFSET"
        const val IS_READ_STUMB = "[IS_READ]"
        const val AUTO_SET_AS_READ = "AUTO_SET_AS_READ"
        const val LAST_DATE = "DuckDuckGoSearchLastUrlDate"
        const val LAST_URL_TEXT = "DuckDuckGoSearchLastUrl"
        fun IsSameUrl(url: String): Boolean {
            return PrefUtils.getString(LAST_URL_TEXT, "") == url &&
                    System.currentTimeMillis() - PrefUtils.getLong(LAST_DATE, 0) < 1000 * 60 * 60 * 24
        }

        @JvmStatic
        fun getApplyTypeCaption(DBId: Int): Int {
            return if (DBId == FilterColumns.DB_APPLIED_TO_CONTENT) R.string.filter_apply_to_content else if (DBId == FilterColumns.DB_APPLIED_TO_TITLE) R.string.filter_apply_to_title else if (DBId == FilterColumns.DB_APPLIED_TO_AUTHOR) R.string.filter_apply_to_author else if (DBId == FilterColumns.DB_APPLIED_TO_CATEGORY) R.string.filter_apply_to_category else if (DBId == FilterColumns.DB_APPLIED_TO_URL) R.string.filter_apply_to_url else R.string.filter_apply_to_title
        }
    }
}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
internal class GetFeedSearchResultsLoader(context: Context?, private var mSearchText: String) : BaseLoader<ArrayList<HashMap<String, String>>>(context) {
    /**
     * This is where the bulk of our work is done. This function is called in a background thread and should generate a new set of data to be
     * published by the loader.
     */
    override fun loadInBackground(): ArrayList<HashMap<String, String>> {
        val results = ArrayList<HashMap<String, String>>()
        return try {
            //    HttpURLConnection conn = NetworkUtils.setupConnection("https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q=" + mSearchText);
            val conn = Connection("http://cloud.feedly.com/v3/search/feeds/?count=20&query=$mSearchText")
            try {
                val jsonStr = String(NetworkUtils.getBytes(conn.inputStream))
                //String jsonStr = "{\"results\":[{\"deliciousTags\":[\"история\",\"science\",\"education\",\"culture\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1.rss\",\"title\":\"Arzamas | Всё\",\"language\":\"ru\",\"lastUpdated\":1493701920000,\"subscribers\":1947,\"velocity\":3.5,\"website\":\"http://arzamas.academy/?utm_campaign=main_rss&utm_medium=rss&utm_source=rss_link\",\"score\":1947.0,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":1507,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"Новые курсы каждый четверг и дополнительные материалы в течение недели\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_cover-15b8675fdfa\",\"iconUrl\":\"http://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_icon-15414d05afc\",\"partial\":false,\"visualUrl\":\"http://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_visual-15414d05afc\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"magazines\",\"история\",\"журналы\",\"блоги\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1/mag.rss\",\"title\":\"Arzamas | Журнал\",\"language\":\"ru\",\"lastUpdated\":1493701980000,\"subscribers\":249,\"velocity\":1.2,\"website\":\"http://arzamas.academy/mag\",\"score\":1871.93,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":1390,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"??????? ???????? ? ?????, ??????? ? ???????????\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_cover-15b8e8b3d47\",\"iconUrl\":\"http://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_icon-1542d1e8523\",\"partial\":false,\"visualUrl\":\"http://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_visual-1542d1e8523\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"science\",\"история\",\"образование\",\"education\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1/courses.rss\",\"title\":\"Arzamas | Курсы\",\"language\":\"ru\",\"lastUpdated\":1435214100000,\"subscribers\":245,\"velocity\":0.01,\"website\":\"http://arzamas.academy/courses?utm_campaign=courses_rss&utm_medium=rss&utm_source=rss_link\",\"score\":24.5,\"coverage\":0.0,\"coverageScore\":0.0,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"Новые курсы каждый четверг\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_cover-15b870310a2\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_icon-15b870310a2\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_visual-15b870310a2\",\"coverColor\":\"000000\",\"art\":0.0},{\"feedId\":\"feed/http://arzamas.academy/feed_v1/podcast.rss\",\"title\":\"История культуры | Курсы | Arzamas\",\"language\":\"ru\",\"lastUpdated\":1492671600000,\"subscribers\":17,\"velocity\":10.5,\"website\":\"http://arzamas.academy/courses?utm_campaign=episodes_podcast_rss&utm_medium=rss&utm_source=rss_link\",\"score\":17.0,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":366,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"audio\",\"description\":\"Arzamas — это просветительский проект, посвященный гуманитарному знанию. В основе Arzamas лежат курсы, или «гуманитарные сериалы», — каждый на свою тему. Мы записываем выдающихся ученых-гуманитариев и снабжаем их лекции дополнительными материалами. Получается своебразный университет, каждую неделю, по четвергам, выпускающий новый курс по истории, литературе, искусству, антропологии, философии — о культуре и человеке. В этом подкасте представлены аудио-версии наших курсов. Полные версии доступы на нашем сайте (http://arzamas.academy).\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_cover-15b99c01c32\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_icon-15b99c01c32\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_visual-15b99c01c32\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"youtube\"],\"feedId\":\"feed/https://www.youtube.com/playlist?list=UUVgvnGSFU41kIhEc09aztEg\",\"title\":\"Arzamas (uploads) on YouTube\",\"language\":\"ru\",\"lastUpdated\":1491832800000,\"subscribers\":28,\"velocity\":1.2,\"website\":\"https://youtube.com/playlist?list=UUVgvnGSFU41kIhEc09aztEg\",\"score\":152.78,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":149,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"video\",\"description\":\"Официальный канал Arzamas.academy\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/jGwxZEqqmeCambMrgz-PZPVTczhBEEZ48IdO6thk3Ww_sicon-15b6534aa74\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/jGwxZEqqmeCambMrgz-PZPVTczhBEEZ48IdO6thk3Ww_svisual-15b6534aa74\",\"art\":0.0}],\"queryType\":\"term\",\"related\":[\"история\"],\"scheme\":\"subs.0\"}";

                // Parse results
                val entries = JSONObject(jsonStr).getJSONArray("results")
                for (i in 0 until entries.length()) {
                    try {
                        val entry = entries[i] as JSONObject
                        val url = entry[EditFeedActivity.ITEM_URL].toString().replaceFirst("feed/http".toRegex(), "http")
                        if (!url.isEmpty()) {
                            val map = HashMap<String, String>()
                            map[EditFeedActivity.ITEM_TITLE] = Html.fromHtml(entry[EditFeedActivity.ITEM_TITLE].toString())
                                    .toString()
                            map[EditFeedActivity.ITEM_URL] = url
                            map[EditFeedActivity.ITEM_DESC] = Html.fromHtml(entry[EditFeedActivity.ITEM_DESC].toString()).toString()
                            results.add(map)
                        }
                    } catch (ignored: Exception) {
                    }
                }
                results
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Dog.e("Error", e)
            results
        }
    }

    init {
        try {
            mSearchText = URLEncoder.encode(mSearchText, Constants.UTF8)
        } catch (ignored: UnsupportedEncodingException) {
        }
    }
}

internal class GetSiteAlternateListLoader(context: Context?, private val mUrl: String) : BaseLoader<ArrayList<HashMap<String, String>>>(context) {
    /**
     * This is where the bulk of our work is done. This function is called in a background thread and should generate a new set of data to be
     * published by the loader.
     */
    override fun loadInBackground(): ArrayList<HashMap<String, String>> {
        val results = ArrayList<HashMap<String, String>>()
        return try {
            val conn = Connection(mUrl)
            try {
                val content = String(NetworkUtils.getBytes(conn.inputStream))
                val TITLE_PATTERN = Pattern.compile("title=\"(.+?)\"", Pattern.CASE_INSENSITIVE)
                val HREF_PATTERN = Pattern.compile("href=\"(.+?)\"", Pattern.CASE_INSENSITIVE)
                val matcher = FetcherService.FEED_LINK_PATTERN.matcher(content)
                //final String HREF = "href=\"";
                while (matcher.find()) { // not "while" as only one link is needed
                    val line = matcher.group()
                    val urlMatcher = HREF_PATTERN.matcher(line)
                    if (urlMatcher.find()) {
                        var url = urlMatcher.group(1)
                        if (!url.toLowerCase().contains("rss") &&
                            !url.toLowerCase().contains("feed") &&
                            !url.toLowerCase().contains("atom")) continue
                        if (url.startsWith(Constants.SLASH)) {
                            val index = mUrl.indexOf('/', 8)
                            url = if (index > -1) {
                                mUrl.substring(0, index) + url
                            } else {
                                mUrl + url
                            }
                        } else if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                            url = "$mUrl/$url"
                        }
                        val titleMatcher = TITLE_PATTERN.matcher(line)
                        val title = if (titleMatcher.find()) unescapeTitle( titleMatcher.group(1) ) else url
                        val map = HashMap<String, String>()
                        map[EditFeedActivity.ITEM_TITLE] = title
                        map[EditFeedActivity.ITEM_DESC] = url
                        map[EditFeedActivity.ITEM_URL] = url
                        results.add(map)
                    }
                }
                if (results.isEmpty()) {
                    val map = HashMap<String, String>()
                    map[EditFeedActivity.ITEM_TITLE] = ""
                    map[EditFeedActivity.ITEM_URL] = mUrl
                    results.add(map)
                }
                results
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Dog.e("Error", e)
            results
        }
    }
}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
internal class GetWebSearchDuckDuckGoResultsLoader(context: Context?, private val mSearchText: String) : BaseLoader<ArrayList<HashMap<String, String>>>(context) {
    val mUrl: String
    private val ITEM_FIELD_SEP = "__#__"
    override fun loadInBackground(): ArrayList<HashMap<String, String>> {
        val LAST_RESULTS = "DuckDuckGoSearchLastResults"
        val ITEM_SEP = "__####__"
        val results = ArrayList<HashMap<String, String>>()
        if (EditFeedActivity.IsSameUrl(mSearchText)) {
            for (item in TextUtils.split(PrefUtils.getString(LAST_RESULTS, ""), ITEM_SEP)) {
                val map = HashMap<String, String>()
                val fieldList = TextUtils.split(item, ITEM_FIELD_SEP)
                if (fieldList.size > 3) {
                    val title = fieldList[0]
                    val url = fieldList[1]
                    val descr = fieldList[2]
                    val icon = fieldList[3]
                    val action = if (fieldList.size > 4) fieldList[4] else ""
                    var read = ""
                    if (FetcherService.GetEntryUri(url) != null) read = EditFeedActivity.IS_READ_STUMB
                    map[EditFeedActivity.ITEM_TITLE] = read + title
                    map[EditFeedActivity.ITEM_URL] = read + url
                    map[EditFeedActivity.ITEM_DESC] = read + descr
                    map[EditFeedActivity.ITEM_ICON] = icon
                    map[EditFeedActivity.ITEM_ACTION] = action
                    results.add(map)
                }
            }
            return results
        }
        return try {
            val conn = Connection(mUrl)
            try {
                val data = ArrayList<String?>()
                val doc = conn.parse
                val globalFilters = FeedFilters( FetcherService.GetExtrenalLinkFeedID() )
                for (el in doc!!.getElementsByClass("results_links")) {
                    try {
                        val title = el.getElementsByClass("result__title").text()
                        var url = el.getElementsByClass("result__title").first()!!.getElementsByTag("a").first()!!.attr("href")
                        url = URLDecoder.decode(url.substring(url.indexOf("http")))
                        url = url.replace("&rut=[^\"]+".toRegex(), "")
                        val descr = el.getElementsByClass("result__snippet").text()
                        var icon = el.getElementsByClass("result__icon__img").first()!!.attr("src")
                        if (!icon.startsWith("https:")) icon = "https:$icon"
                        if ( globalFilters.isEntryFiltered( title, "", url, descr, null ) )
                            continue
                        AddItem(results, data, "", title, url, descr, icon)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                AddItem(results, data, Intent.ACTION_WEB_SEARCH, context.getString(R.string.search_in_browser), "", "", "")
                AddItem(results, data, Intent.ACTION_VIEW, context.getString(R.string.search_in_yandex), YANDEX_SEARCH_TEXT_URL + TextUtils.htmlEncode(mSearchText), "", YANDEX_FAVICON_URL)
                AddItem(results, data, Intent.ACTION_VIEW, context.getString(R.string.search_in_google), GOOGLE_SEARCH_TEXT_URL + TextUtils.htmlEncode(mSearchText), "", GOOGLE_FAVICON_URL)
                PrefUtils.putString(LAST_RESULTS, TextUtils.join(ITEM_SEP, data))
                PrefUtils.putString(EditFeedActivity.LAST_URL_TEXT, mSearchText)
                PrefUtils.putInt(EditFeedActivity.SEARCH_RESULTS_FIRST_VISISBLE_ITEM, 0)
                PrefUtils.putInt(EditFeedActivity.SEARCH_RESULTS_Y_OFFSET, 0)
                PrefUtils.putLong(EditFeedActivity.LAST_DATE, System.currentTimeMillis())
                results
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Dog.e("Error", e)
            results
        }
    }

    private fun AddItem(results: ArrayList<HashMap<String, String>>, data: ArrayList<String?>,
                        action: String,
                        title: String,
                        url: String,
                        descr: String,
                        iconUrl: String) {
        val map = HashMap<String, String>()
        val read = if (FetcherService.GetEntryUri(url) != null) EditFeedActivity.IS_READ_STUMB else ""
        map[EditFeedActivity.ITEM_TITLE] = read + title
        map[EditFeedActivity.ITEM_URL] = read + url
        map[EditFeedActivity.ITEM_DESC] = read + descr
        map[EditFeedActivity.ITEM_ICON] = iconUrl
        map[EditFeedActivity.ITEM_ACTION] = action
        results.add(map)
        val list = ArrayList<String?>()
        list.add(map[EditFeedActivity.ITEM_TITLE])
        list.add(map[EditFeedActivity.ITEM_URL])
        list.add(map[EditFeedActivity.ITEM_DESC])
        list.add(map[EditFeedActivity.ITEM_ICON])
        list.add(map[EditFeedActivity.ITEM_ACTION])
        data.add(TextUtils.join(ITEM_FIELD_SEP, list))
    }

    companion object {
        private const val CLASS_ATTRIBUTE = "result__snippet"
        private const val YANDEX_SEARCH_TEXT_URL = "https://yandex.com/search/?text="
        const val YANDEX_FAVICON_URL = "https://yandex.com/favicon.ico"
        private const val GOOGLE_SEARCH_TEXT_URL = "https://google.com/search?ie=UTF-8&q="
        const val GOOGLE_FAVICON_URL = "https://www.google.com/favicon.ico"
    }

    init {
        var s = mSearchText
        try {
            s = URLEncoder.encode(mSearchText, Constants.UTF8)
        } catch (ignored: UnsupportedEncodingException) {
        }
        mUrl = EditFeedActivity.DUCKDUCKGO_SEARCH_URL + s + "&kl=" + Locale.getDefault().language
    }
}