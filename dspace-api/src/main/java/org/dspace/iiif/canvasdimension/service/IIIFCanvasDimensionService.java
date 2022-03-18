/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension.service;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface IIIFCanvasDimensionService {

    /**
     * Set IIIF canvas dimensions on all IIIF items in a community and its
     * sub-communities.
     * @param context
     * @param community
     * @throws Exception
     */
    int processCommunity(Context context, Community community) throws Exception;

    /**
     * Set IIIF canvas dimensions on all IIIF items in a collection.
     * @param context
     * @param collection
     * @throws Exception
     */
    int processCollection(Context context, Collection collection) throws Exception;

    /**
     * Set IIIF canvas dimensions for an item.
     * @param context
     * @param item
     * @throws Exception
     */
    void processItem(Context context, Item item) throws Exception;

    /**
     * Set the force processing property. If true, existing canvas
     * metadata will be replaced.
     * @param force
     */
    void setForceProcessing(boolean force);

    /**
     * Set whether to output messages during processing.
     * @param quiet
     */
    void setIsQuiet(boolean quiet);

    /**
     * Set the maximum number of items to process.
     * @param max2Process
     */
    void setMax2Process(int max2Process);

    /**
     * Set dso identifiers to skip.
     * @param skipList
     */
    void setSkipList(List<String> skipList);

}
