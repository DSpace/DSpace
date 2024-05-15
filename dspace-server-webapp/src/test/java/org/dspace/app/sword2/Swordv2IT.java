/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.sword2;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import org.dspace.app.rest.test.AbstractWebClientIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Integration test to verify the /swordv2 endpoint is responding as a valid SWORDv2 endpoint.
 * This tests that our dspace-swordv2 module is running at this endpoint.
 * <P>
 * This is a AbstractWebClientIntegrationTest because testing dspace-swordv2 requires
 * running a web server (as dspace-swordv2 makes use of Servlets, not Controllers).
 *
 * @author Tim Donohue
 */
// Ensure the SWORDv2 SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"swordv2-server.enabled = true"})
public class Swordv2IT extends AbstractWebClientIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    // All SWORD v2 paths that we test against
    private final String SWORD_PATH = "/swordv2";
    private final String SERVICE_DOC_PATH = SWORD_PATH + "/servicedocument";
    private final String COLLECTION_PATH = SWORD_PATH + "/collection";
    private final String MEDIA_RESOURCE_PATH = SWORD_PATH + "/edit-media";
    private final String EDIT_PATH = SWORD_PATH + "/edit";
    private final String STATEMENT_PATH = SWORD_PATH + "/statement";

    // Content Types used
    private final String ATOM_SERVICE_CONTENT_TYPE = "application/atomserv+xml;charset=UTF-8";
    private final String ATOM_FEED_CONTENT_TYPE = "application/atom+xml;type=feed;charset=UTF-8";
    private final String ATOM_ENTRY_CONTENT_TYPE = "application/atom+xml;type=entry;charset=UTF-8";

    /**
     * Create a global temporary upload folder which will be cleaned up automatically by JUnit.
     * NOTE: As a ClassRule, this temp folder is shared by ALL tests below.
     **/
    @ClassRule
    public static final TemporaryFolder uploadTempFolder = new TemporaryFolder();

    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that SWORDv2WebConfig is found/available (as this class deploys SWORDv2)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: SWORDv2WebConfig is provided by the 'dspace-swordv2' module
        try {
            Class.forName("org.dspace.app.configuration.SWORDv2WebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }

        // Ensure SWORDv2 URL configurations are set correctly (based on our integration test server's paths)
        // SWORDv2 validates requests against these configs, and throws a 404 if they don't match the request path
        configurationService.setProperty("swordv2-server.url", getURL(SWORD_PATH));
        configurationService.setProperty("swordv2-server.servicedocument.url", getURL(SERVICE_DOC_PATH));
        configurationService.setProperty("swordv2-server.collection.url", getURL(COLLECTION_PATH));

        // Override default value of SWORD upload directory to point at our JUnit TemporaryFolder (see above).
        // This ensures uploaded files are saved in a place where JUnit can clean them up automatically.
        configurationService.setProperty("swordv2-server.upload.tempdir",
                                         uploadTempFolder.getRoot().getAbsolutePath());

        // MUST be set to allow DELETE requests on Items which are in the archive.  (This isn't enabled by default)
        configurationService.setProperty("plugin.single.org.dspace.sword2.WorkflowManager",
                                         "org.dspace.sword2.WorkflowManagerUnrestricted");
    }

    @Test
    public void serviceDocumentUnauthorizedTest() throws Exception {
        // Attempt to GET the ServiceDocument without first authenticating
        ResponseEntity<String> response = getResponseAsString(SERVICE_DOC_PATH);
        // Expect a 401 response code
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        // Attempt to GET the ServiceDocument as any user account
        ResponseEntity<String> response = getResponseAsString(SERVICE_DOC_PATH,
                                                              eperson.getEmail(), password);
        // Expect a 200 response code, and an ATOM service document
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ATOM_SERVICE_CONTENT_TYPE, response.getHeaders().getContentType().toString());

        // Check for correct SWORD version in response body
        assertThat(response.getBody(), containsString("<version xmlns=\"http://purl.org/net/sword/terms/\">2.0</version>"));
    }

    @Test
    public void collectionUnauthorizedTest() throws Exception {
        // Attempt to POST to /collection endpoint without sending authentication information
        ResponseEntity<String> response = postResponseAsString(COLLECTION_PATH, null, null, null);
        // Expect a 401 response code
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * In DSpace the /collections/[handle-prefix]/[handle-suffix] endpoint gives a list of all Items
     * which were deposited BY THE AUTHENTICATED USER into the given collection
     */
    @Test
    public void collectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create all content as the SAME EPERSON we will use to authenticate on this endpoint.
        // THIS IS REQUIRED as the /collections endpoint will only show YOUR ITEM SUBMISSIONS.
        context.setCurrentUser(eperson);
        // Create a top level community and one Collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test SWORDv2 Collection")
                                                 .build();

        // Add one Item into that Collection.
        String itemTitle = "Test SWORDv2 Item";
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle(itemTitle)
                               .withAuthor("Smith, Sam")
                               .build();

        // Above changes MUST be committed to the database for SWORDv2 to see them.
        context.commit();
        context.restoreAuthSystemState();

        // This Collection should exist on the /collection endpoint via its handle.
        // Authenticate as the same user we used to create the test content above.
        ResponseEntity<String> response = getResponseAsString(COLLECTION_PATH + "/" + collection.getHandle(),
                                                              eperson.getEmail(), password);

        // Expect a 200 response code, and an ATOM feed document
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ATOM_FEED_CONTENT_TYPE, response.getHeaders().getContentType().toString());

        // Check for response body to include the Item edit link
        // NOTE: This endpoint will only list items which were submitted by the authenticated EPerson.
        assertThat(response.getBody(), containsString(EDIT_PATH + "/" + item.getID().toString()));
        // Check for response body to include the Item title text
        assertThat(response.getBody(), containsString("<title type=\"text\">" + itemTitle + "</title>"));
    }

    @Test
    public void mediaResourceUnauthorizedTest() throws Exception {
        // Attempt to POST to /edit-media endpoint without sending authentication information
        ResponseEntity<String> response = postResponseAsString(MEDIA_RESOURCE_PATH, null, null, null);
        // Expect a 401 response code
        assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * This tests four different SWORDv2 actions, as these all require starting with a new deposit.
     * 1. Depositing a new item via SWORD (via POST /collections/[collection-uuid])
     * 2. Reading the deposited item (via GET /edit/[item-uuid])
     * 3. Updating the deposited item's metadata (via PUT /edit/[item-uuid])
     * 4. Deleting the deposited item (via DELETE /edit/[item-uuid]).
     */
    @Test
    public void depositAndEditViaSwordTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a top level community and one Collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        // Make sure our Collection allows the "eperson" user to submit into it
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test SWORDv2 Collection")
                                                 .withSubmitterGroup(eperson)
                                                 .build();
        // Above changes MUST be committed to the database for SWORDv2 to see them.
        context.commit();
        context.restoreAuthSystemState();

        // Add file
        LinkedMultiValueMap<Object, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new FileSystemResource(Path.of("src", "test", "resources",
                                                             "org", "dspace", "app", "sword2", "example.zip")));
        // Add required headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setContentDisposition(ContentDisposition.attachment().filename("example.zip").build());
        headers.set("Packaging", "http://purl.org/net/sword/package/METSDSpaceSIP");
        headers.setAccept(List.of(MediaType.APPLICATION_ATOM_XML));

        //----
        // STEP 1: Verify upload/submit via SWORDv2 works
        //----
        // Send POST to upload Zip file via SWORD
        ResponseEntity<String> response = postResponseAsString(COLLECTION_PATH + "/" + collection.getHandle(),
                                                               eperson.getEmail(), password,
                                                               new HttpEntity<>(multipart, headers));

        // Expect a 201 CREATED response with ATOM "entry" content returned
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(ATOM_ENTRY_CONTENT_TYPE, response.getHeaders().getContentType().toString());
        // MUST return a "Location" header which is the "/swordv2/edit/[uuid]" URI of the created item
        assertNotNull(response.getHeaders().getLocation());

        String editLink = response.getHeaders().getLocation().toString();

        // Body should include that link as the rel="edit" URL
        assertThat(response.getBody(), containsString("<link href=\"" + editLink + "\" rel=\"edit\""));

        //----
        // STEP 2: Verify uploaded content can be read via SWORDv2
        //----
        // Edit URI should work when requested by the EPerson who did the deposit
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        RequestEntity request = RequestEntity.get(editLink)
                                             .accept(MediaType.valueOf("application/atom+xml"))
                                             .headers(authHeaders)
                                             .build();
        response = responseAsString(request);

        // Expect a 200 response with ATOM feed content returned
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ATOM_FEED_CONTENT_TYPE, response.getHeaders().getContentType().toString());
        // Body should include links to bitstreams from the zip.
        // This just verifies at least one /swordv2/edit-media/bitstream/* link exists.
        assertThat(response.getBody(), containsString(getURL(MEDIA_RESOURCE_PATH + "/bitstream")));
        // Verify Item title also is returned in the body
        assertThat(response.getBody(), containsString("Attempts to detect retrotransposition"));

        //----
        // STEP 3: Verify uploaded content can be UPDATED via SWORDv2 (by an Admin ONLY)
        //----
        // Edit URI can be used with PUT to update the metadata of the Item.
        // Since we submitted to a collection WITHOUT a workflow, this item is in archive. That means DELETE
        // must be done via a user with Admin privileges on the Item.
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(admin.getEmail(), password);
        // This example simply changes the title.
        String newTitle = "This is a new title updated via PUT";
        String newTitleEntry = "<entry xmlns=\"http://www.w3.org/2005/Atom\"><title>" + newTitle + "</title></entry>";
        request = RequestEntity.put(editLink)
                               .headers(authHeaders)
                               .contentType(MediaType.APPLICATION_ATOM_XML)
                               .body(newTitleEntry);
        response = responseAsString(request);
        // Expect a 200 OK response
        assertEquals(HttpStatus.OK, response.getStatusCode());

        //----
        // STEP 4: Verify content was successfully updated by reading content again
        //----
        // Edit URI should work when requested by the EPerson who did the deposit
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        request = RequestEntity.get(editLink)
                               .accept(MediaType.valueOf("application/atom+xml"))
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify the new Item title is now included in the response body
        assertThat(response.getBody(), containsString(newTitle));

        //----
        // STEP 5: Verify archived Item can be DELETED via SWORDv2 (by an Admin ONLY)
        //----
        // Edit URI should also allow user to DELETE the uploaded content
        // Since we submitted to a collection WITHOUT a workflow, this item is in archive. That means DELETE
        // must be done via a user with Admin privileges on the Item.
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(admin.getEmail(), password);
        request = RequestEntity.delete(editLink)
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        // Expect a 204 No Content response
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify that Edit URI now returns a 404 (using eperson login info)
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        request = RequestEntity.get(editLink)
                               .accept(MediaType.valueOf("application/atom+xml"))
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        // Expect a 404 response as content was deleted
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void deleteWorkspaceItemViaSwordTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a top level community and one Collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test SWORDv2 Collection")
                                                 .withSubmitterGroup(eperson)
                                                 .build();

        String titleOfItem = "This is a test SWORD workspace item";
        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(eperson)
                                                .withTitle(titleOfItem)
                                                .build();

        // Above changes MUST be committed to the database for SWORDv2 to see them.
        context.commit();
        context.restoreAuthSystemState();

        // Edit link of WorkspaceItem is the Item UUID
        String editLink = "/swordv2/edit/" + wsi.getItem().getID();

        //----
        // STEP 1: Verify WorkspaceItem is found via SWORDv2 when logged in as the submitter
        //----
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        RequestEntity request = RequestEntity.get(editLink)
                                             .accept(MediaType.valueOf("application/atom+xml"))
                                             .headers(authHeaders)
                                             .build();
        ResponseEntity<String> response = responseAsString(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify the new Item title is now included in the response body
        assertThat(response.getBody(), containsString(titleOfItem));

        //----
        // STEP 2: Verify WorkspaceItem can be deleted by submitter
        //----
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        request = RequestEntity.delete(editLink)
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        // Expect a 204 No Content response
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify that Edit URI now returns a 404 (deleted successfully)
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        request = RequestEntity.get(editLink)
                               .accept(MediaType.valueOf("application/atom+xml"))
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        // Expect a 404 response as content was deleted
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    @Test
    public void deleteWorkflowItemViaSwordTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a top level community and one Collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        // Create a Collection with a workflow step enabled
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test SWORDv2 Workflow Collection")
                                                 .withSubmitterGroup(eperson)
                                                 .withWorkflowGroup(1, admin)
                                                 .build();

        String titleOfItem = "This is a test SWORD workflow item";
        XmlWorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, collection)
                                                          .withSubmitter(eperson)
                                                          .withTitle(titleOfItem)
                                                          .withIssueDate("2017-10-17")
                                                          .build();
        // Above changes MUST be committed to the database for SWORDv2 to see them.
        context.commit();
        context.restoreAuthSystemState();

        // Edit link of WorkflowItem is the Item UUID
        String editLink = "/swordv2/edit/" + workflowItem.getItem().getID();

        //----
        // STEP 1: Verify WorkflowItem is found via SWORDv2 when logged in as the submitter
        //----
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        RequestEntity request = RequestEntity.get(editLink)
                                             .accept(MediaType.valueOf("application/atom+xml"))
                                             .headers(authHeaders)
                                             .build();
        ResponseEntity<String> response = responseAsString(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify the new Item title is now included in the response body
        assertThat(response.getBody(), containsString(titleOfItem));

        //----
        // STEP 2: Verify WorkflowItem can be deleted by ADMIN only
        //----
        // NOTE: Once Item is in Workflow, deletion requires ADMIN permissions
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(admin.getEmail(), password);
        request = RequestEntity.delete(editLink)
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        // Expect a 204 No Content response
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify that Edit URI now returns a 404 (deleted successfully)
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        request = RequestEntity.get(editLink)
                               .accept(MediaType.valueOf("application/atom+xml"))
                               .headers(authHeaders)
                               .build();
        response = responseAsString(request);
        // Expect a 404 response as content was deleted
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void editUnauthorizedTest() throws Exception {
        // Attempt to POST to /edit endpoint without sending authentication information
        ResponseEntity<String> response = postResponseAsString(EDIT_PATH, null, null, null);
        // Expect a 401 response code
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void statementUnauthorizedTest() throws Exception {
        // Attempt to GET /statement endpoint without sending authentication information
        ResponseEntity<String> response = getResponseAsString(STATEMENT_PATH);
        // Expect a 401 response code
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * Statements exist for Items in DSpace (/statements/[item-uuid])
     * https://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#statement
     */
    @Test
    public void statementTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create all content as the SAME EPERSON we will use to authenticate on this endpoint.
        // THIS IS REQUIRED as the /statements endpoint will only show YOUR ITEM SUBMISSIONS.
        context.setCurrentUser(eperson);
        // Create a top level community and one Collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test SWORDv2 Collection")
                                                 .build();

        // Add one Item into that Collection.
        String itemTitle = "Test SWORDv2 Item";
        String itemAuthor = "Smith, Samantha";
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle(itemTitle)
                               .withAuthor(itemAuthor)
                               .build();

        // Above changes MUST be committed to the database for SWORDv2 to see them.
        context.commit();
        context.restoreAuthSystemState();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        // GET call to /statement MUST include an "Accept" header that matches one of the formats
        // supported by 'SwordStatementDisseminator' (configured in swordv2-server.cfg)
        RequestEntity request = RequestEntity.get(getURL(STATEMENT_PATH + "/" + item.getID().toString()))
                                             .accept(MediaType.valueOf("application/atom+xml"))
                                             .headers(authHeaders)
                                             .build();
        ResponseEntity<String> response = responseAsString(request);

        // Expect a 200 response with ATOM feed content returned
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ATOM_FEED_CONTENT_TYPE, response.getHeaders().getContentType().toString());


        // Body should include the statement path of the Item, as well as the title & author information
        assertThat(response.getBody(),
                   containsString(STATEMENT_PATH + "/" + item.getID().toString()));
        assertThat(response.getBody(),
                   containsString("<title type=\"text\">" + itemTitle + "</title>"));
        assertThat(response.getBody(),
                   containsString("<author><name>" + itemAuthor + "</name></author>"));
        // Also verify Item is in "archived" state
        assertThat(response.getBody(),
                   containsString("<category term=\"http://dspace.org/state/archived\""));
    }
}

