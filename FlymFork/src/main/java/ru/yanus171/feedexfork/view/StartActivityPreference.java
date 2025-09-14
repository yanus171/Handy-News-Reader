package ru.yanus171.feedexfork.view;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static ru.yanus171.feedexfork.Constants.DB_AND;
import static ru.yanus171.feedexfork.Constants.DB_IS_NOT_NULL;
import static ru.yanus171.feedexfork.Constants.DB_IS_NULL;
import static ru.yanus171.feedexfork.Constants.DB_OR;
import static ru.yanus171.feedexfork.MainApplication.UNREAD_NOTIFICATION_CHANNEL_ID;
import static ru.yanus171.feedexfork.provider.FeedData.FeedColumns.IS_AUTO_REFRESH;
import static ru.yanus171.feedexfork.provider.FeedData.PACKAGE_NAME;
import static ru.yanus171.feedexfork.provider.FeedData.getOldContentValues;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
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
		else if (Oper.equals("edit_global_filter"))
			getContext().startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedData.FeedColumns.CONTENT_URI(FetcherService.GetExtrenalLinkFeedID())));
		else if (Oper.equals("show_notification_setup"))
			OpenNotificationSettings("");
	}
	private void OpenNotificationSettings( String channellID ) {
		Intent intent = new Intent();
		intent.setFlags( FLAG_ACTIVITY_NEW_TASK );
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
			intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
			intent.putExtra(Settings.EXTRA_CHANNEL_ID, channellID );
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
			intent.putExtra("app_package", getContext().getPackageName());
			intent.putExtra("app_uid", getContext().getApplicationInfo().uid);
		} else {
			intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.setData(Uri.parse("package:" + getContext().getPackageName()));
		}
		getContext().startActivity(intent);
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
				UiUtils.toast(  String.format( "%d %s", affected, getContext().getString( R.string.feed_count_were_edited) ) );
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
				UiUtils.toast( String.format( "%d %s", affected, getContext().getString( R.string.feed_count_were_edited) ) );
			})
			.setNegativeButton( android.R.string.no, null )
			.create().show();
	}
}
