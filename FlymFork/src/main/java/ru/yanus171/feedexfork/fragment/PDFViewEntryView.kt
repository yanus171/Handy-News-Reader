package ru.yanus171.feedexfork.fragment

import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.BaseActivity
import ru.yanus171.feedexfork.activity.EntryActivity
import ru.yanus171.feedexfork.parser.FeedFilters
import ru.yanus171.feedexfork.parser.FileSelectDialog
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.TITLE
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.PrefUtils
import ru.yanus171.feedexfork.view.EntryView
import ru.yanus171.feedexfork.view.StatusText

class PDFViewEntryView( activity: EntryActivity, mContainer: ViewGroup) : EntryView(activity)
{
    lateinit var mPDFView: PDFView
    init {
        var inflater  = activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE ) as LayoutInflater;
        var rootView = inflater.inflate( R.layout.pdfview, null )
        mPDFView = rootView.findViewById<PDFView>( R.id.pdfView )!!
        if ( mPDFView.parent != null )
            (mPDFView.parent as ViewGroup ).removeView( mPDFView )
        mContainer.addView( mPDFView )
        mView = mPDFView
    }
    override fun setHtml( entryId: Long,
                          articleListUri: Uri,
                          newCursor: Cursor,
                          filters: FeedFilters?,
                          isFullTextShown: Boolean,
                          forceUpdate: Boolean,
                          activity: EntryActivity ): Boolean {
        if ( mContentWasLoaded ) {
            EndStatus()
            return true
        }
        super.setHtml(entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity)
        //Dog.v( TAG, "file =" + mEntryLink )
        val title = newCursor.getString(newCursor.getColumnIndex(FeedData.EntryColumns.TITLE));

        load(title)

        return true
    }

    private fun load(title: String) {
        mPDFView.setBackgroundColor(if (PrefUtils.isImageWhiteBackground()) Color.LTGRAY else Color.BLACK )
        mPDFView.fromUri(Uri.parse(mEntryLink))
            //.pages(0, 2, 1, 3, 3, 3)
            //.enableSwipe(true) // allows to block changing pages using swipe
            .swipeHorizontal(false)
            .defaultPage(mScrollPartY.toInt())
            .enableDoubletap(false)
            .enableAntialiasing(true)
            .pageFitPolicy(FitPolicy.WIDTH)
            .spacing(5)
            .nightMode(!PrefUtils.isImageWhiteBackground())
            .onTap( object : OnTapListener {
                override fun onTap(e: MotionEvent): Boolean {
                    toggleTapZoneVisibility()
                    return true
                }
            })
            .scrollHandle(
                if (PrefUtils.isArticleTapEnabledTemp()) null else DefaultScrollHandle( mActivity, true )
            )
            .onError {
                mContentWasLoaded = true
                Status().SetError(mEntryLink, null, mEntryId.toString(), it as Exception)
                EndStatus()
            }
            .onLoad {
                mContentWasLoaded = true;
                mPDFView.jumpTo(mScrollPartY.toInt())
                //mPDFView.setPositionOffset(mScrollPartY.toFloat(), true)
                if (title.isEmpty() || title.startsWith("content://"))
                    updateTitle()
                EndStatus()
            }
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    mScrollPartY = GetViewScrollPartY()
                }
            })
    //            .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
    //            .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
    //            .pageSnap(false) // snap pages to screen boundaries
    //            .pageFling(false) // make a fling change only a single page like ViewPager
            .load()
    }

    fun extractTitle(): String? {
        var result = mPDFView.documentMeta.title
        if (result.isNotEmpty() )
            return result
        return FileSelectDialog.Companion.getFileName(Uri.parse(mEntryLink))
    }

    fun updateTitle() {
        val title = extractTitle()
        if ( title != null && title.isNotEmpty() )
            Thread {
                val values = ContentValues()
                values.put( TITLE, title )
                mActivity.contentResolver.update(FeedData.EntryColumns.CONTENT_URI( mEntryId), values, null, null )
            }.start()
    }
    override fun GetScrollY(): Int {
        return 0
    }

    override fun ScrollTo(y: Int, smooth: Boolean) {

    }

    fun ScrollTo(scrollPart: Float, smooth: Boolean) {
        if (smooth ) {
            val anim = ObjectAnimator.ofFloat( mPDFView, "positionOffset", mPDFView.positionOffset,  scrollPart )
            anim.duration = BaseActivity.PAGE_SCROLL_DURATION_MSEC.toLong()
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.start();
        } else
            mPDFView.positionOffset = scrollPart
    }

    override fun ScrollToBottom() {
        mPDFView.positionOffset = 1F
    }


    override fun GoTop() {
        mPDFView.positionOffset = 0F
    }

    override fun PageChange(delta: Int, statusText: StatusText) {
        ScrollTo( mPDFView.positionOffset + delta * getPageFloatSize(), true )
    }

    private fun getPageFloatSize() : Float {
        val pixDoc = mPDFView.getPageSize(0).height * mPDFView.zoom *  mPDFView.pageCount - mPDFView.height
        return mPDFView.height / pixDoc
    }
    override fun onResume() {
        super.onResume()
        mPDFView.jumpTo(mScrollPartY.toInt(), false )
    }
    override fun GetViewScrollPartY(): Double {
        return mPDFView.currentPage.toDouble()
    }

    override fun IsScrollAtBottom(): Boolean {
        return false
    }

    override fun getProgressInfo(statusHeight: Int): ProgressInfo? {
        return ProgressInfo()
    }

    override fun UpdateGUI() {
        load("")
    }

    override fun InvalidateContentCache() {
        mContentWasLoaded = false
    }


}