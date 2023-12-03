package ru.yanus171.feedexfork.utils

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.provider.BaseColumns
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.*
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.LabelListActivity
import ru.yanus171.feedexfork.fragment.EntriesListFragment.ALL_LABELS
import ru.yanus171.feedexfork.provider.FeedData.*
import ru.yanus171.feedexfork.provider.FeedDataContentProvider
import ru.yanus171.feedexfork.service.FetcherService
import ru.yanus171.feedexfork.utils.UiUtils.SetFont
import ru.yanus171.feedexfork.utils.UiUtils.dpToPixel

public class Label(id: Long, name: String, var mColor: String, var mOrder: Int) {

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
            mColor = Theme.GetTextColorRead()
    }

    constructor (cursor: Cursor) : this(
            cursor.getLong(0),
            if ( cursor.isNull(1) ) "" else cursor.getString(1),
            cursor.getString(2),
            cursor.getInt(3))

    fun colorInt(): Int {
        return Color.parseColor(mColor)
    }
}

object LabelVoc {
    private val mWithoutChildVoc = HashSet<Long>()
    private var mIsInitialized = false
    private val mVoc = HashMap<Long, Label>()
    private val mEntryVoc = HashMap<Long, HashSet<Long>>()
    private val mChildVoc = HashMap<Long, HashSet<Long>>()

    private fun init_() {
        synchronized(mVoc) {
            if ( mIsInitialized )
                return
            val status = FetcherService.Status().Start("Reading labels", true)

            mVoc.clear()
            run {
                val cursor = MainApplication.getContext().contentResolver.query(
                        LabelColumns.CONTENT_URI, arrayOf(BaseColumns._ID, LabelColumns.NAME, LabelColumns.COLOR, LabelColumns.ORDER),
                        null,
                        null,
                        LabelColumns.ORDER)
                if (cursor != null) {
                    while (cursor.moveToNext())
                        mVoc[cursor.getLong(0)] = Label(cursor)
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
                        val labelId = cursor.getLong(1)
                        if ( !mEntryVoc.containsKey(entryId) )
                            mEntryVoc[entryId] = HashSet()
                        mEntryVoc[entryId]!!.add(labelId)

                    }
                    cursor.close()
                }
            }

            updateChildVoc()

            FetcherService.Status().End(status)
            mIsInitialized = true
        }
    }

    private fun updateChildVoc() {
        mChildVoc.clear()
        mWithoutChildVoc.clear();
        for (labelIdList in mEntryVoc.values)
            if (labelIdList.count() == 1)
                mWithoutChildVoc.add( labelIdList.first() )
            else if (labelIdList.count() > 1)
                for (parentLabelId in labelIdList) {
                    if (!mChildVoc.containsKey(parentLabelId))
                        mChildVoc[parentLabelId] = HashSet<Long>()
                    for (childLabelId in labelIdList)
                        if (parentLabelId != childLabelId) {
                            mChildVoc[parentLabelId]!!.add(childLabelId)
                        }
                }
    }

    fun getChildLabelsNumbers(forReadEntries: Boolean, readEntriesMap: HashSet<Long> ): HashMap<String, Int> {
        val result = HashMap<String, Int>()
        synchronized(mVoc) {
            for ((entryID,labelIDList) in mEntryVoc)
                for (key1 in labelIDList)
                    for (key2 in labelIDList)
                        if ( key1 != key2 && forReadEntries == readEntriesMap.contains( entryID ) ) {
                            val key = "${key1}_${key2}"
                            if ( !result.containsKey( key ) )
                                result[key] = 0
                            result[key] = result[key]!! + 1;
                        }

            for ((entryID,labelIDList) in mEntryVoc)
                if ( forReadEntries == readEntriesMap.contains( entryID ) && labelIDList.count() == 1 ) {
                    val key = "${labelIDList.first()}_${labelIDList.first()}"
                    if ( !result.containsKey( key ) )
                        result[key] = 0
                    result[key] = result[key]!! + 1;
                }
        }
        return result
    }


    fun addLabel(name: String, id: Long, color: String) {
        initInThread()
        synchronized(mVoc) {
            mVoc[id] = Label(id, name, color, getNextOrder())
        }
        FeedDataContentProvider.notifyChangeOnAllUris(FeedDataContentProvider.URI_FEEDS, null)
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
            return if ( PrefUtils.IsLabelABCSort() )
                ArrayList(mVoc.values.sortedBy { it.mName })
            else
                ArrayList(mVoc.values.sortedBy { it.mOrder })
        }
    }

    fun editLabel(label: Label) {
        initInThread()
        synchronized(mVoc) {
            mVoc[label.mID] = label
        }
        FeedDataContentProvider.notifyChangeOnAllUris(FeedDataContentProvider.URI_FEEDS, null)
    }
    fun deleteLabel(id: Long) {
        initInThread()
        synchronized(mVoc) {
            mVoc.remove(id)
            mEntryVoc.remove(id)
            updateChildVoc()
        }
        FeedDataContentProvider.notifyChangeOnAllUris(FeedDataContentProvider.URI_FEEDS, null)
    }
    fun setEntry(entryID: Long, labels: HashSet<Long>) {
        initInThread()
        synchronized(mVoc) {
            mEntryVoc[entryID] = labels
            updateChildVoc()
        }
        FeedDataContentProvider.SetNotifyEnabled(false)
        MainApplication.getContext().contentResolver.delete(EntryLabelColumns.CONTENT_URI(entryID), null, null)
        for (labelID in labels) {
            val values = ContentValues()
            values.put(EntryLabelColumns.ENTRY_ID, entryID)
            values.put(EntryLabelColumns.LABEL_ID, labelID)
            MainApplication.getContext().contentResolver.insert(EntryLabelColumns.CONTENT_URI, values)
        }
        FeedDataContentProvider.SetNotifyEnabled(true)
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

    fun stringToList(s: String ): HashSet<Long> {
        val result = HashSet<Long>()
        for( item in TextUtils.split( s, "," ) )
            if ( get( item.trim().toLong() ) != null )
                result.add( item.trim().toLong() )
        return result;
    }
    fun listToString(list: HashSet<Long> ): String {
        val result = ArrayList<String>()
        for( item in list )
            result.add( item.toString() )
        return TextUtils.join( ",", result );
    }
    fun getLabelIDs(entryID: Long): HashSet<Long> {
        initInThread()
        synchronized(mVoc) {
            return mEntryVoc[entryID] ?: HashSet<Long>()
        }
    }

    fun getLabelEntriesID(labelID: Long): Set<Long> {
        initInThread()
        synchronized(mVoc) {
            val result = HashSet<Long>()
            for ( entryID in mEntryVoc.keys )
                if ( mEntryVoc[entryID]!!.contains(labelID) )
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

    fun showDialogToSetArticleLabels(context: Context, entryID: Long, adapterToNotify: BaseAdapter?) {
        showDialog(context, R.string.article_labels_setup_title, false, getLabelIDs(entryID), adapterToNotify) {
            setEntry(entryID, it)
            run {
                val values = ContentValues()
                values.put(EntryColumns._ID, entryID)
                PutFavorite( values, it.isNotEmpty() )
                context.contentResolver.update(EntryColumns.CONTENT_URI(entryID), values, if (it.isNotEmpty()) EntryColumns.WHERE_NOT_FAVORITE else EntryColumns.WHERE_FAVORITE, null)
            }
        }
    }

    fun showDialog(context: Context, titleID: Int, isAll: Boolean, checkedLabels_: HashSet<Long>, adapterToNotify: BaseAdapter?, action: (HashSet<Long>) -> Unit) {
        val builder = AlertDialog.Builder(context)
        val checkedLabels = checkedLabels_
        val array = getList().toTypedArray()
        var block = false

        var cbAll: CheckBox? = null
        if ( isAll ) {
            val viewTitle = LinearLayout(context)
            viewTitle.orientation = LinearLayout.VERTICAL
            run {
                val label = TextView(context)
                label.setPadding(dpToPixel(25), dpToPixel(10), 0, dpToPixel(10));
                viewTitle.addView(label)
                SetFont(label, 1F)
                label.text = context.getString(titleID)
            }
            cbAll = CheckBox(context)
            viewTitle.addView(cbAll)
            SetFont(cbAll, 1F)
            cbAll.text = context.getString(R.string.all)
            builder.setCustomTitle(viewTitle)
        } else
            builder.setTitle(titleID)

        val adapter = object : ArrayAdapter<Label>(context, R.layout.label_item_select, R.id.text_name, array) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                //Use super class to create the View
                val root = super.getView(position, convertView, parent)
                val cb = root.findViewById<CheckBox>(R.id.text_name)
                val label = array[position]
                cb.text = label.mName
                SetFont(cb, 1F)
                block = true
                cb.isChecked = cbAll?.isChecked == true || checkedLabels.contains(label.mID)
                cb.setTextColor(label.colorInt())
                cb.isEnabled = (cbAll == null || !cbAll.isChecked)
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
        }

        cbAll?.setOnCheckedChangeListener { _, _ -> adapter.notifyDataSetChanged() }
        cbAll?.isChecked = checkedLabels.contains(ALL_LABELS)

        builder
        .setAdapter(adapter, null)
        .setPositiveButton(android.R.string.ok) { dialog, _ ->
            if (cbAll?.isChecked == true) {
                checkedLabels.clear()
                checkedLabels += ALL_LABELS
            } else
                checkedLabels -= ALL_LABELS
            action(checkedLabels)
            adapterToNotify?.notifyDataSetChanged()
            dialog.dismiss()
        }
        .setNeutralButton(R.string.manage_labels) { dialog, _ ->
            val intent = Intent(context, LabelListActivity::class.java)
            dialog.dismiss()
            context.startActivity(intent)
        }
        .setNegativeButton(android.R.string.cancel, null)

        builder.show()
    }
    fun getStringList(entryID: Long): String {
        initInThread()
        return getStringList(getLabelIDs(entryID))
    }
    fun getStringList(labelIDs: Set<Long>): String {
        initInThread()
        var result = ""
        synchronized(mVoc) {
            if ( labelIDs.contains(ALL_LABELS) )
                return MainApplication.getContext().getString(R.string.all_labels)
            var index = 0
            for ( id  in labelIDs ) {
                val label = mVoc[id]
                //result += "<b style=\"text-color: ${label?.mColor}\"> ${label?.mName} </b>"
                val sep = if (index < labelIDs.size - 1) ", " else ""
                result += "<b><font color=\"${label?.mColor}\"> ${label?.mName}$sep </font></b>"
                index++
            }
            return result
        }
    }
    fun removeLabels(entryID: Long) {
        if ( getLabelIDs(entryID).isEmpty() )
            return
        setEntry(entryID, java.util.HashSet())
        MainApplication.getContext().contentResolver.delete(EntryLabelColumns.CONTENT_URI(entryID), null, null)
    }

    fun getChildrenIDs(parentLabelID: Long): ArrayList<Long> {
        val result = ArrayList<Long>()
        synchronized(mVoc) {
            if ( mWithoutChildVoc.contains( parentLabelID ) )
                result.add(parentLabelID)
            if ( mChildVoc.containsKey( parentLabelID ) )
                result.addAll( mChildVoc[parentLabelID]!! )
            return result
        }
    }


}