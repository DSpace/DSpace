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
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotSame;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.RelationshipService;
import org.dspace.versioning.Version;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class VersioningWithRelationshipsTest extends AbstractIntegrationTestWithDatabase {

    private RelationshipService relationshipService;
    private VersioningService versioningService;

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

        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        versioningService = VersionServiceFactory.getInstance().getVersionService();

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
                0, null, 0, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isOrgUnitOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, orgUnitEntityType,
                "isOrgUnitOfPublication", "isPublicationOfOrgUnit",
                0, null, 0, null
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

        //////////////
        // clean up //
        //////////////

        versioningService.removeVersion(context, newPublication);
    }

    @Test
    public void test_createNewVersionOfItemOnRightSideOfRelationships() throws Exception {
        ///////////////////////////////////////////////
        // create a publication with 3 relationships //
        ///////////////////////////////////////////////

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

        //////////////
        // clean up //
        //////////////

        versioningService.removeVersion(context, newPerson);
    }

}
