/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.filler.MetadataConfiguration.MappingDetails;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Implementation of {@link AuthorityImportFiller} that fill the given item
 * starting from the info present in the given metadata, using an inner set of
 * configurations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
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

        List<MetadataValueDTO> metadataDTOList = getMetadataListByRelatedItemAndMetadata(context, sourceItem,
                  metadata);

        MetadataConfiguration metadataConfiguration = configurations.get(metadata.getMetadataField().toString('.'));
        addAllMetadata(context, itemToFill, metadataConfiguration, metadataDTOList);

    }

    public List<MetadataValueDTO> getMetadataListByRelatedItemAndMetadata(Context context, Item relatedItem,
            MetadataValue metadata) {

        List<MetadataValueDTO> listToReturn = new ArrayList<MetadataValueDTO>();

        listToReturn.add(createMetadataValueDTO("dc", "title", null, metadata.getLanguage(), metadata.getValue(),
                null, -1));

        MetadataConfiguration metadataConfiguration = configurations.get(metadata.getMetadataField().toString('.'));
        if (metadataConfiguration == null) {
            return listToReturn;
        }

        Map<String, MappingDetails> configurationMapping = metadataConfiguration.getMapping();
        for (String additionalMetadataField : configurationMapping.keySet()) {

            MappingDetails mappingDetails = configurationMapping.get(additionalMetadataField);

            List<MetadataValue> metadataValuesToAdd = findMetadata(relatedItem, additionalMetadataField);
            if (mappingDetails.isUseAll()) {
                listToReturn.addAll(getAllMetadata(mappingDetails, metadataValuesToAdd, metadata));
            } else {
                MetadataValueDTO singleMetadata = getSingleMetadataByPlace(mappingDetails, metadataValuesToAdd,
                        metadata);
                if (singleMetadata != null) {
                    listToReturn.add(singleMetadata);
                }

            }
        }

        return listToReturn;
    }

    private void addAllMetadata(Context context, Item relatedItem, MetadataConfiguration metadataConfiguration,
            List<MetadataValueDTO> metadataValuesToAdd) throws SQLException {

        Map<String, MappingDetails> configurationMapping = new HashMap<String, MetadataConfiguration.MappingDetails>();

        if (metadataConfiguration != null) {
            configurationMapping = metadataConfiguration.getMapping();
        }
        String metadataFieldPrevious = null;
        for (MetadataValueDTO metadataValueToAdd : metadataValuesToAdd) {
            String metadataField = metadataValueToAdd.getSchema() + "." + metadataValueToAdd.getElement() +
                    ((metadataValueToAdd.getQualifier() != null) ? "." + metadataValueToAdd.getQualifier() : "");
            if (metadataFieldPrevious == null || !metadataFieldPrevious.equals(metadataField)) {
                metadataFieldPrevious = metadataField;
                MappingDetails mappingDetails = configurationMapping.get(metadataField);

                if (mappingDetails != null && !mappingDetails.isAppendMode()) {
                    itemService.clearMetadata(context, relatedItem, mappingDetails.getTargetMetadataSchema(),
                            mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(),
                            Item.ANY);
                }
            }

            itemService.addMetadata(context, relatedItem, metadataValueToAdd.getSchema(),
                    metadataValueToAdd.getElement(), metadataValueToAdd.getQualifier(),
                    metadataValueToAdd.getLanguage(), metadataValueToAdd.getValue(),
                    metadataValueToAdd.getAuthority(), metadataValueToAdd.getConfidence());
        }

    }

    private List<MetadataValueDTO> getAllMetadata(MappingDetails mappingDetails,
            List<MetadataValue> metadataValuesToAdd, MetadataValue archivedItemMetadata) {

        List<MetadataValueDTO> listToReturn = new ArrayList<MetadataValueDTO>();

        for (MetadataValue metadataValueToAdd : metadataValuesToAdd) {
            String valueToAdd = metadataValueToAdd.getValue();

            listToReturn.add(createMetadataValueDTO(mappingDetails.getTargetMetadataSchema(),
                    mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(), null,
                    valueToAdd, null, -1));
        }

        return listToReturn;
    }

    private MetadataValueDTO getSingleMetadataByPlace(MappingDetails mappingDetails,
            List<MetadataValue> metadataValuesToAdd, MetadataValue sourceMetadata) {

        Item sourceItem = (Item) sourceMetadata.getDSpaceObject();

        int place = sourceMetadata.getPlace();
        if (metadataValuesToAdd.size() < (place + 1)) {
            log.error(MISSING_METADATA_FOR_POSITION_MSG, mappingDetails.getTargetMetadata(), place, sourceItem.getID());
            return null;
        }

        MetadataValue metadataValueToAdd = metadataValuesToAdd.get(place);
        String valueToAdd = metadataValueToAdd.getValue();

        return createMetadataValueDTO(mappingDetails.getTargetMetadataSchema(),
                mappingDetails.getTargetMetadataElement(), mappingDetails.getTargetMetadataQualifier(),
                null, valueToAdd, null, -1);

    }

    protected MetadataValueDTO createMetadataValueDTO(String schema, String element, String qualifier,
            String lang, String value, String authority, int confidence) {

        MetadataValueDTO metadataValueDTO = new MetadataValueDTO();
        metadataValueDTO.setSchema(schema);
        metadataValueDTO.setElement(element);
        metadataValueDTO.setQualifier(qualifier);
        metadataValueDTO.setValue(value);
        metadataValueDTO.setAuthority(authority);
        metadataValueDTO.setConfidence(confidence);
        metadataValueDTO.setLanguage(lang);

        return metadataValueDTO;
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
