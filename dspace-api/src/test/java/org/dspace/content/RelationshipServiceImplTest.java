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

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.service.ItemService;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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

    @Before
    public void init() {
        relationshipsList = new ArrayList<>();
        relationshipsList.add(relationship);
    }

    @Test
    public void testFindAll() throws Exception {
        when(relationshipDAO.findAll(context, Relationship.class)).thenReturn(relationshipsList);
        assertEquals("TestFindAll 0", relationshipsList, relationshipService.findAll(context));
    }

    @Test
    public void testFindByItem() throws Exception {
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
        hasDog.setLeftLabel("hasDog");
        hasDog.setRightLabel("isDogOf");
        hasFather.setLeftLabel("hasFather");
        hasFather.setRightLabel("isFatherOf");
        hasMother.setLeftLabel("hasMother");
        hasMother.setRightLabel("isMotherOf");

        relationshipTest.add(getRelationship(cindy, spot, hasDog,0,0));
        relationshipTest.add(getRelationship(cindy, jasper, hasDog,0,1));
        relationshipTest.add(getRelationship(cindy, hank, hasFather,0,0));
        relationshipTest.add(getRelationship(fred, cindy, hasMother,0,0));
        relationshipTest.add(getRelationship(bob, cindy, hasMother,1,0));
        when(relationshipService.findByItem(context, cindy)).thenReturn(relationshipTest);
        when(relationshipDAO.findByItem(context, cindy)).thenReturn(relationshipTest);


        List<Relationship> results = relationshipService.findByItem(context, cindy);
        assertEquals("TestFindByItem 0", relationshipTest, results);
    }

    @Test
    public void testFindLeftPlaceByLeftItem() throws Exception {
        Item item = mock(Item.class);
        when(relationshipDAO.findLeftPlaceByLeftItem(context, item)).thenReturn(0);
        assertEquals("TestFindLeftPlaceByLeftItem 0", relationshipDAO.findLeftPlaceByLeftItem(context, item),
                relationshipService.findLeftPlaceByLeftItem(context, item));
    }

    @Test
    public void testFindRightPlaceByRightItem() throws Exception {
        Item item = mock(Item.class);
        when(relationshipDAO.findRightPlaceByRightItem(context, item)).thenReturn(0);
        assertEquals("TestFindRightPlaceByRightItem 0", relationshipDAO.findRightPlaceByRightItem(context, item),
                relationshipService.findRightPlaceByRightItem(context, item));
    }

    @Test
    public void testFindByItemAndRelationshipType() throws Exception {
        List<Relationship> relList = new LinkedList<>();
        Item item = mock(Item.class);
        RelationshipType testRel = new RelationshipType();

        assertEquals("TestFindByItemAndRelationshipType 0", relList,
                relationshipService.findByItemAndRelationshipType(context, item, testRel, true));
        assertEquals("TestFindByItemAndRelationshipType 1", relList,
                relationshipService.findByItemAndRelationshipType(context, item, testRel));
    }

    @Test
    public void testFindByRelationshipType() throws Exception {
        List<Relationship> relList = new LinkedList<>();
        RelationshipType testRel = new RelationshipType();

        assertEquals("TestFindByRelationshipType 0", relList,
                relationshipService.findByRelationshipType(context, testRel));
        assertEquals("TestFindByRelationshipType 1", relList,
                relationshipService.findByRelationshipType(context, testRel));
    }

    @Test
    public void find() throws Exception {
        Relationship relationship = new Relationship();
        relationship.setId(1337);
        when(relationshipDAO.findByID(context, Relationship.class, relationship.getID())).thenReturn(relationship);
        assertEquals("TestFind 0", relationship, relationshipService.find(context, relationship.getID()));
    }

    @Test
    public void testCreate() throws Exception {
        Relationship relationship = relationshipDAO.create(context,new Relationship());
        context.turnOffAuthorisationSystem();
        when(authorizeService.isAdmin(context)).thenReturn(true);
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
        testRel.setLeftLabel("Entitylabel");
        testRel.setRightLabel("Entitylabel");
        metsList.add(metVal);
        relationship = getRelationship(leftItem, rightItem, testRel, 0,0);
        leftTypelist.add(relationship);
        rightTypelist.add(relationship);
        when(virtualMetadataPopulator
                .isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), true)).thenReturn(true);
        when(authorizeService
                .authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE)).thenReturn(true);
        when(authorizeService
                .authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)).thenReturn(true);
        when(relationshipService.findByItem(context,leftItem)).thenReturn(leftTypelist);
        when(relationshipService.findByItem(context,rightItem)).thenReturn(rightTypelist);
        when(leftEntityType.getLabel()).thenReturn("Entitylabel");
        when(rightEntityType.getLabel()).thenReturn("Entitylabel");
        when(metVal.getValue()).thenReturn("Entitylabel");
        when(metsList.get(0).getValue()).thenReturn("Entitylabel");
        when(relationshipService
                .findByItemAndRelationshipType(context, leftItem, testRel, true)).thenReturn(leftTypelist);
        when(itemService.getMetadata(leftItem, "relationship", "type", null, Item.ANY)).thenReturn(metsList);
        when(itemService.getMetadata(rightItem, "relationship", "type", null, Item.ANY)).thenReturn(metsList);
        when(relationshipDAO.create(any(), any())).thenReturn(relationship);

        assertEquals("TestCreate 1", relationship, relationshipService.create(context, relationship));
        assertEquals("TestCreate 2", relationship, relationshipService.create(context, leftItem, rightItem,
                testRel,0,0));

        context.restoreAuthSystemState();
    }

    @Test
    public void testDelete() throws Exception {
        context.turnOffAuthorisationSystem();
        when(authorizeService.isAdmin(context)).thenReturn(true);
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
        testRel.setLeftLabel("Entitylabel");
        testRel.setRightLabel("Entitylabel");
        testRel.setLeftMinCardinality(0);
        testRel.setRightMinCardinality(0);
        metsList.add(metVal);
        relationship = getRelationship(leftItem, rightItem, testRel, 0,0);
        leftTypelist.add(relationship);
        rightTypelist.add(relationship);
        when(virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), true))
                .thenReturn(true);
        when(authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE))
                .thenReturn(true);
        when(authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE))
                .thenReturn(true);
        when(relationshipService.findByItem(context,leftItem)).thenReturn(leftTypelist);
        when(relationshipService.findByItem(context,rightItem)).thenReturn(rightTypelist);
        when(leftEntityType.getLabel()).thenReturn("Entitylabel");
        when(rightEntityType.getLabel()).thenReturn("Entitylabel");
        when(metVal.getValue()).thenReturn("Entitylabel");
        when(metsList.get(0).getValue()).thenReturn("Entitylabel");
        when(relationshipService.findByItemAndRelationshipType(context, leftItem, testRel, true))
                .thenReturn(leftTypelist);
        when(itemService.getMetadata(leftItem, "relationship", "type", null, Item.ANY)).thenReturn(metsList);
        when(itemService.getMetadata(rightItem, "relationship", "type", null, Item.ANY)).thenReturn(metsList);
        when(relationshipDAO.create(any(), any())).thenReturn(relationship);
        when(relationshipService.find(context,0)).thenReturn(relationship);
        relationshipService.delete(context, relationship);
        Mockito.verify(relationshipDAO).delete(context, relationship);
    }

    @Test
    public void testUpdate() throws Exception {
        context.turnOffAuthorisationSystem();
        when(authorizeService.isAdmin(context)).thenReturn(true);
        MetadataValue metVal = mock(MetadataValue.class);
        List<MetadataValue> metsList = new ArrayList<>();
        Item leftItem = mock(Item.class);
        Item rightItem = mock(Item.class);
        RelationshipType testRel = new RelationshipType();
        EntityType leftEntityType = mock(EntityType.class);
        EntityType rightEntityType = mock(EntityType.class);
        testRel.setLeftType(leftEntityType);
        testRel.setRightType(rightEntityType);
        testRel.setLeftLabel("Entitylabel");
        testRel.setRightLabel("Entitylabel");
        testRel.setLeftMinCardinality(0);
        testRel.setRightMinCardinality(0);
        metsList.add(metVal);
        relationship = getRelationship(leftItem, rightItem, testRel, 0,0);
        when(itemService.getMetadata(leftItem, "relationship", "type", null, Item.ANY)).thenReturn(metsList);
        when(itemService.getMetadata(rightItem, "relationship", "type", null, Item.ANY)).thenReturn(metsList);
        when(authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(),
                Constants.WRITE)).thenReturn(true);
        when(authorizeService.authorizeActionBoolean(context, relationship.getRightItem(),
                Constants.WRITE)).thenReturn(true);
        relationshipService.update(context, relationship);
        Mockito.verify(relationshipDAO).save(context, relationship);
    }

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
