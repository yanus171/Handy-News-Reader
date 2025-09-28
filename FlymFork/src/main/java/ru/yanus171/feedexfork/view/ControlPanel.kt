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
import ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.LANDSCAPE
import ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.NONE
import ru.yanus171.feedexfork.fragment.EntryFragment.ForceOrientation.PORTRAIT
import ru.yanus171.feedexfork.utils.PrefUtils
import ru.yanus171.feedexfork.utils.PrefUtils.PREF_FORCE_ORIENTATION_BY_SENSOR
import ru.yanus171.feedexfork.utils.PrefUtils.getBoolean
import ru.yanus171.feedexfork.utils.Theme

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
    }

    fun setupControlPanelButtonActions() {
        setupButtonAction(R.id.btn_menu, false) {
            mEntryFragment.activity?.openOptionsMenu()
        }
        setupButtonAction(R.id.btn_force_landscape_orientation_toggle, mEntryFragment.mForceOrientation == LANDSCAPE) {
            mEntryFragment.changeOrientation( if (mEntryFragment.mForceOrientation == LANDSCAPE) { NONE } else { LANDSCAPE } )
        }
        setupButtonAction(R.id.btn_force_portrait_orientation_toggle, mEntryFragment.mForceOrientation == PORTRAIT) {
            mEntryFragment.changeOrientation( if (mEntryFragment.mForceOrientation == PORTRAIT) { NONE } else { PORTRAIT } )
        }
        setupButtonAction(R.id.btn_force_orientation_by_sensor, getBoolean(PREF_FORCE_ORIENTATION_BY_SENSOR, true)) {
            PrefUtils.toggleBoolean(PREF_FORCE_ORIENTATION_BY_SENSOR, true)
            mEntryFragment.setOrientationBySensor(getBoolean(PREF_FORCE_ORIENTATION_BY_SENSOR, true))
        }
    }

    fun setupButtonAction(viewId: Int, checked: Boolean, click: View.OnClickListener) {
        val btn = mView!!.findViewById<ImageButton>(viewId)
        if ( btn == null )
            return
        btn.setOnClickListener {
            click.onClick(btn)
            mEntryFragment.mControlPanel.hide()
            mEntryFragment.hideTapZones()
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
