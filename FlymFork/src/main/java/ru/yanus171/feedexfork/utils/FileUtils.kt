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

import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.widget.Toast

import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.provider.FeedData

import ru.yanus171.feedexfork.utils.DebugApp.AddErrorToLog
import java.io.*

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

    fun GetFolder(): File {
        val customPath = PrefUtils.getString(PrefUtils.CUSTOM_DATA_FOLDER, "").trim { it <= ' ' }
        var result = File(if (customPath.isEmpty()) Environment.getExternalStorageDirectory() else File(customPath), "feedex/")
        if (!result.exists())
            if (!result.mkdirs()) {
                result = File(MainApplication.getContext().cacheDir, "feedex/")
                MakeDirs(result)
            }

        return result
    }

    private fun MakeDirs(result: File) {
        if (!result.mkdirs())
            Toast.makeText(MainApplication.getContext(), "Cannot create dir: " + result.path, Toast.LENGTH_LONG).show()
    }

    fun GetImagesFolder(): File {
        if (mGetImagesFolder == null) {
            mGetImagesFolder = File(GetFolder(), "images/")
            if (!mGetImagesFolder!!.exists())
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

    fun LinkToFile(  link: String ): File {
        return File(GetHTMLFolder(), StringUtils.getMd5( link ).replace(" ", HtmlUtils.URL_SPACE) )
    }

    private lateinit var mGetHTMLFolder: File
    private fun GetHTMLFolder(): File {
        if (! ::mGetHTMLFolder.isInitialized ) {
            mGetHTMLFolder = File(GetFolder(), "html/")
            if (!mGetHTMLFolder.exists())
                MakeDirs(mGetHTMLFolder)
            try {
                val file = File("$mGetHTMLFolder/.nomedia")
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return mGetHTMLFolder
    }

    private fun readHTMLFromFile( link: String ): String {
        val contentBuilder = StringBuilder()
        try {
            val reader = BufferedReader( InputStreamReader(
                    FileInputStream( LinkToFile( link ) ), "UTF8") )
            var str: String?
            do  {
                str = reader.readLine()
                contentBuilder.append(str)
            } while ( str != null )
            reader.close()
        } catch (e: IOException) {

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
            values.put(FeedData.EntryColumns.MOBILIZED_HTML, mobilizedHtml.length)
        }
    }

    val MIN_MOBILIZED_LEN = 10

    fun loadMobilizedHTML(link: String, cursor: Cursor) : String {
        val columnIndex = cursor.getColumnIndex(FeedData.EntryColumns.MOBILIZED_HTML)
        if ( !cursor.isNull( columnIndex ) ) {
            val content: String = cursor.getString(columnIndex)
            if ( content.length > MIN_MOBILIZED_LEN )
                saveHTMLToFile( link, content )
        }
        return readHTMLFromFile( link )
    }

    fun isMobilized (link: String, cursor: Cursor, col: Int ): Boolean {
        return !cursor.isNull( col ) && cursor.getString(col).length > MIN_MOBILIZED_LEN ||
                LinkToFile( link ).exists()
    }

    fun isMobilized (link: String, cursor: Cursor ): Boolean {
        return isMobilized( link, cursor, cursor.getColumnIndex( FeedData.EntryColumns.MOBILIZED_HTML ) )
    }

    fun deleteMobilized(uri: Uri ) {
        val cr = MainApplication.getContext().contentResolver
        val cursor = cr.query(uri, arrayOf(FeedData.EntryColumns.LINK), null, null, null)
        if ( cursor.moveToFirst() )
            with ( LinkToFile(cursor.getString(0)) ) { if ( exists() ) delete() }
        cursor.close()

        val values = ContentValues()
        values.putNull(FeedData.EntryColumns.MOBILIZED_HTML)
        cr.update(uri, values, null, null)

    }
}
