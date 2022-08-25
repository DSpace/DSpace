/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.hdlresolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca@4science.com)
 *
 */
public class HdlResolverRestControllerIT extends AbstractControllerIntegrationTest {

    /**
     * Verifies that any mapped <code>hdlIdentifier</code> returns the
     * corresponding <code>handle URL</code>
     * 
     * @throws Exception
     * 
     */
    @Test
    public void givenMappedIdentifierWhenCallHdlresolverThenReturnsMappedURL() throws Exception {
        context.turnOffAuthorisationSystem();

        // ** START GIVEN **

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withLogo("TestingContentForLogo").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1).withTitle("Public item 1").withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John").withSubject("ExtraEntry")
                .withHandle("123456789/testHdlResolver").build();

        context.restoreAuthSystemState();

        // ** END GIVEN **

        ResultMatcher matchHandleResponse = jsonPath("$[0]",
                StringContains.containsString("123456789/testHdlResolver"));
        getClient()
            .perform(get(HdlResolverRestController.LISTPREFIXES + publicItem1.getHandle()))
            .andExpect(status().isOk())
            .andExpect(matchHandleResponse);
        getClient()
            .perform(get(HdlResolverRestController.RESOLVE  + publicItem1.getHandle()))
            .andExpect(status().isOk())
            .andExpect(matchHandleResponse);
        getClient()
            .perform(get(HdlResolverRestController.HDL_RESOLVER + publicItem1.getHandle()))
            .andExpect(status().isOk())
            .andExpect(matchHandleResponse);
        getClient()
            .perform(get(HdlResolverRestController.LISTPREFIXES + publicItem1.getHandle()))
            .andExpect(status().isOk())
            .andExpect(matchHandleResponse);
        getClient()
            .perform(get("/wrongController/" + publicItem1.getHandle()))
            .andExpect(status().isNotFound());

    }

    /**
     * Verifies that a null hdlIdentifier returns a
     * 
     * <code>HttpStatus.BAD_REQUEST</code>
     * 
     * @throws Exception
     * @throws SQLException
     * 
     */
    @Test
    public void givenNullHdlIdentifierWhenCallHdlresolverThenReturnsBadRequest() throws Exception {

        getClient().perform(
                get(HdlResolverRestController.HDL_RESOLVER + "null"))
                .andExpect(status().isBadRequest());
        getClient().perform(
                get(HdlResolverRestController.RESOLVE + "null"))
                .andExpect(status().isBadRequest());
        getClient().perform(
                get(HdlResolverRestController.LISTHANDLES + "null"))
                .andExpect(status().isBadRequest());
        getClient().perform(
                get(HdlResolverRestController.LISTPREFIXES + "null"))
                .andExpect(status().isBadRequest());

    }

    /**
     * Verifies that an empty hdlIdentifier returns a
     * <code>HttpStatus.BAD_REQUEST</code>
     * 
     * @throws Exception
     * @throws SQLException
     * 
     */
    @Test
    public void givenEmptyHdlIdentifierWhenCallHdlresolverThenReturnsNull() throws Exception {

        getClient()
            .perform(get(HdlResolverRestController.HDL_RESOLVER + " "))
            .andExpect(status().isBadRequest());
        getClient()
            .perform(get(HdlResolverRestController.RESOLVE + " "))
            .andExpect(status().isBadRequest());
        getClient()
            .perform(get(HdlResolverRestController.LISTHANDLES + " "))
            .andExpect(status().isBadRequest());
        getClient()
            .perform(get(HdlResolverRestController.LISTPREFIXES + " "))
            .andExpect(status().isBadRequest());

    }

    /**
     * Verifies that any unmapped hdlIdentifier returns a null response
     * 
     * @throws Exception
     * 
     */
    @Test
    public void givenIdentifierNotMappedWhenCallHdlresolverThenReturnsNull() throws Exception {
        getClient()
            .perform(get(HdlResolverRestController.HDL_RESOLVER + "testHdlResolver/2"))
            .andExpect(status().isOk()).andExpect(content().string("null"));
        getClient()
            .perform(get(HdlResolverRestController.RESOLVE + "testHdlResolver/2"))
            .andExpect(status().isOk()).andExpect(content().string("null"));
        getClient()
            .perform(get(HdlResolverRestController.LISTHANDLES + "testHdlResolver/2"))
            .andExpect(status().isOk()).andExpect(content().string("null"));
        getClient()
            .perform(get(HdlResolverRestController.LISTPREFIXES + "testHdlResolver/2"))
            .andExpect(status().isOk()).andExpect(content().string("null"));
    }
}
