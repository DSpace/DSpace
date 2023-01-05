/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.hdlresolver;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.handle.hdlresolver.HdlResolverServiceImpl;
import org.dspace.services.ConfigurationService;
import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca@4science.com)
 *
 */
public class HdlResolverRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

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
            .perform(get(HdlResolverRestController.LISTHANDLES + publicItem1.getHandle()))
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
            .perform(get("/wrongController/" + publicItem1.getHandle()))
            .andExpect(status().isNotFound());

    }

    @Test
    public void givenAnyHandlesWhenDisabledListhandleThenReturnsNotFoundResp()
            throws Exception {
        this.configurationService.setProperty(HdlResolverServiceImpl.LISTHANDLES_HIDE_PROP, true);
        try {
            context.turnOffAuthorisationSystem();
            parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                    .withLogo("TestingContentForLogo").build();

            String handlePrefix = "123456789/PREFIX";
            ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .withHandle(handlePrefix)
                .build();
            context.restoreAuthSystemState();

            getClient()
                .perform(get(HdlResolverRestController.LISTHANDLES))
                .andExpect(status().isNotFound());

            getClient()
                .perform(get(HdlResolverRestController.LISTHANDLES + handlePrefix))
                .andExpect(status().isNotFound());

            getClient()
                .perform(get(HdlResolverRestController.LISTHANDLES + "anyHandlePrefixHere"))
                .andExpect(status().isNotFound());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.configurationService.setProperty(HdlResolverServiceImpl.LISTHANDLES_HIDE_PROP, false);
        }

    }

    @Test
    public void givenMappedHandlesWhenCalledListHandlesWithoutPrefixThenReturnsBadRequestResp()
        throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withLogo("TestingContentForLogo").build();

        String handlePrefix = "123456789/PREFIX";
        ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .withHandle(handlePrefix)
            .build();
        context.restoreAuthSystemState();

        getClient()
            .perform(get(HdlResolverRestController.LISTHANDLES))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenMappedHandlesWhenCalledListHandlesWithPrefixThenReturnsAllHandlesWithThatPrefix()
            throws Exception {
        context.turnOffAuthorisationSystem();

        // ** START GIVEN **

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withLogo("TestingContentForLogo").build();

        String handlePrefix = "123456789/PREFIX";
        ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .withHandle(handlePrefix)
            .build();

        String handlePrefix1 = "123456789/PREFIX1";
        ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .withHandle(handlePrefix1)
            .build();

        String noHandle = "123456789/NOPREFIX";
        ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .withHandle(noHandle)
            .build();

        String testHandle = "123456789/TEST";
        ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .withHandle(testHandle)
            .build();

        context.restoreAuthSystemState();

        // ** END GIVEN **

        ResultMatcher matchHandleResponse =
                jsonPath("$[*]",
                        allOf(
                            containsInAnyOrder(handlePrefix, handlePrefix1),
                            not(containsInAnyOrder(noHandle, testHandle))
                        )
                );

        getClient()
            .perform(get(HdlResolverRestController.LISTHANDLES + handlePrefix))
            .andExpect(status().isOk())
            .andExpect(matchHandleResponse);

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
    }

    @Test
    public void givenMappedPrefixWhenNoAdditionalPrefixesConfThenReturnsHandlePrefix() throws Exception {
        String handlePrefix = this.configurationService.getProperty("handle.prefix");
        ResultMatcher matchHandleResponse =
            jsonPath("$[*]",
                    allOf(
                            containsInAnyOrder(handlePrefix)
                    )
            );

        getClient()
            .perform(get(HdlResolverRestController.LISTPREFIXES))
            .andExpect(status().isOk())
            .andExpect(matchHandleResponse);
    }

    @Test
    public void givenMappedPrefixWhenAdditionalPrefixesConfThenReturnsAllOfThem() throws Exception {
        String handlePrefix = this.configurationService.getProperty("handle.prefix");
        String[] defaultValue = this.configurationService.getArrayProperty("handle.additional.prefixes");
        try {
            String additionalPrefixes = "additional1,additional2";
            this.configurationService.setProperty("handle.additional.prefixes", additionalPrefixes);

            List<String> validPrefixes = Arrays.asList(additionalPrefixes.split(","));
            validPrefixes.add(handlePrefix);
            ResultMatcher matchHandleResponse =
                    jsonPath("$[*]",
                            allOf(
                                containsInAnyOrder(validPrefixes)
                            )
                    );

            getClient()
                .perform(get(HdlResolverRestController.LISTPREFIXES))
                .andExpect(status().isOk())
                .andExpect(matchHandleResponse);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.configurationService.setProperty("handle.additional.prefixse", defaultValue);
        }
    }
}
