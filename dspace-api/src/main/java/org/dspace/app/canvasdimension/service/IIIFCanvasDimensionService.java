/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.canvasdimension.service;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface IIIFCanvasDimensionService {

    void processCommunity(Context context, Community community) throws Exception;

    void processCollection(Context context, Collection collection) throws Exception;

    void processItem(Context context, Item item) throws Exception;

    void setForceProcessing(boolean force);

    void setIsQuiet(boolean quiet);

    void setMax2Process(int max2Process);

    void setSkipList(List<String> skipList);

}
