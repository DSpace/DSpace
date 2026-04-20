/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.hamcrest.Matcher;

public class RegistrationMatcher {

    private RegistrationMatcher() {}

    public static Matcher<? super Object> matchRegistration(String email, UUID epersonUuid) {
        return allOf(
            hasJsonPath("$.email", is(email)),
            hasJsonPath("$.user", is(epersonUuid == null ? null : String.valueOf(epersonUuid)))
        );

    }
}
