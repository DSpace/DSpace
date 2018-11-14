package org.dspace.content.virtual;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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

public class Related implements VirtualBean {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private EntityService entityService;

    private String relationshipTypeString;
    private Integer place;
    private VirtualBean virtualBean;

    public String getRelationshipTypeString() {
        return relationshipTypeString;
    }

    public void setRelationshipTypeString(String relationshipTypeString) {
        this.relationshipTypeString = relationshipTypeString;
    }

    public Integer getPlace() {
        return place;
    }

    public void setPlace(Integer place) {
        this.place = place;
    }

    public VirtualBean getVirtualBean() {
        return virtualBean;
    }

    public void setVirtualBean(VirtualBean virtualBean) {
        this.virtualBean = virtualBean;
    }

    public String getValue(Context context, Item item) throws SQLException {
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
                    return virtualBean.getValue(context, otherItem);
                }
            } else if (relationship.getRelationshipType().getRightType() == entityType) {
                if (relationship.getRightPlace() == place) {
                    Item otherItem = relationship.getLeftItem();
                    return virtualBean.getValue(context, otherItem);
                }
            }
        }

        return null;
    }
}
