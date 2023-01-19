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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test getting and registering item identifiers
 *
 * @author Kim Shepherd
 */
public class ItemIdentifierControllerIT extends AbstractControllerIntegrationTest {
    @Before
    public void setup() throws Exception {
        super.setUp();
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

        // A non-admin should get an unauthorised error from REST method preauth
        // Expect first forbidden
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers?type=doi"))
                .andExpect(status().isForbidden());

        // Set token to admin credentials
        token = getAuthToken(admin.getEmail(), password);

        // Expect a successful 201 CREATED for this item with no DOI
        getClient(token).perform(post("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers?type=doi"))
                .andExpect(status().isCreated());

        // Expected 302 FOUND status code for a DOI already in REGISTERED / TO_BE_REGISTERED state
        getClient(token).perform(post("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers?type=doi"))
                .andExpect(status().isFound());

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
        getClient(token).perform(post("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers"))
                .andExpect(status().isCreated());

        context.restoreAuthSystemState();
    }

    @Test
    public void testGetIdentifiersForItem() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();

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
                        .value(DOIIdentifierProvider.IS_REGISTERED.toString()));

        // Expect a valid Handle with the value, type we expect
        getClient(token).perform(get("/api/core/items/" +
                        publicItem1.getID().toString() + "/identifiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiers[1].type").value("identifier"))
                .andExpect(jsonPath("$.identifiers[1].value").value(publicItem1.getHandle()))
                .andExpect(jsonPath("$.identifiers[1].identifierType").value("handle"));

    }
}
