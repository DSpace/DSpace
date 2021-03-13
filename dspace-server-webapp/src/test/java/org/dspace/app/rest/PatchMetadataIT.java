/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Created by kristof on 20/02/2020
 */
public class PatchMetadataIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    private Collection collection;
    private WorkspaceItem publicationItem;
    private Item personItem1;
    private Item personItem2;
    private RelationshipType publicationPersonRelationshipType;

    private List<String> authorsOriginalOrder;

    private AtomicReference<Integer> idRef1;
    private AtomicReference<Integer> idRef2;

    private String addedAuthor;
    private String replacedAuthor;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent community")
                .build();
        collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .build();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();
        cleanupPersonRelations();
    }

    /**
     * A method to create a workspace publication containing 5 authors: 3 regular authors and 2 related Person items.
     * The authors are added in a specific order:
     * - "Whyte, William": Regular author
     * - "Dahlen, Sarah": Related Person
     * - "Peterson, Karrie": Regular author
     * - "Perotti, Enrico": Regular author
     * - "Linton, Oliver": Related Person
     */
    private void initPersonPublicationWorkspace() throws Exception {
        // Setup the original order of authors
        authorsOriginalOrder = new ArrayList<>();
        authorsOriginalOrder.add("Whyte, William");
        // Second one will be virtual metadata
        authorsOriginalOrder.add("Dahlen, Sarah");
        authorsOriginalOrder.add("Peterson, Karrie");
        authorsOriginalOrder.add("Perotti, Enrico");
        // 5th one will be virtual metadata
        authorsOriginalOrder.add("Linton, Oliver");

        addedAuthor = "Semple, Robert";
        replacedAuthor = "New Value";

        context.turnOffAuthorisationSystem();

        personItem1 = ItemBuilder.createItem(context, collection)
                .withTitle("Person 1")
                .withPersonIdentifierFirstName("Sarah")
                .withPersonIdentifierLastName("Dahlen")
                .withRelationshipType("Person")
                .build();
        personItem2 = ItemBuilder.createItem(context, collection)
                .withTitle("Person 2")
                .withPersonIdentifierFirstName("Oliver")
                .withPersonIdentifierLastName("Linton")
                .withRelationshipType("Person")
                .build();
        publicationItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Publication 1")
                .withRelationshipType("Publication")
                .build();
        publicationPersonRelationshipType = relationshipTypeService.findbyTypesAndTypeName(context,
                entityTypeService.findByEntityType(context, "Publication"),
                entityTypeService.findByEntityType(context, "Person"),
                "isAuthorOfPublication",
                "isPublicationOfAuthor");

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Make sure we grab the latest instance of the Item from the database before adding a regular author
        WorkspaceItem publication = workspaceItemService.find(context, publicationItem.getID());
        itemService.addMetadata(context, publication.getItem(),
                "dc", "contributor", "author", Item.ANY, authorsOriginalOrder.get(0));
        workspaceItemService.update(context, publication);

        context.restoreAuthSystemState();

        // Create a relationship between publication and person 1
        idRef1 = new AtomicReference<>();
        getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType", publicationPersonRelationshipType.getID().toString())
                .contentType(MediaType.parseMediaType
                        (org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/core/items/" + publicationItem.getItem().getID() + "\n" +
                                "https://localhost:8080/server/api/core/items/" + personItem1.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));
        context.turnOffAuthorisationSystem();

        // Add two more regular authors
        List<String> regularMetadata = new ArrayList<>();
        publication = workspaceItemService.find(context, publicationItem.getID());
        regularMetadata.add(authorsOriginalOrder.get(2));
        regularMetadata.add(authorsOriginalOrder.get(3));
        itemService.addMetadata(context, publication.getItem(),
                "dc", "contributor", "author", null, regularMetadata);
        workspaceItemService.update(context, publication);

        context.restoreAuthSystemState();

        // Create a relationship between publication and person 2
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType", publicationPersonRelationshipType.getID().toString())
                .contentType(MediaType.parseMediaType
                        (org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/core/items/" + publicationItem.getItem().getID() + "\n" +
                                "https://localhost:8080/server/api/core/items/" + personItem2.getID()))
                .andExpect(status().isCreated())
                             .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        publication = workspaceItemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
                itemService.getMetadata(publication.getItem(), "dc", "contributor", "author", Item.ANY);
        assertEquals(publicationAuthorList.size(), 5);
        assertThat(publicationAuthorList.get(0).getValue(), equalTo(authorsOriginalOrder.get(0)));
        assertThat(publicationAuthorList.get(0).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(1).getValue(), equalTo(authorsOriginalOrder.get(1)));
        assertThat(publicationAuthorList.get(1).getAuthority(), startsWith("virtual::"));
        assertThat(publicationAuthorList.get(2).getValue(), equalTo(authorsOriginalOrder.get(2)));
        assertThat(publicationAuthorList.get(2).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(3).getValue(), equalTo(authorsOriginalOrder.get(3)));
        assertThat(publicationAuthorList.get(3).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(4).getValue(), equalTo(authorsOriginalOrder.get(4)));
        assertThat(publicationAuthorList.get(4).getAuthority(), startsWith("virtual::"));
    }

    /**
     * Clean up created Person Relationshipts
     * @throws IOException
     * @throws SQLException
     */
    private void cleanupPersonRelations() throws IOException, SQLException {
        if (idRef1 != null) {
            RelationshipBuilder.deleteRelationship(idRef1.get());
            idRef1 = null;
        }
        if (idRef2 != null) {
            RelationshipBuilder.deleteRelationship(idRef2.get());
            idRef2 = null;
        }
    }

    /**
     * A method to create a workspace publication containing 5 authors: 3 regular authors and 2 related Person items.
     * The authors are added in a specific order:
     * - "Whyte, William": Regular author
     * - "Dahlen, Sarah": Regular Person
     * - "Peterson, Karrie": Regular author
     * - "Perotti, Enrico": Regular author
     * - "Linton, Oliver": Regular Person
     */
    private void initPlainTextPublicationWorkspace() throws Exception {
        authorsOriginalOrder = new ArrayList<>();
        authorsOriginalOrder.add("Whyte, William");
        authorsOriginalOrder.add("Dahlen, Sarah");
        authorsOriginalOrder.add("Peterson, Karrie");
        authorsOriginalOrder.add("Perotti, Enrico");
        authorsOriginalOrder.add("Linton, Oliver");

        addedAuthor = "Semple, Robert";
        replacedAuthor = "New Value";

        context.turnOffAuthorisationSystem();

        publicationItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                              .withTitle("Publication 1")
                                              .withRelationshipType("Publication")
                                              .build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Make sure we grab the latest instance of the Item from the database before adding a regular author
        WorkspaceItem publication = workspaceItemService.find(context, publicationItem.getID());
        itemService.addMetadata(context, publication.getItem(),
                                "dc", "contributor", "author", Item.ANY, authorsOriginalOrder);
        workspaceItemService.update(context, publication);

        context.restoreAuthSystemState();

        publication = workspaceItemService.find(context, publicationItem.getID());
        List<MetadataValue> publicationAuthorList =
                itemService.getMetadata(publication.getItem(), "dc", "contributor", "author", Item.ANY);
        assertEquals(publicationAuthorList.size(), 5);
        assertThat(publicationAuthorList.get(0).getValue(), equalTo(authorsOriginalOrder.get(0)));
        assertThat(publicationAuthorList.get(0).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(1).getValue(), equalTo(authorsOriginalOrder.get(1)));
        assertThat(publicationAuthorList.get(1).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(2).getValue(), equalTo(authorsOriginalOrder.get(2)));
        assertThat(publicationAuthorList.get(2).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(3).getValue(), equalTo(authorsOriginalOrder.get(3)));
        assertThat(publicationAuthorList.get(3).getAuthority(), not(startsWith("virtual::")));
        assertThat(publicationAuthorList.get(4).getValue(), equalTo(authorsOriginalOrder.get(4)));
        assertThat(publicationAuthorList.get(4).getAuthority(), not(startsWith("virtual::")));
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 1 to 0 using a PATCH request and verify the order of the authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 1,0,2,3,4
     */
    @Test
    public void moveTraditionalPageOneAuthorOneToZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(1, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 2 to 0 using a PATCH request and verify the order of the authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 2,0,1,3,4
     */
    @Test
    public void moveTraditionalPageOneAuthorTwoToZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(2, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 3 to 0 using a PATCH request and verify the order of the authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 3,0,1,2,4
     */
    @Test
    public void moveTraditionalPageOneAuthorThreeToZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(3, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 4 to 0 using a PATCH request and verify the order of the authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 4,0,1,2,3
     */
    @Test
    public void moveTraditionalPageOneAuthorFourToZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(4));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));

        moveTraditionalPageOneAuthorTest(4, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 1 to 3 using a PATCH request and verify the order of the authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,3,1,4
     */
    @Test
    public void moveTraditionalPageOneAuthorOneToThreeTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(1, 3, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 1 to 4 using a PATCH request and verify the order of the authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,3,4,1
     */
    @Test
    public void moveTraditionalPageOneAuthorOneToFourTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));
        expectedOrder.add(authorsOriginalOrder.get(1));

        moveTraditionalPageOneAuthorTest(1, 4, expectedOrder);
    }

    /**
     * This test will replace an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the order and value of the authors within the section.
     * @throws Exception
     */
    @Test
    public void replaceTraditionalPageOneAuthorZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(replacedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        replaceTraditionalPageOneAuthorTest(0, expectedOrder);
    }

    /**
     * This test will replace an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 2 using a PATCH request and verify the order and value of the authors within the section.
     * @throws Exception
     */
    @Test
    public void replaceTraditionalPageOneAuthorTwoTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(replacedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        replaceTraditionalPageOneAuthorTest(2, expectedOrder);
    }

    /**
     * This test will replace an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 3 using a PATCH request and verify the order and value of the authors within the section.
     * @throws Exception
     */
    @Test
    public void replaceTraditionalPageOneAuthorThreeTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(replacedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(4));

        replaceTraditionalPageOneAuthorTest(3, expectedOrder);
    }


    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: +,0,1,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlaceZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("0", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 1 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,+,1,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlaceOneTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("1", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 2 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,+,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlaceTwoTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("2", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 3 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,+,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlaceThreeTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("3", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 4 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,3,+,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlaceFourTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("4", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: +,0,1,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOneLastPlaceTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));
        expectedOrder.add(addedAuthor);

        addTraditionalPageOneAuthorTest("-", expectedOrder);

    }

    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the order of the remaining authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 1,2,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPageFromPlaceZeroTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        removeTraditionalPageOneAuthorTest(0, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 1 using a PATCH request and verify the order of the remaining authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPageFromPlaceOneTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        // The author at the first place is linked through a relationship and cannot be deleted through a PATCH request
        removeTraditionalPageOneAuthorTest(1, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 2 using a PATCH request and verify the order of the remaining authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPageFromPlaceTwoTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        removeTraditionalPageOneAuthorTest(2, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 3 using a PATCH request and verify the order of the remaining authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,4
     */
    @Test
    public void removeAuthorOnTraditionalPageFromPlaceThreeTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(4));

        removeTraditionalPageOneAuthorTest(3, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 4 using a PATCH request and verify the order of the remaining authors within the section.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPageFromPlaceFourTest() throws Exception {
        initPersonPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        // The author at the fourth place is linked through a relationship and cannot be deleted through a PATCH request
        removeTraditionalPageOneAuthorTest(4, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 1 to 0 using a PATCH request and verify the order of the authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 1,0,2,3,4
     */
    @Test
    public void moveTraditionalPageOnePlainTextAuthorOneToZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(1, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 2 to 0 using a PATCH request and verify the order of the authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 2,0,1,3,4
     */
    @Test
    public void moveTraditionalPageOnePlainTextAuthorTwoToZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(2, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 3 to 0 using a PATCH request and verify the order of the authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 3,0,1,2,4
     */
    @Test
    public void moveTraditionalPageOnePlainTextAuthorThreeToZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(3, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 4 to 0 using a PATCH request and verify the order of the authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 4,0,1,2,3
     */
    @Test
    public void moveTraditionalPageOnePlainTextAuthorFourToZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(4));
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));

        moveTraditionalPageOneAuthorTest(4, 0, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 1 to 3 using a PATCH request and verify the order of the authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,3,1,4
     */
    @Test
    public void moveTraditionalPageOnePlainTextAuthorOneToThreeTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(4));

        moveTraditionalPageOneAuthorTest(1, 3, expectedOrder);
    }

    /**
     * This test will move an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position 1 to 4 using a PATCH request and verify the order of the authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,3,4,1
     */
    @Test
    public void moveTraditionalPageOnePlainTextAuthorOneToFourTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));
        expectedOrder.add(authorsOriginalOrder.get(1));

        moveTraditionalPageOneAuthorTest(1, 4, expectedOrder);
    }

    /**
     * This test will replace an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the order and value of the authors within the section.
     * This test uses only plain text authors
     * @throws Exception
     */
    @Test
    public void replaceTraditionalPagePlainTextOneAuthorZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(replacedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        replaceTraditionalPageOneAuthorTest(0, expectedOrder);
    }

    /**
     * This test will replace an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 2 using a PATCH request and verify the order and value of the authors within the section.
     * This test uses only plain text authors
     * @throws Exception
     */
    @Test
    public void replaceTraditionalPagePlainTextOneAuthorTwoTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(replacedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        replaceTraditionalPageOneAuthorTest(2, expectedOrder);
    }

    /**
     * This test will replace an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 3 using a PATCH request and verify the order and value of the authors within the section.
     * This test uses only plain text authors
     * @throws Exception
     */
    @Test
    public void replaceTraditionalPageOnePlainTextAuthorThreeTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(replacedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(4));

        replaceTraditionalPageOneAuthorTest(3, expectedOrder);
    }


    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: +,0,1,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlainTextPlaceZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("0", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 1 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,+,1,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlainTextPlaceOneTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("1", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 2 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,+,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlainTextPlaceTwoTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("2", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 3 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,+,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlainTextPlaceThreeTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("3", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 4 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,3,+,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOnePlainTextPlaceFourTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(addedAuthor);
        expectedOrder.add(authorsOriginalOrder.get(4));

        addTraditionalPageOneAuthorTest("4", expectedOrder);

    }

    /**
     * This test will add an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the place of the new author and the order of the
     * authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: +,0,1,2,3,4 (with + being the new author)
     */
    @Test
    public void addAuthorOnTraditionalPageOneLastPlainTextPlaceTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));
        expectedOrder.add(addedAuthor);

        addTraditionalPageOneAuthorTest("-", expectedOrder);

    }

    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 0 using a PATCH request and verify the order of the remaining authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 1,2,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPagePlainTextFromPlaceZeroTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        removeTraditionalPageOneAuthorTest(0, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 1 using a PATCH request and verify the order of the remaining authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPagePlainTextFromPlaceOneTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        // The author at the first place is linked through a relationship and cannot be deleted through a PATCH request
        removeTraditionalPageOneAuthorTest(1, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 2 using a PATCH request and verify the order of the remaining authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,3,4
     */
    @Test
    public void removeAuthorOnTraditionalPagePlainTextFromPlaceTwoTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(3));
        expectedOrder.add(authorsOriginalOrder.get(4));

        removeTraditionalPageOneAuthorTest(2, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 3 using a PATCH request and verify the order of the remaining authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,4
     */
    @Test
    public void removeAuthorOnTraditionalPagePlainTextFromPlaceThreeTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(4));

        removeTraditionalPageOneAuthorTest(3, expectedOrder);
    }
    /**
     * This test will remove the author (dc.contributor.author) from a workspace publication's "traditionalpageone"
     * section at position 4 using a PATCH request and verify the order of the remaining authors within the section.
     * This test uses only plain text authors
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,1,2,3
     */
    @Test
    public void removeAuthorOnTraditionalPagePlainTextFromPlaceFourTest() throws Exception {
        initPlainTextPublicationWorkspace();

        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add(authorsOriginalOrder.get(0));
        expectedOrder.add(authorsOriginalOrder.get(1));
        expectedOrder.add(authorsOriginalOrder.get(2));
        expectedOrder.add(authorsOriginalOrder.get(3));

        // The author at the fourth place is linked through a relationship and cannot be deleted through a PATCH request
        removeTraditionalPageOneAuthorTest(4, expectedOrder);
    }


    /**
     * This test will remove all authors (dc.contributor.author) that are not linked through a relationship  from a
     * workspace publication's "traditionalpageone" section using a PATCH request and verify that the only remaining
     * authors are those coming from a relationship.
     */
    @Test
    public void removeAllAuthorsOnTraditionalPageTest() throws  Exception {
        initPersonPublicationWorkspace();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation removeOperation = new RemoveOperation("/sections/traditionalpageone/dc.contributor.author");
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationItem.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.sections.traditionalpageone",
                   // The author at the first and fourth place are linked through a relationship
                   // and cannot be deleted through a PATCH request
                       Matchers.allOf(
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, authorsOriginalOrder.get(1), 0)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, authorsOriginalOrder.get(4), 1))
                   )));



    }

    /**
     * This method moves an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from position "from" to "path" using a PATCH request and verifies the order of the authors within the
     * section using an ordered list of expected author names.
     * @param from              The "from" index to use for the Move operation
     * @param path              The "path" index to use for the Move operation
     * @param expectedOrder     A list of author names sorted in the expected order
     */
    private void moveTraditionalPageOneAuthorTest(int from, int path, List<String> expectedOrder) throws Exception {
        List<Operation> ops = new ArrayList<Operation>();
        MoveOperation moveOperation = getTraditionalPageOneMoveAuthorOperation(from, path);
        ops.add(moveOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationItem.getID())
                .content(patchBody)
                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationItem.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(0), 0)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(1), 1)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(2), 2)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(3), 3)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(4), 4))
                )));
    }

    /**
     * This method replaces an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section at position "path" using a PATCH request and verifies the order of the authors within the
     * section using an ordered list of expected author names.
     * @param path              The "path" index to use for the Replace operation
     * @param expectedOrder     A list of author names sorted in the expected order
     */
    private void replaceTraditionalPageOneAuthorTest(int path, List<String> expectedOrder) throws Exception {
        List<Operation> ops = new ArrayList<Operation>();
        MetadataValueRest value = new MetadataValueRest();
        value.setValue(replacedAuthor);

        ReplaceOperation replaceOperation = new ReplaceOperation("/sections/traditionalpageone/dc.contributor.author/"
                                                                         + path, value);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationItem.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(0), 0)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(1), 1)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(2), 2)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(3), 3)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(4), 4))
                   )));
    }

    /**
     * This method adds an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section to the position "path" using a PATCH request and verifies the place of the new author and the
     * order of the previous authors within the section using an ordered list of expected author names.
     * @param path              The "path" index to use for the Add operation
     * @param expectedOrder     A list of author names sorted in the expected order
     */
    private void addTraditionalPageOneAuthorTest(String path, List<String> expectedOrder) throws Exception {
        List<Operation> ops = new ArrayList<Operation>();
        MetadataValueRest value = new MetadataValueRest();
        value.setValue(addedAuthor);
        AddOperation addOperation = new AddOperation("/sections/traditionalpageone/dc.contributor.author/" + path,
                                                     value);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationItem.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(0), 0)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(1), 1)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(2), 2)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(3), 3)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(4), 4)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(5), 5))
                   )));
    }

    /**
     * This method removes an author (dc.contributor.author) within a workspace publication's "traditionalpageone"
     * section from the position "path" using a PATCH request and verifies the order of the remaining authors
     * within the section using an ordered list of expected author names.
     * @param path              The "path" index to use for the Remove operation
     * @param expectedOrder     A list of author names sorted in the expected order
     */
    private void removeTraditionalPageOneAuthorTest(int path, List<String> expectedOrder) throws Exception {
        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation removeOperation = new RemoveOperation("/sections/traditionalpageone/dc.contributor.author/"
                                                                      + path);
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationItem.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(0), 0)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(1), 1)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(2), 2)),
                           Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(3), 3))
                   )));
    }

    /**
     * Create a move operation on a workspace item's "traditionalpageone" section for
     * metadata field "dc.contributor.author".
     * @param from  The "from" index to use for the Move operation
     * @param path  The "path" index to use for the Move operation
     */
    private MoveOperation getTraditionalPageOneMoveAuthorOperation(int from, int path) {
        return new MoveOperation("/sections/traditionalpageone/dc.contributor.author/" + path,
                "/sections/traditionalpageone/dc.contributor.author/" + from);
    }

}
