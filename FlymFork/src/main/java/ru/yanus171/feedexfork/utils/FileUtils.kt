/*
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
 */

package ru.yanus171.feedexfork.utils

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns._ID
import android.widget.Toast
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.LINK
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.MOBILIZED_HTML
import ru.yanus171.feedexfork.service.FetcherService
import ru.yanus171.feedexfork.utils.DebugApp.AddErrorToLog
import ru.yanus171.feedexfork.view.StorageItem
import java.io.*
import java.util.*

object FileUtils {

    private var mGetImagesFolder: File? = null

    @Throws(IOException::class)
    fun copy(src: File, dst: File) {
        val inStream = FileInputStream(src)
        val outStream = FileOutputStream(dst)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()
    }

    public const val APP_SUBDIR = "feedex/"

    fun getFolder(): File {
        val customPath = PrefUtils.getString(PrefUtils.DATA_FOLDER, "").trim { it <= ' ' }
        var result = File(if (customPath.isEmpty()) GetDefaultStoragePath() else File(customPath), APP_SUBDIR)
        if ( !result.exists() && !result.mkdirs() ) {
            result = File(MainApplication.getContext().cacheDir, APP_SUBDIR)
            MakeDirs(result)
        }

        return result
    }

    public fun GetDefaultStoragePath() = Environment.getExternalStorageDirectory()

    private fun MakeDirs(result: File) {
        if ( !result.exists() && !result.mkdirs())
            Toast.makeText(MainApplication.getContext(), "Cannot create dir: " + result.path, Toast.LENGTH_LONG).show()
    }

    fun GetImagesFolder(): File {
        if (mGetImagesFolder == null) {
            mGetImagesFolder = File(getFolder(), "images/")

            MakeDirs(mGetImagesFolder!!)
            try {
                val file = File(mGetImagesFolder!!.toString() + "/.nomedia")
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        //Toast.makeText(MainApplication.getContext(), "Cannot create dir " + result.getAbsolutePath(), Toast.LENGTH_LONG ).show();
        return mGetImagesFolder as File
    }
    fun reloadPrefs() {
        mGetImagesFolder = null
        mGetHTMLFolder = null
    }

    fun LinkToFile( link: String ): File {
        return File(GetHTMLFolder(), getLinkHash( link ) )
    }

    fun getLinkHash(link: String ): String {
        return StringUtils.getMd5( link ).replace(" ", HtmlUtils.URL_SPACE);
    }

    private var mGetHTMLFolder: File? = null
    public fun GetHTMLFolder(): File {
        if ( mGetHTMLFolder == null ) {
            mGetHTMLFolder = File(getFolder(), "html/")
            MakeDirs(mGetHTMLFolder!!)
            try {
                val file = File("$mGetHTMLFolder/.nomedia")
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return mGetHTMLFolder as File
    }

    private fun readHTMLFromFile( link: String ): String {
        val contentBuilder = StringBuilder()
        try {
            val reader = BufferedReader( InputStreamReader(
                    FileInputStream( LinkToFile( link ) ), "UTF8") )
            var str: String?
            do  {
                str = reader.readLine()
                if ( str != null )
                    contentBuilder.append(str).append( "\n" )
            } while ( str != null )
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return contentBuilder.toString()
    }

    private fun saveHTMLToFile(link: String, html: String): Uri? {
        var result: Uri? = null
        val out: OutputStreamWriter
            val file = LinkToFile( link )
            try {
                out = OutputStreamWriter( FileOutputStream(file.absolutePath))
                out.use { out ->
                    out.write(html)
                }
                result = Uri.parse("file://" + file.absolutePath)
            } catch (e: Exception) {
                AddErrorToLog(null, e)
            }

        return result
    }

    fun saveMobilizedHTML(link: String, mobilizedHtml: String?, values: ContentValues) {
        values.put(FeedData.EntryColumns.LINK, link)
        if ( mobilizedHtml != null ) {
            saveHTMLToFile(link, mobilizedHtml)
            values.put(MOBILIZED_HTML, mobilizedHtml.length)
        }
    }

    private const val MIN_MOBILIZED_LEN = 10

    fun loadMobilizedHTML(link: String, cursor: Cursor?) : String {
        val status = FetcherService.Status().Start("Reading article file", true)
        try {
            if ( cursor != null ) {
                val columnIndex = cursor.getColumnIndex(MOBILIZED_HTML)
                if (!cursor.isNull(columnIndex)) {
                    val content: String = cursor.getString(columnIndex)
                    if (content.length > MIN_MOBILIZED_LEN)
                        saveHTMLToFile(link, content)
                }
            }
            return readHTMLFromFile(link)
        } finally {
            FetcherService.Status().End( status )
        }
    }

    class UpdateMob(private val mMobValue: String, private val mEntryID: Long ) : Thread() {
        override fun run() {
            val cr = MainApplication.getContext().contentResolver
            val values = ContentValues()
            values.put(MOBILIZED_HTML, mMobValue )
            cr.update(FeedData.EntryColumns.CONTENT_URI( mEntryID ), values, null, null)
        }
    }

    fun isMobilized (link: String?, cursor: Cursor?, colMob: Int, colID: Int ): Boolean {
        if ( link != null && ( cursor == null || cursor.isNull( colMob ) || cursor.getString( colMob ) == "0" ) ) {
            val file = LinkToFile( link )
            val mobValue = if ( file.exists() ) {
                "${file.length()}"
            } else {
                EMPTY_MOBILIZED_VALUE
            }

            if ( cursor != null )
                UpdateMob( mobValue, cursor.getLong( colID ) ).start()
//            object : Thread() {
//                override fun run() {
//                    cr.update(FeedData.EntryColumns.CONTENT_URI( cursor.getLong( colID ) ), values, null, null)
//                }
//            }.start()


            return mobValue != EMPTY_MOBILIZED_VALUE
        } else if ( cursor != null )
            return !cursor.isNull( colMob ) && cursor.getString( colMob ) != EMPTY_MOBILIZED_VALUE
        else
            return false
    }

    fun isMobilized (link: String?, cursor: Cursor ): Boolean {
        return isMobilized( link, cursor, cursor.getColumnIndex( MOBILIZED_HTML ), cursor.getColumnIndex( FeedData.EntryColumns._ID ) )
    }

    fun deleteMobilized(uri: Uri ) {
        val cr = MainApplication.getContext().contentResolver
        val cursor = cr.query(uri, arrayOf(_ID, LINK), null, null, null)
        if ( cursor.moveToFirst() )
            deleteMobilized(cursor.getString(1), FeedData.EntryColumns.CONTENT_URI( cursor.getString(0) ))
        cursor.close()


    }

    public fun deleteMobilizedFile(link: String) {
        with(LinkToFile(link)) { if (exists()) delete() }
    }
    public fun deleteMobilized(link: String, entryUri: Uri) {
        deleteMobilizedFile( link )
        val values = ContentValues()
        values.put(MOBILIZED_HTML, EMPTY_MOBILIZED_VALUE)
        MainApplication.getContext().contentResolver.update(entryUri, values, null, null)
    }

    public fun createStorageList(): ArrayList<StorageItem> {
        val list = ArrayList<StorageItem>()
        list += StorageItem( MainApplication.getContext().cacheDir, R.string.internalMemory )
        list += StorageItem( Environment.getExternalStorageDirectory(), R.string.externalMemory )
        if (Build.VERSION.SDK_INT >= 19)
            for (item in MainApplication.getContext().getExternalFilesDirs(null))
                if ( !item.path.startsWith( Environment.getExternalStorageDirectory().path ) )
                    list += StorageItem( item, R.string.externalMemory )
        return list
    }
    public const val EMPTY_MOBILIZED_VALUE = "EMPTY_MOB"

}
