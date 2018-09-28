/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RequestItemBuilder;
import org.dspace.app.rest.matcher.RequestCopyMatcher;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemRepositoryIT
        extends AbstractControllerIntegrationTest {
    /** Where to find {@link RequestItem}s in the local URL namespace. */
    public static final String URI_ROOT = "/api/"
            + RequestItemRest.CATEGORY + '/'
            + RequestItemRest.NAME + 's';

/*
    @BeforeClass
    public static void setUpClass() {
    }
*/

/*
    @AfterClass
    public static void tearDownClass() {
    }
*/

/*
    @Before
    public void setUp() {
    }
*/

/*
    @After
    public void tearDown() {
    }
*/

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
        Community community = CommunityBuilder.createCommunity(context)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
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

        // Test:  was it created correctly?
        final String uri = URI_ROOT + '/'
                + request.getToken();
/*
        getClient().perform(get(uri))
                   .andExpect(status().isOk()) // Can we find it?
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       RequestCopyMatcher.matchRequestCopy(request))));
*/
        //try {
        MockMvc client = getClient();
        MockHttpServletRequestBuilder get = get(uri);
        ResultActions response = client.perform(get);
        response.andExpect(status().isOk()); // Can we find it?
        response.andExpect(content().contentType(contentType));
        response.andExpect(jsonPath("$", Matchers.is(
                       RequestCopyMatcher.matchRequestCopy(request))));
        //} catch (Exception e) {
        //    System.err.println(e.getMessage());
        //}

        // Clean up.
        bitstream.setDeleted(true);
        context.restoreAuthSystemState();
    }

    /**
     * Test of findAll method, of class RequestItemRepository.
     */
/*
    @Test
    public void testFindAll()
    {
        System.out.println("findAll");
        Context context = null;
        Pageable pageable = null;
        RequestItemRepository instance = new RequestItemRepository();
        Page<RequestItemRest> expResult = null;
        Page<RequestItemRest> result = instance.findAll(context, pageable);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of save method, of class RequestItemRepository.
     */
/*
    @Test
    public void testSave()
    {
        System.out.println("save");
        Context context = null;
        RequestItemRest ri = null;
        RequestItemRepository instance = new RequestItemRepository();
        RequestItemRest expResult = null;
        RequestItemRest result = instance.save(context, ri);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class RequestItemRepository.
     */
/*
    @Test
    public void testDelete()
            throws Exception
    {
        System.out.println("delete");
        Context context = null;
        String token = "";
        RequestItemRepository instance = new RequestItemRepository();
        instance.delete(context, token);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getDomainClass method, of class RequestItemRepository.
     */
/*
    @Test
    public void testGetDomainClass()
    {
        System.out.println("getDomainClass");
        RequestItemRepository instance = new RequestItemRepository();
        Class<RequestItemRest> expResult = null;
        Class<RequestItemRest> result = instance.getDomainClass();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of wrapResource method, of class RequestItemRepository.
     */
/*
    @Test
    public void testWrapResource()
    {
        System.out.println("wrapResource");
        RequestItemRest model = null;
        String[] rels = null;
        RequestItemRepository instance = new RequestItemRepository();
        RequestItemResource expResult = null;
        RequestItemResource result = instance.wrapResource(model, rels);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
