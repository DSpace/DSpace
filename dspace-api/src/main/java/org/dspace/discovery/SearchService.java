/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryMoreLikeThisConfiguration;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Search interface that discovery uses
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface SearchService {

    /**
     * Convenient method to call @see #search(Context, DSpaceObject,
     * DiscoverQuery) with a null DSpace Object as scope (i.e. all the
     * repository)
     * 
     * @param context
     *            DSpace Context object.
     * @param query
     *            the discovery query object.
     * @throws SearchServiceException
     */
    DiscoverResult search(Context context, DiscoverQuery query)
            throws SearchServiceException;

    /**
     * Convenient method to call @see #search(Context, DSpaceObject,
     * DiscoverQuery, boolean) with includeWithdrawn=false
     * 
     * @param context
     *            DSpace Context object
     * @param dso
     *            a DSpace Object to use as scope of the search (only results
     *            within this object)
     * @param query
     *            the discovery query object
     * @throws SearchServiceException
     */
    DiscoverResult search(Context context, DSpaceObject dso, DiscoverQuery query)
            throws SearchServiceException;

    /**
     * 
     * @param context
     *            DSpace Context object.
     * @param query
     *            the discovery query object.
     * @param includeWithdrawn
     *            use <code>true</code> to include in the results also withdrawn
     *            items that match the query.
     * @throws SearchServiceException
     */
    DiscoverResult search(Context context, DiscoverQuery query,
            boolean includeWithdrawn) throws SearchServiceException;

    /**
     * 
     * @param context
     *            DSpace Context object
     * @param dso
     *            a DSpace Object to use as scope of the search (only results
     *            within this object)
     * @param query
     *            the discovery query object
     * @param includeWithdrawn
     *            use <code>true</code> to include in the results also withdrawn
     *            items that match the query
     * 
     * @throws SearchServiceException
     */
    DiscoverResult search(Context context, DSpaceObject dso, DiscoverQuery query, boolean includeWithdrawn) throws SearchServiceException;

    
    InputStream searchJSON(Context context, DiscoverQuery query, String jsonIdentifier) throws SearchServiceException;

    InputStream searchJSON(Context context, DiscoverQuery query, DSpaceObject dso, String jsonIdentifier) throws SearchServiceException;


    List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery);


    /**
     * Transforms the given string field and value into a filter query
     * @param context the DSpace context
     * @param field the field of the filter query
     * @param value the filter query value
     * @return a filter query
     * @throws SQLException ...
     */
    DiscoverFilterQuery toFilterQuery(Context context, String field, String operator, String value) throws SQLException;

	List<Item> getRelatedItems(Context context, Item item, DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration);

    /**
     * Transforms the metadata field of the given sort configuration into the indexed field which we can then use in our solr queries
     * @param metadataField the metadata field
     * @return the indexed field
     */
    String toSortFieldIndex(String metadataField, String type);
}
