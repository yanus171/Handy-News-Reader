package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_IS_NOT_NULL;
import static ru.yanus171.feedexfork.Constants.DB_IS_NULL;
import static ru.yanus171.feedexfork.Constants.DB_OR;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.IS_AUTO_REFRESH;
import static ru.yanus171.feedexfork.provider.FeedData.PACKAGE_NAME;

import android.content.ContentValues;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;

public class StartActivityPreference extends Preference {
	private final String NS = PACKAGE_NAME + ".views.StartActivityPreference";
	private String ClassName;
	private String Oper;

	// ------------------------------------------------------------------
	public StartActivityPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		ClassName = attrs.getAttributeValue(NS, "className");
		Oper = attrs.getAttributeValue(NS, "oper");
	}

	// ------------------------------------------------------------------
	@Override
	protected void onClick() {
		if (Oper.equals("refresh_only_selected_select_all"))
			SelectAllFeedsForAutoRefresh();
		else if (Oper.equals("refresh_only_selected_select_none"))
			SelectNoneFeedsForAutoRefresh();
}
	private void SelectAllFeedsForAutoRefresh() {
		Theme.CreateDialog( getContext() )
			.setMessage( getContext().getString( R.string.refresh_only_selected_select_all_summary ) + " ?" )
			.setTitle( R.string.question )
			.setPositiveButton( android.R.string.yes, (dialogInterface, i) -> {
				ContentValues values = new ContentValues();
				values.put( IS_AUTO_REFRESH, 1 );
				int affected = getContext().getContentResolver()
					.update(FeedData.FeedColumns.CONTENT_URI, values, IS_AUTO_REFRESH + DB_IS_NULL + DB_OR + IS_AUTO_REFRESH + "=0", null );
				UiUtils.toast( getContext(), String.format( "%d %s", affected, getContext().getString( R.string.feed_count_were_edited) ) );
			})
			.setNegativeButton( android.R.string.no, null )
			.create().show();
	}

	private void SelectNoneFeedsForAutoRefresh() {
		Theme.CreateDialog( getContext() )
			.setMessage( getContext().getString( R.string.refresh_only_selected_select_none_summary ) + " ?" )
			.setTitle( R.string.question )
			.setPositiveButton( android.R.string.yes, (dialogInterface, i) -> {
				ContentValues values = new ContentValues();
				values.putNull( IS_AUTO_REFRESH );
				int affected = getContext().getContentResolver()
					.update(FeedData.FeedColumns.CONTENT_URI, values, IS_AUTO_REFRESH + DB_IS_NOT_NULL + DB_AND + IS_AUTO_REFRESH + "=1", null );
				UiUtils.toast( getContext(), String.format( "%d %s", affected, getContext().getString( R.string.feed_count_were_edited) ) );
			})
			.setNegativeButton( android.R.string.no, null )
			.create().show();
	}
}
