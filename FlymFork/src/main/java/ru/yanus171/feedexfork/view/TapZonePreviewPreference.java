package ru.yanus171.feedexfork.view;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static ru.yanus171.feedexfork.utils.PrefUtils.GetTapZoneSize;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabled;
import static ru.yanus171.feedexfork.utils.UiUtils.SetSize;

public final class TapZonePreviewPreference extends DialogPreference {
    public TapZonePreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView () {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_entry, null, false);
        SetupZoneSizes(view, true);
        view.findViewById( R.id.btnEndEditing ).setVisibility( View.GONE );
        view.findViewById( R.id.progressText ).setVisibility( View.GONE );
        view.findViewById( R.id.progressBarLoader ).setVisibility( View.GONE );
        view.findViewById( R.id.statusText ).setVisibility( View.GONE );
        view.findViewById( R.id.errorText ).setVisibility( View.GONE );
        return view;
    }
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog d = (AlertDialog) getDialog();
        Button b = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setVisibility(View.GONE);
    }

    public static void SetupZoneSizes(View parentView, boolean preview) {
        final int size = GetTapZoneSize();
        SetupZone(parentView, size, R.id.pageUpBtn, MATCH_PARENT, preview);
        SetupZone(parentView, size, R.id.pageDownBtn, MATCH_PARENT, preview);
        //SetupZone(parentView, MATCH_PARENT, R.id.pageDownBtnVert, size, preview);
        SetupZone(parentView, MATCH_PARENT, R.id.brightnessSliderLeft, size, preview);
        SetupZone(parentView, MATCH_PARENT, R.id.brightnessSliderRight, size, preview);
        SetupZone(parentView, size, R.id.entryLeftBottomBtn, size, preview);
        SetupZone(parentView, size, R.id.entryRightBottomBtn, size, preview);
        SetupZone(parentView, size, R.id.leftTopBtn, size, preview);
        SetupZone(parentView, size, R.id.rightTopBtn, size, preview);
        SetupZone(parentView, size, R.id.leftTopBtnFS, size, preview);
        SetupZone(parentView, size, R.id.rightTopBtnFS, size, preview);
        if ( !preview )
            HideTapZonesText(parentView.getRootView());
    }

    private static void SetupZone(View parentView, int size, int viewID, int matchParent, boolean preview) {
        View view = parentView.findViewById( viewID );
        if ( view != null ) {
            if (preview || isArticleTapEnabled() || viewID == R.id.rightTopBtn) {
                view.setVisibility(View.VISIBLE);
                SetSize(parentView, viewID, matchParent, size);
            } else
                view.setVisibility(View.GONE);
        }
    }

    static public void HideTapZonesText( View rootView ) {
        final boolean tapZonesVisible = PrefUtils.getBoolean(PrefUtils.TAP_ZONES_VISIBLE, true );
        //UiUtils.HideButtonText(rootView, R.id.pageDownBtnVert, true);
        UiUtils.HideButtonText(rootView, R.id.pageDownBtn, true);
        UiUtils.HideButtonText(rootView, R.id.pageUpBtn, true);
        UiUtils.HideButtonText(rootView, R.id.pageUpBtnFS, true);
        UiUtils.HideButtonText(rootView, R.id.entryRightBottomBtn, !tapZonesVisible || isArticleTapEnabled() && !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.entryLeftBottomBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.leftTopBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.rightTopBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.leftTopBtnFS, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.rightTopBtnFS, !tapZonesVisible);
    }

}
