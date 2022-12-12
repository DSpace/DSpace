/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.contentreports.Filter;
import org.dspace.app.rest.converter.FilteredItemConverter;
import org.dspace.app.rest.model.ContentReportsSupportRest;
import org.dspace.app.rest.model.FilteredCollectionRest;
import org.dspace.app.rest.model.FilteredCollectionsQuery;
import org.dspace.app.rest.model.FilteredCollectionsRest;
import org.dspace.app.rest.model.FilteredItemRest;
import org.dspace.app.rest.model.FilteredItemsQuery;
import org.dspace.app.rest.model.FilteredItemsQueryPredicate;
import org.dspace.app.rest.model.FilteredItemsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.FilteredCollectionsReportUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.contentreports.QueryPredicate;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This repository serves the content reports ported from DSpace 6.x
 * (Filtered Collections and Filtered Items).
 * @author Jean-François Morin (Université Laval)
 */
@Component(ContentReportsSupportRest.CATEGORY + "." + ContentReportsSupportRest.NAME)
public class ContentReportsRestRepository extends AbstractDSpaceRestRepository {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContentReportsRestRepository.class);

    @Autowired
    private FilteredCollectionsReportUtils reportUtils;
    @Autowired
    private ItemService itemService;
    @Autowired
    private MetadataFieldService metadataFieldService;
    @Autowired
    private FilteredItemConverter itemConverter;

    public ContentReportsSupportRest getContentReportsSupport() {
        return new ContentReportsSupportRest();
    }

    public FilteredCollectionsRest findFilteredCollections(Context context, FilteredCollectionsQuery query) {
        FilteredCollectionsRest report = new FilteredCollectionsRest();
        report.setId("filteredcollections");
        try {
            Set<Filter> filters = query.getEnabledFilters();
            List<FilteredCollectionRest> colls = reportUtils.getFilteredCollections(context, filters);
            colls.forEach(report::addCollection);
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
        return report;
    }

    public FilteredItemsRest findFilteredItems(Context context, FilteredItemsQuery query, Pageable pageable) {
        FilteredItemsRest report = new FilteredItemsRest();
        report.setId("filtereditems");

        List<QueryPredicate> predicates = query.getQueryPredicates().stream()
                .map(pred -> convertPredicate(context, pred))
                .collect(Collectors.toList());
        List<UUID> collectionUuids = getUuidsFromStrings(query.getCollections());
        List<Filter> filters = query.getFilters().entrySet().stream()
                .filter(e -> e.getValue().booleanValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();

        try {
            List<Item> items = itemService.findByMetadataQuery(context, predicates, collectionUuids, offset, limit);
            for (Item item : items) {
                boolean matchesFilters = filters.stream().allMatch(f -> f.testItem(context, item));
                if (matchesFilters) {
                    FilteredItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
                    report.addItem(itemRest);
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        try {
            long count = itemService.countForMetadataQuery(context, predicates, collectionUuids);
            report.setItemCount(count);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return report;
    }

    private QueryPredicate convertPredicate(Context context, FilteredItemsQueryPredicate predicate) {
        try {
            List<MetadataField> fields = getMetadataFields(context, predicate.getField());
            return QueryPredicate.of(fields, predicate.getOperator(), predicate.getValue());
        } catch (SQLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private List<MetadataField> getMetadataFields(org.dspace.core.Context context, String query_field)
            throws SQLException {
        List<MetadataField> fields = new ArrayList<>();
        if ("*".equals(query_field)) {
            return fields;
        }
        String schema = "";
        String element = "";
        String qualifier = null;
        String[] parts = query_field.split("\\.");
        if (parts.length > 0) {
            schema = parts[0];
        }
        if (parts.length > 1) {
            element = parts[1];
        }
        if (parts.length > 2) {
            qualifier = parts[2];
        }

        if (Item.ANY.equals(qualifier)) {
            fields.addAll(metadataFieldService.findFieldsByElementNameUnqualified(context, schema, element));
        } else {
            MetadataField mf = metadataFieldService.findByElement(context, schema, element, qualifier);
            if (mf != null) {
                fields.add(mf);
            }
        }
        return fields;
    }

    private static List<UUID> getUuidsFromStrings(List<String> collSel) {
        List<UUID> uuids = new ArrayList<>();
        for (String s: collSel) {
            try {
                uuids.add(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid collection UUID: " + s);
            }
        }
        return uuids;
    }

}
