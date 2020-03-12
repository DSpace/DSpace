/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.VersionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class VersionRestRepositoryIT extends AbstractControllerIntegrationTest {

    Item item;
    Version version;

    @Autowired
    private ItemService itemService;

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConfigurationService configurationService;

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
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        item = ItemBuilder.createItem(context, col1)
                          .withTitle("Public item 1")
                          .withIssueDate("2017-10-17")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();
        version = versioningService.createNewVersion(context, item);
        context.restoreAuthSystemState();
    }

    @Test
    public void findOneTest() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))));
    }

    @Test
    public void findOneEPersonLinkVisisbleTest() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))))
                             .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)));
    }

    @Test
    public void findOneEPersonLinkConfigurationPropertyFalseAdminUserLinkVisibleTest() throws Exception {

        configurationService.setProperty("versioning.item.history.include.submitter", false);

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))))
                             .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)));

        configurationService.setProperty("versioning.item.history.include.submitter", true);

    }

    @Test
    public void findOneEPersonLinkConfigurationPropertyTrueNormalUserLinkVisibleTest() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))))
                             .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)));

    }

    @Test
    public void findOneEPersonLinkConfigurationPropertyTrueAnonUserLinkVisibleTest() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))))
                             .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)));

    }

    @Test
    public void findOneEPersonLinkConfigurationPropertyFalseNormalUserLinkInvisibleTest() throws Exception {

        configurationService.setProperty("versioning.item.history.include.submitter", false);

        String adminToken = getAuthToken(eperson.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))))
                             .andExpect(jsonPath("$._links.eperson.href").doesNotExist());

        configurationService.setProperty("versioning.item.history.include.submitter", true);

    }
    @Test
    public void findOneUnauthorizedTest() throws Exception {

        getClient().perform(get("/api/versioning/versions/" + version.getID()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {

        configurationService.setProperty("versioning.item.history.view.admin", true);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/versioning/versions/" + version.getID()))
                        .andExpect(status().isForbidden());
        configurationService.setProperty("versioning.item.history.view.admin", false);
    }

    @Test
    public void versionForItemTest() throws Exception {

        getClient().perform(get("/api/core/items/" + version.getItem().getID() + "/version"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version))));

    }

    @Test
    public void versionEPersonTest() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID() + "/eperson"))
                             .andExpect(status().isOk())
                             .andExpect(
                                 jsonPath("$", Matchers.is(EPersonMatcher.matchEPersonEntry(item.getSubmitter()))));
    }

    @Test
    public void versionEPersonTestConfigurationPropertyFalseAndAdminSucces() throws Exception {

        configurationService.setProperty("versioning.item.history.include.submitter", false);
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID() + "/eperson"))
                             .andExpect(status().isOk())
                             .andExpect(
                                 jsonPath("$", Matchers.is(EPersonMatcher.matchEPersonEntry(item.getSubmitter()))));
        configurationService.setProperty("versioning.item.history.include.submitter", true);
    }

    @Test
    public void versionEPersonTestConfigurationPropertyFalseAndNormalUserAccessDenied() throws Exception {

        configurationService.setProperty("versioning.item.history.include.submitter", false);
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/versioning/versions/" + version.getID() + "/eperson"))
                             .andExpect(status().isForbidden());
        configurationService.setProperty("versioning.item.history.include.submitter", true);
    }

    @Test
    public void versionEPersonTestConfigurationPropertyFalseAndAnonAccessDenied() throws Exception {

        configurationService.setProperty("versioning.item.history.include.submitter", false);

        getClient().perform(get("/api/versioning/versions/" + version.getID() + "/eperson"))
                        .andExpect(status().isUnauthorized());
        configurationService.setProperty("versioning.item.history.include.submitter", true);
    }

    @Test
    public void versionEPersonTestWrongId() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + ((version.getID() + 5) * 57) + "/eperson"))
                             .andExpect(status().isNotFound());
    }
    @Test
    public void versionItemTest() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + version.getID() + "/item"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemProperties(version.getItem()))));
    }

    @Test
    public void versionItemTestWrongId() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/versioning/versions/" + ((version.getID() + 5) * 57) + "/item"))
                             .andExpect(status().isNotFound());
    }
}
