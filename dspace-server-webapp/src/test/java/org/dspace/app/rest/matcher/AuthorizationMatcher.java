/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.dspace.app.rest.authorization.Authorization;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an Authorization object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class AuthorizationMatcher {

    private AuthorizationMatcher() { }

    /**
     * Check if the returned json expose all the required links and properties
     * 
     * @param authz
     *            the authorization, if null only the presence of the generic properties will be verified
     * @return
     */
    public static Matcher<? super Object> matchAuthorization(Authorization authz) {
        return allOf(
                // Check authorization properties
                matchProperties(authz),
                // Check links
                matchLinks(authz));
    }

    /**
     * Check that the id and type are exposed
     * 
     * @param authz
     *            the authorization, if null only the presence of the generic properties will be verified
     * @return
     */
    public static Matcher<? super Object> matchProperties(Authorization authz) {
        if (authz != null) {
            return allOf(
                    hasJsonPath("$.id", is(authz.getID())),
                    hasJsonPath("$.type", is("authorization"))
            );
        } else {
            return allOf(
                    hasJsonPath("$.id"),
                    hasJsonPath("$.type", is("authorization"))
            );
        }
    }

    /**
     * Check that the required links are present
     * 
     * @param authz
     *            the authorization, if null only the presence of the generic properties will be verified
     * @return
     */
    public static Matcher<? super Object> matchLinks(Authorization authz) {
        if (authz != null) {
            return allOf(
                    hasJsonPath("$._links.self.href",
                            is(REST_SERVER_URL + "authz/authorizations/" + authz.getID())),
                    hasJsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)),
                    hasJsonPath("$._links.feature.href", startsWith(REST_SERVER_URL)),
                    hasJsonPath("$._links.object.href", startsWith(REST_SERVER_URL)));
        } else {
            return allOf(
                    hasJsonPath("$._links.self.href"),
                    hasJsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)),
                    hasJsonPath("$._links.feature.href", startsWith(REST_SERVER_URL)),
                    hasJsonPath("$._links.object.href", startsWith(REST_SERVER_URL)));
            }
    }
}
