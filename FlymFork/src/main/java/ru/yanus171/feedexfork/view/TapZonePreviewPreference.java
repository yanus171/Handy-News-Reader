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
import android.widget.TextView;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static ru.yanus171.feedexfork.activity.HomeActivity.GetIsActionBarEntryListHidden;
import static ru.yanus171.feedexfork.utils.PrefUtils.GetTapZoneSize;
import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_TAP_ENABLED;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabled;
import static ru.yanus171.feedexfork.utils.PrefUtils.isArticleTapEnabledTemp;
import static ru.yanus171.feedexfork.utils.UiUtils.SetSize;

public final class TapZonePreviewPreference extends DialogPreference {
    public TapZonePreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView () {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_entry, null, false);
        SetupZones( view, true );
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

    public static void SetupZones(View parentView, boolean showText) {
        final int size = GetTapZoneSize();
        SetupZone(parentView, R.id.pageUpBtn, MATCH_PARENT, size, showText);
        SetupZone(parentView, R.id.pageUpBtnFS, MATCH_PARENT, size, showText);
        SetupZone(parentView, R.id.pageDownBtn, MATCH_PARENT, size, showText);
        SetupZone(parentView, R.id.brightnessSliderLeft, size, MATCH_PARENT, showText);
        SetupZone(parentView, R.id.brightnessSliderRight, size, MATCH_PARENT, showText);
        SetupZone(parentView, R.id.entryLeftBottomBtn, size, size, showText );
        SetupZone(parentView, R.id.entryRightBottomBtn, size, size, showText);
        SetupZone(parentView, R.id.leftTopBtn, size, size, showText);
        SetupZone(parentView, R.id.rightTopBtn, size, size, showText);
        SetupZone(parentView, R.id.backBtn, size, size, showText);
        SetupZone(parentView, R.id.leftTopBtnFS, size, size, showText);
        SetupZone(parentView, R.id.rightTopBtnFS, size, size, showText);
        SetupZone(parentView, R.id.entryCenterBtn, size, size, showText);
    }

    public static boolean IsZoneEnabled(int viewID, boolean preview, boolean isArticleList) {
        if ( isArticleList && viewID == R.id.backBtn)
            return false;
        if ( !isArticleList && viewID == R.id.rightTopBtn && !isArticleTapEnabledTemp() )
            return true;
        boolean hideByFullScreenMode = false;
        if ( isArticleList ) {
            if ( viewID == R.id.leftTopBtn || viewID == R.id.rightTopBtn || viewID == R.id.pageUpBtn )
                hideByFullScreenMode = GetIsActionBarEntryListHidden();
            else if ( viewID == R.id.leftTopBtnFS || viewID == R.id.rightTopBtnFS || viewID == R.id.pageUpBtnFS )
                hideByFullScreenMode = !GetIsActionBarEntryListHidden();
        }
        return !hideByFullScreenMode &&
               ( preview ||
                 isArticleList && getBoolean( PREF_TAP_ENABLED, true ) ||
                 !isArticleList && isArticleTapEnabled() );
    }
    private static void SetupZone(View parentView, int viewID, int width, int height, boolean showText) {
        TextView view = parentView.findViewById(viewID );
        if ( view == null )
            return;
        SetSize(parentView, viewID, width, height);
        if ( !showText )
            view.setText( "" );
    }

    static public void UpdateTapZonesTextAndVisibility( View rootView, boolean visible ) {
        UiUtils.UpdateButtonVisibility(rootView, R.id.pageDownBtn, false);
        UiUtils.UpdateButtonVisibility(rootView, R.id.pageUpBtn, false);
        UiUtils.UpdateButtonVisibility(rootView, R.id.pageUpBtnFS, false);
        UiUtils.UpdateButtonVisibility(rootView, R.id.entryRightBottomBtn, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.entryLeftBottomBtn, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.backBtn, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.leftTopBtn, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.rightTopBtn, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.leftTopBtnFS, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.rightTopBtnFS, visible);
        UiUtils.UpdateButtonVisibility(rootView, R.id.entryCenterBtn, visible);
    }

}
