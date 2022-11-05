package ru.yanus171.feedexfork.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;

public abstract class ListSelectPreference extends Preference {
	private final boolean IsPositive;
	private final char Separator;
	public static final char DefaultSeparator = '_';
	protected String KeyAll;
	public static final boolean POSITIVE = true;
	public static final boolean NEGATIVE = false;
	// ------------------------------------------------------------------
    public ListSelectPreference(Context context, String keySelectedLists, String keyAll, char separator, boolean isPositive, AttributeSet attrs) {
		super(context, attrs);
		if ( !keySelectedLists.isEmpty() )
			setKey(keySelectedLists);
		KeyAll = keyAll;
		Separator = separator;
		IsPositive = isPositive;
	}

	// ------------------------------------------------------------------
	@Override
	protected void onClick() {
		LinearLayout vLayout = new LinearLayout(getContext());
		vLayout.setOrientation(LinearLayout.VERTICAL);

		final CheckBox cbAll = AddCheckBox( vLayout, null, R.string.all);

		ScrollView scroll = new ScrollView(getContext());
		vLayout.addView(scroll);

		final RadioGroup group = new RadioGroup(getContext());
		scroll.addView(group);
		//group.setBackgroundResource(android.R.drawable.dialog_frame);
		group.setBackgroundColor(Theme.GetMenuBackgroundColor());
		group.setOrientation(RadioGroup.VERTICAL);

		PopulateList(group);

		if ( !KeyAll.isEmpty() ) {
			cbAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
				for (int i = 0; i < group.getChildCount(); i++) {
					group.getChildAt(i).setEnabled(!isChecked);
				}
			});
			cbAll.setChecked(GetIsAll(KeyAll));
		} else
			cbAll.setVisibility(View.GONE );

		Theme.CreateDialog(getContext()).setView(vLayout).setPositiveButton(android.R.string.ok, (dialog, which) -> {
			StringBuilder listSave = new StringBuilder();
			for (int i = 0; i < group.getChildCount(); i++) {
				CheckBox btn = (CheckBox) group.getChildAt(i);
				if (IsPositive == btn.isChecked()) {
					listSave.append((String) btn.getTag()).append(Separator);
				}
			}
			//SharedPreferences.Editor editor = PrefUtils..Prefs.edit();
			PrefUtils.putString( getKey(), listSave.toString());
			PrefUtils.putBoolean( KeyAll, cbAll.isChecked());
			callChangeListener(null);
			dialog.dismiss();
			OnDialogClosed();
		}).setNegativeButton(android.R.string.cancel, null).create().show();
	}

	public void OnDialogClosed() {
	}

	// -------------------------------------------------------------------
	public static CheckBox AddCheckBox( ViewGroup layout, LinearLayout.LayoutParams lp, int textID ) {
		return AddCheckBox( layout, lp, layout.getContext().getString( textID ) );
	}
	// -------------------------------------------------------------------
	public static CheckBox AddCheckBox( ViewGroup layout, LinearLayout.LayoutParams lp, String text ) {
		CheckBox result = new CheckBox(layout.getContext());
		result.setText(text);
		//result.setTextSize(TypedValue.COMPLEX_UNIT_DIP, Global.GetViewMainFontSize());
		result.setTextColor(Theme.GetMenuFontColor());
		if (lp != null)
			layout.addView(result, lp);
		else
			layout.addView(result);
		return result;
	}
	// -------------------------------------------------------------------------
	protected CheckBox AddListCheckBox(String ID_, String name, ViewGroup group, String keySelectedLists) {
		CheckBox btn = AddCheckBox( group, null, name );
		btn.setTag(ID_);
		btn.setChecked(!IsPositive);
		for (String ID : GetSplitter(keySelectedLists, Separator))
			if ( btn.getTag().equals(ID) )
				btn.setChecked(IsPositive);
		return btn;
	}

	// -------------------------------------------------------------------------
	protected abstract void PopulateList(ViewGroup group);

	// -------------------------------------------------------------------------
	static public TextUtils.SimpleStringSplitter GetSplitter(String keySelectedLists, char separator) {
		String prefString = PrefUtils.getString(keySelectedLists, "");
		TextUtils.SimpleStringSplitter result = new TextUtils.SimpleStringSplitter(separator);
		result.setString(prefString);
		return result;
	}

	// -------------------------------------------------------------------------
	protected static boolean GetIsAll(String keyAll) {
		return PrefUtils.getBoolean( keyAll, true );
	}

	// -------------------------------------------------------------------------
	public  static String GetIDList(String keySelectedLists, char separator) {
		StringBuilder result = new StringBuilder();
		for (String ID : GetSplitter(keySelectedLists, separator)) {
			result.append(ID).append(", ");
		}
		if (result.length() > 0) {
			result = new StringBuilder(result.substring(0, result.length() - 2));
		}
		return result.toString();
	}

	// -------------------------------------------------------------------------
	protected static boolean IsIDInList(String listID, String keySelectedLists, String keyAllLists, char separator, boolean isPositive) {
		boolean result = !isPositive;
		if (GetIsAll(keyAllLists)) {
			result = true;
		} else {
			for (String itemID : GetSplitter(keySelectedLists, separator)) {
				if (itemID.equals(listID)) {
					result = isPositive;
					break;
				}
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	static boolean IsIDInList(long listID, String keySelectedLists, String keyAllLists, char separator, boolean isPositive) {
		return IsIDInList(String.valueOf(listID), keySelectedLists, keyAllLists, separator, isPositive);
	}

}
