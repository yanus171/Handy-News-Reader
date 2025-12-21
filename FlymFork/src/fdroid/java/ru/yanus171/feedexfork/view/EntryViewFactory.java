package ru.yanus171.feedexfork.view;

import static ru.yanus171.feedexfork.view.EntryView.TAG;

import android.os.Build;
import android.view.ViewGroup;

import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.utils.Dog;

public class EntryViewFactory {
    public static EntryView Create(String link, long entryId, EntryFragment fragment, ViewGroup container, int position) {
        Dog.v( TAG, "EntryView.Create link = " + link);
        return new WebEntryView( fragment, container, entryId, position );
    }
}
