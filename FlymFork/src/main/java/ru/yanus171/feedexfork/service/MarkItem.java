package ru.yanus171.feedexfork.service;

import java.util.HashSet;

public class MarkItem {
    public final String mCaption;
    public final String mLink;
    public final HashSet<Long> mLabelIDList;

    public MarkItem( String caption, String link, HashSet<Long> labelIDList ) {
        mCaption =  caption;
        mLink = link;
        mLabelIDList = labelIDList;
    }
}
