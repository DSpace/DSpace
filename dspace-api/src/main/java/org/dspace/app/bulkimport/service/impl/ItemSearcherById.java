/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.bulkimport.service.ItemSearcher;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;

public class ItemSearcherById implements ItemSearcher {

    private ItemService itemService;

    public ItemSearcherById(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public Item searchBy(Context context, String searchParam) throws SQLException {
        UUID uuid = UUIDUtils.fromString(searchParam);
        return itemService.find(context, uuid);
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

}
