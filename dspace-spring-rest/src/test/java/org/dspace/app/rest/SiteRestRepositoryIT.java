/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.rest.matcher.SiteMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SiteRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception{

        getClient().perform(get("/api/core/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.sites[0]", SiteMatcher.matchEntry()))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/sites")))
                .andExpect(jsonPath("$.page.size", is(20)));

    }
}
