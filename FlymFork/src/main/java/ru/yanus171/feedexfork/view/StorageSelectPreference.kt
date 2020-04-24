package ru.yanus171.feedexfork.view

import android.app.AlertDialog
import android.content.Context
import android.os.StatFs
import android.util.AttributeSet
import android.view.Gravity
import android.widget.*
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.utils.FileUtils
import ru.yanus171.feedexfork.utils.FileUtils.APP_SUBDIR
import ru.yanus171.feedexfork.utils.FileUtils.GetDefaultStoragePath
import ru.yanus171.feedexfork.utils.PrefUtils
import java.io.File


class StorageItem (val mPath: File, private val mCaptionID: Int  ) {
    init {
        val appDir = File ( mPath, APP_SUBDIR )
        if ( mPath.exists() && !appDir.exists() )
            appDir.mkdir()
    }
    private val BYTES_IN_GIGABYTE =  1024.0 * 1024.0 * 1024.0
    fun getCaption(): String {
        return try {
            val stat = StatFs(mPath.path)
            val blockSize = stat.blockSize.toLong()
            val allSpace =  String.format("%.1f", stat.blockCount.toLong() * blockSize / BYTES_IN_GIGABYTE )
            val freeSpace = String.format("%.1f", stat.availableBlocks.toLong() * blockSize / BYTES_IN_GIGABYTE )
            val caption = MainApplication.getContext().getString( mCaptionID )
            "$caption ( ${freeSpace}G / ${allSpace}G )"
        } catch (e: IllegalArgumentException) {
            "Error"
        } catch (e: Exception) {
            "Error"
        }
    }
}

class StorageSelectPreference(context: Context?, attrs: AttributeSet?) : AutoSummaryEditPreference(context, attrs) {




    override fun onClick() {

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val scroll = ScrollView(context)
        layout.addView(scroll, WRAP_CONTENT, WRAP_CONTENT )

        val scrollLayout = RadioGroup(context)
        scroll.addView(scrollLayout, WRAP_CONTENT, WRAP_CONTENT)
        scrollLayout.orientation = LinearLayout.VERTICAL
        val storageList = FileUtils.createStorageList()
        var rbList = emptyArray<RadioButton>()
        val value = PrefUtils.getString( key, GetDefaultStoragePath().absolutePath )
        for (item in storageList) {

            val radioButton = RadioButton( context )
            scrollLayout.addView( radioButton, WRAP_CONTENT, WRAP_CONTENT )
            radioButton.text = item.getCaption()
            radioButton.tag = item.mPath.absolutePath
            if ((radioButton.tag as String? ).equals( value) )
                radioButton.isChecked = true
            rbList += radioButton

            val textView = TextView( context )
            scrollLayout.addView( textView, WRAP_CONTENT, WRAP_CONTENT )
            textView.setPadding(0, 0, 0, 20)

            textView.text = item.mPath.absolutePath
        }

        AlertDialog.Builder(context).setView(layout).setPositiveButton(android.R.string.ok) { dialog, _ ->
            for ( item in rbList )
                if ( item.isChecked ) {
                    text = item.tag as String?
                    PrefUtils.putStringCommit( key, text )
                    FileUtils.reloadPrefs()
                    break
                }
            dialog.dismiss()
        }.setNegativeButton(android.R.string.cancel, null).create().show()
    }
}

    
