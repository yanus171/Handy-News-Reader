package ru.yanus171.feedexfork.view;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.ViewGroup;

import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.Label;
import ru.yanus171.feedexfork.utils.LabelVoc;

public class LabelSelectPreference extends ListSelectPreference {
	// ------------------------------------------------------------------
	public LabelSelectPreference(Context context, AttributeSet attrs) {
		super(context, "", "", DefaultSeparator, POSITIVE, attrs);
	}

	// -------------------------------------------------------------------------
	@Override
    protected void PopulateList(ViewGroup group) {
		for ( Label label: LabelVoc.INSTANCE.getList() )
			AddListCheckBox(String.valueOf(label.mID), label.mName, group, getKey());
	}
}
