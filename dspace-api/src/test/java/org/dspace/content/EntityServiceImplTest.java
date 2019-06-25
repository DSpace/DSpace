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
        // Declare objects utilized in unit test
        Item item = mock(Item.class);
        List<Relationship> relationshipList = new ArrayList<>();
        Relationship relationship = mock(Relationship.class);
        relationship.setId(9);
        relationshipList.add(relationship);

        // Mock the state of objects utilized in findByItemId() to meet the success criteria of an invocation
        when(itemService.find(any(), any())).thenReturn(item);
        when(item.getName()).thenReturn("ItemName");
        when(relationshipService.findByItem(any(), any())).thenReturn(relationshipList);

        // The returned Entity's item should match the mocked item's name
        assertEquals("TestFindByItem 0", "ItemName",
                entityService.findByItemId(context, item.getID()).getItem().getName());
    }

    @Test
    public void testGetType() throws Exception {
        // Declare objects utilized in unit test
        Entity entity = mock(Entity.class);
        EntityTypeService entityTypeService = mock(EntityTypeService.class);
        Item item = mock(Item.class);
        List<MetadataValue> list = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        list.add(metadataValue);
        EntityType entityType = entityTypeService.findByEntityType(context, "testType");

        // Mock the state of objects utilized in getType() to meet the success criteria of an invocation
        when(metadataValue.getValue()).thenReturn("testType");
        when(itemService.getMetadata(item, "relationship", "type", null, Item.ANY)).thenReturn(list);

        // The returned EntityType should equal our defined entityType case
        assertEquals("TestGetType 0", entityType, entityService.getType(context, entity));
    }

    @Test
    public void testGetLeftRelation() {
        // Declare objects utilized in unit test
        Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();
        Relationship relationship = mock(Relationship.class);
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);
        Entity entity = mock(Entity.class);

        // Mock the state of objects utilized in getLeftRelation() to meet the success criteria of an invocation
        when(entity.getItem()).thenReturn(item);
        when(relationship.getLeftItem()).thenReturn(item);
        when(item.getID()).thenReturn(uuid);
        when(entity.getRelationships()).thenReturn(relationshipList);

        // The left relation(s) reported from our mocked Entity should match our relationshipList
        assertEquals("TestGetLeftRelations 0", relationshipList, entityService.getLeftRelations(context, entity));
    }

    @Test
    public void testGetRightRelation() {
        // Declare objects utilized in unit test
        Item item = mock(Item.class);
        UUID uuid = UUID.randomUUID();
        Relationship relationship = mock(Relationship.class);
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);
        Entity entity = mock(Entity.class);

        // Mock the state of objects utilized in getRightRelation() to meet the success criteria of an invocation
        when(entity.getItem()).thenReturn(item);
        when(relationship.getRightItem()).thenReturn(item);
        when(item.getID()).thenReturn(uuid);
        when(entity.getRelationships()).thenReturn(relationshipList);

        // The right relation(s) reported from our mocked Entity should match our relationshipList
        assertEquals("TestGetLeftRelations 0", relationshipList, entityService.getRightRelations(context, entity));
    }

    @Test
    public void testGetRelationsByLabel() throws Exception {
        // Declare objects utilized in unit test
        Relationship relationship = mock(Relationship.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationship.setRelationshipType(relationshipType);

        // Currently this unit test will only test one case with one relationship
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);

        // Mock the state of objects utilized in getRelationsByLabel() to meet the success criteria of an invocation
        when(relationshipService.findAll(context)).thenReturn(relationshipList);
        when(relationship.getRelationshipType()).thenReturn(relationshipType);
        when(relationshipType.getLeftLabel()).thenReturn("leftLabel");
        when(relationshipType.getRightLabel()).thenReturn("rightLabel");

        // The relation(s) reported from our defined label should match our relationshipList
        assertEquals("TestGetRelationsByLabel 0", relationshipList,
                entityService.getRelationsByLabel(context, "leftLabel"));
    }

    @Test
    public void testGetAllRelationshipTypes() throws Exception {
        // Declare objects utilized for this test
        List<MetadataValue> list = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        list.add(metadataValue);
        Item item = mock(Item.class);
        RelationshipTypeDAO relationshipTypeDAO = mock(RelationshipTypeDAO.class);
        Entity entity = mock(Entity.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationshipType.setLeftType(leftType);
        relationshipType.setLeftType(rightType);

        // Currently this unit test will only test one case with one relationshipType
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        relationshipTypeList.add(relationshipType);

        // Mock the state of objects utilized in getAllRelationshipTypes()
        // to meet the success criteria of the invocation
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
        when(entityService.getType(context, entity)).thenReturn(leftType); // Mock

        // The relation(s) reported from our mocked Entity should match our relationshipList
        assertEquals("TestGetAllRelationshipTypes 0", relationshipTypeList,
                entityService.getAllRelationshipTypes(context, entity));
    }

    @Test
    public void testGetLeftRelationshipTypes() throws Exception {
        // Declare objects utilized in unit test
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        EntityType entityType = mock(EntityType.class);
        RelationshipType relationshipType = mock(RelationshipType.class);

        // Currently this unit test will only test one case with one relationshipType
        List<RelationshipType> relationshipTypeList = new LinkedList<>();
        relationshipTypeList.add(relationshipType);
        List<MetadataValue> metsList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        metsList.add(metadataValue);

        // Mock the state of objects utilized in getLeftRelationshipTypes()
        // to meet the success criteria of the invocation
        when(itemService.getMetadata(any(), any(), any(), any(), any())).thenReturn(metsList);
        when(entity.getItem()).thenReturn(item);
        when(entityType.getID()).thenReturn(0);
        when(relationshipTypeService.findAll(any())).thenReturn(relationshipTypeList);
        when(relationshipType.getLeftType()).thenReturn(entityType);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(entityTypeService.findByEntityType(any(), any())).thenReturn(entityType);

        // The left relationshipType(s) reported from our mocked Entity should match our relationshipList
        assertEquals("TestGetLeftRelationshipTypes 0", relationshipTypeList,
                entityService.getLeftRelationshipTypes(context, entity));
    }

    @Test
    public void testGetRightRelationshipTypes() throws Exception {
        // Declare objects utilized in unit test
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        EntityType entityType = mock(EntityType.class);
        RelationshipType relationshipType = mock(RelationshipType.class);

        // Currently this unit test will only test one case with one relationshipType
        List<RelationshipType> relationshipTypeList = new LinkedList<>();
        relationshipTypeList.add(relationshipType);
        List<MetadataValue> metsList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        metsList.add(metadataValue);

        // Mock the state of objects utilized in getRightRelationshipTypes()
        // to meet the success criteria of the invocation
        when(itemService.getMetadata(any(), any(), any(), any(), any())).thenReturn(metsList);
        when(entity.getItem()).thenReturn(item);
        when(entityType.getID()).thenReturn(0);
        when(relationshipTypeService.findAll(any())).thenReturn(relationshipTypeList);
        when(relationshipType.getRightType()).thenReturn(entityType);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(entityTypeService.findByEntityType(any(), any())).thenReturn(entityType);

        // The right relationshipType(s) reported from our mocked Entity should match our relationshipList
        assertEquals("TestGetRightRelationshipTypes 0", relationshipTypeList,
                entityService.getRightRelationshipTypes(context, entity));
    }


    @Test
    public void testGetRelationshipTypesByLabel() throws Exception {
        // Declare objects utilized in unit test
        List<RelationshipType> list = new LinkedList<>();
        RelationshipType relationshipType = mock(RelationshipType.class);
        list.add(relationshipType);

        // Mock the state of objects utilized in getRelationshipTypesByLabel()
        // to meet the success criteria of the invocation
        when(relationshipTypeService.findAll(context)).thenReturn(list);
        when(relationshipType.getLeftLabel()).thenReturn("leftLabel");
        when(relationshipType.getRightLabel()).thenReturn("rightLabel");

        // The RelationshipType(s) reported from our mocked Entity should match our list
        assertEquals("TestGetRelationshipTypesByLabel 0", list,
                entityService.getRelationshipTypesByLabel(context, "leftLabel"));
    }




}
