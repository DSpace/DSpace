/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.filler.MetadataConfiguration.MappingDetails;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Implementation of {@link AuthorityImportFiller} that fill the given item
 * starting from the info present in the given metadata, using an inner set of
 * configurations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemMetadataImportFiller implements AuthorityImportFiller {

    private final static String MISSING_METADATA_FOR_POSITION_MSG = "Missing metadata {} for position {} in item {}";

    private static Logger log = LogManager.getLogger(ItemMetadataImportFiller.class);

    private boolean allowsUpdateByDefault = false;

    private Map<String, MetadataConfiguration> configurations;

    private ItemService itemService;

    @Override
    public boolean allowsUpdate(Context context, MetadataValue metadata, Item itemToFill) {

        if (MapUtils.isEmpty(configurations)) {
            return false;
        }

        String metadataField = metadata.getMetadataField().toString('.');
        MetadataConfiguration config = configurations.get(metadataField);
        if (config == null) {
            return false;
        }

        return config.getUpdateEnabled() != null ? config.getUpdateEnabled() : allowsUpdateByDefault;
    }

    @Override
    public void fillItem(Context context, MetadataValue metadata, Item itemToFill) throws SQLException {

        Item sourceItem = (Item) metadata.getDSpaceObject();

        MetadataConfiguration metadataConfiguration = configurations.get(metadata.getMetadataField().toString('.'));
        if (metadataConfiguration == null) {
            return;
        }

        Map<String, MappingDetails> configurationMapping = metadataConfiguration.getMapping();
        for (String additionalMetadataField : configurationMapping.keySet()) {

            MappingDetails mappingDetails = configurationMapping.get(additionalMetadataField);

            List<MetadataValue> metadataValuesToAdd = findMetadata(sourceItem, additionalMetadataField);
            if (mappingDetails.isUseAll()) {
                addAllMetadata(context, mappingDetails, itemToFill, metadataValuesToAdd,
                        metadata);
            } else {
                addSingleMetadataByPlace(context, mappingDetails, itemToFill, metadataValuesToAdd,
                        metadata);
            }
        }

    }

    private void addAllMetadata(Context context, MappingDetails mappingDetails, Item relatedItem,
            List<MetadataValue> metadataValuesToAdd, MetadataValue archivedItemMetadata) throws SQLException {
        if (!mappingDetails.isAppendMode()) {
            itemService.clearMetadata(context, relatedItem, mappingDetails.getTargetMetadataSchema(),
                    mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(), Item.ANY);
        }

        for (MetadataValue metadataValueToAdd : metadataValuesToAdd) {
            String valueToAdd = metadataValueToAdd.getValue();
            itemService.addMetadata(context, relatedItem, mappingDetails.getTargetMetadataSchema(),
                    mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(), null,
                    valueToAdd, null, -1);
        }

    }

    private void addSingleMetadataByPlace(Context context, MappingDetails mappingDetails, Item relatedItem,
            List<MetadataValue> metadataValuesToAdd, MetadataValue sourceMetadata) throws SQLException {

        Item sourceItem = (Item) sourceMetadata.getDSpaceObject();

        int place = sourceMetadata.getPlace();
        if (metadataValuesToAdd.size() < (place + 1)) {
            log.error(MISSING_METADATA_FOR_POSITION_MSG, mappingDetails.getTargetMetadata(), place, sourceItem.getID());
            return;
        }

        if (!mappingDetails.isAppendMode()) {
            itemService.clearMetadata(context, relatedItem, mappingDetails.getTargetMetadataSchema(),
                    mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(), Item.ANY);
        }

        MetadataValue metadataValueToAdd = metadataValuesToAdd.get(place);
        String valueToAdd = metadataValueToAdd.getValue();
        itemService.addMetadata(context, relatedItem, mappingDetails.getTargetMetadataSchema(),
                mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(), null,
                valueToAdd, null, -1);

    }

    private List<MetadataValue> findMetadata(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    public void setAllowsUpdateByDefault(boolean allowsUpdateByDefault) {
        this.allowsUpdateByDefault = allowsUpdateByDefault;
    }

    public void setConfigurations(Map<String, MetadataConfiguration> configurations) {
        this.configurations = configurations;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

}
