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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration Tests against the /api/config/properties/[property] endpoint
 */
public class ConfigurationRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Test
    public void getSingleValue() throws Exception {
        getClient().perform(get("/api/config/properties/configuration.exposed.single.value"))
            .andExpect(jsonPath("$.values[0]", is("public_value")))
            .andExpect(jsonPath("$.type", is("property")))
            .andExpect(jsonPath("$.name", is("configuration.exposed.single.value")))
            .andExpect(jsonPath("$._links.self.href", is("http://localhost/api/config/properties/configuration.exposed.single.value")));
    }

    @Test
    public void getArrayValue() throws Exception {
        getClient().perform(get("/api/config/properties/configuration.exposed.array.value"))
        .andExpect(jsonPath("$.values[0]", is("public_value_1")))
        .andExpect(jsonPath("$.values[1]", is("public_value_2")));
    }

    @Test
    public void getNonExistingValue() throws Exception {
        getClient().perform(get("/api/config/properties/configuration.not.existing"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getNonExposedValue() throws Exception {
        getClient().perform(get("/api/config/properties/configuration.not.exposed"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getAll() throws Exception {
        getClient().perform(get("/api/config/properties/"))
            .andExpect(status().isMethodNotAllowed());
    }
}
