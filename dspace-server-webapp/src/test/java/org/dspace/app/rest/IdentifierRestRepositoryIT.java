/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Test getting and registering item identifiers
 *
 * @author Kim Shepherd
 */
public class IdentifierRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Before
    public void setup() throws Exception {
        super.setUp();
    }

    @Test
    public void testValidIdentifier() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a top community to receive an identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        context.restoreAuthSystemState();

        String handle = parentCommunity.getHandle();
        String communityDetail = REST_SERVER_URL + "core/communities/" + parentCommunity.getID();

        getClient().perform(get("/api/pid/find?id={handle}",handle))
                .andExpect(status().isFound())
                //We expect a Location header to redirect to the community details
                .andExpect(header().string("Location", communityDetail));
    }

    @Test

    public void testValidIdentifierItemHandlePrefix() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // Create an item with a handle identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Owning Collection")
                .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
                .withTitle("Test item")
                .build();

        String handle = item.getHandle();
        String itemLocation = REST_SERVER_URL + "core/items/" + item.getID();

        getClient().perform(get("/api/pid/find?id=hdl:{handle}", handle))
                .andExpect(status().isFound())
                // We expect a Location header to redirect to the item's page
                .andExpect(header().string("Location", itemLocation));
    }

    @Test
    public void testUnexistentIdentifier() throws Exception {
        getClient().perform(get("/api/pid/find?id={id}","fakeIdentifier"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Ignore
    /**
     * This test will check the return status code when no id is supplied. It currently fails as our
     * RestResourceController take the precedence over the pid controller returning a 404 Repository not found
     *
     * @throws Exception
     */
    public void testMissingIdentifierParameter() throws Exception {
        getClient().perform(get("/api/pid/find"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testRegisterDoiForItem() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();

        //** GIVEN **
        //1. A community-collection structure with one parent community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

        // This item should not have a DOI
        DOIIdentifierProvider doiIdentifierProvider =
                DSpaceServicesFactory.getInstance().getServiceManager()
                        .getServiceByName("org.dspace.identifier.DOIIdentifierProvider",
                                org.dspace.identifier.DOIIdentifierProvider.class);
        doiIdentifierProvider.delete(context, publicItem1);

        // Body of POST to create an identifier for public item 1
        String uriList = "https://localhost:8080/server/api/core/items/" + publicItem1.getID();

        // A non-admin should get an unauthorised error from REST method preauth
        // Expect first forbidden
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/pid/identifiers")
                        .queryParam("type", "doi")
                        .contentType(MediaType.parseMediaType("text/uri-list"))
                        .content(uriList))
                .andExpect(status().isForbidden());

        // Set token to admin credentials
        token = getAuthToken(admin.getEmail(), password);

        // Expect a successful 201 CREATED for this item with no DOI
        getClient(token).perform(post("/api/pid/identifiers")
                        .queryParam("type", "doi")
                        .contentType(MediaType.parseMediaType("text/uri-list"))
                        .content(uriList))
                .andExpect(status().isCreated());

        // Expected 400 BAD REQUEST status code for a DOI already in REGISTERED / TO_BE_REGISTERED state
        getClient(token).perform(post("/api/pid/identifiers")
                        .queryParam("type", "doi")
                        .contentType(MediaType.parseMediaType("text/uri-list"))
                        .content(uriList))
                .andExpect(status().isBadRequest());

        // Get the doi we minted and queued for registration
        DOI doi = doiService.findDOIByDSpaceObject(context, publicItem1);
        // The DOI should not be null
        assertNotNull(doi);
        // The DOI status should be TO_BE_REGISTERED
        Assert.assertEquals(DOIIdentifierProvider.TO_BE_REGISTERED, doi.getStatus());

        // Now, set the DOI status back to pending and update
        doi.setStatus(DOIIdentifierProvider.PENDING);
        doiService.update(context, doi);

        // Do another POST, again this should return 201 CREATED as we shift the DOI from PENDING to TO_BE_REGISTERED
        getClient(token).perform(post("/api/pid/identifiers")
                        .queryParam("type", "doi")
                        .contentType(MediaType.parseMediaType("text/uri-list"))
                        .content(uriList))
                .andExpect(status().isCreated());

        context.restoreAuthSystemState();
    }

    @Test
    public void testGetIdentifiersForItemByLink() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

        //1. A community-collection structure with one parent community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

        String doiString = "10.5072/dspace-identifier-test-" + publicItem1.getID();

        // Use the DOI service to directly manipulate the DOI on this object so that we can predict and
        // test values via the REST request
        DOI doi = doiService.findDOIByDSpaceObject(context, publicItem1);

        // Assert non-null DOI, since we should be minting them automatically here
        assertNotNull(doi);

        // Set specific string and state we expect to get back from a REST request
        doi.setDoi(doiString);
        doi.setStatus(DOIIdentifierProvider.IS_REGISTERED);
        doiService.update(context, doi);

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Get identifiers for this item - we expect a 200 OK response and the type of the resource is plural
        // "identifiers"
        getClient(token).perform(get("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("identifiers"));

        // Expect an array of identifiers
        getClient(token).perform(get("/api/core/items/" +
                publicItem1.getID().toString() + "/identifiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiers").isArray());

        // Expect a valid DOI with the value, type and status we expect
        getClient(token).perform(get("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiers[0].type").value("identifier"))
                .andExpect(jsonPath("$.identifiers[0].value").value(doiService.DOIToExternalForm(doiString)))
                .andExpect(jsonPath("$.identifiers[0].identifierType").value("doi"))
                .andExpect(jsonPath("$.identifiers[0].identifierStatus")
                        .value(DOIIdentifierProvider.statusText[DOIIdentifierProvider.IS_REGISTERED]));

        // Expect a valid Handle with the value, type we expect
        getClient(token).perform(get("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiers[1].type").value("identifier"))
                .andExpect(jsonPath("$.identifiers[1].value")
                        .value(handleService.getCanonicalForm(publicItem1.getHandle())))
                .andExpect(jsonPath("$.identifiers[1].identifierType").value("handle"));

    }

    @Test
    public void testFindIdentifiersByItem() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

        //1. A community-collection structure with one parent community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

        String doiString = "10.5072/dspace-identifier-test-" + publicItem1.getID();

        // Use the DOI service to directly manipulate the DOI on this object so that we can predict and
        // test values via the REST request
        DOI doi = doiService.findDOIByDSpaceObject(context, publicItem1);

        // Assert non-null DOI, since we should be minting them automatically here
        assertNotNull(doi);

        // Set specific string and state we expect to get back from a REST request
        doi.setDoi(doiString);
        doi.setStatus(DOIIdentifierProvider.IS_REGISTERED);
        doiService.update(context, doi);

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Get identifiers for this item - we expect a 200 OK response and the type of the resource is plural
        // "identifiers"
        getClient(token).perform(get("/api/pid/identifiers/search/findByItem").queryParam("uuid",
                        publicItem1.getID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.identifiers").exists());

        // Expect an array of identifiers
        getClient(token).perform(get("/api/pid/identifiers/search/findByItem").queryParam("uuid",
                        publicItem1.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.identifiers").isArray());

        // Expect a valid DOI with the value, type and status we expect
        getClient(token).perform(get("/api/pid/identifiers/search/findByItem").queryParam("uuid",
                        publicItem1.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.identifiers[0].type").value("identifier"))
                .andExpect(jsonPath("$._embedded.identifiers[0].value").value(doiService.DOIToExternalForm(doiString)))
                .andExpect(jsonPath("$._embedded.identifiers[0].identifierType").value("doi"))
                .andExpect(jsonPath("$._embedded.identifiers[0].identifierStatus")
                        .value(DOIIdentifierProvider.statusText[DOIIdentifierProvider.IS_REGISTERED]));

        // Expect a valid Handle with the value, type we expect
        getClient(token).perform(get("/api/pid/identifiers/search/findByItem").queryParam("uuid",
                        publicItem1.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.identifiers[1].type").value("identifier"))
                .andExpect(jsonPath("$._embedded.identifiers[1].value")
                        .value(handleService.getCanonicalForm(publicItem1.getHandle())))
                .andExpect(jsonPath("$._embedded.identifiers[1].identifierType").value("handle"));

    }
}
