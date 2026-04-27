package ru.yanus171.feedexfork.fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.view.DragNDropExpandableListView;
import ru.yanus171.feedexfork.view.DragNDropListener;

public class EditFeedListDragNDropListener implements DragNDropListener {
    boolean fromHasGroupIndicator = false;
    DragNDropExpandableListView mListView = null;
    EditFeedListDragNDropListener( DragNDropExpandableListView listView ){
        mListView = listView;
    }
    @Override
    public void onStopDrag(View itemView) {
    }

    @Override
    public void onStartDrag(View itemView) {
        fromHasGroupIndicator = itemView.findViewById(R.id.indicator).getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDrop(final int flatPosFrom, final int flatPosTo) {
        final boolean fromIsGroup = ExpandableListView.getPackedPositionType(mListView.getExpandableListPosition(flatPosFrom)) == ExpandableListView.PACKED_POSITION_TYPE_GROUP;
        final boolean toIsGroup = ExpandableListView.getPackedPositionType(mListView.getExpandableListPosition(flatPosTo)) == ExpandableListView.PACKED_POSITION_TYPE_GROUP;

        final boolean fromIsFeedWithoutGroup = fromIsGroup && !fromHasGroupIndicator;

        View toView = mListView.getChildAt(flatPosTo - mListView.getFirstVisiblePosition());
        boolean toIsFeedWithoutGroup = toIsGroup && toView.findViewById(R.id.indicator).getVisibility() != View.VISIBLE;

        final long packedPosTo = mListView.getExpandableListPosition(flatPosTo);
        final int packedGroupPosTo = ExpandableListView.getPackedPositionGroup(packedPosTo);

        if ((fromIsFeedWithoutGroup || !fromIsGroup) && toIsGroup && !toIsFeedWithoutGroup) {
            new AlertDialog.Builder(mListView.getContext()) //
                    .setTitle(R.string.to_group_title) //
                    .setMessage(R.string.to_group_message) //
                    .setPositiveButton(R.string.to_group_into, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ContentValues values = new ContentValues();
                            values.put(FeedData.FeedColumns.PRIORITY, 1);
                            values.put(FeedData.FeedColumns.GROUP_ID, mListView.getItemIdAtPosition(flatPosTo));

                            ContentResolver cr = mListView.getContext().getContentResolver();
                            cr.update(FeedData.FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
                            cr.notifyChange(FeedData.FeedColumns.GROUPS_AND_ROOT_CONTENT_URI, null);
                        }
                    }).setNegativeButton(R.string.to_group_above, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom);
                        }
                    }).show();
        } else {
            moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom);
        }
    }

    @Override
    public void onDrag(int x, int y, ListView listView) {
    }
    private void moveItem(boolean fromIsGroup, boolean toIsGroup, boolean fromIsFeedWithoutGroup, long packedPosTo, int packedGroupPosTo,
                          int flatPosFrom) {
        ContentValues values = new ContentValues();
        ContentResolver cr = mListView.getContext().getContentResolver();

        if (fromIsGroup && toIsGroup) {
            values.put(FeedData.FeedColumns.PRIORITY, packedGroupPosTo + 1);
            cr.update(FeedData.FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
        } else if (!fromIsGroup && toIsGroup) {
            values.put(FeedData.FeedColumns.PRIORITY, packedGroupPosTo + 1);
            values.putNull(FeedData.FeedColumns.GROUP_ID);
            cr.update(FeedData.FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
        } else if ((!fromIsGroup && !toIsGroup) || (fromIsFeedWithoutGroup && !toIsGroup)) {
            int groupPrio = ExpandableListView.getPackedPositionChild(packedPosTo) + 1;
            values.put(FeedData.FeedColumns.PRIORITY, groupPrio);

            int flatGroupPosTo = mListView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(packedGroupPosTo));
            values.put(FeedData.FeedColumns.GROUP_ID, mListView.getItemIdAtPosition(flatGroupPosTo));
            cr.update(FeedData.FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null);
        }
    }
}
