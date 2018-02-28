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

import org.hamcrest.Matcher;

public class EPersonMetadataMatcher {

    private EPersonMetadataMatcher() { }

    public static Matcher<? super Object> matchFirstName(String firstName) {
        return allOf(
            hasJsonPath("$.key", is("eperson.firstname")),
            hasJsonPath("$.value", is(firstName))
        );
    }

    public static Matcher<? super Object> matchLastName(String lastName) {
        return allOf(
            hasJsonPath("$.key", is("eperson.lastname")),
            hasJsonPath("$.value", is(lastName))
        );
    }

}
