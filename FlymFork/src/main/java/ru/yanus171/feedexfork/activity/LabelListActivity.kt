package ru.yanus171.feedexfork.activity

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.provider.FeedData.LabelColumns
import ru.yanus171.feedexfork.utils.LabelVoc
import java.util.*


class LabelListActivity : AppCompatActivity(), Observer {

    private lateinit var mListView: ListView
    private lateinit var mMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_list)
        registerForContextMenu(findViewById(R.id.list_view))

        mListView = findViewById(R.id.list_view)
        mListView.adapter = object: ResourceCursorAdapter(
                this,
                R.layout.label_item,
                CreateCursor(),
                false) {
            override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
                val tag = EntryTag(cursor!!)
                val nameTextView = view!!.findViewById<TextView>(R.id.text_name)
                nameTextView.text = tag.mName
            }

            override fun getItem(position: Int): EntryTag {
                cursor.moveToPosition(position)
                return EntryTag(cursor)
            }
        };
//        mListView.setOnItemClickListener { parent, view, position, id ->
//            WebLoadRefreshService.startActionRefresh(mListView.getItemAtPosition(position) as WebLoadAccount, true)
//        }
    }

    private fun CreateCursor() = contentResolver.query(
            LabelColumns.CONTENT_URI,
            arrayOf(LabelColumns._ID, LabelColumns.NAME, LabelColumns.COLOR),
            null,
            null,
            LabelColumns._ID)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item!!.menuInfo as AdapterContextMenuInfo
        val tag = mListView.adapter.getItem(info.position) as EntryTag
        when (item.itemId) {
            MENU_EDIT -> createTagAddEditDialog(tag).create().show()
            MENU_REMOVE -> createTagRemoveDialog(tag).create().show()
        }
        return super.onContextItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun createTagAddEditDialog(tag_: EntryTag?): AlertDialog.Builder {
        var tag = tag_
        val builder = AlertDialog.Builder(this)
                .setTitle(if (tag_ == null) R.string.context_menu_add else R.string.context_menu_edit )
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val root = inflater.inflate(R.layout.label_edit_layout, null, false) as ViewGroup

        if ( tag == null )
            tag = EntryTag( null )
        val editName = root.findViewById<EditText>(R.id.edit_name)
        editName.setText( tag.mName )

        builder.setView(root)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            tag.mName = editName.text.toString()
            val values = ContentValues()
            values.put( LabelColumns.NAME, tag.mName )
            if ( tag_ == null ) {
                LabelVoc.addLabel( tag.mName )
                contentResolver.insert(LabelColumns.CONTENT_URI, values)
            } else {
                LabelVoc.editLabel( tag.mID, tag.mName )
                contentResolver.update( LabelColumns.CONTENT_URI( tag.mID ), values, null, null )
            }
            RefreshAdapter()

        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }

        return builder
    }

    private fun RefreshAdapter() {
        (mListView.adapter as ResourceCursorAdapter).changeCursor(CreateCursor());
    }

    private fun createTagRemoveDialog(tag: EntryTag): AlertDialog.Builder {
        return AlertDialog.Builder(this)
               .setTitle(tag.mName)
               .setMessage(R.string.remove_tag_confirmation)
               .setPositiveButton(android.R.string.ok){ dialog, _ ->
                   LabelVoc.deleteLabel( tag.mID )
                   contentResolver.delete( LabelColumns.CONTENT_URI(tag.mID), null, null )
                   RefreshAdapter()
                    dialog.dismiss()
               }
               .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        if (v == mListView) {
            menu.add(Menu.NONE, Companion.MENU_EDIT, Menu.NONE, R.string.context_menu_edit)
            menu.add(Menu.NONE, Companion.MENU_REMOVE, Menu.NONE, R.string.context_menu_delete)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mMenu = menu!!
        menu.clear() // This is needed to remove a bug on Android 4.0.3
        menuInflater.inflate(R.menu.label_list_activity_menu, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> createTagAddEditDialog(null).create().show()
        }
        return true;
    }
    override fun onDestroy() {
        //Global.GetWebLoadAccountList().mObservable.deleteObserver(this)
        super.onDestroy()
    }

    companion object {
        private const val MENU_EDIT = 102;
        //private const val MENU_ADD = 103;
        private const val MENU_REMOVE = 104;
    }

    override fun update(p0: Observable?, p1: Any?) {
        (mListView.adapter as BaseAdapter ).notifyDataSetChanged()
    }
}

class EntryTag( val cursor: Cursor? ) {
    val mID: Long = cursor?.getLong( 0 ) ?: 0
    var mName: String = cursor?.getString( 1 ) ?: ""
    val mColor: String = cursor?.getString( 2 ) ?: ""
}
