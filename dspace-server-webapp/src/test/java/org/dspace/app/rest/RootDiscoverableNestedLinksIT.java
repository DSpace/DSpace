/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

public class RootDiscoverableNestedLinksIT extends AbstractControllerIntegrationTest {

    @Test
    public void rootDiscoverableNestedLinksTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links",Matchers.allOf(
                            hasJsonPath("$.authorizations.href",
                                     is("http://localhost/api/authz/authorizations")),
                            hasJsonPath("$.authorization-search.href",
                                     is("http://localhost/api/authz/authorization/search")),
                            hasJsonPath("$.resourcepolicies.href",
                                     is("http://localhost/api/authz/resourcepolicies")),
                            hasJsonPath("$.resourcepolicy-search.href",
                                     is("http://localhost/api/authz/resourcepolicy/search")),
                            hasJsonPath("$.claimedtasks.href",
                                     is("http://localhost/api/workflow/claimedtasks")),
                            hasJsonPath("$.claimedtask-search.href",
                                     is("http://localhost/api/workflow/claimedtask/search")),
                            hasJsonPath("$.pooltasks.href",
                                     is("http://localhost/api/workflow/pooltasks")),
                            hasJsonPath("$.pooltask-search.href",
                                     is("http://localhost/api/workflow/pooltask/search")),
                            hasJsonPath("$.eperson-registration.href",
                                     is("http://localhost/api/eperson/registrations"))
                        )));
    }

}
