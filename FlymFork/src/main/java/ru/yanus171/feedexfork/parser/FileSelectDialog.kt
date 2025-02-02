package ru.yanus171.feedexfork.parser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import okio.use
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.utils.DebugApp
import ru.yanus171.feedexfork.utils.FileUtils
import ru.yanus171.feedexfork.utils.Theme
import ru.yanus171.feedexfork.utils.Theme.CreateDialog
import ru.yanus171.feedexfork.utils.UiUtils
import java.io.*
import java.util.*

public class FileSelectDialog(private val mAction: ActionWithFileName,
                              private val mFileExt: String,
                              private val mRequestCode: Int,
                              private val mErrorTextID: Int) {
    interface ActionWithFileName {
        fun run(activity: Activity, fileName: String?, isFileNameUri: Boolean)
    }

    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, fromDownloadsSubFolder: Boolean ) {
        if (requestCode == mRequestCode) {
            if (resultCode == Activity.RESULT_OK && data != null )
                mAction.run(activity, data.data.toString(), true)
            else
                displayCustomFilePicker(activity, fromDownloadsSubFolder)
        }
    }
    private fun displayCustomFilePicker(activity: Activity, fromDownloadsSubFolder: Boolean) {
        val builder = CreateDialog(activity)
        builder.setTitle(activity.getString( if ( fromDownloadsSubFolder ) R.string.select_file_from_subfolder else R.string.select_file ))
        val path = if ( fromDownloadsSubFolder ) File( getPublicDir(), FileUtils.SUB_FOLDER ) else getPublicDir()
        try {
            val fileNames = path.list { dir: File?, filename: String? -> File(dir, filename).isFile && File(dir, filename).extension.equals(mFileExt, ignoreCase = true) }
            builder.setItems(fileNames) { _: DialogInterface?, which: Int -> mAction.run(activity, path.toString() + File.separator + fileNames[which], false) }
            builder.show()
        } catch (unused: Exception) {
            Toast.makeText(activity, mErrorTextID, Toast.LENGTH_LONG ).show()
        }
    }


    companion object {
        fun getPublicDir(): File {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }

        fun startFilePickerIntent(activity: Activity, fileType: String, requestCode: Int) {
            val intent: Intent
            when {
                Build.VERSION.SDK_INT >= 28 -> {
                    intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                }
                else -> intent = Intent(Intent.ACTION_GET_CONTENT)
                //else -> intent = Intent(Intent.ACTION_PICK)
            }
            if (fileType.isNotEmpty()) intent.type = fileType
            activity.startActivityForResult(intent, requestCode)
        }

        @SuppressLint("Range")
        fun getFileName(uri: Uri): String? {
            var result: String? = null
            if (uri.scheme == "content") {
                MainApplication.getContext().contentResolver.query(uri, null, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result!!.lastIndexOf('/')
                if (cut != -1) {
                    result = result!!.substring(cut + 1)
                }
            }
            return result
        }
        fun copyFile( source: String, destFileName: String, isSourceUri: Boolean, context: Context ): Boolean {
            return if (isSourceUri)
                copyFile(Uri.parse(source), destFileName, context, true)
            else
                copyFile(source, destFileName, context)
        }
        // ----------------------------------------------------------------
        @SuppressLint("NewApi", "SetWorldReadable")
        fun copyFile(sourcePath: String?, destPath: String?, context: Context): Boolean {
            var result = false
            //val sd = Environment.getExternalStorageDirectory()
            //if (sd.canWrite()) {
                try {
                    val src = File(sourcePath)
                    val dest = File(destPath)
                    if (!dest.exists()) {
                        dest.createNewFile()
                    }
                    //if (src.exists()) {
                        FileInputStream(src).channel.use { inChannel -> FileOutputStream(dest).channel.use { outChannel -> result = inChannel.transferTo(0, inChannel.size(), outChannel) == inChannel.size() } }
                    //}
                    dest.setReadable(true, false)
                } catch (e: IOException) {
                    DebugApp.ShowError( null, e, context )
                }
            //}
            val s = String.format(MainApplication.getContext().getString(if (result) R.string.fileCopied else R.string.unableToCopyFile), sourcePath)
            Toast.makeText(MainApplication.getContext(), s, Toast.LENGTH_LONG).show()
            return result
        }

        // ----------------------------------------------------------------
        fun copyFile(sourceUri: Uri, destPath: String?, context: Context, showError: Boolean): Boolean {
            var result = false
            try {
                MainApplication.getContext().contentResolver.openInputStream(sourceUri).use { inputStream ->
                    BufferedReader(
                            InputStreamReader(Objects.requireNonNull(inputStream))).use { reader ->
                        val selectedFileOutPutStream: OutputStream = FileOutputStream(destPath)
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (inputStream?.read(buffer).also { length = it!! }!! > 0) {
                            selectedFileOutPutStream.write(buffer, 0, length)
                        }
                        selectedFileOutPutStream.flush()
                        selectedFileOutPutStream.close()
                        result = true
                    }
                }
            } catch (e: IOException) {
                if ( showError )
                    DebugApp.ShowError( null, e, context )
            }
            UiUtils.RunOnGuiThread {
                if ( showError )
                    Toast.makeText(MainApplication.getContext(),
                            String.format(MainApplication.getContext().getString(if (result) R.string.fileCopied else R.string.unableToCopyFile), sourceUri.toString()),
                            Toast.LENGTH_LONG).show()
            }
            return result
        }
        // ----------------------------------------------------------------
        fun copyFile(sourceUri: Uri, destUri: Uri) {
            MainApplication.getContext().contentResolver.openInputStream(sourceUri).use { inputStream ->
                BufferedReader(
                        InputStreamReader(Objects.requireNonNull(inputStream))).use { reader ->
                            val selectedFileOutPutStream: OutputStream = MainApplication.getContext().contentResolver.openOutputStream( destUri )!!
                            val buffer = ByteArray(1024)
                            var length: Int
                            while (inputStream?.read(buffer).also { length = it!! }!! > 0) {
                                selectedFileOutPutStream.write(buffer, 0, length)
                            }
                            selectedFileOutPutStream.flush()
                            selectedFileOutPutStream.close()
                        }
            }
        }

    }

}