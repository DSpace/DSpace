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

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.ItemSearchService;
import org.dspace.authority.service.ItemSearcherMapper;
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

    private static final String UUID_PREFIX = "UUID";

    @Autowired
    private ItemSearcherMapper mapper;

    @Autowired
    private ItemService itemService;

    @Override
    public Item search(Context context, String searchParam) throws Exception {
        return search(context, searchParam, null);
    }

    @Override
    public Item search(Context context, String searchParam, String relationshipType) throws Exception {

        if (UUIDUtils.fromString(searchParam) != null) {
            Item item = mapper.search(context, UUID_PREFIX, searchParam);
            return item != null && hasRelationshipTypeEqualsTo(item, relationshipType) ? item : null;
        }

        Item item = findByCrisSourceId(context, searchParam, relationshipType);
        if (item != null) {
            return item;
        }

        String[] searchParamSections = searchParam.split("::");
        if (searchParamSections.length == 2) {
            item = mapper.search(context, searchParamSections[0], searchParamSections[1]);
            return item != null && hasRelationshipTypeEqualsTo(item, relationshipType) ? item : null;
        }

        return null;
    }

    private Item findByCrisSourceId(Context context, String crisSourceId, String relationshipType) throws Exception {

        Iterator<Item> items = itemService.findByMetadataField(context, CRIS.getName(), "sourceId", null, crisSourceId);
        if (StringUtils.isBlank(relationshipType)) {
            return items.hasNext() ? items.next() : null;
        }

        while (items.hasNext()) {
            Item item = items.next();
            if (relationshipType.equals(itemService.getMetadataFirstValue(item, "relationship", "type", null, ANY))) {
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
