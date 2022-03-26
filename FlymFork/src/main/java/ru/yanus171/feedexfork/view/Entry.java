package ru.yanus171.feedexfork.view;

public class Entry {
    public Long mID;
    public String mLink;
    public boolean mRestorePosition;

    public Entry(long entryId, String entryLink, boolean restorePosition)  {
        mID = entryId;
        mLink = entryLink;
        mRestorePosition = restorePosition;
    }
    public Entry(long entryId, String entryLink)  {
        mID = entryId;
        mLink = entryLink;
        mRestorePosition = false;
    }
}
