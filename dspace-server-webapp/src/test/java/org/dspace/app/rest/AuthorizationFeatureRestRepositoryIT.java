/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.dspace.app.rest.authorization.AlwaysTrueFeature;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the Authorization Feature endpoint
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorizationFeatureRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    private AuthorizationFeatureService authzFeatureService;

    /**
     * All the features should be returned.
     *
     * @throws Exception
     */
    @Test
    public void findAllTest() throws Exception {
        int featuresNum = authzFeatureService.findAll().size();
        int expReturn = featuresNum > 20 ? 20 : featuresNum;
        String adminToken = getAuthToken(admin.getEmail(), password);

        // verify that only the admin can access the endpoint (see subsequent call in the method)
        getClient(adminToken).perform(get("/api/authz/features")).andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.features", Matchers.hasSize(is(expReturn))))
                             .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/authz/features")))
                             .andExpect(jsonPath("$.page.size", is(20)))
                             .andExpect(jsonPath("$.page.totalElements", is(featuresNum)));
        // verify that anonymous user cannot access
        getClient().perform(get("/api/authz/features")).andExpect(status().isUnauthorized());
        // verify that normal user cannot access
        String epersonAuthToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonAuthToken).perform(get("/api/authz/features")).andExpect(status().isForbidden());

    }

    /**
     * The feature endpoint must provide proper pagination. Unauthorized and
     * forbidden scenarios are managed in the findAllTest
     *
     * @throws Exception
     */
    @Test
    public void findAllWithPaginationTest() throws Exception {
        int featuresNum = authzFeatureService.findAll().size();

        String adminToken = getAuthToken(admin.getEmail(), password);
        List<String> featureIDs = new ArrayList<>();
        for (int page = 0; page < featuresNum; page++) {
            AtomicReference<String> idRef = new AtomicReference<>();

            getClient(adminToken)
                .perform(get("/api/authz/features").param("page", String.valueOf(page)).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.features", Matchers.hasSize(is(1))))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/authz/features")))
                .andExpect(
                    (page == 0) ? jsonPath("$._links.prev.href").doesNotExist()
                        : jsonPath("$._links.prev.href", Matchers.containsString("/api/authz/features")))
                .andExpect((page == featuresNum - 1)
                               ? jsonPath("$._links.next.href").doesNotExist()
                               : jsonPath("$._links.next.href", Matchers.containsString("/api/authz/features")))
                .andExpect(jsonPath("$._links.first.href", Matchers.containsString("/api/authz/features")))
                .andExpect(jsonPath("$._links.last.href", Matchers.containsString("/api/authz/features")))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(Integer.valueOf(featuresNum))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$._embedded.features[0].id")));

            if (idRef.get() == null || featureIDs.contains(idRef.get())) {
                fail("Duplicate feature " + idRef.get() + " returned at page " + page);
            }
            featureIDs.add(idRef.get());
        }
    }

    /**
     * The feature resource endpoint must expose the proper structure and be
     * reserved to administrators.
     *
     * @throws Exception
     */
    @Test
    public void findOneTest() throws Exception {
        getClient().perform(get("/api/authz/features/withdrawItem")).andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is("withdrawItem")))
                   .andExpect(jsonPath("$.description", Matchers.any(String.class)))
                   .andExpect(jsonPath("$.resourcetypes", Matchers.contains("core.item")))
                   .andExpect(jsonPath("$.type", is("feature")));
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        getClient().perform(get("/api/authz/features/not-existing-feature")).andExpect(status().isNotFound());

    }

    /**
     * It should be possible to find features by resourcetype. The endpoint is only available to administrators
     *
     * @throws Exception
     */
    @Test
    public void findByResourceTypeTest() throws Exception {
        AuthorizationFeature alwaysTrueFeature = authzFeatureService.find(AlwaysTrueFeature.NAME);
        String adminToken = getAuthToken(admin.getEmail(), password);
        for (String type : alwaysTrueFeature.getSupportedTypes()) {
            // verify that only the admin can access the endpoint (see subsequent call in the method)
            getClient(adminToken).perform(get("/api/authz/features/search/resourcetype").param("type", type))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$",
                                                     JsonPathMatchers.hasJsonPath("$._embedded.features",
                                                                                  Matchers.everyItem(
                                                                                      JsonPathMatchers.hasJsonPath(
                                                                                          "$.resourcetypes",
                                                                                          Matchers.hasItem(is(type))))
                                                     )))
                                 .andExpect(
                                     jsonPath("$._links.self.href",
                                              Matchers.containsString("/api/authz/features/search/resourcetype")));
        }
        // verify that the right response code is returned also for not existing types
        getClient(adminToken).perform(get("/api/authz/features/search/resourcetype").param("type", "NOT-EXISTING"))
                             .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", is(0)));
        // verify that anonymous user cannot access, without information disclosure
        getClient().perform(get("/api/authz/features/search/resourcetype").param("type", "core.item"))
                   .andExpect(status().isUnauthorized());
        // verify that normal user cannot access, without information disclosure
        String epersonAuthToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonAuthToken).perform(get("/api/authz/features/search/resourcetype").param("type", "core.item"))
                                   .andExpect(status().isForbidden());

    }

}
