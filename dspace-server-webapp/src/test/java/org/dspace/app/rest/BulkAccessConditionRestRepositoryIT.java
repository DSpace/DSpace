/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.AccessConditionOptionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test class for the bulkaccessconditionoptions endpoint.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessConditionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllByAdminUserTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/config/bulkaccessconditionoptions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$._embedded.bulkaccessconditionoptions", containsInAnyOrder(allOf(
                hasJsonPath("$.id", is("default")),
                hasJsonPath("$.itemAccessConditionOptions", Matchers.containsInAnyOrder(
                    AccessConditionOptionMatcher.matchAccessConditionOption("openaccess", false , false, null, null),
                    AccessConditionOptionMatcher.matchAccessConditionOption("embargo", true , false, "+36MONTHS", null),
                    AccessConditionOptionMatcher.matchAccessConditionOption("administrator", false , false, null, null),
                    AccessConditionOptionMatcher.matchAccessConditionOption("lease", false , true, null, "+6MONTHS"))
                ),
                hasJsonPath("$.bitstreamAccessConditionOptions", Matchers.containsInAnyOrder(
                    AccessConditionOptionMatcher.matchAccessConditionOption("openaccess", false , false, null, null),
                    AccessConditionOptionMatcher.matchAccessConditionOption("embargo", true , false, "+36MONTHS", null),
                    AccessConditionOptionMatcher.matchAccessConditionOption("administrator", false , false, null, null),
                    AccessConditionOptionMatcher.matchAccessConditionOption("lease", false , true, null, "+6MONTHS"))
                )))));
    }

    @Test
    public void findAllByAdminUserOfAnCommunityTest() throws Exception {

        context.turnOffAuthorisationSystem();

        // create community and assign eperson to admin group
            CommunityBuilder.createCommunity(context)
                            .withName("community")
                            .withAdminGroup(eperson)
                        .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/config/bulkaccessconditionoptions"))
                            .andExpect(status().isOk());
    }

    @Test
    public void findAllByAdminUserOfAnCollectionTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community =
            CommunityBuilder.createCommunity(context)
                            .withName("community")
                        .build();

        // create collection and assign eperson to admin group
        CollectionBuilder.createCollection(context, community)
                         .withName("collection")
                         .withAdminGroup(eperson)
                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/config/bulkaccessconditionoptions"))
                            .andExpect(status().isOk());
    }

    @Test
    public void findAllByAdminUserOfAnItemTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community =
            CommunityBuilder.createCommunity(context)
                            .withName("community")
                        .build();

        Collection collection =
            CollectionBuilder.createCollection(context, community)
                             .withName("collection")
                             .build();

        // create item and assign eperson as admin user
        ItemBuilder.createItem(context, collection)
                   .withTitle("item")
                   .withAdminUser(eperson)
                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/config/bulkaccessconditionoptions"))
                            .andExpect(status().isOk());
    }

    @Test
    public void findAllByNormalUserTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/config/bulkaccessconditionoptions"))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findAllByAnonymousUserTest() throws Exception {
        getClient().perform(get("/api/config/bulkaccessconditionoptions"))
                            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneByAdminTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin)
            .perform(get("/api/config/bulkaccessconditionoptions/default"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("default")))
            .andExpect(jsonPath("$.itemAccessConditionOptions", Matchers.containsInAnyOrder(
                AccessConditionOptionMatcher.matchAccessConditionOption("openaccess", false , false, null, null),
                AccessConditionOptionMatcher.matchAccessConditionOption("embargo", true , false, "+36MONTHS", null),
                AccessConditionOptionMatcher.matchAccessConditionOption("administrator", false , false, null, null),
                AccessConditionOptionMatcher.matchAccessConditionOption("lease", false , true, null, "+6MONTHS"))
            ))
            .andExpect(jsonPath("$.bitstreamAccessConditionOptions", Matchers.containsInAnyOrder(
                AccessConditionOptionMatcher.matchAccessConditionOption("openaccess", false , false, null, null),
                AccessConditionOptionMatcher.matchAccessConditionOption("embargo", true , false, "+36MONTHS", null),
                AccessConditionOptionMatcher.matchAccessConditionOption("administrator", false , false, null, null),
                AccessConditionOptionMatcher.matchAccessConditionOption("lease", false , true, null, "+6MONTHS"))
            ))
            .andExpect(jsonPath("$.type", is("bulkaccessconditionoption")));
    }

    @Test
    public void findOneByAdminOfAnCommunityTest() throws Exception {

        context.turnOffAuthorisationSystem();

        // create community and assign eperson to admin group
        CommunityBuilder.createCommunity(context)
                        .withName("community")
                        .withAdminGroup(eperson)
                        .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken)
            .perform(get("/api/config/bulkaccessconditionoptions/default"))
            .andExpect(status().isOk());
    }

    @Test
    public void findOneByAdminOfAnCollectionTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community =
            CommunityBuilder.createCommunity(context)
                            .withName("community")
                            .build();

        // create collection and assign eperson to admin group
        CollectionBuilder.createCollection(context, community)
                         .withName("collection")
                         .withAdminGroup(eperson)
                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken)
            .perform(get("/api/config/bulkaccessconditionoptions/default"))
            .andExpect(status().isOk());
    }

    @Test
    public void findOneByAdminOfAnItemTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community =
            CommunityBuilder.createCommunity(context)
                            .withName("community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, community)
                             .withName("collection")
                             .build();

        // create item and assign eperson as admin user
        ItemBuilder.createItem(context, collection)
                   .withTitle("item")
                   .withAdminUser(eperson)
                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/config/bulkaccessconditionoptions/default"))
                            .andExpect(status().isOk());
    }

    @Test
    public void findOneByNormalUserTest() throws Exception {
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson)
            .perform(get("/api/config/bulkaccessconditionoptions/default"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findOneByAnonymousUserTest() throws Exception {
        getClient().perform(get("/api/config/bulkaccessconditionoptions/default"))
                   .andExpect(status().isUnauthorized());
    }


    @Test
    public void findOneNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/config/bulkaccessconditionoptions/wrong"))
                            .andExpect(status().isNotFound());
    }

}