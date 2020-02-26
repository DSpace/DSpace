/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringEscapeUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test suite for the WorkspaceItem endpoint
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class WorkspaceItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);
    }

    @Test
    /**
     * All the workspaceitem should be returned regardless of the collection where they were created
     *
     * @throws Exception
     */
    public void findAllTest() throws Exception {
        context.setCurrentUser(admin);

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();


        //2. Three workspace items in two different collections
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                      .withTitle("Workspace Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.workspaceitems", Matchers.containsInAnyOrder(
                        WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                "2017-10-17"),
                        WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                "2016-02-13"),
                        WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3",
                                "2016-02-13"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/submission/workspaceitems")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    /**
     * The workspaceitem endpoint must provide proper pagination
     *
     * @throws Exception
     */
    public void findAllWithPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();


        //2. Three workspace items in two different collections
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                      .withTitle("Workspace Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.containsInAnyOrder(
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                        "2017-10-17"),
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                        "2016-02-13"))))
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.not(Matchers.contains(WorkspaceItemMatcher
                                .matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3", "2016-02-13")))));

        getClient(token).perform(get("/api/submission/workspaceitems").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.contains(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3,
                                "Workspace Item 3", "2016-02-13"))))
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.not(Matchers.contains(
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                        "2017-10-17"),
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                        "2016-02-13")))))
                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    /**
     * The workspaceitem resource endpoint must expose the proper structure
     *
     * @throws Exception
     */
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                "Workspace Item 1", "2017-10-17", "ExtraEntry"))));
    }

    @Test
    /**
     * The workspaceitem resource endpoint must expose the proper structure
     *
     * @throws Exception
     */
    public void findOneRelsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID() + "/collection")
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers
                        .is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle()))
                ));

        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID() + "/item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemWithTitleAndDateIssued(witem.getItem(),
                        "Workspace Item 1", "2017-10-17"))));

        getClient(token).perform(get("/api/submission/workspaceitems/" + witem.getID() + "/submissionDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(hasJsonPath("$.id", is("traditional")))));

    }

    @Test
    /**
     * Check the response code for unexistent workspaceitem
     *
     * @throws Exception
     */
    public void findOneWrongUUIDTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Removing a workspaceitem should result in delete of all the underline resources (item and bitstreams)
     *
     * @throws Exception
     */
    public void deleteOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .build();

        Item item = witem.getItem();

        //Add a bitstream to the item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder
                    .createBitstream(context, item, is)
                    .withName("Bitstream1")
                    .withMimeType("text/plain").build();
        }

        String token = getAuthToken(admin.getEmail(), password);

        //Delete the workspaceitem
        getClient(token).perform(delete("/api/submission/workspaceitems/" + witem.getID()))
                    .andExpect(status().is(204));

        //Trying to get deleted item should fail with 404
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted workspaceitem's item should fail with 404
        getClient().perform(get("/api/core/items/" + item.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted workspaceitem's bitstream should fail with 404
        getClient().perform(get("/api/core/biststreams/" + bitstream.getID()))
                   .andExpect(status().is(404));
    }

    @Test
    /**
     * Create three workspaceitem with two different submitter and verify that the findBySubmitter return the proper
     * list of workspaceitem for each submitter also paginating
     *
     * @throws Exception
     */
    public void findBySubmitterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. create two users to use as submitters
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withEmail("submitter1@example.com")
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withEmail("submitter2@example.com")
                .build();

        // create two workspaceitems with the first submitter
        context.setCurrentUser(submitter1);


        //3. Two workspace items in two different collections
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                      .withTitle("Workspace Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        //4. A workspaceitem for the second submitter
        context.setCurrentUser(submitter2);
        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        // use our admin to retrieve all the workspace by submitter
        String token = getAuthToken(admin.getEmail(), password);

        // the first submitter has two workspace
        getClient(token).perform(get("/api/submission/workspaceitems/search/findBySubmitter")
                .param("size", "20")
                .param("uuid", submitter1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workspaceitems",
                    Matchers.containsInAnyOrder(
                            WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                    "2017-10-17"),
                            WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                    "2016-02-13"))))
            .andExpect(jsonPath("$._embedded.workspaceitems",
                    Matchers.not(Matchers.contains(WorkspaceItemMatcher
                            .matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3", "2016-02-13")))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        // the first submitter has two workspace so if we paginate with a 1-size windows the page 1 will contains the
        // second workspace
        getClient(token).perform(get("/api/submission/workspaceitems/search/findBySubmitter")
                .param("size", "1")
                .param("page", "1")
                .param("uuid", submitter1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workspaceitems",
                    Matchers.contains(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2,
                            "Workspace Item 2", "2016-02-13"))))
            .andExpect(jsonPath("$._embedded.workspaceitems",
                    Matchers.not(Matchers.contains(
                            WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                    "2017-10-17"),
                            WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3",
                                    "2016-02-13")))))
            .andExpect(jsonPath("$.page.size", is(1)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        // the second submitter has a single workspace
        getClient(token).perform(get("/api/submission/workspaceitems/search/findBySubmitter")
                .param("size", "20")
                .param("uuid", submitter2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workspaceitems",
                    Matchers.contains(
                            WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3",
                                    "2016-02-13"))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    /**
     * Test the creation of workspaceitem POSTing to the resource collection endpoint. It should respect the collection
     * param if present or use a default if it is not used
     *
     * @throws Exception
     */
    public void createEmptyWorkspateItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        // create a workspaceitem explicitly in the col1
        getClient(authToken).perform(post("/api/submission/workspaceitems")
                    .param("owningCollection", col1.getID().toString())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._embedded.collection.id", is(col1.getID().toString())));

        // create a workspaceitem explicitly in the col2
        getClient(authToken).perform(post("/api/submission/workspaceitems")
                    .param("owningCollection", col2.getID().toString())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._embedded.collection.id", is(col2.getID().toString())));

        // create a workspaceitem without an explicit collection, this will go in the first valid collection for the
        // user: the col1
        getClient(authToken).perform(post("/api/submission/workspaceitems")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._embedded.collection.id", is(col1.getID().toString())));

        // TODO cleanup the context!!!
    }

    @Test
    /**
     * Test the creation of workspaceitems POSTing to the resource collection endpoint a bibtex file
     *
     * @throws Exception
     */
    public void createMultipleWorkspaceItemFromFileTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        String authToken = getAuthToken(admin.getEmail(), password);

        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "bibtex-test.bib", "application/x-bibtex",
                bibtex);

        // bulk create workspaceitems in the default collection (col1)
        getClient(authToken).perform(fileUpload("/api/submission/workspaceitems")
                    .file(bibtexFile))
                // bulk create should return 200, 201 (created) is better for single resource
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems[0].sections.traditionalpageone['dc.title'][0].value",
                        is("My Article")))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[0]._embedded.collection.id", is(col1.getID().toString())))
                .andExpect(jsonPath("$._embedded.workspaceitems[1].sections.traditionalpageone['dc.title'][0].value",
                        is("My Article 2")))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[1]._embedded.collection.id", is(col1.getID().toString())))
                .andExpect(jsonPath("$._embedded.workspaceitems[2].sections.traditionalpageone['dc.title'][0].value",
                        is("My Article 3")))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[2]._embedded.collection.id", is(col1.getID().toString())))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[*]._embedded.upload").doesNotExist())
        ;

        // bulk create workspaceitems explicitly in the col2
        getClient(authToken).perform(fileUpload("/api/submission/workspaceitems")
                    .file(bibtexFile)
                    .param("collection", col2.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems[0].sections.traditionalpageone['dc.title'][0].value",
                        is("My Article")))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[0]._embedded.collection.id", is(col2.getID().toString())))
                .andExpect(jsonPath("$._embedded.workspaceitems[1].sections.traditionalpageone['dc.title'][0].value",
                        is("My Article 2")))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[1]._embedded.collection.id", is(col2.getID().toString())))
                .andExpect(jsonPath("$._embedded.workspaceitems[2].sections.traditionalpageone['dc.title'][0].value",
                        is("My Article 3")))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[2]._embedded.collection.id", is(col2.getID().toString())))
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[*]._embedded.upload").doesNotExist())
        ;

        bibtex.close();
    }

    @Test
    /**
     * Test the creation of a workspaceitem POSTing to the resource collection endpoint a PDF file. As a single item
     * will be created we expect to have the pdf file stored as a bitstream
     *
     * @throws Exception
     */
    public void createWorkspaceItemFromPDFFileTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        String authToken = getAuthToken(admin.getEmail(), password);

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        final MockMultipartFile pdfFile = new MockMultipartFile("file", "/local/path/myfile.pdf", "application/pdf",
                pdf);

        // bulk create a workspaceitem
        getClient(authToken).perform(fileUpload("/api/submission/workspaceitems")
                    .file(pdfFile))
                // bulk create should return 200, 201 (created) is better for single resource
                .andExpect(status().isOk())
                // testing grobid extraction
                .andExpect(jsonPath(
                      "$._embedded.workspaceitems[0].sections.traditionalpageone['dc.title'][0].value",
                is("This is a simple test file")))
                .andExpect(jsonPath(
                      "$._embedded.workspaceitems[0].sections.traditionalpageone['dc.contributor.author'][0].value",
                is("Bollini, Andrea")))
                .andExpect(jsonPath(
                      "$._embedded.workspaceitems[0].sections.traditionalpageone['dc.date.issued'][0].value",
                is("2018")))
                .andExpect(jsonPath(
                      "$._embedded.workspaceitems[0].sections.traditionalpagetwo['dc.description.abstract'][0].value",
                is("This is the abstract of our PDF file")))
                // we can just check that the pdf is stored in the item
                .andExpect(
                        jsonPath("$._embedded.workspaceitems[0].sections.upload.files[0].metadata['dc.title'][0].value",
                                is("myfile.pdf")))
                .andExpect(jsonPath(
                        "$._embedded.workspaceitems[0].sections.upload.files[0].metadata['dc.source'][0].value",
                        is("/local/path/myfile.pdf")))
        ;

        pdf.close();
    }

    @Test
    /**
     * Test the exposition of validation error for missing required metadata both at the creation time than on existent
     * workspaceitems
     *
     * @throws Exception
     */
    public void validationErrorsRequiredMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .build();

        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
        ;

        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 2")
                .build();

        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItem2.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                        Matchers.contains(
                                hasJsonPath("$.paths", Matchers.contains(
                                        hasJsonPath("$", Matchers.is("/sections/traditionalpageone/dc.date.issued"))
                                )))))
        ;

        // create an empty workspaceitem explicitly in the col1, check validation on creation
        getClient(authToken).perform(post("/api/submission/workspaceitems")
                    .param("collection", col1.getID().toString())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                // title and dateissued are required in the first panel
                // the json path with a @ selector always return an array
                .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                        Matchers.contains(
                                hasJsonPath("$.paths", Matchers.containsInAnyOrder(
                                        hasJsonPath("$", Matchers.is("/sections/traditionalpageone/dc.title")),
                                        hasJsonPath("$", Matchers.is("/sections/traditionalpageone/dc.date.issued"))
                                )))))
        ;
    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupDOIMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .build();

        // try to add the web of science identifier
        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "10.1021/ac0354342");
        values.add(value);
        addId.add(new AddOperation("/sections/traditionalpageone/dc.identifier.doi", values));

        String patchBody = getPatchContent(addId);

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.doi'][0].value",
                        is("10.1021/ac0354342")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.title'][0].value",
                        is("Multistep microreactions with proteins using electrocapture technology.")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.type'][0].value",
                        is("Journal Article")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.date.issued'][0].value",
                        is("2004-05-01")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][0].value",
                        is("Astorga-Wells, Juan")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][1].value",
                        is("Bergman, Tomas")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][2].value",
                        is(StringEscapeUtils.unescapeJava("J\\u00F6rnvall, Hans"))))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.issn'][0].value",
                        is("0003-2700")))
                    .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.description.abstract'][0].value",
                        is("A method to perform multistep reactions by means of electroimmobilization of a " +
                           "target molecule in a microflow stream is presented. A target protein is captured " +
                           "by the opposing effects between the hydrodynamic and electric forces, after which " +
                           "another medium is injected into the system. The second medium carries enzymes or " +
                           "other reagents, which are brought into contact with the target protein and react." +
                           " The immobilization is reversed by disconnecting the electric field, " +
                           "upon which products are collected at the outlet of the device for analysis. On-line " +
                           "reduction, alkylation, and trypsin digestion of proteins is demonstrated and was" +
                           " monitored by MALDI mass spectrometry.")))
            ;

            // verify that the patch changes have been persisted
            getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.doi'][0].value",
                    is("10.1021/ac0354342")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.title'][0].value",
                    is("Multistep microreactions with proteins using electrocapture technology.")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.type'][0].value",
                    is("Journal Article")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.date.issued'][0].value",
                    is("2004-05-01")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][0].value",
                    is("Astorga-Wells, Juan")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][1].value",
                    is("Bergman, Tomas")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][2].value",
                    is(StringEscapeUtils.unescapeJava("J\\u00F6rnvall, Hans"))))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.issn'][0].value",
                    is("0003-2700")))
                .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.description.abstract'][0].value",
                    is("A method to perform multistep reactions by means of electroimmobilization of a " +
                       "target molecule in a microflow stream is presented. A target protein is captured " +
                       "by the opposing effects between the hydrodynamic and electric forces, after which " +
                       "another medium is injected into the system. The second medium carries enzymes or " +
                       "other reagents, which are brought into contact with the target protein and react." +
                       " The immobilization is reversed by disconnecting the electric field, " +
                       "upon which products are collected at the outlet of the device for analysis. On-line " +
                       "reduction, alkylation, and trypsin digestion of proteins is demonstrated and was" +
                       " monitored by MALDI mass spectrometry.")))
            ;

    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupScopusMetadataTest() throws Exception {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String apikey = configService.getProperty("submission.lookup.scopus.apikey");
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .build();

        // try to add the web of science identifier
        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "10.1016/j.joi.2016.11.006");
        values.add(value);
        addId.add(new AddOperation("/sections/traditionalpageone/dc.identifier.doi", values));

        String patchBody = getPatchContent(addId);

        if (apikey == null || apikey.equals("")) {
            getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.doi'][0].value",
                        is("10.1016/j.joi.2016.11.006")));

                // verify that the patch changes have been persisted
                getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.doi'][0].value",
                        is("10.1016/j.joi.2016.11.006")));
        } else {
            getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.doi'][0].value",
                        is("10.1016/j.joi.2016.11.006")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.title'][0].value",
                        is("Partial orders for zero-sum arrays with applications to network theory")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.date.issued'][0].value",
                        is("2017-02-01")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][0].value",
                        is("Liu Y.")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][1].value",
                        is("Rousseau R")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][2].value",
                        is("Egghe L.")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.issn'][0].value",
                        is("17511577")))
                    .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.description.abstract'][0].value",
                        is(StringEscapeUtils.unescapeJava("\\u00A9 2016 Elsevier Ltd In this contribution" +
                           " we study partial orders in the set" +
                           " of zero-sum arrays. Concretely, these partial orders relate to local and" +
                           " global hierarchy and dominance theories. The exact relation between hierarchy" +
                           " and dominance curves is explained. Based on this investigation we design a" +
                           " new approach for measuring dominance or stated otherwise, power structures," +
                           " in networks. A new type of Lorenz curve to measure dominance or power is proposed," +
                           " and used to illustrate intrinsic characteristics of networks. The new curves," +
                           " referred to as D-curves are partly concave and partly convex. As such they do" +
                           " not satisfy Dalton's transfer principle. Most importantly, this article" +
                           " introduces a framework to compare different power structures as a whole." +
                           " It is shown that D-curves have several properties making them suitable to" +
                           " measure dominance. If dominance and being a subordinate are reversed, the" +
                           " dominance structure in a network is also reversed."))));

            // verify that the patch changes have been persisted
            getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.doi'][0].value",
                        is("10.1016/j.joi.2016.11.006")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.title'][0].value",
                        is("Partial orders for zero-sum arrays with applications to network theory")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.date.issued'][0].value",
                        is("2017-02-01")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][0].value",
                        is("Liu Y.")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][1].value",
                        is("Rousseau R")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][2].value",
                        is("Egghe L.")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.issn'][0].value",
                        is("17511577")))
                    .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.description.abstract'][0].value",
                        is(StringEscapeUtils.unescapeJava("\\u00A9 2016 Elsevier Ltd In this contribution" +
                           " we study partial orders in the set" +
                           " of zero-sum arrays. Concretely, these partial orders relate to local and" +
                           " global hierarchy and dominance theories. The exact relation between hierarchy" +
                           " and dominance curves is explained. Based on this investigation we design a" +
                           " new approach for measuring dominance or stated otherwise, power structures," +
                           " in networks. A new type of Lorenz curve to measure dominance or power is proposed," +
                           " and used to illustrate intrinsic characteristics of networks. The new curves," +
                           " referred to as D-curves are partly concave and partly convex. As such they do" +
                           " not satisfy Dalton's transfer principle. Most importantly, this article" +
                           " introduces a framework to compare different power structures as a whole." +
                           " It is shown that D-curves have several properties making them suitable to" +
                           " measure dominance. If dominance and being a subordinate are reversed, the" +
                           " dominance structure in a network is also reversed."))));
        }
    }

    @Test
    /**
     * Test the metadata lookup
     *
     * @throws Exception
     */
    public void lookupWOSMetadataTest() throws Exception {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String wosUser = configService.getProperty("submission.lookup.webofknowledge.user");
        String wosPassword = configService.getProperty("submission.lookup.webofknowledge.password");
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .build();

        // try to add the web of science identifier
        List<Operation> addId = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "WOS:000270372400005");
        values.add(value);
        addId.add(new AddOperation("/sections/traditionalpageone/dc.identifier.isi", values));

        String patchBody = getPatchContent(addId);

        if (wosUser == null || wosUser.equals("") || wosPassword == null ||  wosPassword.equals("")) {
            getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.isi'][0].value",
                        is("WOS:000270372400005")));

                // verify that the patch changes have been persisted
                getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.isi'][0].value",
                        is("WOS:000270372400005")));
        } else {
            getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk())
                    // testing lookup
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.isi'][0].value",
                        is("WOS:000270372400005")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.title'][0].value",
                        is("Individual Susceptibility to Cadmium Toxicity and Metallothionein Gene Polymorphisms:" +
                           " with References to Current Status of Occupational Cadmium Exposure")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.type'][0].value",
                        is("Article")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.date.issued'][0].value",
                        is("2009")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][0].value",
                        is("Miura, N")))
                    .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.issn'][0].value",
                        is("0019-8366")));

            // verify that the patch changes have been persisted
            getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                // testing lookup
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.isi'][0].value",
                    is("WOS:000270372400005")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.title'][0].value",
                    is("Individual Susceptibility to Cadmium Toxicity and Metallothionein Gene Polymorphisms:" +
                       " with References to Current Status of Occupational Cadmium Exposure")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.type'][0].value",
                    is("Article")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.date.issued'][0].value",
                    is("2009")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.contributor.author'][0].value",
                    is("Miura, N")))
                .andExpect(jsonPath("$.sections.traditionalpageone['dc.identifier.issn'][0].value",
                    is("0019-8366")));
        }
    }

    @Test
    /**
     * Test the update of metadata
     *
     * @throws Exception
     */
    public void patchUpdateMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("ExtraEntry")
                .build();

        // a simple patch to update an existent metadata
        List<Operation> updateTitle = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        updateTitle.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.errors").doesNotExist())
                        .andExpect(jsonPath("$",
                                // check the new title and untouched values
                                Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                        "New Title", "2017-10-17", "ExtraEntry"))));
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$",
                    Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            "New Title", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Test delete of a metadata
     *
     * @throws Exception
     */
    public void patchDeleteMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("ExtraEntry")
                .build();

        WorkspaceItem witemMultipleSubjects = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("Subject1")
                .withSubject("Subject2")
                .withSubject("Subject3")
                .withSubject("Subject4")
                .build();

        WorkspaceItem witemWithTitleDateAndSubjects = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("Subject1")
                .withSubject("Subject2")
                .withSubject("Subject3")
                .withSubject("Subject4")
                .build();

        // try to remove the title
        List<Operation> removeTitle = new ArrayList<Operation>();
        removeTitle.add(new RemoveOperation("/sections/traditionalpageone/dc.title/0"));

        String patchBody = getPatchContent(removeTitle);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                                Matchers.contains(hasJsonPath("$.paths",
                                        Matchers.contains(
                                                hasJsonPath("$",
                                                        Matchers.is("/sections/traditionalpageone/dc.title")))))))
                            .andExpect(jsonPath("$",
                                    // check the new title and untouched values
                                    Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                            null, "2017-10-17", "ExtraEntry"))));
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                    Matchers.contains(
                            hasJsonPath("$.paths", Matchers.contains(
                                    hasJsonPath("$", Matchers.is("/sections/traditionalpageone/dc.title"))
                            )))))
            .andExpect(jsonPath("$",
                    Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            null, "2017-10-17", "ExtraEntry"))))
        ;

        // try to remove a metadata in a specific position
        List<Operation> removeMidSubject = new ArrayList<Operation>();
        removeMidSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/1"));

        patchBody = getPatchContent(removeMidSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject3")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Subject4")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject3")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Subject4")))
        ;

        List<Operation> removeFirstSubject = new ArrayList<Operation>();
        removeFirstSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/0"));

        patchBody = getPatchContent(removeFirstSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject4")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject4")))
        ;

        List<Operation> removeLastSubject = new ArrayList<Operation>();
        removeLastSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/1"));

        patchBody = getPatchContent(removeLastSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
        ;

        List<Operation> removeFinalSubject = new ArrayList<Operation>();
        removeFinalSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/0"));

        patchBody = getPatchContent(removeFinalSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;

        // remove all the subjects with a single operation
        List<Operation> removeSubjectsAllAtOnce = new ArrayList<Operation>();
        removeSubjectsAllAtOnce.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject"));

        patchBody = getPatchContent(removeSubjectsAllAtOnce);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witemWithTitleDateAndSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witemWithTitleDateAndSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;
    }

    @Test
    /**
     * Test delete of a section
     *
     * @throws Exception
     */
    public void patchDeleteSectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("Subject1")
                .withSubject("Subject2")
                .withSubject("Subject3")
                .withSubject("Subject4")
                .withAbstract("This is a sample abstract")
                .build();

        // remove entire section metadata
        List<Operation> removeSubjectsAllAtOnce = new ArrayList<Operation>();
        removeSubjectsAllAtOnce.add(new RemoveOperation("/sections/traditionalpagetwo"));

        String patchBody = getPatchContent(removeSubjectsAllAtOnce);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.description.abstract']")
                                    .doesNotExist())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.description.abstract']").doesNotExist())
        ;
    }

    @Test
    /**
     * Test the addition of metadata
     *
     * @throws Exception
     */
    public void patchAddMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withIssueDate("2017-10-17")
                .withSubject("ExtraEntry")
                .build();


        // try to add the title
        List<Operation> addTitle = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        values.add(value);
        addTitle.add(new AddOperation("/sections/traditionalpageone/dc.title", values));

        String patchBody = getPatchContent(addTitle);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$",
                                    // check if the new title if back and the other values untouched
                                    Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                            "New Title", "2017-10-17", "ExtraEntry"))));
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$",
                    Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            "New Title", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Test the addition of metadata
     *
     * @throws Exception
     */
    public void patchAddMultipleMetadataValuesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .build();

        // try to add multiple subjects at once
        List<Operation> addSubjects = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value1 = new HashMap<String, String>();
        value1.put("value", "Subject1");
        Map<String, String> value2 = new HashMap<String, String>();
        value2.put("value", "Subject2");
        values.add(value1);
        values.add(value2);

        addSubjects.add(new AddOperation("/sections/traditionalpagetwo/dc.subject", values));

        String patchBody = getPatchContent(addSubjects);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject2")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject2")))
        ;

        // add a subject in the first position
        List<Operation> addFirstSubject = new ArrayList<Operation>();
        Map<String, String> firstSubject = new HashMap<String, String>();
        firstSubject.put("value", "First Subject");

        addFirstSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/0", firstSubject));

        patchBody = getPatchContent(addFirstSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Subject2")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Subject2")))
        ;

        // add a subject in a central position
        List<Operation> addMidSubject = new ArrayList<Operation>();
        Map<String, String> midSubject = new HashMap<String, String>();
        midSubject.put("value", "Mid Subject");

        addMidSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/2", midSubject));

        patchBody = getPatchContent(addMidSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Mid Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value",
                                    is("Subject2")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Mid Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value", is("Subject2")))
        ;

        // append a last subject without specifying the index
        List<Operation> addLastSubject = new ArrayList<Operation>();
        Map<String, String> lastSubject = new HashMap<String, String>();
        lastSubject.put("value", "Last Subject");

        addLastSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/4", lastSubject));

        patchBody = getPatchContent(addLastSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Mid Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value",
                                    is("Subject2")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value",
                                    is("Last Subject")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Mid Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value", is("Subject2")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value", is("Last Subject")))
        ;

        // append a last subject without specifying the index
        List<Operation> addFinalSubject = new ArrayList<Operation>();
        Map<String, String> finalSubject = new HashMap<String, String>();
        finalSubject.put("value", "Final Subject");

        addFinalSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/-", finalSubject));

        patchBody = getPatchContent(addFinalSubject);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Mid Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value",
                                    is("Subject2")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value",
                                    is("Last Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][5].value",
                                    is("Final Subject")))
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Mid Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value", is("Subject2")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value", is("Last Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][5].value", is("Final Subject")))
        ;
    }

    @Test
    /**
     * Test the acceptance of the deposit license
     *
     * @throws Exception
     */
    public void patchAcceptLicenseTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .build();

        WorkspaceItem witem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem 2")
                .withIssueDate("2017-10-17")
                .build();

        WorkspaceItem witem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem 3")
                .withIssueDate("2017-10-17")
                .build();

        WorkspaceItem witem4 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem 4")
                .withIssueDate("2017-10-17")
                .build();

        // check that our workspaceitems come without a license (all are build in the same way, just check the first)
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(false)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // try to grant the license with an add operation
        List<Operation> addGrant = new ArrayList<Operation>();
        addGrant.add(new AddOperation("/sections/license/granted", true));

        String patchBody = getPatchContent(addGrant);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(true)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(true)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // try to grant the license with an add operation supplying a string instead than a boolean
        List<Operation> addGrantString = new ArrayList<Operation>();
        addGrantString.add(new AddOperation("/sections/license/granted", "true"));

        patchBody = getPatchContent(addGrantString);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem2.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(true)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(true)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // try to grant the license with a replace operation
        List<Operation> replaceGrant = new ArrayList<Operation>();
        replaceGrant.add(new ReplaceOperation("/sections/license/granted", true));

        patchBody = getPatchContent(replaceGrant);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem3.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(true)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem3.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(true)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // try to grant the license with a replace operation supplying a string
        List<Operation> replaceGrantString = new ArrayList<Operation>();
        replaceGrant.add(new ReplaceOperation("/sections/license/granted", "true"));

        patchBody = getPatchContent(replaceGrant);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem4.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(true)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem4.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(true)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;
    }

    @Test
    /**
     * Test the reject of the deposit license
     *
     * @throws Exception
     */
    public void patchRejectLicenseTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .grantLicense()
                .build();

        WorkspaceItem witem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem 2")
                .withIssueDate("2017-10-17")
                .grantLicense()
                .build();

        WorkspaceItem witem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem 3")
                .withIssueDate("2017-10-17")
                .grantLicense()
                .build();

        WorkspaceItem witem4 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem 4")
                .withIssueDate("2017-10-17")
                .grantLicense()
                .build();

        // check that our workspaceitems come with a license (all are build in the same way, just check the first)
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(true)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isNotEmpty())
            .andExpect(jsonPath("$.sections.license.url").isNotEmpty())
        ;

        // try to reject the license with an add operation
        List<Operation> addGrant = new ArrayList<Operation>();
        addGrant.add(new AddOperation("/sections/license/granted", false));

        String patchBody = getPatchContent(addGrant);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(false)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(false)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // try to reject the license with an add operation supplying a string instead than a boolean
        List<Operation> addGrantString = new ArrayList<Operation>();
        addGrantString.add(new AddOperation("/sections/license/granted", "false"));

        patchBody = getPatchContent(addGrantString);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem2.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(false)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem2.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(false)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // try to reject the license with a replace operation
        List<Operation> replaceGrant = new ArrayList<Operation>();
        replaceGrant.add(new ReplaceOperation("/sections/license/granted", false));

        patchBody = getPatchContent(replaceGrant);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem3.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(false)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem3.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(false)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // try to reject the license with a replace operation supplying a string
        List<Operation> replaceGrantString = new ArrayList<Operation>();
        replaceGrant.add(new ReplaceOperation("/sections/license/granted", "false"));

        patchBody = getPatchContent(replaceGrant);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem4.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.license.granted",
                                    is(false)))
                            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
                            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

        // verify that the patch changes have been persisted
        getClient().perform(get("/api/submission/workspaceitems/" + witem4.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.license.granted",
                    is(false)))
            .andExpect(jsonPath("$.sections.license.acceptanceDate").isEmpty())
            .andExpect(jsonPath("$.sections.license.url").isEmpty())
        ;

    }

    @Test
    /**
     * Test update of bitstream metadata in the upload section
     *
     * @throws Exception
     */
    public void patchUploadTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        String authToken = getAuthToken(admin.getEmail(), password);

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .build();

        // check the file metadata
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                    is("/local/path/simple-article.pdf")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                    is("simple-article.pdf")))
        ;

        // try to change the filename and add a description
        List<Operation> addOpts = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "newfilename.pdf");
        Map<String, String> valueDesc  = new HashMap<String, String>();
        valueDesc.put("value", "Description");
        List valueDescs = new ArrayList();
        valueDescs.add(valueDesc);
        addOpts.add(new AddOperation("/sections/upload/files/0/metadata/dc.title/0", value));
        addOpts.add(new AddOperation("/sections/upload/files/0/metadata/dc.description", valueDescs));

        String patchBody = getPatchContent(addOpts);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            // is the source still here?
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                                    is("/local/path/simple-article.pdf")))
                            // check the new filename
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                                    is("newfilename.pdf")))
                            // check the description
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.description'][0].value",
                                    is("Description")))
        ;

        // check that changes persist
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                    is("/local/path/simple-article.pdf")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                    is("newfilename.pdf")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.description'][0].value",
                    is("Description")))
        ;

        // try to remove the description and the source now
        List<Operation> removeOpts = new ArrayList<Operation>();
        removeOpts.add(new RemoveOperation("/sections/upload/files/0/metadata/dc.source/0"));
        removeOpts.add(new RemoveOperation("/sections/upload/files/0/metadata/dc.description"));

        patchBody = getPatchContent(removeOpts);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            // check the removed source
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source']").doesNotExist())
                            // check the filename still here
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                                    is("newfilename.pdf")))
                            // check the removed description
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.description']").doesNotExist())
        ;

        // check that changes persist
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source']").doesNotExist())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                    is("newfilename.pdf")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.description']").doesNotExist())        ;

        // try to update the filename with an update opt
        List<Operation> updateOpts = new ArrayList<Operation>();
        Map<String, String> updateValue = new HashMap<String, String>();
        updateValue.put("value", "another-filename.pdf");
        updateOpts.add(new ReplaceOperation("/sections/upload/files/0/metadata/dc.title/0", updateValue));

        patchBody = getPatchContent(updateOpts);
        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            // check the filename still here
                            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                                    is("another-filename.pdf")))
        ;

        // check that changes persist
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                    is("another-filename.pdf")))
        ;
    }

    @Test
    /**
     * Test the upload of files in the upload over section
     *
     * @throws Exception
     */
    public void uploadTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .build();

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        final MockMultipartFile pdfFile = new MockMultipartFile("file", "/local/path/simple-article.pdf",
                "application/pdf", pdf);

        // upload the file in our workspaceitem
        getClient(authToken).perform(fileUpload("/api/submission/workspaceitems/" + witem.getID())
                .file(pdfFile))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                            is("simple-article.pdf")))
                    .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                            is("/local/path/simple-article.pdf")))
        ;

        // check the file metadata
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                    is("simple-article.pdf")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                    is("/local/path/simple-article.pdf")))
        ;
    }

    @Test
    public void createWorkspaceWithFiles_UploadRequired() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withTitle("Test WorkspaceItem")
            .withIssueDate("2017-10-17")
            .build();

        configurationService.setProperty("webui.submit.upload.required", true);

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        final MockMultipartFile pdfFile = new MockMultipartFile("file", "/local/path/simple-article.pdf",
            "application/pdf", pdf);

        // upload the file in our workspaceitem
        getClient(authToken).perform(fileUpload("/api/submission/workspaceitems/" + witem.getID())
            .file(pdfFile))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value",
                is("simple-article.pdf")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                is("/local/path/simple-article.pdf")))
        ;

        //Verify there are no errors since file was uploaded (with upload required set to true)
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    public void createWorkspaceWithoutFiles_UploadRequired() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        String authToken = getAuthToken(admin.getEmail(), password);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withTitle("Test WorkspaceItem")
            .withIssueDate("2017-10-17")
            .build();

        configurationService.setProperty("webui.submit.upload.required", true);

        //Verify there is an error since no file was uploaded (with upload required set to true)
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").isNotEmpty())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.filerequired')]",
                Matchers.contains(
                    hasJsonPath("$.paths", Matchers.contains(
                        hasJsonPath("$", Matchers.is("/sections/upload"))
                    )))));
    }

    @Test
    public void createWorkspaceItemFromExternalSources() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/submission/workspaceitems?owningCollection="
                                                                + col1.getID().toString())
                                                           .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                           .content("https://localhost:8080/server/api/integration/" +
                                                                        "externalsources/mock/entryValues/one"))
                                            .andExpect(status().isCreated()).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        Integer workspaceItemId = (Integer) map.get("id");
        String itemUuidString = String.valueOf(((Map) ((Map) map.get("_embedded")).get("item")).get("uuid"));

        getClient(token).perform(get("/api/submission/workspaceitems/" + workspaceItemId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.id", is(workspaceItemId)),
                            hasJsonPath("$.type", is("workspaceitem")),
                            hasJsonPath("$._embedded.item", Matchers.allOf(
                                hasJsonPath("$.id", is(itemUuidString)),
                                hasJsonPath("$.uuid", is(itemUuidString)),
                                hasJsonPath("$.type", is("item")),
                                hasJsonPath("$.metadata", Matchers.allOf(
                                    MetadataMatcher.matchMetadata("dc.contributor.author", "Donald, Smith")
                                )))))
                        ));
    }

    @Test
    public void createWorkspaceItemFromExternalSourcesNoOwningCollectionUuidBadRequest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/submission/workspaceitems")
                                     .contentType(parseMediaType(
                                         TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                "mock/entryValues/one"))
                        .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void createWorkspaceItemFromExternalSourcesRandomOwningCollectionUuidBadRequest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/submission/workspaceitems?owningCollection=" + UUID.randomUUID())
                                     .contentType(parseMediaType(
                                         TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mock/entryValues/one"))
                        .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void createWorkspaceItemFromExternalSourcesWrongUriList() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/submission/workspaceitems?owningCollection="
                                          + col1.getID().toString())
                                     .contentType(parseMediaType(
                                         TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/mock/mock/mock/" +
                                                  "mock/entryValues/one")).andExpect(status().isBadRequest());
    }

    @Test
    public void createWorkspaceItemFromExternalSourcesWrongSourceBadRequest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/submission/workspaceitems?owningCollection="
                                          + col1.getID().toString())
                                     .contentType(parseMediaType(
                                         TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mockWrongSource/entryValues/one"))
                        .andExpect(status().isBadRequest());

    }

    @Test
    public void createWorkspaceItemFromExternalSourcesWrongIdResourceNotFound() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/submission/workspaceitems?owningCollection="
                                          + col1.getID().toString())
                                     .contentType(parseMediaType(
                                         TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mock/entryValues/azeazezaezeaz"))
                        .andExpect(status().is(404));

    }

    @Test
    public void createWorkspaceItemFromExternalSourcesForbidden() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/submission/workspaceitems?owningCollection="
                                          + col1.getID().toString())
                                     .contentType(parseMediaType(
                                         TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mock/entryValues/one"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void createWorkspaceItemFromExternalSourcesUnauthorized() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        getClient().perform(post("/api/submission/workspaceitems?owningCollection="
                                     + col1.getID().toString())
                                .contentType(parseMediaType(
                                    TEXT_URI_LIST_VALUE))
                                .content("https://localhost:8080/server/api/integration/externalsources/" +
                                             "mock/entryValues/one"))
                   .andExpect(status().isUnauthorized());
    }
}
