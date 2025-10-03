package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.utils.PrefUtils.PREF_FORCE_ORIENTATION_BY_SENSOR;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.ControlPanel;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.WebEntryView;

class EntryOrientation {
    private final Activity mActivity;
    private final EntryFragment mEntryFragment;
    private enum Orientation {NONE, LANDSCAPE, PORTRAIT}
    private Orientation mForceOrientation = Orientation.NONE;
    private int mLastScreenState = -1;
    private MenuItem mForcePortraitOrientationMenuItem = null;
    private MenuItem mForceLandscapeOrientationMenuItem = null;

    public EntryOrientation( EntryFragment entryFragment ) {
        mEntryFragment = entryFragment;
        mActivity = entryFragment.getActivity();
    }

    public void onCreateOptionsMenu(Menu menu) {
        mForceLandscapeOrientationMenuItem = menu.findItem(R.id.menu_force_landscape_orientation_toggle);
        mForcePortraitOrientationMenuItem = menu.findItem(R.id.menu_force_portrait_orientation_toggle);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_force_landscape_orientation_toggle).setChecked(mForceOrientation == Orientation.LANDSCAPE);
        menu.findItem(R.id.menu_force_portrait_orientation_toggle).setChecked(mForceOrientation == Orientation.PORTRAIT);
    }

    @SuppressLint("NonConstantResourceId")
    public void onOptionsItemSelected(MenuItem item, Uri uri) {
        switch (item.getItemId()) {
            case R.id.menu_force_orientation_by_sensor: {
                item.setChecked(!item.isChecked());
                setOrientationBySensor(item.isChecked());
                break;
            }
            case R.id.menu_force_landscape_orientation_toggle: {
                item.setChecked(!item.isChecked());
                changeOrientation(item.isChecked() ? Orientation.LANDSCAPE : Orientation.NONE, uri);
                break;
            }
            case R.id.menu_force_portrait_orientation_toggle: {
                item.setChecked(!item.isChecked());
                changeOrientation(item.isChecked() ? Orientation.PORTRAIT : Orientation.NONE, uri);
                break;
            }
        }
    }

    public void refreshUI( EntryView view, Uri uri) {
        SetForceOrientation(ForceOrientationFromInt(view.mCursor.getInt(view.mIsLandscapePos)), uri);
        applyForceOrientation();
    }

    public boolean onConfigurationChanged(Configuration newConfig, EntryView entryView) {
        boolean result = false;
        if (newConfig.orientation != mLastScreenState ) {
            if ( mForceOrientation != Orientation.LANDSCAPE &&
                 entryView instanceof WebEntryView && ((WebEntryView) entryView).mHasScripts) {
                result = true;
            }
        }
        mLastScreenState = newConfig.orientation;
        return result;
    }

    public void onResume() {
        mLastScreenState = mActivity.getResources().getConfiguration().orientation;
        applyForceOrientation();
    }

    private Orientation ForceOrientationFromInt(int code) {
        return code == 1 ? Orientation.LANDSCAPE : code == 2 ? Orientation.PORTRAIT : Orientation.NONE;
    }

    void setupControlPanelButtonActions(ControlPanel controlPanel, Uri uri) {
        controlPanel.setupButtonAction(R.id.btn_force_landscape_orientation_toggle, mForceOrientation == Orientation.LANDSCAPE,
                v -> changeOrientation(mForceOrientation == Orientation.LANDSCAPE ? Orientation.NONE : Orientation.LANDSCAPE, uri )
        );
        controlPanel.setupButtonAction(R.id.btn_force_portrait_orientation_toggle, mForceOrientation == Orientation.PORTRAIT,
                v -> changeOrientation(mForceOrientation == Orientation.PORTRAIT ? Orientation.NONE : Orientation.PORTRAIT, uri)
        );
        controlPanel.setupButtonAction(R.id.btn_force_orientation_by_sensor, getBoolean(PREF_FORCE_ORIENTATION_BY_SENSOR, true),
                v -> {
                    PrefUtils.toggleBoolean(PREF_FORCE_ORIENTATION_BY_SENSOR, true);
                    setOrientationBySensor( PrefUtils.getBoolean(PrefUtils.PREF_FORCE_ORIENTATION_BY_SENSOR, true));
                }
        );
    }

    private void applyForceOrientation() {
        if (mForceOrientation == Orientation.NONE)
            return;

        int or = mForceOrientation == Orientation.LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE :
                mForceOrientation == Orientation.PORTRAIT ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT :
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        if (or != mActivity.getRequestedOrientation())
            mActivity.setRequestedOrientation(or);
    }

    private void setOrientationBySensor( boolean value ) {
        PrefUtils.putBoolean(PREF_FORCE_ORIENTATION_BY_SENSOR, value);
        applyForceOrientation();
    }

    private void SetForceOrientation(final Orientation forceOrientation, Uri uri) {
        if (mForceOrientation == forceOrientation)
            return;
        mForceOrientation = forceOrientation;
        //final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        ContentValues values = new ContentValues();
        values.put(FeedData.EntryColumns.IS_LANDSCAPE, ForceOrientationToInt(mForceOrientation));
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.update(uri, values, null, null);
        mEntryFragment.GetSelectedEntryView().InvalidateContentCache();
        mEntryFragment.restartCurrentEntryLoader();
        mActivity.invalidateOptionsMenu();
    }

    private void changeOrientation(Orientation orientation, Uri uri) {
        SetForceOrientation(orientation, uri);
        applyForceOrientation();
        if (orientation == Orientation.LANDSCAPE && mForcePortraitOrientationMenuItem != null)
            mForcePortraitOrientationMenuItem.setChecked(false);
        else if (orientation == Orientation.PORTRAIT && mForceLandscapeOrientationMenuItem != null)
            mForceLandscapeOrientationMenuItem.setChecked(false);
    }

    private int ForceOrientationToInt(Orientation fo) {
        return fo == Orientation.LANDSCAPE ? 1 : fo == Orientation.PORTRAIT ? 2 : 0;
    }
}
