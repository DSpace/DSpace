/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A bean implementing the {@link VirtualMetadataConfiguration} interface to achieve the generation of
 * Virtual metadata by traversing the path of relation specified in the config for this bean
 * The Related bean will find the relationshiptype defined in the relationshipTypeString property on
 * the current item and it'll use the related item from that relationship to pass it along to the
 * virtualMetadataConfiguration property which in turn refers to another VirtualBean instance and it continues
 * the chain until it reaches either a Concatenate or Collected bean to retrieve the values. It will then return
 * that value through the chain again and it'll fill the values into the virtual metadata fields that are defined
 * in the map for the first Related bean.
 */
public class Related implements VirtualMetadataConfiguration {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private EntityService entityService;

    /**
     * The String representing the relationshipType that needs to be used to find the next item
     */
    private String relationshipTypeString;
    /**
     * The left or right place that this relationship needs to have to retrieve the proper item
     */
    private Integer place;
    /**
     * The next bean to call its getValues() method on
     */
    private VirtualMetadataConfiguration virtualMetadataConfiguration;

    /**
     * The boolean value indicating whether this field should be used for place or not
     */
    private boolean useForPlace = false;

    /**
     * Generic getter for the relationshipTypeString property of this class
     * @return  The relationshipTypeString property
     */
    public String getRelationshipTypeString() {
        return relationshipTypeString;
    }

    /**
     * Generic setter for the relationshipTypeString property of this class
     * @param relationshipTypeString    The String to which the relationshipTypeString will be set to
     */
    public void setRelationshipTypeString(String relationshipTypeString) {
        this.relationshipTypeString = relationshipTypeString;
    }

    /**
     * Generic getter for the place property of this class
     * @return  The place property
     */
    public Integer getPlace() {
        return place;
    }

    /**
     * Generic setter for the place property of this class
     * @param place The Integer to which the place property will be set to
     */
    public void setPlace(Integer place) {
        this.place = place;
    }

    /**
     * Generic getter for the virtualMetadataConfiguration property of this class
     * @return  The virtualMetadataConfiguration property
     */
    public VirtualMetadataConfiguration getVirtualMetadataConfiguration() {
        return virtualMetadataConfiguration;
    }

    /**
     * Generic setter for the virtualMetadataConfiguration property of this class
     * @param virtualMetadataConfiguration   The VirtualBean to which the
     *                                             virtualMetadataConfiguration property will be set to
     */
    public void setVirtualMetadataConfiguration(VirtualMetadataConfiguration
                                                        virtualMetadataConfiguration) {
        this.virtualMetadataConfiguration = virtualMetadataConfiguration;
    }

    /**
     * Generic setter for the useForPlace property
     * @param useForPlace   The boolean value that the useForPlace property will be set to
     */
    @Override
    public void setUseForPlace(boolean useForPlace) {
        this.useForPlace = useForPlace;
    }

    /**
     * Generic getter for the useForPlace property
     * @return  The useForPlace to be used by this bean
     */
    @Override
    public boolean getUseForPlace() {
        return useForPlace;
    }

    @Override
    public void setPopulateWithNameVariant(boolean populateWithNameVariant) { }

    @Override
    public boolean getPopulateWithNameVariant() {
        return false;
    }

    /**
     * This method will find the correct Relationship from the given item to retrieve the other item from it
     * and pass this along to the next VirtualBean that's stored in this class.
     * @param context   The relevant DSpace context
     * @param item      The item that will be used to find the related item through its relationships
     * @return          The String value of the metadata fields concatened with a seperator as defined
     *                  in the deepest Concatened bean in the chain
     *                  Will return an empty list if no relationships are found
     * @throws SQLException If something goes wrong
     */
    @Override
    public List<String> getValues(Context context, Item item) throws SQLException {
        Entity entity = entityService.findByItemId(context, item.getID());
        EntityType entityType = entityService.getType(context, entity);

        List<RelationshipType> relationshipTypes = entityService.getAllRelationshipTypes(context, entity);
        List<RelationshipType> possibleRelationshipTypes = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypes) {
            if (StringUtils.equals(relationshipType.getLeftwardType(), relationshipTypeString) || StringUtils
                .equals(relationshipType.getRightwardType(), relationshipTypeString)) {
                possibleRelationshipTypes.add(relationshipType);
            }
        }

        List<Relationship> relationships = new LinkedList<>();
        for (RelationshipType relationshipType : possibleRelationshipTypes) {
            relationships.addAll(relationshipService.findByItemAndRelationshipType(context, item, relationshipType));
        }

        for (Relationship relationship : relationships) {
            if (relationship.getRelationshipType().getLeftType() == entityType) {
                if (relationship.getLeftPlace() == place) {
                    Item otherItem = relationship.getRightItem();
                    return virtualMetadataConfiguration.getValues(context, otherItem);
                }
            } else if (relationship.getRelationshipType().getRightType() == entityType) {
                if (relationship.getRightPlace() == place) {
                    Item otherItem = relationship.getLeftItem();
                    return virtualMetadataConfiguration.getValues(context, otherItem);
                }
            }
        }

        //Return an empty list if no relationships were found
        return new LinkedList<>();
    }

}
