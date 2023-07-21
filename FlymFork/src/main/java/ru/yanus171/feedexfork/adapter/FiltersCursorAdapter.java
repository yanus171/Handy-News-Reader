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

package ru.yanus171.feedexfork.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns;
import ru.yanus171.feedexfork.utils.LabelVoc;

import static ru.yanus171.feedexfork.activity.EditFeedActivity.getApplyTypeCaption;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupSmallTextView;
import static ru.yanus171.feedexfork.utils.UiUtils.SetupTextView;

import java.util.HashSet;

public class FiltersCursorAdapter extends ResourceCursorAdapter {

    private int mFilterTextColumnPosition;
    private int mApplyTypeColumnPosition;
    private int mIsAcceptRulePosition;

    private int mSelectedFilter = -1;
    private int mIsMarkAsStarredPosition = -1;
    private int mIsRemoveTextPosition = -1;
    private int mLabelIDListPosition = -1;

    public FiltersCursorAdapter(Context context, Cursor cursor) {
        super(context, R.layout.item_rule_list, cursor, 0);
        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView isAcceptRuleTextView = SetupSmallTextView( view, android.R.id.text1);
        TextView labelListTextView = SetupSmallTextView( view, R.id.labelList);
        TextView filterTextTextView = SetupSmallTextView( view, android.R.id.text2);
        TextView isAppliedToTitleTextView = SetupSmallTextView( view, R.id.text3);

        if (cursor.getPosition() == mSelectedFilter) {
            view.setBackgroundResource(android.R.color.holo_blue_dark);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
        }

        boolean isAcceptRule = cursor.getInt(mIsAcceptRulePosition) == 1;
        boolean isMarkAsStarred = cursor.getInt(mIsMarkAsStarredPosition) == 1;
        boolean isRemoveText = cursor.getInt(mIsRemoveTextPosition) == 1;
        labelListTextView.setVisibility( View.GONE );
        if (isMarkAsStarred) {
            isAcceptRuleTextView.setText(R.string.markAsStarred);
            isAcceptRuleTextView.setTextColor( context.getResources().getColor(android.R.color.holo_blue_dark) );
            final HashSet<Long> labelIDList = LabelVoc.INSTANCE.stringToList( cursor.getString(mLabelIDListPosition) );
            if ( !labelIDList.isEmpty() ) {
                labelListTextView.setVisibility(View.VISIBLE);
                labelListTextView.setText(Html.fromHtml(view.getContext().getString(R.string.with_labels) + " " + LabelVoc.INSTANCE.getStringList(labelIDList)));
            }
        } else if (isAcceptRule) {
            isAcceptRuleTextView.setText(R.string.accept);
            isAcceptRuleTextView.setTextColor( context.getResources().getColor(android.R.color.holo_green_dark) );
        } else if (isRemoveText)     {
            isAcceptRuleTextView.setText(R.string.removeText);
            isAcceptRuleTextView.setTextColor( context.getResources().getColor(android.R.color.holo_red_dark) );
        } else {
            isAcceptRuleTextView.setText(R.string.reject);
            isAcceptRuleTextView.setTextColor( context.getResources().getColor(android.R.color.holo_red_dark) );
        }
        filterTextTextView.setText(cursor.getString(mFilterTextColumnPosition));
        isAppliedToTitleTextView.setText( getApplyTypeCaption( cursor.getInt(mApplyTypeColumnPosition)  ) );
    }

    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mFilterTextColumnPosition = cursor.getColumnIndex(FilterColumns.FILTER_TEXT);
            mApplyTypeColumnPosition = cursor.getColumnIndex(FilterColumns.APPLY_TYPE);
            mIsAcceptRulePosition = cursor.getColumnIndex(FilterColumns.IS_ACCEPT_RULE);
            mIsMarkAsStarredPosition = cursor.getColumnIndex(FilterColumns.IS_MARK_STARRED);
            mIsRemoveTextPosition = cursor.getColumnIndex(FilterColumns.IS_REMOVE_TEXT);
            mLabelIDListPosition = cursor.getColumnIndex(FilterColumns.LABEL_ID_LIST);
        }
    }

    public int getSelectedFilter() {
        return mSelectedFilter;
    }

    public void setSelectedFilter(int filterPos) {
        mSelectedFilter = filterPos;
    }
}
