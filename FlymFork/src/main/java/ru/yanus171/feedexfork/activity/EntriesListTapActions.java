package ru.yanus171.feedexfork.activity;

import static ru.yanus171.feedexfork.activity.HomeActivity.GetIsActionBarEntryListHidden;
import static ru.yanus171.feedexfork.activity.HomeActivity.GetIsStatusBarEntryListHidden;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.SetupZones;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

public class EntriesListTapActions {
    private final EntriesListFragment mFragment;
    private final HomeActivity mActivity;

    public EntriesListTapActions( EntriesListFragment fragment, HomeActivity activity) {
        mFragment = fragment;
        mActivity = activity;
        TapZonePreviewPreference.SetupZones(activity.findViewById(R.id.layout_root), false);
        {
            final View.OnClickListener listener = view -> {
                activity.setFullScreen(GetIsStatusBarEntryListHidden(), !GetIsActionBarEntryListHidden());
                AppBarLayout appBar = activity.findViewById(R.id.appbar);

                if (!GetIsActionBarEntryListHidden())
                    appBar.setExpanded(true);
                mFragment.UpdateHeader();
            };
            activity.findViewById(R.id.rightTopBtn).setOnClickListener(listener);
            activity.findViewById(R.id.rightTopBtnFS).setOnClickListener(listener);
        }
        {
            final View.OnLongClickListener listener = view -> {
                mFragment.FilterByLabels();
                return true;
            };
            activity.findViewById(R.id.rightTopBtn).setOnLongClickListener(listener);
            activity.findViewById(R.id.rightTopBtnFS).setOnLongClickListener(listener);
        }
        {
            final View.OnClickListener listener = view -> {
                activity.setFullScreen(!GetIsStatusBarEntryListHidden(), GetIsActionBarEntryListHidden());
            };
            activity.findViewById(R.id.leftTopBtn).setOnClickListener( listener );
            activity.findViewById(R.id.leftTopBtnFS).setOnClickListener( listener );
        }

        View pageUpBtn = activity.findViewById(R.id.pageUpBtn);
        View pageUpBtnFS = activity.findViewById(R.id.pageUpBtnFS);

        {
            View.OnClickListener listener = (view -> mFragment.PageUpDown(-1));
            pageUpBtn.setOnClickListener(listener);
            pageUpBtnFS.setOnClickListener(listener);
        }
        {
            View.OnLongClickListener listener = mFragment.getOnPageUpLongClickListener();

            pageUpBtn.setOnLongClickListener(listener);
            pageUpBtnFS.setOnLongClickListener(listener);
        }

        activity.findViewById(R.id.pageDownBtn).setOnClickListener(view -> mFragment.PageUpDown( 1 ));
        activity.findViewById(R.id.pageDownBtn).setOnLongClickListener(mFragment.getOnPageDownLongClickListener());
        Update();
    }

    public void Update() {
        final boolean atTop = mFragment.atTop();
        final boolean isActionBar = !GetIsActionBarEntryListHidden();
        final boolean topBtnVisibleFullScreen = !isActionBar && !atTop;
        final boolean topBntVisibleActionBar = isActionBar && !atTop;
        SetupZones(mActivity.mRootView, false );
        UpdateButton( R.id.pageUpBtn, topBntVisibleActionBar );
        UpdateButton( R.id.leftTopBtn, topBntVisibleActionBar );
        UpdateButton( R.id.rightTopBtn, topBntVisibleActionBar );
        UpdateButton( R.id.pageUpBtnFS, topBtnVisibleFullScreen  );
        UpdateButton( R.id.leftTopBtnFS, topBtnVisibleFullScreen );
        UpdateButton( R.id.rightTopBtnFS, topBtnVisibleFullScreen );
        UpdateButton( R.id.pageDownBtn, true );
        UpdateButton( R.id.brightnessSliderLeft, true );
        UpdateButton( R.id.brightnessSliderRight, true );
        UpdateButton( R.id.entryLeftBottomBtn, true );
        UpdateButton( R.id.entryRightBottomBtn, true );
        UpdateButton( R.id.backBtn, false );
        UpdateButton( R.id.entryCenterBtn, false );
    }
    public static void hideAll(View rootView ) {
        UpdateButtonEnable( rootView, R.id.pageUpBtn, false );
        UpdateButtonEnable( rootView, R.id.leftTopBtn, false );
        UpdateButtonEnable( rootView, R.id.rightTopBtn, false );
        UpdateButtonEnable( rootView, R.id.pageUpBtnFS, false  );
        UpdateButtonEnable( rootView, R.id.leftTopBtnFS, false );
        UpdateButtonEnable( rootView, R.id.rightTopBtnFS, false );
        UpdateButtonEnable( rootView, R.id.pageDownBtn, false );
        UpdateButtonEnable( rootView, R.id.brightnessSliderLeft, false );
        UpdateButtonEnable( rootView, R.id.brightnessSliderRight, false );
        UpdateButtonEnable( rootView, R.id.entryLeftBottomBtn, false );
        UpdateButtonEnable( rootView, R.id.entryRightBottomBtn, false );
        UpdateButtonEnable( rootView, R.id.backBtn, false );
        UpdateButtonEnable( rootView, R.id.entryCenterBtn, false );
    }
    private void UpdateButton(int viewID, boolean enabled ) {
        UpdateButtonEnable( mActivity.mRootView, viewID, enabled );
    }
    private static void UpdateButtonEnable(View rootView, int ID, boolean enable) {
        TextView btn = rootView.findViewById(ID);
        if ( btn != null ) {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setVisibility( enable ? View.VISIBLE : View.GONE );
        }
    }
}
