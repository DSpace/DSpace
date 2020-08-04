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
            String entityType = getEntityTypeStringFromMetadata(item);
            if (StringUtils.isNotBlank(entityType)) {
                List<Relationship> relationships = relationshipService.findByItem(context, item);
                for (Relationship relationship : relationships) {
                    fullMetadataValueList
                        .addAll(findRelationshipMetadataValueForItemRelationship(context, item, entityType,
                                relationship, enableVirtualMetadata));
                }

            }
        } catch (SQLException e) {
            log.error("Lookup for Relationships for item with uuid: " + item.getID() + " caused DSpace to crash", e);
        }
        return fullMetadataValueList;
    }

    public String getEntityTypeStringFromMetadata(Item item) {
        List<MetadataValue> list = item.getMetadata();
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

    @Override
    public List<RelationshipMetadataValue> findRelationshipMetadataValueForItemRelationship(
            Context context, Item item, String entityType, Relationship relationship, boolean enableVirtualMetadata)
        throws SQLException {
        List<RelationshipMetadataValue> resultingMetadataValueList = new LinkedList<>();
        RelationshipType relationshipType = relationship.getRelationshipType();
        HashMap<String, VirtualMetadataConfiguration> hashMaps;
        String relationName;
        Item otherItem;
        int place = 0;
        boolean isLeftwards;
        if (StringUtils.equals(relationshipType.getLeftType().getLabel(), entityType)) {
            hashMaps = virtualMetadataPopulator.getMap().get(relationshipType.getLeftwardType());
            otherItem = relationship.getRightItem();
            relationName = relationship.getRelationshipType().getLeftwardType();
            place = relationship.getLeftPlace();
            isLeftwards = false; //if the current item is stored on the left,
            // the name variant is retrieved from the rightwards label
        } else if (StringUtils.equals(relationshipType.getRightType().getLabel(), entityType)) {
            hashMaps = virtualMetadataPopulator.getMap().get(relationshipType.getRightwardType());
            otherItem = relationship.getLeftItem();
            relationName = relationship.getRelationshipType().getRightwardType();
            place = relationship.getRightPlace();
            isLeftwards = true; //if the current item is stored on the right,
            // the name variant is retrieved from the leftwards label
        } else {
            //No virtual metadata can be created
            return resultingMetadataValueList;
        }

        if (hashMaps != null && enableVirtualMetadata) {
            resultingMetadataValueList.addAll(findVirtualMetadataFromConfiguration(context, item, hashMaps,
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

    /**
     * This method will retrieve a list of RelationshipMetadataValue objects based on the config passed along in the
     * hashmaps parameter. The beans will be used to retrieve the values for the RelationshipMetadataValue objects
     * and the keys of the hashmap will be used to construct the RelationshipMetadataValue object.
     *
     * @param context               The context
     * @param item                  The item whose virtual metadata is requested
     * @param hashMaps              The list of VirtualMetadataConfiguration objects which will generate the
     *                              virtual metadata. These configurations are applicable for a relationship
     *                              between both items
     * @param otherItem             The related item whose actual metadata is requested
     * @param relationName          The name of the relationship
     * @param relationship          The relationship whose virtual metadata is requested
     * @param place                 The place to use in the virtual metadata
     * @param isLeftwards           Determines the direction of the virtual metadata
     * @return                      The list of virtual metadata values
     */
    private List<RelationshipMetadataValue> findVirtualMetadataFromConfiguration(Context context, Item item,
        HashMap<String, VirtualMetadataConfiguration> hashMaps, Item otherItem, String relationName,
        Relationship relationship, int place, boolean isLeftwards) throws SQLException {

        List<RelationshipMetadataValue> resultingMetadataValueList = new LinkedList<>();
        for (Map.Entry<String, VirtualMetadataConfiguration> entry : hashMaps.entrySet()) {
            String key = entry.getKey();
            VirtualMetadataConfiguration virtualBean = entry.getValue();

            if (virtualBean.getPopulateWithNameVariant()) {
                String wardLabel = isLeftwards ? relationship.getLeftwardValue() : relationship.getRightwardValue();
                if (wardLabel != null) {
                    resultingMetadataValueList.add(
                        constructRelationshipMetadataValue(context, item, relationship.getID(), place, key, virtualBean,
                                                           wardLabel));
                } else {
                    resultingMetadataValueList.addAll(
                            findRelationshipMetadataValueFromBean(context, item, otherItem, relationship, place, key,
                                    virtualBean));
                }
            } else {
                resultingMetadataValueList.addAll(
                        findRelationshipMetadataValueFromBean(context, item, otherItem, relationship, place, key,
                                virtualBean));
            }
        }
        return resultingMetadataValueList;
    }

    /**
     * This method will retrieve a list of RelationshipMetadataValue objects based on the config passed along in the
     * hashmaps parameter. The beans will be used to retrieve the values for the RelationshipMetadataValue objects
     * and the keys of the hashmap will be used to construct the RelationshipMetadataValue object.
     *
     * @param context               The context
     * @param item                  The item whose virtual metadata is requested
     * @param otherItem             The related item whose actual metadata is requested
     * @param relationship          The relationship whose virtual metadata is requested
     * @param place                 The place to use in the virtual metadata
     * @param key                   The key corresponding to the VirtualMetadataConfiguration
     * @param virtualBean           The VirtualMetadataConfiguration object which will generate the
     *                              virtual metadata. This configuration is applicable for a relationship
     *                              between both items
     * @return                      The list of virtual metadata values
     */
    private List<RelationshipMetadataValue> findRelationshipMetadataValueFromBean(
            Context context, Item item, Item otherItem, Relationship relationship, int place,
            String key, VirtualMetadataConfiguration virtualBean) throws SQLException {
        List<RelationshipMetadataValue> resultingMetadataValueList = new LinkedList<>();
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
        return resultingMetadataValueList;
    }

    //This method will construct a RelationshipMetadataValue object with proper schema, element, qualifier,
    //authority, item, place and useForPlace based on the key String parameter passed along to it
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
        return metadataValue;
    }


    //This method will update a RelationshipMetadataValue object with authority info and relation to the item
    private RelationshipMetadataValue constructResultingMetadataValue(Item item, String value,
                                                                      RelationshipMetadataValue metadataValue,
                                                                      Integer relationshipId) {
        metadataValue.setValue(value);
        metadataValue.setAuthority(Constants.VIRTUAL_AUTHORITY_PREFIX + relationshipId);
        metadataValue.setConfidence(-1);
        metadataValue.setDSpaceObject(item);
        return metadataValue;
    }


    // This method will create the Relationship Metadatavalue that describes the relationship type and has the ID
    // of the other item as value
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
