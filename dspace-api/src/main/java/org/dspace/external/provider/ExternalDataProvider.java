/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider;

import java.util.List;
import java.util.Optional;

import org.dspace.external.model.ExternalDataObject;

/**
 * This interface should be implemented by all providers that will deal with external data
 */
public interface ExternalDataProvider {

    /**
     * This method will return the SourceIdentifier for the ExternalDataProvider that implements the interface
     * @return  The source identifier as a String
     */
    public String getSourceIdentifier();

    /**
     * This method will take a String id as a parameter and it'll call the ExternalDataProvider's endpoint or data
     * source to retrieve and build the ExternalDataObject
     * @param id    The id on which will be searched
     * @return      An Optional object of ExternalDataObject. This is to indicate that this object may be null.
     *              This ExternalDataObject will return all the data returned by the ExternalDataProvider
     */
    Optional<ExternalDataObject> getExternalDataObject(String id);

    /**
     * This method will query the ExternalDataProvider's endpoint or data source to retrieve and build a list of
     * ExternalDataObjects through a search with the given parameters
     * @param query The query for the search
     * @param start The start of the search
     * @param limit The max amount of records to be returned by the search
     * @return      A list of ExternalDataObjects that were retrieved and built by this search
     */
    List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit);

    /**
     * This method will return a boolean indicating whether this ExternalDataProvider can deal with the given source
     * or not
     * @param source The source on which the check needs to be done
     * @return       A boolean indicating whether this ExternalDataProvider can deal with this source or not
     */
    public boolean supports(String source);

    /**
     * Returns the total amount of results that this source can return for the given query
     * @param query The query to be search on and give the total amount of results
     * @return      The total amount of results that the source can return for the given query
     */
    public int getNumberOfResults(String query);

}
