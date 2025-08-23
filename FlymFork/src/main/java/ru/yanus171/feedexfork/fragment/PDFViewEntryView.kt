package ru.yanus171.feedexfork.fragment

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.barteksc.pdfviewer.PDFView
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.EntryActivity
import ru.yanus171.feedexfork.parser.FeedFilters
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.PrefUtils.getBoolean
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
        super.setHtml(entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity)
        //Dog.v( TAG, "file =" + mEntryLink )
        mPDFView.setBackgroundColor(Color.LTGRAY)
        mPDFView.fromUri( Uri.parse(mEntryLink) )
        //.pages(0, 2, 1, 3, 3, 3)
        //.enableSwipe(true) // allows to block changing pages using swipe
        .swipeHorizontal(false)
        .defaultPage( mScrollPartY.toInt() )
        //.enableDoubletap(true)
        .defaultPage(mScrollPartY.toInt())
        //.enableDoubletap(true)
        .enableAntialiasing(true)
        .onError{
            Status().SetError( null, null, mEntryId.toString(), it as Exception )
            EndStatus()
        }
        .onLoad {
            mContentWasLoaded = true;
            mPDFView.jumpTo( mScrollPartY.toInt() )
            //mPDFView.setPositionOffset(mScrollPartY.toFloat(), true)
            EndStatus()
        }


//            .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
//            .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
//            .pageSnap(false) // snap pages to screen boundaries
//            .pageFling(false) // make a fling change only a single page like ViewPager
//            .nightMode(false)
        .load()
        //EndStatus()
        return true
    }

    override fun GetScrollY(): Int {
        return 0
    }

    override fun ScrollTo(y: Int, smooth: Boolean) {
        //mPDFView.scrollTo( 0, y );
    }

    override fun ScrollToBottom() {

    }

    override fun PageChange(delta: Int, statusText: StatusText) {
        mScrollPartY = mScrollPartY + delta
        mPDFView.jumpTo(mScrollPartY.toInt(), true)
        //mPDFView.moveTo(mPDFView.currentXOffset, mPDFView.currentYOffset - delta * mPDFView.height.toFloat(), true);
        //mPDFView.setPositionOffset(mPDFView.positionOffset + getPageFloatSize() )
        //mPDFView.moveTo(0F, (delta * (mPDFView.height - statusText.GetHeight())).toFloat())
//        ScrollTo(((mPDFView.scrollY + delta * (mPDFView.height - statusText.GetHeight()) *
//                (if (getBoolean("page_up_down_90_pct", false) ) 0.9 else 0.98)).toInt()), true);
    }

    private fun getPageFloatSize() : Float {
        return 1F / mPDFView.pageCount
    }

    override fun onResume() {
        super.onResume()
        mPDFView.jumpTo(mScrollPartY.toInt(), false );
    }
    override fun GetViewScrollPartY(): Double {
        return mPDFView.positionOffset.toDouble()
    }

    override fun IsScrollAtBottom(): Boolean {
        return false
    }

    override fun getProgressInfo(statusHeight: Int): ProgressInfo? {
        return ProgressInfo()
    }

}