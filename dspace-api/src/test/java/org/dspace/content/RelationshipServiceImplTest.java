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

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.utils.RelationshipVersioningUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelationshipServiceImplTest {

    @InjectMocks
    private RelationshipServiceImpl relationshipService;

    @Mock
    private RelationshipDAO relationshipDAO;

    @Mock
    private Context context;

    @Mock
    private Relationship relationship;

    @Mock
    private List<Relationship> relationshipsList;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private ItemService itemService;

    @Mock
    private VirtualMetadataPopulator virtualMetadataPopulator;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private RelationshipMetadataService relationshipMetadataService;

    @Mock
    private EntityTypeService entityTypeService;

    @Mock
    private ConfigurationService configurationService;

    @Spy
    private RelationshipVersioningUtils relationshipVersioningUtils;

    @Before
    public void init() {
        relationshipsList = new ArrayList<>();
        relationshipsList.add(relationship);
    }

    @Test
    public void testFindAll() throws Exception {
        // Mock DAO to return our mocked relationshipsList
        when(relationshipDAO.findAll(context, Relationship.class, -1, -1)).thenReturn(relationshipsList);
        // The reported Relationship(s) should match our relationshipsList
        assertEquals("TestFindAll 0", relationshipsList, relationshipService.findAll(context));
    }

    @Test
    public void testFindByItem() throws Exception {
        // Declare objects utilized in unit test
        List<Relationship> relationshipTest = new ArrayList<>();
        Item cindy = mock(Item.class);
        Item fred = mock(Item.class);
        Item bob = mock(Item.class);
        Item hank = mock(Item.class);
        Item jasper = mock(Item.class);
        Item spot = mock(Item.class);
        RelationshipType hasDog = new RelationshipType();
        RelationshipType hasFather = new RelationshipType();
        RelationshipType hasMother = new RelationshipType();
        hasDog.setLeftwardType("hasDog");
        hasDog.setRightwardType("isDogOf");
        hasFather.setLeftwardType("hasFather");
        hasFather.setRightwardType("isFatherOf");
        hasMother.setLeftwardType("hasMother");
        hasMother.setRightwardType("isMotherOf");

        relationshipTest.add(getRelationship(cindy, spot, hasDog,0,0));
        relationshipTest.add(getRelationship(cindy, jasper, hasDog,0,1));
        relationshipTest.add(getRelationship(cindy, hank, hasFather,0,0));
        relationshipTest.add(getRelationship(fred, cindy, hasMother,0,0));
        relationshipTest.add(getRelationship(bob, cindy, hasMother,1,0));
        when(relationshipService.findByItem(context, cindy, -1, -1, false)).thenReturn(relationshipTest);

        List<Relationship> results = relationshipService.findByItem(context, cindy);
        assertEquals("TestFindByItem 0", relationshipTest, results);
        for (int i = 0; i < relationshipTest.size(); i++) {
            assertEquals("TestFindByItem sort integrity", relationshipTest.get(i), results.get(i));
        }
    }

    @Test
    public void testFindByItemAndRelationshipType() throws Exception {
        // Declare objects utilized in unit test
        List<Relationship> relList = new ArrayList<>();
        Item item = mock(Item.class);
        RelationshipType testRel = new RelationshipType();

        // The Relationship(s) reported should match our our relList, given left place as true
        assertEquals("TestFindByItemAndRelationshipType 0", relList,
                relationshipService.findByItemAndRelationshipType(context, item, testRel, true));
        // The Relationship(s) reported should match our our relList
        assertEquals("TestFindByItemAndRelationshipType 1", relList,
                relationshipService.findByItemAndRelationshipType(context, item, testRel));
    }

    @Test
    public void testFindByRelationshipType() throws Exception {
        // Declare objects utilized in unit test
        List<Relationship> relList = new ArrayList<>();
        RelationshipType testRel = new RelationshipType();

        // The Relationship(s) reported should match our our relList
        assertEquals("TestFindByRelationshipType 0", relList,
                relationshipService.findByRelationshipType(context, testRel));
    }

    @Test
    public void find() throws Exception {
        // Declare objects utilized in unit test
        Relationship relationship = new Relationship();
        relationship.setId(1337);

        // Mock DAO to return our mocked relationship
        when(relationshipDAO.findByID(context, Relationship.class, relationship.getID())).thenReturn(relationship);

        // The reported Relationship should match our mocked relationship
        assertEquals("TestFind 0", relationship, relationshipService.find(context, relationship.getID()));
    }

    @Test
    public void testCreate() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Declare objects utilized in unit test
        Relationship relationship = relationshipDAO.create(context,new Relationship());
        context.turnOffAuthorisationSystem();
        assertEquals("TestCreate 0", relationship, relationshipService.create(context));
        MetadataValue metVal = mock(MetadataValue.class);
        List<MetadataValue> metsList = new ArrayList<>();
        List<Relationship> leftTypelist = new ArrayList<>();
        List<Relationship> rightTypelist = new ArrayList<>();
        Item leftItem = mock(Item.class);
        Item rightItem = mock(Item.class);
        RelationshipType testRel = new RelationshipType();
        EntityType leftEntityType = mock(EntityType.class);
        EntityType rightEntityType = mock(EntityType.class);
        testRel.setLeftType(leftEntityType);
        testRel.setRightType(rightEntityType);
        testRel.setLeftwardType("Entitylabel");
        testRel.setRightwardType("Entitylabel");
        metsList.add(metVal);
        relationship = getRelationship(leftItem, rightItem, testRel, 0,0);
        leftTypelist.add(relationship);
        rightTypelist.add(relationship);

        // Mock the state of objects utilized in create() to meet the success criteria of the invocation
        when(virtualMetadataPopulator
                .isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), true)).thenReturn(true);
        when(authorizeService
                .authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE)).thenReturn(true);
        when(relationshipService.findByItem(context,leftItem)).thenReturn(leftTypelist);
        when(relationshipService.findByItem(context,rightItem)).thenReturn(rightTypelist);
        when(leftEntityType.getLabel()).thenReturn("Entitylabel");
        when(rightEntityType.getLabel()).thenReturn("Entitylabel");
        when(metVal.getValue()).thenReturn("Entitylabel");
        when(metsList.get(0).getValue()).thenReturn("Entitylabel");
        when(relationshipService
                .findByItemAndRelationshipType(context, leftItem, testRel, true)).thenReturn(leftTypelist);
        when(itemService.getMetadata(leftItem, "dspace", "entity", "type", Item.ANY, false)).thenReturn(metsList);
        when(itemService.getMetadata(rightItem, "dspace", "entity", "type", Item.ANY, false)).thenReturn(metsList);
        when(relationshipDAO.create(any(), any())).thenReturn(relationship);

        // The reported Relationship should match our defined relationship
        assertEquals("TestCreate 1", relationship, relationshipService.create(context, relationship));
        // The reported Relationship should match our defined relationship, given left/right item
        // and RelationshipType
        assertEquals("TestCreate 2", relationship, relationshipService.create(context, leftItem, rightItem,
                testRel,0,0));

        context.restoreAuthSystemState();
    }

    @Test
    public void testDelete() throws Exception {

        // Declare objects utilized in unit test
        List<Relationship> leftTypelist = new ArrayList<>();
        List<Relationship> rightTypelist = new ArrayList<>();
        Item leftItem = mock(Item.class);
        Item rightItem = mock(Item.class);
        RelationshipType testRel = new RelationshipType();
        EntityType leftEntityType = mock(EntityType.class);
        EntityType rightEntityType = mock(EntityType.class);
        testRel.setLeftType(leftEntityType);
        testRel.setRightType(rightEntityType);
        testRel.setLeftwardType("Entitylabel");
        testRel.setRightwardType("Entitylabel");
        testRel.setLeftMinCardinality(0);
        testRel.setRightMinCardinality(0);
        relationship = getRelationship(leftItem, rightItem, testRel, 0,0);
        leftTypelist.add(relationship);
        rightTypelist.add(relationship);

        // Mock the state of objects utilized in delete() to meet the success criteria of the invocation
        when(virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), true))
                .thenReturn(true);
        when(authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE))
                .thenReturn(true);
        when(relationshipService.findByItem(context,leftItem)).thenReturn(leftTypelist);
        when(relationshipService.findByItem(context,rightItem)).thenReturn(rightTypelist);
        when(relationshipService.findByItemAndRelationshipType(context, leftItem, testRel, true))
                .thenReturn(leftTypelist);
        when(relationshipService.findByItemAndRelationshipType(context, rightItem, testRel, false))
                .thenReturn(rightTypelist);
        when(relationshipService.find(context,0)).thenReturn(relationship);

        // Invoke delete()
        relationshipService.delete(context, relationship);

        // Verify RelationshipService.delete() ran once to confirm proper invocation of delete()
        Mockito.verify(relationshipDAO).delete(context, relationship);
    }

    @Test
    public void testUpdate() throws Exception {
        // Mock admin state
        context.turnOffAuthorisationSystem();

        // Declare objects utilized in unit test
        MetadataValue metVal = mock(MetadataValue.class);
        List<MetadataValue> metsList = new ArrayList<>();
        Item leftItem = mock(Item.class);
        Item rightItem = mock(Item.class);
        RelationshipType testRel = new RelationshipType();
        EntityType leftEntityType = mock(EntityType.class);
        EntityType rightEntityType = mock(EntityType.class);
        testRel.setLeftType(leftEntityType);
        testRel.setRightType(rightEntityType);
        testRel.setLeftwardType("Entitylabel");
        testRel.setRightwardType("Entitylabel");
        testRel.setLeftMinCardinality(0);
        testRel.setRightMinCardinality(0);
        metsList.add(metVal);
        relationship = getRelationship(leftItem, rightItem, testRel, 0,0);

        // Mock the state of objects utilized in update() to meet the success criteria of the invocation
        when(itemService.getMetadata(leftItem, "dspace", "entity", "type", Item.ANY, false)).thenReturn(metsList);
        when(itemService.getMetadata(rightItem, "dspace", "entity", "type", Item.ANY, false)).thenReturn(metsList);
        when(authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(),
                Constants.WRITE)).thenReturn(true);

        // Invoke update()
        relationshipService.update(context, relationship);

        // Verify RelationshipDAO.delete() ran once to confirm proper invocation of update()
        Mockito.verify(relationshipDAO).save(context, relationship);
    }

    @Test
    public void testCountTotal() throws Exception {
        when(relationshipDAO.countRows(context)).thenReturn(0);
        assertEquals("TestCountTotal 1", 0, relationshipService.countTotal(context));
    }

    /**
     * Helper method that returns a configured Relationship
     * @param leftItem Relationship's left item
     * @param rightItem Relationship's right item
     * @param relationshipType Relationship's RelationshipType
     * @param leftPlace Relationship's left place
     * @param rightPlace Relationship's right place
     * @return Configured Relationship
     */
    private Relationship getRelationship(Item leftItem, Item rightItem, RelationshipType relationshipType,
                                         int leftPlace, int rightPlace) {
        Relationship relationship = new Relationship();
        relationship.setId(0);
        relationship.setLeftItem(leftItem);
        relationship.setRightItem(rightItem);
        relationship.setRelationshipType(relationshipType);
        relationship.setLeftPlace(leftPlace);
        relationship.setRightPlace(rightPlace);

        return relationship;
    }


}
