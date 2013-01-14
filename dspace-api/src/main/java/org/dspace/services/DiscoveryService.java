/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import org.dspace.services.discovery.DiscoverException;
import org.dspace.services.discovery.DiscoverQuery;
import org.dspace.services.discovery.DiscoverResult;

/**
 * Search interface
 *
 * @author João Melo <jmelo@lyncode.com>
 */
public interface DiscoveryService {

    /**
     * Method that will be used to search
     * 
     * @param query The query object
     * @throws SearchServiceException
     */
    DiscoverResult search(DiscoverQuery query) throws DiscoverException;
}
