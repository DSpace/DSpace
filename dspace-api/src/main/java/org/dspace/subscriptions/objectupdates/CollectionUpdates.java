/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions.objectupdates;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.FrequencyType;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class which will be used to find
 * all collection objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public class CollectionUpdates implements DSpaceObjectUpdates {

    @Autowired
    private SearchService searchService;

    @Override
    @SuppressWarnings("rawtypes")
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency)
            throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        getDefaultFilterQueries().stream().forEach(fq -> discoverQuery.addFilterQueries(fq));
        discoverQuery.addFilterQueries("location.coll:(" + dSpaceObject.getID() + ")");
        discoverQuery.addFilterQueries("lastModified:" + FrequencyType.findLastFrequency(frequency));
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        return discoverResult.getIndexableObjects();
    }

}