package org.dspace.app.canvasdimension.service;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface IIIFCanvasDimensionService {

    public void processSite(Context context) throws Exception;

    public void processCommunity(Context context, Community community) throws Exception;

    public void processCollection(Context context, Collection collection) throws Exception;

    public void processItem(Context context, Item item) throws Exception;

    public void setForceProcessing(boolean force);

    public void setIsQuiet(boolean quiet);

    public void setMax2Process(int max2Process);

    public void setSkipList(List<String> skipList);



}
