/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.RelationshipBuilder;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipDeleteRestRepositoryIT extends AbstractEntityIntegrationTest {

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    private Item leftItem;
    private Item rightItem;
    private Collection collection;
    private RelationshipType relationshipType;
    private Relationship relationship;
    private String adminAuthToken;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        adminAuthToken = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();
        collection = CollectionBuilder.createCollection(context, community)
            .withName("Collection")
            .build();
        context.restoreAuthSystemState();
    }

    private void initPublicationAuthor() throws Exception {
        context.turnOffAuthorisationSystem();

        leftItem = ItemBuilder.createItem(context, collection)
            .withTitle("Left item")
            .withRelationshipType("Publication")
            .build();
        rightItem = ItemBuilder.createItem(context, collection)
            .withTitle("Right item")
            .withRelationshipType("Person")
            .withPersonIdentifierFirstName("firstName")
            .withPersonIdentifierLastName("familyName")
            .build();
        relationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                entityTypeService.findByEntityType(context, "Person"),
                "isAuthorOfPublication", "isPublicationOfAuthor");
        relationship = RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, relationshipType)
            .withLeftPlace(0)
            .build();

        context.restoreAuthSystemState();
    }

    private void initJournalVolumeIssue() throws Exception {
        context.turnOffAuthorisationSystem();

        leftItem = ItemBuilder.createItem(context, collection)
            .withTitle("Left item")
            .withRelationshipType("JournalIssue")
            .withPublicationIssueNumber("2")
            .build();
        rightItem = ItemBuilder.createItem(context, collection)
            .withTitle("Right item")
            .withRelationshipType("JournalVolume")
            .withPublicationVolumeNumber("30")
            .build();
        relationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "JournalIssue"),
                entityTypeService.findByEntityType(context, "JournalVolume"),
                "isJournalVolumeOfIssue", "isIssueOfJournalVolume");
        relationship = RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, relationshipType)
            .withLeftPlace(0)
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToLeftItem() throws Exception {
        initPublicationAuthor();

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=left"))
            .andExpect(status().isNoContent());

        leftItem = itemService.find(context, leftItem.getID());
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

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=right"))
            .andExpect(status().isNoContent());

        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(0));

        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToBothItems() throws Exception {
        initPublicationAuthor();

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=all"))
            .andExpect(status().isNoContent());

        leftItem = itemService.find(context, leftItem.getID());
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
    public void testDeleteJournalRelationshipCopyToLeftItem() throws Exception {
        initJournalVolumeIssue();

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=left"))
            .andExpect(status().isNoContent());

        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        rightItem = itemService.find(context, rightItem.getID());
        List<MetadataValue> issueList =
            itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(0));
    }

    @Test
    public void testJournalDeleteRelationshipCopyToRightItem() throws Exception {
        initJournalVolumeIssue();

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=right"))
            .andExpect(status().isNoContent());

        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(0));

        rightItem = itemService.find(context, rightItem.getID());
        List<MetadataValue> issueList =
            itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }

    @Test
    public void testDeleteJournalRelationshipCopyToBothItems() throws Exception {
        initJournalVolumeIssue();

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=all"))
            .andExpect(status().isNoContent());

        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        rightItem = itemService.find(context, rightItem.getID());
        List<MetadataValue> issueList =
            itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }
}
