/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authority.service.ItemSearcher;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;

/**
 * Implementation of {@link ItemSearcher} to search the item by id.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemSearcherById implements ItemSearcher {

    private ItemService itemService;

    public ItemSearcherById(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public Item searchBy(Context context, String searchParam, Item source) {
        UUID uuid = UUIDUtils.fromString(searchParam);
        try {
            return itemService.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException("An SQL error occurs searching the item by uuid", e);
        }
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

}
