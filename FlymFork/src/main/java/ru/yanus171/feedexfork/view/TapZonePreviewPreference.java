package ru.yanus171.feedexfork.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

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
        SetupZoneSizes(view);
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

    public static void SetupZoneSizes(View parentView) {
        final int size = GetTapZoneSize();
        SetupZone(parentView, size, R.id.pageUpBtn, MATCH_PARENT);
        SetupZone(parentView, size, R.id.pageDownBtn, MATCH_PARENT);
        SetupZone(parentView, MATCH_PARENT, R.id.pageDownBtnVert, size);
        SetupZone(parentView, MATCH_PARENT, R.id.brightnessSlider, size);
        SetupZone(parentView, size, R.id.entryPrevBtn, size);
        SetupZone(parentView, size, R.id.entryNextBtn, size);
        SetupZone(parentView, size, R.id.toggleFullScreenStatusBarBtn, size);
        SetupZone(parentView, size, R.id.toggleFullscreenBtn, size);
    }

    private static void SetupZone(View parentView, int size, int viewID, int matchParent) {
        View view = parentView.findViewById( viewID );
        if ( view != null ) {
            if (isArticleTapEnabled()) {
                view.setVisibility(View.VISIBLE);
                SetSize(parentView, viewID, matchParent, size);
            } else
                view.setVisibility(View.GONE);
        }
    }

    static public void HideTapZonesText( View rootView ) {
        final boolean tapZonesVisible = PrefUtils.getBoolean(PrefUtils.TAP_ZONES_VISIBLE, true );
        UiUtils.HideButtonText(rootView, R.id.pageDownBtnVert, true);
        UiUtils.HideButtonText(rootView, R.id.pageDownBtn, true);
        UiUtils.HideButtonText(rootView, R.id.pageUpBtn, true);
        UiUtils.HideButtonText(rootView, R.id.entryNextBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.entryPrevBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.toggleFullScreenStatusBarBtn, !tapZonesVisible);
        UiUtils.HideButtonText(rootView, R.id.toggleFullscreenBtn, !tapZonesVisible);
    }

}
