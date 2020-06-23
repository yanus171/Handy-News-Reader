package ru.yanus171.feedexfork.utils

import ru.yanus171.feedexfork.service.FetcherService
import ru.yanus171.feedexfork.utils.FileUtils.GetImagesFolder
import java.io.File
import java.util.*

object ImageFileVoc {
    private var mIsInProcess = false
    private var mIsInitialized = false
    private val mVoc = HashSet<String>()

    private fun getKey( fileName: String ): String {
        return fileName.replace( GetImagesFolder().absolutePath + "/", "" )
    }
    public fun addImageFile(fileName: String ) {
        init1()
        synchronized(mVoc) {
            mVoc.add( getKey( fileName ) )
        }
    }
    public fun removeImageFile(fileName: String ): Boolean {
        init1()
        synchronized(mVoc) {
            return if( File(GetImagesFolder(), fileName).delete() ) {
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
            else if ( !mIsInitialized )
                mIsInProcess = true
        }
        Thread() {
            synchronized(mVoc) {
                val status = FetcherService.Status().Start("Reading images folder", true)
                mVoc.clear()
                for (item in File(GetImagesFolder().path).listFiles())
                    mVoc.add(getKey( item.toString() ))
                FetcherService.Status().End(status)
            }
            synchronized(mIsInitialized) {
                mIsInitialized = true;
                mIsInProcess = false;
            }
        }.start()
    }
}