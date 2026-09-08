/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.sword;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.dspace.app.rest.test.AbstractWebClientIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
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

/**
 * Integration test to verify that the /sword endpoint is responding as a valid SWORD endpoint.
 * This tests that our dspace-sword module is running at this endpoint.
 * <P>
 * This is a AbstractWebClientIntegrationTest because testing dspace-sword requires
 * running a web server (as dspace-sword makes use of Servlets, not Controllers).
 *
 * @author Tim Donohue
 */
// Ensure the SWORD SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"sword-server.enabled = true"})
public class Swordv1IT extends AbstractWebClientIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    // All SWORD paths that we test against
    private final String SERVICE_DOC_PATH = "/sword/servicedocument";
    private final String DEPOSIT_PATH = "/sword/deposit";
    private final String MEDIA_LINK_PATH = "/sword/media-link";

    // ATOM Content type returned by SWORDv1
    private final String ATOM_CONTENT_TYPE = "application/atom+xml;charset=UTF-8";

    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that SWORDWebConfig is found/available (as this class deploys SWORD)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: SWORDWebConfig is provided by the 'dspace-sword' module
        try {
            Class.forName("org.dspace.app.configuration.SWORDWebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }

        // Ensure SWORD URL configurations are set correctly (based on our integration test server's paths)
        // SWORD validates requests against these configs, and throws a 404 if they don't match the request path
        configurationService.setProperty("sword-server.servicedocument.url", getURL(SERVICE_DOC_PATH));
        configurationService.setProperty("sword-server.deposit.url", getURL(DEPOSIT_PATH));
        configurationService.setProperty("sword-server.media-link.url", getURL(MEDIA_LINK_PATH));
    }

    @Test
    public void serviceDocumentUnauthorizedTest() throws Exception {
        // Attempt to load the ServiceDocument without first authenticating
        ResponseEntity<String> response = getResponseAsString(SERVICE_DOC_PATH);
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        // Attempt to load the ServiceDocument as an Admin user.
        ResponseEntity<String> response = getResponseAsString(SERVICE_DOC_PATH,
                                                              admin.getEmail(), password);
        // Expect a 200 response code, and an ATOM UTF-8 document
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType().toString(), equalTo("application/atomsvc+xml;charset=UTF-8"));

        // Check for SWORD version in response body
        assertThat(response.getBody(), containsString("<sword:version>1.3</sword:version>"));
    }

    @Test
    public void depositUnauthorizedTest() throws Exception {
        // Attempt to access /deposit endpoint without sending authentication information
        ResponseEntity<String> response = postResponseAsString(DEPOSIT_PATH, null, null, null);
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void depositTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a top level community and one Collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        // Make sure our Collection allows the "eperson" user to submit into it
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test SWORDv1 Collection")
                                                 .withSubmitterGroup(eperson)
                                                 .build();
        // Above changes MUST be committed to the database for SWORDv2 to see them.
        context.commit();
        context.restoreAuthSystemState();

        // Specify zip file
        // NOTE: We are using the same "example.zip" as SWORDv2IT because that same ZIP is valid for both v1 and v2
        FileSystemResource zipFile = new FileSystemResource(Path.of("src", "test", "resources", "org",
                                                                    "dspace", "app", "sword2", "example.zip"));

        // Add required headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("example.zip").build());
        headers.set("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
        headers.setAccept(List.of(MediaType.APPLICATION_ATOM_XML));

        //----
        // STEP 1: Verify upload/submit via SWORDv1 works
        //----
        // Send POST to upload Zip file via SWORD
        ResponseEntity<String> response = postResponseAsString(DEPOSIT_PATH + "/" + collection.getHandle(),
                                                               eperson.getEmail(), password,
                                                               new HttpEntity<>(zipFile.getContentAsByteArray(),
                                                                                headers));

        // Expect a 201 CREATED response with ATOM content returned
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(ATOM_CONTENT_TYPE, response.getHeaders().getContentType().toString());

        // MUST return a "Location" header which is the "/sword/media-link/*" URI of the zip file bitstream within
        // the created item (e.g. /sword/media-link/[handle-prefix]/[handle-suffix]/bitstream/[uuid])
        assertNotNull(response.getHeaders().getLocation());
        String mediaLink = response.getHeaders().getLocation().toString();

        // Body should include the SWORD version in generator tag
        MatcherAssert.assertThat(response.getBody(),
                                 containsString("<atom:generator uri=\"http://www.dspace.org/ns/sword/1.3.1\"" +
                                                    " version=\"1.3\"/>"));
        // Verify Item title also is returned in the body
        MatcherAssert.assertThat(response.getBody(), containsString("Attempts to detect retrotransposition"));

        //----
        // STEP 2: Verify /media-link access works
        //----
        // Media-Link URI should work when requested by the EPerson who did the deposit
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth(eperson.getEmail(), password);
        RequestEntity request = RequestEntity.get(mediaLink)
                                             .accept(MediaType.valueOf("application/atom+xml"))
                                             .headers(authHeaders)
                                             .build();
        response = responseAsString(request);

        // Expect a 200 response with ATOM feed content returned
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ATOM_CONTENT_TYPE, response.getHeaders().getContentType().toString());
        // Body should include a link to the zip bitstream in the newly created Item
        // This just verifies "example.zip" exists in the body.
        MatcherAssert.assertThat(response.getBody(), containsString("example.zip"));
    }

    @Test
    public void mediaLinkUnauthorizedTest() throws Exception {
        // Attempt to access /media-link endpoint without sending authentication information
        ResponseEntity<String> response = getResponseAsString(MEDIA_LINK_PATH);
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));

        //NOTE: An authorized /media-link test is performed in depositTest() above.
    }
}

