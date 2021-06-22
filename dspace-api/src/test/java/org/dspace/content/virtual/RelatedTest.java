/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelatedTest {

    @InjectMocks
    private Related related;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private EntityService entityService;

    @Mock
    private VirtualMetadataConfiguration virtualMetadataConfiguration;

    @Mock
    private Context context;


    @Test
    public void testGetRelationshipTypeString() {
        // Setup objects utilized in unit test
        related.setRelationshipTypeString("TestType");
        // The Type String reported should match our defined Type String
        assertEquals("TestGetRelationshipTypeString 0", "TestType", related.getRelationshipTypeString());
    }

    @Test
    public void testSetRelationshipTypeString() {
        // Setup objects utilized in unit test
        related.setRelationshipTypeString("TestType");
        // The Type String reported should match our defined Type String
        assertEquals("TestSetRelationshipTypeString 0", "TestType", related.getRelationshipTypeString());
    }

    @Test
    public void testSetPlace() {
        // Setup objects utilized in unit test
        related.setPlace(0);
        // The place reported should match our defined place
        assertTrue("TestSetPlace 0", 0 == related.getPlace());
    }

    @Test
    public void testGetPlace() {
        // Setup objects utilized in unit test
        related.setPlace(0);
        // The place reported should match our defined place
        assertTrue("TestGetPlace 0", 0 == related.getPlace());
    }

    @Test
    public void testGetVirtualMetadataConfiguration() {
        // The class reported should match our defined virtualMetadataConfiguration.getClass()
        assertEquals("TestGetVirtualMetadataConfiguration 0", virtualMetadataConfiguration.getClass(),
                related.getVirtualMetadataConfiguration().getClass());
    }

    @Test
    public void testSetVirtualMetadataConfiguration() {
        // Setup objects utilized in unit test
        related.setVirtualMetadataConfiguration(virtualMetadataConfiguration);
        // The class reported should match our defined virtualMetadataConfiguration.getClass()
        assertEquals("TestGetVirtualMetadataConfiguration 0", virtualMetadataConfiguration,
                related.getVirtualMetadataConfiguration());
    }

    @Test
    public void testSetUseForPlace() {
        // Setup objects utilized in unit test
        related.setUseForPlace(true);
        // related.getUseForPlace() should return true
        assertEquals("TestSetVirtualMetadataConfiguration 0", true, related.getUseForPlace());
    }

    @Test
    public void testGetUseForPlace() {
        // Setup objects utilized in unit test
        related.setUseForPlace(true);
        // related.getUseForPlace() should return true
        assertEquals("TestSetVirtualMetadataConfiguration 0", true, related.getUseForPlace());
    }

    @Test
    public void testGetValues() throws Exception {
        // Declare objects utilized in unit test
        List<RelationshipType> relationshipTypeList = new ArrayList<>();
        List<Relationship> relationshipList = new ArrayList<>();
        Relationship relationship = mock(Relationship.class);
        Item item = mock(Item.class);
        Entity entity = mock(Entity.class);
        EntityType entityType = mock(EntityType.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        related.setRelationshipTypeString("LeftwardType");
        relationshipTypeList.add(relationshipType);
        relationshipList.add(relationship);
        related.setPlace(0);

        // Mock the state of objects utilized in getRelationsByLabel() to meet the success criteria of an invocation
        when(item.getID()).thenReturn(UUID.randomUUID());
        when(relationshipType.getLeftwardType()).thenReturn("LeftwardType");
        when(relationshipType.getLeftType()).thenReturn(entityType);
        when(entityService.getAllRelationshipTypes(context, entity)).thenReturn(relationshipTypeList);
        when(entityService.findByItemId(context, item.getID())).thenReturn(entity);
        when(entityService.getType(context, entity)).thenReturn(entityType);
        when(relationshipService.findByItemAndRelationshipType(context, item, relationshipType))
                .thenReturn(relationshipList);
        when(relationship.getRelationshipType()).thenReturn(relationshipType);
        when(relationship.getLeftPlace()).thenReturn(0);
        when(relationship.getRightItem()).thenReturn(item);

        // The reported values should match out mocked collection of values
        assertEquals("TestGetValues 0", virtualMetadataConfiguration.getValues(context, item),
                related.getValues(context, item));
        related.setPlace(1);
        // Mock state to hit else if coverage
        assertEquals("TestGetValues 1", virtualMetadataConfiguration.getValues(context, item),
                related.getValues(context, item));
        related.setPlace(2);
        // No match should return empty List
        assertEquals("TestGetValues 2", new ArrayList<>(), related.getValues(context, item));
    }


}
