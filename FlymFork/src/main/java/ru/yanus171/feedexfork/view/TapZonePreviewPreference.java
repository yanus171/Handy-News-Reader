package ru.yanus171.feedexfork.view;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        return view;
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

    public static void SetupZone(View parentView, int size, int viewID, int matchParent) {
        if ( isArticleTapEnabled() ) {
            parentView.findViewById( viewID ).setVisibility( View.VISIBLE );
            SetSize(parentView, viewID, matchParent, size);
        } else
            parentView.findViewById( viewID ).setVisibility( View.GONE );
    }


}
