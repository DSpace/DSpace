/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityServiceImplTest  {

    @InjectMocks
    private EntityServiceImpl entityService;

    @Mock
    private Context context;

    @Mock
    private ItemService itemService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private EntityTypeService entityTypeService;

    @Mock
    private EntityType leftType;

    @Mock
    private EntityType rightType;

    @Test
    public void testfindByItemId() throws Exception {
        Item item = mock(Item.class);
        List<Relationship> relationshipList = new ArrayList<>();
        Relationship relationship = mock(Relationship.class);
        relationship.setId(9);
        relationshipList.add(relationship);
        when(itemService.find(any(), any())).thenReturn(item);
        when(item.getName()).thenReturn("ItemName");
        when(relationshipService.findByItem(any(), any())).thenReturn(relationshipList);
        assertEquals("TestFindByItem 0", "ItemName",
                entityService.findByItemId(context, item.getID()).getItem().getName());
    }

    @Test
    public void testGetType() throws Exception {
        Entity entity = mock(Entity.class);
        EntityTypeService entityTypeService = mock(EntityTypeService.class);
        Item item = mock(Item.class);
        List<MetadataValue> list = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        list.add(metadataValue);
        EntityType entityType = entityTypeService.findByEntityType(context, "testType");
        when(metadataValue.getValue()).thenReturn("testType");
        when(itemService.getMetadata(item, "relationship", "type", null, Item.ANY)).thenReturn(list);
        assertEquals("TestGetType 0", entityType, entityService.getType(context, entity));
    }

    @Test
    public void testGetLeftRelation() {
        Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();
        Relationship relationship = mock(Relationship.class);
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);
        Entity entity = mock(Entity.class);
        when(entity.getItem()).thenReturn(item);
        when(relationship.getLeftItem()).thenReturn(item);
        when(item.getID()).thenReturn(uuid);
        when(entity.getRelationships()).thenReturn(relationshipList);
        assertEquals("TestGetLeftRelations 0", relationshipList, entityService.getLeftRelations(context, entity));
    }

    @Test
    public void testGetRightRelation() {
        Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();
        Relationship relationship = mock(Relationship.class);
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);
        Entity entity = mock(Entity.class);
        when(entity.getItem()).thenReturn(item);
        when(relationship.getRightItem()).thenReturn(item);
        when(item.getID()).thenReturn(uuid);
        when(entity.getRelationships()).thenReturn(relationshipList);
        assertEquals("TestGetLeftRelations 0", relationshipList, entityService.getRightRelations(context, entity));
    }

    @Test
    public void testGetRelationsByLabel() throws Exception {
        Relationship relationship = mock(Relationship.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationship.setRelationshipType(relationshipType);
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);
        when(relationshipService.findAll(context)).thenReturn(relationshipList);
        when(relationship.getRelationshipType()).thenReturn(relationshipType);
        when(relationshipType.getLeftLabel()).thenReturn("leftLabel");
        when(relationshipType.getRightLabel()).thenReturn("rightLabel");
        assertEquals("TestGetRelationsByLabel 0", relationshipList,
                entityService.getRelationsByLabel(context, "leftLabel"));
    }

    @Test
    public void testGetAllRelationshipTypes() throws Exception {
        List<MetadataValue> list = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        list.add(metadataValue);
        Item item = mock(Item.class);
        RelationshipTypeDAO relationshipTypeDAO = mock(RelationshipTypeDAO.class);
        Entity entity = mock(Entity.class);
        Relationship relationship = mock(Relationship.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationship.setRelationshipType(relationshipType);
        relationshipType.setLeftType(leftType);
        relationshipType.setLeftType(rightType);
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        relationshipTypeList.add(relationshipType);
        when(metadataValue.getValue()).thenReturn("testType");
        when(entity.getItem()).thenReturn(item);
        when(itemService.getMetadata(item, "relationship", "type", null, Item.ANY)).thenReturn(list);
        when(relationshipTypeDAO.findAll(context, RelationshipType.class)).thenReturn(relationshipTypeList);
        when(relationshipTypeService.findAll(context)).thenReturn(relationshipTypeList);
        when(relationshipType.getLeftType()).thenReturn(leftType);
        when(relationshipType.getRightType()).thenReturn(rightType);
        when(entityTypeService.findByEntityType(context, "value")).thenReturn(leftType);
        when(leftType.getID()).thenReturn(0);
        when(rightType.getID()).thenReturn(1);
        when(entityService.getType(context, entity)).thenReturn(leftType);
        assertEquals("TestGetAllRelationshipTypes 0", relationshipTypeList,
                entityService.getAllRelationshipTypes(context, entity));
    }

    @Test
    public void testGetLeftRelationshipTypes() throws Exception {
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        EntityType entityType = mock(EntityType.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        List<RelationshipType> relationshipTypeList = new LinkedList<>();
        relationshipTypeList.add(relationshipType);
        List<MetadataValue> metsList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        metsList.add(metadataValue);
        when(itemService.getMetadata(any(), any(), any(), any(), any())).thenReturn(metsList);
        when(entity.getItem()).thenReturn(item);
        when(entityType.getID()).thenReturn(0);
        when(relationshipTypeService.findAll(any())).thenReturn(relationshipTypeList);
        when(relationshipType.getLeftType()).thenReturn(entityType);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(entityTypeService.findByEntityType(any(), any())).thenReturn(entityType);

        assertEquals("TestGetLeftRelationshipTypes 0", relationshipTypeList,
                entityService.getLeftRelationshipTypes(context, entity));
    }

    @Test
    public void testGetRightRelationshipTypes() throws Exception {
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        EntityType entityType = mock(EntityType.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        List<RelationshipType> relationshipTypeList = new LinkedList<>();
        relationshipTypeList.add(relationshipType);
        List<MetadataValue> metsList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        metsList.add(metadataValue);
        when(itemService.getMetadata(any(), any(), any(), any(), any())).thenReturn(metsList);
        when(entity.getItem()).thenReturn(item);
        when(entityType.getID()).thenReturn(0);
        when(relationshipTypeService.findAll(any())).thenReturn(relationshipTypeList);
        when(relationshipType.getRightType()).thenReturn(entityType);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(entityTypeService.findByEntityType(any(), any())).thenReturn(entityType);

        assertEquals("TestGetRightRelationshipTypes 0", relationshipTypeList,
                entityService.getRightRelationshipTypes(context, entity));
    }


    @Test
    public void testGetRelationshipTypesByLabel() throws Exception {
        List<RelationshipType> list = new LinkedList<>();
        RelationshipType relationshipType = mock(RelationshipType.class);
        list.add(relationshipType);
        when(relationshipTypeService.findAll(context)).thenReturn(list);
        when(relationshipType.getLeftLabel()).thenReturn("leftLabel");
        when(relationshipType.getRightLabel()).thenReturn("rightLabel");
        assertEquals("TestGetRelationshipTypesByLabel 0", list,
                entityService.getRelationshipTypesByLabel(context, "leftLabel"));
    }




}
