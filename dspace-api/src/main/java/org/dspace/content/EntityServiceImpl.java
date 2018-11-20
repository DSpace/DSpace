/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityServiceImpl implements EntityService {

    @Autowired(required = true)
    protected EntityTypeService entityTypeService;

    @Autowired(required = true)
    protected RelationshipService relationshipService;

    @Autowired(required = true)
    protected RelationshipTypeService relationshipTypeService;

    @Autowired(required = true)
    protected ItemService itemService;

    public Entity findByItemId(Context context, UUID itemId) throws SQLException {
        Item item = itemService.find(context, itemId);
        List<Relationship> relationshipList = relationshipService.findByItem(context, item);
        return new Entity(item, relationshipList);
    }

    public EntityType getType(Context context, Entity entity) throws SQLException {
        Item item = entity.getItem();
        List<MetadataValue> list = itemService.getMetadata(item, "relationship", "type", null, Item.ANY);
        if (!list.isEmpty()) {
            return entityTypeService.findByEntityType(context, list.get(0).getValue());
        } else {
            return null;
        }
    }

    public List<Relationship> getAllRelations(Context context, Entity entity) {
        return entity.getRelationships();
    }

    public List<Relationship> getLeftRelations(Context context, Entity entity) {
        List<Relationship> fullList = this.getAllRelations(context, entity);
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : fullList) {
            if (relationship.getLeftItem() == entity.getItem()) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    public List<Relationship> getRightRelations(Context context, Entity entity) {
        List<Relationship> fullList = this.getAllRelations(context, entity);
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : fullList) {
            if (relationship.getRightItem() == entity.getItem()) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    public List<Relationship> getRelationsByLabel(Context context, String label) throws SQLException {
        List<Relationship> listToReturn = new LinkedList<>();
        List<Relationship> relationshipList = relationshipService.findAll(context);
        for (Relationship relationship : relationshipList) {
            RelationshipType relationshipType = relationship.getRelationshipType();
            if (StringUtils.equals(relationshipType.getLeftLabel(),label) ||
                StringUtils.equals(relationshipType.getRightLabel(),label)) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getAllRelationshipTypes(Context context, Entity entity) throws SQLException {
        EntityType entityType = this.getType(context, entity);
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (relationshipType.getLeftType() == entityType ||
                relationshipType.getRightType() == entityType) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getLeftRelationshipTypes(Context context, Entity entity) throws SQLException {
        EntityType entityType = this.getType(context, entity);
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (relationshipType.getLeftType() == entityType) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getRightRelationshipTypes(Context context, Entity entity) throws SQLException {
        EntityType entityType = this.getType(context, entity);
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (relationshipType.getRightType() == entityType) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getRelationshipTypesByLabel(Context context, String label) throws SQLException {
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (StringUtils.equals(relationshipType.getLeftLabel(),label) ||
                StringUtils.equals(relationshipType.getRightLabel(),label)) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }
}
