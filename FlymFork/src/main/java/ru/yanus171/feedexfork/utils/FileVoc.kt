package ru.yanus171.feedexfork.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.service.FetcherService
import java.io.File
import java.util.*

class FileVoc(val mFolder: File ) {
    private var mIsInProcess = false
    private var mIsInitialized = false
    private val mVoc = HashSet<String>()

    init {

    }
    private fun getKey( fileName: String ): String {
        return fileName.replace( mFolder.absolutePath + "/", "" ).replace( "file://", "" )
    }
    fun addFile(fileName: String ) {
        init1()
        synchronized(mVoc) {
            mVoc.add( getKey( fileName ) )
        }
    }
    public fun removeFile(fileName: String ): Boolean {
        init1()
        synchronized(mVoc) {
            return if( File(mFolder, fileName).delete() ) {
                mVoc.remove(getKey( fileName ))
                true
            } else
                false
        }


    }
    public fun isExists(fileName: String ): Boolean {
        init1()
        if ( !isInitialized() )
            return File( fileName ).exists()
        synchronized(mVoc) {
            return mVoc.contains( getKey( fileName ) )
        }
    }

    private fun isInitialized(): Boolean {
        synchronized(mIsInitialized) {
            return mIsInitialized;
        }
    }
    public fun init1(){

        synchronized( mIsInitialized ) {
            if ( mIsInProcess || mIsInitialized )
                return
            else if ( ContextCompat.checkSelfPermission(MainApplication.getContext(),
                                                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED )
                return
            else if ( !mIsInitialized )
                mIsInProcess = true
        }
        Thread {
            synchronized(mVoc) {
                val status = FetcherService.Status().Start("Reading folder $mFolder", true)
                mVoc.clear()
                for (item in File(mFolder.path).listFiles())
                    mVoc.add(getKey( item.toString() ))
                FetcherService.Status().End(status)
            }
            synchronized(mIsInitialized) {
                mIsInitialized = true
                mIsInProcess = false
            }
        }.start()
    }
}