package ru.yanus171.feedexfork.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.fragment.EntryMenu.setItemChecked
import ru.yanus171.feedexfork.fragment.EntryMenu.setItemVisible
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.X_OFFSET
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns.ZOOM
import ru.yanus171.feedexfork.utils.UiUtils
import ru.yanus171.feedexfork.view.EntryView
import ru.yanus171.feedexfork.view.EntryView.TAP_TIMEOUT
import java.lang.reflect.Method
import java.util.Date

@SuppressLint("ClickableViewAccessibility")
class PDFViewScrollAndPosition(val mPDFView: PDFView) {
    private var mXOffset: Float = 0.0F
    private var mZoom: Float = 1.0F
    private var mIsScrollZoomEnabled = true
    private var mIsTouching = false
    private var mDragPinchManager: Any? = null
    private var mOnTouchMethod: Method? = null
    private var mIsBlockScroll = false
    private var mLastTimeScrolled = 0L
    private val mRestoreZoom = RestoreZoom()
    init {
        mDragPinchManager = getDragPinchManager(mPDFView)
        mOnTouchMethod = getOnTouchMethod(mDragPinchManager)

        mPDFView.setOnTouchListener { view, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> mIsTouching = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mIsTouching = false
                    if (!mIsScrollZoomEnabled) {
                        mRestoreZoom.forceStart()
                        restoreXOffset()
                        restoreZoomIfNeeded()
                    }
                }

            }
            var result = false
            if (mOnTouchMethod != null) {
                result = mOnTouchMethod!!.invoke(mDragPinchManager, mPDFView, event) as? Boolean == true
            }
            result
        }
    }
    fun onPageScroll() {
        restoreZoomIfNeeded()
        mLastTimeScrolled = Date().time
        saveState()
    }

    private fun getDragPinchManager(pdfView: PDFView): Any? {
        return try {
            val field = PDFView::class.java.getDeclaredField("dragPinchManager")
            field.isAccessible = true
            field.get(pdfView)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveState() {
        if (mIsBlockScroll)
            return
        if (!mIsScrollZoomEnabled)
            return
        mXOffset = mPDFView.currentXOffset
        mZoom = mPDFView.zoom
    }

    fun restoreState() {
        mPDFView.zoomTo(mZoom)
        restoreXOffset()
    }

    private fun restoreXOffset() {
        mPDFView.moveTo(mXOffset, mPDFView.currentYOffset)
    }

    fun saveStateToDB(values: ContentValues) {
        saveState()
        values.put(ZOOM, mZoom)
        values.put(X_OFFSET, mXOffset)
    }

    fun readDataFromDB( view: EntryView ) {
        mZoom = view.readFloat(ZOOM, mZoom)
        mXOffset = view.readFloat(X_OFFSET, mXOffset)
        mIsScrollZoomEnabled = view.readBooleanWithNullTrue(EntryColumns.IS_SCROLL_ZOOM)
    }

    private fun getOnTouchMethod(mDragPinchManager: Any?): Method {
        val method = mDragPinchManager!!.javaClass.getDeclaredMethod(
            "onTouch",
            View::class.java,
            MotionEvent::class.java
        )
        method.isAccessible = true
        return method
    }

    fun restoreZoomIfNeeded() {
        if (!mIsScrollZoomEnabled) {
            if (!mIsBlockScroll) {
                mIsBlockScroll = true
                if (mPDFView.zoom == mZoom)
                    restoreXOffset()
                mRestoreZoom.check()
                mIsBlockScroll = false
            }
        }
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        setItemVisible( menu, R.id.menu_zoom_shift_enabled, true )
        setItemChecked( menu, R.id.menu_zoom_shift_enabled, mIsScrollZoomEnabled )
    }

    fun toggleZoomShiftEnabled( entryUri: Uri ) {
        saveState()
        mIsScrollZoomEnabled = !mIsScrollZoomEnabled
        let {
            val values = ContentValues()
            values.put(EntryColumns.IS_SCROLL_ZOOM, if ( mIsScrollZoomEnabled )  1 else 0 )
            MainApplication.getContext().contentResolver.update(entryUri, values, null, null)
        }
        UiUtils.toast(if (mIsScrollZoomEnabled) R.string.zoom_shift_were_enabled else R.string.zoom_shift_were_disabled)
    }

    fun setupControlPanelButtonActions(view: EntryView) {
        view.setupButtonAction( R.id.btn_zoom_shift_enabled, mIsScrollZoomEnabled) {
            toggleZoomShiftEnabled( view.uri )
        }
    }

    fun isTapTimeout(): Boolean {
        return Date().time - mLastTimeScrolled > TAP_TIMEOUT
    }

    inner class RestoreZoom() {
        var mTimer = 0L
        var mIsScheduled = false
        val DELAY = 100
        var savedZoom = 1F
        var xOffset = 0F
        var mStarted = false
        fun check() {
            if (mPDFView.zoom == mZoom || mIsTouching) {
                savedZoom = mZoom
                xOffset = mPDFView.positionOffset
                return
            }
            if (mTimer == 0L || Date().time - mTimer < DELAY) {
                mStarted = true
                schedule()
            } else if (mStarted) {
                restoreSavedState()
                mStarted = false
            }
            mTimer = Date().time
        }

        fun forceStart() {
            mStarted = true
        }

        fun restoreSavedState() {
            mPDFView.zoomTo(savedZoom)
            mPDFView.positionOffset = xOffset
            Toast.makeText(mPDFView.context, R.string.zoom_is_disabled, Toast.LENGTH_SHORT).show()
        }

        private fun schedule() {
            if (mIsScheduled)
                return
            mIsScheduled = true
            UiUtils.RunOnGuiThread(object : Runnable {
                override fun run() {
                    mIsScheduled = false
                    check()
                }
            }, DELAY)
        }
    }
}