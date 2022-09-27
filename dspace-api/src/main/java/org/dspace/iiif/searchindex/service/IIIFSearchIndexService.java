/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.searchindex.service;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for the search index service.
 *
 *  @author Michael Spalti  mspalti@willamette.edu
 */
public interface IIIFSearchIndexService {

    /**
     * Checks to see if the OCR processor service
     * is running at the location specified in
     * iiif.cfg.
     */
    boolean checkStatus();

    /**
     * Set dso identifiers to skip.
     * @param skipList
     */
    void setSkipList(List<String> skipList);

    /**
     * Set the maximum number of items to process.
     * @param max
     */
    void setMax2Process(int max);

    /**
     * Set whether to output messages during processing.
     * @param quiet
     */
    void setIsQuiet(boolean quiet);


    /**Indexing task for all items in a community and its sub-communities.
     * @param context
     * @param dso
     * @throws Exception
     */
    int processCommunity(Context context, Community dso, String action) throws Exception;

    /**
     * Indexing task for all items in the collection.
     * @param context
     * @param dso
     * @throws Exception
     */
    int processCollection(Context context, Collection dso, String action) throws Exception;

    /**
     * Adds or deletes item from solr index word_highlighting
     * @param context the DSpace context
     * @param dso the item
     * @param action the processing task (add or delete)
     * @throws Exception
     */
    void processItem(Context context, Item dso, String action);

}
