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
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;

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
import org.springframework.test.web.servlet.MvcResult;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemRepositoryIT
        extends AbstractControllerIntegrationTest {
    /** Where to find {@link RequestItem}s in the local URL namespace. */
    public static final String URI_ROOT = REST_SERVER_URL
            + RequestItemRest.CATEGORY + '/'
            + RequestItemRest.NAME + 's';

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
     * Test of findOne method, of class RequestItemRepository.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testFindOne()
            throws Exception {
        System.out.println("findOne");

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

        // Test:  can we find it?
        final String uri = URI_ROOT + '/'
                + request.getToken();
        getClient().perform(get(uri))
                   .andExpect(status().isOk()) // Can we find it?
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       RequestCopyMatcher.matchRequestCopy(request))));

        // Clean up.
        bitstream.setDeleted(true);
        context.restoreAuthSystemState();
    }

    /**
     * Test of createAndReturn method, of class RequestItemRepository.
     *
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    @Test
    public void testCreateAndReturn()
            throws SQLException, AuthorizeException, IOException, Exception {
        System.out.println("createAndReturn");

        context.turnOffAuthorisationSystem();

        // Create some necessary objects.
        Collection col = CollectionBuilder.createCollection(context,
                parentCommunity).build();
        Item item = ItemBuilder.createItem(context, col).build();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                .withName("/dev/null")
                .withMimeType("text/plain")
                .build();

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
        MvcResult mvcResult = getClient(authToken)
                .perform(post(URI_ROOT)
                        .content(mapper.writeValueAsBytes(rir))
                        .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", not(isEmptyOrNullString())),
                        hasJsonPath("$.type", is(RequestItemRest.NAME)),
                        hasJsonPath("$.token", not(isEmptyOrNullString())),
                        hasJsonPath("$.requestEmail", is(RequestItemBuilder.REQ_EMAIL)),
                        hasJsonPath("$.requestMessage", is(RequestItemBuilder.REQ_MESSAGE)),
                        hasJsonPath("$.requestName", is(RequestItemBuilder.REQ_NAME)),
                        hasJsonPath("$.allfiles", is(false)),
                        hasJsonPath("$.requestDate", not(isEmptyOrNullString())), // TODO should be an ISO datetime
                        hasJsonPath("$._links.self.href", not(isEmptyOrNullString()))
                )))
                .andReturn();

        // Clean up the created request.
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String requestToken = String.valueOf(map.get("token"));
        RequestItem ri = requestItemService.findByToken(context, requestToken);
        requestItemService.delete(context, ri);

        context.restoreAuthSystemState();
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
