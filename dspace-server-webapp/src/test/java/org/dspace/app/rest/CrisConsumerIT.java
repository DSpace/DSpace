/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Arrays.asList;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.converter.JsonPatchConverter;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.CrisConsumer;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test suite to verify the related entities creation via {@link CrisConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisConsumerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private EPerson submitter;

    private Collection collection;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();

    }

    /**
     * Verify that the related entities are created when an item submission occurs.
     *
     * @throws Exception
     */
    @Test
    public void testItemSubmission() throws Exception {

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .withAuthor("Mario Rossi")
                .withEditor("Mario Rossi")
                .grantLicense()
                .build();

        String authToken = getAuthToken(submitter.getEmail(), password);

        submitItemViaRest(authToken, wsitem.getID());

        // verify the dc.contributor.author and dc.contributor.editor authority value
        ItemRest item = getItemViaRestByID(authToken, wsitem.getItem().getID());

        MetadataValueRest author = findSingleMetadata(item, "dc.contributor.author");
        String authorAuthority = author.getAuthority();
        assertThat("The author should have the authority set", authorAuthority, notNullValue());
        assertThat("The author should have an ACCEPTED confidence", author.getConfidence(), equalTo(CF_ACCEPTED));

        MetadataValueRest editor = findSingleMetadata(item, "dc.contributor.editor");
        String editorAuthority = editor.getAuthority();
        assertThat("The editor should have the authority set", editorAuthority, notNullValue());
        assertThat("The editor should have an ACCEPTED confidence", editor.getConfidence(), equalTo(CF_ACCEPTED));

        assertThat("Editor and author should have the same authority", authorAuthority, equalTo(editorAuthority));

        // search the related person item
        ItemRest relatedItem = getItemViaRestByID(authToken, UUID.fromString(authorAuthority));

        MetadataValueRest crisSourceId = findSingleMetadata(relatedItem, "cris.sourceId");
        assertThat("cris.sourceId value and author should be equals", crisSourceId.getValue(), equalTo("Mario Rossi"));

        MetadataValueRest relationshipType = findSingleMetadata(relatedItem, "relationship.type");
        assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Person"));

    }

    /**
     * Verify that when an already installed item is updated with a metadata
     * addition the related entities are created.
     *
     * @throws Exception
     */
    @Test
    public void testItemMetadataModification() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        MetadataValueRest valueToAdd = new MetadataValueRest("The Journal");
        Patch patch = new Patch(asList(new AddOperation("/metadata/dc.relation.journal", valueToAdd)));

        getClient(authToken).perform(patch(BASE_REST_SERVER_URL + "/api/core/items/{id}", item.getID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJsonString(patch)))
                .andExpect(status().isOk());

        // verify the dc.relation.journal authority value
        ItemRest updatedItem = getItemViaRestByID(authToken, item.getID());

        MetadataValueRest journal = findSingleMetadata(updatedItem, "dc.relation.journal");
        String journalAuthority = journal.getAuthority();
        assertThat("The journal should have the authority set", journalAuthority, notNullValue());
        assertThat("The journal should have an ACCEPTED confidence", journal.getConfidence(), equalTo(CF_ACCEPTED));

        // search the related journal item
        ItemRest relatedItem = getItemViaRestByID(authToken, UUID.fromString(journalAuthority));

        MetadataValueRest crisSourceId = findSingleMetadata(relatedItem, "cris.sourceId");
        assertThat("cris.sourceId and journal should be equals", crisSourceId.getValue(), equalTo("The Journal"));

        MetadataValueRest relationshipType = findSingleMetadata(relatedItem, "relationship.type");
        assertThat("The relationship.type should be Journal", relationshipType.getValue(), equalTo("Journal"));

    }

    /**
     * Verify that the related entities are created when an item submission occurs
     * and that same entities can be reused during different items submissions.
     *
     * @throws Exception
     */
    @Test
    public void testDifferentItemsSubmission() throws Exception {

        String authToken = getAuthToken(submitter.getEmail(), password);

        InputStream firstFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem firstWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withProject("My project")
                .withFulltext("text.txt", "/local/path/text.txt", firstFullText)
                .build();

        submitItemViaRest(authToken, firstWsitem.getID());

        InputStream secondFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem secondWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Another Submission Item")
                .withIssueDate("2017-10-18")
                .withProject("My project")
                .withFulltext("text-2.txt", "/local/path/text-2.txt", secondFullText)
                .build();

        submitItemViaRest(authToken, secondWsitem.getID());

        // verify the two dc.relation.project have the same authority value
        ItemRest firstItem = getItemViaRestByID(authToken, firstWsitem.getItem().getID());
        ItemRest secondItem = getItemViaRestByID(authToken, secondWsitem.getItem().getID());

        MetadataValueRest firstProject = findSingleMetadata(firstItem, "dc.relation.project");
        String firstProjectAuthority = firstProject.getAuthority();
        assertThat("The project should have the authority set", firstProjectAuthority, notNullValue());

        MetadataValueRest secondProject = findSingleMetadata(secondItem, "dc.relation.project");
        String secondProjectAuthority = secondProject.getAuthority();
        assertThat("The project should have the authority set", secondProjectAuthority, notNullValue());

        assertThat("The project authority of the two items should be the same",
                firstProjectAuthority, equalTo(secondProjectAuthority));
    }

    /**
     * Verify that the related entities are created when an item submission occurs
     * and that stored entities are not reused during different items submissions if
     * the metadata value is the same but the relationship.type is different.
     *
     * @throws Exception
     */
    @Test
    public void testItemsSubmissionWithDifferentRelationshipTypeAndSameValue() throws Exception {

        String authToken = getAuthToken(submitter.getEmail(), password);

        InputStream firstFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem firstWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withProject("Same Name")
                .withFulltext("text.txt", "/local/path/text.txt", firstFullText)
                .build();

        submitItemViaRest(authToken, firstWsitem.getID());

        InputStream secondFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem secondWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Another Submission Item")
                .withIssueDate("2017-10-18")
                .withAuthor("Same Name")
                .withFulltext("text-2.txt", "/local/path/text-2.txt", secondFullText)
                .build();

        submitItemViaRest(authToken, secondWsitem.getID());

        // verify the two dc.relation.project have the different authority values
        ItemRest firstItem = getItemViaRestByID(authToken, firstWsitem.getItem().getID());
        ItemRest secondItem = getItemViaRestByID(authToken, secondWsitem.getItem().getID());

        MetadataValueRest project = findSingleMetadata(firstItem, "dc.relation.project");
        String projectAuthority = project.getAuthority();
        assertThat("The project should have the authority set", projectAuthority, notNullValue());

        MetadataValueRest author = findSingleMetadata(secondItem, "dc.contributor.author");
        String authorAuthority = author.getAuthority();
        assertThat("The author should have the authority set", authorAuthority, notNullValue());

        assertThat("The project and the author authority of the two items should be different",
                projectAuthority, not(equalTo(authorAuthority)));

        // verify the two created items metadata values (one for the author and the other for the project)
        ItemRest authorItem = getItemViaRestByID(authToken, UUID.fromString(authorAuthority));

        MetadataValueRest crisSourceId = findSingleMetadata(authorItem, "cris.sourceId");
        assertThat("cris.sourceId value and author should be equals", crisSourceId.getValue(), equalTo("Same Name"));

        MetadataValueRest relationshipType = findSingleMetadata(authorItem, "relationship.type");
        assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Person"));

        ItemRest projectItem = getItemViaRestByID(authToken, UUID.fromString(projectAuthority));

        crisSourceId = findSingleMetadata(projectItem, "cris.sourceId");
        assertThat("cris.sourceId value and project should be equals", crisSourceId.getValue(), equalTo("Same Name"));

        relationshipType = findSingleMetadata(projectItem, "relationship.type");
        assertThat("The relationship.type should be Project", relationshipType.getValue(), equalTo("Project"));

    }

    private ItemRest getItemViaRestByID(String authToken, UUID id) throws Exception {
        MvcResult result = getClient(authToken)
                .perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", id))
                .andExpect(status().isOk())
                .andReturn();
        return readResponse(result, ItemRest.class);
    }

    private void submitItemViaRest(String authToken, Integer wsId) throws Exception, SQLException {
        getClient(authToken).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                .content("/api/submission/workspaceitems/" + wsId).contentType(textUriContentType))
                .andExpect(status().isCreated());
    }

    private MetadataValueRest findSingleMetadata(ItemRest item, String metadataField) {
        List<MetadataValueRest> metadata = item.getMetadata().getMap().get(metadataField);
        assertThat("Item should have the '" + metadataField + "' metadata", metadata, notNullValue());
        assertThat("Item should have only one '" + metadataField + "' metadata", metadata, hasSize(1));
        return metadata.get(0);
    }

    private <T> T readResponse(MvcResult result, Class<T> responseClass) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), responseClass);
    }

    private String convertToJsonString(Patch patch) {
        return new JsonPatchConverter(objectMapper).convert(patch).toString();
    }
}
