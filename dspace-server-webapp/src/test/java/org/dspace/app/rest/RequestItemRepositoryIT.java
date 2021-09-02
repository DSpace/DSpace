/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.converter.RequestItemConverter;
import org.dspace.app.rest.matcher.RequestCopyMatcher;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.repository.RequestItemRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RequestItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemRepositoryIT
        extends AbstractControllerIntegrationTest {
    /** Where to find {@link RequestItem}s in the local URL namespace. */
    public static final String URI_ROOT = REST_SERVER_URL
            + RequestItemRest.CATEGORY + '/'
            + RequestItemRest.PLURAL_NAME;

    @Autowired(required = true)
    RequestItemConverter requestItemConverter;

    @Autowired(required = true)
    RequestItemService requestItemService;

    @Before
    public void init() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName(
                "Parent Community").build();
        context.restoreAuthSystemState();
    }

    /**
     * Test of findOne method, with an authenticated user.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testFindOneAuthenticated()
            throws Exception {
        System.out.println("findOne (authenticated)");

        context.turnOffAuthorisationSystem();

        // Create necessary supporting objects.
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .build();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                .build();

        // Create a request.
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        context.restoreAuthSystemState();

        // Test:  can we find it?
        String authToken = getAuthToken(admin.getEmail(), password);
        final String uri = URI_ROOT + '/' + request.getToken();
        getClient(authToken).perform(get(uri))
                   .andExpect(status().isOk()) // Can we find it?
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       RequestCopyMatcher.matchRequestCopy(request))));
    }

    /**
     * Test of findOne method, with an UNauthenticated user.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testFindOneNotAuthenticated()
            throws Exception {
        System.out.println("findOne (not authenticated)");

        context.turnOffAuthorisationSystem();

        // Create necessary supporting objects.
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .build();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                .build();

        // Create a request.
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        context.restoreAuthSystemState();

        // Test:  can we find it?
        final String uri = URI_ROOT + '/' + request.getToken();
        getClient().perform(get(uri))
                   .andExpect(status().isOk()) // Can we find it?
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       RequestCopyMatcher.matchRequestCopy(request))));
    }

    /**
     * Test of createAndReturn method, with an authenticated user.
     *
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    @Test
    public void testCreateAndReturnAuthenticated()
            throws SQLException, AuthorizeException, IOException, Exception {
        System.out.println("createAndReturn");

        // Create some necessary objects.
        context.turnOffAuthorisationSystem();

        Collection col = CollectionBuilder.createCollection(context,
                parentCommunity).build();
        Item item = ItemBuilder.createItem(context, col).build();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                .withName("/dev/null")
                .withMimeType("text/plain")
                .build();

        context.restoreAuthSystemState();

        // Fake up a request in REST form.
        RequestItemRest rir = new RequestItemRest();
        rir.setBitstreamId(bitstream.getID().toString());
        rir.setItemId(item.getID().toString());
        rir.setRequestEmail(RequestItemBuilder.REQ_EMAIL);
        rir.setRequestMessage(RequestItemBuilder.REQ_MESSAGE);
        rir.setRequestName(RequestItemBuilder.REQ_NAME);
        rir.setAllfiles(false);

        // Create it and see if it was created correctly.
        ObjectMapper mapper = new ObjectMapper();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", not(is(emptyOrNullString()))),
                        hasJsonPath("$.type", is(RequestItemRest.NAME)),
                        hasJsonPath("$.token", not(is(emptyOrNullString()))),
                        hasJsonPath("$.requestEmail", is(RequestItemBuilder.REQ_EMAIL)),
                        hasJsonPath("$.requestMessage", is(RequestItemBuilder.REQ_MESSAGE)),
                        hasJsonPath("$.requestName", is(RequestItemBuilder.REQ_NAME)),
                        hasJsonPath("$.allfiles", is(false)),
                        hasJsonPath("$.requestDate", not(is(emptyOrNullString()))), // TODO should be an ISO datetime
                        hasJsonPath("$._links.self.href", not(is(emptyOrNullString())))
                )))
                .andDo((var result) -> saveToken(read(result.getResponse().getContentAsString(), "token")))
                .andReturn();

        // Clean up the created request.
        RequestItemBuilder.deleteRequestItem(requestToken);
    }

    /**
     * Test of createAndReturn method, with an UNauthenticated user.
     * This should succeed:  anyone can file a request.
     *
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    @Test
    public void testCreateAndReturnNotAuthenticated()
            throws SQLException, AuthorizeException, IOException, Exception {
        System.out.println("createAndReturn");

        // Create some necessary objects.
        context.turnOffAuthorisationSystem();

        Collection col = CollectionBuilder.createCollection(context,
                parentCommunity).build();
        Item item = ItemBuilder.createItem(context, col).build();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                .withName("/dev/null")
                .withMimeType("text/plain")
                .build();

        context.restoreAuthSystemState();

        // Fake up a request in REST form.
        RequestItemRest rir = new RequestItemRest();
        rir.setBitstreamId(bitstream.getID().toString());
        rir.setItemId(item.getID().toString());
        rir.setRequestEmail(RequestItemBuilder.REQ_EMAIL);
        rir.setRequestMessage(RequestItemBuilder.REQ_MESSAGE);
        rir.setRequestName(RequestItemBuilder.REQ_NAME);
        rir.setAllfiles(false);

        // Create it and see if it was created correctly.
        ObjectMapper mapper = new ObjectMapper();
        getClient().perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", not(is(emptyOrNullString()))),
                        hasJsonPath("$.type", is(RequestItemRest.NAME)),
                        hasJsonPath("$.token", not(is(emptyOrNullString()))),
                        hasJsonPath("$.requestEmail", is(RequestItemBuilder.REQ_EMAIL)),
                        hasJsonPath("$.requestMessage", is(RequestItemBuilder.REQ_MESSAGE)),
                        hasJsonPath("$.requestName", is(RequestItemBuilder.REQ_NAME)),
                        hasJsonPath("$.allfiles", is(false)),
                        hasJsonPath("$.requestDate", not(is(emptyOrNullString()))), // TODO should be an ISO datetime
                        hasJsonPath("$._links.self.href", not(is(emptyOrNullString())))
                )))
                .andDo((var result) -> saveToken(read(result.getResponse().getContentAsString(), "token")))
                .andReturn();

        // Clean up the created request.
        RequestItemBuilder.deleteRequestItem(requestToken);
    }

    /**
     * Verify that Spring Security's CSRF protection is working as we expect.
     * We must test this using a simple non-GET request, as CSRF Tokens are not
     * validated in a GET request.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testCreateWithInvalidCSRF()
            throws Exception {
        // Login via password to retrieve a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        // Remove "Bearer " from that token, so that we are left with the token itself
        token = token.replace("Bearer ", "");

        // Save token to an Authorization cookie
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

        // Create some necessary objects.
        context.turnOffAuthorisationSystem();

        Collection col = CollectionBuilder.createCollection(context,
                parentCommunity).build();
        Item item = ItemBuilder.createItem(context, col).build();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                .withName("/dev/null")
                .withMimeType("text/plain")
                .build();

        context.restoreAuthSystemState();

        // Fake up a request in REST form.
        RequestItemRest rir = new RequestItemRest();
        rir.setBitstreamId(bitstream.getID().toString());
        rir.setItemId(item.getID().toString());
        rir.setRequestEmail(RequestItemBuilder.REQ_EMAIL);
        rir.setRequestMessage(RequestItemBuilder.REQ_MESSAGE);
        rir.setRequestName(RequestItemBuilder.REQ_NAME);
        rir.setAllfiles(false);

        ObjectMapper mapper = new ObjectMapper();
        getClient().perform(post(URI_ROOT)
                .content(mapper.writeValueAsBytes(rir))
                .contentType(contentType)
                .with(csrf().useInvalidToken().asHeader())
                .secure(true)
                .cookie(cookies))
                // Should return a 403 Forbidden, for an invalid CSRF token
                .andExpect(status().isForbidden())
                // Verify it includes our custom error reason (from DSpaceApiExceptionControllerAdvice)
                .andExpect(status().reason(containsString("Invalid CSRF token")))
                // And, a new/updated token should be returned (as both server-side cookie and header)
                // This is handled by DSpaceAccessDeniedHandler
                .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                .andExpect(header().exists("DSPACE-XSRF-TOKEN"));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    /**
     * Verify that Spring Security's CORS settings are working as we expect.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testUntrustedOrigin()
            throws Exception {
        // First, get a valid login token
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify token works
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token cannot be used from an *untrusted* Origin
        // (NOTE: this Origin is NOT listed in our 'rest.cors.allowed-origins' configuration)
        getClient(token).perform(get(URI_ROOT)
                .header("Origin", "https://example.org"))
                // should result in a 403 error as Spring Security
                //returns that for untrusted origins
                .andExpect(status().isForbidden());

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    /**
     * Test of getDomainClass method, of class RequestItemRepository.
     */
    @Test
    public void testGetDomainClass() {
        System.out.println("getDomainClass");
        RequestItemRepository instance = new RequestItemRepository();
        Class instanceClass = instance.getDomainClass();
        assertEquals("Wrong domain class", RequestItemRest.class, instanceClass);
    }

    /** Saves the request token generated by creating an item request. */
    private String requestToken;

    /**
     * Silly work-around because lambdas can't mutate external variables.
     * @param token a request token to be remembered.
     */
    private void saveToken(String token) {
        requestToken = token;
    }
}
