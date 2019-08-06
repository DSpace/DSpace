/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.virtual.VirtualMetadataConfiguration;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipMetadataServiceImpl implements RelationshipMetadataService {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Autowired(required = true)
    protected RelationshipService relationshipService;

    @Autowired(required = true)
    protected VirtualMetadataPopulator virtualMetadataPopulator;

    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;

    @Override
    public List<RelationshipMetadataValue> getRelationshipMetadata(Item item, boolean enableVirtualMetadata) {
        Context context = new Context();
        List<RelationshipMetadataValue> fullMetadataValueList = new LinkedList<>();
        try {
            List<MetadataValue> list = item.getMetadata();
            String entityType = getEntityTypeStringFromMetadata(list);
            if (StringUtils.isNotBlank(entityType)) {
                List<Relationship> relationships = relationshipService.findByItem(context, item);
                for (Relationship relationship : relationships) {
                    fullMetadataValueList
                        .addAll(handleItemRelationship(context, item, entityType, relationship, enableVirtualMetadata));
                }

            }
        } catch (SQLException e) {
            log.error("Lookup for Relationships for item with uuid: " + item.getID() + " caused DSpace to crash", e);
        }
        return fullMetadataValueList;
    }

    private String getEntityTypeStringFromMetadata(List<MetadataValue> list) {
        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getMetadataField().getMetadataSchema().getName(),
                                   "relationship")
                && StringUtils.equals(mdv.getMetadataField().getElement(),
                                      "type")) {

                return mdv.getValue();
            }
        }
        return null;
    }

    //This method processes the Relationship of an Item and will return a list of RelationshipMetadataValue objects
    //that are generated for this specfic relationship for the item through the config in VirtualMetadataPopulator
    private List<RelationshipMetadataValue> handleItemRelationship(Context context, Item item, String entityType,
                                                                   Relationship relationship,
                                                                   boolean enableVirtualMetadata)
        throws SQLException {
        List<RelationshipMetadataValue> resultingMetadataValueList = new LinkedList<>();
        RelationshipType relationshipType = relationship.getRelationshipType();
        HashMap<String, VirtualMetadataConfiguration> hashMaps;
        String relationName;
        Item otherItem;
        int place = 0;
        boolean isLeftwards;
        if (StringUtils.equals(relationshipType.getLeftType().getLabel(), entityType)) {
            hashMaps = virtualMetadataPopulator.getMap().get(relationshipType.getLeftLabel());
            otherItem = relationship.getRightItem();
            relationName = relationship.getRelationshipType().getLeftLabel();
            place = relationship.getLeftPlace();
            isLeftwards = false; //if the current item is stored on the left, the name variant is retrieved from the rightwards label
        } else if (StringUtils.equals(relationshipType.getRightType().getLabel(), entityType)) {
            hashMaps = virtualMetadataPopulator.getMap().get(relationshipType.getRightLabel());
            otherItem = relationship.getLeftItem();
            relationName = relationship.getRelationshipType().getRightLabel();
            place = relationship.getRightPlace();
            isLeftwards = true; //if the current item is stored on the right, the name variant is retrieved from the leftwards label
        } else {
            //No virtual metadata can be created
            return resultingMetadataValueList;
        }

        if (hashMaps != null && enableVirtualMetadata) {
            resultingMetadataValueList.addAll(handleRelationshipTypeMetadataMapping(context, item, hashMaps,
                                                                                    otherItem, relationName,
                                                                                    relationship, place, isLeftwards));
        }
        RelationshipMetadataValue relationMetadataFromOtherItem =
            getRelationMetadataFromOtherItem(context, otherItem, relationName, relationship.getID(), place);
        if (relationMetadataFromOtherItem != null) {
            resultingMetadataValueList.add(relationMetadataFromOtherItem);
        }
        return resultingMetadataValueList;
    }

    //This method will retrieve a list of RelationshipMetadataValue objects based on the config passed along in the
    //hashmaps parameter. The beans will be used to retrieve the values for the RelationshipMetadataValue objects
    //and the keys of the hashmap will be used to construct the RelationshipMetadataValue object.
    private List<RelationshipMetadataValue> handleRelationshipTypeMetadataMapping(Context context, Item item,
        HashMap<String, VirtualMetadataConfiguration> hashMaps, Item otherItem, String relationName,
        Relationship relationship, int place, boolean isLeftwards) throws SQLException {

        List<RelationshipMetadataValue> resultingMetadataValueList = new LinkedList<>();
        for (Map.Entry<String, VirtualMetadataConfiguration> entry : hashMaps.entrySet()) {
            String key = entry.getKey();
            VirtualMetadataConfiguration virtualBean = entry.getValue();

            if (virtualBean.getPopulateWithNameVariant()) {
                String wardLabel = isLeftwards ? relationship.getLeftwardLabel() : relationship.getRightwardLabel();
                if (wardLabel != null) {
                    resultingMetadataValueList.add(
                        constructRelationshipMetadataValue(context, item, relationship.getID(), place, key, virtualBean,
                                                           wardLabel));
                } else {
                    handleVirtualBeanValues(context, item, otherItem, relationship, place, resultingMetadataValueList,
                                            key, virtualBean);
                }
            } else {
                handleVirtualBeanValues(context, item, otherItem, relationship, place, resultingMetadataValueList, key,
                                        virtualBean);
            }
        }
        return resultingMetadataValueList;
    }

    private void handleVirtualBeanValues(Context context, Item item, Item otherItem, Relationship relationship,
                                         int place, List<RelationshipMetadataValue> resultingMetadataValueList,
                                         String key, VirtualMetadataConfiguration virtualBean) throws SQLException {
        for (String value : virtualBean.getValues(context, otherItem)) {
            RelationshipMetadataValue relationshipMetadataValue = constructRelationshipMetadataValue(context, item,
                                                                                                     relationship
                                                                                                         .getID(),
                                                                                                     place,
                                                                                                     key, virtualBean,
                                                                                                     value);
            if (relationshipMetadataValue != null) {
                resultingMetadataValueList.add(relationshipMetadataValue);
            }
        }
    }

    private RelationshipMetadataValue constructRelationshipMetadataValue(Context context, Item item,
                                                                         Integer relationshipId, int place,
                                                                         String key,
                                                                         VirtualMetadataConfiguration virtualBean,
                                                                         String value) {
        RelationshipMetadataValue metadataValue = constructMetadataValue(context, key);
        if (metadataValue != null) {
            metadataValue = constructResultingMetadataValue(item, value, metadataValue, relationshipId);
            metadataValue.setUseForPlace(virtualBean.getUseForPlace());
            metadataValue.setPlace(place);
            if (StringUtils.isNotBlank(metadataValue.getValue())) {
                return metadataValue;
            }
        }
        return null;
    }

    //This method will construct a RelationshipMetadataValue object with proper schema, element and qualifier based
    //on the key String parameter passed along to it
    private RelationshipMetadataValue constructMetadataValue(Context context, String key) {
        String[] splittedKey = key.split("\\.");
        RelationshipMetadataValue metadataValue = new RelationshipMetadataValue();
        String metadataSchema = splittedKey.length > 0 ? splittedKey[0] : null;
        String metadataElement = splittedKey.length > 1 ? splittedKey[1] : null;
        String metadataQualifier = splittedKey.length > 2 ? splittedKey[2] : null;
        MetadataField metadataField = null;
        try {
            metadataField = metadataFieldService
                .findByElement(context, metadataSchema, metadataElement, metadataQualifier);
        } catch (SQLException e) {
            log.error("Could not find element with MetadataSchema: " + metadataSchema +
                          ", MetadataElement: " + metadataElement + " and MetadataQualifier: " + metadataQualifier, e);
            return null;
        }
        if (metadataField == null) {
            log.error("A MetadataValue was attempted to construct with MetadataField for parameters: " +
                          "metadataschema: {}, metadataelement: {}, metadataqualifier: {}",
                      metadataSchema, metadataElement, metadataQualifier);
            return null;
        }
        metadataValue.setMetadataField(metadataField);
        metadataValue.setLanguage(Item.ANY);
        return metadataValue;
    }


    private RelationshipMetadataValue constructResultingMetadataValue(Item item, String value,
                                                                      RelationshipMetadataValue metadataValue,
                                                                      Integer relationshipId) {
        metadataValue.setValue(value);
        metadataValue.setAuthority(Constants.VIRTUAL_AUTHORITY_PREFIX + relationshipId);
        metadataValue.setConfidence(-1);
        metadataValue.setDSpaceObject(item);
        return metadataValue;
    }


    private RelationshipMetadataValue getRelationMetadataFromOtherItem(Context context, Item otherItem,
                                                                       String relationName,
                                                                       Integer relationshipId, int place) {
        RelationshipMetadataValue metadataValue = constructMetadataValue(context,
                                                                         MetadataSchemaEnum.RELATION
                                                                             .getName() + "." + relationName);
        if (metadataValue != null) {
            metadataValue.setAuthority(Constants.VIRTUAL_AUTHORITY_PREFIX + relationshipId);
            metadataValue.setValue(otherItem.getID().toString());
            metadataValue.setPlace(place);
            return metadataValue;
        }
        return null;
    }
}
