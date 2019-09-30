package ru.yanus171.feedexfork.view;

public class Entry {
    public Long mID;
    public String mLink;

    public Entry(long entryId, String entryLink)  {
        mID = entryId;
        mLink = entryLink;
    }
}
