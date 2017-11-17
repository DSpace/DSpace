/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.*;

public class HitHighlightMatcher {

    public static Matcher<? super Object> entry(String value, String expectedField) {
        return allOf(
                hasJsonPath("$.['"+expectedField+"']", contains(containsString("Public")))
        );
    }


}
