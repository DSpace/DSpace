/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.allOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for metadata PATCH operations on DSpace Objects.
 * Verifies that single and array values are applied correctly through the core item endpoint.
 */
public class DSpaceObjectMetadataPatchIT extends AbstractEntityIntegrationTest {

    private static final String AUTHOR_FIELD = "dc.contributor.author";
    private static final String AUTHOR_PATH = "/metadata/" + AUTHOR_FIELD;

    private Collection collection;
    private Item item;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .build();

        context.restoreAuthSystemState();
    }

    /**
     * Verifies that adding multiple metadata values without an index inserts them at the front in order.
     */
    @Test
    public void addMultipleMetadataValuesArrayWithoutIndexAddsAtFrontInOrderTest() throws Exception {
        initItemWithAuthors("Author One");

        List<Operation> operations = List.of(
            new AddOperation(AUTHOR_PATH, metadataValues("Author Two", "Author Three"))
        );

        performPatch(operations);

        assertAuthorOrder("Author Two", "Author Three", "Author One");
    }

    /**
     * Verifies that adding multiple metadata values at index 0 inserts all values at the front.
     */
    @Test
    public void addMultipleMetadataValuesArrayAtFrontTest() throws Exception {
        initItemWithAuthors("Author Three");

        List<Operation> operations = List.of(
            new AddOperation(AUTHOR_PATH + "/0", metadataValues("Author One", "Author Two"))
        );

        performPatch(operations);

        assertAuthorOrder("Author Two", "Author One", "Author Three");
    }

    /**
     * Verifies that adding multiple metadata values at the append index adds all values at the end in order.
     */
    @Test
    public void addMultipleMetadataValuesArrayAtBackKeepsOrderTest() throws Exception {
        initItemWithAuthors("Author One");

        List<Operation> operations = List.of(
            new AddOperation(AUTHOR_PATH + "/-", metadataValues("Author Two", "Author Three"))
        );

        performPatch(operations);

        assertAuthorOrder("Author One", "Author Two", "Author Three");
    }

    /**
     * Verifies that adding a single metadata value at index 0 adds it at the front.
     */
    @Test
    public void addSingleMetadataValueAtFrontTest() throws Exception {
        initItemWithAuthors("Author Two");

        List<Operation> operations = List.of(
            new AddOperation(AUTHOR_PATH + "/0", metadataValue("Author One"))
        );

        performPatch(operations);

        assertAuthorOrder("Author One", "Author Two");
    }

    /**
     * Verifies that adding a single metadata value at the append index adds it at the back.
     */
    @Test
    public void addSingleMetadataValueAtBackTest() throws Exception {
        initItemWithAuthors("Author One");

        List<Operation> operations = List.of(
            new AddOperation(AUTHOR_PATH + "/-", metadataValue("Author Two"))
        );

        performPatch(operations);

        assertAuthorOrder("Author One", "Author Two");
    }

    /**
     * Verifies that adding multiple metadata values to an item without existing values stores all values in order.
     */
    @Test
    public void addMultipleMetadataValuesArrayToEmptyFieldKeepsOrderTest() throws Exception {
        initItemWithAuthors();

        List<Operation> operations = List.of(
            new AddOperation(AUTHOR_PATH, metadataValues("Author One", "Author Two"))
        );

        performPatch(operations);

        assertAuthorOrder("Author One", "Author Two");
    }

    /**
     * Creates a test item with the provided author metadata values.
     *
     * @param authors author values to add to the item
     */
    private void initItemWithAuthors(String... authors) throws Exception {
        context.turnOffAuthorisationSystem();

        ItemBuilder itemBuilder = ItemBuilder.createItem(context, collection)
                                             .withTitle("Test Item");

        for (String author : authors) {
            itemBuilder.withAuthor(author);
        }

        item = itemBuilder.build();

        context.restoreAuthSystemState();
    }

    /**
     * Performs a metadata PATCH request against the test item.
     *
     * @param operations patch operations to apply
     */
    private void performPatch(List<Operation> operations) throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
                                     .content(getPatchContent(operations))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());
    }

    /**
     * Creates a single metadata value REST object.
     *
     * @param value metadata value
     * @return metadata value REST object
     */
    private MetadataValueRest metadataValue(String value) {
        MetadataValueRest metadataValueRest = new MetadataValueRest();
        metadataValueRest.setValue(value);
        return metadataValueRest;
    }

    /**
     * Creates metadata value REST objects for array PATCH values.
     *
     * @param values metadata values
     * @return metadata value REST objects
     */
    private List<MetadataValueRest> metadataValues(String... values) {
        List<MetadataValueRest> metadataValues = new ArrayList<>();

        for (String value : values) {
            metadataValues.add(metadataValue(value));
        }

        return metadataValues;
    }

    /**
     * Verifies the item author metadata values and their order.
     *
     * @param expectedAuthors expected author values in order
     */
    private void assertAuthorOrder(String... expectedAuthors) throws Exception {
        List<org.hamcrest.Matcher<? super Object>> matchers = new ArrayList<>();

        for (int i = 0; i < expectedAuthors.length; i++) {
            matchers.add(MetadataMatcher.matchMetadata(AUTHOR_FIELD, expectedAuthors[i], i));
        }

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.metadata", allOf(matchers)));
    }

}
