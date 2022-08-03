package ru.yanus171.feedexfork.view;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.TreeSet;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;

import static ru.yanus171.feedexfork.fragment.EntryFragment.DpToPx;
import static ru.yanus171.feedexfork.view.AppSelectPreference.CreateAppList;
import static ru.yanus171.feedexfork.view.AppSelectPreference.Separator;
import static ru.yanus171.feedexfork.view.AppSelectPreference.cNoApp;

public class AppSelectPreference extends Preference {
	final static String Separator = "#";
	//final static String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	final static String cNoApp = "noApp";

	private Spinner mSpinApp;
	private String DefaultValue = cNoApp;
	private static AppList mBrowserAppList = null;
	private static AppList mImageAppList = null;

	// ------------------------------------------------------------------
	public AppSelectPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		DefaultValue = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue");
	}

	// ------------------------------------------------------------------
    public AppSelectPreference(Context context) {
		super(context);
	}

	// --------------------------------------------------------------
	@SuppressLint("MissingSuperCall")
	@Override
	protected View onCreateView(ViewGroup parent) {
		//super.onCreateView(parent);
		LinearLayout vLayout = new LinearLayout(getContext());
		vLayout.setOrientation(LinearLayout.VERTICAL);
		vLayout.setPadding(DpToPx( 15 ), DpToPx( 10 ), DpToPx( 15 ), DpToPx( 10 ));

//		TextView title = new TextView(getContext());
//		title.setText(getTitle());
//		title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, PrefUtils.getFontSize());
//		vLayout.addView(title);

		mSpinApp = new Spinner(getContext());
		AppListAdapter mAdapter = new AppListAdapter(mSpinApp);
		mSpinApp.setAdapter(mAdapter);
		mSpinApp.setSelection(GetSpinnerItemPosByName(getPersistedString(DefaultValue)));
		mSpinApp.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				persistString(GetCurrentApp());
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		});

		vLayout.addView(mSpinApp);

		if ((getSummary() != null) && (getSummary().length() > 0)) {
			TextView summary = new TextView(getContext());
			summary.setText(getSummary());
			vLayout.addView(summary);
		}

		return vLayout;
	}

	/*
	 * //-------------------------------------------------------------- private
	 * String GetAppCaptionByName(String name) { String result = ""; PackageInfo
	 * pinfo; try { PackageManager pm = getContext().getPackageManager(); pinfo
	 * = pm.getPackageInfo(name, 0); result =
	 * pinfo.applicationInfo.loadLabel(pm).toString(); } catch
	 * (NameNotFoundException e) {
	 * 
	 * } return result; }
	 */

	private AppList GetAppList() {
		AppList list = getKey().equals( "openLinkInBrowserTapAction" ) ? mBrowserAppList : mImageAppList;
		if ( list == null )
			list = new AppList();
		return list;
	}
	// --------------------------------------------------------------
	private int GetSpinnerItemPosByName(String name) {
		int result = -1;
		int index = 0;
		for (SelectActivityInfo item : GetAppList() ) {
			if (item.mName.equals(name)) {
				mSpinApp.setSelection(index);
				result = index;
				break;
			}
			index++;
		}
		return result;
	}

	//--------------------------------------------------------------
    private String GetCurrentApp() {
		String result = "";
		if (mSpinApp.getSelectedItem() != null) {
			result = ((SelectActivityInfo) mSpinApp.getSelectedItem()).mName;
		}
		return result;
	}

	// **************************************************************
	private class AppListAdapter extends BaseAdapter implements SpinnerAdapter {
		Spinner mSpinner;

		// --------------------------------------------------------------
        AppListAdapter(Spinner spinner) {
			mSpinner = spinner;
		}

		// --------------------------------------------------------------
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			final int dim = DpToPx( 5 );

			LinearLayout hLayout = new LinearLayout(getContext());
			hLayout.setOrientation(LinearLayout.HORIZONTAL);
			hLayout.setPadding(dim, dim, dim, dim);

			hLayout.setBackgroundColor(Theme.GetMenuBackgroundColor());
			
			AddImageView(hLayout, getItem(position).GetIcon(), 0);

			TextView textView = new TextView(getContext());
			textView.setText(getItem(position).mCaption);
			textView.setTextColor(Theme.GetMenuFontColor());
			textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			textView.setPadding( dim, 0, 0, 0);
			textView.setGravity(Gravity.CENTER_VERTICAL);
			if (position == mSpinner.getSelectedItemPosition()) {
				textView.setTypeface(Typeface.DEFAULT_BOLD);
			}
			hLayout.addView(textView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
			return hLayout;
		}

		// --------------------------------------------------------------
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout hLayout = new LinearLayout(getContext());
			hLayout.setOrientation(LinearLayout.HORIZONTAL);

			SelectActivityInfo item = getItem(position);
			if (item != null) {
				AddImageView(hLayout, item.GetIcon(), 0);
				TextView textView = new TextView(getContext());
				textView.setText(getItem(position).mCaption);
				textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
				textView.setPadding(10, 0, 0, 0);
				textView.setGravity(Gravity.CENTER_VERTICAL);
				hLayout.addView(textView);
			}
			return hLayout;
		}

		// -----------------------------------------------------------------
		@Override
		public int getCount() {
			return GetAppList().size();
		}

		// -----------------------------------------------------------------
		public SelectActivityInfo getItem(int index) {
			return GetAppList().GetItemByIndex(index);
		}

		// -----------------------------------------------------------------
		public long getItemId(int index) {
			return index;
		}

		// -----------------------------------------------------------------
		@Override
		public int getItemViewType(int arg0) {
			return IGNORE_ITEM_VIEW_TYPE;
		}

		// -----------------------------------------------------------------
		@Override
		public int getViewTypeCount() {
			return 1;
		}

		// -----------------------------------------------------------------
		@Override
		public boolean hasStableIds() {
			return true;
		}

		// -----------------------------------------------------------------
		@Override
		public boolean isEmpty() {
			return GetAppList().isEmpty();
		}

		// -----------------------------------------------------------------
		public void registerDataSetObserver(DataSetObserver arg0) {

		}

		// -----------------------------------------------------------------
		public void unregisterDataSetObserver(DataSetObserver arg0) {

		}
	}

	// --------------------------------------------------------------
	public static void Init() {
		if (mBrowserAppList == null) {
			new UpdateAppList().execute(new Void[] { null });
		}
	}

	// --------------------------------------------------------------------------
	private static void AddItem(String name, int captionID, int iconID, AppList appList) {
		SelectActivityInfo item = new SelectActivityInfo();
		item.mName = name;
		item.mCaption = MainApplication.getContext().getString(captionID);
		item.IconID = iconID;
		appList.add(item);
	}

	// --------------------------------------------------------------------------
	static private ArrayList<ResolveInfo> GetBrowserAppResolveInfoList() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse( "http://example.com" ));
		PackageManager pm = MainApplication.getContext().getPackageManager();
		return (ArrayList<ResolveInfo>) pm.queryIntentActivities(intent,
																 Build.VERSION.SDK_INT >= 23 ? PackageManager.MATCH_ALL : 0);
	}
	// --------------------------------------------------------------------------
	static private ArrayList<ResolveInfo> GetImageAppResolveInfoList() {
		Intent intent = new Intent(Intent.ACTION_VIEW, null);
		intent.setType( "image/*" );
		PackageManager pm = MainApplication.getContext().getPackageManager();
		return (ArrayList<ResolveInfo>) pm.queryIntentActivities(intent,
																 Build.VERSION.SDK_INT >= 23 ? PackageManager.MATCH_ALL : 0);
	}
	// -------------------------------------------------------------------------------------
	static void CreateAppList( UpdateAppList updateAppList ) {
		if ( mBrowserAppList == null )
			mBrowserAppList = CreateAppList(updateAppList, GetBrowserAppResolveInfoList() );
		if ( mImageAppList == null )
			mImageAppList = CreateAppList(updateAppList, GetImageAppResolveInfoList() );
	}
	// -------------------------------------------------------------------------------------
	static AppList CreateAppList(UpdateAppList updateAppList, ArrayList<ResolveInfo> list ) {
		AppList appList = new AppList();
		AddItem(cNoApp, R.string.not_selected, android.R.drawable.btn_dialog, appList);

		PackageManager pm = MainApplication.getContext().getPackageManager();
		int index = 0;
		for (ResolveInfo pInfo : list) {
			if ( pInfo.activityInfo.packageName.equals( MainApplication.getContext().getPackageName() ) )
				continue;
			SelectActivityInfo item = new SelectActivityInfo();
			item.mName = GetAppName(pInfo.activityInfo.packageName, pInfo.activityInfo.name);
			item.mCaption = String.format("%s", pInfo.loadLabel(pm));
			item.ClassName = pInfo.activityInfo.name;
			item.PackageName = pInfo.activityInfo.packageName;
			appList.add(item);
			index++;
			if (updateAppList != null) {
				updateAppList.PublishProgress(index);
			}
		}
		index = 0;
		for (SelectActivityInfo item : appList ) {
			appList.IndexToItem.put(index, item);
			index++;
		}
		return appList;
	}

	// -------------------------------------------------------------------------
	public static String GetAppName(String packageName, String activityName) {
		return String.format("%s%s%s", packageName, Separator, activityName);
	}

	// -------------------------------------------------------------------------
	static public String GetPackageNameForAction(String appKey) {
		String pref = PrefUtils.getString(appKey, cNoApp);
		if (pref.contains(Separator)) {
			String[] prefName = pref.split(Separator);
			if (prefName.length == 2) {
				String packageName = prefName[0];
				return packageName;
			}
		}
		return null;
	}

	// -----------------------------------------------------------------
	static ImageView AddImageView(LinearLayout layout, Drawable image, int size) {
		int imageSize = size;
		if (imageSize == 0)
			imageSize = PrefUtils.getIntFromText( "appShortcutImageSize", 0 );//MainActivity.DpToPx(cImageDim));
		ImageView result = new ImageView(MainApplication.getContext());
		result.setImageDrawable(image);
		result.setScaleType(ScaleType.CENTER_INSIDE);
		result.setAdjustViewBounds(true);

		if (imageSize != 0) {
			result.setAdjustViewBounds(true);
			result.setMinimumWidth(imageSize);
			result.setMinimumHeight(imageSize);
		} else {
			imageSize = DpToPx(40);
		}
		layout.addView(result, imageSize, imageSize);
		return result;
	}

	static public Intent GetShowInBrowserIntent( String url ) {
		final String packageName = GetPackageNameForAction("openLinkInBrowserTapAction" );
		final Intent result = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		if ( !url.contains( "//t.me/" ) && packageName != null )
			result.setPackage(packageName);
		return result;
	}
}
class SelectActivityInfo {
	String mName;
	String mCaption;
	String PackageName;
	String ClassName;
	int IconID = 0;

	// --------------------------------------------------------------
	@Override
	public String toString() {
		return mCaption;
	}

	// --------------------------------------------------------------
	Drawable GetIcon() {
		Drawable result = null;
		if (IconID != 0) {
			result = AppCompatResources.getDrawable( MainApplication.getContext(), IconID );
		} else {
			PackageManager pm = MainApplication.getContext().getPackageManager();
			try {
				result = pm.getActivityIcon(new ComponentName(PackageName, ClassName));
			} catch (NameNotFoundException ignored) {
			}
		}
		return result;
	}
}
// *****************************************************************
class AppList extends TreeSet<SelectActivityInfo> {
	private static final long serialVersionUID = 1L;
	SparseArray<SelectActivityInfo> IndexToItem = new SparseArray<>();

	// --------------------------------------------------------------
	AppList() {
		super((a, b) -> {
			int result;

			if (a.mName.equals(cNoApp)) {
				result = -1;
			} else if (b.mName.equals(cNoApp)) {
				result = 1;
			} else {
				if (!IsAppAction(a.mName) && IsAppAction(b.mName)) {
					result = -1;
				} else if (IsAppAction(a.mName) && !IsAppAction(b.mName)) {
					result = 1;
				} else {
					result = a.mCaption.compareTo(b.mCaption);
					if (result == 0 && a.PackageName != null && b.PackageName != null)
						result = a.PackageName.compareTo(b.PackageName);
				}
			}
			return result;
		});
	}

	// -------------------------------------------------------------
	SelectActivityInfo GetItemByIndex(int index) {
		return IndexToItem.get(index);
	}
	// --------------------------------------------------------------
	private static boolean IsAppAction(String name) {
		return name.contains(Separator);
	}


}

// ************************************************************************
class UpdateAppList extends AsyncTask<Void, Integer, Void> {
	// --------------------------------------------------------------------------
	UpdateAppList() {
	}

	// --------------------------------------------------------------------------
	@Override
	protected Void doInBackground(Void... params) {
		CreateAppList(this );
		return null;
	}

	// --------------------------------------------------------------------------
	@Override
	protected void onProgressUpdate(Integer... progress) {

		//Dialog.setProgress(progress[0]);
	}

	// --------------------------------------------------------------------------
	@Override
	protected void onPreExecute() {
//			if (mAppList == null) {
//				Dialog = new ProgressDialog(getContext());
//				Dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//				Dialog.setMax(GetAppResolveInfoList().size());
//				Dialog.setMessage(MainApplication.getContext().getText(R.string.appListLoading));
//				Dialog.show();
//			}
	}

	// --------------------------------------------------------------------------
	@Override
	protected void onPostExecute(Void result) {
//			if (Dialog != null) {
//				try {
//					Dialog.dismiss();
//				} catch (IllegalArgumentException e) {
//					// TODO: handle exception
//				}
//			}
	}

	// --------------------------------------------------------------------------
	void PublishProgress(int index) {
		publishProgress(index);
	}
}
