package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.harvest.OAIHarvester;
import org.junit.Test;

/**
 * Integration test for harvester metadata controller
 *
 * @author Jelle Pelgrims (jelle.atmire at gmail.com)
 */
public class HarvesterMetadataControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void GetReturnsAllAvailableMetadataFormats() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();

        Function<String,List<String>> getAllValues =
            key -> configs.stream().map(x -> x.get(key)).collect(Collectors.toList());

        getClient(token).perform(
            get("/api/config/harvestermetadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs[*].id", is(getAllValues.apply("id"))))
                .andExpect(jsonPath("$.configs[*].label", is(getAllValues.apply("label"))))
                .andExpect(jsonPath("$.configs[*].namespace", is(getAllValues.apply("namespace"))))
                .andExpect(jsonPath("$._links.self.href", notNullValue()));
    }
}
