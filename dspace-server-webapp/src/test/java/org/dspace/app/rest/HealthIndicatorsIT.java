/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.configuration.ActuatorConfiguration.UP_WITH_ISSUES_STATUS;
import static org.dspace.app.rest.link.search.HealthIndicatorMatcher.match;
import static org.dspace.app.rest.link.search.HealthIndicatorMatcher.matchDatabase;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;
import org.springframework.boot.actuate.health.Status;

/**
 * Integration tests to verify the health indicators configuration.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class HealthIndicatorsIT extends AbstractControllerIntegrationTest {

    private static final String HEALTH_PATH = "/actuator/health";

    @Test
    public void testWithAnonymousUser() throws Exception {

        getClient().perform(get(HEALTH_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is(UP_WITH_ISSUES_STATUS.getCode())))
            .andExpect(jsonPath("$.components").doesNotExist());

    }

    @Test
    public void testWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get(HEALTH_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is(UP_WITH_ISSUES_STATUS.getCode())))
            .andExpect(jsonPath("$.components").doesNotExist());

    }

    @Test
    public void testWithAdminUser() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get(HEALTH_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is(UP_WITH_ISSUES_STATUS.getCode())))
            .andExpect(jsonPath("$.components", allOf(
                matchDatabase(Status.UP),
                match("solrSearchCore", Status.UP, Map.of("status", 0, "detectedPathType", "root")),
                match("solrStatisticsCore", Status.UP, Map.of("status", 0, "detectedPathType", "root")),
                match("geoIp", UP_WITH_ISSUES_STATUS,
                    Map.of("reason", "The required 'dbfile' configuration is missing in usage-statistics.cfg!"))
                )));

    }
}
