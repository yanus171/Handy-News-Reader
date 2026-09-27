package ru.yanus171.feedexfork.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.DebugApp;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.utils.UiUtils.CreateTextView;

public class SendErrorActivity extends Activity {
	public static final String cExceptionTextExtra = "ExceptionTextExtra";
	public static final String cLogPathExtra = "LogPathExtra";

	// --------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String text = getIntent().getStringExtra(cExceptionTextExtra);
		if (text == null) {
			final String logPath = getIntent().getStringExtra(cLogPathExtra);
			text = ReadLogFile(logPath);
		}
		if (text == null)
			text = "";
		final String reportText = text;

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);

		title: {
			TextView labelTitle = CreateTextView(this);
			labelTitle.setText(R.string.criticalErrorOccured);
			layout.addView(labelTitle, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
		}

		text: {
			ScrollView scrollView = new ScrollView(this);
			layout.addView(scrollView, new LayoutParams(LayoutParams.FILL_PARENT, 0, 8));

			TextView labelText = CreateTextView(this);
			labelText.setText(reportText);
			scrollView.addView(labelText, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		}

		btn: {
			LinearLayout layoutBtn = new LinearLayout(this);
			layoutBtn.setOrientation(LinearLayout.HORIZONTAL);
			layout.addView(layoutBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			Button btnSend = new Button( this );
			btnSend.setText(R.string.sendEmail);
			btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// The user report is always sent as an attached file
					// (never as mail body text).
					final Uri uri = DebugApp.SaveReportFile(reportText);
					if (uri == null) {
						UiUtils.toast(R.string.criticalErrorSending);
						return;
					}
					final Intent emailIntent = new Intent(Intent.ACTION_SEND);
					emailIntent.setType("text/plain");
					emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "workyalex@mail.ru" });
					emailIntent.putExtra(Intent.EXTRA_SUBJECT, GetMailSubject());
					emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
					startActivity(Intent.createChooser(emailIntent, getString(R.string.criticalErrorSending)));
					finish();
				}

				private String GetMailSubject() {
					String version = "";
					try {
						version = getBaseContext().getPackageManager().getPackageInfo(getBaseContext().getPackageName(),
								0).versionName;
					} catch (NameNotFoundException e) {
					}
					return String.format("HandyClock error stacktrace %s", version);
				}
			});
			layoutBtn.addView(btnSend, new LayoutParams(0, LayoutParams.FILL_PARENT, 1));

			Button btnCopy = new Button( this );
			btnCopy.setText(R.string.copyToClipboard);
			btnCopy.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(reportText);
					finish();
				}
			});
			layoutBtn.addView(btnCopy, new LayoutParams(0, LayoutParams.FILL_PARENT, 1));

			Button btnCancel = new Button( this );
			btnCancel.setText(android.R.string.cancel);
			btnCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					finish();
				}
			});
			layoutBtn.addView(btnCancel, new LayoutParams(0, LayoutParams.FILL_PARENT, 1));

		}

		setContentView(layout);
	}

	// --------------------------------------------------------------------------------
	private static String ReadLogFile(String path) {
		if (path == null)
			return null;
		final File file = new File(path);
		if (!file.exists())
			return null;
		final StringBuilder sb = new StringBuilder();
		try {
			final Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			try {
				final char[] buf = new char[8192];
				int n;
				while ((n = reader.read(buf)) != -1)
					sb.append(buf, 0, n);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			return null;
		}
		return sb.toString();
	}
}