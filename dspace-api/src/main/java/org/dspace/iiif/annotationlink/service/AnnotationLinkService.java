package org.dspace.iiif.annotationlink.service;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface AnnotationLinkService {

    /**
     * Link all OCR files with bitstreams in a community and its
     * sub-communities.
     * @param context
     * @param community
     * @throws Exception
     */
    int processCommunity(Context context, Community community) throws Exception;

    /**
     * Link all OCR files with bitstreams in a collection.
     * @param context
     * @param collection
     * @throws Exception
     */
    int processCollection(Context context, Collection collection) throws Exception;

    /**
     * Link all OCR files with bitstreams for an item.
     * @param context
     * @param item
     * @throws Exception
     */
    void processItem(Context context, Item item) throws Exception;

}
