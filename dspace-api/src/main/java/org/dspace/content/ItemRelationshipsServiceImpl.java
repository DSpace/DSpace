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
import org.dspace.content.service.ItemRelationshipTypeService;
import org.dspace.content.service.ItemRelationshipsService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class ItemRelationshipsServiceImpl implements ItemRelationshipsService {

    @Autowired(required = true)
    protected ItemRelationshipTypeService itemRelationshipTypeService;

    @Autowired(required = true)
    protected RelationshipService relationshipService;

    @Autowired(required = true)
    protected RelationshipTypeService relationshipTypeService;

    @Autowired(required = true)
    protected ItemService itemService;

    public ItemRelationshipsUtil findByItemId(Context context, UUID itemId) throws SQLException {
        Item item = itemService.find(context, itemId);
        List<Relationship> relationshipList = relationshipService.findByItem(context, item);
        return new ItemRelationshipsUtil(item, relationshipList);
    }

    public ItemRelationshipsType getType(Context context, ItemRelationshipsUtil itemRelationshipsUtil)
        throws SQLException {
        Item item = itemRelationshipsUtil.getItem();
        List<MetadataValue> list = itemService.getMetadata(item, "relationship", "type", null, Item.ANY);
        if (!list.isEmpty()) {
            return itemRelationshipTypeService.findByEntityType(context, list.get(0).getValue());
        } else {
            return null;
        }
    }

    public List<Relationship> getAllRelations(Context context, ItemRelationshipsUtil itemRelationshipsUtil) {
        return itemRelationshipsUtil.getRelationships();
    }

    public List<Relationship> getLeftRelations(Context context, ItemRelationshipsUtil itemRelationshipsUtil) {
        List<Relationship> fullList = this.getAllRelations(context, itemRelationshipsUtil);
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : fullList) {
            if (relationship.getLeftItem() == itemRelationshipsUtil.getItem()) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    public List<Relationship> getRightRelations(Context context, ItemRelationshipsUtil itemRelationshipsUtil) {
        List<Relationship> fullList = this.getAllRelations(context, itemRelationshipsUtil);
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : fullList) {
            if (relationship.getRightItem() == itemRelationshipsUtil.getItem()) {
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
            if (StringUtils.equals(relationshipType.getLeftLabel(), label) ||
                StringUtils.equals(relationshipType.getRightLabel(), label)) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getAllRelationshipTypes(Context context, ItemRelationshipsUtil itemRelationshipsUtil)
        throws SQLException {
        ItemRelationshipsType itemRelationshipsType = this.getType(context, itemRelationshipsUtil);
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (relationshipType.getLeftType() == itemRelationshipsType ||
                relationshipType.getRightType() == itemRelationshipsType) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getLeftRelationshipTypes(Context context, ItemRelationshipsUtil itemRelationshipsUtil)
        throws SQLException {
        ItemRelationshipsType itemRelationshipsType = this.getType(context, itemRelationshipsUtil);
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (relationshipType.getLeftType() == itemRelationshipsType) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getRightRelationshipTypes(Context context,
                                                            ItemRelationshipsUtil itemRelationshipsUtil)
        throws SQLException {
        ItemRelationshipsType itemRelationshipsType = this.getType(context, itemRelationshipsUtil);
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (relationshipType.getRightType() == itemRelationshipsType) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }

    public List<RelationshipType> getRelationshipTypesByLabel(Context context, String label) throws SQLException {
        List<RelationshipType> listToReturn = new LinkedList<>();
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            if (StringUtils.equals(relationshipType.getLeftLabel(), label) ||
                StringUtils.equals(relationshipType.getRightLabel(), label)) {
                listToReturn.add(relationshipType);
            }
        }
        return listToReturn;
    }
}
