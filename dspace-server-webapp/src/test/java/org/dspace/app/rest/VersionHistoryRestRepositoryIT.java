/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.matcher.VersionHistoryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class VersionHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {


    VersionHistory versionHistory;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        versionHistory = versionHistoryService.create(context);
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


}

