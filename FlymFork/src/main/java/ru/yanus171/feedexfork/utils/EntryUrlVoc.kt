package ru.yanus171.feedexfork.utils

import android.net.Uri
import android.provider.BaseColumns
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.service.FetcherService
import java.util.*

object EntryUrlVoc {
    private var mIsInitialized = false
    private val mVoc = HashMap<String, Long>()

    private fun getKey(url: String ): String {
        return url.replace( "https", "" ).replace( "http", "" );
    }
    fun set(url: String, uri: Uri) {
        set( url, uri.lastPathSegment.toLong() )
    }
    fun set(url: String, id: Long ) {
        init1()
        synchronized( mVoc) {
            mVoc[getKey( url )] = id
        }
    }
    public fun remove(url: String ) {
        init1()
        synchronized(mVoc) {
            mVoc.remove(getKey( url ))
        }
    }
    public fun get(url: String ): Long? {
        init1()
        synchronized(mVoc) {
            return (if ( mVoc.containsKey( getKey( url )) )
                mVoc[getKey( url )]
            else
                null )
        }
    }

    public fun init1(){
        synchronized( mVoc ) {
            if ( mIsInitialized )
                return
        }
        Thread() {
            synchronized(mVoc) {
                val status = FetcherService.Status().Start("Reading articles url", true)
                mVoc.clear()
                val cursor = MainApplication.getContext().contentResolver.query(
                            EntryColumns.CONTENT_URI, arrayOf(BaseColumns._ID, EntryColumns.LINK),
                            null,
                            null,
                            null)
                if ( cursor != null ) {
                    while (cursor.moveToNext())
                        mVoc[getKey( cursor.getString(1) )] = cursor.getLong(0)
                    cursor.close()
                }
                FetcherService.Status().End(status)
                mIsInitialized = true;
            }
        }.start()
    }

    fun reinit() {
        synchronized( mVoc ) {
            mIsInitialized = false
        }
        init1()
    }
}