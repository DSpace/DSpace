/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

/**
 * This is an interface that will deal with all Service level calls for External Data
 */
public interface ExternalDataService {

    /**
     * This method will return a list of ExternalDataProvider objects defined by all ExternalDataProvider spring beans
     * @return  A list of all ExternalDataProvider objects
     */
    public List<ExternalDataProvider> getExternalDataProviders();

    /**
     * This method will return a single ExternalDataProvider which will support the given sourceIdentifier param
     * @param sourceIdentifier  The source identifier that the ExternalDataProvider that will be returned by this
     *                          method has to support
     * @return                  The ExternalDataProvider that supports the given source identifier
     */
    public ExternalDataProvider getExternalDataProvider(String sourceIdentifier);

    /**
     * This method will return an Optional instance of ExternalDataObject for the given source and identifier
     * It will try to retrieve one through an ExternalDataProvider as defined by the source with the given identifier
     * @param source        The source in which the lookup will be done
     * @param identifier    The identifier which will be looked up
     * @return              An Optional instance of ExternalDataObject
     */
    public Optional<ExternalDataObject> getExternalDataObject(String source, String identifier);

    /**
     * This method will return a list of ExternalDataObjects as defined through the source in which they will be
     * searched for, the given query start and limit parameters
     * @param source    The source that defines which ExternalDataProvider is to be used
     * @param query     The query for which the search will be done
     * @param start     The start of the search
     * @param limit     The maximum amount of records to be returned by the search
     * @return          A list of ExternalDataObjects that obey the rules in the parameters
     */
    public List<ExternalDataObject> searchExternalDataObjects(String source, String query, int start, int limit);

    /**
     * This method wil return the total amount of results that will be found for the given query in the given source
     * @param source    The source in which the query will happen to return the number of results
     * @param query     The query to be ran in this source to retrieve the total amount of results
     * @return          The total amount of results that can be returned for this query in the given source
     */
    public int getNumberOfResults(String source, String query);

    /**
     * This method will create a WorkspaceItem in the given Collection based on the given ExternalDataObject.
     * @param context               The relevant DSpace context
     * @param externalDataObject    The relevant ExternalDataObject to be used
     * @param collection            The Collection in which the item will be present
     * @return                      The created Item
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    WorkspaceItem createWorkspaceItemFromExternalDataObject(Context context, ExternalDataObject externalDataObject,
                                                            Collection collection)
        throws AuthorizeException, SQLException;
}
