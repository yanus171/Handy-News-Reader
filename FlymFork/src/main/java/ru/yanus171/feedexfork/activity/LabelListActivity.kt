package ru.yanus171.feedexfork.activity

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Color
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
import ru.yanus171.feedexfork.utils.*
import ru.yanus171.feedexfork.view.ColorPreference
import ru.yanus171.feedexfork.view.DragNDropListView
import ru.yanus171.feedexfork.view.DragNDropListener
import java.util.*


class LabelListActivity : AppCompatActivity(), Observer {

    private lateinit var mListView: DragNDropListView
    private lateinit var mMenu: Menu

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_list)
        registerForContextMenu(findViewById(R.id.list_view))

        mListView = findViewById(R.id.list_view)
        mListView.adapter = object: ResourceCursorAdapter(
                this,
                R.layout.label_item,
                createCursor(),
                false) {
            override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
                val label = Label(cursor!!)
                val nameTextView = view!!.findViewById<TextView>(R.id.text_name)
                nameTextView.text = label.mName
                nameTextView.setTextColor(label.colorInt())
            }

            override fun getItem(position: Int): Label {
                cursor.moveToPosition(position)
                return Label(cursor)
            }
        }

        mListView.onItemClickListener  = AdapterView.OnItemClickListener { _, _, pos, _ -> createLabelAddEditDialog(mListView.adapter.getItem(pos) as Label).create().show()
        }

        mListView.setDragNDropListener(object : DragNDropListener {
            override fun onStopDrag(itemView: View) {}
            override fun onStartDrag(itemView: View) {}

            override fun onDrop(flatPosFrom: Int, flatPosTo: Int) {
                if ( flatPosFrom < flatPosTo )
                    for ( i in flatPosFrom until flatPosTo)
                        swapLabelsOrder( LabelVoc.getList()[i], LabelVoc.getList()[i+1] )
                else
                    for ( i in flatPosFrom downTo (flatPosTo + 1)  )
                        swapLabelsOrder( LabelVoc.getList()[i], LabelVoc.getList()[i-1] )
                refreshAdapter()
            }

            private fun swapLabelsOrder( label1: Label, label2: Label ) {
                val order1 = label1.mOrder
                setLabelOrder(label1, label2.mOrder)
                setLabelOrder(label2, order1)
            }

            private fun setLabelOrder(label: Label, order: Int) {
                label.mOrder = order
                LabelVoc.set(label)
                val values = ContentValues()
                values.put(LabelColumns.ORDER, label.mOrder)
                contentResolver.update(LabelColumns.CONTENT_URI(label.mID), values, null, null)
            }

            override fun onDrag(x: Int, y: Int, listView: ListView) {}
        })
    }

    private fun createCursor() = contentResolver.query(
            LabelColumns.CONTENT_URI,
            arrayOf(LabelColumns._ID, LabelColumns.NAME, LabelColumns.COLOR, LabelColumns.ORDER),
            null,
            null,
            LabelColumns.ORDER)


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val label = mListView.adapter.getItem(info.position) as Label
        when (item.itemId) {
            MENU_EDIT -> createLabelAddEditDialog(label).create().show()
            MENU_REMOVE -> createLabelRemoveDialog(label).create().show()
        }
        return super.onContextItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun createLabelAddEditDialog(label_: Label?): AlertDialog.Builder {
        var label = label_
        val builder = AlertDialog.Builder(this)
                .setTitle(if (label_ == null) R.string.context_menu_add else R.string.context_menu_edit)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val root = inflater.inflate(R.layout.label_edit_layout, null, false) as ViewGroup

        if ( label == null )
            label = Label(LabelVoc.getNextID(), "", "", LabelVoc.getNextOrder())
        val editName = root.findViewById<EditText>(R.id.edit_name)
        editName.setText(label.mName)

        val colorView = root.findViewById<TextView>(R.id.color_view)
        updateColorView(colorView, label)

        colorView.setOnClickListener {
            val colorDialog = ColorDialog(this,
                    ColorTB.Create(label.colorInt(), Color.TRANSPARENT), false, true, false,
                    getString(R.string.label_color_dialog_title), "text", "")
            colorDialog.CreateBuilder().setPositiveButton(android.R.string.ok) { dialog, _ ->
                label.mColor = ColorPreference.ToHex(colorDialog.mColor.Text, false)
                updateColorView(colorView, label)
                dialog.dismiss()
            }.show()
        }

        builder.setView(root)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            label.mName = editName.text.toString()
            val values = ContentValues()
            values.put(LabelColumns.NAME, label.mName)
            values.put(LabelColumns.COLOR, label.mColor)
            values.put(LabelColumns.ORDER, label.mOrder)
            if ( label_ == null ) {
                val id = contentResolver.insert(LabelColumns.CONTENT_URI, values).lastPathSegment.toLong()
                LabelVoc.addLabel(label.mName, id, label.mColor)
            } else {
                LabelVoc.editLabel(label)
                contentResolver.update(LabelColumns.CONTENT_URI(label.mID), values, null, null)
            }
            refreshAdapter()

        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }

        return builder
    }

    private fun updateColorView(colorView: TextView, label: Label) {
        colorView.setBackgroundColor(label.colorInt())
    }

    private fun refreshAdapter() {
        (mListView.adapter as ResourceCursorAdapter).changeCursor(createCursor())
    }

    private fun createLabelRemoveDialog(label: Label): AlertDialog.Builder {
        return AlertDialog.Builder(this)
               .setTitle(label.mName)
               .setMessage(R.string.remove_label_confirmation)
               .setPositiveButton(android.R.string.ok){ dialog, _ ->
                   LabelVoc.deleteLabel(label.mID)
                   contentResolver.delete(LabelColumns.CONTENT_URI(label.mID), null, null)
                   refreshAdapter()
                    dialog.dismiss()
               }
               .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        if (v == mListView) {
            menu.add(Menu.NONE, MENU_EDIT, Menu.NONE, R.string.context_menu_edit)
            menu.add(Menu.NONE, MENU_REMOVE, Menu.NONE, R.string.context_menu_delete)
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
            R.id.menu_add -> createLabelAddEditDialog(null).create().show()
        }
        return true
    }

    companion object {
        private const val MENU_EDIT = 102

        //private const val MENU_ADD = 103;
        private const val MENU_REMOVE = 104
    }

    override fun update(p0: Observable?, p1: Any?) {
        (mListView.adapter as BaseAdapter ).notifyDataSetChanged()
    }
}

