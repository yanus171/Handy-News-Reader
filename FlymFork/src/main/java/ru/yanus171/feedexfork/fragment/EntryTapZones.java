package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsStatusBarHidden;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_ARTICLE_TAP_ENABLED_TEMP;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.UpdateTextAndVisibility;

import android.view.View;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;

public class EntryTapZones {
    private boolean mVisible = false;
    private final EntryFragment mFragment;
    private final View mRootView;

    public EntryTapZones( EntryFragment fragment ) {
        mFragment = fragment;
        mRootView = fragment.mRootView;
        Update();
        setupButtonActions();

    }
    static public void hideAll(View rootView ) {
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
    public void Update() {
        final boolean visible = mVisible;
        UpdateButton( R.id.pageUpBtn, false );
        UpdateButton( R.id.pageDownBtn, false );
        UpdateButton( R.id.brightnessSliderLeft, false );
        UpdateButton( R.id.brightnessSliderRight, false );
        UpdateButton( R.id.entryLeftBottomBtn, visible );
        UpdateButton( R.id.entryRightBottomBtn, visible );
        UpdateButton( R.id.leftTopBtn, visible );
        UpdateButton( R.id.rightTopBtn, visible );
        UpdateButton( R.id.backBtn, visible );
        UpdateButton( R.id.leftTopBtnFS, visible );
        UpdateButton( R.id.rightTopBtnFS, visible );
        UpdateButton( R.id.entryCenterBtn, visible && !mFragment.mControlPanel.isVisible() );

        final EntryView view = mFragment.GetSelectedEntryView();
        final boolean isBackBtnVisible = view != null && view.CanGoBack() && visible;
        mRootView.findViewById( R.id.backBtn ).setVisibility(isBackBtnVisible ? View.VISIBLE : View.GONE );

        if ( !isArticleTapEnabledTemp() ) {
            UpdateButton(R.id.rightTopBtn, true);
            mRootView.findViewById(R.id.rightTopBtn).setVisibility( View.VISIBLE );
        }
    }

    public void onResune() {
        UpdateTextAndVisibility(mRootView, mVisible);
    }
    public void toggleVisibility() {
        if ( mFragment.mControlPanel.isVisible() ) {
            mVisible = false;
            mFragment.mControlPanel.hide();
        } else
            mVisible = !mVisible;
        Update();
    }
    public void Hide() {
        mVisible = false;
        Update();
    }
    public void onPageScrolled() {
        Hide();
    }

    private void setupButtonActions() {
        mRootView.findViewById(R.id.backBtn).setOnClickListener(v -> mFragment.GetSelectedEntryView().GoBack() );
        mRootView.findViewById(R.id.backBtn).setOnLongClickListener(v -> {
            mFragment.GetSelectedEntryView().ClearHistoryAnchor();
            return true;
        });

        mRootView.findViewById(R.id.rightTopBtn).setOnClickListener(v -> {
            if ( PrefUtils.isArticleTapEnabledTemp() )
                mFragment.getEntryActivity().setFullScreen( GetIsStatusBarHidden(), !GetIsActionBarHidden() );
            else
                Enable();
            mFragment.mControlPanel.hide();
        });
        mRootView.findViewById(R.id.rightTopBtn).setOnLongClickListener(view -> {
            if ( PrefUtils.isArticleTapEnabledTemp() ) {
                PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, false);
                Update();
                mFragment.GetSelectedEntryView().update( true );
                UiUtils.toast( R.string.tap_actions_were_disabled );
            } else
                Enable();
            return true;
        });

    }

    private void UpdateButton(int viewID, boolean visible ) {
        UiUtils.UpdateTapZoneButton( mRootView, viewID, visible );
    }
    private void Enable() {
        PrefUtils.putBoolean(PREF_ARTICLE_TAP_ENABLED_TEMP, true );
        Update();
        mFragment.GetSelectedEntryView().update( true );
        UiUtils.toast( R.string.tap_actions_were_enabled );
    }
}
