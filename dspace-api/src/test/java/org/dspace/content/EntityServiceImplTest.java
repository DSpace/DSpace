/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class EntityServiceImplTest {

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
        assertEquals("ItemName",
            entityService.findByItemId(context, item.getID()).getItem().getName(),
            "TestFindByItem 0");
    }

    @Test
    public void testGetType() throws Exception {
        // Declare objects utilized in unit test
        Entity entity = mock(Entity.class);
        EntityTypeService entityTypeService = mock(EntityTypeService.class);
        EntityType entityType = entityTypeService.findByEntityType(context, "testType");

        // The returned EntityType should equal our defined entityType case
        assertEquals(entityType, entityService.getType(context, entity), "TestGetType 0");
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
        assertEquals(relationshipList, entityService.getLeftRelations(context, entity), "TestGetLeftRelations 0");
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
        assertEquals(relationshipList, entityService.getRightRelations(context, entity), "TestGetLeftRelations 0");
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
        assertEquals(relationshipList,
            entityService.getRelationsByTypeName(context, "leftwardType"),
            "TestGetRelationsByLabel 0");
    }

    @Test
    public void testGetAllRelationshipTypes() throws Exception {
        // Declare objects utilized for this test
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        EntityType entityType = mock(EntityType.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        relationshipType.setLeftType(leftType);
        relationshipType.setLeftType(rightType);

        // Currently this unit test will only test one case with one relationshipType
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        relationshipTypeList.add(relationshipType);

        // Mock the state of objects utilized in getAllRelationshipTypes()
        // to meet the success criteria of the invocation
        when(entity.getItem()).thenReturn(item);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(relationshipTypeService.findByEntityType(context, entityType, -1, -1))
            .thenReturn(relationshipTypeList);

        // The relation(s) reported from our mocked Entity should match our relationshipList
        assertEquals(relationshipTypeList,
            entityService.getAllRelationshipTypes(context, entity),
            "TestGetAllRelationshipTypes 0");
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
        when(entity.getItem()).thenReturn(item);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(relationshipTypeService.findByEntityType(context, entityType, true, -1, -1))
            .thenReturn(relationshipTypeList);

        // The left relationshipType(s) reported from our mocked Entity should match our relationshipList
        assertEquals(relationshipTypeList,
            entityService.getLeftRelationshipTypes(context, entity),
            "TestGetLeftRelationshipTypes 0");
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
        when(entity.getItem()).thenReturn(item);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(relationshipTypeService.findByEntityType(context, entityType, false, -1, -1))
            .thenReturn(relationshipTypeList);

        // The right relationshipType(s) reported from our mocked Entity should match our relationshipList
        assertEquals(relationshipTypeList,
            entityService.getRightRelationshipTypes(context, entity),
            "TestGetRightRelationshipTypes 0");
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
        assertEquals(list,
            entityService.getRelationshipTypesByTypeName(context, "leftwardType"),
            "TestGetRelationshipTypesByLabel 0");
    }


}
