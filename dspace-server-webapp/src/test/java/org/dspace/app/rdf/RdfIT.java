/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rdf;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

import java.net.URI;

import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.test.AbstractWebClientIntegrationTest;
import org.dspace.content.Community;
import org.dspace.content.service.SiteService;
import org.dspace.rdf.RDFUtil;
import org.dspace.rdf.conversion.RDFConverter;
import org.dspace.rdf.factory.RDFFactoryImpl;
import org.dspace.rdf.storage.RDFStorage;
import org.dspace.rdf.storage.RDFStorageImpl;
import org.dspace.services.ConfigurationService;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify that the /rdf endpoint is responding as a valid RDF endpoint.
 * This tests that our dspace-rdf module is running at this endpoint.
 * <P>
 * This is a AbstractWebClientIntegrationTest because testing dspace-rdf requires
 * running a web server (as dspace-rdf makes use of Servlets, not Controllers).
 * <P>
 * NOTE: At this time, these ITs do NOT run against a real RDF triplestore. Instead,
 * individual tests are expected to mock triplestore responses via a spy-able RDFStorage.
 *
 * @author Tim Donohue
 */
// Ensure the RDF endpoint IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"rdf.enabled = true"})
public class RdfIT extends AbstractWebClientIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private RDFConverter rdfConverter;

    @Autowired
    private RDFFactoryImpl rdfFactory;

    // Create a new spy-able instance of RDFStorage. We will use this instance in all below tests (see @Before)
    // so that we can fake a triplestore backend. No triplestore is used in these tests.
    @Spy
    RDFStorage rdfStorage = new RDFStorageImpl();

    // All RDF paths that we test against
    private final String SERIALIZE_PATH = "/rdf/handle";
    private final String REDIRECTION_PATH = "/rdf/resource";

    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that RDFWebConfig is found/available (as this class deploys RDF)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: RDFWebConfig is provided by the 'dspace-rdf' module
        try {
            Class.forName("org.dspace.app.configuration.RDFWebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }

        // Change the running RDFFactory to use our spy-able, default instance of RDFStorage
        // Again, this lets us fake a triplestore backend in individual tests below.
        rdfFactory.setStorage(rdfStorage);
    }

    @Test
    public void serializationTest() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        // Create a Community
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();
        // Ensure Community is written to test DB immediately
        context.commit();
        context.restoreAuthSystemState();

        // Get the RDF identifiers for this new Community & our Site object
        String communityIdentifier = RDFUtil.generateIdentifier(context, community);
        String siteIdentifier = RDFUtil.generateIdentifier(context, siteService.findSite(context));

        // Mock an RDF triplestore's response by returning the RDF conversion of our Community
        // when rdfStorage.load() is called with the RDF identifier for this Community
        doReturn(rdfConverter.convert(context, community)).when(rdfStorage).load(communityIdentifier);

        // Perform a GET request on the RDF /handle path, using our new Community's Handle
        ResponseEntity<String> response = getResponseAsString(SERIALIZE_PATH + "/" + community.getHandle());
        // Expect a 200 response code, and text/turtle (RDF Turtle syntax) response
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType().toString(), equalTo("text/turtle;charset=UTF-8"));

        // Turtle response should include the RDF identifier of Community
        assertThat(response.getBody(), containsString(communityIdentifier));
        // Turtle response should also note that this Community is part of our Site object
        assertThat(response.getBody(), containsString("dspace:isPartOfRepository  <" + siteIdentifier + "> ;"));
    }

    @Test
    public void redirectionTest() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        // Create a Community
        Community community = CommunityBuilder.createCommunity(context)
                                                    .withName("Test Community")
                                                    .build();
        // Ensure Community is written to test DB immediately (so that a lookup via handle will succeed below)
        context.commit();
        context.restoreAuthSystemState();

        String communityHandle = community.getHandle();

        // Perform a GET request on the RDF /resource path, using this Community's Handle
        ResponseEntity<String> response = getResponseAsString(REDIRECTION_PATH + "/" + communityHandle);
        // Expect a 303 (See Other) response code, redirecting us to the HTTP URI of the Community
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SEE_OTHER));
        // Expect location of redirection to be [dspace.ui.url]/handle/[community_handle]
        assertThat(response.getHeaders().getLocation(), equalTo(
            URI.create(configurationService.getProperty("dspace.ui.url") + "/handle/" + communityHandle + "/")));
    }
}
