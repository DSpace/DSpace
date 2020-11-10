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

        if (UUIDUtils.fromString(searchParam) != null) {
            Item item = itemService.findByIdOrLegacyId(context, searchParam);
            return item != null && hasRelationshipTypeEqualsTo(item, relationshipType) ? item : null;
        }

        Item item = findByCrisSourceIdAndRelationshipType(context, searchParam, relationshipType);
        if (item != null) {
            return item;
        }

        String[] searchParamSections = searchParam.split(AuthorityValueService.SPLIT);
        if (searchParamSections.length == 2) {
            item = mapper.search(context, searchParamSections[0], searchParamSections[1]);
            return item != null && hasRelationshipTypeEqualsTo(item, relationshipType) ? item : null;
        }

        return null;
    }

    private Item findByCrisSourceIdAndRelationshipType(Context context, String crisSourceId, String relationshipType)
        throws SQLException, AuthorizeException {

        Iterator<Item> items = itemService.findUnfilteredByMetadataField(context, CRIS.getName(),
            "sourceId", null, crisSourceId);

        while (items.hasNext()) {
            Item item = items.next();
            if (hasRelationshipTypeEqualsTo(item, relationshipType)) {
                return item;
            }
        }

        return null;

    }

    private boolean hasRelationshipTypeEqualsTo(Item item, String relationshipType) {
        if (relationshipType == null) {
            return true;
        }
        return relationshipType.equals(itemService.getMetadataFirstValue(item, "relationship", "type", null, ANY));
    }

}
