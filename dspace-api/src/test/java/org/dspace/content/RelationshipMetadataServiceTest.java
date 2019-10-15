/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RelationshipMetadataServiceTest extends AbstractUnitTest {

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(RelationshipMetadataServiceTest.class);

    protected RelationshipMetadataService relationshipMetadataService = ContentServiceFactory
                                                                        .getInstance().getRelationshipMetadataService();
    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                     .getRelationshipTypeService();
    protected EntityService entityService = ContentServiceFactory.getInstance().getEntityService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    Item leftItem;
    Item rightItem;
    Relationship relationship;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            Community community = communityService.create(null, context);

            Collection col = collectionService.create(context, community);
            WorkspaceItem leftIs = workspaceItemService.create(context, col, false);
            WorkspaceItem rightIs = workspaceItemService.create(context, col, false);

            leftItem = installItemService.installItem(context, leftIs);
            rightItem = installItemService.installItem(context, rightIs);
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        context.abort();
        super.destroy();
    }

    private void initPublicationAuthor() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, leftItem, "relationship", "type", null, null, "Publication");
        itemService.addMetadata(context, rightItem, "relationship", "type", null, null, "Author");
        itemService.addMetadata(context, rightItem, "person", "familyName", null, null, "familyName");
        itemService.addMetadata(context, rightItem, "person", "givenName", null, null, "firstName");
        EntityType publicationEntityType = entityTypeService.create(context, "Publication");
        EntityType authorEntityType = entityTypeService.create(context, "Author");
        RelationshipType isAuthorOfPublication = relationshipTypeService
                .create(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null);

        relationship = relationshipService.create(context, leftItem, rightItem, isAuthorOfPublication, 0, 0);
        context.restoreAuthSystemState();
    }

    private void initJournalVolumeIssue() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, leftItem, "relationship", "type", null, null, "JournalIssue");
        itemService.addMetadata(context, rightItem, "relationship", "type", null, null, "JournalVolume");
        itemService.addMetadata(context, leftItem, "publicationissue", "issueNumber", null, null, "2");
        itemService.addMetadata(context, rightItem, "publicationvolume", "volumeNumber", null, null, "30");
        EntityType journalIssueEntityType = entityTypeService.create(context, "JournalIssue");
        EntityType publicationVolumeEntityType = entityTypeService.create(context, "JournalVolume");
        RelationshipType isIssueOfVolume = relationshipTypeService
                .create(context, journalIssueEntityType, publicationVolumeEntityType,
                        "isJournalVolumeOfIssue", "isIssueOfJournalVolume",
                        null, null, null, null);

        relationship = relationshipService.create(context, leftItem, rightItem, isIssueOfVolume, 0, 0);
        context.restoreAuthSystemState();
    }

    @Test
    public void testGetAuthorRelationshipMetadata() throws SQLException, AuthorizeException {
        initPublicationAuthor();
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));

        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));
        assertThat(relationshipMetadataList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));

        List<RelationshipMetadataValue> list = relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(list.size(), equalTo(2));
        assertThat(list.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(list.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(list.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(list.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertThat(list.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(list.get(1).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(list.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("relation"));
        assertThat(list.get(1).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(list.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToLeftItem() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship, true, false);
        context.restoreAuthSystemState();

        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        List<MetadataValue> relationshipMetadataList = itemService
                .getMetadata(leftItem, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testAuthorDeleteRelationshipCopyToRightItem() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship, false, true);
        context.restoreAuthSystemState();

        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(0));

        List<MetadataValue> relationshipMetadataList = itemService
                .getMetadata(leftItem, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToBothItems() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship, true, true);
        context.restoreAuthSystemState();

        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        List<MetadataValue> relationshipMetadataList = itemService
                .getMetadata(leftItem, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testGetJournalRelationshipMetadata() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();

        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));

        List<RelationshipMetadataValue> issueRelList =
                relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(issueRelList.size(), equalTo(2));
        assertThat(issueRelList.get(0).getValue(), equalTo("30"));
        assertThat(issueRelList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("publicationvolume"));
        assertThat(issueRelList.get(0).getMetadataField().getElement(), equalTo("volumeNumber"));
        assertThat(issueRelList.get(0).getMetadataField().getQualifier(), equalTo(null));
        assertThat(issueRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(issueRelList.get(1).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(issueRelList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("relation"));
        assertThat(issueRelList.get(1).getMetadataField().getElement(), equalTo("isJournalVolumeOfIssue"));
        assertThat(issueRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        List<RelationshipMetadataValue> volumeRelList =
                relationshipMetadataService.getRelationshipMetadata(rightItem, true);
        assertThat(volumeRelList.size(), equalTo(2));
        assertThat(volumeRelList.get(0).getValue(), equalTo("2"));
        assertThat(volumeRelList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("publicationissue"));
        assertThat(volumeRelList.get(0).getMetadataField().getElement(), equalTo("issueNumber"));
        assertThat(volumeRelList.get(0).getMetadataField().getQualifier(), equalTo(null));
        assertThat(volumeRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(volumeRelList.get(1).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(volumeRelList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("relation"));
        assertThat(volumeRelList.get(1).getMetadataField().getElement(), equalTo("isIssueOfJournalVolume"));
        assertThat(volumeRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

    @Test
    public void testDeleteJournalRelationshipCopyToLeftItem() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship, true, false);
        context.restoreAuthSystemState();

        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(0));
    }

    @Test
    public void testJournalDeleteRelationshipCopyToRightItem() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship, false, true);
        context.restoreAuthSystemState();

        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(0));

        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }

    @Test
    public void testDeleteJournalRelationshipCopyToBothItems() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();
        context.turnOffAuthorisationSystem();
        relationshipService.delete(context, relationship, true, true);
        context.restoreAuthSystemState();

        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }
}
