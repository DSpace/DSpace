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

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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


    Item publicItem1;
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
        publicItem1 = ItemBuilder.createItem(context, col1)
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

        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .header("X-Forwarded-For", "5.5.5.5"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .header("X-FORWARDED-FOR", "6.6.6.6"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void verifyIPAndPasswordAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", IP_AND_PASS);

        groupService.addMember(context, staff, eperson);

        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .header("X-Forwarded-For", "5.5.5.5"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .header("X-Forwarded-For", "6.6.6.6"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/items/" + publicItem1.getID()))
                        .andExpect(status().isOk());

        getClient(getAuthTokenWithXForwardedForHeader(eperson.getEmail(), password, "6.6.6.6"))
            .perform(get("/api/core/items/" + publicItem1.getID())
                         .header("X-Forwarded-For", "6.6.6.6"))
            .andExpect(status().isOk());
    }

    @Test
    public void verifyPasswordAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS);

        groupService.addMember(context, staff, eperson);

        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .header("X-Forwarded-For", "5.5.5.5"))
                   .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .header("X-Forwarded-For", "6.6.6.6"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/items/" + publicItem1.getID()))
                        .andExpect(status().isOk());

        getClient(getAuthTokenWithXForwardedForHeader(eperson.getEmail(), password, "6.6.6.6"))
            .perform(get("/api/core/items/" + publicItem1.getID())
                         .header("X-Forwarded-For", "6.6.6.6"))
            .andExpect(status().isOk());
    }
}
