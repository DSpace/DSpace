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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matcher;
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

    private static final String SECTIONS_TRADITIONALPAGEONE_DC_CONTRIBUTOR_AUTHOR =
        "/sections/traditionalpageone/dc.contributor.author/%1$s";

    private static final String getPath(Object element) {
        return String.format(SECTIONS_TRADITIONALPAGEONE_DC_CONTRIBUTOR_AUTHOR, element);
    }

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private ConfigurationService configurationService;

    private Collection collection;
    private Collection collection2;
    private WorkspaceItem publicationWorkspaceItem;
    private Item publicationItem;
    private Item personItem1;
    private Item personItem2;
    private RelationshipType publicationPersonRelationshipType;

    private List<String> authorsOriginalOrder;

    private List<MetadataValue> authorsMetadataOriginalOrder;

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
                .withEntityType("Person")
                .build();
        collection2 = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .withEntityType("Publication")
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
                .build();
        personItem2 = ItemBuilder.createItem(context, collection)
                .withTitle("Person 2")
                .withPersonIdentifierFirstName("Oliver")
                .withPersonIdentifierLastName("Linton")
                .build();
        publicationWorkspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection2)
                                                       .withTitle("Publication 1")
                                                       .withEntityType("Publication")
                                                       .build();
        publicationPersonRelationshipType = relationshipTypeService.findbyTypesAndTypeName(context,
                entityTypeService.findByEntityType(context, "Publication"),
                entityTypeService.findByEntityType(context, "Person"),
                "isAuthorOfPublication",
                "isPublicationOfAuthor");

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Make sure we grab the latest instance of the Item from the database before adding a regular author
        WorkspaceItem publication = workspaceItemService.find(context, publicationWorkspaceItem.getID());
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
                .content("https://localhost:8080/server/api/core/items/" + publicationWorkspaceItem.getItem().getID() + "\n" +
                                "https://localhost:8080/server/api/core/items/" + personItem1.getID()))
                .andExpect(status().isCreated())
                .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));
        context.turnOffAuthorisationSystem();

        // Add two more regular authors
        List<String> regularMetadata = new ArrayList<>();
        publication = workspaceItemService.find(context, publicationWorkspaceItem.getID());
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
                .content("https://localhost:8080/server/api/core/items/" + publicationWorkspaceItem.getItem().getID() + "\n" +
                                "https://localhost:8080/server/api/core/items/" + personItem2.getID()))
                .andExpect(status().isCreated())
                             .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        publication = workspaceItemService.find(context, publicationWorkspaceItem.getID());
        authorsMetadataOriginalOrder =
                itemService.getMetadata(publication.getItem(), "dc", "contributor", "author", Item.ANY);
        assertEquals(authorsMetadataOriginalOrder.size(), 5);
        assertThat(authorsMetadataOriginalOrder.get(0).getValue(), equalTo(authorsOriginalOrder.get(0)));
        assertThat(authorsMetadataOriginalOrder.get(0).getAuthority(), not(startsWith("virtual::")));
        assertThat(authorsMetadataOriginalOrder.get(1).getValue(), equalTo(authorsOriginalOrder.get(1)));
        assertThat(authorsMetadataOriginalOrder.get(1).getAuthority(), startsWith("virtual::"));
        assertThat(authorsMetadataOriginalOrder.get(2).getValue(), equalTo(authorsOriginalOrder.get(2)));
        assertThat(authorsMetadataOriginalOrder.get(2).getAuthority(), not(startsWith("virtual::")));
        assertThat(authorsMetadataOriginalOrder.get(3).getValue(), equalTo(authorsOriginalOrder.get(3)));
        assertThat(authorsMetadataOriginalOrder.get(3).getAuthority(), not(startsWith("virtual::")));
        assertThat(authorsMetadataOriginalOrder.get(4).getValue(), equalTo(authorsOriginalOrder.get(4)));
        assertThat(authorsMetadataOriginalOrder.get(4).getAuthority(), startsWith("virtual::"));
    }

    /**
     * A method to create a simple Item with 5 authors
     */
    private void initSimplePublicationItem() throws Exception {
        // Setup the original order of authors
        authorsOriginalOrder = new ArrayList<>();
        authorsOriginalOrder.add("Whyte, William");
        authorsOriginalOrder.add("Dahlen, Sarah");
        authorsOriginalOrder.add("Peterson, Karrie");
        authorsOriginalOrder.add("Perotti, Enrico");
        authorsOriginalOrder.add("Linton, Oliver");
        authorsOriginalOrder.add("bla, Oliver");

        context.turnOffAuthorisationSystem();

        publicationItem = ItemBuilder.createItem(context, collection)
                                     .withTitle("Publication 1")
                                     .build();

        for (String author : authorsOriginalOrder) {
            itemService.addMetadata(
                context, publicationItem, "dc", "contributor", "author", null, author
            );
        }

        context.restoreAuthSystemState();
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
     * A method to create a publication Item containing 5 authors: 3 regular authors and 2 related Person items.
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

        publicationWorkspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                       .withTitle("Publication 1")
                                                       .withEntityType("Publication")
                                                       .build();

        // Make sure we grab the latest instance of the Item from the database before adding a regular author
        WorkspaceItem publication = workspaceItemService.find(context, publicationWorkspaceItem.getID());
        itemService.addMetadata(context, publication.getItem(),
                                "dc", "contributor", "author", Item.ANY, authorsOriginalOrder);
        workspaceItemService.update(context, publication);

        context.restoreAuthSystemState();

        publication = workspaceItemService.find(context, publicationWorkspaceItem.getID());
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

    @Test
    public void replaceMultipleTraditionalPageOnePlainTextAuthorTest() throws Exception {
        final boolean virtualMetadataEnabled =
            configurationService.getBooleanProperty("item.enable-virtual-metadata", false);

        configurationService.setProperty("item.enable-virtual-metadata", false);
        try {
            initPlainTextPublicationWorkspace();

            Map<Integer, String> replacedAuthors =
                Map.of(
                    0, authorsOriginalOrder.get(4),
                    1, authorsOriginalOrder.get(1),
                    2, authorsOriginalOrder.get(2),
                    3, authorsOriginalOrder.get(3),
                    4, authorsOriginalOrder.get(0)
                );

            List<String> expectedOrder =
                List.of(
                    authorsOriginalOrder.get(4),
                    authorsOriginalOrder.get(1),
                    authorsOriginalOrder.get(2),
                    authorsOriginalOrder.get(3),
                    authorsOriginalOrder.get(0)
                );

            replaceTraditionalPageMultipleAuthorsTest(replacedAuthors, expectedOrder);
        } catch (Exception e) {
            throw e;
        } finally {
            configurationService.setProperty("item.enable-virtual-metadata", virtualMetadataEnabled);
        }
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

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID()))
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
     * This test will overwrite all authors (dc.contributor.author) of a workspace publication's "traditionalpageone"
     * section using a PATCH add with the entire array values.
     * It makes sure that virtual values are correctly reordered or deleted.
     */
    @Test
    public void patchAddAllAuthorsOnTraditionalPageTest() throws  Exception {

        // "Whyte, William"
        // "Dahlen, Sarah" (virtual)
        // "Peterson, Karrie"
        // "Perotti, Enrico"
        // "Linton, Oliver" (virtual)
        initPersonPublicationWorkspace();

        List<MetadataValue> expectedValues = new ArrayList<MetadataValue>();
        expectedValues.add(this.authorsMetadataOriginalOrder.get(2)); // "Peterson, Karrie"
        expectedValues.add(this.authorsMetadataOriginalOrder.get(4)); // "Linton, Oliver" (virtual)
        expectedValues.add(this.authorsMetadataOriginalOrder.get(0)); // "Whyte, William"
        patchAddEntireArray(expectedValues);

    }

    /**
     * This test will overwrite all authors (dc.contributor.author) of a workspace publication's "traditionalpageone"
     * section using a PATCH add with an array composed by only a not existent virtual metadata.
     */
    @Test
    public void patchAddAllAuthorsOnTraditionalPageNotExistentRelationTest() throws  Exception {

        initPersonPublicationWorkspace();

        List<Operation> ops = new ArrayList<Operation>();
        List<MetadataValueRest> value = new ArrayList<MetadataValueRest>();

        MetadataValueRest mrv = new MetadataValueRest();
        value.add(mrv);
        mrv.setValue("Dumbar, John");
        mrv.setAuthority("virtual::" + Integer.MAX_VALUE);

        AddOperation add = new AddOperation("/sections/traditionalpageone/dc.contributor.author", value);
        ops.add(add);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnprocessableEntity());

    }

    /**
     * This test will move an Item's dc.contributor.author value from position 1 to 2 using a PATCH request with
     * a single move operation.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,1,3,4
     */
    @Test
    public void moveMetadataAuthorOneToTwoTest() throws Exception {
        initSimplePublicationItem();

        List<String> expectedOrder = List.of(
            authorsOriginalOrder.get(0),
            authorsOriginalOrder.get(2),
            authorsOriginalOrder.get(1),
            authorsOriginalOrder.get(3),
            authorsOriginalOrder.get(4)
        );
        List<Operation> moves = List.of(
            getMetadataMoveAuthorOperation(1, 2)
        );

        moveMetadataAuthorTest(moves, expectedOrder);
    }

    /**
     * This test will move an Item's dc.contributor.author value from position 2 to 1 using a PATCH request with
     * a single move operation.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,1,3,4
     */
    @Test
    public void moveMetadataAuthorTwoToOneTest() throws Exception {
        initSimplePublicationItem();

        List<String> expectedOrder = List.of(
            authorsOriginalOrder.get(0),
            authorsOriginalOrder.get(2),
            authorsOriginalOrder.get(1),
            authorsOriginalOrder.get(3),
            authorsOriginalOrder.get(4)
        );
        List<Operation> moves = List.of(
            getMetadataMoveAuthorOperation(2, 1)
        );

        moveMetadataAuthorTest(moves, expectedOrder);
    }

    /**
     * This test will move an Item's dc.contributor.author value from position 1 to 4 using a PATCH request with
     * a single move operation.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,2,3,4,1
     */
    @Test
    public void moveMetadataAuthorOneToFourTest() throws Exception {
        initSimplePublicationItem();

        List<String> expectedOrder = List.of(
            authorsOriginalOrder.get(0),
            authorsOriginalOrder.get(2),
            authorsOriginalOrder.get(3),
            authorsOriginalOrder.get(4),
            authorsOriginalOrder.get(1)
        );
        List<Operation> moves = List.of(
            getMetadataMoveAuthorOperation(1, 4)
        );

        moveMetadataAuthorTest(moves, expectedOrder);
    }

    /**
     * This test will move an Item's dc.contributor.author value from position 4 to 1 using a PATCH request with
     * a single move operation.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,4,1,2,3
     */
    @Test
    public void moveMetadataAuthorFourToOneTest() throws Exception {
        initSimplePublicationItem();

        List<String> expectedOrder = List.of(
            authorsOriginalOrder.get(0),
            authorsOriginalOrder.get(4),
            authorsOriginalOrder.get(1),
            authorsOriginalOrder.get(2),
            authorsOriginalOrder.get(3)
        );
        List<Operation> moves = List.of(
            getMetadataMoveAuthorOperation(4, 1)
        );

        moveMetadataAuthorTest(moves, expectedOrder);
    }

    /**
     * This test will move an Item's dc.contributor.author value from position 4 to 1 using a PATCH request with
     * multiple move operations and verify the order of the authors within the section.
     * The move operations are equivalent to a regular 4 to 1 move and representative of the kind of PATCH request the
     * frontend actually sends in this kind of scenario.
     * Original Order: 0,1,2,3,4
     * Expected Order: 0,4,1,2,3
     */
    @Test
    public void moveMetadataAuthorFourToOneMultiOpTest() throws Exception {
        initSimplePublicationItem();

        List<String> expectedOrder = List.of(
            authorsOriginalOrder.get(0),
            authorsOriginalOrder.get(4),
            authorsOriginalOrder.get(1),
            authorsOriginalOrder.get(2),
            authorsOriginalOrder.get(3)
        );
        List<Operation> moves = List.of(
            getMetadataMoveAuthorOperation(1, 2),
            getMetadataMoveAuthorOperation(1, 3),
            getMetadataMoveAuthorOperation(2, 4),
            getMetadataMoveAuthorOperation(3, 1)
        );

        moveMetadataAuthorTest(moves, expectedOrder);
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

        assertReplacementOrder(expectedOrder, patchBody);
    }

    /**
     * This method rearranges an Item's dc.contributor.author values using multiple PATCH request and verifies the order
     * of the authors within the section using an ordered list of expected author names.
     * @param moves               A list of move operations
     * @param expectedOrder     A list of author names sorted in the expected order
     */
    private void moveMetadataAuthorTest(List<Operation> moves, List<String> expectedOrder) throws Exception {
        String patchBody = getPatchContent(moves);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/items/" + publicationItem.getID())
                                     .content(patchBody)
                                     .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/core/items/" + publicationItem.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.metadata", Matchers.allOf(
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
        String patchBody =
            getPatchContent(
                List.of(
                    this.mapToReplaceOperation(path, replacedAuthor)
                )
            );

        assertReplacementOrder(expectedOrder, patchBody);
    }

    private void replaceTraditionalPageMultipleAuthorsTest(
        Map<Integer, String> values, List<String> expectedOrder
    ) throws Exception {
        List<Operation> ops =
            values
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> mapToReplaceOperation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        String patchBody = getPatchContent(ops);

        assertReplacementOrder(expectedOrder, patchBody);
    }

    private ReplaceOperation mapToReplaceOperation(int path, String author) {
        return new ReplaceOperation(getPath(path), new MetadataValueRest(author));
    }

    private void assertReplacementOrder(List<String> expectedOrder, String patchBody) throws Exception, SQLException {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token)
            .perform(
                patch("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID())
                .content(patchBody)
                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON)
            )
            .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token)
            .perform(get("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(
                content().contentType(contentType)
            )
            .andExpect(
                jsonPath(
                    "$.sections.traditionalpageone",
                    Matchers.allOf(
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(0), 0)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(1), 1)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(2), 2)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(3), 3)),
                        Matchers.is(MetadataMatcher.matchMetadata(authorField, expectedOrder.get(4), 4))
                    )
                )
            );
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
        AddOperation addOperation = new AddOperation(getPath(path), value);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID()))
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
        RemoveOperation removeOperation = new RemoveOperation(getPath(path));
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        String authorField = "dc.contributor.author";
        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID()))
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
     * This method set the entire authors list (dc.contributor.author) within a workspace
     * publication's "traditionalpageone" section
     * @param metadataValues The metadata list of all the metadata values
     */
    private void patchAddEntireArray(List<MetadataValue> metadataValues) throws Exception {
        List<Operation> ops = new ArrayList<Operation>();
        List<MetadataValueRest> value = new ArrayList<MetadataValueRest>();

        // generates the MetadataValueRest list
        metadataValues.stream().forEach(mv -> {
            MetadataValueRest mrv = new MetadataValueRest();
            value.add(mrv);
            mrv.setValue(mv.getValue());
            if (mv.getAuthority() != null && mv.getAuthority().startsWith("virtual::")) {
                mrv.setAuthority(mv.getAuthority());
                mrv.setConfidence(mv.getConfidence());
            }
        });

        AddOperation add = new AddOperation("/sections/traditionalpageone/dc.contributor.author", value);
        ops.add(add);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID())
                                         .content(patchBody)
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        final String authorField = "dc.contributor.author";
        final List<Matcher<? super Object>> matchers = new ArrayList<>();
        IntStream.range(0, metadataValues.size()).forEach((i) -> {
            matchers.add(Matchers.is(MetadataMatcher.matchMetadata(authorField, metadataValues.get(i).getValue(), i)));
        });


        getClient(token).perform(get("/api/submission/workspaceitems/" + publicationWorkspaceItem.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(matchers)));
    }

    /**
     * Create a move operation on a workspace item's "traditionalpageone" section for
     * metadata field "dc.contributor.author".
     * @param from  The "from" index to use for the Move operation
     * @param path  The "path" index to use for the Move operation
     */
    private MoveOperation getTraditionalPageOneMoveAuthorOperation(int from, int path) {
        return new MoveOperation(
            getPath(path),
            getPath(from)
        );
    }

    /**
     * Create a move operation on an Item's metadata field "dc.contributor.author".
     * @param from  The "from" index to use for the Move operation
     * @param path  The "path" index to use for the Move operation
     */
    private MoveOperation getMetadataMoveAuthorOperation(int from, int path) {
        return new MoveOperation("/metadata/dc.contributor.author/" + path,
                                 "/metadata/dc.contributor.author/" + from);
    }

}
