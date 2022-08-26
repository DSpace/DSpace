/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.RelationshipService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class RelationshipServiceImplVersioningTest extends AbstractIntegrationTestWithDatabase {

    private RelationshipService relationshipService;
    private RelationshipDAO relationshipDAO;

    protected Community community;
    protected Collection collection;
    protected EntityType publicationEntityType;
    protected EntityType personEntityType;
    protected RelationshipType relationshipType;
    protected Item publication1;
    protected Item publication2;
    protected Item publication3;
    protected Item person1;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        relationshipDAO = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServicesByType(RelationshipDAO.class).get(0);

        context.turnOffAuthorisationSystem();

        community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
            .build();

        personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person")
            .build();

        relationshipType = RelationshipTypeBuilder.createRelationshipTypeBuilder(
            context, publicationEntityType, personEntityType,
            "isAuthorOfPublication", "isPublicationOfAuthor",
            null, null, null, null
        )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        publication1 = ItemBuilder.createItem(context, collection)
            .withTitle("publication1")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        publication2 = ItemBuilder.createItem(context, collection)
            .withTitle("publication2")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        publication3 = ItemBuilder.createItem(context, collection)
            .withTitle("publication3")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        person1 = ItemBuilder.createItem(context, collection)
            .withTitle("person1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testRelationshipLatestVersionStatusDefault() throws Exception {
        // create method #1
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = relationshipService.create(
            context, publication1, person1, relationshipType, 3, 5, "left", "right"
        );
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship1.getLatestVersionStatus());
        Relationship relationship2 = relationshipService.find(context, relationship1.getID());
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship2.getLatestVersionStatus());

        // create method #2
        context.turnOffAuthorisationSystem();
        Relationship relationship3 = relationshipService.create(
            context, publication2, person1, relationshipType, 3, 5
        );
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship3.getLatestVersionStatus());
        Relationship relationship4 = relationshipService.find(context, relationship3.getID());
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship4.getLatestVersionStatus());

        // create method #3
        Relationship inputRelationship = new Relationship();
        inputRelationship.setLeftItem(publication3);
        inputRelationship.setRightItem(person1);
        inputRelationship.setRelationshipType(relationshipType);
        context.turnOffAuthorisationSystem();
        Relationship relationship5 = relationshipService.create(context, inputRelationship);
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship5.getLatestVersionStatus());
        Relationship relationship6 = relationshipService.find(context, relationship5.getID());
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship6.getLatestVersionStatus());

        // clean up
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship1);
        relationshipService.delete(context, relationship3);
        relationshipService.delete(context, relationship5);
        context.restoreAuthSystemState();
    }

    @Test
    public void testRelationshipLatestVersionStatusBoth() throws Exception {
        // create method #1
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = relationshipService.create(
            context, publication1, person1, relationshipType, 3, 5, "left", "right",
            Relationship.LatestVersionStatus.BOTH // set latest version status
        );
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship1.getLatestVersionStatus());
        Relationship relationship2 = relationshipService.find(context, relationship1.getID());
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship2.getLatestVersionStatus());

        // create method #2
        Relationship inputRelationship = new Relationship();
        inputRelationship.setLeftItem(publication2);
        inputRelationship.setRightItem(person1);
        inputRelationship.setRelationshipType(relationshipType);
        inputRelationship.setLatestVersionStatus(Relationship.LatestVersionStatus.BOTH); // set latest version status
        context.turnOffAuthorisationSystem();
        Relationship relationship3 = relationshipService.create(context, inputRelationship);
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship3.getLatestVersionStatus());
        Relationship relationship4 = relationshipService.find(context, relationship3.getID());
        assertEquals(Relationship.LatestVersionStatus.BOTH, relationship4.getLatestVersionStatus());

        // clean up
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship1);
        relationshipService.delete(context, relationship3);
        context.restoreAuthSystemState();
    }

    @Test
    public void testRelationshipLatestVersionStatusLeftOnly() throws Exception {
        // create method #1
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = relationshipService.create(
            context, publication1, person1, relationshipType, 3, 5, "left", "right",
            Relationship.LatestVersionStatus.LEFT_ONLY // set latest version status
        );
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.LEFT_ONLY, relationship1.getLatestVersionStatus());
        Relationship relationship2 = relationshipService.find(context, relationship1.getID());
        assertEquals(Relationship.LatestVersionStatus.LEFT_ONLY, relationship2.getLatestVersionStatus());

        // create method #2
        Relationship inputRelationship = new Relationship();
        inputRelationship.setLeftItem(publication2);
        inputRelationship.setRightItem(person1);
        inputRelationship.setRelationshipType(relationshipType);
        inputRelationship.setLatestVersionStatus(Relationship.LatestVersionStatus.LEFT_ONLY); // set LVS
        context.turnOffAuthorisationSystem();
        Relationship relationship3 = relationshipService.create(context, inputRelationship);
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.LEFT_ONLY, relationship3.getLatestVersionStatus());
        Relationship relationship4 = relationshipService.find(context, relationship3.getID());
        assertEquals(Relationship.LatestVersionStatus.LEFT_ONLY, relationship4.getLatestVersionStatus());

        // clean up
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship1);
        relationshipService.delete(context, relationship3);
        context.restoreAuthSystemState();
    }

    @Test
    public void testRelationshipLatestVersionStatusRightOnly() throws Exception {
        // create method #1
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = relationshipService.create(
            context, publication1, person1, relationshipType, 3, 5, "left", "right",
            Relationship.LatestVersionStatus.RIGHT_ONLY // set latest version status
        );
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.RIGHT_ONLY, relationship1.getLatestVersionStatus());
        Relationship relationship2 = relationshipService.find(context, relationship1.getID());
        assertEquals(Relationship.LatestVersionStatus.RIGHT_ONLY, relationship2.getLatestVersionStatus());

        // create method #2
        Relationship inputRelationship = new Relationship();
        inputRelationship.setLeftItem(publication2);
        inputRelationship.setRightItem(person1);
        inputRelationship.setRelationshipType(relationshipType);
        inputRelationship.setLatestVersionStatus(Relationship.LatestVersionStatus.RIGHT_ONLY); // set LVS
        context.turnOffAuthorisationSystem();
        Relationship relationship3 = relationshipService.create(context, inputRelationship);
        context.restoreAuthSystemState();
        assertEquals(Relationship.LatestVersionStatus.RIGHT_ONLY, relationship3.getLatestVersionStatus());
        Relationship relationship4 = relationshipService.find(context, relationship3.getID());
        assertEquals(Relationship.LatestVersionStatus.RIGHT_ONLY, relationship4.getLatestVersionStatus());

        // clean up
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship1);
        relationshipService.delete(context, relationship3);
        context.restoreAuthSystemState();
    }

    protected void assertRelationship(Relationship expectedRelationship, List<Relationship> relationships) {
        assertNotNull(relationships);
        assertEquals(1, relationships.size());
        assertEquals(expectedRelationship, relationships.get(0));
    }

    protected void assertNoRelationship(List<Relationship> relationships) {
        assertNotNull(relationships);
        assertEquals(0, relationships.size());
    }

    @Test
    public void testExcludeNonLatestBoth() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, person1, relationshipType)
            .withLatestVersionStatus(Relationship.LatestVersionStatus.BOTH)
            .build();
        context.restoreAuthSystemState();

        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, false, true)
        );

        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, -1, -1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, -1, -1, false, true)
        );

        assertEquals(1, relationshipDAO.countByItem(context, publication1, false, false));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, false, true));
        assertEquals(1, relationshipDAO.countByItem(context, person1, false, false));
        assertEquals(1, relationshipDAO.countByItem(context, person1, false, true));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, true, false));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, true, true));
        assertEquals(1, relationshipDAO.countByItem(context, person1, true, false));
        assertEquals(1, relationshipDAO.countByItem(context, person1, true, true));

        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, true)
        );

        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, true)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, true)
        );

        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, false, true)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, true, false)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, true, true)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, false, false)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, false, true)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, true, true)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false, true)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, true)
        );

        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1)
        );

        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, true)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, true)
        );

        assertEquals(1, relationshipService.countByItem(context, publication1));
        assertEquals(1, relationshipService.countByItem(context, person1));

        assertEquals(1, relationshipService.countByItem(context, publication1, false, false));
        assertEquals(1, relationshipService.countByItem(context, publication1, false, true));
        assertEquals(1, relationshipService.countByItem(context, person1, false, false));
        assertEquals(1, relationshipService.countByItem(context, person1, false, true));
        assertEquals(1, relationshipService.countByItem(context, publication1, true, false));
        assertEquals(1, relationshipService.countByItem(context, publication1, true, true));
        assertEquals(1, relationshipService.countByItem(context, person1, true, false));
        assertEquals(1, relationshipService.countByItem(context, person1, true, true));

        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true)
        );

        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true, false)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false, false)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false, true)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true, true)
        );
    }

    @Test
    public void testExcludeNonLatestLeftOnly() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, person1, relationshipType)
            .withLatestVersionStatus(Relationship.LatestVersionStatus.LEFT_ONLY)
            .build();
        context.restoreAuthSystemState();

        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, false, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItem(context, publication1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, false, true)
        );

        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, -1, -1, false, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItem(context, publication1, -1, -1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, -1, -1, false, true)
        );

        assertEquals(1, relationshipDAO.countByItem(context, publication1, false, false));
        assertEquals(0, relationshipDAO.countByItem(context, publication1, false, true));
        assertEquals(1, relationshipDAO.countByItem(context, person1, false, false));
        assertEquals(1, relationshipDAO.countByItem(context, person1, false, true));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, true, false));
        assertEquals(0, relationshipDAO.countByItem(context, publication1, true, true));
        assertEquals(1, relationshipDAO.countByItem(context, person1, true, false));
        assertEquals(1, relationshipDAO.countByItem(context, person1, true, true));

        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, true)
        );

        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, true)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, true)
        );

        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, false, true)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, true, true)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, false, false)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, false, true)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, true, true)
        );

        assertNoRelationship(
            relationshipService.findByItem(context, publication1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1)
        );

        assertNoRelationship(
            relationshipService.findByItem(context, publication1, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false, false)
        );
        assertNoRelationship(
            relationshipService.findByItem(context, publication1, -1, -1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false, true)
        );

        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType)
        );

        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, true)
        );

        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1)
        );

        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, true)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, true)
        );

        assertEquals(0, relationshipService.countByItem(context, publication1));
        assertEquals(1, relationshipService.countByItem(context, person1));

        assertEquals(1, relationshipService.countByItem(context, publication1, false, false));
        assertEquals(0, relationshipService.countByItem(context, publication1, false, true));
        assertEquals(1, relationshipService.countByItem(context, person1, false, false));
        assertEquals(1, relationshipService.countByItem(context, person1, false, true));
        assertEquals(1, relationshipService.countByItem(context, publication1, true, false));
        assertEquals(0, relationshipService.countByItem(context, publication1, true, true));
        assertEquals(1, relationshipService.countByItem(context, person1, true, false));
        assertEquals(1, relationshipService.countByItem(context, person1, true, true));

        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true)
        );

        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false, false)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false, true)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true, true)
        );
    }

    @Test
    public void testExcludeNonLatestRightOnly() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, person1, relationshipType)
            .withLatestVersionStatus(Relationship.LatestVersionStatus.RIGHT_ONLY)
            .build();
        context.restoreAuthSystemState();

        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, false, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItem(context, person1, false, true)
        );

        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, publication1, -1, -1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItem(context, person1, -1, -1, false, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItem(context, person1, -1, -1, false, true)
        );

        assertEquals(1, relationshipDAO.countByItem(context, publication1, false, false));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, false, true));
        assertEquals(1, relationshipDAO.countByItem(context, person1, false, false));
        assertEquals(0, relationshipDAO.countByItem(context, person1, false, true));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, true, false));
        assertEquals(1, relationshipDAO.countByItem(context, publication1, true, true));
        assertEquals(1, relationshipDAO.countByItem(context, person1, true, false));
        assertEquals(0, relationshipDAO.countByItem(context, person1, true, true));

        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, true)
        );

        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, true)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipDAO.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, true)
        );

        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, false, true)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, true, false)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, publication1, relationshipType, true, true)
        );
        assertEquals(
            1, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, false, true)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipDAO.countByItemAndRelationshipType(context, person1, relationshipType, true, true)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1)
        );
        assertNoRelationship(
            relationshipService.findByItem(context, person1)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItem(context, person1, -1, -1, false)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, publication1, -1, -1, false, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItem(context, person1, -1, -1, false, false)
        );
        assertNoRelationship(
            relationshipService.findByItem(context, person1, -1, -1, false, true)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1)
        );

        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, -1, -1, true)
        );

        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1)
        );

        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, false, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, false)
        );
        assertRelationship(
            relationship1,
            relationshipService
                .findByItemAndRelationshipType(context, publication1, relationshipType, true, -1, -1, true)
        );
        assertRelationship(
            relationship1,
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, false, -1, -1, true)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, false)
        );
        assertNoRelationship(
            relationshipService.findByItemAndRelationshipType(context, person1, relationshipType, true, -1, -1, true)
        );

        assertEquals(1, relationshipService.countByItem(context, publication1));
        assertEquals(0, relationshipService.countByItem(context, person1));

        assertEquals(1, relationshipService.countByItem(context, publication1, false, false));
        assertEquals(1, relationshipService.countByItem(context, publication1, false, true));
        assertEquals(1, relationshipService.countByItem(context, person1, false, false));
        assertEquals(0, relationshipService.countByItem(context, person1, false, true));
        assertEquals(1, relationshipService.countByItem(context, publication1, true, false));
        assertEquals(1, relationshipService.countByItem(context, publication1, true, true));
        assertEquals(1, relationshipService.countByItem(context, person1, true, false));
        assertEquals(0, relationshipService.countByItem(context, person1, true, true));

        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true)
        );

        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, false, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true, false)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, publication1, relationshipType, true, true)
        );
        assertEquals(
            1, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, false, true)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true, false)
        );
        assertEquals(
            0, relationshipService.countByItemAndRelationshipType(context, person1, relationshipType, true, true)
        );
    }

}
