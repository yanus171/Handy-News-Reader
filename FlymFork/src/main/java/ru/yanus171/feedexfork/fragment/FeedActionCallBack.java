package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.getFeedTitle;
import static ru.yanus171.feedexfork.provider.FeedDataContentProvider.isGroup;
import static ru.yanus171.feedexfork.service.FetcherService.GetExtrenalLinkFeedIDLong;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.view.ActionMode;

import java.util.HashSet;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EditFeedActivity;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.EntryUrlVoc;
import ru.yanus171.feedexfork.utils.UiUtils;

public class FeedActionCallBack implements ActionMode.Callback {
    private final EditFeedListFragment mFragment;

    FeedActionCallBack( EditFeedListFragment fragment ) {
        mFragment = fragment;
    }
    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.feed_context_menu, menu);
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

        switch (item.getItemId()) {
            case R.id.menu_edit:
                if ( mFragment.mAdapter.isSingleIdSelected() ){
                    mFragment.startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedData.FeedColumns.CONTENT_URI(mFragment.getSelectedFeedId())));
                    mode.finish();
                }
                return true;
            case R.id.menu_delete:
                if ( mFragment.mAdapter.isSingleIdSelected() )
                    DeleteFeed(mFragment.getActivity(), FeedData.FeedColumns.CONTENT_URI(mFragment.getSelectedFeedId()), mode);
                else
                    DeleteFeedList( mFragment.getActivity(), mFragment.mAdapter.getSelectedIDs(), mode);
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
    public static void DeleteFeed(final Activity activity, final Uri feedUri, ActionMode mode) {
        new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getFeedTitle( feedUri ))
                .setMessage(R.string.question_delete_feed)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.delete(feedUri, null, null);
                            EntryUrlVoc.INSTANCE.reinit( true );
                        }
                    }.start();
                    if ( mode != null )
                        mode.finish();
                    if ( activity instanceof EditFeedActivity)
                        activity.finish();
                }).setNegativeButton(android.R.string.no, null).show();
    }

    public static void DeleteFeedList( final Activity activity, final HashSet<Long> feedIDs, ActionMode mode) {
        new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle( R.string.delete_several_feeds_title)
                .setMessage(activity.getString( R.string.question_delete_feeds, feedIDs.size() ) )
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            for ( long id: feedIDs )
                                if ( !isGroup( id ) && id != GetExtrenalLinkFeedIDLong())
                                    cr.delete(FeedData.FeedColumns.CONTENT_URI(id), null, null);
                            EntryUrlVoc.INSTANCE.reinit( true );
                            UiUtils.toast( R.string.feed_deleted );
                        }
                    }.start();
                    mode.finish();
                    if ( activity instanceof EditFeedActivity)
                        activity.finish();
                }).setNegativeButton(android.R.string.no, null).show();
    }


}
