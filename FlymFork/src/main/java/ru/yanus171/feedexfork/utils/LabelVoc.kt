package ru.yanus171.feedexfork.utils

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.provider.BaseColumns
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckBox
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.LabelListActivity
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.provider.FeedData.EntryLabelColumns
import ru.yanus171.feedexfork.provider.FeedData.LabelColumns
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.provider.FeedDataContentProvider
import ru.yanus171.feedexfork.service.FetcherService
import ru.yanus171.feedexfork.utils.LabelVoc.getNextOrder
import ru.yanus171.feedexfork.utils.UiUtils.SetTypeFace
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

public class Label(id: Long, name: String, var mColor: String) {

    @JvmField
    var mOrder: Int = 0

    @JvmField
    var mEntriesReadCount: Int = 0

    @JvmField
    var mEntriesUnreadCount: Int = 0

    @JvmField
    var mEntriesImagesSize: Long = 0

    @kotlin.jvm.JvmField
    var mID = id

    @kotlin.jvm.JvmField
    var mName = name

    init {
        if (mColor.isNullOrEmpty())
            mColor = Theme.GetColor(Theme.TEXT_COLOR_READ, R.string.default_read_color)
        mOrder = getNextOrder()
    }

    constructor (cursor: Cursor) : this(cursor.getLong(0), cursor.getString(1), cursor.getString(2))

    fun colorInt(): Int {
        return Color.parseColor(mColor)
    }
}

object LabelVoc {
    private var mIsInitialized = false
    private val mVoc = HashMap<Long, Label>()
    private val mEntryVoc = HashMap<Long, HashSet<Long>>()

    private fun init_() {
        synchronized(mVoc) {
            if ( mIsInitialized )
                return
            val status = FetcherService.Status().Start("Reading labels", true)
            mVoc.clear()
            run {
                val cursor = MainApplication.getContext().contentResolver.query(
                        LabelColumns.CONTENT_URI, arrayOf(BaseColumns._ID, LabelColumns.NAME, LabelColumns.COLOR),
                        null,
                        null,
                        null)
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        mVoc[cursor.getLong(0)] = Label(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
                    }
                    cursor.close()
                }
            }

            mEntryVoc.clear()
            run {
                val cursor = MainApplication.getContext().contentResolver.query(
                        EntryLabelColumns.CONTENT_URI, arrayOf(EntryLabelColumns.ENTRY_ID, EntryLabelColumns.LABEL_ID),
                        null,
                        null,
                        null)
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val entryId = cursor.getLong(0)
                        if ( !mEntryVoc.containsKey(entryId) )
                            mEntryVoc[entryId] = HashSet<Long>()
                        mEntryVoc[entryId]!!.add(cursor.getLong(1))
                    }
                    cursor.close()
                }
            }

            FetcherService.Status().End(status)
            mIsInitialized = true
        }
    }


    fun addLabel(name: String, id: Long, color: String) {
        initInThread()
        synchronized(mVoc) {
            mVoc[id] = Label(id, name, color)
        }
    }

    fun getNextID() = (mVoc.keys.maxOrNull() ?: 0) + 1
    fun getNextOrder(): Int {
        var result = 0
        for( item in mVoc.values )
            if ( item.mOrder > result )
                result = item.mOrder
        return result + 1
    }
    fun getList(): ArrayList<Label> {
        initInThread()
        synchronized(mVoc) {
            return ArrayList(mVoc.values)
        }
    }

    fun editLabel(label: Label) {
        initInThread()
        synchronized(mVoc) {
            mVoc[label.mID] = label

        }
    }
    fun deleteLabel(id: Long) {
        initInThread()
        synchronized(mVoc) {
            mVoc.remove(id)
            mEntryVoc.remove(id)
        }
    }
    fun setEntry(entryID: Long, labels: HashSet<Long>) {
        initInThread()
        synchronized(mVoc) {
            mEntryVoc[entryID] = labels
        }
    }

    fun get(labelID: Long): Label? {
        synchronized(mVoc) {
            init_()
            return mVoc.get(labelID)
        }
    }

    fun set(label: Label) {
        initInThread()
        synchronized(mVoc) {
            mVoc[label.mID] = label
        }
    }

    fun getLabelIDs(entryID: Long): HashSet<Long> {
        initInThread()
        synchronized(mVoc) {
            return mEntryVoc.get(entryID) ?: HashSet<Long>()
        }
    }

    fun getLabelEntriesID(labelID: Long): Set<Long> {
        initInThread()
        synchronized(mVoc) {
            val result = HashSet<Long>()
            for ( entryID in mEntryVoc.keys )
                if ( mEntryVoc[entryID]!!.contains( labelID ) )
                    result += entryID
            return result
        }
    }


    fun initInThread(){
        synchronized(mVoc) {
            if ( mIsInitialized )
                return
        }
        Thread{
            init_()
        }.start()
    }

    fun reinit(inThread: Boolean) {
        synchronized(mVoc) {
            mIsInitialized = false
        }
        if ( inThread )
            initInThread()
        else
            init_()
    }

    fun showDialog(context: Context, entryID: Long, adapterToNotify: BaseAdapter?) {
        val builder = AlertDialog.Builder(context)
        val checkedLabels = getLabelIDs(entryID)
        val array = mVoc.values.toTypedArray()
        var block = false
        builder.setAdapter(object : ArrayAdapter<Label>(context, R.layout.label_item_select, R.id.text_name, array) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                //Use super class to create the View
                val root = super.getView(position, convertView, parent)
                val cb = root.findViewById<CheckBox>(R.id.text_name)
                val label = array[position]
                cb.text = label.mName
                SetTypeFace(cb)
                block = true
                cb.isChecked = checkedLabels.contains(label.mID)
                cb.setTextColor(label.colorInt())
                block = false
                cb.tag = label.mID
                cb.setOnCheckedChangeListener { btn, isChecked ->
                    if (!block) {
                        if (isChecked)
                            checkedLabels.add(btn.tag as Long)
                        else
                            checkedLabels.remove(btn.tag as Long)
                    }
                }
                return root
            }
        }, null)
        .setPositiveButton(android.R.string.ok) { dialog, _ ->
            setEntry(entryID, checkedLabels)
            Thread {
                FeedDataContentProvider.SetNotifyEnabled(false)
                context.contentResolver.delete(EntryLabelColumns.CONTENT_URI(entryID), null, null)
                for( labelID in checkedLabels ) {
                    val values = ContentValues()
                    values.put(EntryLabelColumns.ENTRY_ID, entryID)
                    values.put(EntryLabelColumns.LABEL_ID, labelID)
                    context.contentResolver.insert(EntryLabelColumns.CONTENT_URI, values)
                }
                FeedDataContentProvider.SetNotifyEnabled(true)
                run {
                    val values = ContentValues()
                    values.put(FeedData.EntryColumns._ID, entryID)
                    values.put(FeedData.EntryColumns.IS_FAVORITE, if (checkedLabels.isNotEmpty()) 1 else 0)
                    context.contentResolver.update(FeedData.EntryColumns.CONTENT_URI(entryID), values, if (checkedLabels.isNotEmpty()) EntryColumns.WHERE_NOT_FAVORITE else EntryColumns.WHERE_FAVORITE, null )
                }

            }.start()
            adapterToNotify?.notifyDataSetChanged()
            dialog.dismiss()
        }
        .setNeutralButton(R.string.manage_labels) { dialog, _ ->
            val intent = Intent(context, LabelListActivity::class.java)
            dialog.dismiss()
            context.startActivity(intent)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .setTitle(R.string.article_labels_setup_title)
        builder.show()
    }
    fun getStringList(entryID: Long): String {
        initInThread()
        var result = ""
        synchronized(mVoc) {
            for ( id in getLabelIDs(entryID) ) {
                val label = mVoc[id]
                //result += "<b style=\"text-color: ${label?.mColor}\"> ${label?.mName} </b>"
                result += "<b><font color=\"${label?.mColor}\"> ${label?.mName} </font></b>"
            }
            return result
        }
    }



}