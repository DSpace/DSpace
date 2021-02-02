/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.sql.SQLException;

import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class NBSimpleMetadataAction implements NBAction {
    private String metadata;
    private String metadataSchema;
    private String metadataElement;
    private String metadataQualifier;
    @Autowired
    private ItemService itemService;

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
        String[] split = metadata.split("\\.");
        this.metadataSchema = split[0];
        this.metadataElement = split[1];
        if (split.length == 3) {
            this.metadataQualifier = split[2];
        }
    }

    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, MessageDto message) {
        try {
            itemService.addMetadata(context, item, metadataSchema, metadataElement, metadataQualifier, null,
                    message.getAbstracts());
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }
}
