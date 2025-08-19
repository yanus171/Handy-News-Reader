package ru.yanus171.feedexfork.fragment

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.activity.EntryActivity
import ru.yanus171.feedexfork.parser.FeedFilters
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.Dog
import ru.yanus171.feedexfork.utils.FileUtils
import ru.yanus171.feedexfork.utils.FileUtils.LinkToFile
import ru.yanus171.feedexfork.view.EntryView
import ru.yanus171.feedexfork.view.StatusText
import java.io.File
import java.io.IOException
import java.net.URI

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PDFEntryView(activity: EntryActivity, val mContainer: ViewGroup) : EntryView(activity) {
    var mRenderer: PdfRenderer? = null
    val mListView = ListView(activity)
    init {
        mView = mListView
        mContainer.addView(mListView)
        mLoadTitleOnly = false
    }
    override fun setHtml( entryId: Long,
                          articleListUri: Uri,
                          newCursor: Cursor,
                          filters: FeedFilters?,
                          isFullTextShown: Boolean,
                          forceUpdate: Boolean,
                          activity: EntryActivity ): Boolean {
        super.setHtml(entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity)
        openRenderer( mEntryLink )
        mListView.adapter = ListAdapter(mRenderer)
        return true
    }

    fun openRenderer(link: String) {
        try {
            mRenderer?.close()
            mRenderer = null
            Dog.v(TAG, link)
            var fileDescriptor = MainApplication.getContext().contentResolver.openFileDescriptor( Uri.parse(link), "r" )
            mRenderer = PdfRenderer(fileDescriptor!!)
            mContentWasLoaded = true;
        } catch ( e: IOException ) {
            Status().SetError( null, null, mEntryId.toString(), e )
        }
        EndStatus()
    }

    override fun GetScrollY(): Int {
        return 0
    }

    override fun ScrollTo(y: Int, smooth: Boolean) {
    }

    override fun ScrollToBottom() {
    }

    override fun PageChange(delta: Int, statusText: StatusText?) {
    }

    override fun GetViewScrollPartY(): Double {
        return 0.0
    }

    override fun IsScrollAtBottom(): Boolean {
        return false
    }

    override fun getProgressInfo(statusHeight: Int): ProgressInfo? {
        return ProgressInfo()
    }
    override fun Destroy() {
        mRenderer?.close()
    }
    internal inner class ListAdapter(private val mRenderer: PdfRenderer?) : BaseAdapter() {
        private val mBitmapCache = SparseArray<Bitmap?>()
        override fun getCount(): Int {
            return mRenderer?.pageCount ?: 0
        }

        override fun getItem(i: Int): Any? {
            return null
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View? {
            var imageView = view as ImageView?
            if (view == null) {
                imageView = ImageView(viewGroup.context)
            }
            val bitmap = getBitmap( i )
            if ( bitmap != null )
                imageView!!.setImageBitmap( bitmap )
            return imageView

        }

        private fun getBitmap(pageIndex: Int): Bitmap? {
            var result = mBitmapCache.get(pageIndex)
            if ( result != null )
                return result
            if ( mRenderer != null ) {
                val page = mRenderer.openPage(pageIndex)!!
                result = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(result, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                mBitmapCache.put(pageIndex, result)
            }
            return result
        }
    }
}
