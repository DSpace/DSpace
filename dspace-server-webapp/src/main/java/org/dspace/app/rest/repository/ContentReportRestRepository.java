/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.app.rest.converter.FilteredItemConverter;
import org.dspace.app.rest.model.ContentReportSupportRest;
import org.dspace.app.rest.model.FilteredCollectionsQuery;
import org.dspace.app.rest.model.FilteredCollectionsRest;
import org.dspace.app.rest.model.FilteredItemRest;
import org.dspace.app.rest.model.FilteredItemsQueryPredicate;
import org.dspace.app.rest.model.FilteredItemsQueryRest;
import org.dspace.app.rest.model.FilteredItemsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataField;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.FilteredCollection;
import org.dspace.contentreport.FilteredCollections;
import org.dspace.contentreport.FilteredItems;
import org.dspace.contentreport.FilteredItemsQuery;
import org.dspace.contentreport.QueryPredicate;
import org.dspace.contentreport.service.ContentReportService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This repository serves the content reports ported from DSpace 6.x
 * (Filtered Collections and Filtered Items).
 * @author Jean-François Morin (Université Laval)
 */
@Component(ContentReportSupportRest.CATEGORY + "." + ContentReportSupportRest.NAME)
public class ContentReportRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private ContentReportService contentReportService;
    @Autowired
    private FilteredItemConverter itemConverter;

    public ContentReportSupportRest getContentReportSupport() {
        return new ContentReportSupportRest();
    }

    public FilteredCollectionsRest findFilteredCollections(Context context, FilteredCollectionsQuery query) {
        Set<Filter> filters = query.getFilters();

        List<FilteredCollection> colls = contentReportService.findFilteredCollections(context, filters);
        FilteredCollections report = FilteredCollections.of(colls);

        FilteredCollectionsRest reportRest = FilteredCollectionsRest.of(report);
        reportRest.setId("filteredcollections");
        return reportRest;
    }

    public FilteredItemsRest findFilteredItems(Context context, FilteredItemsQueryRest queryRest, Pageable pageable) {
        List<QueryPredicate> predicates = queryRest.getQueryPredicates().stream()
                .map(pred -> convertPredicate(context, pred))
                .collect(Collectors.toList());
        FilteredItemsQuery query = new FilteredItemsQuery();
        query.setCollections(queryRest.getCollections());
        query.setQueryPredicates(predicates);
        query.setFilters(queryRest.getFilters());
        query.setAdditionalFields(queryRest.getAdditionalFields());
        query.setOffset(pageable.getOffset());
        query.setPageLimit(pageable.getPageSize());

        FilteredItems items = contentReportService.findFilteredItems(context, query);

        List<FilteredItemRest> filteredItemsRest = items.getItems().stream()
                .map(item -> itemConverter.convert(item, Projection.DEFAULT))
                .collect(Collectors.toList());
        FilteredItemsRest report = FilteredItemsRest.of(filteredItemsRest, items.getItemCount());
        report.setId("filtereditems");

        return report;
    }

    private QueryPredicate convertPredicate(Context context, FilteredItemsQueryPredicate predicate) {
        try {
            List<MetadataField> fields = contentReportService.getMetadataFields(context, predicate.getField());
            return QueryPredicate.of(fields, predicate.getOperator(), predicate.getValue());
        } catch (SQLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
