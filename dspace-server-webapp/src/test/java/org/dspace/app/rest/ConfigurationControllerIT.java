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

public class ConfigurationControllerIT extends AbstractControllerIntegrationTest {
    @Test
    public void getSingleValue() throws Exception {
        getClient().perform(get("/api/config/properties/configuration.exposed.single.value"))
            .andExpect(jsonPath("$[0]", is("public_value")));
    }

    @Test
    public void getArrayValue() throws Exception {
        getClient().perform(get("/api/config/properties/configuration.exposed.array.value"))
        .andExpect(jsonPath("$[0]", is("public_value_1")))
        .andExpect(jsonPath("$[1]", is("public_value_2")));
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
}
