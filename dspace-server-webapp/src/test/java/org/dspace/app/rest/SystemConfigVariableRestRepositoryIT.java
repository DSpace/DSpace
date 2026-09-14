/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration tests for {@link org.dspace.app.rest.repository.SystemConfigVariableRestRepository}.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class SystemConfigVariableRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllAsAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/systemconfigvariables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.systemconfigvariables", not(empty())))
                .andExpect(jsonPath("$._embedded.systemconfigvariables[?(@.key == 'dspace.name')].placeholder")
                        .value("${config.get('dspace.name')}"))
                .andExpect(jsonPath("$._embedded.systemconfigvariables[?(@.key == 'dspace.ui.url')].placeholder")
                        .value("${config.get('dspace.ui.url')}"));
    }

    @Test
    public void findOneAsAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/systemconfigvariables/dspace.name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is("dspace.name")))
                .andExpect(jsonPath("$.placeholder", is("${config.get('dspace.name')}")))
                .andExpect(jsonPath("$.type", is("systemconfigvariable")));
    }

    @Test
    public void findAsAnonymous() throws Exception {
        getClient().perform(get("/api/system/systemconfigvariables"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAsNonAdmin() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/systemconfigvariables"))
                .andExpect(status().isForbidden());
    }
}
