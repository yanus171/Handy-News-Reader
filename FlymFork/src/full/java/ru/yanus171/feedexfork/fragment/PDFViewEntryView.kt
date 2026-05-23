package ru.yanus171.feedexfork.fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.Menu
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.LinkHandler
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.model.LinkTapEvent
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.activity.BaseActivity
import ru.yanus171.feedexfork.fragment.EntryMenu.setVisible
import ru.yanus171.feedexfork.parser.FileSelectDialog
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.TITLE
import ru.yanus171.feedexfork.service.FetcherService.Status
import ru.yanus171.feedexfork.utils.PrefUtils
import ru.yanus171.feedexfork.utils.PrefUtils.STATE_IMAGE_WHITE_BACKGROUND
import ru.yanus171.feedexfork.utils.UiUtils
import ru.yanus171.feedexfork.view.EntryView
import ru.yanus171.feedexfork.view.WebEntryView.ShowLinkMenu

class PDFViewEntryView(fragment: EntryFragment, private val mContainer: ViewGroup, entryID: Long, position: Int) : EntryView(fragment, entryID, position)
{
    lateinit var mPDFView: PDFView
    var mTitleWasUpdated = false
    var mIsLoaded = false
    val mScrollAndPosition: PDFViewScrollAndPosition
    init {
        createView()
        mScrollAndPosition = PDFViewScrollAndPosition(mPDFView)
    }

    private fun createView() {
        var inflater  = context.getSystemService( Context.LAYOUT_INFLATER_SERVICE ) as LayoutInflater
        var rootView = inflater.inflate( R.layout.pdfview, null )
        mPDFView = rootView.findViewById<PDFView>(R.id.pdfView)!!
        if (mPDFView.parent != null)
            (mPDFView.parent as ViewGroup).removeView(mPDFView)
        mContainer.addView(mPDFView)
        mView = mPDFView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun load(title: String) {
        mContentWasLoaded = true
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
                    mEntryFragment.mTapZones.onPageScrolled()
                    if ( !mIsLoaded )
                        return
                    mEntryFragment.mControlPanel.hide()
                    mScrollPartY = GetViewScrollPartY()
                    mScrollAndPosition.onPageScroll()
                    mEntryFragment.UpdateHeader()
                }


            })
            .onTap( object : OnTapListener {
                override fun onTap(e: MotionEvent): Boolean {
                    if ( mScrollAndPosition.isTapTimeout()  )
                        mEntryFragment.mTapZones.toggleVisibility()
                    return true
                }
            })
            .scrollHandle(
                if (PrefUtils.isArticleTapEnabledTemp()) null else DefaultScrollHandle(context, true )
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
                        mScrollAndPosition.restoreState()
                        mPDFView.positionOffset = scrollPart
                        mEntryFragment.UpdateHeader()
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
                context.contentResolver.update(EntryColumns.CONTENT_URI( mEntryId), values, null, null )
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
            anim.start()
        } else
            mPDFView.positionOffset = scrollPart
        SaveScrollPos()
    }

    override fun longClickOnBottom() {
        PrefUtils.toggleBoolean(STATE_IMAGE_WHITE_BACKGROUND, false)
        update(true)
    }


    override fun goTop() {
        //mPDFView.positionOffset = 0F
        Toast.makeText( mEntryFragment.activity, R.string.scroll_to_top_disabled_for_pdf, Toast.LENGTH_LONG ).show()
    }

    override fun scrollOneScreen(direction: Int) {
        ScrollTo( mPDFView.positionOffset + direction * getPageFloatSize(), true )
    }
    private fun getScreenHeight() : Int {
        var result = mPDFView.height
        result -= mEntryFragment.mStatusText.GetHeight()
        return result
    }
    private fun getPageFloatSize() : Float {
        val pixDoc = mPDFView.getPageSize(mPDFView.pageCount - 1).height * mPDFView.zoom *  mPDFView.pageCount - getScreenHeight()
        return (getScreenHeight() / pixDoc) * getPageChangeMultiplier()
    }
    override fun onResume() {
        super.onResume()
        generateArticleContent()
    }
    override fun onStart() {
        super.onStart()
        generateArticleContent()
    }


    override fun SaveStateToDB( values: ContentValues ){
        mScrollAndPosition.saveStateToDB(values)
    }
    override fun readDataFromDB() {
        super.readDataFromDB()
        mScrollAndPosition.readDataFromDB( this )
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

    override fun update(isGenerateArticleContent: Boolean){
        super.update(mIsLoaded)
    }

    @SuppressLint("Range")
    override fun generateArticleContent() {
        if ( mContentWasLoaded ) {
            EndStatus()
            return
        }
        if ( mIsLoaded )
            return
        super.generateArticleContent()
        load(mTitle)
    }

    override fun InvalidateContentCache() {
        super.InvalidateContentCache()
        mIsLoaded = false
    }

    @SuppressLint("Range")
    override fun loadingDataFinished() {
        super.loadingDataFinished()
        UiUtils.RunOnGuiThread(object: Runnable {
            override fun run(){
                generateArticleContent()
            }
        }, 500 )
    }

    override fun onPrepareOptionsMenu(menu: Menu ) {
        super.onPrepareOptionsMenu(menu)
        mScrollAndPosition.onPrepareOptionsMenu( menu )

        setVisible( menu, R.id.menu_labels )
        setVisible( menu, R.id.menu_reload_full_text )
        setVisible( menu, R.id.menu_cancel_refresh )
        setVisible( menu, R.id.menu_share )
        setVisible( menu, R.id.menu_zoom_shift_enabled )
    }
    override fun onOptionsItemSelected(item: android.view.MenuItem ) {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.menu_zoom_shift_enabled )
            toggleZoomShiftEnabled()
        else if ( item.itemId == R.id.menu_share )
            share()
    }

    private fun share() {
        share( mPDFView.context, Uri.parse(mEntryLink), mTitle )
    }

    private fun toggleZoomShiftEnabled() {
        mScrollAndPosition.toggleZoomShiftEnabled( uri )
        mEntryFragment.mTapZones.Update()
        update(false)
    }

    override fun leftBottomBtnClick() {
        scrollOneScreen(+1)
    }

    override fun rightBottomBtnClick() {
        scrollOneScreen(+1)
    }

    override fun setupControlPanelButtonActions() {
        super.setupControlPanelButtonActions()
        setupButtonAction( R.id.btn_share, false) { share() }
        mScrollAndPosition.setupControlPanelButtonActions( this )
    }

    override fun ScrollToPage(page: Int) {
        mPDFView.jumpTo(page)
    }
}