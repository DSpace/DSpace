/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryMoreLikeThisConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * Search interface that discovery uses
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface SearchService {

    /**
     * Convenient method to call {@link #search(Context, DSpaceObject,
     * DiscoverQuery)} with a null DSpace Object as scope (i.e. all the
     * repository)
     *
     * @param context DSpace Context object.
     * @param query   the discovery query object.
     * @return discovery search result object
     * @throws SearchServiceException if search error
     */
    DiscoverResult search(Context context, DiscoverQuery query)
        throws SearchServiceException;


    /**
     * Convenient method to call @see #search(Context, DSpaceObject,
     * DiscoverQuery, boolean) with includeWithdrawn=false
     *
     * @param context DSpace Context object
     * @param dso     a DSpace Object to use as scope of the search (only results
     *                within this object)
     * @param query   the discovery query object
     * @return discovery search result object
     * @throws SearchServiceException if search error
     */
    DiscoverResult search(Context context, IndexableObject dso, DiscoverQuery query)
        throws SearchServiceException;

    /**
     * Convenience method to call @see #search(Context, DSpaceObject, DiscoverQuery) and getting an iterator for the
     * results
     *
     * @param context   DSpace context object
     * @param dso       a DSpace object to use as a scope of the search
     * @param query     the discovery query object
     * @return          an iterator iterating over all results from the search
     * @throws SearchServiceException   if search error
     */
    Iterator<Item> iteratorSearch(Context context, IndexableObject dso, DiscoverQuery query)
        throws SearchServiceException;


    List<IndexableObject> search(Context context, String query, String orderfield, boolean ascending, int offset,
                                 int max, String... filterquery);

    /**
     * Transforms the given string field and value into a filter query
     *
     * @param context  The relevant DSpace Context.
     * @param field    the field of the filter query
     * @param operator equals/notequals/notcontains/authority/notauthority
     * @param value    the filter query value
     * @param config   (nullable) the discovery configuration (if not null, field's corresponding facet.type checked to
     *                be standard so suffix is not added for equals operator)
     * @return a filter query
     * @throws SQLException if database error
     *                      An exception that provides information on a database access error or other errors.
     */
    DiscoverFilterQuery toFilterQuery(Context context, String field, String operator, String value,
        DiscoveryConfiguration config) throws SQLException;

    List<Item> getRelatedItems(Context context, Item item,
                               DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration);

    /**
     * Method to create a  Query that includes all
     * communities and collections a user may administrate.
     * If a user has the appropriate rights to administrate communities and/or
     * collections we want to look up all contents of those communities and/or
     * collections, ignoring the read policies of the items (e.g. to list all
     * private items of communities/collections the user administrates). This
     * method returns a query to filter for items that belong to those
     * communities/collections only.
     *
     * @param context The relevant DSpace Context.
     * @return query string specific to the user's rights
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    String createLocationQueryForAdministrableItems(Context context) throws SQLException;

    /**
     * Transforms the metadata field of the given sort configuration into the indexed field which we can then use in
     * our Solr queries.
     *
     * @param metadataField the metadata field
     * @param type          see {@link org.dspace.discovery.configuration.DiscoveryConfigurationParameters}
     * @return the indexed field
     */
    String toSortFieldIndex(String metadataField, String type);

    /**
     * Utility method to escape any special characters in a user's query
     *
     * @param query User's query to escape.
     * @return query with any special characters escaped
     */
    String escapeQueryChars(String query);

    FacetYearRange getFacetYearRange(Context context, IndexableObject scope, DiscoverySearchFilterFacet facet,
            List<String> filterQueries, DiscoverQuery parentQuery)
                    throws SearchServiceException;

    /**
     * This method returns us either the highest or lowest value for the field that we give to it
     * depending on what sortOrder we give this method.
     *
     * @param context       The relevant DSpace context
     * @param valueField    The field in solr for which we'll calculate the extreme value
     * @param sortField     The field in solr for which we'll sort the calculated extreme value on
     *                      This is typically the valueField appended with "_sort"
     * @param sortOrder     Entering ascending will return the minimum value
     *                      Entering descending will return the maximum value
     * @return              Returns the min or max value based on the field in the parameters.
     * @throws SearchServiceException
     */
    String calculateExtremeValue(Context context, String valueField,
                                 String sortField, DiscoverQuery.SORT_ORDER sortOrder)
        throws SearchServiceException;
}
