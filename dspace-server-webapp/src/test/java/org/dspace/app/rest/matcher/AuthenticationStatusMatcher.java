/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import org.hamcrest.Matcher;

/**
 * Created by: Andrew Wood
 * Date: 10 Feb 2020
 */
public class AuthenticationStatusMatcher {

    private AuthenticationStatusMatcher() { }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "eperson",
                "specialGroups"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks() {
        return allOf(
                //FIXME https://github.com/DSpace/DSpace/issues/8274
                hasJsonPath("$._links.eperson.href", containsString("api/eperson/epersons")),
                hasJsonPath("$._links.self.href", containsString("api/authn/status")),
                hasJsonPath("$._links.specialGroups.href", containsString("api/authn/status/specialGroups")));
    }
}
