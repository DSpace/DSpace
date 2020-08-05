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
import static org.junit.Assert.assertTrue;
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
    RelationshipType isAuthorOfPublicationRelationshipType;

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

    /**
     * Common function to convert leftItem to a publication item, convert rightItem to an author item,
     * and relating them to each other stored in the relationship field
     */
    private void initPublicationAuthor() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, leftItem, "relationship", "type", null, null, "Publication");
        itemService.addMetadata(context, rightItem, "relationship", "type", null, null, "Author");
        itemService.addMetadata(context, rightItem, "person", "familyName", null, null, "familyName");
        itemService.addMetadata(context, rightItem, "person", "givenName", null, null, "firstName");
        EntityType publicationEntityType = entityTypeService.create(context, "Publication");
        EntityType authorEntityType = entityTypeService.create(context, "Author");
        isAuthorOfPublicationRelationshipType = relationshipTypeService
                .create(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null);

        relationship = relationshipService.create(context, leftItem, rightItem,
            isAuthorOfPublicationRelationshipType, 0, 0);
        context.restoreAuthSystemState();
    }

    /**
     * Common function to convert leftItem to a publication item, convert rightItem to an author item,
     * and relating them to each other stored in the relationship field
     */
    private void initPublicationAuthorWithCopyParams(boolean copyToLeft, boolean copyToRight)
        throws SQLException, AuthorizeException {
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
                    null, null, null, null, copyToLeft, copyToRight);

        relationship = relationshipService.create(context, leftItem, rightItem, isAuthorOfPublication, 0, 0);
        context.restoreAuthSystemState();
    }

    /**
     * Common function to convert leftItem to a journal issue item, convert rightItem to a journal volume item,
     * and relating them to each other stored in the relationship field
     */
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
        //leftItem is the publication
        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));

        //verify the relation.isAuthorOfPublication virtual metadata
        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));
        assertThat(relationshipMetadataList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));

        //request the virtual metadata of the publication only
        List<RelationshipMetadataValue> list = relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(list.size(), equalTo(2));
        assertThat(list.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(list.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(list.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(list.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertThat(list.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(list.get(1).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(list.get(1).getMetadataField().getMetadataSchema().getName(),
                equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(list.get(1).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(list.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToLeftItem() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        //delete the relationship, copying the virtual metadata to actual metadata on the leftItem
        //leftItem is the publication
        relationshipService.delete(context, relationship, true, false);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> relationshipMetadataList = itemService
                .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));

        //request the virtual metadata of the publication only
        List<RelationshipMetadataValue> list = relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(list.size(), equalTo(0));
    }

    @Test
    public void testAuthorDeleteRelationshipCopyToRightItem() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        //delete the relationship, copying the virtual metadata to actual metadata on the rightItem
        //rightItem is the author
        relationshipService.delete(context, relationship, false, true);
        context.restoreAuthSystemState();

        //verify there's no dc.contributor.author actual metadata on the publication
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(0));

        //verify there's no relation.isAuthorOfPublication actual metadata on the publication
        List<MetadataValue> relationshipMetadataList = itemService
                .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToBothItems() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        //delete the relationship, copying the virtual metadata to actual metadata on the both items
        relationshipService.delete(context, relationship, true, true);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> relationshipMetadataList = itemService
                .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testGetJournalRelationshipMetadata() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();

        //leftItem is the journal issue item
        //verify the publicationvolume.volumeNumber virtual metadata
        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        //rightItem is the journal volume item
        //verify the publicationissue.issueNumber virtual metadata
        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));

        //request the virtual metadata of the journal issue
        List<RelationshipMetadataValue> issueRelList =
                relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(issueRelList.size(), equalTo(2));
        assertThat(issueRelList.get(0).getValue(), equalTo("30"));
        assertThat(issueRelList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("publicationvolume"));
        assertThat(issueRelList.get(0).getMetadataField().getElement(), equalTo("volumeNumber"));
        assertThat(issueRelList.get(0).getMetadataField().getQualifier(), equalTo(null));
        assertThat(issueRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(issueRelList.get(1).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(issueRelList.get(1).getMetadataField().getMetadataSchema().getName(),
                equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(issueRelList.get(1).getMetadataField().getElement(), equalTo("isJournalVolumeOfIssue"));
        assertThat(issueRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        //request the virtual metadata of the journal volume
        List<RelationshipMetadataValue> volumeRelList =
                relationshipMetadataService.getRelationshipMetadata(rightItem, true);
        assertThat(volumeRelList.size(), equalTo(2));
        assertThat(volumeRelList.get(0).getValue(), equalTo("2"));
        assertThat(volumeRelList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("publicationissue"));
        assertThat(volumeRelList.get(0).getMetadataField().getElement(), equalTo("issueNumber"));
        assertThat(volumeRelList.get(0).getMetadataField().getQualifier(), equalTo(null));
        assertThat(volumeRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(volumeRelList.get(1).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(volumeRelList.get(1).getMetadataField().getMetadataSchema().getName(),
                equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(volumeRelList.get(1).getMetadataField().getElement(), equalTo("isIssueOfJournalVolume"));
        assertThat(volumeRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

    @Test
    public void testDeleteJournalRelationshipCopyToLeftItem() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();
        context.turnOffAuthorisationSystem();
        //leftItem is the journal issue item
        relationshipService.delete(context, relationship, true, false);
        context.restoreAuthSystemState();

        //verify the left item's publicationvolume.volumeNumber actual metadata
        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        //verify the right item doesn't contain the actual metadata
        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(0));
    }

    @Test
    public void testJournalDeleteRelationshipCopyToRightItem() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();
        context.turnOffAuthorisationSystem();
        //rightItem is the journal volume item
        relationshipService.delete(context, relationship, false, true);
        context.restoreAuthSystemState();

        //verify the left item doesn't contain the publicationvolume.volumeNumber actual metadata
        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(0));

        //verify the right item's publicationissue.issueNumber actual metadata
        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }

    @Test
    public void testDeleteJournalRelationshipCopyToBothItems() throws SQLException, AuthorizeException {
        initJournalVolumeIssue();
        context.turnOffAuthorisationSystem();
        //leftItem is the journal issue item
        //rightItem is the journal volume item
        relationshipService.delete(context, relationship, true, true);
        context.restoreAuthSystemState();

        //verify the left item's publicationvolume.volumeNumber actual metadata
        List<MetadataValue> volumeList =
                itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        //verify the right item's publicationissue.issueNumber actual metadata
        List<MetadataValue> issueList =
                itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToLeftItemFromDefaultInDb() throws Exception {
        initPublicationAuthorWithCopyParams(true, false);
        context.turnOffAuthorisationSystem();
        //delete the relationship, copying the virtual metadata to actual metadata on the leftItem
        //leftItem is the publication
        relationshipService.delete(context, relationship);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));

        //request the virtual metadata of the publication only
        List<RelationshipMetadataValue> list = relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(list.size(), equalTo(0));
    }

    @Test
    public void testAuthorDeleteRelationshipCopyToRightItemFromDefaultInDb() throws Exception {
        initPublicationAuthorWithCopyParams(false, true);
        context.turnOffAuthorisationSystem();
        //delete the relationship, copying the virtual metadata to actual metadata on the rightItem
        //rightItem is the author
        relationshipService.delete(context, relationship);
        context.restoreAuthSystemState();

        //verify there's no dc.contributor.author actual metadata on the publication
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(0));

        //verify there's no relation.isAuthorOfPublication actual metadata on the publication
        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToBothItemsFromDefaultsInDb() throws Exception {
        initPublicationAuthorWithCopyParams(true, true);
        context.turnOffAuthorisationSystem();
        //delete the relationship, copying the virtual metadata to actual metadata on the both items
        relationshipService.delete(context, relationship);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testGetNextRightPlace() throws Exception {
        assertThat(relationshipService.findNextRightPlaceByRightItem(context, rightItem), equalTo(0));
        initPublicationAuthor();

        assertThat(relationshipService.findNextRightPlaceByRightItem(context, rightItem), equalTo(1));

        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);

        Collection col = collectionService.create(context, community);
        WorkspaceItem is = workspaceItemService.create(context, col, false);
        Item secondItem = installItemService.installItem(context, is);
        itemService.addMetadata(context, secondItem, "relationship", "type", null, null, "Publication");
        relationshipService.create(context, secondItem, rightItem,
                                   isAuthorOfPublicationRelationshipType, 0, 0);
        context.restoreAuthSystemState();

        assertThat(relationshipService.findNextRightPlaceByRightItem(context, rightItem), equalTo(2));



    }

    @Test
    public void testGetNextLeftPlace() throws Exception {
        assertThat(relationshipService.findNextLeftPlaceByLeftItem(context, leftItem), equalTo(0));
        initPublicationAuthor();

        assertThat(relationshipService.findNextLeftPlaceByLeftItem(context, leftItem), equalTo(1));

        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);

        Collection col = collectionService.create(context, community);
        WorkspaceItem is = workspaceItemService.create(context, col, false);
        Item secondAuthor = installItemService.installItem(context, is);
        itemService.addMetadata(context, secondAuthor, "relationship", "type", null, null, "Author");
        itemService.addMetadata(context, secondAuthor, "person", "familyName", null, null, "familyName");
        itemService.addMetadata(context, secondAuthor, "person", "givenName", null, null, "firstName");
        relationshipService.create(context, leftItem, secondAuthor,
                                   isAuthorOfPublicationRelationshipType, 0, 0);
        context.restoreAuthSystemState();

        assertThat(relationshipService.findNextLeftPlaceByLeftItem(context, leftItem), equalTo(2));


    }

    @Test
    public void testGetVirtualMetadata() throws SQLException, AuthorizeException {
        // Journal, JournalVolume, JournalIssue, Publication items, related to each other using the relationship types
        // isJournalOfVolume, isJournalVolumeOfIssue, isJournalIssueOfPublication.
        context.turnOffAuthorisationSystem();
        EntityType publicationEntityType = entityTypeService.create(context, "Publication");
        EntityType journalIssueEntityType = entityTypeService.create(context, "JournalIssue");
        EntityType journalVolumeEntityType = entityTypeService.create(context, "JournalVolume");
        EntityType journalEntityType = entityTypeService.create(context, "Journal");
        RelationshipType isJournalVolumeOfIssueRelationshipType = relationshipTypeService
            .create(context, journalIssueEntityType, journalVolumeEntityType,
                "isIssueOfJournalVolume", "isJournalVolumeOfIssue",
                null, null, null, null);
        RelationshipType isJournalVolumeOfJournalRelationshipType = relationshipTypeService
            .create(context, journalVolumeEntityType, journalEntityType,
                "isJournalOfVolume", "isVolumeOfJournal",
                null, null, null, null);
        RelationshipType isJournalIssueOfPublicationRelationshipType = relationshipTypeService
            .create(context, publicationEntityType, journalIssueEntityType,
                "isJournalIssueOfPublication", "isPublicationOfJournalIssue",
                null, null, null, null);

        Community community = communityService.create(null, context);
        Collection collection = collectionService.create(context, community);

        WorkspaceItem iss = workspaceItemService.create(context, collection, false);
        Item journalIssue = installItemService.installItem(context, iss);
        itemService.addMetadata(context, journalIssue, "relationship", "type", null, null, "JournalIssue");
        WorkspaceItem vol = workspaceItemService.create(context, collection, false);
        Item journalVolume = installItemService.installItem(context, vol);
        itemService.addMetadata(context, journalVolume, "publicationvolume", "volumeNumber", null, null, "30");
        itemService.addMetadata(context, journalVolume, "relationship", "type", null, null, "JournalVolume");
        WorkspaceItem jour = workspaceItemService.create(context, collection, false);
        Item journal = installItemService.installItem(context, jour);
        itemService.addMetadata(context, journal, "creativeworkseries", "issn", null, null, "issn journal");
        itemService.addMetadata(context, journal, "relationship", "type", null, null, "Journal");
        relationship =
            relationshipService
                .create(context, journalIssue, journalVolume, isJournalVolumeOfIssueRelationshipType, 0, 0);
        relationship =
            relationshipService.create(context, journalVolume, journal, isJournalVolumeOfJournalRelationshipType, 0, 0);

        WorkspaceItem pub = workspaceItemService.create(context, collection, false);
        Item publication = installItemService.installItem(context, pub);
        itemService.addMetadata(context, publication, "dc", "title", null, null, "Pub 1");
        itemService.addMetadata(context, publication, "relationship", "type", null, null, "Publication");

        relationship =
            relationshipService
                .create(context, publication, journalIssue, isJournalIssueOfPublicationRelationshipType, 0, 0);
        context.restoreAuthSystemState();

        // Query for the publication itemService.getMetadata(publication, Item.ANY, Item.ANY, null, Item.ANY, true);
        // and verify it contains a relation.isJournalOfPublication metadata value with the value being the journal’s
        // UUID
        List<MetadataValue> mdPublication =
            itemService.getMetadata(publication, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        boolean foundVirtualMdIsJournalOfPublicationInAllMD = false;
        for (MetadataValue metadataValue : mdPublication) {
            String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
            String element = metadataValue.getMetadataField().getElement();
            String qualifier = metadataValue.getMetadataField().getQualifier();
            if (schema.equals("relation") && element.equals("isJournalOfPublication") && qualifier == null &&
                metadataValue.getValue().equals(journal.getID().toString())) {
                foundVirtualMdIsJournalOfPublicationInAllMD = true;
            }
        }
        assertTrue(foundVirtualMdIsJournalOfPublicationInAllMD);

        // Query for the publication itemService.getMetadata(publication, "relation", "isJournalOfPublication", null,
        // Item.ANY, true); and verify it contains a relation.isJournalOfPublication metadata value with the value
        // being the journal’s UUID
        List<MetadataValue> mdPublicationRelationIsJournalOfPublication =
            itemService.getMetadata(publication, "relation", "isJournalOfPublication", Item.ANY, Item.ANY, true);
        boolean foundVirtualMdIsJournalOfPublicationInSpecificQuery = false;
        for (MetadataValue metadataValue : mdPublicationRelationIsJournalOfPublication) {
            String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
            String element = metadataValue.getMetadataField().getElement();
            String qualifier = metadataValue.getMetadataField().getQualifier();
            if (schema.equals("relation") && element.equals("isJournalOfPublication") && qualifier == null &&
                metadataValue.getValue().equals(journal.getID().toString())) {
                foundVirtualMdIsJournalOfPublicationInSpecificQuery = true;
            }
        }
        assertTrue(foundVirtualMdIsJournalOfPublicationInSpecificQuery);

        // Query for the publication itemService.getMetadata(publication, Item.ANY, Item.ANY, null, Item.ANY, true);
        // and verify it contains a creativeworkseries.issn metadata value with the value being the journal’s issn
        boolean foundVirtualMdCreativeWorksISSNInAllMD = false;
        for (MetadataValue metadataValue : mdPublication) {
            String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
            String element = metadataValue.getMetadataField().getElement();
            String qualifier = metadataValue.getMetadataField().getQualifier();
            if (schema.equals("creativeworkseries") && element.equals("issn") && qualifier == null &&
                metadataValue.getValue().equals("issn journal")) {
                foundVirtualMdCreativeWorksISSNInAllMD = true;
            }
        }
        assertTrue(foundVirtualMdCreativeWorksISSNInAllMD);

        // Query for the publication itemService.getMetadata(publication, "creativeworkseries", "issn", null, Item.ANY, true);
        // and verify it contains a creativeworkseries.issn metadata value with the value being the journal’s issn
        List<MetadataValue> mdPublicationVirtualMdCreativeWorksISSN = itemService.getMetadata(publication,
            "creativeworkseries", "issn", Item.ANY, Item.ANY, true);
        boolean foundCreativeWorksISSNInSpecificQuery = false;
        for (MetadataValue metadataValue : mdPublicationVirtualMdCreativeWorksISSN) {
            String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
            String element = metadataValue.getMetadataField().getElement();
            String qualifier = metadataValue.getMetadataField().getQualifier();
            if (schema.equals("creativeworkseries") && element.equals("issn") && qualifier == null &&
                metadataValue.getValue().equals("issn journal")) {
                foundCreativeWorksISSNInSpecificQuery = true;
            }
        }
        assertTrue(foundCreativeWorksISSNInSpecificQuery);
    }
}
