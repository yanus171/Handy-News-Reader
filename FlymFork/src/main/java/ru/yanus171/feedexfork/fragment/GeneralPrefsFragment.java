/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.service.AutoWorker;
import ru.yanus171.feedexfork.utils.Brightness;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.ColorPreference;

import static ru.yanus171.feedexfork.utils.PrefUtils.DATA_FOLDER;

public class GeneralPrefsFragment extends PreferenceFragment implements  PreferenceScreen.OnPreferenceClickListener {
    public static Boolean mSetupChanged = false;

    private final Preference.OnPreferenceChangeListener mOnRefreshChangeListener = (preference, newValue) -> {
        Activity activity = getActivity();
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= 21 )
                AutoWorker.Companion.init();
        }
        return true;
    };

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( PrefUtils.getString( DATA_FOLDER, "" ).isEmpty() )
            PrefUtils.putString( DATA_FOLDER, FileUtils.INSTANCE.GetDefaultStoragePath().getAbsolutePath() );

        addPreferencesFromResource(R.xml.general_preferences);


        Preference preference = findPreference(PrefUtils.REFRESH_ENABLED);
        preference.setOnPreferenceChangeListener(mOnRefreshChangeListener);
        preference = findPreference(PrefUtils.REFRESH_INTERVAL);
        preference.setOnPreferenceChangeListener(mOnRefreshChangeListener);

        if ( Build.VERSION.SDK_INT > 28 )
            RemovePref( "prefs_advanced", "use_standard_file_manager" );
        if ( Build.VERSION.SDK_INT > 30 )
            RemovePref( "notificationScreen", "reading_notification" );
        if ( Build.VERSION.SDK_INT >= 26 )
            RemovePref(null, "notificationScreen");
        else {
            setRingtoneSummary();
            RemovePref(null, "show_notification_setup");
        }
        findPreference(PrefUtils.THEME).setOnPreferenceChangeListener((preference1, newValue) -> {
            PrefUtils.putString(preference1.getKey(), (String) newValue);
            PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit().commit(); // to be sure all prefs are written
            Process.killProcess(Process.myPid()); // Restart the app
            // this return statement will never be reached
            return true;
        });

        findPreference(PrefUtils.LANGUAGE).setOnPreferenceChangeListener((preference12, newValue) -> {
            PrefUtils.putString(PrefUtils.LANGUAGE, (String)newValue);
            PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit().commit(); // to be sure all prefs are written
            Process.killProcess(Process.myPid()); // Restart the app
            // this return statement will never be reached
            return true;
        });


        if ( PrefUtils.getBoolean(PrefUtils.BRIGHTNESS_GESTURE_ENABLED, false ) )
            ApplyBrightness( getPreferenceScreen(), (BaseActivity) getActivity());
    }

    // -------------------------------------------------------------------------
    private void RemovePref(String keyScreen, String keyPref) {
        PreferenceScreen screen;
        if (keyScreen == null) {
            screen = getPreferenceScreen();
        } else {
            screen = (PreferenceScreen) findPreference(keyScreen);
        }

        if (screen != null) {
            Preference pref = findPreference(keyPref);
            if (pref != null) {
                screen.removePreference(pref);
            }
        }
    }

    private static void ApplyBrightness(PreferenceScreen screen, final BaseActivity activity ) {
        for ( int i = 0; i < screen.getPreferenceCount(); i++ ) {
            if (screen.getPreference(i) instanceof PreferenceScreen ||
                    screen.getPreference(i) instanceof ColorPreference)
                screen.getPreference(i).setOnPreferenceClickListener(preference -> {
                    SetupBrightness(preference, activity);
                    return false;
                });
            if (screen.getPreference(i) instanceof PreferenceScreen)
                ApplyBrightness((PreferenceScreen) screen.getPreference(i), activity);
        }
    }


    @Override
    public void onDestroy() {
        SetupChanged();
        super.onDestroy();
    }

    public static void SetupChanged() {
        mSetupChanged = true;
        UiUtils.InvalidateTypeFace();
    }

    @Override
    public void onResume() {
        // The ringtone summary text should be updated using
        // OnSharedPreferenceChangeListener(), but I can't get it to work.
        // Updating in onResume is a very simple hack that seems to work, but is inefficient.
        setRingtoneSummary();

        super.onResume();

    }

    private void setRingtoneSummary() {
        if ( Build.VERSION.SDK_INT >= 26 )
            return;
        try {
            Preference ringtone_preference = findPreference(PrefUtils.NOTIFICATIONS_RINGTONE);
            Uri ringtoneUri = Uri.parse(PrefUtils.getString(PrefUtils.NOTIFICATIONS_RINGTONE, ""));
            if (TextUtils.isEmpty(ringtoneUri.toString())) {
                ringtone_preference.setSummary(R.string.settings_notifications_ringtone_none);
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(MainApplication.getContext(), ringtoneUri);
                if (ringtone == null) {
                    ringtone_preference.setSummary(R.string.settings_notifications_ringtone_none);
                } else {
                    ringtone_preference.setSummary(ringtone.getTitle(MainApplication.getContext()));
                }
            }
        } catch ( NullPointerException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        SetupBrightness(preference, getActivity());
        return true;
    }

    private static void SetupBrightness(Preference preference, Activity activity) {
        if ( preference instanceof PreferenceScreen) {
            PreferenceScreen screen = (PreferenceScreen) preference;
            Brightness br =  ((BaseActivity) activity ).mBrightness;
            if (screen.getDialog() != null) {
                Brightness.Companion.setBrightness(br.getMCurrentAlpha(), screen.getDialog().getWindow());
            }
        }
    }
}
