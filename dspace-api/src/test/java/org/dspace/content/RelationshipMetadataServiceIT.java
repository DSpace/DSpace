/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
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
import org.junit.Before;
import org.junit.Test;

public class RelationshipMetadataServiceIT extends AbstractIntegrationTestWithDatabase {

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
    Collection col;
    Collection col2;
    Relationship relationship;
    RelationshipType isAuthorOfPublicationRelationshipType;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed.
     *
     * @throws Exception passed through.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();

        col = CollectionBuilder.createCollection(context, community)
                               .withEntityType("Publication")
                               .build();
        col2 = CollectionBuilder.createCollection(context, community)
                                .withEntityType("Author")
                                .build();

        leftItem = ItemBuilder.createItem(context, col).build();
        rightItem = ItemBuilder.createItem(context, col2).build();
        context.restoreAuthSystemState();
    }

    /**
     * Common function to convert leftItem to a publication item, convert rightItem to an author item,
     * and relating them to each other stored in the relationship field
     */
    protected void initPublicationAuthor() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Author").build();
        leftItem = ItemBuilder.createItem(context, col).build();
        rightItem = ItemBuilder.createItem(context, col2)
                               .withPersonIdentifierLastName("familyName")
                               .withPersonIdentifierFirstName("firstName").build();
        isAuthorOfPublicationRelationshipType =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor",
                null, null, null, null).build();

        relationship =
            RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem,
                isAuthorOfPublicationRelationshipType).build();
        context.restoreAuthSystemState();
    }

    /**
     * Common function to convert leftItem to a publication item, convert rightItem to an author item,
     * and relating them to each other stored in the relationship field
     */
    private void initPublicationAuthorWithCopyParams(boolean copyToLeft, boolean copyToRight)
        throws SQLException {
        context.turnOffAuthorisationSystem();
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Author").build();
        leftItem = ItemBuilder.createItem(context, col).build();
        rightItem = ItemBuilder.createItem(context, col2)
                               .withPersonIdentifierLastName("familyName")
                               .withPersonIdentifierFirstName("firstName").build();
        RelationshipType isAuthorOfPublication =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor",
                null, null, null, null)
                                   .withCopyToLeft(copyToLeft).withCopyToRight(copyToRight).build();

        relationship =
            RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, isAuthorOfPublication).build();
        context.restoreAuthSystemState();
    }

    /**
     * Common function to convert leftItem to a journal issue item, convert rightItem to a journal volume item,
     * and relating them to each other stored in the relationship field
     */
    protected void initJournalVolumeIssue() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();

        Collection col = CollectionBuilder.createCollection(context, community)
                               .withEntityType("JournalIssue")
                               .build();
        Collection col2 = CollectionBuilder.createCollection(context, community)
                                .withEntityType("JournalVolume")
                                .build();

        EntityType journalIssueEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();
        EntityType publicationVolumeEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        leftItem = ItemBuilder.createItem(context, col)
                              .withPublicationIssueNumber("2").build();
        rightItem = ItemBuilder.createItem(context, col2)
                               .withPublicationVolumeNumber("30").build();
        RelationshipType isIssueOfVolume =
            RelationshipTypeBuilder
                .createRelationshipTypeBuilder(context, journalIssueEntityType, publicationVolumeEntityType,
                    "isJournalVolumeOfIssue", "isIssueOfJournalVolume",
                    null, null, null, null).build();

        relationship =
            RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, isIssueOfVolume).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testGetAuthorRelationshipMetadata() throws Exception {
        initPublicationAuthor();
        //leftItem is the publication
        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));

        //verify the relation.isAuthorOfPublication virtual metadata
        List<MetadataValue> leftRelationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(leftRelationshipMetadataList.size(), equalTo(1));
        assertThat(leftRelationshipMetadataList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));

        //request the virtual metadata of the publication only
        List<RelationshipMetadataValue> leftList = relationshipMetadataService
            .getRelationshipMetadata(leftItem, true);
        assertThat(leftList.size(), equalTo(3));

        assertThat(leftList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(leftList.get(0).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(leftList.get(0).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(leftList.get(0).getMetadataField().getQualifier(), equalTo("latestForDiscovery"));
        assertThat(leftList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(leftList.get(1).getValue(), equalTo("familyName, firstName"));
        assertThat(leftList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(leftList.get(1).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(leftList.get(1).getMetadataField().getQualifier(), equalTo("author"));
        assertThat(leftList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(leftList.get(2).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(leftList.get(2).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(leftList.get(2).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(leftList.get(2).getMetadataField().getQualifier(), nullValue());
        assertThat(leftList.get(2).getAuthority(), equalTo("virtual::" + relationship.getID()));

        // rightItem is the author
        List<MetadataValue> rightRelationshipMetadataList = itemService
            .getMetadata(rightItem, MetadataSchemaEnum.RELATION.getName(), "isPublicationOfAuthor", null, Item.ANY);
        assertThat(rightRelationshipMetadataList.size(), equalTo(1));
        assertThat(rightRelationshipMetadataList.get(0).getValue(), equalTo(String.valueOf(leftItem.getID())));

        //request the virtual metadata of the publication
        List<RelationshipMetadataValue> rightList = relationshipMetadataService
            .getRelationshipMetadata(rightItem, true);
        assertThat(rightList.size(), equalTo(2));

        assertThat(rightList.get(0).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(rightList.get(0).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(rightList.get(0).getMetadataField().getElement(), equalTo("isPublicationOfAuthor"));
        assertThat(rightList.get(0).getMetadataField().getQualifier(), equalTo("latestForDiscovery"));
        assertThat(rightList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(rightList.get(1).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(rightList.get(1).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(rightList.get(1).getMetadataField().getElement(), equalTo("isPublicationOfAuthor"));
        assertThat(rightList.get(1).getMetadataField().getQualifier(), nullValue());
        assertThat(rightList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToLeftItem() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();

        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                        metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(0));

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> plainRelationshipMetadataList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList());
        assertThat(plainRelationshipMetadataList.size(), equalTo(0));

        //delete the relationship, copying the virtual metadata to actual metadata on the leftItem
        //leftItem is the publication
        relationshipService.delete(context, relationship, true, false);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                        metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's relation.isAuthorOfPublication actual metadata
        plainRelationshipMetadataList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList());
        assertThat(plainRelationshipMetadataList.size(), equalTo(1));
        //verify there's relation.isAuthorOfPublication actual metadata
        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));

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

        //verify there's relation.isPublicationOfAuthor actual metadata on the author
        assertThat(rightItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isPublicationOfAuthor"))
                .collect(Collectors.toList()).size(), equalTo(1));
        assertThat(itemService
                .getMetadata(rightItem, MetadataSchemaEnum.RELATION.getName(), "isPublicationOfAuthor", null, Item.ANY)
                .size(), equalTo(1));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToBothItems() throws Exception {
        initPublicationAuthor();
        context.turnOffAuthorisationSystem();
        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                                         metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(0));

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> plainRelationshipMetadataList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList());
        assertThat(plainRelationshipMetadataList.size(), equalTo(0));

        //delete the relationship, copying the virtual metadata to actual metadata on the both items
        relationshipService.delete(context, relationship, true, true);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                                         metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's relation.isPublicationOfAuthor actual metadata
        assertEquals(1, rightItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isPublicationOfAuthor"))
                .collect(Collectors.toList()).size());
        assertEquals(1, itemService
                .getMetadata(rightItem, MetadataSchemaEnum.RELATION.getName(), "isPublicationOfAuthor", null, Item.ANY)
                .size());

        //verify there's relation.isAuthorOfPublication actual metadata
        assertEquals(1, leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList()).size());
        assertEquals(1, itemService
                .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY)
                .size());
    }

    @Test
    public void testGetJournalRelationshipMetadata() throws Exception {
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
        assertThat(issueRelList.size(), equalTo(3));

        assertThat(issueRelList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(issueRelList.get(0).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(issueRelList.get(0).getMetadataField().getElement(), equalTo("isJournalVolumeOfIssue"));
        assertThat(issueRelList.get(0).getMetadataField().getQualifier(), equalTo("latestForDiscovery"));
        assertThat(issueRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(issueRelList.get(1).getValue(), equalTo("30"));
        assertThat(issueRelList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("publicationvolume"));
        assertThat(issueRelList.get(1).getMetadataField().getElement(), equalTo("volumeNumber"));
        assertThat(issueRelList.get(1).getMetadataField().getQualifier(), equalTo(null));
        assertThat(issueRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(issueRelList.get(2).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(issueRelList.get(2).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(issueRelList.get(2).getMetadataField().getElement(), equalTo("isJournalVolumeOfIssue"));
        assertThat(issueRelList.get(2).getMetadataField().getQualifier(), nullValue());
        assertThat(issueRelList.get(2).getAuthority(), equalTo("virtual::" + relationship.getID()));

        //request the virtual metadata of the journal volume
        List<RelationshipMetadataValue> volumeRelList =
            relationshipMetadataService.getRelationshipMetadata(rightItem, true);
        assertThat(volumeRelList.size(), equalTo(3));

        assertThat(volumeRelList.get(0).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(volumeRelList.get(0).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(volumeRelList.get(0).getMetadataField().getElement(), equalTo("isIssueOfJournalVolume"));
        assertThat(volumeRelList.get(0).getMetadataField().getQualifier(), equalTo("latestForDiscovery"));
        assertThat(volumeRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(volumeRelList.get(1).getValue(), equalTo("2"));
        assertThat(volumeRelList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("publicationissue"));
        assertThat(volumeRelList.get(1).getMetadataField().getElement(), equalTo("issueNumber"));
        assertThat(volumeRelList.get(1).getMetadataField().getQualifier(), equalTo(null));
        assertThat(volumeRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(volumeRelList.get(2).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(volumeRelList.get(2).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(volumeRelList.get(2).getMetadataField().getElement(), equalTo("isIssueOfJournalVolume"));
        assertThat(volumeRelList.get(2).getMetadataField().getQualifier(), nullValue());
        assertThat(volumeRelList.get(2).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

    @Test
    public void testDeleteJournalRelationshipCopyToLeftItem() throws Exception {
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
    public void testJournalDeleteRelationshipCopyToRightItem() throws Exception {
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
    public void testDeleteJournalRelationshipCopyToBothItems() throws Exception {
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

        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                        metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(0));

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> plainRelationshipMetadataList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList());
        assertThat(plainRelationshipMetadataList.size(), equalTo(0));

        //delete the relationship, copying the virtual metadata to actual metadata on the leftItem
        //leftItem is the publication
        relationshipService.delete(context, relationship);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                        metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's relation.isAuthorOfPublication actual metadata
        plainRelationshipMetadataList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList());
        assertThat(plainRelationshipMetadataList.size(), equalTo(1));
        //verify there's relation.isAuthorOfPublication actual metadata
        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));

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

        //verify there's relation.isPublicationOfAuthor actual metadata on the author
        assertThat(rightItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isPublicationOfAuthor"))
                .collect(Collectors.toList()).size(), equalTo(1));
        assertThat(itemService
                .getMetadata(rightItem, MetadataSchemaEnum.RELATION.getName(), "isPublicationOfAuthor", null, Item.ANY)
                .size(), equalTo(1));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToBothItemsFromDefaultsInDb() throws Exception {
        initPublicationAuthorWithCopyParams(true, true);
        context.turnOffAuthorisationSystem();
        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        List<MetadataValue> plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                        metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(0));

        //verify there's no relation.isAuthorOfPublication actual metadata
        List<MetadataValue> plainRelationshipMetadataList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList());
        assertThat(plainRelationshipMetadataList.size(), equalTo(0));

        //delete the relationship, copying the virtual metadata to actual metadata on the both items
        relationshipService.delete(context, relationship);
        context.restoreAuthSystemState();

        //verify the dc.contributor.author actual metadata
        plainMetadataAuthorList = leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                        metadataValue.getMetadataField().getQualifier().equals("author"))
                .collect(Collectors.toList());
        assertThat(plainMetadataAuthorList.size(), equalTo(1));

        //verify the dc.contributor.author actual metadata
        authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(authorList.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(authorList.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(authorList.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertNull(authorList.get(0).getAuthority());

        //verify there's relation.isAuthorOfPublication actual metadata on the publication
        assertThat(leftItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isAuthorOfPublication"))
                .collect(Collectors.toList()).size(), equalTo(1));
        assertThat(itemService
                .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY)
                .size(), equalTo(1));
        //verify there's relation.isPublicationOfAuthor actual metadata on the author
        assertThat(rightItem.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().getElement().equals("isPublicationOfAuthor"))
                .collect(Collectors.toList()).size(), equalTo(1));
        assertThat(itemService
                .getMetadata(rightItem, MetadataSchemaEnum.RELATION.getName(), "isPublicationOfAuthor", null, Item.ANY)
                .size(), equalTo(1));
    }

    @Test
    public void testGetVirtualMetadata() throws SQLException, AuthorizeException {
        // Journal, JournalVolume, JournalIssue, Publication items, related to each other using the relationship types
        // isJournalOfVolume, isJournalVolumeOfIssue, isJournalIssueOfPublication.
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection col = CollectionBuilder.createCollection(context, community)
                                          .withEntityType("JournalIssue")
                                          .build();
        Collection col2 = CollectionBuilder.createCollection(context, community)
                                           .withEntityType("JournalVolume")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, community)
                                           .withEntityType("Journal")
                                           .build();
        Collection col4 = CollectionBuilder.createCollection(context, community)
                                           .withEntityType("Publication")
                                           .build();

        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType journalIssueEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();
        EntityType journalVolumeEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        EntityType journalEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        RelationshipType isJournalVolumeOfIssueRelationshipType =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalIssueEntityType,
                journalVolumeEntityType, "isIssueOfJournalVolume", "isJournalVolumeOfIssue", null, null, null, null)
                                   .build();
        RelationshipType isJournalVolumeOfJournalRelationshipType =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalVolumeEntityType,
                journalEntityType, "isJournalOfVolume", "isVolumeOfJournal", null, null, null, null)
                                   .build();
        RelationshipType isJournalIssueOfPublicationRelationshipType =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType,
                journalIssueEntityType, "isJournalIssueOfPublication", "isPublicationOfJournalIssue", null, null, null,
                null)
                                   .build();

        Item journalIssue = ItemBuilder.createItem(context, col).build();
        Item journalVolume = ItemBuilder.createItem(context, col2)
                                        .withPublicationVolumeNumber("30")
                                        .build();
        Item journal = ItemBuilder.createItem(context, col3)
                                  .withMetadata("creativeworkseries", "issn", null, "issn journal")
                                  .build();
        RelationshipBuilder.createRelationshipBuilder(context, journalIssue, journalVolume,
            isJournalVolumeOfIssueRelationshipType).build();
        RelationshipBuilder.createRelationshipBuilder(context, journalVolume, journal,
            isJournalVolumeOfJournalRelationshipType).build();

        Item publication = ItemBuilder.createItem(context, col4)
                                      .withTitle("Pub 1")
                                      .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication, journalIssue,
            isJournalIssueOfPublicationRelationshipType).build();
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

        // Query for the publication itemService.getMetadata(publication, "creativeworkseries", "issn", null, Item
        // .ANY, true); and verify it contains a creativeworkseries.issn metadata value with the value being the
        // journal’s issn
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
