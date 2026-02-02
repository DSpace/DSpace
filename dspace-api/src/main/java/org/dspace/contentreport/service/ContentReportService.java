/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport.service;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.dspace.content.MetadataField;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.FilteredCollection;
import org.dspace.contentreport.FilteredItems;
import org.dspace.contentreport.FilteredItemsQuery;
import org.dspace.core.Context;

public interface ContentReportService {

    /**
     * Returns <code>true<</code> if Content Reports are enabled.
     * @return <code>true<</code> if Content Reports are enabled
     */
    boolean getEnabled();

    /**
     * Retrieves item statistics per collection according to a set of Boolean filters.
     * @param context DSpace context
     * @param filters Set of filters
     * @return a list of collections with the requested statistics for each of them
     */
    List<FilteredCollection> findFilteredCollections(Context context, Collection<Filter> filters);

    /**
     * Retrieves a list of items according to a set of criteria.
     * @param context DSpace context
     * @param query structured query to find items against
     * @return a list of items filtered according to the provided query
     */
    FilteredItems findFilteredItems(Context context, FilteredItemsQuery query);

    /**
     * Converts a metadata field name to a list of {@link MetadataField} instances
     * (one if no wildcards are used, possibly more otherwise).
     * @param context DSpace context
     * @param metadataField field to search for
     * @return a corresponding list of {@link MetadataField} entries
     */
    List<MetadataField> getMetadataFields(org.dspace.core.Context context, String metadataField)
            throws SQLException;

}
