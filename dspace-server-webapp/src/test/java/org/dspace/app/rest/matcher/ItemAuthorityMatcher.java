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

/**
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 */
public class ItemAuthorityMatcher {

    private ItemAuthorityMatcher() {}

    public static Matcher<? super Object> matchItemAuthorityProperties(String authority, String display, String value,
            String type) {
        return allOf(
                hasJsonPath("$.authority", is(authority)),
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.type", is(type))
        );
    }

    public static Matcher<? super Object> matchItemAuthorityWithOtherInformations(String authority, String display,
            String value, String type, String otherInfMetadata, String metadataValue) {
        return allOf(
                hasJsonPath("$.authority", is(authority)),
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.type", is(type)),
                hasJsonPath("$.otherInformation.data-dc_" + otherInfMetadata, is(metadataValue))
        );
    }
}
