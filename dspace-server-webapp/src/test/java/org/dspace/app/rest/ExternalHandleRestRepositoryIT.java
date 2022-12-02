/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.ExternalHandleMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ClarinHandleBuilder;
import org.dspace.handle.external.ExternalHandleConstants;
import org.dspace.handle.external.Handle;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.services.ConfigurationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;

/**
 * Integration test class for the ExternalHandleRestRepository
 */
public class ExternalHandleRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    HandleClarinService handleClarinService;

    @Autowired
    ConfigurationService configurationService;

    List<org.dspace.handle.Handle> handlesWithMagicURLs = new ArrayList<>();

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        List<String> magicURLs = this.createMagicURLs();

        // create Handles with magicURLs
        int index = 0;
        for (String magicURL : magicURLs) {
            // create Handle

            org.dspace.handle.Handle handle = ClarinHandleBuilder
                    .createHandle(context, "123/" + index, magicURL)
                    .build();
            this.handlesWithMagicURLs.add(handle);
            index++;
        }

        context.commit();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllExternalHandles() throws Exception {
        // call endpoint which should return external handles
        List<Handle> expectedExternalHandles =
                this.handleClarinService.convertHandleWithMagicToExternalHandle(this.handlesWithMagicURLs);

        // expectedExternalHandles should not be empty
        Assert.assertFalse(ObjectUtils.isEmpty(expectedExternalHandles));
        Handle externalHandle = expectedExternalHandles.get(0);

        getClient().perform(get("/api/services/handles/magic"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", ExternalHandleMatcher.matchListOfExternalHandles(
                        expectedExternalHandles
                )))
        ;
    }

    @Test
    public void updateHandle() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        org.dspace.handle.Handle handleToUpdate = this.handlesWithMagicURLs.get(0);
        String handle = handleToUpdate.getHandle();

        String repositoryName = configurationService.getProperty("dspace.name");
        String token = "token0";
        String updatedMagicURL = "@magicLindat@@magicLindat@" + repositoryName +
                "@magicLindat@@magicLindat@@magicLindat@@magicLindat@@magicLindat@@magicLindat@" + token +
                "@magicLindat@https://lindat.mff.cuni.cz/#!/services/pmltq/";
        Handle updatedHandle = new Handle(handle, updatedMagicURL);

        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(put("/api/services/handles")
                .content(mapper.writeValueAsBytes(updatedHandle))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", is(updatedHandle.url)))
        ;
    }

    private List<String> createMagicURLs() {
        // External handle attributes
        String url = "url";
        String title = "title";
        String repository = "repository";
        String submitDate = "submitDate";
        String reporteMail = "reporteMail";
        String datasetName = "datasetName";
        String datasetVersion = "datasetVersion";
        String query = "query";
        String token = "token";
        String subprefix = "subprefix";

        List<String> magicURLs = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            // create mock object
            String magicURL =
                            ExternalHandleConstants.MAGIC_BEAN + title + i +
                            ExternalHandleConstants.MAGIC_BEAN + repository + i +
                            ExternalHandleConstants.MAGIC_BEAN + submitDate + i +
                            ExternalHandleConstants.MAGIC_BEAN + reporteMail + i +
                            ExternalHandleConstants.MAGIC_BEAN + datasetName + i +
                            ExternalHandleConstants.MAGIC_BEAN + datasetVersion + i +
                            ExternalHandleConstants.MAGIC_BEAN + query + i +
                            ExternalHandleConstants.MAGIC_BEAN + token + i +
                            ExternalHandleConstants.MAGIC_BEAN + url + i;
            // add mock object to the magicURLs
            magicURLs.add(magicURL);
        }

        return magicURLs;
    }
}
