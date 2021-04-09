/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test via REST for {@link OpenAIREProjectAuthority} class.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Ignore
public class OpenAIREProjectAuthorityIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setup() {
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            "org.dspace.content.authority.OpenAIREProjectAuthority = OpenAIREProjectAuthority");
    }

    @Test
    public void openAIREAuthorityTest() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/OpenAIREProjectAuthority/entries")
            .param("metadata", "dc.contributor.author")
            .param("filter", "openaire"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::openAireProject::777541",
                    "OpenAIRE Advancing Open Scholarship",
                    "OpenAIRE Advancing Open Scholarship(777541)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::openAireProject::731011",
                    "OpenAIRE - CONNECTing scientific results in support of Open Science",
                    "OpenAIRE - CONNECTing scientific results in support of Open Science(731011)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::openAireProject::101017452",
                    "OpenAIRE-Nexus Scholarly Communication Services for EOSC users",
                    "OpenAIRE-Nexus Scholarly Communication Services for EOSC users(101017452)",
                    "vocabularyEntry"))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

    }

}