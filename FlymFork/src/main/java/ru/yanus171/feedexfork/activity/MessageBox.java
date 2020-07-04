package ru.yanus171.feedexfork.activity;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.Theme;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.utils.UiUtils.CreateTextView;

public class MessageBox extends BaseActivity {
	static final int PAD = 10;
	// --------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new LinearLayout(this));

		FrameLayout frameLayout = new FrameLayout( this );

		ScrollView scrollView = new ScrollView(this);
		frameLayout.addView( scrollView );

		LinearLayout parent = new LinearLayout(this);
		parent.setOrientation(LinearLayout.VERTICAL);
		parent.setGravity(Gravity.CENTER);
		scrollView.addView(parent);

		TextView label = CreateTextView(this);
		label.setText(getIntent().getStringExtra("Text"));
		label.setPadding(PAD, PAD, PAD, PAD);
		label.setTextColor(Theme.GetMenuFontColor());
		label.setAutoLinkMask(Linkify.ALL);
		label.setTextIsSelectable( true );
		label.setLinkTextColor(Color.LTGRAY);
		label.setFocusable(true);
		label.setFocusableInTouchMode(true);

		if (getIntent().getBooleanExtra("horizontallyScrolling", false)) {
			label.setHorizontallyScrolling(true);
		} else {
			label.setHorizontallyScrolling(false);
		}

		if (getIntent().getBooleanExtra("horizontallyScrolling", false)) {
			HorizontalScrollView hScrollView = new HorizontalScrollView(this);
			hScrollView.addView(label);
			parent.addView(hScrollView);
		} else {
			parent.addView(label, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		}

		Button btn = new Button( this );
		btn.setText( android.R.string.ok );
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		parent.addView(btn);

		View dimFrame = new View( this );
		dimFrame.setId( R.id.dimFrame );
		frameLayout.addView( dimFrame, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );

		View brightnessSlider = new TextView( this );
		brightnessSlider.setId( R.id.brightnessSlider );
		LayoutParams lp = new LayoutParams( 20, LayoutParams.MATCH_PARENT );
		lp.gravity = Gravity.LEFT;
		frameLayout.addView( brightnessSlider, lp);

		setContentView(frameLayout);
	}

	// --------------------------------------------------------------------------------
	public static void Show(String message) {
		Show(message, false);
	}

	// --------------------------------------------------------------------------------
	public static void Show(String message, boolean horizontallyScrolling) {
		Intent intent = GetIntent(message);
		if (horizontallyScrolling) {
			intent.putExtra("horizontallyScrolling", true);
		}
		MainApplication.getContext().startActivity(intent);
	}

	// --------------------------------------------------------------------------------
	static Intent GetIntent(String message) {
		Intent intent = new Intent(MainApplication.getContext(), MessageBox.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("Text", message);
		return intent;
	}

	// ----------------------------------------------------------------------
	public static void Show(int messageID) {
		Show(MainApplication.getContext().getString(messageID));

	}
}
