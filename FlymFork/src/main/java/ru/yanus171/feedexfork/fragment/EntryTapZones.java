package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.isTapActionEnabled;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.UpdateTapZonesTextAndVisibility;

import android.view.View;
import android.widget.Toast;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;

public class EntryTapZones {
    private boolean mIsTapZoneVisible = false;
    EntryFragment mFragment = null;
    private View mRootView = null;

    public EntryTapZones( EntryFragment fragment ) {
        mFragment = fragment;
        mRootView = fragment.mRootView;
        SetupZones();
        setupTapZoneButtonActions();

    }

    static public void hideAllTapZones( View rootView ) {
        UiUtils.UpdateTapZoneButton( rootView, R.id.pageUpBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.pageUpBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.pageDownBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.brightnessSliderLeft, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.brightnessSliderRight, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.entryLeftBottomBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.entryRightBottomBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.leftTopBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.rightTopBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.backBtn, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.leftTopBtnFS, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.rightTopBtnFS, false );
        UiUtils.UpdateTapZoneButton( rootView, R.id.entryCenterBtn, false );
    }
    public void SetupZones() {
        final boolean visible = mIsTapZoneVisible;
        UpdateTapZoneButton( R.id.pageUpBtn, false );
        UpdateTapZoneButton( R.id.pageDownBtn, false );
        UpdateTapZoneButton( R.id.brightnessSliderLeft, false );
        UpdateTapZoneButton( R.id.brightnessSliderRight, false );
        UpdateTapZoneButton( R.id.entryLeftBottomBtn, visible );
        UpdateTapZoneButton( R.id.entryRightBottomBtn, visible );
        UpdateTapZoneButton( R.id.leftTopBtn, visible );
        UpdateTapZoneButton( R.id.rightTopBtn, visible );
        UpdateTapZoneButton( R.id.backBtn, visible );
        UpdateTapZoneButton( R.id.leftTopBtnFS, visible );
        UpdateTapZoneButton( R.id.rightTopBtnFS, visible );
        UpdateTapZoneButton( R.id.entryCenterBtn, visible && !mFragment.mControlPanel.isVisible() );

        final EntryView view = mFragment.GetSelectedEntryView();
        final boolean isBackBtnVisible = view != null && view.CanGoBack() && visible;
        mRootView.findViewById( R.id.backBtn ).setVisibility(isBackBtnVisible ? View.VISIBLE : View.GONE );

        if ( !isArticleTapEnabledTemp() ) {
            UpdateTapZoneButton(R.id.rightTopBtn, true);
            mRootView.findViewById(R.id.rightTopBtn).setVisibility( View.VISIBLE );
        }
    }

    public void onResune() {
        UpdateTapZonesTextAndVisibility(mRootView, mIsTapZoneVisible );
    }
    public void toggleTapZoneVisibility() {
        if ( mFragment.mControlPanel.isVisible() ) {
            mIsTapZoneVisible = false;
            mFragment.mControlPanel.hide();
        } else
            mIsTapZoneVisible = !mIsTapZoneVisible;
        SetupZones();
    }
    public void hideTapZones() {
        mIsTapZoneVisible = false;
        SetupZones();
    }
    private void setupTapZoneButtonActions() {
        mRootView.findViewById(R.id.backBtn).setOnClickListener(v -> mFragment.GetSelectedEntryView().GoBack() );
        mRootView.findViewById(R.id.backBtn).setOnLongClickListener(v -> {
            mFragment.GetSelectedEntryView().ClearHistoryAnchor();
            return true;
        });

        mRootView.findViewById(R.id.rightTopBtn).setOnClickListener(v -> {
            if ( PrefUtils.isArticleTapEnabledTemp() )
                mFragment.getEntryActivity().setFullScreen( GetIsStatusBarHidden(), !GetIsActionBarHidden() );
            else
                EnableTapActions();
            mFragment.mControlPanel.hide();
        });
        mRootView.findViewById(R.id.rightTopBtn).setOnLongClickListener(view -> {
            if ( PrefUtils.isArticleTapEnabledTemp() ) {
                PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, false);
                SetupZones();
                mFragment.GetSelectedEntryView().refreshUI( true );
                UiUtils.toast( R.string.tap_actions_were_disabled );
            } else
                EnableTapActions();
            return true;
        });

    }

    private void UpdateTapZoneButton( int viewID, boolean visible ) {
        UiUtils.UpdateTapZoneButton( mRootView, viewID, visible );
    }
    private void EnableTapActions() {
        PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, true );
        SetupZones();
        mFragment.GetSelectedEntryView().refreshUI( true );
        UiUtils.toast( R.string.tap_actions_were_enabled );
    }

    public void onPageScrolled() {
        hideTapZones();
    }

    public void DisableTapActionsIfVideo(EntryView view) {
        if (view.mLoadTitleOnly)
            return;
        final boolean tapActionsEnabled;
        synchronized (this) {
            tapActionsEnabled = mFragment.GetSelectedEntryWebView().mIsFullTextShown ||
                    !PrefUtils.getBoolean("disable_tap_actions_when_video", true) ||
                    !mFragment.hasVideo();
        }

        if (tapActionsEnabled != isArticleTapEnabledTemp()) {
            PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, tapActionsEnabled);
            SetupZones();
            Toast.makeText(MainApplication.getContext(),
                    tapActionsEnabled ?
                            mFragment.getContext().getString(R.string.tap_actions_were_enabled) :
                            mFragment.getContext().getString(R.string.video_tag_found_in_article) + ". " + mFragment.getContext().getString(R.string.tap_actions_were_disabled),
                    Toast.LENGTH_LONG).show();
        }
    }


}
