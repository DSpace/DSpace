/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Testing class for the {@link org.dspace.app.rest.security.AnonymousAdditionalAuthorizationFilter} filter
 */
public class AnonymousAdditionalAuthorizationFilterIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    public static final String[] IP = {"org.dspace.authenticate.IPAuthentication"};
    public static final String[] IP_AND_PASS =
        {"org.dspace.authenticate.IPAuthentication",
            "org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] PASS = {"org.dspace.authenticate.PasswordAuthentication"};


    Item staffAccessItem1;
    Group staff;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        staff = GroupBuilder.createGroup(context).withName("Staff").build();

        //2. Three public items that are readable by Anonymous with different subjects
        staffAccessItem1 = ItemBuilder.createItem(context, col1)
                                 .withTitle("Public item 1")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                 .withSubject("ExtraEntry")
                                 .withReaderGroup(staff)
                                 .build();

    }

    @Test
    public void verifyIPAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", IP);

        // Make sure that the item is not accessible for anonymous
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()))
                   .andExpect(status().isUnauthorized());

        // Test that we can access the item using the IP that's configured for the Staff group
        // (in our test environment's local.cfg)
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()).with(ip("5.5.5.5")))
                   .andExpect(status().isOk());

        // Test that we also can access the item using that same IP via a proxy
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID())
                                .header("X-Forwarded-For", "5.5.5.5"))
                   .andExpect(status().isOk());

        // Test that we can't access the item using the IP that's configured for the Students group
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()).with(ip("6.6.6.6")))
                   .andExpect(status().isUnauthorized());

        // Test that we also can't also access the item using that same IP via a proxy
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID())
                                .header("X-Forwarded-For", "6.6.6.6"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void verifyIPAndPasswordAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", IP_AND_PASS);

        groupService.addMember(context, staff, eperson);

        // Make sure that the item is not accessible for anonymous
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()))
                   .andExpect(status().isUnauthorized());

        // Test that we can access the item using the IP that's configured for the Staff group
        // (in our test environment's local.cfg)
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()).with(ip("5.5.5.5")))
                   .andExpect(status().isOk());

        // Test that we can also access the item using that same IP via a proxy
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID())
                                .header("X-Forwarded-For", "5.5.5.5"))
                   .andExpect(status().isOk());

        // Test that we can't access the item using the IP that's configured for the Students group
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()).with(ip("6.6.6.6")))
                   .andExpect(status().isUnauthorized());

        // Test that we also can't also access the item using that same IP via a proxy
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID())
                                .header("X-Forwarded-For", "6.6.6.6"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);

        // Test that the user in the Staff group can access the Item with the normal password authentication
        getClient(token).perform(get("/api/core/items/" + staffAccessItem1.getID()))
                        .andExpect(status().isOk());

        // Test that the user in the Staff group can access the Item with the normal password authentication even
        // when it's IP is configured to be part of the students group
        getClient(getAuthTokenWithXForwardedForHeader(eperson.getEmail(), password, "6.6.6.6"))
            .perform(get("/api/core/items/" + staffAccessItem1.getID())
                         .header("X-Forwarded-For", "6.6.6.6"))
            .andExpect(status().isOk());
    }

    @Test
    public void verifyPasswordAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS);

        groupService.addMember(context, staff, eperson);

        // Make sure that the item is not accessible for anonymous
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID()))
                   .andExpect(status().isUnauthorized());

        // Test that the Item can't be accessed with the IP for the Staff group if the config is turned off and only
        // allows password authentication
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID())
                                .header("X-Forwarded-For", "5.5.5.5"))
                   .andExpect(status().isUnauthorized());

        // Test that the Item can't be accessed with the IP for the Students group if the config is turned off and only
        // allows password authentication
        getClient().perform(get("/api/core/items/" + staffAccessItem1.getID())
                                .header("X-Forwarded-For", "6.6.6.6"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);

        // Test that the Item is accessible for a user in the Staff group by password login
        getClient(token).perform(get("/api/core/items/" + staffAccessItem1.getID()))
                        .andExpect(status().isOk());

        // Test that the Item is accessible for a user in the Staff group by password Login when the request
        // is coming from the IP that's configured to be for the Student group
        getClient(getAuthTokenWithXForwardedForHeader(eperson.getEmail(), password, "6.6.6.6"))
            .perform(get("/api/core/items/" + staffAccessItem1.getID())
                         .header("X-Forwarded-For", "6.6.6.6"))
            .andExpect(status().isOk());
    }
}
