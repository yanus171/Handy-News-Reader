/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.activity;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;

public class AddGoogleNewsActivity extends BaseActivity {

    private static final int[] TOPIC_NAME = new int[]{R.string.google_news_top_stories, R.string.google_news_world, R.string.google_news_business,
            R.string.google_news_technology, R.string.google_news_entertainment, R.string.google_news_sports, R.string.google_news_science, R.string.google_news_health};

    private static final int[] CB_IDS = new int[]{R.id.cb_top_stories, R.id.cb_world, R.id.cb_business, R.id.cb_technology, R.id.cb_entertainment,
            R.id.cb_sports, R.id.cb_science, R.id.cb_health};
    private EditText mCustomTopicEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_add_google_news);
        mCustomTopicEditText = findViewById(R.id.google_news_custom_topic);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.google_news_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.google_news, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_validate:
                for (int topic = 0; topic < TOPIC_NAME.length; topic++) {
                    if (((CheckBox) findViewById(CB_IDS[topic])).isChecked()) {
                        String url = "https://news.google.com/rss/search?hl=" + Locale.getDefault().getLanguage() + "&q=" + getString(TOPIC_NAME[topic]);
                        FeedDataContentProvider.addFeed(this, url, getString(TOPIC_NAME[topic]), null, true, false, false,"");
                    }
                }

                String custom_topic = mCustomTopicEditText.getText().toString();
                if(!custom_topic.isEmpty())
                {
                    try {
                        String url = "https://news.google.com/rss/search?hl=" + Locale.getDefault().getLanguage() + "&q=" + URLEncoder.encode(custom_topic, "UTF-8");
                        FeedDataContentProvider.addFeed(this, url, custom_topic, null,  true, false, false, "");
                    } catch (UnsupportedEncodingException ignored) {
                    }
                }

                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

