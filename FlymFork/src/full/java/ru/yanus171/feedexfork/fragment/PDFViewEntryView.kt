package ru.yanus171.feedexfork.fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.Menu
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.LinkHandler
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.model.LinkTapEvent
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import ru.yanus171.feedexfork.Constants
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.BaseActivity
import ru.yanus171.feedexfork.fragment.EntryMenu.setVisible
import ru.yanus171.feedexfork.fragment.EntryMenu.setItemChecked
import ru.yanus171.feedexfork.fragment.EntryMenu.setItemVisible
import ru.yanus171.feedexfork.parser.FileSelectDialog
import ru.yanus171.feedexfork.provider.FeedData
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.TITLE
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.X_OFFSET
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.ZOOM
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.PrefUtils
import ru.yanus171.feedexfork.utils.PrefUtils.STATE_IMAGE_WHITE_BACKGROUND
import ru.yanus171.feedexfork.utils.PrefUtils.getBoolean
import ru.yanus171.feedexfork.utils.UiUtils
import ru.yanus171.feedexfork.view.EntryView
import ru.yanus171.feedexfork.view.WebEntryView.ShowLinkMenu
import java.util.Date

class PDFViewEntryView(private val fragment: EntryFragment, private val mContainer: ViewGroup, entryID: Long, position: Int) : EntryView(fragment, entryID, position)
{
    lateinit var mPDFView: PDFView
    var mXOffset: Float = 0.0F
    var mZoom: Float = 1.0F
    var mTitleWasUpdated = false
    var mIsLoaded = false
    var mIsBlockScroll = false
    val mRestoreZoom = RestoreZoom()
    var mLastTimeScrolled = 0L
    var mIsScrollZoomEnabled = true

    init {
        createView()
    }

    private fun createView() {
        var inflater  = getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE ) as LayoutInflater;
        var rootView = inflater.inflate( R.layout.pdfview, null )
        mPDFView = rootView.findViewById<PDFView>(R.id.pdfView)!!
        if (mPDFView.parent != null)
            (mPDFView.parent as ViewGroup).removeView(mPDFView)
        mContainer.addView(mPDFView)
        mView = mPDFView
    }

    private fun load(title: String) {
        mContentWasLoaded = true;
        mIsLoaded = false
        mPDFView.setBackgroundColor(if (PrefUtils.isImageWhiteBackground()) Color.LTGRAY else Color.BLACK )
        mPDFView.fromUri(Uri.parse(mEntryLink))
            //.pages(0, 2, 1, 3, 3, 3)
            //.enableSwipe(false) // allows to block changing pages using swipe
            .swipeHorizontal(false)
            //.defaultPage(mScrollPartY.toInt())
            .enableDoubletap(false)
            .enableAntialiasing(true)
            //.pageFitPolicy(FitPolicy.WIDTH)
            .spacing(5)
            .nightMode(!PrefUtils.isImageWhiteBackground())
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    if (mIsLoaded )
                        mScrollPartY = GetViewScrollPartY()
                }
            })
            .onPageScroll( object: OnPageScrollListener {
                override fun onPageScrolled(page: Int, positionOffset: Float) {
                    mEntryFragment.mTapZones.onPageScrolled();
                    if ( !mIsLoaded )
                        return
                    mScrollPartY = GetViewScrollPartY()
                    if ( !mIsScrollZoomEnabled ) {
                        if ( !mIsBlockScroll ) {
                            mIsBlockScroll = true
                            if ( mPDFView.zoom == mZoom )
                                mPDFView.moveTo(mXOffset, mPDFView.currentYOffset)
                            mRestoreZoom.check()
                            mIsBlockScroll = false
                        }
                    }
                    mLastTimeScrolled = Date().time
                    saveState()
                    mEntryFragment.UpdateHeader();
                }
            })
            .onTap( object : OnTapListener {
                override fun onTap(e: MotionEvent): Boolean {
                    if ( Date().time - mLastTimeScrolled > TAP_TIMEOUT )
                        mEntryFragment.mTapZones.toggleVisibility()
                    return true
                }
            })
            .scrollHandle(
                if (PrefUtils.isArticleTapEnabledTemp()) null else DefaultScrollHandle( getContext(), true )
            )
            .onError {
                Status().SetError(mEntryLink, null, mEntryId.toString(), it as Exception)
                EndStatus()
            }
            .linkHandler(
                object : LinkHandler {
                    override fun handleLinkEvent(event: LinkTapEvent) {
                        val uri = event.link.uri
                        val page = event.link.destPageIdx
                        if ( page != null )
                            showPageJumpMenu( page )
                        else if ( uri != null )
                            ShowLinkMenu( uri, "", context )
                    }

                    private fun showPageJumpMenu( page: Int  ) {
                        AlertDialog.Builder(context)
                            .setMessage( context.getString( R.string.page_jump_PDF_confirm, page ) )
                            .setPositiveButton( android.R.string.ok ) { dialog, _ -> mPDFView.jumpTo(page) }
                            .setNeutralButton( android.R.string.cancel ){ dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                }
            )
            .onRender {
            }
            .onLoad {
                if (title.isEmpty() || title.startsWith("content://"))
                    updateTitle()
                val scrollPart = mScrollPartY.toFloat()
                UiUtils.RunOnGuiThread(object: Runnable {
                    override fun run(){
                        mIsLoaded = true
                        restoreState()
                        mPDFView.positionOffset = scrollPart
                    }
                }, 0 )
                EndStatus()
            }

    //            .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
    //            .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
    //            .pageSnap(false) // snap pages to screen boundaries
    //            .pageFling(false) // make a fling change only a single page like ViewPager
            .load()
    }

    inner class RestoreZoom(){
        var mTimer = 0L
        var mIsScheduled = false
        val DELAY = 100
        var savedZoom = 1F
        var xOffset = 0F
        var mStarted = false
        fun check() {
            if (mPDFView.zoom == mZoom ) {
                savedZoom = mZoom
                xOffset = mPDFView.positionOffset
                return
            }
            if ( mTimer == 0L || Date().time - mTimer < DELAY ) {
                mStarted = true
                schedule()
            } else if ( mStarted ) {
                restoreSavedState()
                mStarted = false
            }
            mTimer = Date().time
        }

        private fun restoreSavedState() {
            mPDFView.zoomTo(savedZoom)
            mPDFView.positionOffset = xOffset
            Toast.makeText(getContext(), R.string.zoom_is_disabled, Toast.LENGTH_SHORT).show()
        }

        private fun schedule() {
            if (mIsScheduled )
                return
            mIsScheduled = true
            UiUtils.RunOnGuiThread(object: Runnable {
                override fun run(){
                    mIsScheduled = false
                    check()
                }
            }, DELAY )
        }
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
                getContext().contentResolver.update(FeedData.EntryColumns.CONTENT_URI( mEntryId), values, null, null )
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

    override fun LongClickOnBottom() {
        PrefUtils.toggleBoolean(STATE_IMAGE_WHITE_BACKGROUND, false);
        update(true);
        generateArticleContent(true);
    }


    override fun GoTop() {
        //mPDFView.positionOffset = 0F
        Toast.makeText( mEntryFragment.activity, R.string.scroll_to_top_disabled_for_pdf, Toast.LENGTH_LONG ).show()
    }

    override fun ScrollOneScreen(direction: Int) {
        ScrollTo( mPDFView.positionOffset + direction * getPageFloatSize(), true )
    }
    private fun getScreenHeight() : Int {
        var result = mPDFView.height;
        result -= mEntryFragment.mStatusText.GetHeight()
        return result
    }
    private fun getPageFloatSize() : Float {
        val pixDoc = mPDFView.getPageSize(mPDFView.pageCount - 1).height * mPDFView.zoom *  mPDFView.pageCount - getScreenHeight()
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
        if ( !mIsScrollZoomEnabled )
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
        return mPDFView.positionOffset.toDouble()
    }

    override fun IsScrollAtBottom(): Boolean {
        return false
    }

    override fun getProgressInfo(): ProgressInfo? {
        val result = ProgressInfo()
        result.max = mPDFView.pageCount
        result.progress = mPDFView.currentPage
        result.step = 1
        return result
    }

    override fun update(invalidateContent: Boolean){
        super.update(invalidateContent)
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
        mEntryFragment.update(false)
        if ( mCursor != null && !mContentWasLoaded ) {
            mZoom = readFloat( ZOOM, mZoom )
            mXOffset = readFloat( X_OFFSET, mXOffset )
            mIsScrollZoomEnabled = readBooleanWithNullTrue( EntryColumns.IS_SCROLL_ZOOM )
        }
        UiUtils.RunOnGuiThread(object: Runnable {
            override fun run(){
                generateArticleContent(false)
            }
        }, 500 )
    }

    override fun onPrepareOptionsMenu(menu: Menu ) {
        super.onPrepareOptionsMenu(menu)
        setItemVisible( menu, R.id.menu_zoom_shift_enabled, true )
        setItemChecked( menu, R.id.menu_zoom_shift_enabled, mIsScrollZoomEnabled )

        setVisible( menu, R.id.menu_labels );
        setVisible( menu, R.id.menu_reload_full_text );
        setVisible( menu, R.id.menu_cancel_refresh );
        setVisible( menu, R.id.menu_share );
        setVisible( menu, R.id.menu_zoom_shift_enabled );
    }
    override fun onOptionsItemSelected(item: android.view.MenuItem ) {
        super.onOptionsItemSelected(item);
        if (item.itemId == R.id.menu_zoom_shift_enabled )
            toggleZoomShiftEnabled()
        else if ( item.itemId == R.id.menu_share )
            share()
    }

    private fun share() {
        share( mPDFView.context, Uri.parse(mEntryLink), mTitle );
    }

    private fun toggleZoomShiftEnabled() {
        saveState();
        mIsScrollZoomEnabled = !mIsScrollZoomEnabled;
        let {
            val values = ContentValues()
            values.put(FeedData.EntryColumns.IS_SCROLL_ZOOM, if ( mIsScrollZoomEnabled )  1 else 0 );
            MainApplication.getContext().contentResolver.update(uri, values, null, null);
        }
        mEntryFragment.mTapZones.Update()
        UiUtils.toast(if (mIsScrollZoomEnabled) R.string.zoom_shift_were_enabled else R.string.zoom_shift_were_disabled)
        update(true)
    }

    override fun leftBottomBtnClick() {
        ScrollOneScreen(+1)
    }

    override fun rightBottomBtnClick() {
        ScrollOneScreen(+1)
    }

    override fun setupControlPanelButtonActions() {
        super.setupControlPanelButtonActions()
        setupButtonAction( R.id.btn_share, false) { share() }
        setupButtonAction( R.id.btn_zoom_shift_enabled, mIsScrollZoomEnabled) {
            toggleZoomShiftEnabled()
        }
    }

    override fun ScrollToPage(page: Int) {
        mPDFView.jumpTo(page)
    }
}