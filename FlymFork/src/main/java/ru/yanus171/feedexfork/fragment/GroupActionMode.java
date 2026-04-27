package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.getFeedTitle;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.view.ActionMode;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;

class GroupActionModeCallBack implements ActionMode.Callback {
    private final EditFeedListFragment mFragment;

    GroupActionModeCallBack( EditFeedListFragment fragment ) {
        mFragment = fragment;
    }

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.edit_context_menu, menu);
        return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // Return false if nothing is done
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if ( !mFragment.mAdapter.isSingleIdSelected() )
            return false;
        switch (item.getItemId()) {
            case R.id.menu_edit:
                editGroup(mFragment.getSelectedFeedId());
                mode.finish(); // Action picked, so close the CAB
                return true;
            case R.id.menu_delete:
                deleteGroup(mFragment.getSelectedFeedId());

                mode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mFragment.mAdapter.clearSelectedIDs();
    }
    private void deleteGroup(long groupId) {
        new AlertDialog.Builder(mFragment.getActivity()) //
                .setIcon(android.R.drawable.ic_dialog_alert) //
                .setTitle(getFeedTitle( groupId )) //
                .setMessage(R.string.question_delete_group) //
                .setPositiveButton(android.R.string.yes, (dialog, which) -> new Thread() {
                    @Override
                    public void run() {
                        ContentResolver cr = mFragment.getActivity().getContentResolver();
                        cr.delete(FeedData.FeedColumns.GROUPS_CONTENT_URI(groupId), null, null);
                    }
                }.start()).setNegativeButton(android.R.string.no, null).show();
    }

    private void editGroup(long groupId) {
        final EditText input = new EditText(mFragment.getActivity());
        input.setSingleLine(true);
        input.setText(getFeedTitle( groupId ));
        new AlertDialog.Builder(mFragment.getActivity())
                .setTitle(R.string.edit_group_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> new Thread() {
                    @Override
                    public void run() {
                        String groupName = input.getText().toString();
                        if (!groupName.isEmpty()) {
                            ContentResolver cr = mFragment.getActivity().getContentResolver();
                            ContentValues values = new ContentValues();
                            values.put(FeedData.FeedColumns.NAME, groupName);
                            cr.update(FeedData.FeedColumns.CONTENT_URI(groupId), values, null, null);
                        }
                    }
                }.start()).setNegativeButton(android.R.string.cancel, null).show();
    }
}

