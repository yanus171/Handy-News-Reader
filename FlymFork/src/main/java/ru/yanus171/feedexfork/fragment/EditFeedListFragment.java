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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.fragment.EntryMenu.updateMenuIcon;
import static ru.yanus171.feedexfork.fragment.EntryMenu.updateMenuWithIcon;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.isGroup;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.ListFragment;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.adapter.FeedsCursorAdapter;
import ru.yanus171.feedexfork.parser.OPML;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.view.DragNDropExpandableListView;

public class EditFeedListFragment extends ListFragment {

    FeedsCursorAdapter mAdapter = null;

    private final ActionMode.Callback mFeedActionModeCallback = new FeedActionCallBack( this );
    private final ActionMode.Callback mGroupActionModeCallBack = new GroupActionModeCallBack( this );


    private DragNDropExpandableListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_feed_list, container, false);

        mListView = rootView.findViewById(android.R.id.list);
        mListView.setFastScrollEnabled(true);
        mListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            long feedID = mListView.getItemIdAtPosition( groupPosition );
            if ( isGroup( feedID ) ) {
                mAdapter.clearSelectedIDs();
                if (v.findViewById(R.id.indicator).getVisibility() != View.VISIBLE) { // This is no a real group
                    startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedColumns.CONTENT_URI(id)));
                    return true;
                }
            } else
                mAdapter.addOrRemoveIdToSelected( feedID );
            return false;
        });
        mListView.setOnChildClickListener((expandableListView, view, groupPosition, childPosition, l) -> {
            if ( mAdapter.hasSelectedIDs() ) {
                int flatPosition = expandableListView.getFlatListPosition(
                        ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                long feedID = mListView.getItemIdAtPosition(flatPosition);
                mAdapter.addOrRemoveIdToSelected( feedID );
            }
            return true;
        });
        mListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                long feedID = mListView.getItemIdAtPosition( position );
                activity.startSupportActionMode(isGroup(feedID) ? mGroupActionModeCallBack: mFeedActionModeCallback);
                mAdapter.setSingleSelectedId( feedID );
            }
            return true;
        });

        mListView.setAdapter(mAdapter = new FeedsCursorAdapter(getActivity(), FeedColumns.GROUPS_AND_ROOT_CONTENT_URI));
        mListView.setDragNDropListener(new EditFeedListDragNDropListener( mListView ) );
        return rootView;
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(0); // This is needed to avoid an activity leak!
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.feed_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
        updateMenuIcon( menu );
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_feed: {
                startActivity(new Intent(Intent.ACTION_INSERT).setData(FeedColumns.CONTENT_URI));
                return true;
            }
            case R.id.menu_add_group: {
                final EditText input = new EditText(getActivity());
                input.setSingleLine(true);
                new AlertDialog.Builder(getActivity()) //
                        .setTitle(R.string.add_group_title) //
                        .setView(input)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        String groupName = input.getText().toString();
                                        if (!groupName.isEmpty()) {
                                            ContentResolver cr = getActivity().getContentResolver();
                                            ContentValues values = new ContentValues();
                                            values.put(FeedColumns.IS_GROUP, true);
                                            values.put(FeedColumns.NAME, groupName);
                                            cr.insert(FeedColumns.GROUPS_CONTENT_URI, values);
                                        }
                                    }
                                }.start();
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
            case R.id.menu_export_to_opml: {
                OPML.OnMenuExportImportClick(getActivity(), OPML.ExportImport.ExportToOPML );
                return true;
            }
            case R.id.menu_import_from_opml: {
                OPML.OnMenuExportImportClick(getActivity(), OPML.ExportImport.Import );
                return true;
            }
            case R.id.menu_create_backup: {
                OPML.OnMenuExportImportClick( getActivity(), OPML.ExportImport.Backup );
                return true;
            }
            case R.id.menu_create_auto_backup: {
                FetcherService.Start(FetcherService.GetIntent(Constants.FROM_AUTO_BACKUP ), false );
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    long getSelectedFeedId() {
        return mAdapter.getSelectedFeedId();
    }

}
