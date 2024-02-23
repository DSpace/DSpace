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

    boolean getEnabled();
    List<FilteredCollection> findFilteredCollections(Context context, Collection<Filter> filters);
    FilteredItems findFilteredItems(Context context, FilteredItemsQuery query);
    List<MetadataField> getMetadataFields(org.dspace.core.Context context, String queryField)
            throws SQLException;

}
