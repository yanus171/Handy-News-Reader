/**
 * Flym
 *
 *
 * Copyright (c) 2012-2015 Frederic Julian
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet

import java.util.ArrayList

class AutoSummaryListPreference(context: Context, attrs: AttributeSet) : android.preference.ListPreference(context, attrs) {
    private var mSummary: CharSequence = summary

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (positiveResult) {
            summary = summary
        }
    }

    override fun getSummary(): CharSequence {
        val list = ArrayList<CharSequence>()
        if ( mSummary != null  )
            list.add(mSummary)
        try {
            if (entry != null)
                list.add(entry.toString().replace("%", "%%"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return TextUtils.join("\n", list)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(restoreValue, defaultValue)
        summary = summary
    }
}
