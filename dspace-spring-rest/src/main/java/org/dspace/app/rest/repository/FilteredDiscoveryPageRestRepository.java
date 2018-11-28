/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.converter.FilteredDiscoveryPageConverter;
import org.dspace.app.rest.model.FilteredDiscoveryPageRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.FilteredDiscoveryPageResource;
import org.dspace.content.ItemRelationshipsType;
import org.dspace.content.service.ItemRelationshipTypeService;
import org.dspace.content.virtual.EntityTypeToFilterQueryService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage FilteredDiscoveryPage Rest objects
 */
@Component(FilteredDiscoveryPageRest.CATEGORY + "." + FilteredDiscoveryPageRest.NAME)
public class FilteredDiscoveryPageRestRepository extends DSpaceRestRepository<FilteredDiscoveryPageRest, String> {

    @Autowired
    private ItemRelationshipTypeService itemRelationshipTypeService;

    @Autowired
    private FilteredDiscoveryPageConverter filteredDiscoveryPageConverter;

    @Autowired
    private EntityTypeToFilterQueryService entityTypeToFilterQueryService;

    public FilteredDiscoveryPageRest findOne(Context context, String string) {
        try {
            return filteredDiscoveryPageConverter
                .fromModel(itemRelationshipTypeService.findByEntityType(context, string));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Page<FilteredDiscoveryPageRest> findAll(Context context, Pageable pageable) {
        List<ItemRelationshipsType> itemRelationshipsTypeList = null;
        try {
            itemRelationshipsTypeList = itemRelationshipTypeService.findAll(context);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        List<ItemRelationshipsType> resultingList = new LinkedList<>();
        for (ItemRelationshipsType itemRelationshipsType : itemRelationshipsTypeList) {
            if (entityTypeToFilterQueryService.hasKey(itemRelationshipsType.getLabel())) {
                resultingList.add(itemRelationshipsType);
            }
        }
        Page<FilteredDiscoveryPageRest> page = utils.getPage(resultingList, pageable)
                                                    .map(filteredDiscoveryPageConverter);
        return page;
    }

    public Class<FilteredDiscoveryPageRest> getDomainClass() {
        return FilteredDiscoveryPageRest.class;
    }

    public DSpaceResource<FilteredDiscoveryPageRest> wrapResource(FilteredDiscoveryPageRest model, String... rels) {
        return new FilteredDiscoveryPageResource(model, utils, rels);
    }
}
