/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.content.Relationship.LatestVersionStatus;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.versioning.Version;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class VersioningWithRelationshipsTest extends AbstractIntegrationTestWithDatabase {

    private final RelationshipService relationshipService =
        ContentServiceFactory.getInstance().getRelationshipService();
    private final VersioningService versioningService =
        VersionServiceFactory.getInstance().getVersionService();
    private final WorkspaceItemService workspaceItemService =
        ContentServiceFactory.getInstance().getWorkspaceItemService();
    private final InstallItemService installItemService =
        ContentServiceFactory.getInstance().getInstallItemService();
    private final ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();

    protected Community community;
    protected Collection collection;
    protected EntityType publicationEntityType;
    protected EntityType personEntityType;
    protected EntityType projectEntityType;
    protected EntityType orgUnitEntityType;
    protected RelationshipType isAuthorOfPublication;
    protected RelationshipType isProjectOfPublication;
    protected RelationshipType isOrgUnitOfPublication;
    protected RelationshipType isMemberOfProject;
    protected RelationshipType isMemberOfOrgUnit;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

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

        projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project")
            .build();

        orgUnitEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit")
            .build();

        isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isOrgUnitOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, orgUnitEntityType,
                "isOrgUnitOfPublication", "isPublicationOfOrgUnit",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isMemberOfProject = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, projectEntityType, personEntityType,
                "isMemberOfProject", "isProjectOfMember",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isMemberOfOrgUnit = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, orgUnitEntityType, personEntityType,
                "isMemberOfOrgUnit", "isOrgUnitOfMember",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();
    }

    protected Matcher<Object> isRelationship(
        Item leftItem, RelationshipType relationshipType, Item rightItem, LatestVersionStatus latestVersionStatus
    ) {
        return allOf(
            hasProperty("leftItem", is(leftItem)),
            hasProperty("relationshipType", is(relationshipType)),
            hasProperty("rightItem", is(rightItem)),
            // NOTE: place is not checked
            hasProperty("leftwardValue", nullValue()),
            hasProperty("rightwardValue", nullValue()),
            hasProperty("latestVersionStatus", is(latestVersionStatus))
        );
    }

    @Test
    public void test_createNewVersionOfItemOnLeftSideOfRelationships() throws Exception {
        ///////////////////////////////////////////////
        // create a publication with 3 relationships //
        ///////////////////////////////////////////////

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        Item project1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 1")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, person1, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, project1, isProjectOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, orgUnit1, isOrgUnitOfPublication)
            .build();

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        /////////////////////////////////////////////
        // create a new version of the publication //
        /////////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        ////////////////////////////////////////
        // do item install on new publication //
        ////////////////////////////////////////

        WorkspaceItem newPublicationWSI = workspaceItemService.findByItem(context, newPublication);
        installItemService.installItem(context, newPublicationWSI);
        context.dispatchEvents();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    @Test
    public void test_createNewVersionOfItemAndModifyRelationships() throws Exception {
        ///////////////////////////////////////////////
        // create a publication with 3 relationships //
        ///////////////////////////////////////////////

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        Item project1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 1")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, person1, isAuthorOfPublication)
            .build();

        RelationshipBuilder
            .createRelationshipBuilder(context, originalPublication, project1, isProjectOfPublication)
            .build();

        RelationshipBuilder
            .createRelationshipBuilder(context, originalPublication, orgUnit1, isOrgUnitOfPublication)
            .build();

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        /////////////////////////////////////////////
        // create a new version of the publication //
        /////////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        /////////////////////////////////////////////
        // modify relationships on new publication //
        /////////////////////////////////////////////

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("person 2")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        Item orgUnit2 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 2")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        // on new item, remove relationship with project 1
        List<Relationship> newProjectRels = relationshipService
            .findByItemAndRelationshipType(context, newPublication, isProjectOfPublication);
        assertEquals(1, newProjectRels.size());
        relationshipService.delete(context, newProjectRels.get(0));

        // on new item remove relationship with org unit 1
        List<Relationship> newOrgUnitRels = relationshipService
            .findByItemAndRelationshipType(context, newPublication, isOrgUnitOfPublication);
        assertEquals(1, newOrgUnitRels.size());
        relationshipService.delete(context, newOrgUnitRels.get(0));

        RelationshipBuilder.createRelationshipBuilder(context, newPublication, person2, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, newPublication, orgUnit2, isOrgUnitOfPublication)
            .build();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                // NOTE: BOTH because new relationship
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                // NOTE: BOTH because new relationship
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        ////////////////////////////////////////
        // do item install on new publication //
        ////////////////////////////////////////

        WorkspaceItem newPublicationWSI = workspaceItemService.findByItem(context, newPublication);
        installItemService.installItem(context, newPublicationWSI);
        context.dispatchEvents();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            empty()
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            empty()
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY),
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH),
                isRelationship(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH)
            ))
        );

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    @Test
    public void test_createNewVersionOfItemOnRightSideOfRelationships() throws Exception {
        //////////////////////////////////////////
        // create a person with 3 relationships //
        //////////////////////////////////////////

        Item publication1 = ItemBuilder.createItem(context, collection)
            .withTitle("publication 1")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        Item project1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 1")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        Item originalPerson = ItemBuilder.createItem(context, collection)
            .withTitle("original person")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1, originalPerson, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, project1, originalPerson, isMemberOfProject)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit1, originalPerson, isMemberOfOrgUnit)
            .build();

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        ////////////////////////////////////////
        // create a new version of the person //
        ////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPerson);
        Item newPerson = newVersion.getItem();
        assertNotSame(originalPerson, newPerson);

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH),
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        ///////////////////////////////////
        // do item install on new person //
        ///////////////////////////////////

        WorkspaceItem newPersonWSI = workspaceItemService.findByItem(context, newPerson);
        installItemService.installItem(context, newPersonWSI);
        context.dispatchEvents();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH),
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH),
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.LEFT_ONLY)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(project1, isMemberOfProject, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.LEFT_ONLY),
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRelationship(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH),
                isRelationship(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH),
                isRelationship(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH)
            ))
        );

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    @Test
    public void test_createNewVersionOfItemAndVerifyMetadataOrder() throws Exception {
        /////////////////////////////////////////
        // create a publication with 6 authors //
        /////////////////////////////////////////

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        // author 1 (plain metadata)
        itemService.addMetadata(context, originalPublication, "dc", "contributor", "author", null, "author 1 (plain)");

        // author 2 (virtual)
        Item author2 = ItemBuilder.createItem(context, collection)
            .withTitle("author 2 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("2 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author2, isAuthorOfPublication)
            .build();

        // author 3 (virtual)
        Item author3 = ItemBuilder.createItem(context, collection)
            .withTitle("author 3 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("3 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author3, isAuthorOfPublication)
            .build();

        // author 4 (virtual)
        Item author4 = ItemBuilder.createItem(context, collection)
            .withTitle("author 4 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("4 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author4, isAuthorOfPublication)
            .build();

        // author 5 (virtual)
        Item author5 = ItemBuilder.createItem(context, collection)
            .withTitle("author 5 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("5 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author5, isAuthorOfPublication)
            .build();

        // author 6 (plain metadata)
        itemService.addMetadata(context, originalPublication, "dc", "contributor", "author", null, "author 6 (plain)");

        // author 7 (virtual)
        Item author7 = ItemBuilder.createItem(context, collection)
            .withTitle("author 7 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("7 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author7, isAuthorOfPublication)
            .build();

        // author 8 (plain metadata)
        itemService.addMetadata(context, originalPublication, "dc", "contributor", "author", null, "author 8 (plain)");

        // author 9 (virtual)
        Item author9 = ItemBuilder.createItem(context, collection)
            .withTitle("author 9 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("9 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author9, isAuthorOfPublication)
            .build();

        ////////////////////////////////
        // test dc.contributor.author //
        ////////////////////////////////

        List<MetadataValue> oldMdvs = itemService.getMetadata(
            originalPublication, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(9, oldMdvs.size());

        assertFalse(oldMdvs.get(0) instanceof RelationshipMetadataValue);
        assertEquals("author 1 (plain)", oldMdvs.get(0).getValue());
        assertEquals(0, oldMdvs.get(0).getPlace());

        assertTrue(oldMdvs.get(1) instanceof RelationshipMetadataValue);
        assertEquals("author, 2 (item)", oldMdvs.get(1).getValue());
        assertEquals(1, oldMdvs.get(1).getPlace());

        assertTrue(oldMdvs.get(2) instanceof RelationshipMetadataValue);
        assertEquals("author, 3 (item)", oldMdvs.get(2).getValue());
        assertEquals(2, oldMdvs.get(2).getPlace());

        assertTrue(oldMdvs.get(3) instanceof RelationshipMetadataValue);
        assertEquals("author, 4 (item)", oldMdvs.get(3).getValue());
        assertEquals(3, oldMdvs.get(3).getPlace());

        assertTrue(oldMdvs.get(4) instanceof RelationshipMetadataValue);
        assertEquals("author, 5 (item)", oldMdvs.get(4).getValue());
        assertEquals(4, oldMdvs.get(4).getPlace());

        assertFalse(oldMdvs.get(5) instanceof RelationshipMetadataValue);
        assertEquals("author 6 (plain)", oldMdvs.get(5).getValue());
        assertEquals(5, oldMdvs.get(5).getPlace());

        assertTrue(oldMdvs.get(6) instanceof RelationshipMetadataValue);
        assertEquals("author, 7 (item)", oldMdvs.get(6).getValue());
        assertEquals(6, oldMdvs.get(6).getPlace());

        assertFalse(oldMdvs.get(7) instanceof RelationshipMetadataValue);
        assertEquals("author 8 (plain)", oldMdvs.get(7).getValue());
        assertEquals(7, oldMdvs.get(7).getPlace());

        assertTrue(oldMdvs.get(8) instanceof RelationshipMetadataValue);
        assertEquals("author, 9 (item)", oldMdvs.get(8).getValue());
        assertEquals(8, oldMdvs.get(8).getPlace());

        /////////////////////////////////////////////
        // test relationship isAuthorOfPublication //
        /////////////////////////////////////////////

        List<Relationship> oldRelationships = relationshipService.findByItem(context, originalPublication);
        assertEquals(6, oldRelationships.size());

        assertEquals(originalPublication, oldRelationships.get(0).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(0).getRelationshipType());
        assertEquals(author2, oldRelationships.get(0).getRightItem());
        assertEquals(1, oldRelationships.get(0).getLeftPlace());
        assertEquals(0, oldRelationships.get(0).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(1).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(1).getRelationshipType());
        assertEquals(author3, oldRelationships.get(1).getRightItem());
        assertEquals(2, oldRelationships.get(1).getLeftPlace());
        assertEquals(0, oldRelationships.get(1).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(2).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(2).getRelationshipType());
        assertEquals(author4, oldRelationships.get(2).getRightItem());
        assertEquals(3, oldRelationships.get(2).getLeftPlace());
        assertEquals(0, oldRelationships.get(2).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(3).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(3).getRelationshipType());
        assertEquals(author5, oldRelationships.get(3).getRightItem());
        assertEquals(4, oldRelationships.get(3).getLeftPlace());
        assertEquals(0, oldRelationships.get(3).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(4).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(4).getRelationshipType());
        assertEquals(author7, oldRelationships.get(4).getRightItem());
        assertEquals(6, oldRelationships.get(4).getLeftPlace());
        assertEquals(0, oldRelationships.get(4).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(5).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(5).getRelationshipType());
        assertEquals(author9, oldRelationships.get(5).getRightItem());
        assertEquals(8, oldRelationships.get(5).getLeftPlace());
        assertEquals(0, oldRelationships.get(5).getRightPlace());

        ///////////////////////////////////////
        // create new version of publication //
        ///////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        ////////////////////////////////
        // test dc.contributor.author //
        ////////////////////////////////

        List<MetadataValue> newMdvs = itemService.getMetadata(
            newPublication, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(9, newMdvs.size());

        assertFalse(newMdvs.get(0) instanceof RelationshipMetadataValue);
        assertEquals("author 1 (plain)", newMdvs.get(0).getValue());
        assertEquals(0, newMdvs.get(0).getPlace());

        assertTrue(newMdvs.get(1) instanceof RelationshipMetadataValue);
        assertEquals("author, 2 (item)", newMdvs.get(1).getValue());
        assertEquals(1, newMdvs.get(1).getPlace());

        assertTrue(newMdvs.get(2) instanceof RelationshipMetadataValue);
        assertEquals("author, 3 (item)", newMdvs.get(2).getValue());
        assertEquals(2, newMdvs.get(2).getPlace());

        assertTrue(newMdvs.get(3) instanceof RelationshipMetadataValue);
        assertEquals("author, 4 (item)", newMdvs.get(3).getValue());
        assertEquals(3, newMdvs.get(3).getPlace());

        assertTrue(newMdvs.get(4) instanceof RelationshipMetadataValue);
        assertEquals("author, 5 (item)", newMdvs.get(4).getValue());
        assertEquals(4, newMdvs.get(4).getPlace());

        assertFalse(newMdvs.get(5) instanceof RelationshipMetadataValue);
        assertEquals("author 6 (plain)", newMdvs.get(5).getValue());
        assertEquals(5, newMdvs.get(5).getPlace());

        assertTrue(newMdvs.get(6) instanceof RelationshipMetadataValue);
        assertEquals("author, 7 (item)", newMdvs.get(6).getValue());
        assertEquals(6, newMdvs.get(6).getPlace());

        assertFalse(newMdvs.get(7) instanceof RelationshipMetadataValue);
        assertEquals("author 8 (plain)", newMdvs.get(7).getValue());
        assertEquals(7, newMdvs.get(7).getPlace());

        assertTrue(newMdvs.get(8) instanceof RelationshipMetadataValue);
        assertEquals("author, 9 (item)", newMdvs.get(8).getValue());
        assertEquals(8, newMdvs.get(8).getPlace());

        /////////////////////////////////////////////
        // test relationship isAuthorOfPublication //
        /////////////////////////////////////////////

        List<Relationship> newRelationships = relationshipService.findByItem(context, newPublication);
        assertEquals(6, newRelationships.size());

        assertEquals(newPublication, newRelationships.get(0).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(0).getRelationshipType());
        assertEquals(author2, newRelationships.get(0).getRightItem());
        assertEquals(1, newRelationships.get(0).getLeftPlace());
        assertEquals(0, newRelationships.get(0).getRightPlace());

        assertEquals(newPublication, newRelationships.get(1).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(1).getRelationshipType());
        assertEquals(author3, newRelationships.get(1).getRightItem());
        assertEquals(2, newRelationships.get(1).getLeftPlace());
        assertEquals(0, newRelationships.get(1).getRightPlace());

        assertEquals(newPublication, newRelationships.get(2).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(2).getRelationshipType());
        assertEquals(author4, newRelationships.get(2).getRightItem());
        assertEquals(3, newRelationships.get(2).getLeftPlace());
        assertEquals(0, newRelationships.get(2).getRightPlace());

        assertEquals(newPublication, newRelationships.get(3).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(3).getRelationshipType());
        assertEquals(author5, newRelationships.get(3).getRightItem());
        assertEquals(4, newRelationships.get(3).getLeftPlace());
        assertEquals(0, newRelationships.get(3).getRightPlace());

        assertEquals(newPublication, newRelationships.get(4).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(4).getRelationshipType());
        assertEquals(author7, newRelationships.get(4).getRightItem());
        assertEquals(6, newRelationships.get(4).getLeftPlace());
        assertEquals(0, newRelationships.get(4).getRightPlace());

        assertEquals(newPublication, newRelationships.get(5).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(5).getRelationshipType());
        assertEquals(author9, newRelationships.get(5).getRightItem());
        assertEquals(8, newRelationships.get(5).getLeftPlace());
        assertEquals(0, newRelationships.get(5).getRightPlace());

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

}
