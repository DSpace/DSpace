/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.dspace.content.Item.ANY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.junit.After;
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

    @Autowired
    private EPersonService ePersonService;

    private Item leftItem;
    private Item rightItem;
    private Collection collection;
    private Collection collection2;
    private Collection collection3;
    private Collection collection4;
    private Collection collection5;
    private RelationshipType relationshipType;
    private Relationship relationship;
    private String adminAuthToken;
    private EPerson collectionAdmin;
    private Item personItem;
    private Item projectItem;
    private Item publicationItem;
    private RelationshipType personProjectRelationshipType;
    private RelationshipType publicationPersonRelationshipType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        adminAuthToken = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();

        collectionAdmin = ePersonService.findByEmail(context, "collectionAdminTest@email.com");
        if (collectionAdmin == null) {
            // This EPerson creation should only happen once (i.e. for first test run)
            collectionAdmin = ePersonService.create(context);
            collectionAdmin.setFirstName(context, "first");
            collectionAdmin.setLastName(context, "last");
            collectionAdmin.setEmail("collectionAdminTest@email.com");
            collectionAdmin.setCanLogIn(true);
            collectionAdmin.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
            ePersonService.setPassword(collectionAdmin, password);
            // actually save the eperson to unit testing DB
            ePersonService.update(context, collectionAdmin);
        }

        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();
        collection = CollectionBuilder.createCollection(context, community)
            .withName("Collection")
            .withAdminGroup(collectionAdmin)
            .withEntityType("Publication")
            .build();

        collection2 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 2")
                .withAdminGroup(collectionAdmin)
                .withEntityType("Person")
                .build();

        collection3 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 3")
                .withAdminGroup(collectionAdmin)
                .withEntityType("JournalIssue")
                .build();

        collection4 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 4")
                .withAdminGroup(collectionAdmin)
                .withEntityType("JournalVolume")
                .build();

        collection5 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 4")
                .withAdminGroup(collectionAdmin)
                .withEntityType("Project")
                .build();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            collectionAdmin = ePersonService.findByEmail(context, "collectionAdminTest@email.com");
            if (collectionAdmin != null) {
                ePersonService.delete(context, collectionAdmin);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        super.destroy();
    }

    private void initPublicationAuthor() throws Exception {
        context.turnOffAuthorisationSystem();

        leftItem = ItemBuilder.createItem(context, collection)
            .withTitle("Left item")
            .build();
        rightItem = ItemBuilder.createItem(context, collection2)
            .withTitle("Right item")
            .withPersonIdentifierFirstName("firstName")
            .withPersonIdentifierLastName("familyName")
            .build();
        relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                entityTypeService.findByEntityType(context, "Person"),
                "isAuthorOfPublication", "isPublicationOfAuthor");
        relationship = RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, relationshipType)
            .withLeftPlace(0)
            .build();

        context.restoreAuthSystemState();
    }

    private void initJournalVolumeIssue() throws Exception {
        context.turnOffAuthorisationSystem();

        leftItem = ItemBuilder.createItem(context, collection3)
            .withTitle("Left item")
            .withPublicationIssueNumber("2")
            .build();
        rightItem = ItemBuilder.createItem(context, collection4)
            .withTitle("Right item")
            .withPublicationVolumeNumber("30")
            .build();
        relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "JournalIssue"),
                entityTypeService.findByEntityType(context, "JournalVolume"),
                "isJournalVolumeOfIssue", "isIssueOfJournalVolume");
        relationship = RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, relationshipType)
            .withLeftPlace(0)
            .build();

        context.restoreAuthSystemState();
    }

    private void initPersonProjectPublication() throws Exception {
        context.turnOffAuthorisationSystem();
        personItem = ItemBuilder.createItem(context, collection2)
            .withTitle("Person 1")
            .withPersonIdentifierFirstName("Donald")
            .withPersonIdentifierLastName("Smith")
            .build();
        projectItem = ItemBuilder.createItem(context, collection5)
            .withTitle("Project 1")
            .build();
        publicationItem = ItemBuilder.createItem(context, collection)
            .withTitle("Publication 1")
            .build();
        personProjectRelationshipType = relationshipTypeService.findbyTypesAndTypeName(context,
            entityTypeService.findByEntityType(context, "Person"),
            entityTypeService.findByEntityType(context, "Project"),
            "isProjectOfPerson",
            "isPersonOfProject");
        publicationPersonRelationshipType = relationshipTypeService.findbyTypesAndTypeName(context,
            entityTypeService.findByEntityType(context, "Publication"),
            entityTypeService.findByEntityType(context, "Person"),
            "isAuthorOfPublication",
            "isPublicationOfAuthor");
        RelationshipBuilder
            .createRelationshipBuilder(context, personItem, projectItem, personProjectRelationshipType)
            .withLeftPlace(0)
            .build();
        RelationshipBuilder
            .createRelationshipBuilder(context, publicationItem, personItem, publicationPersonRelationshipType)
            .withLeftPlace(0)
            .build();
        context.restoreAuthSystemState();

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(publicationAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(2));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(projectAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(2));
    }

    @Test
    public void testDeleteAuthorRelationshipCopyToLeftItem() throws Exception {
        initPublicationAuthor();

        // Verify the dc.contributor.author virtual metadata of the left item
        assertEquals(
                1,
                itemService
                        .getMetadata(leftItem, "dc", "contributor", "author", Item.ANY)
                        .size()
        );

        // Verify there's no dc.contributor.author actual metadata on the left item
        assertEquals(
                0,
                leftItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's no relation.isAuthorOfPublication actual metadata on the left item
        assertEquals(
                0,
                leftItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "isAuthorOfPublication"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=left"))
            .andExpect(status().isNoContent());

        // Check left item to ensure that the metadata is copied
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
        assertThat(relationshipMetadataList.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the left item
        assertEquals(
                1,
                leftItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> metadataValue.getMetadataField().getQualifier() != null &&
                                metadataValue.getMetadataField().getQualifier().equals("author"))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isAuthorOfPublication actual metadata on the left item
        assertEquals(
                1,
                leftItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "isAuthorOfPublication"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        // Check right item to ensure that no metadata is copied

        rightItem = context.reloadEntity(rightItem);
        relationshipMetadataList = itemService
            .getMetadata(rightItem, "relation", "isPublicationOfAuthor", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));
    }

    @Test
    public void testAuthorDeleteRelationshipCopyToRightItem() throws Exception {
        initPublicationAuthor();

        // Verify there's no dc.contributor.author actual metadata on the right item
        assertEquals(
                0,
                rightItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's no relation.isPublicationOfAuthor actual metadata on the right item
        assertEquals(
                0,
                rightItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "isPublicationOfAuthor"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=right"))
            .andExpect(status().isNoContent());

        // Check left item to ensure that the metadata hadn't been copied
        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(0));

        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(leftItem, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(0));

        // Check right item to ensure that the metadata is copied

        rightItem = itemService.find(context, rightItem.getID());
        relationshipMetadataList = itemService
            .getMetadata(rightItem, "relation", "isPublicationOfAuthor", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));

        // Verify there's relation.isPublicationOfAuthor actual metadata on the right item
        assertEquals(
                1,
                rightItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "isPublicationOfAuthor"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        // There is no additional Metadata to check on the rightItem because the configuration of the virtual
        // metadata holds no config to display virtual metadata on the author of the publication
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
        assertThat(relationshipMetadataList.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the left item
        assertEquals(
                1,
                leftItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isAuthorOfPublication actual metadata on the left item
        assertEquals(
                1,
                leftItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "isAuthorOfPublication"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        rightItem = itemService.find(context, rightItem.getID());
        relationshipMetadataList = itemService
            .getMetadata(rightItem, "relation", "isPublicationOfAuthor", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));

        // Verify there's relation.isPublicationOfAuthor actual metadata on the right item
        assertEquals(
                1,
                rightItem.getMetadata()
                        .stream()
                        .filter(metadataValue -> "isPublicationOfAuthor"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        // There is no additional Metadata to check on the rightItem because the configuration of the virtual
        // metadata holds no config to display virtual metadata on the author of the publication
        // Verify testDeleteJournalRelationshipCopyToBothItems test for an example where metadata is actually configured
        // to be displayed on both items for the relationship

    }

    @Test
    public void testDeleteJournalRelationshipCopyToLeftItem() throws Exception {
        initJournalVolumeIssue();

        getClient(adminAuthToken).perform(
            delete("/api/core/relationships/" + relationship.getID() + "?copyVirtualMetadata=left"))
            .andExpect(status().isNoContent());

        // Check the leftItem to ensure that the metadata is copied
        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        // Check the rightItem to ensure that the metadata is not copied
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

        // Check the leftItem to ensure that the metadata is not copied
        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(0));

        // Check the right item to ensure that the metadata is copied
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

        // Check the left item to ensure that the metadata is copied
        leftItem = itemService.find(context, leftItem.getID());
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(1));
        assertThat(volumeList.get(0).getValue(), equalTo("30"));

        // Check the rightItem to ensure that the metadata is copied
        rightItem = itemService.find(context, rightItem.getID());
        List<MetadataValue> issueList =
            itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));
    }

    @Test
    public void deleteItemCopyVirtualMetadataAll() throws Exception {
        initPersonProjectPublication();

        for (Item item : asList(publicationItem, projectItem)) {

            // Verify the dc.contributor.author virtual metadata
            assertEquals(
                    1,
                    itemService.getMetadata(item, "dc", "contributor", "author", ANY).size()
            );

            // Verify there's no dc.contributor.author actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                            .collect(toList()).size()
            );

            // Verify there's no relation.isAuthorOfPublication actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "isAuthorOfPublication"
                                    .equals(metadataValue.getMetadataField().getElement()))
                            .collect(toList()).size()
            );
        }

        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID() + "?copyVirtualMetadata=all"))
            .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList = itemService.getMetadata(publicationItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(publicationAuthorList.get(0).getAuthority());
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the publication item
        assertEquals(
                1,
                publicationItem.getMetadata().stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isAuthorOfPublication actual metadata on the publication item
        assertEquals(
                1,
                publicationItem.getMetadata().stream()
                        .filter(metadataValue -> "isAuthorOfPublication"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList = itemService.getMetadata(projectItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(projectAuthorList.get(0).getAuthority());
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the project item
        assertEquals(
                1,
                projectItem.getMetadata().stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isPersonOfProject actual metadata on the project item
        assertEquals(
                1,
                projectItem.getMetadata().stream()
                        .filter(metadataValue -> "isPersonOfProject"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );
    }

    @Test
    public void deleteItemCopyVirtualMetadataOneType() throws Exception {
        initPersonProjectPublication();

        for (Item item : asList(publicationItem, projectItem)) {

            // Verify the dc.contributor.author virtual metadata
            assertEquals(
                    1,
                    itemService.getMetadata(item, "dc", "contributor", "author", ANY).size()
            );

            // Verify there's no dc.contributor.author actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                            .collect(toList()).size()
            );

            // Verify there's no relation.isAuthorOfPublication actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "isAuthorOfPublication"
                                    .equals(metadataValue.getMetadataField().getElement()))
                            .collect(toList()).size()
            );
        }

        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID() + "?copyVirtualMetadata="
                + publicationPersonRelationshipType.getID()))
            .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList = itemService.getMetadata(publicationItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(publicationAuthorList.get(0).getAuthority());
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the publication item
        assertEquals(
                1,
                publicationItem.getMetadata().stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isAuthorOfPublication actual metadata on the publication item
        assertEquals(
                1,
                publicationItem.getMetadata().stream()
                        .filter(metadataValue -> "isAuthorOfPublication"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList = itemService.getMetadata(projectItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(0));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(0));
    }

    @Test
    public void deleteItemCopyVirtualMetadataTwoTypes() throws Exception {
        initPersonProjectPublication();

        for (Item item : asList(publicationItem, projectItem)) {

            // Verify the dc.contributor.author virtual metadata
            assertEquals(
                    1,
                    itemService.getMetadata(item, "dc", "contributor", "author", ANY).size()
            );

            // Verify there's no dc.contributor.author actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                            .collect(toList()).size()
            );

            // Verify there's no relation.isAuthorOfPublication actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "isAuthorOfPublication"
                                    .equals(metadataValue.getMetadataField().getElement()))
                            .collect(toList()).size()
            );
        }

        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID()
                + "?copyVirtualMetadata=" + publicationPersonRelationshipType.getID()
                + "&copyVirtualMetadata=" + personProjectRelationshipType.getID()))
            .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList = itemService.getMetadata(publicationItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(publicationAuthorList.get(0).getAuthority());
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the publication item
        assertEquals(
                1,
                publicationItem.getMetadata().stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isAuthorOfPublication actual metadata on the publication item
        assertEquals(
                1,
                publicationItem.getMetadata().stream()
                        .filter(metadataValue -> "isAuthorOfPublication"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList = itemService.getMetadata(projectItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(projectAuthorList.get(0).getAuthority());
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the project item
        assertEquals(
                1,
                projectItem.getMetadata().stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isPersonOfProject actual metadata on the project item
        assertEquals(
                1,
                projectItem.getMetadata().stream()
                        .filter(metadataValue -> "isPersonOfProject"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );
    }

    @Test
    public void deleteItemCopyVirtualMetadataNone() throws Exception {
        initPersonProjectPublication();

        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID()))
            .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList = itemService.getMetadata(publicationItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(0));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(0));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList = itemService.getMetadata(projectItem,
            "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(0));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(0));
    }

    @Test
    public void deleteItemCopyVirtualMetadataInvalid() throws Exception {
        initPersonProjectPublication();

        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID()
                + "?copyVirtualMetadata=" + publicationPersonRelationshipType.getID()
                + "&copyVirtualMetadata=" + personProjectRelationshipType.getID()
                + "&copyVirtualMetadata=SomeThingWrong"))
            .andExpect(status().isBadRequest());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(publicationAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(2));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(projectAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(2));
    }

    @Test
    public void deleteItemCopyVirtualMetadataAllNoPermissions() throws Exception {
        initPersonProjectPublication();

        for (Item item : asList(publicationItem, projectItem)) {

            // Verify the dc.contributor.author virtual metadata
            assertEquals(
                    1,
                    itemService.getMetadata(item, "dc", "contributor", "author", ANY).size()
            );

            // Verify there's no dc.contributor.author actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                            .collect(toList()).size()
            );

            // Verify there's no relation.isAuthorOfPublication actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "isAuthorOfPublication"
                                    .equals(metadataValue.getMetadataField().getElement()))
                            .collect(toList()).size()
            );
        }

        getClient(getAuthToken(eperson.getEmail(), password)).perform(
            delete("/api/core/items/" + personItem.getID()))
            .andExpect(status().isForbidden());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(publicationAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(2));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(projectAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(2));
    }

    @Test
    public void deleteItemCopyVirtualMetadataAllAsCollectionAdmin() throws Exception {
        initPersonProjectPublication();

        getClient(getAuthToken(collectionAdmin.getEmail(), password)).perform(
            delete("/api/core/items/" + personItem.getID()))
            .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(0));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(0));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(0));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(0));
    }

    @Test
    public void deleteItemCopyVirtualMetadataTypeAsCollectionAdmin() throws Exception {
        initPersonProjectPublication();

        getClient(getAuthToken(collectionAdmin.getEmail(), password)).perform(
            delete("/api/core/items/" + personItem.getID()
                + "?copyVirtualMetadata=" + publicationPersonRelationshipType.getID()))
            .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(publicationAuthorList.get(0).getAuthority(), equalTo(null));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(1));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(0));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(0));
    }

    /**
     * Create a collection admin that is unrelated to {@link #collection}.
     * @return an EPerson
     */
    protected EPerson createOtherCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson otherCollectionAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("other", "col admin")
            .withCanLogin(true)
            .withEmail("otherCollectionAdmin@email.com")
            .withPassword(password)
            .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
            .build();

        Community otherCommunity = CommunityBuilder.createCommunity(context)
            .withName("Other Community")
            .build();

        CollectionBuilder.createCollection(context, otherCommunity)
            .withName("Other Collection")
            .withAdminGroup(otherCollectionAdmin)
            .build();

        context.restoreAuthSystemState();

        return otherCollectionAdmin;
    }

    @Test
    public void deleteItemCopyVirtualMetadataAllInsufficientPermissions() throws Exception {
        initPersonProjectPublication();

        EPerson otherCollectionAdmin = createOtherCollectionAdmin();

        getClient(getAuthToken(otherCollectionAdmin.getEmail(), password)).perform(
            delete("/api/core/items/" + personItem.getID()))
            .andExpect(status().isForbidden());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(publicationAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(2));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(projectAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(2));
    }

    @Test
    public void deleteItemCopyVirtualMetadataTypeInsufficientPermissions() throws Exception {
        initPersonProjectPublication();

        EPerson otherCollectionAdmin = createOtherCollectionAdmin();

        getClient(getAuthToken(otherCollectionAdmin.getEmail(), password)).perform(
            delete("/api/core/items/" + personItem.getID()
                + "?copyVirtualMetadata=" + publicationPersonRelationshipType.getID()))
            .andExpect(status().isForbidden());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
            itemService.getMetadata(publicationItem, "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(1));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(publicationAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(2));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList =
            itemService.getMetadata(projectItem, "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertThat(projectAuthorList.get(0).getAuthority(), startsWith("virtual::"));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(2));
    }

    @Test
    public void deleteItemCopyVirtualMetadataConfigured() throws Exception {
        initPersonProjectPublication();

        for (Item item : asList(publicationItem, projectItem)) {

            // Verify the dc.contributor.author virtual metadata
            assertEquals(
                    1,
                    itemService.getMetadata(item, "dc", "contributor", "author", ANY).size()
            );

            // Verify there's no dc.contributor.author actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                            .collect(toList()).size()
            );

            // Verify there's no relation.isAuthorOfPublication actual metadata on the item
            assertEquals(
                    0,
                    item.getMetadata().stream()
                            .filter(metadataValue -> "isAuthorOfPublication"
                                    .equals(metadataValue.getMetadataField().getElement()))
                            .collect(toList()).size()
            );
        }

        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID() + "?copyVirtualMetadata=configured"))
                                 .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList = itemService.getMetadata(publicationItem,
                                                                            "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(0));

        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
            "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(0));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList = itemService.getMetadata(projectItem,
                                                                        "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(1));
        assertThat(projectAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(projectAuthorList.get(0).getAuthority());
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(1));

        // Verify there's dc.contributor.author actual metadata on the project item
        assertEquals(
                1,
                projectItem.getMetadata().stream()
                        .filter(metadataValue -> "author".equals(metadataValue.getMetadataField().getQualifier()))
                        .collect(toList())
                        .size()
        );

        // Verify there's relation.isPersonOfProject actual metadata on the project item
        assertEquals(
                1,
                projectItem.getMetadata().stream()
                        .filter(metadataValue -> "isPersonOfProject"
                                .equals(metadataValue.getMetadataField().getElement()))
                        .collect(toList())
                        .size()
        );
    }

    @Test
    public void deleteItemCopyVirtualMetadataToCorrectPlace() throws Exception {
        initPersonProjectPublication();

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publicationItem, "dc", "contributor", "author", null, "Test Author");
        itemService.update(context, publicationItem);
        context.restoreAuthSystemState();
        getClient(adminAuthToken).perform(
            delete("/api/core/items/" + personItem.getID() + "?copyVirtualMetadata="
                       + publicationPersonRelationshipType.getID()))
                                 .andExpect(status().isNoContent());

        publicationItem = itemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList = itemService.getMetadata(publicationItem,
                                                                            "dc", "contributor", "author", Item.ANY);
        assertThat(publicationAuthorList.size(), equalTo(2));
        assertThat(publicationAuthorList.get(0).getValue(), equalTo("Smith, Donald"));
        assertNull(publicationAuthorList.get(0).getAuthority());
        List<MetadataValue> publicationRelationships = itemService.getMetadata(publicationItem,
                                                "relation", "isAuthorOfPublication", Item.ANY, Item.ANY);
        assertThat(publicationRelationships.size(), equalTo(1));

        projectItem = itemService.find(context, projectItem.getID());
        List<MetadataValue> projectAuthorList = itemService.getMetadata(projectItem,
                                                                        "dc", "contributor", "author", Item.ANY);
        assertThat(projectAuthorList.size(), equalTo(0));
        List<MetadataValue> projectRelationships = itemService.getMetadata(projectItem,
                                            "relation", "isPersonOfProject", Item.ANY, Item.ANY);
        assertThat(projectRelationships.size(), equalTo(0));
    }

}
