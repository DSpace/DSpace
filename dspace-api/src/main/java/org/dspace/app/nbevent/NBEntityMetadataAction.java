/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class NBEntityMetadataAction implements NBAction {
    private String metadata;
    private String metadataSchema;
    private String metadataElement;
    private String metadataQualifier;
    private String entityType;
    private Map<String, String> entityMetadata;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
        String[] split = splitMetadata(metadata);
        this.metadataSchema = split[0];
        this.metadataElement = split[1];
        this.metadataQualifier = split[2];
    }

    public String[] splitMetadata(String metadata) {
        String[] result = new String[3];
        String[] split = metadata.split("\\.");
        result[0] = split[0];
        result[1] = split[1];
        if (split.length == 3) {
            result[2] = split[2];
        }
        return result;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Map<String, String> getEntityMetadata() {
        return entityMetadata;
    }

    public void setEntityMetadata(Map<String, String> entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, MessageDto message) {
        try {
            if (relatedItem != null) {
                itemService.addMetadata(context, item, metadataSchema, metadataElement, metadataQualifier, null,
                        relatedItem.getName(), relatedItem.getID().toString(), Choices.CF_ACCEPTED);
                itemService.update(context, item);
            } else {
                Collection collection = collectionService.retrieveCollectionByRelationshipType(item, entityType);
                WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
                relatedItem = workspaceItem.getItem();
                for (String key : entityMetadata.keySet()) {
                    String value = getValue(message, key);
                    if (StringUtils.isNotBlank(value)) {
                        String[] targetMetadata = splitMetadata(entityMetadata.get(key));
                        itemService.addMetadata(context, relatedItem, targetMetadata[0], targetMetadata[1],
                                targetMetadata[2], null, value);
                    }
                }
                installItemService.installItem(context, workspaceItem);
                itemService.update(context, relatedItem);
                itemService.addMetadata(context, item, metadataSchema, metadataElement, metadataQualifier, null,
                        relatedItem.getName(), relatedItem.getID().toString(), Choices.CF_ACCEPTED);
                itemService.update(context, item);
            }
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private String getValue(MessageDto message, String key) {
        if (StringUtils.equals(key, "acronym")) {
            return message.getAcronym();
        } else if (StringUtils.equals(key, "code")) {
            return message.getCode();
        } else if (StringUtils.equals(key, "funder")) {
            return message.getFunder();
        } else if (StringUtils.equals(key, "fundingProgram")) {
            return message.getFundingProgram();
        } else if (StringUtils.equals(key, "jurisdiction")) {
            return message.getJurisdiction();
        } else if (StringUtils.equals(key, "openaireId")) {
            return message.getOpenaireId();
        } else if (StringUtils.equals(key, "title")) {
            return message.getTitle();
        }
        return null;
    }
}
