package ru.yanus171.feedexfork.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import ru.yanus171.feedexfork.MainApplication
import ru.yanus171.feedexfork.R
import ru.yanus171.feedexfork.fragment.EntryFragment
import ru.yanus171.feedexfork.utils.Theme
import ru.yanus171.feedexfork.utils.UiUtils.SetFont

class ControlPanel( val mRootView: View, val mEntryFragment: EntryFragment ) {
    private var mView: View? = null
    private var mEntryView: EntryView? = null

    init {
        hide()
    }

    fun isVisible(): Boolean {
        return mView != null && mView!!.visibility == View.VISIBLE
    }

    fun hide() {
        mView?.visibility = View.GONE
        val titleTextView: TextView = mRootView.findViewById(R.id.title)
        titleTextView.visibility = View.GONE
    }

    @SuppressLint("InflateParams")
    fun show(entryView: EntryView ) {
        val inflater = MainApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = inflater.inflate(R.layout.control_panel, null)
        mEntryView = entryView
        val controlPanelRoot: ViewGroup = mRootView.findViewById(R.id.control_panel_root)
        controlPanelRoot.removeAllViews()
        controlPanelRoot.addView(mView)
        mView!!.visibility = View.VISIBLE
        mView!!.setBackgroundColor(Theme.GetMenuBackgroundColor())
        setupPageSeekbar()
        setupControlPanelButtonActions()
        mEntryView?.setupControlPanelButtonActions()
        val titleTextView: TextView = mRootView.findViewById(R.id.title)
        titleTextView.visibility = View.VISIBLE
        titleTextView.setBackgroundColor(Theme.GetMenuBackgroundColor())
        titleTextView.text = mEntryView!!.mTitle;
        SetFont(titleTextView, 1F);

    }

    fun setupControlPanelButtonActions() {
        setupButtonAction(R.id.btn_menu, false) {
            mEntryFragment.activity?.openOptionsMenu()
        }
    }

    fun setupButtonAction(viewId: Int, checked: Boolean, click: View.OnClickListener) {
        val btn = mView!!.findViewById<ImageButton>(viewId)
        if ( btn == null )
            return
        btn.setOnClickListener {
            click.onClick(btn)
            hide()
            mEntryFragment.mTapZones.Hide()
        }
        btn.visibility = View.VISIBLE
        if (checked)
            btn.setBackgroundColor(Theme.GetToolBarColorInt())
        else
            btn.setBackgroundResource(android.R.drawable.screen_background_dark)
    }

    private fun setupPageSeekbar() {
        val seekBar = mRootView.findViewById<SeekBar>(R.id.seekbar)
        val seekBarText = mRootView.findViewById<TextView>(R.id.seekbar_text)
        seekBarText.setTextColor( Theme.GetTextColorInt() )
        SetFont(seekBarText, 1F);
        val info = mEntryView!!.getProgressInfo()
        seekBar.setOnSeekBarChangeListener(null)
        seekBar.max = info.max
        seekBar.progress = info.progress
        updatePageSeekbarLabel(seekBar)
        setupPageSeekbarOnChangeListener(seekBar)
    }

    private fun updatePageSeekbarLabel(seekBar: SeekBar) {
        mRootView.findViewById<TextView>(R.id.seekbar_text).text =
            MainApplication.getContext().getString(R.string.page_number_from_count, seekBar.progress, seekBar.max)
    }

    private fun setupPageSeekbarOnChangeListener(seekBar: SeekBar) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                mEntryView!!.ScrollToPage(i)
                updatePageSeekbarLabel(seekBar)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
}
