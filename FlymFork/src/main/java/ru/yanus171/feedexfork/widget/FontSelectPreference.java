package ru.yanus171.feedexfork.widget;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.Preference;
import android.text.Html;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView.ScaleType;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.StorageSelectPreference;

import static ru.yanus171.feedexfork.utils.UiUtils.AddSmallText;

public class FontSelectPreference extends Preference {
	private final static String Separator = "#";
	// final static int cImageHeight = 40;
	final static String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	static public final String DefaultFontFamily = "Default";
	private static final String FontsDir = "fonts";
	private static final int cImageDim = 40;

	private Spinner mSpinApp;
	private AppListAdapter mAdapter;
	private static FontList mFontList = null;
	private ImageView mImage = null;

	private static class FontInfo {
		String mFontName;
		Typeface mTypeface;

		FontInfo( String name ) {
			mFontName = name;
			mTypeface = FontSelectPreference.GetTypeFaceByName( name );
		}
		// --------------------------------------------------------------
		@Override
		public String toString() {
			return mFontName;
		}
	}

	// -----------------------------------------------------------------------------
	static public Typeface GetTypeFace(String key) {
		return GetTypeFaceByName( PrefUtils.getString(key, "") );
	}
	// -----------------------------------------------------------------------------
	static public Typeface GetTypeFaceByName( String name ) {
		String fileName = name;
		Typeface result = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD);
		if (!fileName.equals(FontSelectPreference.DefaultFontFamily)) {
			try {
				result = Typeface.createFromAsset(MainApplication.getContext().getAssets(), FontsDir + "/" + fileName);
			} catch (Exception ignored) {
				try {
					result = Typeface.createFromFile(FileUtils.INSTANCE.getFontsFolder() + "/" + fileName );
				} catch (Exception e) {
					DebugApp.AddErrorToLog(null, e );
				}
			}
		}
		return result;
	}
	// -----------------------------------------------------------------------------
	static public String GetTypeFaceLocalUrl(String fontName, boolean isEditingMode ) {
		if (!fontName.equals(FontSelectPreference.DefaultFontFamily) && !isEditingMode) {
			try {
				if ( Arrays.asList(MainApplication.getContext().getAssets().list(FontsDir ) ).contains(fontName ) )
					return "file:///android_asset/" + FontsDir + "/" + fontName;
				else
					return "file://" + FileUtils.INSTANCE.getFontsFolder() + "/" + fontName;
			} catch (IOException e) {
				DebugApp.AddErrorToLog( null, e );
			}
		}
		return "";
	}
	// *****************************************************************
	static class FontList extends ArrayList<FontInfo> {
		private static final long serialVersionUID = 1L;
		SparseArray<FontInfo> IndexToItem = new SparseArray<FontInfo>();

		// --------------------------------------------------------------
        FontList() {
			super();
		}

		// -------------------------------------------------------------
        FontInfo GetItemByIndex(int index) {
			FontInfo result = null;
			result = IndexToItem.get(index);
			return result;
		}
	}

	// ------------------------------------------------------------------
	public FontSelectPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		CreateFontList( getContext());
	}

	// ------------------------------------------------------------------
    public FontSelectPreference(Context context) {
		super(context);
		CreateFontList( getContext());
	}

	// --------------------------------------------------------------
	@Override
	protected View onCreateView(ViewGroup parent) {

		LinearLayout vLayout = new LinearLayout(getContext());
		vLayout.setOrientation(LinearLayout.VERTICAL);
		vLayout.setPadding(15, 10, 15, 10);

		TextView title = new TextView(getContext());
		title.setText(getTitle());
		title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
		vLayout.addView(title);

		mSpinApp = new Spinner(getContext());
		mAdapter = new AppListAdapter(mSpinApp);
		mSpinApp.setAdapter(mAdapter);
		mSpinApp.setSelection(GetSpinnerItemPosByName(getPersistedString( DefaultFontFamily ) ));
		mSpinApp.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				persistString(GetCurrentFontName());
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		});

		vLayout.addView(mSpinApp);

		if ((getSummary() != null) && (getSummary().length() > 0)) {
			TextView summary = new TextView(getContext());
			summary.setText(getSummary() + " " + FileUtils.INSTANCE.getFontsFolder().getPath() );
			vLayout.addView(summary);
		}

		return vLayout;
		// return lLayout;
	}

	// --------------------------------------------------------------
	private int GetSpinnerItemPosByName(String name) {
		int result = -1;
		int index = 0;
		for (FontInfo item : mFontList) {
			if (item.mFontName.equals(name)) {
				mSpinApp.setSelection(index);
				result = index;
				break;
			}
			index++;
		}
		return result;
	}
	// --------------------------------------------------------------
    private String GetCurrentFontName() {
		String result = "";
		if (mSpinApp.getSelectedItem() != null) {
			result = ((FontInfo) mSpinApp.getSelectedItem()).mFontName;
		}
		return result;
	}

	// **************************************************************
	private class AppListAdapter extends BaseAdapter implements SpinnerAdapter {
		Spinner mSpinner;

		// --------------------------------------------------------------
        AppListAdapter(Spinner spinner) {
			mSpinner = spinner;
			if (mFontList == null) {
				mFontList = new FontList();
			}
		}

		// --------------------------------------------------------------
		@SuppressWarnings("deprecation")
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {

			LinearLayout vLayout = new LinearLayout(getContext());
			vLayout.setOrientation(LinearLayout.VERTICAL);
			//vLayout.setBackgroundColor(Theme.GetMenuBackgroundColor());

			LinearLayout hLayout = new LinearLayout(getContext());
			hLayout.setOrientation(LinearLayout.HORIZONTAL);

			//hLayout.setBackgroundColor(Theme.GetMenuBackgroundColor());

			TextView textView = new TextView(getContext());
			textView.setText(Html.fromHtml( getItem(position).mFontName.replace( ".ttf", "" ) + " (<b>" + getContext().getString(R.string.bold) + "</b>)" ) );
			textView.setTextColor(Theme.GetMenuFontColor());
			textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			final int PAD = UiUtils.dpToPixel(5);
			textView.setPadding(PAD, PAD, 0, PAD);
			textView.setSingleLine();
			textView.setGravity(Gravity.CENTER_VERTICAL);
			textView.setTypeface( getItem(position).mTypeface );
			hLayout.addView(textView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
			vLayout.addView(hLayout);
			if ( position > 0 ){
				TextView small = AddSmallText(vLayout, null, Gravity.LEFT, null, getItem(position).mFontName);
				small.setTypeface(Typeface.DEFAULT);
				small.setPadding(PAD, 0, 0, PAD);
				small.setTextColor(Theme.GetMenuFontColor());
				small.setEnabled( false );
			}
//			vLayout.setBackgroundResource(android.R.drawable.dialog_frame);
			return vLayout;

		}

		// --------------------------------------------------------------
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout hLayout = new LinearLayout(getContext());
			hLayout.setOrientation(LinearLayout.HORIZONTAL);

			// View superView = super.getView(position, convertView, parent);

			FontInfo item = getItem(position);
			if (item != null) {
				//AddImageView(hLayout, item.GetIcon(), 0);

				TextView textView = new TextView(getContext());
				textView.setText( getItem(position).mFontName );
				textView.setTypeface( getItem(position).mTypeface );
				textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
				//textView.setPadding(EventListView.cPad, 0, 0, 0);
				textView.setGravity(Gravity.CENTER_VERTICAL);
				hLayout.addView(textView);

				// hLayout.addView( superView );
			}

			return hLayout;
		}

		// -----------------------------------------------------------------
		@Override
		public int getCount() {
			return mFontList.size();
		}

		// -----------------------------------------------------------------
		public FontInfo getItem(int index) {
			return mFontList.GetItemByIndex(index);
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
			return mFontList.isEmpty();
		}

		// -----------------------------------------------------------------
		public void registerDataSetObserver(DataSetObserver arg0) {

		}

		// -----------------------------------------------------------------
		public void unregisterDataSetObserver(DataSetObserver arg0) {

		}
	}

	// --------------------------------------------------------------------------
	private static void AddItem(String name) {
		mFontList.add(new FontInfo( name ));
	}

	// -------------------------------------------------------------------------------------
	static void CreateFontList(Context context) {
		//if (mFontList == null) {
			mFontList = new FontList();
			mFontList.clear();
			String[] list1 = {};
			try {
				list1 = context.getAssets().list(FontsDir);
			} catch (IOException ignored) {
			}

			ArrayList<String> list = new ArrayList<>();
			Collections.addAll(list, list1);

			try {
				File dir = FileUtils.INSTANCE.getFontsFolder();
				File[] files = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return true;//name.endsWith(".ttf");
					}
				});
				for (File item : files)
					list.add( item.getName() );
			} catch ( Exception e ) {
				DebugApp.AddErrorToLog(null, e);
			}
			AddItem(DefaultFontFamily);
			for ( String item: list )
				AddItem( item );

			int index = 0;
			for (FontInfo item : mFontList) {
				mFontList.IndexToItem.put(index, item);
				index++;
			}
		//}
	}
}
