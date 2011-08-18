/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;

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

    DiscoverResult search(Context context, DiscoverQuery query) throws SearchServiceException;

    DiscoverResult search(Context context, DSpaceObject dso, DiscoverQuery query) throws SearchServiceException;

    String searchJSON(DiscoverQuery query, String jsonIdentifier) throws SearchServiceException;

    String searchJSON(DiscoverQuery query, DSpaceObject dso, String jsonIdentifier) throws SearchServiceException;


    List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery);


    /**
     * Transforms the given string into a filter query
     * @param context the DSpace context
     * @param filterQuery the filter query
     * @return a filter query object
     * @throws java.sql.SQLException ...
     */
    DiscoverFilterQuery toFilterQuery(Context context, String filterQuery) throws SQLException;

    /**
     * Transforms the given string field and value into a filter query
     * @param context the DSpace context
     * @param field the field of the filter query
     * @param value the filter query value
     * @return a filter query
     * @throws SQLException ...
     */
    DiscoverFilterQuery toFilterQuery(Context context, String field, String value) throws SQLException;

    /**
     * Transforms the metadata field of the given sort configuration into the indexed field which we can then use in our solr queries
     * @param metadataField the metadata field
     * @return the indexed field
     */
    String toSortFieldIndex(String metadataField, String type);
}
