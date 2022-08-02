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

package ru.yanus171.feedexfork.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.GeneralPrefsFragment;
import ru.yanus171.feedexfork.parser.FileSelectDialog;
import ru.yanus171.feedexfork.service.AutoJobService;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.FontSelectPreference;

import static ru.yanus171.feedexfork.utils.Theme.GetToolBarColorInt;
import static ru.yanus171.feedexfork.view.FontSelectPreference.cAddFontFileResultCode;

public class GeneralPrefsActivity extends BaseActivity {

    @SuppressLint("StaticFieldLeak")
    public static Activity mActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.setPreferenceTheme(this);

        setContentView(R.layout.activity_general_prefs);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_settings);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable( new ColorDrawable(GetToolBarColorInt() ) );
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // -------------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        mActivity = this;
    }
    // -------------------------------------------------------------------------
    @Override
    public void onPause() {
        super.onPause();
        mActivity = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    public void onBackPressed() {
        GeneralPrefsFragment.SetupChanged();
        if (Build.VERSION.SDK_INT >= 21 )
            AutoJobService.init( this );
        Theme.ReInit();
        super.onBackPressed();

    }

    // ----------------------------------------------------------------

    FileSelectDialog mAddCustomFileSelectDialog =
        new FileSelectDialog(FontSelectPreference::addCustom, "ttf", cAddFontFileResultCode, R.string.error_add_customm_font );

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode);
        mAddCustomFileSelectDialog.onActivityResult( this, requestCode, resultCode, data, false );
    }
}
