package ru.yanus171.feedexfork.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.adapter.FiltersCursorAdapter
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.utils.LabelVoc
import ru.yanus171.feedexfork.utils.UiUtils

class FilterEdit(val mContext: Context, val mFeedID: String) {
    private val dialogView = (mContext.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.dialog_filter_edit, null)
    val actionType = dialogView.findViewById(R.id.actionTypeRadioGroup) as RadioGroup
    val filterText = dialogView.findViewById(R.id.filterText) as EditText
    val regexCheckBox = UiUtils.SetupSmallTextView(dialogView, R.id.regexCheckBox) as CheckBox
    val acceptRadio = UiUtils.SetupSmallTextView(dialogView, R.id.acceptRadio) as RadioButton
    val markAsStarredRadio = UiUtils.SetupSmallTextView(dialogView, R.id.markAsStarredRadio) as RadioButton
    var labelIDList = HashSet<Long>()
    val removeTextRadio = UiUtils.SetupSmallTextView(dialogView, R.id.removeText) as RadioButton
    private val rejectRadio = UiUtils.SetupSmallTextView(dialogView, R.id.rejectRadio) as RadioButton
    private val applyAuthorButton = UiUtils.SetupSmallTextView(dialogView, R.id.applyAuthorRadio) as RadioButton
    private val applyCategoryButton = UiUtils.SetupSmallTextView(dialogView, R.id.applyCategoryRadio) as RadioButton
    private val applyUrlButton = UiUtils.SetupSmallTextView(dialogView, R.id.applyUrlRadio) as RadioButton
    private val markAsStarredTextRadio = UiUtils.SetupSmallTextView(dialogView, R.id.markAsStarredRadio) as RadioButton
    private val labelListTextView = UiUtils.SetupSmallTextView(dialogView, R.id.labelListTextView) as TextView
    private val labelListCaptionTextView = UiUtils.SetupSmallTextView(dialogView, R.id.labelListCaptionTextView) as TextView
    init {
        UiUtils.SetupSmallTextView(dialogView, R.id.applyTitleRadio)
        UiUtils.SetupSmallTextView(dialogView, R.id.applyContentRadio)
        actionType.setOnCheckedChangeListener { radioGroup, selectedID ->
            for (i in 0 until actionType.childCount) actionType.getChildAt(i).isEnabled = true
            if (selectedID == removeTextRadio.id) {
                applyAuthorButton.isEnabled = false
                applyCategoryButton.isEnabled = false
                applyUrlButton.isEnabled = false
            }
            run {
                val visibility = if (selectedID == markAsStarredTextRadio.id) View.VISIBLE else View.GONE
                labelListCaptionTextView.setVisibility(visibility)
                labelListTextView.setVisibility(visibility)
            }
        }
        labelListTextView.setOnClickListener { v -> LabelVoc.showDialog( v.context, R.string.filter_by_labels, true, labelIDList, null) {
            labelIDList = it
            UpdateUI()
        } }
    }

    private fun UpdateUI() {
        if ( labelIDList.isEmpty() )
            labelListTextView.text = dialogView.context.getString( R.string.none )
        else
            labelListTextView.text = Html.fromHtml( LabelVoc.getStringList( labelIDList ) )
    }

    @SuppressLint("Range")
    fun edit(adapter: FiltersCursorAdapter) {
        val c = adapter.cursor
        if (c.moveToPosition(adapter.selectedFilter)) {
            filterText.setText(c.getString(c.getColumnIndex(FeedData.FilterColumns.FILTER_TEXT)))
            regexCheckBox.isChecked = c.getInt(c.getColumnIndex(FeedData.FilterColumns.IS_REGEX)) == 1
            actionType.check(getAppliedTypeBtnId(c.getInt(c.getColumnIndex(FeedData.FilterColumns.APPLY_TYPE))))
            if ( !c.isNull( c.getColumnIndex(FeedData.FilterColumns.LABEL_ID_LIST) ) )
                labelIDList = LabelVoc.stringToList( c.getString( c.getColumnIndex(FeedData.FilterColumns.LABEL_ID_LIST) ) )
            if (c.getInt(c.getColumnIndex(FeedData.FilterColumns.IS_MARK_STARRED)) == 1) {
                markAsStarredRadio.isChecked = true
            } else if (c.getInt(c.getColumnIndex(FeedData.FilterColumns.IS_REMOVE_TEXT)) == 1) {
                removeTextRadio.isChecked = true
            } else if (c.getInt(c.getColumnIndex(FeedData.FilterColumns.IS_ACCEPT_RULE)) == 1) {
                acceptRadio.isChecked = true
            } else {
                rejectRadio.isChecked = true
            }
            val filterId = adapter.getItemId(adapter.selectedFilter)
            AlertDialog.Builder(mContext)
                    .setTitle(R.string.filter_edit_title)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        object : Thread() {
                            override fun run() {
                                val filter = filterText.text.toString()
                                if (!filter.isEmpty()) {
                                    val cr = mContext.contentResolver
                                    val values = ContentValues()
                                    values.put(FeedData.FilterColumns.FILTER_TEXT, filter)
                                    values.put(FeedData.FilterColumns.IS_REGEX, regexCheckBox.isChecked)
                                    values.put(FeedData.FilterColumns.APPLY_TYPE, getDBAppliedType(actionType.checkedRadioButtonId))
                                    values.put(FeedData.FilterColumns.IS_ACCEPT_RULE, acceptRadio.isChecked)
                                    values.put(FeedData.FilterColumns.IS_MARK_STARRED, markAsStarredRadio.isChecked)
                                    values.put(FeedData.FilterColumns.LABEL_ID_LIST, LabelVoc.listToString( labelIDList ) )
                                    values.put(FeedData.FilterColumns.IS_REMOVE_TEXT, removeTextRadio.isChecked)
                                    if (cr.update(FeedData.FilterColumns.CONTENT_URI, values, FeedData.FilterColumns._ID + '=' + filterId, null) > 0) {
                                        cr.notifyChange(
                                                FeedData.FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(mFeedID),
                                                null)
                                    }
                                }
                            }
                        }.start()
                    }.setNegativeButton(android.R.string.cancel, null).show()
        }
        UpdateUI()
    }
    fun add() {
        UpdateUI()
        AlertDialog.Builder(mContext )
                .setTitle(R.string.filter_add_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val filterText = (dialogView.findViewById<View>(R.id.filterText) as EditText).text.toString()
                    if (filterText.isNotEmpty()) {
                        val values = ContentValues()
                        values.put(FeedData.FilterColumns.FILTER_TEXT, filterText)
                        values.put(FeedData.FilterColumns.IS_REGEX, (dialogView.findViewById<View>(R.id.regexCheckBox) as CheckBox).isChecked)
                        values.put(FeedData.FilterColumns.APPLY_TYPE, getDBAppliedType(actionType.checkedRadioButtonId))
                        values.put(FeedData.FilterColumns.IS_ACCEPT_RULE, (dialogView.findViewById<View>(R.id.acceptRadio) as RadioButton).isChecked)
                        values.put(FeedData.FilterColumns.IS_MARK_STARRED, (dialogView.findViewById<View>(R.id.markAsStarredRadio) as RadioButton).isChecked)
                        values.put(FeedData.FilterColumns.IS_REMOVE_TEXT, (dialogView.findViewById<View>(R.id.removeText) as RadioButton).isChecked)
                        values.put(FeedData.FilterColumns.LABEL_ID_LIST, LabelVoc.listToString( labelIDList ) )
                        mContext.contentResolver.insert(FeedData.FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(mFeedID), values)
                    }
                }.setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
    }

    fun getDBAppliedType(btnID: Int): Int {
        return if (btnID == R.id.applyContentRadio) FeedData.FilterColumns.DB_APPLIED_TO_CONTENT else if (btnID == R.id.applyTitleRadio) FeedData.FilterColumns.DB_APPLIED_TO_TITLE else if (btnID == R.id.applyAuthorRadio) FeedData.FilterColumns.DB_APPLIED_TO_AUTHOR else if (btnID == R.id.applyCategoryRadio) FeedData.FilterColumns.DB_APPLIED_TO_CATEGORY else if (btnID == R.id.applyUrlRadio) FeedData.FilterColumns.DB_APPLIED_TO_URL else FeedData.FilterColumns.DB_APPLIED_TO_TITLE
    }
    private fun getAppliedTypeBtnId(DBId: Int): Int {
        return if (DBId == FeedData.FilterColumns.DB_APPLIED_TO_CONTENT) R.id.applyContentRadio else if (DBId == FeedData.FilterColumns.DB_APPLIED_TO_TITLE) R.id.applyTitleRadio else if (DBId == FeedData.FilterColumns.DB_APPLIED_TO_AUTHOR) R.id.applyAuthorRadio else if (DBId == FeedData.FilterColumns.DB_APPLIED_TO_CATEGORY) R.id.applyCategoryRadio else if (DBId == FeedData.FilterColumns.DB_APPLIED_TO_URL) R.id.applyUrlRadio else R.id.applyTitleRadio
    }

}