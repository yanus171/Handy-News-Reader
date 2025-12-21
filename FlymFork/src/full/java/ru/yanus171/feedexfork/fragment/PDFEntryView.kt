package ru.yanus171.feedexfork.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.graphics.Matrix
import android.os.Build
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.Dog
import ru.yanus171.feedexfork.view.EntryView
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PDFEntryView(fragment: EntryFragment, mContainer: ViewGroup, entryId: Long, position: Int) : EntryView(fragment, entryId, position) {
    var mRenderer: PdfRenderer? = null
    val mListView = ListView(getContext())
    init {
        mView = mListView
        mContainer.addView(mListView)
    }

    override fun generateArticleContent(forceUpdate: Boolean) {
        super.generateArticleContent(forceUpdate)
        //super.setHtml(entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity)
        openRenderer( mEntryLink )
        mListView.adapter = ListAdapter(mRenderer)
    }

    override fun leftBottomBtnClick() {
        ScrollOneScreen(+1)
    }

    override fun rightBottomBtnClick() {
        ScrollOneScreen(+1)
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

    override fun LongClickOnBottom() {
    }

    override fun ScrollOneScreen(direction: Int) {
    }

    override fun GetViewScrollPartY(): Double {
        return 0.0
    }

    override fun IsScrollAtBottom(): Boolean {
        return false
    }

    override fun getProgressInfo(): ProgressInfo? {
        return ProgressInfo()
    }

    override fun Destroy() {
        mRenderer?.close()
    }

    override fun ScrollToPage(page: Int) {
        //TODO("Not yet implemented")
    }

    internal inner class ListAdapter(private val mRenderer: PdfRenderer?) : BaseAdapter() {
        private val mBitmapCache = SparseArray<Bitmap?>()
        val mScaleDetector = ScaleGestureDetector( mListView.context, ScaleListener() )
        var mScaleFactor: Float = 3F

        internal inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener(){
            override fun onScale( detector: ScaleGestureDetector ): Boolean {
                mScaleFactor *= detector.scaleFactor;
                mBitmapCache.clear()
                notifyDataSetInvalidated()
                Toast.makeText( mListView.context, "onScale ${mScaleFactor}", Toast.LENGTH_SHORT ).show()
                return true;
            }


        }


        override fun getCount(): Int {
            return mRenderer?.pageCount ?: 0
        }

        override fun getItem(i: Int): Any? {
            return null
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View? {
            var imageView = view as ImageView?
            if (view == null) {
                imageView = ImageView(viewGroup.context)
                imageView.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        return mScaleDetector.onTouchEvent(event)
                    }
                })
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
                result = Bitmap.createBitmap(mListView.width,
                    (mListView.width * (page.height.toFloat() / page.width.toFloat())).toInt(), Bitmap.Config.ARGB_8888)
                val matrix = Matrix();
                matrix.setScale( mScaleFactor, mScaleFactor )
                page.render(result, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                mBitmapCache.put(pageIndex, result)
            }
            return result
        }
    }
}
