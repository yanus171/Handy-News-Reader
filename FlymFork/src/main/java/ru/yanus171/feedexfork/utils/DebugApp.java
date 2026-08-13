package ru.yanus171.feedexfork.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.SendErrorActivity;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;

//****************************************************************************
public class DebugApp {
	private static final String TAG = "HandyNews";
	private static final int cPad = 10;
	private static StringBuilder ErrorLog = new StringBuilder();
	private static final int c1024 = 1024;

	class Info {
		String VersionName;
		Integer VersionCode;
		String PackageName;
		String FilePath;
		String PhoneModel;
		String AndroidVersion;
		String Board;
		String Brand;
		// String CPU_ABI;
		String Device;
		String Display;
		String FingerPrint;
		String Host;
		String ID;
		String Manufacturer;
		String Model;
		String Product;
		String Tags;
		long Time;
		String Type;
		String User;

		Info() {
			UpdateInformation();
		}

		// ------------------------------------------------------------------------
		void UpdateInformation() {
			final Context context = MainApplication.getContext();

			PackageManager pm = context.getPackageManager();
			try {
				PackageInfo pi;
				// Version
				pi = pm.getPackageInfo(context.getPackageName(), 0);
				VersionName = pi.versionName;
				VersionCode = pi.versionCode;
				// Package name
				PackageName = pi.packageName;
				// Files dir for storing the stack traces
				FilePath = context.getFilesDir().getAbsolutePath();
				// Device model
				PhoneModel = android.os.Build.MODEL;
				// Android version
				AndroidVersion = android.os.Build.VERSION.RELEASE;

				Board = android.os.Build.BOARD;
				Brand = android.os.Build.BRAND;
				// CPU_ABI = android.os.Build.;
				Device = android.os.Build.DEVICE;
				Display = android.os.Build.DISPLAY;
				FingerPrint = android.os.Build.FINGERPRINT;
				Host = android.os.Build.HOST;
				ID = android.os.Build.ID;
				// Manufacturer = android.os.Build.;
				Model = android.os.Build.MODEL;
				Product = android.os.Build.PRODUCT;
				Tags = android.os.Build.TAGS;
				Time = android.os.Build.TIME;
				Type = android.os.Build.TYPE;
				User = android.os.Build.USER;

			} catch (NameNotFoundException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		// --------------------------------------------------------------------------
        String GetInformationString() {
			String ReturnVal = "";
			ReturnVal += "Version : " + VersionName;
			ReturnVal += "\n";
			ReturnVal += "Version Code: " + VersionCode.toString();
			ReturnVal += "\n";
			ReturnVal += "Package : " + PackageName;
			ReturnVal += "\n";
			ReturnVal += "FilePath : " + FilePath;
			ReturnVal += "\n";
			ReturnVal += "Phone Model: " + PhoneModel;
			ReturnVal += "\n";
			ReturnVal += "Android Version : " + AndroidVersion;
			ReturnVal += "\n";
			ReturnVal += "Board : " + Board;
			ReturnVal += "\n";
			ReturnVal += "Brand : " + Brand;
			ReturnVal += "\n";
			ReturnVal += "Device : " + Device;
			ReturnVal += "\n";
			// ReturnVal += "Display : " + Display;
			// ReturnVal += "\n";
			// ReturnVal += "Finger Print : " + FingerPrint;
			// ReturnVal += "\n";
			// ReturnVal += "Host : " + Host;
			// ReturnVal += "\n";
			// ReturnVal += "ID : " + ID;
			// ReturnVal += "\n";
			ReturnVal += "Model : " + Model;
			ReturnVal += "\n";
			ReturnVal += "Product : " + Product;
			ReturnVal += "\n";
			// ReturnVal += "Tags : " + Tags;
			// ReturnVal += "\n";
			// ReturnVal += "Time : " + Time;
			// ReturnVal += "\n";
			// ReturnVal += "Type : " + Type;
			// ReturnVal += "\n";
			ReturnVal += "User : " + User;
			ReturnVal += "\n";
			ReturnVal += "Total Internal memory : " + getTotalInternalMemorySize();
			ReturnVal += "\n";
			ReturnVal += "Available Internal memory : " + getAvailableInternalMemorySize();
			ReturnVal += "\n";

			return ReturnVal;
		}

		// --------------------------------------------------------------------------
		long getTotalInternalMemorySize() {
			File path = Environment.getDataDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long totalBlocks = stat.getBlockCount();
			return totalBlocks * blockSize;
		}
	}
	// -------------------------------------------------------------------
	static public void SendException( Throwable throwable, final Context context) {
		final StringWriter st = new StringWriter();
		st.append("UNCAUGHT EXCEPTION\n");
		st.append("-------------------\n");
		st.append(GetStackTrace(throwable) + "\n");
		st.append("-------------------\n");
		st.append(new DebugApp().new Info().GetInformationString());
		st.append("-------------------\n");
		// st.append(CalendarEvent.GetProviderInfo( mContext ));
		// st.append("-------------------\n");

		PrefUtils.putStringCommit( "crashText", st.toString() );

		Dog.e("HandyNewsLog", st.toString());

		throwable.printStackTrace();

		try {
			CreateFileUri(FileUtils.INSTANCE.getFolder().getAbsolutePath(), "crash.txt", st.toString());
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "workyalex@mail.ru" });
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "HandyNews error stacktrace");
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, st.toString());
		if ( PrefUtils.getBoolean( "copy_debug_info_to_clipboard", true ) )
			((ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE))
				.setText(st.toString());
		UiUtils.RunOnGuiThread(() -> Toast.makeText(context, R.string.toastAppCrashed, Toast.LENGTH_LONG).show());
		context.startActivity(
				Intent.createChooser(emailIntent, context.getString(R.string.criticalErrorSending)).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ));
		System.exit(1);
	}

	// ****************************************************************************
	public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		Thread.UncaughtExceptionHandler mOldHandler;
		Context mContext;

		public UncaughtExceptionHandler(Context context) {
			mOldHandler = Thread.getDefaultUncaughtExceptionHandler();
			mContext = context;
		}


		@Override
		public void uncaughtException(Thread thread, Throwable throwable) {
			if ( PrefUtils.getBoolean( "send_debug_info_on_crash", true ) )
				SendException( throwable, mContext );
			else
				mOldHandler.uncaughtException( thread, throwable );
		}
	}


	// ----------------------------------------------------------------------------
	private static String ToString(String[] array) {
		String result = "";
		for (String s : array) {
			result = result + s + ", ";
		}
		return result;
	}

	// --------------------------------------------------------------------
	private static String GetErrorLog() {
		StringBuilder result = new StringBuilder();
		result.append("====================================\n");
		result.append("ERROR_LOG\n");
		result.append(ErrorLog);
		return result.toString();
	}

	// --------------------------------------------------------------------
	private static String GetPreferences() {
		StringBuilder result = new StringBuilder();
		result.append("====================================\n");
		result.append("PREFERENCES\n");
		ArrayList<String> list = new ArrayList<>();
		for (Map.Entry<String, ?> entry : PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).getAll().entrySet()) {
			if ( !entry.getKey().startsWith("BalanceReseted") &&
			     !entry.getKey().startsWith("SMSRemovedList") )
				list.add(String.format("%s=%s\n", entry.getKey(), entry.getValue().toString()));
		}

		Collections.sort(list, new Comparator<String>() {
			public int compare(String a, String b) {
				return a.compareTo(b);
			}
		});

		for (String entry : list) {
			result.append(entry);
		}
		result.append("------------------------------------\n");
		return result.toString();
	}

	// -------------------------------------------------------------------
	private static String GetStackTrace(Throwable th) {
		final StringWriter st = new StringWriter();
		st.append("Stacktrace:\n");
		PrintWriter out = new PrintWriter(st);
		th.printStackTrace(out);
		return st.toString();
	}

	/* --------------------------------------------------------------------------
	static String GetFileName(String name) {
		return TAG + name + "DebugInfo_" + new DebugApp().new Info().VersionName + "_"
				+ DateTime.ToString(DateTime.Now()).replace(" ", "_").replace(":", "").replace("-", "") + ".txt";
	}*/

	// --------------------------------------------------------------------------------
	static String GetLogCat() {
		StringBuilder result = new StringBuilder();
		result.append("====================================\n");
		result.append("LOGCAT\n");
		Process process;
		try {
			process = Runtime.getRuntime().exec("logcat -d ActivityManager:I AppWidgetHostView:I HandyNews:I *:S");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				result.append(line + "\n");
			}
		} catch (IOException e1) {
		}
		result.append("------------------------------------\n");
		return result.toString();
	}

	// ------------------------------------------------------------------------
	private static String GetErrorInfo(String text, Exception e) {
		String result = "";
		if (text != null) {
			result += text + "\n";
		}
		if (e != null) {
			result += GetStackTrace(e) + "\n";
			if (e.getMessage() != null) {
				result += e.getMessage();
				Log.e(TAG, e.getMessage());
			}

		}
		return result;
	}

	// ------------------------------------------------------------------------
	public static void AddErrorToLog(String text, Exception e) {
		ErrorLog.append("----------------------\n");
		ErrorLog.append(GetErrorInfo(text, e));
		( e == null ? new Exception() : e ).printStackTrace();
		FetcherService.Status().SetError( text, "", "", e );
	}

	// ------------------------------------------------------------------------
	static String GetWithCaller(String currentName) {
		String result = "";
		try {
			StackTraceElement[] stack = new Exception().getStackTrace();
			result = GetClassName(stack[2].getClassName()) + "." + stack[2].getMethodName() + "->" + currentName;
		} catch (Exception e) {
			DebugApp.AddErrorToLog(null, e);
		}
		return result;
	}

	 //--------------------------------------------------------------------------------
	public static Uri CreateFileUri(String dir, String fileName, String text) {

		Uri result = null;
		fileName = fileName.replace(",", "_");
		fileName = fileName.replace(":", "_");
		File file;
		if (fileName.startsWith("/"))
			file = new File(fileName);
		else {
			file = new File(dir);
			file.mkdirs();
			file = new File(file, fileName);
		}

		try (Writer out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(file.getAbsolutePath()), "UTF-8"))) {
			out.write(text);
			result = FileUtils.INSTANCE.getUriForFile( file );
		} catch (UnsupportedEncodingException e) {
			AddErrorToLog(null, e);
		} catch (IOException e) {
			AddErrorToLog(null, e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	static long getAvailableInternalMemorySizeMB() {
		return DebugApp.getAvailableInternalMemorySize() / c1024 / c1024;
	}

	// --------------------------------------------------------------------------
	private static long getAvailableInternalMemorySize() {
		try {
			File path = Environment.getDataDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			return availableBlocks * blockSize;
		} catch (IllegalArgumentException e) {
			return 0;
		} catch (Exception e) {
			return 0;
		}
	}

	// --------------------------------------------------------------------------
	static long getAvailableExternalMemorySizeMB() {
		return DebugApp.getAvailableExternalMemorySize(Environment.getExternalStorageDirectory()) / c1024 / c1024;
	}
	// --------------------------------------------------------------------------
	@TargetApi(Build.VERSION_CODES.KITKAT)
	static ArrayList<Long> getAvailableExternalMemorysSizeMB() {
		ArrayList<Long> result = new ArrayList<>();
		for ( File item: MainApplication.getContext().getExternalFilesDirs(null) )
			result.add( DebugApp.getAvailableExternalMemorySize( item ) / c1024 / c1024 );
		return result;
	}

	// --------------------------------------------------------------------------
	private static long getAvailableExternalMemorySize(File path) {
		try {
			//File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			return availableBlocks * blockSize;
		} catch (IllegalArgumentException e) {
			return 0;
		} catch (Exception e) {
			return 0;
		}

	}

	// --------------------------------------------------------------------------
	private static String GetClassName(String fullClassName) {
		String s = fullClassName;
		return s.substring(s.lastIndexOf(".") + 1);
	}

	// ------------------------------------------------------------------------
	public static void ShowError(String text, Exception e, Context context ) {
		UiUtils.RunOnGuiThread(() -> Theme.CreateDialog(context )
			.setTitle( R.string.sendErrorConfirm )
			.setNegativeButton(android.R.string.no, (dialogInterface, i) -> dialogInterface.dismiss() )
			.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
				StringBuilder error = new StringBuilder();
				error.append("----------------------\n");
				if (text != null) {
					error.append(text + "\n");
				}
				if (e != null) {
					error.append(GetStackTrace(e) + "\n");
					if (e.getMessage() != null) {
						Log.v(TAG, e.getMessage());
					}
				}
				ShowSendErrorActivity(error.toString());
			}).create().show());

	}

	// --------------------------------------------------------------------------
	private static void ShowSendErrorActivity(String crashText) {
		//SaveExceptionToFile(crashText);
		PrefUtils.putStringCommit("crashText", crashText);
		// MessageBox.Show(crashText, this);
		Intent intent = new Intent(MainApplication.getContext(), SendErrorActivity.class);
		intent.putExtra(SendErrorActivity.cExceptionTextExtra, crashText);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		MainApplication.getContext().startActivity(intent);
	}

	/*private static void SaveExceptionToFile(String crashText) {
		CreateFileUri(Global.dirException,
				"HandyClockException_" + DateTime.ToString(DateTime.NowLong()).replace(":", "").replace(" ", ""), crashText);
	}*/

	/*
	static String GetMemoryUsedText() {
		int cMB = 1024 * 1024;
		int cPageSize = 4 * 1024;

		// Runtime info = Runtime.getRuntime();
		//
		// String text = String.format("T %.2f MB, F %.2f MB, U %.2f MB",
		// ( ( float )info.totalMemory() ) / cMB,
		// ( ( float )info.freeMemory() ) / cMB,
		// ( ( float )( info.totalMemory() - info.freeMemory() ) ) / cMB );
		String text = "";

		int pid = android.os.Process.myPid();
		RandomAccessFile reader;
		try {
			reader = new RandomAccessFile(String.format("/proc/%d/statm", pid), "r");
			String[] list = reader.readLine().split("\\s");
			for (int i = 0; i < list.length; i++) {
				int f = Integer.parseInt(list[i]) * cPageSize / cMB;
				if ((f > 0) && (i > 0))
					// text += String.format("%d: %.1f MB, ", i, f);
					text += String.format("%d ", f);
			}
			text = String
					.format("%.1f MB [%s]", ((float) (Integer.parseInt(list[1]) - Integer.parseInt(list[2]))) * cPageSize / cMB,
							DateTime.IntervalFromNowToString(MainService.FirstUpdateTime, true)) // text
					// )
					// text = String.format("%.1f MB", ( (float)(
					// Integer.parseInt(list[1]) - Integer.parseInt(list[2]) ) )
					// * cPageSize / cMB )
					.replace(",", ".");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text;
	}

	static void CreateTestPhoneItems() {
		DateTime.SaveNow();
		Global.WSSMSList.AddItem(Global.WSSMSList.new SMSItem(1, "Вася Пупкин",
				"Текст пришедшей СМС длинннннный текст длинннннный текст длинннннный текст длинннннный текст длинннннный текст длинннннный текст",
				DateTime.ToCalendar(DateTime.SavedTodayLong + DateTime.MillsInHour * 2)));
		Global.WSSMSList.AddItem(Global.WSSMSList.new SMSItem(2, "900", "Текст пришедшей СМС 2",
				DateTime.ToCalendar(DateTime.SavedTodayLong + DateTime.MillsInHour * 1)));
		Global.WSSMSList.onChangeCO(null);
		Global.WSLastCall.LastDate = DateTime.NowLong();
		Global.WSLastCall.Contact = "Пупкин Василий Игнатьевич";
		Global.WSLastCall.HideTime = DateTime.Now();
		Global.WSLastCall.HideTime.add(Calendar.MINUTE, 1);
		Global.WSLastCall.Enabled = true;
		Global.WSLastCall.Duration = 59;
		// Global.WSLastCall.onChangeCO();
		Global.WSLastCall.SetNeedsUpdate();
		Global.ScrollRemoteFactorySetNeedUpdate();

		// Test.CreateExampleRu();

		Global.EventList().NotifyToDraw("MainActivity.Test", true);
	}
	*/
}
