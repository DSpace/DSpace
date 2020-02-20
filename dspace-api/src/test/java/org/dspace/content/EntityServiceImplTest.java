/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
        Relationship relationship = mock(Relationship.class);
        relationship.setId(9);

        // Mock the state of objects utilized in findByItemId() to meet the success criteria of an invocation
        when(itemService.find(any(), any())).thenReturn(item);
        when(item.getName()).thenReturn("ItemName");

        // The returned Entity's item should match the mocked item's name
        assertEquals("TestFindByItem 0", "ItemName",
                entityService.findByItemId(context, item.getID()).getItem().getName());
    }

    @Test
    public void testGetType() throws Exception {
        // Declare objects utilized in unit test
        Entity entity = mock(Entity.class);
        EntityTypeService entityTypeService = mock(EntityTypeService.class);
        EntityType entityType = entityTypeService.findByEntityType(context, "testType");

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
    public void testGetRelationsByTypeName() throws Exception {
        // Declare objects utilized in unit test
        Relationship relationship = mock(Relationship.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationship.setRelationshipType(relationshipType);

        // Currently this unit test will only test one case with one relationship
        List<Relationship> relationshipList = new ArrayList<>();
        relationshipList.add(relationship);

        // Mock the state of objects utilized in getRelationsByType() to meet the success criteria of an invocation
        when(relationshipService.findByTypeName(context, "leftwardType", -1, -1)).thenReturn(relationshipList);

        // The relation(s) reported from our defined type should match our relationshipList
        assertEquals("TestGetRelationsByLabel 0", relationshipList,
                entityService.getRelationsByTypeName(context, "leftwardType"));
    }

    @Test
    public void testGetAllRelationshipTypes() throws Exception {
        // Declare objects utilized for this test
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationshipType.setLeftType(leftType);
        relationshipType.setLeftType(rightType);

        // Currently this unit test will only test one case with one relationshipType
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        relationshipTypeList.add(relationshipType);

        // Mock the state of objects utilized in getAllRelationshipTypes()
        // to meet the success criteria of the invocation
        when(entity.getItem()).thenReturn(item);
        when(relationshipTypeService.findByEntityType(context, entityService.getType(context, entity), -1, -1))
                .thenReturn(relationshipTypeList);

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
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        relationshipTypeList.add(relationshipType);
        List<MetadataValue> metsList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        metsList.add(metadataValue);

        // Mock the state of objects utilized in getLeftRelationshipTypes()
        // to meet the success criteria of the invocation
        when(itemService.getMetadata(item, "relationship", "type", null, Item.ANY, false)).thenReturn(metsList);
        when(entity.getItem()).thenReturn(item);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(relationshipTypeService.findByEntityType(context, entityService.getType(context, entity), true, -1, -1))
                .thenReturn(relationshipTypeList);

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
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        relationshipTypeList.add(relationshipType);
        List<MetadataValue> metsList = new ArrayList<>();
        MetadataValue metadataValue = mock(MetadataValue.class);
        metsList.add(metadataValue);

        // Mock the state of objects utilized in getRightRelationshipTypes()
        // to meet the success criteria of the invocation
        when(itemService.getMetadata(item, "relationship", "type", null, Item.ANY, false)).thenReturn(metsList);
        when(entity.getItem()).thenReturn(item);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(relationshipTypeService.findByEntityType(context, entityService.getType(context, entity), false, -1, -1))
                .thenReturn(relationshipTypeList);

        // The right relationshipType(s) reported from our mocked Entity should match our relationshipList
        assertEquals("TestGetRightRelationshipTypes 0", relationshipTypeList,
                entityService.getRightRelationshipTypes(context, entity));
    }


    @Test
    public void testGetRelationshipTypesByTypeName() throws Exception {
        // Declare objects utilized in unit test
        List<RelationshipType> list = new ArrayList<>();
        RelationshipType relationshipType = mock(RelationshipType.class);
        list.add(relationshipType);

        // Mock the state of objects utilized in getRelationshipTypesByTypeName()
        // to meet the success criteria of the invocation
        when(relationshipTypeService.findByLeftwardOrRightwardTypeName(context, "leftwardType", -1, -1))
                .thenReturn(list);

        // The RelationshipType(s) reported from our mocked Entity should match our list
        assertEquals("TestGetRelationshipTypesByLabel 0", list,
                entityService.getRelationshipTypesByTypeName(context, "leftwardType"));
    }




}
