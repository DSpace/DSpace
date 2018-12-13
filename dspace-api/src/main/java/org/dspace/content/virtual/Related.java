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
 * A bean implementing the {@link VirtualBean} interface to achieve the generation of Virtual metadata
 * by traversing the path of relation specified in the config for this bean
 */
public class Related implements VirtualBean {

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
    private VirtualBean virtualBean;

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
     * Generic getter for the virtualBean property of this class
     * @return  The virtualBean property
     */
    public VirtualBean getVirtualBean() {
        return virtualBean;
    }

    /**
     * Generic setter for the virtualBean property of this class
     * @param virtualBean   The VirtualBean to which the virtualBean property will be set to
     */
    public void setVirtualBean(VirtualBean virtualBean) {
        this.virtualBean = virtualBean;
    }

    /**
     * This method will find the correct Relationship from the given item to retrieve the other item from it
     * and pass this along to the next VirtualBean that's stored in this class.
     * @param context   The relevant DSpace context
     * @param item      The item that will be used to find the related item through its relationships
     * @return          The String value of the metadata fields concatened with a seperator as defined
     *                  in the deepest Concatened bean in the chain
     *                  Will return null if no relationships are found
     * @throws SQLException If something goes wrong
     */
    public List<String> getValues(Context context, Item item) throws SQLException {
        Entity entity = entityService.findByItemId(context, item.getID());
        EntityType entityType = entityService.getType(context, entity);

        List<RelationshipType> relationshipTypes = entityService.getAllRelationshipTypes(context, entity);
        List<RelationshipType> possibleRelationshipTypes = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypes) {
            if (StringUtils.equals(relationshipType.getLeftLabel(), relationshipTypeString) || StringUtils
                .equals(relationshipType.getRightLabel(), relationshipTypeString)) {
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
                    return virtualBean.getValues(context, otherItem);
                }
            } else if (relationship.getRelationshipType().getRightType() == entityType) {
                if (relationship.getRightPlace() == place) {
                    Item otherItem = relationship.getLeftItem();
                    return virtualBean.getValues(context, otherItem);
                }
            }
        }

        return null;
    }
}
