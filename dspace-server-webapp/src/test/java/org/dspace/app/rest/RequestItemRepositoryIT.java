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
import static org.exparity.hamcrest.date.DateMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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

    public static final String URI_SINGULAR_ROOT = REST_SERVER_URL
            + RequestItemRest.CATEGORY + '/'
            + RequestItemRest.NAME;

    @Autowired(required = true)
    RequestItemConverter requestItemConverter;

    @Autowired(required = true)
    RequestItemService requestItemService;

    private Collection collection;

    private Item item;

    private Bitstream bitstream;

    @Before
    public void init()
            throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder
                .createCommunity(context)
                .withName("Community")
                .build();

        collection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("Collection")
                .withAdminGroup(eperson)
                .build();

        item = ItemBuilder
                .createItem(context, collection)
                .withTitle("Item")
                .build();

        InputStream is = new ByteArrayInputStream(new byte[0]);
        bitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("Bitstream")
                .build();

        context.restoreAuthSystemState();
    }

    /**
     * Test of findAll method.
     *
     * @throws Exception passed through.
     */
    @Test
    public void testFindAll()
            throws Exception {
        System.out.println("findAll");

        getClient().perform(get(URI_ROOT))
                .andExpect(status().isMethodNotAllowed());
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

        // Create a request.
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        // Test:  can we find it?
        String authToken = getAuthToken(eperson.getEmail(), password);
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

        // Create a request.
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        // Test:  can we find it?
        final String uri = URI_ROOT + '/' + request.getToken();
        getClient().perform(get(uri))
                   .andExpect(status().isOk()) // Can we find it?
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       RequestCopyMatcher.matchRequestCopy(request))));
    }

    /**
     * Test of findOne with an unknown token.
     *
     * @throws Exception passed through.
     */
    @Test
    public void testFindOneNonexistent()
            throws Exception {
        System.out.println("findOne (nonexistent request)");

        String uri = URI_ROOT + "/impossible";
        getClient().perform(get(uri))
                .andExpect(status().isNotFound());
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
        System.out.println("createAndReturn (authenticated)");

        // Fake up a request in REST form.
        RequestItemRest rir = new RequestItemRest();
        rir.setAllfiles(true);
        rir.setItemId(item.getID().toString());
        rir.setRequestEmail(eperson.getEmail());
        rir.setRequestName(eperson.getFullName());
        rir.setRequestMessage(RequestItemBuilder.REQ_MESSAGE);

        // Create it and see if it was created correctly.
        ObjectMapper mapper = new ObjectMapper();
        String authToken = getAuthToken(eperson.getEmail(), password);
        AtomicReference<String> requestTokenRef = new AtomicReference<>();
        try {
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
                            hasJsonPath("$.requestEmail", is(eperson.getEmail())),
                            hasJsonPath("$.requestMessage", is(RequestItemBuilder.REQ_MESSAGE)),
                            hasJsonPath("$.requestName", is(eperson.getFullName())),
                            hasJsonPath("$.allfiles", is(true)),
                            // TODO should be an ISO datetime
                            hasJsonPath("$.requestDate", not(is(emptyOrNullString()))),
                            hasJsonPath("$._links.self.href", not(is(emptyOrNullString())))
                    )))
                    .andDo((var result) -> requestTokenRef.set(
                            read(result.getResponse().getContentAsString(), "token")));
        } finally {
            // Clean up the created request.
            RequestItemBuilder.deleteRequestItem(requestTokenRef.get());
        }
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
        System.out.println("createAndReturn (not authenticated)");

        // Fake up a request in REST form.
        RequestItemRest rir = new RequestItemRest();
        rir.setAllfiles(false);
        rir.setBitstreamId(bitstream.getID().toString());
        rir.setItemId(item.getID().toString());
        rir.setRequestEmail(RequestItemBuilder.REQ_EMAIL);
        rir.setRequestMessage(RequestItemBuilder.REQ_MESSAGE);
        rir.setRequestName(RequestItemBuilder.REQ_NAME);

        // Create it and see if it was created correctly.
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> requestTokenRef = new AtomicReference<>();
        try {
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
                            // TODO should be an ISO datetime
                            hasJsonPath("$.requestDate", not(is(emptyOrNullString()))),
                            hasJsonPath("$._links.self.href", not(is(emptyOrNullString())))
                    )))
                    .andDo((var result) -> requestTokenRef.set(
                            read(result.getResponse().getContentAsString(), "token")));
        } finally {
            // Clean up the created request.
            RequestItemBuilder.deleteRequestItem(requestTokenRef.get());
        }
    }

    /**
     * Test of createAndReturn method, with various errors.
     *
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    @Test
    public void testCreateAndReturnBadRequest()
            throws SQLException, AuthorizeException, IOException, Exception {
        System.out.println("createAndReturn (bad requests)");

        // Fake up a request in REST form.
        RequestItemRest rir = new RequestItemRest();
        rir.setBitstreamId(bitstream.getID().toString());
        rir.setItemId(item.getID().toString());
        rir.setRequestEmail(eperson.getEmail());
        rir.setRequestMessage(RequestItemBuilder.REQ_MESSAGE);
        rir.setRequestName(eperson.getFullName());
        rir.setAllfiles(false);

        // Try to create it, with various malformations.
        ObjectMapper mapper = new ObjectMapper();
        String authToken = getAuthToken(eperson.getEmail(), password);

        // Test missing bitstream ID
        rir.setBitstreamId(null);
        getClient(authToken)
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());

        // Test unknown bitstream ID
        rir.setBitstreamId(UUID.randomUUID().toString());
        getClient(authToken)
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());

        rir.setBitstreamId(bitstream.getID().toString());

        // Test missing item ID
        rir.setItemId(null);
        getClient(authToken)
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());

        // Test unknown item ID
        rir.setItemId(UUID.randomUUID().toString());
        getClient(authToken)
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());

        rir.setItemId(item.getID().toString());

        // Test missing email
        rir.setRequestEmail(null);
        getClient() // Unauthenticated so that email is required.
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());

        // Test bad email
        rir.setRequestEmail("<script>window.location='http://evil.example.com/';</script>");
        getClient() // Unauthenticated so that email is required.
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
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
        System.out.println("testCreateWithInvalidCSRF");

        // Login via password to retrieve a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        // Remove "Bearer " from that token, so that we are left with the token itself
        token = token.replace("Bearer ", "");

        // Save token to an Authorization cookie
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

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

    @Test
    public void testDelete()
            throws Exception {
        System.out.println("delete");

        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();
        getClient().perform(delete(URI_ROOT + '/' + request.getToken()))
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Verify that Spring Security's CORS settings are working as we expect.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testUntrustedOrigin()
            throws Exception {
        System.out.println("testUntrustedOrigin");

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
     * Test of put method, of class RequestItemRepository.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testPut()
            throws Exception {
        System.out.println("put");

        // Create an item request to approve.
        RequestItem itemRequest = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        // Create the HTTP request body.
        Map<String, String> parameters = Map.of(
                "acceptRequest", "true",
                "subject", "subject",
                "responseMessage", "Request accepted",
                "suggestOpenAccess", "true");
        String content = new ObjectMapper()
                .writer()
                .writeValueAsString(parameters);

        // Send the request.
        String authToken = getAuthToken(eperson.getEmail(), password);
        AtomicReference<String> requestTokenRef = new AtomicReference<>();
        getClient(authToken).perform(put(URI_ROOT + '/' + itemRequest.getToken())
                .contentType(contentType)
                .content(content))
                .andExpect(status().isOk()
                )
                .andDo((var result) -> requestTokenRef.set(
                        read(result.getResponse().getContentAsString(), "token")));
        RequestItem foundRequest
                = requestItemService.findByToken(context, requestTokenRef.get());
        assertTrue("acceptRequest should be true", foundRequest.isAccept_request());
        assertThat("decision_date must be within a minute of now",
                foundRequest.getDecision_date(),
                within(1, ChronoUnit.MINUTES, new Date()));
    }

    @Test
    public void testPutUnauthenticated()
            throws Exception {
        System.out.println("put unauthenticated request");
        RequestItem itemRequest = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        Map<String, String> parameters;
        String content;

        ObjectWriter mapperWriter = new ObjectMapper().writer();

        // Unauthenticated user should be allowed.
        parameters = Map.of(
                "acceptRequest", "true",
                "subject", "put unauthenticated",
                "responseMessage", "Request accepted",
                "suggestOpenAccess", "false");

        content = mapperWriter.writeValueAsString(parameters);
        getClient().perform(put(URI_ROOT + '/' + itemRequest.getToken())
                .contentType(contentType)
                .content(content))
                .andExpect(status().isOk());
    }

    @Test
    public void testPutBadRequest()
            throws Exception {
        System.out.println("put bad requests");

        // Create an item request to approve.
        RequestItem itemRequest = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        String authToken;
        Map<String, String> parameters;
        String content;

        ObjectWriter mapperWriter = new ObjectMapper().writer();

        // Missing acceptRequest
        parameters = Map.of(
                "subject", "subject",
                "responseMessage", "Request accepted");
        content = mapperWriter.writeValueAsString(parameters);
        authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(put(URI_ROOT + '/' + itemRequest.getToken())
                .contentType(contentType)
                .content(content))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testPutCompletedRequest()
            throws Exception {
        System.out.println("put completed request");

        // Create an item request that is already denied.
        RequestItem itemRequest = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(false)
                .withDecisionDate(new Date())
                .build();

        // Try to accept it again.
        Map<String, String> parameters = Map.of(
                "acceptRequest", "true",
                "subject", "subject",
                "responseMessage", "Request accepted");
        ObjectWriter mapperWriter = new ObjectMapper().writer();
        String content = mapperWriter.writeValueAsString(parameters);
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(put(URI_ROOT + '/' + itemRequest.getToken())
                .contentType(contentType)
                .content(content))
                .andExpect(status().isUnprocessableEntity());
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
}
