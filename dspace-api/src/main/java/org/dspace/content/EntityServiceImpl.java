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

    @Override
    public Entity findByItemId(Context context, UUID itemId) throws SQLException {

        return findByItemId(context, itemId, -1, -1);
    }

    @Override
    public Entity findByItemId(Context context, UUID itemId, Integer limit, Integer offset) throws SQLException {

        Item item = itemService.find(context, itemId);
        List<Relationship> relationshipList = relationshipService.findByItem(context, item, limit, offset);
        return new Entity(item, relationshipList);
    }

    @Override
    public EntityType getType(Context context, Entity entity) throws SQLException {
        Item item = entity.getItem();
        List<MetadataValue> list = itemService.getMetadata(item, "relationship", "type", null, Item.ANY, false);
        if (!list.isEmpty()) {
            return entityTypeService.findByEntityType(context, list.get(0).getValue());
        } else {
            return null;
        }
    }

    @Override
    public List<Relationship> getLeftRelations(Context context, Entity entity) {
        List<Relationship> fullList = entity.getRelationships();
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : fullList) {
            if (relationship.getLeftItem().getID() == entity.getItem().getID()) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    @Override
    public List<Relationship> getRightRelations(Context context, Entity entity) {
        List<Relationship> fullList = entity.getRelationships();
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : fullList) {
            if (relationship.getRightItem().getID() == entity.getItem().getID()) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    @Override
    public List<Relationship> getRelationsByTypeName(Context context, String typeName) throws SQLException {
        return getRelationsByTypeName(context, typeName, -1, -1);
    }

    @Override
    public List<Relationship> getRelationsByTypeName(Context context, String typeName, Integer limit, Integer offset)
            throws SQLException {
        return relationshipService.findByTypeName(context, typeName, limit, offset);
    }

    @Override
    public List<RelationshipType> getAllRelationshipTypes(Context context, Entity entity) throws SQLException {

        return getAllRelationshipTypes(context, entity, -1, -1);
    }

    @Override
    public List<RelationshipType> getAllRelationshipTypes(Context context, Entity entity, Integer limit, Integer offset)
            throws SQLException {
        return relationshipTypeService.findByEntityType(context, this.getType(context, entity), limit, offset);
    }

    @Override
    public List<RelationshipType> getLeftRelationshipTypes(Context context, Entity entity) throws SQLException {

        return getLeftRelationshipTypes(context, entity, true, -1, -1);
    }

    @Override
    public List<RelationshipType> getLeftRelationshipTypes(Context context, Entity entity, boolean isLeft,
                                                           Integer limit, Integer offset) throws SQLException {
        return relationshipTypeService.findByEntityType(context, this.getType(context, entity), isLeft, limit, offset);
    }

    @Override
    public List<RelationshipType> getRightRelationshipTypes(Context context, Entity entity) throws SQLException {

        return getRightRelationshipTypes(context, entity, false, -1, -1);
    }

    @Override
    public List<RelationshipType> getRightRelationshipTypes(Context context, Entity entity, boolean isLeft,
                                                            Integer limit, Integer offset) throws SQLException {

        return relationshipTypeService.findByEntityType(context, this.getType(context, entity), isLeft, limit, offset);
    }

    @Override
    public List<RelationshipType> getRelationshipTypesByTypeName(Context context, String type) throws SQLException {
        return getRelationshipTypesByTypeName(context, type, -1, -1);
    }

    @Override
    public List<RelationshipType> getRelationshipTypesByTypeName(Context context, String typeName,
                                                                 Integer limit, Integer offset) throws SQLException {
        return relationshipTypeService.findByLeftwardOrRightwardTypeName(context, typeName, limit, offset);
    }
}
