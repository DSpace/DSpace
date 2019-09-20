/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.dspace.app.rest.matcher.MetadataConfigsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.harvest.OAIHarvester;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test for harvester metadata controller
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public class HarvesterMetadataControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void GetReturnsAllAvailableMetadataFormats() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();

        getClient(token).perform(
            get("/api/config/harvestermetadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                    MetadataConfigsMatcher.matchMetadataConfigs(configs)
                )))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/config/harvestermetadata")));


        getClient().perform(
            get("/api/config/harvestermetadata"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            MetadataConfigsMatcher.matchMetadataConfigs(configs)
                        )))
                        .andExpect(jsonPath("$._links.self.href", endsWith("/api/config/harvestermetadata")));

        token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            get("/api/config/harvestermetadata"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            MetadataConfigsMatcher.matchMetadataConfigs(configs)
                        )))
                        .andExpect(jsonPath("$._links.self.href", endsWith("/api/config/harvestermetadata")));

    }
}
