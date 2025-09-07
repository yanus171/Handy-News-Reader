package ru.yanus171.feedexfork.fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.Menu
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.BaseActivity
import ru.yanus171.feedexfork.activity.EntryActivity
import ru.yanus171.feedexfork.parser.FileSelectDialog
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.SCROLL_POS
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.TITLE
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.X_OFFSET
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.ZOOM
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.PrefUtils
import ru.yanus171.feedexfork.utils.PrefUtils.PREF_ZOOM_SCROLL_ENABLED
import ru.yanus171.feedexfork.utils.UiUtils
import ru.yanus171.feedexfork.view.EntryView
import ru.yanus171.feedexfork.view.StatusText

class PDFViewEntryView(private val activity: EntryActivity, private val mContainer: ViewGroup, entryID: Long) : EntryView(activity, entryID)
{
    lateinit var mPDFView: PDFView
    var mXOffset: Float = 0.0F
    var mZoom: Float = 1.0F
    var mTitleWasUpdated = false
    var mIsLoaded = false
    var mIsBlockScroll = false

    init {
        createView()
    }

    private fun createView() {
        var inflater  = activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE ) as LayoutInflater;
        var rootView = inflater.inflate( R.layout.pdfview, null )
        mPDFView = rootView.findViewById<PDFView>(R.id.pdfView)!!
        if (mPDFView.parent != null)
            (mPDFView.parent as ViewGroup).removeView(mPDFView)
        mContainer.addView(mPDFView)
        mView = mPDFView
    }

    private fun readFloat(cursor: Cursor, fieldName: String, defaultValue: Float): Float {
        return if ( !cursor.isNull(cursor.getColumnIndex(fieldName)) )
        cursor.getFloat(cursor.getColumnIndex(fieldName)) else defaultValue
    }
    private fun readDouble(cursor: Cursor, fieldName: String, defaultValue: Double): Double {
        return if ( !cursor.isNull(cursor.getColumnIndex(fieldName)) )
            cursor.getDouble(cursor.getColumnIndex(fieldName)) else defaultValue
    }
    private fun load(title: String) {
        mContentWasLoaded = true;
        mPDFView.setBackgroundColor(if (PrefUtils.isImageWhiteBackground()) Color.LTGRAY else Color.BLACK )
        mPDFView.fromUri(Uri.parse(mEntryLink))
            //.pages(0, 2, 1, 3, 3, 3)
            //.enableSwipe(false) // allows to block changing pages using swipe
            .swipeHorizontal(false)
            .defaultPage(mScrollPartY.toInt())
            .enableDoubletap(false)
            .enableAntialiasing(true)
            //.pageFitPolicy(FitPolicy.WIDTH)
            .spacing(5)
            .nightMode(!PrefUtils.isImageWhiteBackground())
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    mScrollPartY = GetViewScrollPartY()
                }
            })
            .onPageScroll( object: OnPageScrollListener {
                override fun onPageScrolled(page: Int, positionOffset: Float) {
                    if ( !mIsLoaded )
                        return
                    mScrollPartY = GetViewScrollPartY()
                    if (!PrefUtils.getBoolean( PREF_ZOOM_SCROLL_ENABLED, true ) ) {
                        if ( !mIsBlockScroll ) {
                            mIsBlockScroll = true
                            //mPDFView.zoomTo(mZoom)
                            mPDFView.moveTo(mXOffset, mPDFView.currentYOffset)
                            mIsBlockScroll = false
                        }
                    }
                    saveState()
                }
            })
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
                Status().SetError(mEntryLink, null, mEntryId.toString(), it as Exception)
                EndStatus()
            }
            .onRender {
            }
            .onLoad {
                mIsLoaded = true
                mIsBlockScroll = true
                mPDFView.jumpTo(mScrollPartY.toInt())
                mIsBlockScroll = false
                restoreState()
                //mPDFView.setPositionOffset(mScrollPartY.toFloat(), true)
                if (title.isEmpty() || title.startsWith("content://"))
                    updateTitle()
                //refreshUI();
                EndStatus()
            }

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
        if ( mTitleWasUpdated )
            return
        mTitleWasUpdated = true
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
        //mPDFView.positionOffset = 1F
        Toast.makeText( mActivity, R.string.scroll_to_bottom_disabled_for_pdf, Toast.LENGTH_LONG ).show()
    }


    override fun GoTop() {
        //mPDFView.positionOffset = 0F
        Toast.makeText( mActivity, R.string.scroll_to_top_disabled_for_pdf, Toast.LENGTH_LONG ).show()
    }

    override fun PageChange(delta: Int, statusText: StatusText) {
        ScrollTo( mPDFView.positionOffset + delta * getPageFloatSize(), true )
    }
    private fun getScreenHeight() : Int {
        var result = mPDFView.height;
        result -= mActivity.mEntryFragment.mStatusText.GetHeight()
        return result
    }
    private fun getPageFloatSize() : Float {
        val pixDoc = mPDFView.getPageSize(0).height * mPDFView.zoom *  mPDFView.pageCount - getScreenHeight()
        return (getScreenHeight() / pixDoc) * getPageChangeMultiplier()
    }
    override fun onResume() {
        super.onResume()
        generateArticleContent( false )
    }
    override fun onStart() {
        super.onStart()
        generateArticleContent( false )
    }
    fun saveState(){
        if ( mIsBlockScroll )
            return
        if ( !PrefUtils.getBoolean( PREF_ZOOM_SCROLL_ENABLED, true ) )
            return
        mXOffset = mPDFView.currentXOffset
        mZoom = mPDFView.zoom
    }
    fun restoreState(){
        if ( !mIsLoaded )
            return
        mPDFView.zoomTo( mZoom )
        mPDFView.moveTo( mXOffset, mPDFView.currentYOffset )
    }
    override fun SaveStateToDB( values: ContentValues ){
        saveState()
        values.put( ZOOM, mZoom )
        values.put( X_OFFSET, mXOffset )
    }

    override fun onPause() {
        super.onPause()
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

    override fun refreshUI(invalidateContent: Boolean){
        super.refreshUI(invalidateContent)
        if ( !mIsLoaded )
            return
        generateArticleContent(false)
    }

    @SuppressLint("Range")
    override fun generateArticleContent(forceUpdate: Boolean) {
        if ( mContentWasLoaded ) {
            EndStatus()
            return
        }
        super.generateArticleContent(forceUpdate)
        //setHtml(entryId, articleListUri, newCursor, filters, isFullTextShown, forceUpdate, activity)
        //Dog.v( TAG, "file =" + mEntryLink )
        load(mTitle)
    }

    @SuppressLint("Range")
    override fun loadingDataFinished() {
        super.loadingDataFinished()
        mActivity.mEntryFragment.refreshUI(false)
        if ( mCursor != null && !mContentWasLoaded ) {
            mTitle = mCursor.getString(mCursor.getColumnIndex(TITLE))
            if ( mScrollPartY == -1.0 )
                mScrollPartY = readDouble( mCursor, SCROLL_POS, 0.0)
            mZoom = readFloat( mCursor, ZOOM, mZoom )
            mXOffset = readFloat( mCursor, X_OFFSET, mXOffset )
        }
        UiUtils.RunOnGuiThread(object: Runnable {
            override fun run(){
                generateArticleContent(false)
            }
        }, 1000 )
    }

    override fun onPrepareOptionsMenu(menu: Menu ) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem( R.id.menu_scroll_zoom_enabled ).isVisible = true
        menu.findItem( R.id.menu_scroll_zoom_enabled ).isChecked = PrefUtils.getBoolean( PREF_ZOOM_SCROLL_ENABLED, true )
    }
    override fun onOptionsItemSelected(item: android.view.MenuItem ) {
        super.onOptionsItemSelected(item);
        if (item.itemId == R.id.menu_scroll_zoom_enabled ) {
            saveState();
            PrefUtils.toggleBoolean( PREF_ZOOM_SCROLL_ENABLED, item.isChecked );
            mActivity.mEntryFragment.SetupZones();
            Toast.makeText( mActivity, if (item.isChecked) R.string.tap_actions_were_enabled else R.string.tap_actions_were_disabled, Toast.LENGTH_LONG ).show();
            refreshUI(true);
        }
    }
}