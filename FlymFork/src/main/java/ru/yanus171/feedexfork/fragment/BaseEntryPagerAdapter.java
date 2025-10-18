package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.fragment.GeneralPrefsFragment.mSetupChanged;

import android.database.Cursor;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import org.jetbrains.annotations.NotNull;

import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.view.Entry;
import ru.yanus171.feedexfork.view.EntryView;

public abstract class BaseEntryPagerAdapter extends PagerAdapter {
    final SparseArray<EntryView> mEntryViews = new SparseArray<>();
    private EntryFragment mEntryFragment = null;
    protected Entry[] mEntriesIds = new Entry[1];

    BaseEntryPagerAdapter( EntryFragment fragment ) {
        mEntryFragment = fragment;
    }
    Cursor getCursor(int pagerPos) {
        EntryView view = GetEntryView( pagerPos );
        if (view != null ) {
            return view.mCursor;
        }
        return null;
    }
    @Override
    public boolean isViewFromObject(@NotNull View view, Object object) {
        return view == object;
    }

    void generateArticleContent(int pagerPos, boolean forceUpdate, boolean invalidateCache ) {
        Dog.d( "EntryPagerAdapter.articleDataWasLoaded" + pagerPos +  ", mAnchor = " + mEntryFragment.mAnchor);

        EntryView view = GetEntryView( pagerPos );
        if (view != null ) {
            view.StatusStartPageLoading();
            if ( invalidateCache ) {
                view.InvalidateContentCache();
                if ( mSetupChanged )
                    view.InvalidateContentCache();
                view.generateArticleContent(forceUpdate );
            }
        }
        mEntryFragment.SetupZones();
    }

    void onResume() {
        for (int i = 0; i < mEntryViews.size(); i++)
            mEntryViews.valueAt(i).onResume();
    }

    void onPause() {
        for (int i = 0; i < mEntryViews.size(); i++)
            mEntryViews.valueAt(i).onPause();
    }

    EntryView GetEntryView(int pagerPos) {
        return mEntryViews.get(pagerPos);
    }
    public EntryView GetEntryView( Entry entry ) {
        for(int i = 0; i < mEntryViews.size(); i++) {
            EntryView view = mEntryViews.get(mEntryViews.keyAt(i));
            if ( view.mEntryId == entry.mID )
                return view;
        }
        return null;
    }


    @Override
    public void destroyItem(ViewGroup container, final int position, @NotNull Object object) {
        Dog.d( "EntryPagerAdapter.destroyItem " + position );
        assert GetEntry(position) != null;
        FetcherService.removeActiveEntryID( GetEntry( position ).mID );
        mEntryFragment.getLoaderManager().destroyLoader(position);
        container.removeView((View) object);
        GetEntryView(position).Destroy();
        mEntryViews.delete(position);
    }

    @NotNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Dog.d( "EntryPagerAdapter.instantiateItem" + position );
        final EntryView view = mEntryFragment.CreateWebEntryView( position, container );
        mEntryViews.put(position, view);

//            NestedScrollView sv = new NestedScrollView( getContext() );
//            sv.addView( view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
//            sv.setFillViewport( true );
//            container.addView(sv, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        view.mLoadTitleOnly = true;
        Entry entry = GetEntry( position );
        if ( entry != null ) {
            FetcherService.addActiveEntryID(entry.mID);
            mEntryFragment.getLoaderManager().restartLoader(position, null, mEntryFragment);
        }

        return view.mView;
    }

    void setData(Cursor cursor ) {
        if (cursor != null && cursor.getCount() > 0) {
            synchronized (this) {
                mEntriesIds = new Entry[cursor.getCount()];
            }
            int i = 0;
            while (cursor.moveToNext()) {
                SetEntryID(i, cursor.getLong(0), cursor.getString(1));
                i++;
            }
        }
    }
    int GetEntryIndexByID( long id ) {
        for ( int index = 0; index < mEntriesIds.length; index++ )
            if ( mEntriesIds[index].mID == id )
                return index;
        return -1;
    }
    public void SetEntryID(int position, long entryID, String entryLink)  {
        synchronized ( this ) {
            mEntriesIds[position] = new Entry(entryID, entryLink );
        }
    }
    Entry GetEntry(int position)  {
        synchronized ( this ) {
            if ( position >= 0 && position < mEntriesIds.length )
                return mEntriesIds[position];
            else
                return null;
        }
    }


}

class EntryPagerAdapter extends BaseEntryPagerAdapter {


    EntryPagerAdapter( EntryFragment fragment ) {
        super( fragment );
    }

    @Override
    public int getCount() {
        synchronized ( this ) {
            return mEntriesIds != null ? mEntriesIds.length : 0;
        }
    }
}

class SingleEntryPagerAdapter extends BaseEntryPagerAdapter {
    SingleEntryPagerAdapter( EntryFragment fragment ) {
        super( fragment );
    }

    @Override
    public int getCount() {
        return 1;
    }


    @NotNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final View view = (View)super.instantiateItem(container, position);
        final EntryView entryView = (EntryView)view.getTag();
        entryView.mLoadTitleOnly = false;
        //entryView.mActivity.mEntryFragment.refreshUI();
        return view;
    }
}

