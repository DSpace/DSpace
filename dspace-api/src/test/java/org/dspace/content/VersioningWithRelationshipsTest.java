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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

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
    protected EntityType journalIssueEntityType;
    protected EntityType journalVolumeEntityType;
    protected RelationshipType isAuthorOfPublication;
    protected RelationshipType isProjectOfPublication;
    protected RelationshipType isOrgUnitOfPublication;
    protected RelationshipType isMemberOfProject;
    protected RelationshipType isMemberOfOrgUnit;
    protected RelationshipType isIssueOfJournalVolume;

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

        journalIssueEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue")
            .build();

        journalVolumeEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume")
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

        isIssueOfJournalVolume = RelationshipTypeBuilder.createRelationshipTypeBuilder(
            context, journalVolumeEntityType, journalIssueEntityType,
            "isIssueOfJournalVolume", "isJournalVolumeOfIssue",
            null, null, null, null
        )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();
    }

    protected Matcher<Object> isRel(
        Item leftItem, RelationshipType relationshipType, Item rightItem, LatestVersionStatus latestVersionStatus,
        int leftPlace, int rightPlace
    ) {
        return allOf(
            hasProperty("leftItem", is(leftItem)),
            hasProperty("relationshipType", is(relationshipType)),
            hasProperty("rightItem", is(rightItem)),
            hasProperty("leftPlace", is(leftPlace)),
            hasProperty("rightPlace", is(rightPlace)),
            hasProperty("leftwardValue", nullValue()),
            hasProperty("rightwardValue", nullValue()),
            hasProperty("latestVersionStatus", is(latestVersionStatus))
        );
    }

    protected Relationship getRelationship(
        Item leftItem, RelationshipType relationshipType, Item rightItem
    ) throws Exception {
        List<Relationship> rels = relationshipService.findByRelationshipType(context, relationshipType).stream()
            .filter(rel -> leftItem.getID().equals(rel.getLeftItem().getID()))
            .filter(rel -> rightItem.getID().equals(rel.getRightItem().getID()))
            .collect(Collectors.toList());

        if (rels.size() == 0) {
            return null;
        }

        if (rels.size() == 1) {
            return rels.get(0);
        }

        // NOTE: this shouldn't be possible because of database constraints
        throw new IllegalStateException();
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
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
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
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
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
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
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
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
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
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
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
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0)
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
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, LatestVersionStatus.RIGHT_ONLY, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, LatestVersionStatus.RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, LatestVersionStatus.BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person2, LatestVersionStatus.BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, LatestVersionStatus.BOTH, 0, 0)
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
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH, 0, 0)
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
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
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
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LatestVersionStatus.LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LatestVersionStatus.BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LatestVersionStatus.BOTH, 0, 0)
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

    @Test
    public void test_placeRecalculationAfterDelete() throws Exception {
        // NOTE: this test uses relationship isIssueOfJournalVolume, because it adds virtual metadata
        //       on both sides of the relationship
        // TODO: make sure useForPlace = true, otherwise test fails

        //////////////////
        // create items //
        //////////////////

        // journal volume 1.1
        Item v1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal volume 1")
            .withMetadata("dspace", "entity", "type", journalVolumeEntityType.getLabel())
            .withMetadata("publicationvolume", "volumeNumber", null, "volume nr 1 (rel)")
            .build();

        // journal volume 3.1
        Item v3_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal volume 3")
            .withMetadata("dspace", "entity", "type", journalVolumeEntityType.getLabel())
            .withMetadata("publicationvolume", "volumeNumber", null, "volume nr 3 (rel)")
            .build();

        // journal volume 5.1
        Item v5_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal volume 5")
            .withMetadata("dspace", "entity", "type", journalVolumeEntityType.getLabel())
            .withMetadata("publicationvolume", "volumeNumber", null, "volume nr 5 (rel)")
            .build();

        // journal issue 1.1
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 1")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 1 (rel)")
            .build();

        // journal issue 3.1
        Item i3_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 3")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 3 (rel)")
            .build();

        // journal issue 5.1
        Item i5_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 5")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 5 (rel)")
            .build();

        //////////////////////////////////////////////
        // create relationships and metadata values //
        //////////////////////////////////////////////

        // relationship - volume 3 & issue 1
        RelationshipBuilder.createRelationshipBuilder(context, v3_1, i1_1, isIssueOfJournalVolume)
            .build();

        // metadata - volume 3 & issue 2
        itemService.addMetadata(context, v3_1, "publicationissue", "issueNumber", null, null, "issue nr 2 (plain)");

        // relationship - volume 1 & issue 3
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i3_1, isIssueOfJournalVolume)
            .build();

        // metadata - volume 2 & issue 3
        itemService.addMetadata(context, i3_1, "publicationvolume", "volumeNumber", null, null, "volume nr 2 (plain)");

        // relationship - volume 3 & issue 3
        RelationshipBuilder.createRelationshipBuilder(context, v3_1, i3_1, isIssueOfJournalVolume)
            .build();

        // metadata - volume 4 & issue 3
        itemService.addMetadata(context, i3_1, "publicationvolume", "volumeNumber", null, null, "volume nr 4 (plain)");

        // relationship - volume 5 & issue 3
        RelationshipBuilder.createRelationshipBuilder(context, v5_1, i3_1, isIssueOfJournalVolume)
            .build();

        // metadata - volume 6 & issue 3
        itemService.addMetadata(context, i3_1, "publicationvolume", "volumeNumber", null, null, "volume nr 6 (plain)");

        // metadata - volume 7 & issue 5
        itemService.addMetadata(context, i5_1, "publicationvolume", "volumeNumber", null, null, "volume nr 7 (plain)");

        // relationship - volume 5 & issue 5
        RelationshipBuilder.createRelationshipBuilder(context, v5_1, i5_1, isIssueOfJournalVolume)
            .build();

        // metadata - volume 3 & issue 4
        itemService.addMetadata(context, v3_1, "publicationissue", "issueNumber", null, null, "issue nr 4 (plain)");

        // relationship - volume 3 & issue 5
        RelationshipBuilder.createRelationshipBuilder(context, v3_1, i5_1, isIssueOfJournalVolume)
            .build();

        // metadata - volume 3 & issue 6
        itemService.addMetadata(context, v3_1, "publicationissue", "issueNumber", null, null, "issue nr 6 (plain)");

        // SUMMARY
        //
        // volume 3
        // - pos 0: issue 1 (rel)
        // - pos 1: issue 2 (plain)
        // - pos 2: issue 3 (rel)     [A]
        // - pos 3: issue 4 (plain)
        // - pos 4: issue 5 (rel)     [B]
        // - pos 5: issue 6 (plain)
        //
        // issue 3
        // - pos 0: volume 1 (rel)
        // - pos 1: volume 2 (plain)
        // - pos 2: volume 3 (rel)    [A]
        // - pos 3: volume 4 (plain)
        // - pos 4: volume 5 (rel)
        // - pos 5: volume 6 (plain)
        //
        // issue 5
        // - pos 0: volume 7 (plain)
        // - pos 1: volume 5 (rel)
        // - pos 2: volume 3 (rel)    [B]

        /////////////////////////////////
        // initial - verify volume 3.1 //
        /////////////////////////////////

        List<MetadataValue> mdvs1 = itemService.getMetadata(
            v3_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs1.size());

        assertTrue(mdvs1.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs1.get(0).getValue());
        assertEquals(0, mdvs1.get(0).getPlace());

        assertFalse(mdvs1.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs1.get(1).getValue());
        assertEquals(1, mdvs1.get(1).getPlace());

        assertTrue(mdvs1.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs1.get(2).getValue());
        assertEquals(2, mdvs1.get(2).getPlace());

        assertFalse(mdvs1.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs1.get(3).getValue());
        assertEquals(3, mdvs1.get(3).getPlace());

        assertTrue(mdvs1.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs1.get(4).getValue());
        assertEquals(4, mdvs1.get(4).getPlace());

        assertFalse(mdvs1.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs1.get(5).getValue());
        assertEquals(5, mdvs1.get(5).getPlace());

        ////////////////////////////////
        // initial - verify issue 3.1 //
        ////////////////////////////////

        List<MetadataValue> mdvs2 = itemService.getMetadata(
            i3_1, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(6, mdvs2.size());

        assertTrue(mdvs2.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 1 (rel)", mdvs2.get(0).getValue());
        assertEquals(0, mdvs2.get(0).getPlace());

        assertFalse(mdvs2.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 2 (plain)", mdvs2.get(1).getValue());
        assertEquals(1, mdvs2.get(1).getPlace());

        assertTrue(mdvs2.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs2.get(2).getValue());
        assertEquals(2, mdvs2.get(2).getPlace());

        assertFalse(mdvs2.get(3) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 4 (plain)", mdvs2.get(3).getValue());
        assertEquals(3, mdvs2.get(3).getPlace());

        assertTrue(mdvs2.get(4) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs2.get(4).getValue());
        assertEquals(4, mdvs2.get(4).getPlace());

        assertFalse(mdvs2.get(5) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 6 (plain)", mdvs2.get(5).getValue());
        assertEquals(5, mdvs2.get(5).getPlace());

        ////////////////////////////////
        // initial - verify issue 5.1 //
        ////////////////////////////////

        List<MetadataValue> mdvs3 = itemService.getMetadata(
            i5_1, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(3, mdvs3.size());

        assertFalse(mdvs3.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 7 (plain)", mdvs3.get(0).getValue());
        assertEquals(0, mdvs3.get(0).getPlace());

        assertTrue(mdvs3.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs3.get(1).getValue());
        assertEquals(1, mdvs3.get(1).getPlace());

        assertTrue(mdvs3.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs3.get(2).getValue());
        assertEquals(2, mdvs3.get(2).getPlace());

        /////////////////////////////////////
        // create new version - volume 3.2 //
        /////////////////////////////////////

        Item v3_2 = versioningService.createNewVersion(context, v3_1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, v3_2));
        context.commit();

        ////////////////////////////////////
        // create new version - issue 3.2 //
        ////////////////////////////////////

        Item i3_2 = versioningService.createNewVersion(context, i3_1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, i3_2));
        context.commit();

        ////////////////////////////////////////////////
        // after version creation - verify volume 3.1 //
        ////////////////////////////////////////////////

        List<MetadataValue> mdvs4 = itemService.getMetadata(
            v3_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs4.size());

        assertTrue(mdvs4.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());

        assertFalse(mdvs4.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs4.get(1).getValue());
        assertEquals(1, mdvs4.get(1).getPlace());

        assertTrue(mdvs4.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs4.get(2).getValue());
        assertEquals(2, mdvs4.get(2).getPlace());

        assertFalse(mdvs4.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs4.get(3).getValue());
        assertEquals(3, mdvs4.get(3).getPlace());

        assertTrue(mdvs4.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs4.get(4).getValue());
        assertEquals(4, mdvs4.get(4).getPlace());

        assertFalse(mdvs4.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs4.get(5).getValue());
        assertEquals(5, mdvs4.get(5).getPlace());

        ///////////////////////////////////////////////
        // after version creation - verify issue 3.1 //
        ///////////////////////////////////////////////

        List<MetadataValue> mdvs5 = itemService.getMetadata(
            i3_1, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(6, mdvs5.size());

        assertTrue(mdvs5.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 1 (rel)", mdvs5.get(0).getValue());
        assertEquals(0, mdvs5.get(0).getPlace());

        assertFalse(mdvs5.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 2 (plain)", mdvs5.get(1).getValue());
        assertEquals(1, mdvs5.get(1).getPlace());

        assertTrue(mdvs5.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs5.get(2).getValue());
        assertEquals(2, mdvs5.get(2).getPlace());

        assertFalse(mdvs5.get(3) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 4 (plain)", mdvs5.get(3).getValue());
        assertEquals(3, mdvs5.get(3).getPlace());

        assertTrue(mdvs5.get(4) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs5.get(4).getValue());
        assertEquals(4, mdvs5.get(4).getPlace());

        assertFalse(mdvs5.get(5) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 6 (plain)", mdvs5.get(5).getValue());
        assertEquals(5, mdvs5.get(5).getPlace());

        ///////////////////////////////////////////////
        // after version creation - verify issue 5.1 //
        ///////////////////////////////////////////////

        List<MetadataValue> mdvs6 = itemService.getMetadata(
            i5_1, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(3, mdvs6.size());

        assertFalse(mdvs6.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 7 (plain)", mdvs6.get(0).getValue());
        assertEquals(0, mdvs6.get(0).getPlace());

        assertTrue(mdvs6.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs6.get(1).getValue());
        assertEquals(1, mdvs6.get(1).getPlace());

        assertTrue(mdvs6.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs6.get(2).getValue());
        assertEquals(2, mdvs6.get(2).getPlace());

        ////////////////////////////////////////////////
        // after version creation - verify volume 3.2 //
        ////////////////////////////////////////////////

        List<MetadataValue> mdvs7 = itemService.getMetadata(
            v3_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs7.size());

        assertTrue(mdvs7.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());

        assertFalse(mdvs7.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs7.get(1).getValue());
        assertEquals(1, mdvs7.get(1).getPlace());

        assertTrue(mdvs7.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs7.get(2).getValue());
        assertEquals(2, mdvs7.get(2).getPlace());

        assertFalse(mdvs7.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs7.get(3).getValue());
        assertEquals(3, mdvs7.get(3).getPlace());

        assertTrue(mdvs7.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs7.get(4).getValue());
        assertEquals(4, mdvs7.get(4).getPlace());

        assertFalse(mdvs7.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs7.get(5).getValue());
        assertEquals(5, mdvs7.get(5).getPlace());

        ///////////////////////////////////////////////
        // after version creation - verify issue 3.2 //
        ///////////////////////////////////////////////

        List<MetadataValue> mdvs8 = itemService.getMetadata(
            i3_2, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(6, mdvs8.size());

        assertTrue(mdvs8.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 1 (rel)", mdvs8.get(0).getValue());
        assertEquals(0, mdvs8.get(0).getPlace());

        assertFalse(mdvs8.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 2 (plain)", mdvs8.get(1).getValue());
        assertEquals(1, mdvs8.get(1).getPlace());

        assertTrue(mdvs8.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs8.get(2).getValue());
        assertEquals(2, mdvs8.get(2).getPlace());

        assertFalse(mdvs8.get(3) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 4 (plain)", mdvs8.get(3).getValue());
        assertEquals(3, mdvs8.get(3).getPlace());

        assertTrue(mdvs8.get(4) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs8.get(4).getValue());
        assertEquals(4, mdvs8.get(4).getPlace());

        assertFalse(mdvs8.get(5) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 6 (plain)", mdvs8.get(5).getValue());
        assertEquals(5, mdvs8.get(5).getPlace());

        //////////////////////////////////////////////////
        // remove relationship - volume 3.2 & issue 3.2 //
        //////////////////////////////////////////////////

        Relationship rel1 = getRelationship(v3_2, isIssueOfJournalVolume, i3_2);
        assertNotNull(rel1);

        relationshipService.delete(context, rel1, false, false);
        context.commit();

        ////////////////////////////////////////
        // after remove 1 - verify volume 3.1 //
        ////////////////////////////////////////

        List<MetadataValue> mdvs9 = itemService.getMetadata(
            v3_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs9.size());

        assertTrue(mdvs9.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());

        assertFalse(mdvs9.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs9.get(1).getValue());
        assertEquals(1, mdvs9.get(1).getPlace());

        assertTrue(mdvs9.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs9.get(2).getValue());
        assertEquals(2, mdvs9.get(2).getPlace());

        assertFalse(mdvs9.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs9.get(3).getValue());
        assertEquals(3, mdvs9.get(3).getPlace());

        assertTrue(mdvs9.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs9.get(4).getValue());
        assertEquals(4, mdvs9.get(4).getPlace());

        assertFalse(mdvs9.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs9.get(5).getValue());
        assertEquals(5, mdvs9.get(5).getPlace());

        ///////////////////////////////////////
        // after remove 1 - verify issue 3.1 //
        ///////////////////////////////////////

        List<MetadataValue> mdvs10 = itemService.getMetadata(
            i3_1, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(6, mdvs10.size());

        assertTrue(mdvs10.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 1 (rel)", mdvs10.get(0).getValue());
        assertEquals(0, mdvs10.get(0).getPlace());

        assertFalse(mdvs10.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 2 (plain)", mdvs10.get(1).getValue());
        assertEquals(1, mdvs10.get(1).getPlace());

        assertTrue(mdvs10.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs10.get(2).getValue());
        assertEquals(2, mdvs10.get(2).getPlace());

        assertFalse(mdvs10.get(3) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 4 (plain)", mdvs10.get(3).getValue());
        assertEquals(3, mdvs10.get(3).getPlace());

        assertTrue(mdvs10.get(4) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs10.get(4).getValue());
        assertEquals(4, mdvs10.get(4).getPlace());

        assertFalse(mdvs10.get(5) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 6 (plain)", mdvs10.get(5).getValue());
        assertEquals(5, mdvs10.get(5).getPlace());

        ///////////////////////////////////////
        // after remove 1 - verify issue 5.1 //
        ///////////////////////////////////////

        List<MetadataValue> mdvs11 = itemService.getMetadata(
            i5_1, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(3, mdvs11.size());

        assertFalse(mdvs11.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 7 (plain)", mdvs11.get(0).getValue());
        assertEquals(0, mdvs11.get(0).getPlace());

        assertTrue(mdvs11.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs11.get(1).getValue());
        assertEquals(1, mdvs11.get(1).getPlace());

        assertTrue(mdvs11.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 3 (rel)", mdvs11.get(2).getValue());
        assertEquals(2, mdvs11.get(2).getPlace());

        ////////////////////////////////////////
        // after remove 1 - verify volume 3.2 //
        ////////////////////////////////////////

        List<MetadataValue> mdvs12 = itemService.getMetadata(
            v3_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs12.size());

        assertTrue(mdvs12.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs12.get(0).getValue());
        assertEquals(0, mdvs12.get(0).getPlace());

        assertFalse(mdvs12.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs12.get(1).getValue());
        assertEquals(1, mdvs12.get(1).getPlace());

        assertFalse(mdvs12.get(32) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs12.get(32).getValue());
        assertEquals(32, mdvs12.get(2).getPlace());

        assertTrue(mdvs12.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs12.get(3).getValue());
        assertEquals(3, mdvs12.get(3).getPlace());

        assertFalse(mdvs12.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs12.get(4).getValue());
        assertEquals(4, mdvs12.get(4).getPlace());

        ///////////////////////////////////////
        // after remove 1 - verify issue 3.2 //
        ///////////////////////////////////////

        List<MetadataValue> mdvs13 = itemService.getMetadata(
            i3_2, "publicationvolume", "volumeNumber", null, Item.ANY
        );
        assertEquals(5, mdvs13.size());

        assertTrue(mdvs13.get(0) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 1 (rel)", mdvs13.get(0).getValue());
        assertEquals(0, mdvs13.get(0).getPlace());

        assertFalse(mdvs13.get(1) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 2 (plain)", mdvs13.get(1).getValue());
        assertEquals(1, mdvs13.get(1).getPlace());

        assertFalse(mdvs13.get(2) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 4 (plain)", mdvs13.get(2).getValue());
        assertEquals(2, mdvs13.get(2).getPlace());

        assertTrue(mdvs13.get(3) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 5 (rel)", mdvs13.get(3).getValue());
        assertEquals(3, mdvs13.get(3).getPlace());

        assertFalse(mdvs13.get(4) instanceof RelationshipMetadataValue);
        assertEquals("volume nr 6 (plain)", mdvs13.get(4).getValue());
        assertEquals(4, mdvs13.get(4).getPlace());

        // delete mdv from latest
        // delete rel from latest

        // TODO
        // delete mdv from older
        // delete rel from older
    }

    @Test
    public void test_virtualMetadataPreserved() throws Exception { // TODO
        //////////////////////////////////////////////
        // create a publication and link two people //
        //////////////////////////////////////////////

        Item publication1V1 = ItemBuilder.createItem(context, collection)
            .withTitle("publication 1V1")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        Item person1V1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 1V1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("Donald")
            .withPersonIdentifierLastName("Smith")
            .build();

        Item person2V1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 2V1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("Jane")
            .withPersonIdentifierLastName("Doe")
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1V1, person1V1, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1V1, person2V1, isAuthorOfPublication)
            .withRightwardValue("Doe, J.")
            .build();

        ///////////////////////////////////////////////
        // test dc.contributor.author of publication //
        ///////////////////////////////////////////////

        List<MetadataValue> mdvs1 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs1.size());

        assertTrue(mdvs1.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs1.get(0).getValue());
        assertEquals(0, mdvs1.get(0).getPlace());

        assertTrue(mdvs1.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs1.get(1).getValue());
        assertEquals(1, mdvs1.get(1).getPlace());

        ///////////////////////////////////////////////////////
        // create a new version of publication 1 and archive //
        ///////////////////////////////////////////////////////

        Item publication1V2 = versioningService.createNewVersion(context, publication1V1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, publication1V2));
        context.dispatchEvents();

        ////////////////////////////////////
        // create new version of person 1 //
        ////////////////////////////////////

        Item person1V2 = versioningService.createNewVersion(context, person1V1).getItem();
        // update "Smith, Donald" to "Smith, D."
        itemService.replaceMetadata(
            context, person1V2, "person", "givenName", null, null, "D.",
            null, -1, 0
        );
        itemService.update(context, person1V2);

        ///////////////////////////////////////////////////
        // test dc.contributor.author of old publication //
        ///////////////////////////////////////////////////

        List<MetadataValue> mdvs2 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs2.size());

        assertTrue(mdvs2.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs2.get(0).getValue());
        assertEquals(0, mdvs2.get(0).getPlace());

        assertTrue(mdvs2.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs2.get(1).getValue());
        assertEquals(1, mdvs2.get(1).getPlace());

        ///////////////////////////////////////////////////
        // test dc.contributor.author of new publication //
        ///////////////////////////////////////////////////

        List<MetadataValue> mdvs3 = itemService.getMetadata(
            publication1V2, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs3.size());

        assertTrue(mdvs3.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs3.get(0).getValue());
        assertEquals(0, mdvs3.get(0).getPlace());

        assertTrue(mdvs3.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs3.get(1).getValue());
        assertEquals(1, mdvs3.get(1).getPlace());

        /////////////////////////////////////
        // archive new version of person 1 //
        /////////////////////////////////////

        installItemService.installItem(context, workspaceItemService.findByItem(context, person1V2));
        context.dispatchEvents();

        ///////////////////////////////////////////////////
        // test dc.contributor.author of old publication //
        ///////////////////////////////////////////////////

        List<MetadataValue> mdvs4 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs4.size());

        assertTrue(mdvs4.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());

        assertTrue(mdvs4.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs4.get(1).getValue());
        assertEquals(1, mdvs4.get(1).getPlace());

        ///////////////////////////////////////////////////
        // test dc.contributor.author of new publication //
        ///////////////////////////////////////////////////

        List<MetadataValue> mdvs5 = itemService.getMetadata(
            publication1V2, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs5.size());

        assertTrue(mdvs5.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, D.", mdvs5.get(0).getValue());// TODO fix
        assertEquals(0, mdvs5.get(0).getPlace());

        assertTrue(mdvs5.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs5.get(1).getValue());
        assertEquals(1, mdvs5.get(1).getPlace());
    }

    // TODO
}
