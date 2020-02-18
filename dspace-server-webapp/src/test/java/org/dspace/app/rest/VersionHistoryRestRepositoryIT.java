/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.VersionHistoryMatcher;
import org.dspace.app.rest.matcher.VersionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class VersionHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {


    VersionHistory versionHistory;
    Item item;
    Version version;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private VersioningService versioningService;

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        versionHistory = versionHistoryService.create(context);
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
        version = versioningService.createNewVersion(context, versionHistory, item, "test", new Date(), 0);
        context.restoreAuthSystemState();
    }

    @Test
    public void findOneTest() throws Exception {
        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", is(VersionHistoryMatcher.matchEntry(versionHistory))));

    }


    @Test
    public void findOneForbiddenTest() throws Exception {

        configurationService.setProperty("versioning.item.history.view.admin", true);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/versioning/versionhistories/" + versionHistory.getID()))
                        .andExpect(status().isForbidden());
        configurationService.setProperty("versioning.item.history.view.admin", false);
    }

    @Test
    public void findOneWrongIDTest() throws Exception {
        int wrongVersionHistoryId = (versionHistory.getID() + 5) * 57;
        getClient().perform(get("/api/versioning/versionhistories/" + wrongVersionHistoryId))
                   .andExpect(status().isNotFound());

    }
    @Test
    public void findVersionsOneWrongIDTest() throws Exception {
        int wrongVersionHistoryId = (versionHistory.getID() + 5) * 57;
        getClient().perform(get("/api/versioning/versionhistories/" + wrongVersionHistoryId + "/versions"))
                   .andExpect(status().isNotFound());

    }

    @Test
    public void findVersionsOfVersionHistoryTest() throws Exception {
        Version version = versionHistoryService.getFirstVersion(context, versionHistory);
        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", contains(VersionMatcher.matchEntry(version))));

        context.turnOffAuthorisationSystem();
        Version secondVersion = versioningService
            .createNewVersion(context, versionHistory, item, "test", new Date(), 0);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", containsInAnyOrder(VersionMatcher.matchEntry(version),
                                                                                  VersionMatcher
                                                                                      .matchEntry(secondVersion))));

    }
    @Test
    public void findVersionsOfVersionHistoryPaginationTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Version version = versionHistoryService.getFirstVersion(context, versionHistory);
        Version secondVersion = versioningService
            .createNewVersion(context, versionHistory, item, "test", new Date(), 0);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions")
                                .param("size", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", contains(VersionMatcher.matchEntry(secondVersion))))
                   .andExpect(jsonPath("$._embedded.versions",
                                       Matchers.not(contains(VersionMatcher.matchEntry(version)))));
        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions")
                                .param("size", "1")
                                .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", contains(VersionMatcher.matchEntry(version))))
                   .andExpect(jsonPath("$._embedded.versions",
                                       Matchers.not(contains(VersionMatcher.matchEntry(secondVersion)))));

    }

}

