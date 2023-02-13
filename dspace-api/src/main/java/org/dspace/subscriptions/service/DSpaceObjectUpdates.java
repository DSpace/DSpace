/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions.service;

import java.util.Arrays;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;

/**
 * Interface class which will be used to find all objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public interface DSpaceObjectUpdates {

    /**
     * Send an email to some addresses, concerning a Subscription, using a given dso.
     *
     * @param context current DSpace session.
     */
    @SuppressWarnings("rawtypes")
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency)
            throws SearchServiceException;

    default List<String> getDefaultFilterQueries() {
        return Arrays.asList("search.resourcetype:" + Item.class.getSimpleName(),
                             "-discoverable:" + false,
                             "-withdrawn:" + true);
    }

}
