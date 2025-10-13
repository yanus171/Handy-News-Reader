package ru.yanus171.feedexfork.activity;

import static ru.yanus171.feedexfork.activity.HomeActivity.AppBarLayoutState.EXPANDED;
import static ru.yanus171.feedexfork.activity.HomeActivity.GetIsActionBarEntryListHidden;
import static ru.yanus171.feedexfork.activity.HomeActivity.GetIsStatusBarEntryListHidden;
import static ru.yanus171.feedexfork.view.TapZonePreviewPreference.SetupZones;

import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.EntriesListFragment;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

public class EntriesListTapActions {
    private final EntriesListFragment mEntriesFragment;
    private final HomeActivity mActivity;

    public EntriesListTapActions( EntriesListFragment fragment, HomeActivity activity) {
        mEntriesFragment = fragment;
        mEntriesFragment.mTapActions = this;
        mActivity = activity;
        TapZonePreviewPreference.SetupZones(activity.findViewById(R.id.layout_root), false);
        {
            final View.OnClickListener listener = view -> {
                activity.setFullScreen(GetIsStatusBarEntryListHidden(), !GetIsActionBarEntryListHidden());
                AppBarLayout appBar = activity.findViewById(R.id.appbar);

                if (!GetIsActionBarEntryListHidden())
                    appBar.setExpanded(true);
                mEntriesFragment.UpdateHeader();
            };
            activity.findViewById(R.id.rightTopBtn).setOnClickListener(listener);
            activity.findViewById(R.id.rightTopBtnFS).setOnClickListener(listener);
        }
        {
            final View.OnLongClickListener listener = view -> {
                mEntriesFragment.FilterByLabels();
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
            View.OnClickListener listener = (view -> mEntriesFragment.PageUpDown(-1));
            pageUpBtn.setOnClickListener(listener);
            pageUpBtnFS.setOnClickListener(listener);
        }
        {
            View.OnLongClickListener listener = mEntriesFragment.getOnPageUpLongClickListener();

            pageUpBtn.setOnLongClickListener(listener);
            pageUpBtnFS.setOnLongClickListener(listener);
        }

        activity.findViewById(R.id.pageDownBtn).setOnClickListener(view -> mEntriesFragment.PageUpDown( 1 ));
        activity.findViewById(R.id.pageDownBtn).setOnLongClickListener(mEntriesFragment.getOnPageDownLongClickListener());
        UpdateTopTapZoneVisibility();
    }

    public void UpdateTopTapZoneVisibility() {
        final boolean atTop = mEntriesFragment.atTop();
        final boolean isActionBar = !GetIsActionBarEntryListHidden();
        final boolean topBtnVisibleFullScreen = !isActionBar && !atTop;
        final boolean topBntVisibleActionBar = isActionBar && !atTop;
        SetupZones(mActivity.mRootView, false );
        UpdateTapZoneButton( R.id.pageUpBtn, topBntVisibleActionBar );
        UpdateTapZoneButton( R.id.leftTopBtn, topBntVisibleActionBar );
        UpdateTapZoneButton( R.id.rightTopBtn, topBntVisibleActionBar );
        UpdateTapZoneButton( R.id.pageUpBtnFS, topBtnVisibleFullScreen  );
        UpdateTapZoneButton( R.id.leftTopBtnFS, topBtnVisibleFullScreen );
        UpdateTapZoneButton( R.id.rightTopBtnFS, topBtnVisibleFullScreen );
        UpdateTapZoneButton( R.id.pageDownBtn, true );
        UpdateTapZoneButton( R.id.brightnessSliderLeft, true );
        UpdateTapZoneButton( R.id.brightnessSliderRight, true );
        UpdateTapZoneButton( R.id.entryLeftBottomBtn, true );
        UpdateTapZoneButton( R.id.entryRightBottomBtn, true );
        UpdateTapZoneButton( R.id.backBtn, false );
        UpdateTapZoneButton( R.id.entryCenterBtn, false );
    }


    private void UpdateTapZoneButton( int viewID, boolean enabled ) {
        UpdateTapZoneButtonEnable( mActivity.mRootView, viewID, enabled );
    }

    private static void UpdateTapZoneButtonEnable(View rootView, int ID, boolean enable) {
        TextView btn = rootView.findViewById(ID);
        if ( btn != null ) {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setVisibility( enable ? View.VISIBLE : View.GONE );
        }
    }
}
