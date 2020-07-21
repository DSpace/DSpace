/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test suite to verify the related entities creation via {@link CrisConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisConsumerIT extends AbstractControllerIntegrationTest {

    private static String[] consumers;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigurationService configurationService;

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    private EPerson submitter;

    private Collection publicationCollection;

    private Community subCommunity;

    @Autowired
    private PoolTaskService poolTaskService;

    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * configure the event.dispatcher.default.consumers property to add the
     * CrisConsumer.
     */
    @BeforeClass
    public static void initCrisConsumer() {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        consumers = configService.getArrayProperty("event.dispatcher.default.consumers");
        String newConsumers = consumers.length > 0 ? String.join(",", consumers) + ",crisconsumer" : "crisconsumer";
        configService.setProperty("event.dispatcher.default.consumers", newConsumers);
        EventService eventService = EventServiceFactory.getInstance().getEventService();
        eventService.reloadConfiguration();
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.setProperty("event.dispatcher.default.consumers", consumers);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        configurationSetUp();

        submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        subCommunity = CommunityBuilder.createCommunity(context)
                .withName("Sub Community")
                .addParentCommunity(context, parentCommunity)
                .build();

        publicationCollection = createCollection("Collection of publications", "Publication", subCommunity);

        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();

    }

    @Override
    public void destroy() throws Exception {
        poolTaskService.findAll(context).forEach(this::deletePoolTask);
        super.destroy();
    }

    private void deletePoolTask(PoolTask poolTask) {
        try {
            poolTaskService.delete(context, poolTask);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private void configurationSetUp() {
        configurationService.setProperty("cris.import.submission.enabled.entity", true);
        configurationService.setProperty("cris.import.submission.strategy.uuid.dc_relation_project", false);
        configurationService.setProperty("cris.import.submission.strategy.uuid.dc_contributor_author", false);
        configurationService.setProperty("cris.import.submission.strategy.uuid.dc_contributor_editor", false);
        configurationService.setProperty("cris.import.submission.strategy.uuid.dc_relation_journal", false);
    }

    /**
     * Verify that the related entities are created when an item submission occurs.
     *
     * @throws Exception
     */
    @Test
    public void testItemSubmission() throws Exception {

        InputStream pdf = simpleArticle.getInputStream();

        WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .withAuthor("Mario Rossi")
                .withAuthorAffilitation("4Science")
                .withEditor("Mario Rossi")
                .grantLicense()
                .build();

        context.turnOffAuthorisationSystem();
        Collection personCollection = createCollection("Collection of persons", "Person", subCommunity);
        context.restoreAuthSystemState();

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
        assertThat("The related item should be archived", relatedItem.getInArchive(), is(true));

        MetadataValueRest relatedItemtitle = findSingleMetadata(relatedItem, "dc.title");
        assertThat("dc.title value of the related item should be equals to the text value of the original metadata",
                relatedItemtitle.getValue(), equalTo("Mario Rossi"));

        MetadataValueRest crisSourceId = findSingleMetadata(relatedItem, "cris.sourceId");
        assertThat("cris.sourceId value and author md5 hash should be equals", crisSourceId.getValue(),
                equalTo(generateMd5Hash("Mario Rossi")));

        MetadataValueRest relationshipType = findSingleMetadata(relatedItem, "relationship.type");
        assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Person"));

        MetadataValueRest affiliation = findSingleMetadata(relatedItem, "crisrp.dept");
        assertThat("The crisrp.dept should be 4Science", affiliation.getValue(), equalTo("4Science"));

        // verify that the authors collections is the Person collection
        CollectionRest collection = getOwnCollectionViaRestByItemId(authToken, UUID.fromString(relatedItem.getId()));
        assertThat("The collection should be the person collection",
                collection.getId(), equalTo(personCollection.getID().toString()));
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
        Item item = ItemBuilder.createItem(context, publicationCollection).build();
        createCollection("Collection of journals", "Journal", parentCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        MetadataValueRest valueToAdd = new MetadataValueRest("The Journal");
        List<Operation> operations = asList(new AddOperation("/metadata/dc.relation.journal", valueToAdd));

        getClient(authToken).perform(patch(BASE_REST_SERVER_URL + "/api/core/items/{id}", item.getID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(getPatchContent(operations)))
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
        assertThat("cris.sourceId and journal md5 hash should be equals",
                crisSourceId.getValue(), equalTo(generateMd5Hash("The Journal")));

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

        context.turnOffAuthorisationSystem();
        Collection projectCollection = createCollection("Collection of projects", "Project", parentCommunity);
        createCollection("Collection of persons", "Person", subCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        InputStream firstFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem firstWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withProject("My project")
                .withAuthor("Mario Rossi")
                .withAuthorAffilitation("4Science")
                .withFulltext("text.txt", "/local/path/text.txt", firstFullText)
                .build();

        submitItemViaRest(authToken, firstWsitem.getID());

        InputStream secondFullText = new ByteArrayInputStream("Second submission".getBytes());

        WorkspaceItem secondWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Another Submission Item")
                .withIssueDate("2017-10-18")
                .withProject("My project")
                .withAuthor("Mario Rossi")
                .withAuthorAffilitation("My Org")
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

        MetadataValueRest firstAuthor = findSingleMetadata(firstItem, "dc.contributor.author");
        String firstAuthorAuthority = firstAuthor.getAuthority();
        assertThat("The author should have the authority set", firstAuthorAuthority, notNullValue());

        MetadataValueRest secondAuthor = findSingleMetadata(secondItem, "dc.contributor.author");
        String secondAuthorAuthority = secondAuthor.getAuthority();
        assertThat("The author should have the authority set", secondAuthorAuthority, notNullValue());

        assertThat("The author authority of the two items should be the same",
                firstAuthorAuthority, equalTo(secondAuthorAuthority));

        // search the related author item
        ItemRest authorItem = getItemViaRestByID(authToken, UUID.fromString(firstAuthorAuthority));

        String affiliation = findSingleMetadata(authorItem, "crisrp.dept").getValue();
        assertThat("The crisrp.dept should be 4Science", affiliation, equalTo("4Science"));

        // verify that the project collections is the Project collection
        CollectionRest firstCol = getOwnCollectionViaRestByItemId(authToken, fromString(firstProjectAuthority));
        assertThat("The collection should be the project collection",
                firstCol.getId(), equalTo(projectCollection.getID().toString()));

        CollectionRest secondCol = getOwnCollectionViaRestByItemId(authToken, fromString(secondProjectAuthority));
        assertThat("The collection should be the project collection",
                secondCol.getId(), equalTo(projectCollection.getID().toString()));
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

        context.turnOffAuthorisationSystem();
        createCollection("Collection of projects", "Project", parentCommunity);
        createCollection("Collection of authors", "Person", subCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        InputStream firstFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem firstWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withProject("Same Name")
                .withFulltext("text.txt", "/local/path/text.txt", firstFullText)
                .build();

        submitItemViaRest(authToken, firstWsitem.getID());

        InputStream secondFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem secondWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
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
        assertThat("cris.sourceId value and author md5 hash should be equals",
                crisSourceId.getValue(), equalTo(generateMd5Hash("Same Name")));

        MetadataValueRest relationshipType = findSingleMetadata(authorItem, "relationship.type");
        assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Person"));

        ItemRest projectItem = getItemViaRestByID(authToken, UUID.fromString(projectAuthority));

        crisSourceId = findSingleMetadata(projectItem, "cris.sourceId");
        assertThat("cris.sourceId value and project md5 hash should be equals",
                crisSourceId.getValue(), equalTo(generateMd5Hash("Same Name")));

        relationshipType = findSingleMetadata(projectItem, "relationship.type");
        assertThat("The relationship.type should be Project", relationshipType.getValue(), equalTo("Project"));

    }

    /**
     * Verify no related entity is created when an item submission occurs but no
     * collection of the same entity's relationship.type is found.
     *
     * @throws Exception
     */
    @Test
    public void testItemSubmissionWhenNoCollectionsFoundForRelatedEntity() throws Exception {

        InputStream pdf = simpleArticle.getInputStream();

        WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .withAuthor("Mario Rossi")
                .grantLicense()
                .build();

        context.turnOffAuthorisationSystem();
        // create a Person collection in a separate repository branch so that it is not eligible as target
        Community unrelatedCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        createCollection("Collection of persons", "Person", unrelatedCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        submitItemViaRest(authToken, wsitem.getID());

        // verify the dc.contributor.author and dc.contributor.editor authority value
        ItemRest item = getItemViaRestByID(authToken, wsitem.getItem().getID());

        MetadataValueRest author = findSingleMetadata(item, "dc.contributor.author");
        String authorAuthority = author.getAuthority();
        assertThat("The author should not have the authority set", authorAuthority, nullValue());
        assertThat("The author should have an CF_AMBIGUOUS confidence", author.getConfidence(), equalTo(CF_UNSET));

    }

    /**
     * Verify that the related entities are created when an item submission occurs
     * and but the same entities are not reused during different items submissions
     * if the UUID strategy for the crisSourceId generation is active for that
     * metadata.
     *
     * @throws Exception
     */
    @Test
    public void testDifferentItemsSubmissionWithUUIDStrategyForCrisSourceIdGenerationActive() throws Exception {

        // activation of uuid strategy for crisSourceId generation
        configurationService.setProperty("cris.import.submission.strategy.uuid.dc_relation_project", true);

        context.turnOffAuthorisationSystem();
        createCollection("Collection of projects", "Project", parentCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        InputStream firstFullText = new ByteArrayInputStream("First submission".getBytes());

        WorkspaceItem firstWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withProject("My project")
                .withFulltext("text.txt", "/local/path/text.txt", firstFullText)
                .build();

        submitItemViaRest(authToken, firstWsitem.getID());

        InputStream secondFullText = new ByteArrayInputStream("Second submission".getBytes());

        WorkspaceItem secondWsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Another Submission Item")
                .withIssueDate("2017-10-18")
                .withProject("My project")
                .withFulltext("text-2.txt", "/local/path/text-2.txt", secondFullText)
                .build();

        submitItemViaRest(authToken, secondWsitem.getID());

        // verify the two dc.relation.project have different authority values
        ItemRest firstItem = getItemViaRestByID(authToken, firstWsitem.getItem().getID());
        ItemRest secondItem = getItemViaRestByID(authToken, secondWsitem.getItem().getID());

        MetadataValueRest firstProject = findSingleMetadata(firstItem, "dc.relation.project");
        String firstProjectAuthority = firstProject.getAuthority();
        assertThat("The project should have the authority set", firstProjectAuthority, notNullValue());

        MetadataValueRest secondProject = findSingleMetadata(secondItem, "dc.relation.project");
        String secondProjectAuthority = secondProject.getAuthority();
        assertThat("The project should have the authority set", secondProjectAuthority, notNullValue());

        assertThat("The project authority of the two items should be different uuids",
                firstProjectAuthority, not(equalTo(secondProjectAuthority)));
    }

    /**
     * Verify that the related entities are created archived/not archived when an
     * item submission occurs based on configuration.
     *
     * @throws Exception
     */
    @Test
    public void testItemSubmissionCreatesNotArchivedRelatedItem() throws Exception {

        // disable submission enabled entity but not for projects
        configurationService.setProperty("cris.import.submission.enabled.entity", false);
        configurationService.setProperty("cris.import.submission.enabled.entity.dc_relation_project", true);

        InputStream pdf = simpleArticle.getInputStream();

        WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .withAuthor("Mario Rossi")
                .withProject("Test project")
                .grantLicense()
                .build();

        context.turnOffAuthorisationSystem();
        //create a person collection with workflow group to not archive the author item that will be created
        createCollectionWithWorkflowGroup("Collection of persons", "Person", subCommunity);
        createCollection("Collection of projects", "Project", parentCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        submitItemViaRest(authToken, wsitem.getID());

        // verify the dc.contributor.author and dc.contributor.editor authority value
        ItemRest item = getItemViaRestByID(authToken, wsitem.getItem().getID());

        MetadataValueRest author = findSingleMetadata(item, "dc.contributor.author");
        String authorAuthority = author.getAuthority();
        assertThat("The author should have the authority set", authorAuthority, notNullValue());
        assertThat("The author should have an ACCEPTED confidence", author.getConfidence(), equalTo(CF_ACCEPTED));

        MetadataValueRest project = findSingleMetadata(item, "dc.relation.project");
        String projectAuthority = project.getAuthority();
        assertThat("The project should have the authority set", projectAuthority, notNullValue());
        assertThat("The editor should have an ACCEPTED confidence", project.getConfidence(), equalTo(CF_ACCEPTED));

        // search the related person item
        ItemRest authorItem = getItemViaRestByID(authToken, UUID.fromString(authorAuthority));
        assertThat("The author related item should not be archived", authorItem.getInArchive(), is(false));

        MetadataValueRest crisSourceId = findSingleMetadata(authorItem, "cris.sourceId");
        assertThat("cris.sourceId value and author md5 hash should be equals", crisSourceId.getValue(),
                equalTo(generateMd5Hash("Mario Rossi")));

        MetadataValueRest relationshipType = findSingleMetadata(authorItem, "relationship.type");
        assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Person"));

        // search the related project item
        ItemRest projectItem = getItemViaRestByID(authToken, UUID.fromString(projectAuthority));
        assertThat("The project related item should be archived", projectItem.getInArchive(), is(true));

        crisSourceId = findSingleMetadata(projectItem, "cris.sourceId");
        assertThat("cris.sourceId value and author md5 hash should be equals", crisSourceId.getValue(),
                equalTo(generateMd5Hash("Test project")));

        relationshipType = findSingleMetadata(projectItem, "relationship.type");
        assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Project"));

    }

    /**
     * Test an item submission with many authors and affiliations metadata.
     *
     * @throws Exception
     */
    @Test
    public void testItemWithManyAuthorsSubmission() throws Exception {
        InputStream pdf = simpleArticle.getInputStream();

        WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .withAuthor("Mario Rossi")
                .withAuthorAffilitation("4Science")
                .withAuthor("Luigi Rossi")
                .withAuthorAffilitation("My org")
                .grantLicense()
                .build();

        context.turnOffAuthorisationSystem();
        createCollection("Collection of persons", "Person", subCommunity);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        submitItemViaRest(authToken, wsitem.getID());

        // verify the dc.contributor.author and dc.contributor.editor authority value
        ItemRest item = getItemViaRestByID(authToken, wsitem.getItem().getID());

        List<MetadataValueRest> authors = item.getMetadata().getMap().get("dc.contributor.author");
        for (MetadataValueRest author : authors) {

            String authorAuthority = author.getAuthority();
            assertThat("The author should have the authority set", authorAuthority, notNullValue());
            assertThat("The author should have an ACCEPTED confidence", author.getConfidence(), equalTo(CF_ACCEPTED));

            // search the related person item
            ItemRest relatedItem = getItemViaRestByID(authToken, UUID.fromString(authorAuthority));
            assertThat("The related item should be archived", relatedItem.getInArchive(), is(true));

            String expectedName = author.getPlace() == 0 ? "Mario Rossi" : "Luigi Rossi";
            MetadataValueRest crisSourceId = findSingleMetadata(relatedItem, "cris.sourceId");
            assertThat("cris.sourceId value and author md5 hash should be equals", crisSourceId.getValue(),
                    equalTo(generateMd5Hash(expectedName)));

            MetadataValueRest relationshipType = findSingleMetadata(relatedItem, "relationship.type");
            assertThat("The relationship.type should be Person", relationshipType.getValue(), equalTo("Person"));

            String expectedAffiliation = author.getPlace() == 0 ? "4Science" : "My org";
            MetadataValueRest affiliation = findSingleMetadata(relatedItem, "crisrp.dept");
            assertThat("The crisrp.dept should be " + expectedAffiliation, affiliation.getValue(),
                    equalTo(expectedAffiliation));

        }
    }

    private ItemRest getItemViaRestByID(String authToken, UUID id) throws Exception {
        MvcResult result = getClient(authToken)
                .perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", id))
                .andExpect(status().isOk())
                .andReturn();
        return readResponse(result, ItemRest.class);
    }

    private CollectionRest getOwnCollectionViaRestByItemId(String authToken, UUID itemId) throws Exception {
        MvcResult result = getClient(authToken)
                .perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}/owningCollection", itemId))
                .andExpect(status().isOk())
                .andReturn();
        return readResponse(result, CollectionRest.class);
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

    private Collection createCollection(String name, String relationshipType, Community community) throws Exception {
        return CollectionBuilder.createCollection(context, community)
                .withName(name)
                .withRelationshipType(relationshipType)
                .withSubmitterGroup(submitter)
                .build();
    }

    private Collection createCollectionWithWorkflowGroup(String name, String relationshipType, Community community)
            throws Exception {
        return CollectionBuilder.createCollection(context, community)
                .withName(name)
                .withRelationshipType(relationshipType)
                .withSubmitterGroup(submitter)
                .withWorkflowGroup(1, submitter)
                .build();
    }

    private String generateMd5Hash(String value) {
        return DigestUtils.md5Hex(value.toUpperCase());
    }
}
