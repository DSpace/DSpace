/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service.impl;

import static org.dspace.content.Item.ANY;
import static org.dspace.content.MetadataSchemaEnum.CRIS;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authority.service.ItemSearchService;
import org.dspace.authority.service.ItemSearcherMapper;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemSearchService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private ItemSearcherMapper mapper;

    @Autowired
    private ItemService itemService;

    @Override
    public Item search(Context context, String searchParam) {
        return search(context, searchParam, null);
    }

    @Override
    public Item search(Context context, String searchParam, String relationshipType) {
        try {
            return performSearch(context, searchParam, relationshipType);
        } catch (SQLException | AuthorizeException ex) {
            String msg = "An error occurs searching an item by " + searchParam;
            msg = StringUtils.isBlank(relationshipType) ? msg : " and relationship type " + relationshipType;
            throw new RuntimeException(msg, ex);
        }
    }

    private Item performSearch(Context context, String searchParam, String relationshipType)
        throws SQLException, AuthorizeException {

        return findByUuid(context, searchParam, relationshipType)
            .or(() -> findByCrisSourceIdAndRelationshipType(context, searchParam, relationshipType))
            .or(() -> findByItemSearcher(context, searchParam, relationshipType))
            .orElse(null);
    }

    private Optional<Item> findByUuid(Context context, String searchParam, String relationshipType)
        throws SQLException {
        UUID uuid = UUIDUtils.fromString(searchParam);
        if (uuid == null) {
            return Optional.empty();
        }
        Item item = itemService.find(context, uuid);
        return Optional.ofNullable(item)
            .filter(i -> hasRelationshipTypeEqualsTo(i, relationshipType));
    }

    private Optional<Item> findByCrisSourceIdAndRelationshipType(Context context, String crisSourceId,
        String relationshipType) {
        Iterator<Item> items = findByCrisSourceId(context, crisSourceId);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(items, Spliterator.ORDERED), false)
            .filter(item -> hasRelationshipTypeEqualsTo(item, relationshipType))
            .findFirst();
    }

    private Optional<Item> findByItemSearcher(Context context, String searchParam, String relationshipType) {
        String[] searchParamSections = searchParam.split(AuthorityValueService.SPLIT);
        if (searchParamSections.length != 2) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.search(context, searchParamSections[0], searchParamSections[1]))
            .filter(item -> hasRelationshipTypeEqualsTo(item, relationshipType));
    }

    private Iterator<Item> findByCrisSourceId(Context context, String crisSourceId) {
        try {
            return itemService.findUnfilteredByMetadataField(context, CRIS.getName(), "sourceId", null, crisSourceId);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException("An error occurs searching items by crisSourceId " + crisSourceId, e);
        }
    }

    private boolean hasRelationshipTypeEqualsTo(Item item, String relationshipType) {
        if (relationshipType == null) {
            return true;
        }
        return relationshipType.equals(itemService.getMetadataFirstValue(item, "relationship", "type", null, ANY));
    }

}
