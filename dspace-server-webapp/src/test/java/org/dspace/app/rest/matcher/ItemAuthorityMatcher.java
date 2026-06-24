/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for ItemAuthority
 *
 * @author Mykhaylo Boychuk (4Science.it)
 */
public class ItemAuthorityMatcher {

    private ItemAuthorityMatcher() {
    }

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
                                                                                  String value, String type,
                                                                                  String otherInfMetadata,
                                                                                  String metadataValue) {
        return allOf(
            hasJsonPath("$.authority", is(authority)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.otherInformation", aMapWithSize(2)),
            hasJsonPath("$.otherInformation['" + otherInfMetadata + "']", is(metadataValue)),
            hasJsonPath("$.otherInformation['" + "data-" + otherInfMetadata + "']", is(metadataValue))
        );
    }

    public static Matcher<? super Object> matchItemAuthorityWithOtherInformations(String authority, String display,
                                                                                  String value, String type,
                                                                                  String otherInfMetadata,
                                                                                  String metadataValue, String source) {
        return allOf(
            hasJsonPath("$.authority", is(authority)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.source", is(source)),
            hasJsonPath("$.otherInformation", aMapWithSize(2)),
            hasJsonPath("$.otherInformation['" + otherInfMetadata + "']", is(metadataValue)),
            hasJsonPath("$.otherInformation['" + "data-" + otherInfMetadata + "']", is(metadataValue))
        );
    }

    public static Matcher<? super Object> matchItemAuthorityWithOtherInformations(
        String authority,
        String display,
        String value,
        String type,
        Map<String, String> otherInformation
    ) {
        return allOf(
            hasJsonPath("$.authority", is(authority)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.otherInformation", is(otherInformation))
        );
    }

    public static Matcher<? super Object> matchItemAuthorityWithOtherInformations(String authority, String display,
                                                                                  String value, String type,
                                                                                  Map<String, String> otherInformation,
                                                                                  String source) {
        return allOf(
            hasJsonPath("$.authority", is(authority)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.otherInformation", is(otherInformation)),
            hasJsonPath("$.source", is(source))
        );
    }

    public static Matcher<? super Object> matchItemAuthorityWithTwoMetadataInOtherInformations(
        String authority,
        String display,
        String value,
        String type,
        String firstOtherMetadata,
        String firstOtherValue,
        String secondOtherMetadata,
        String secondOtherValue,
        String source
    ) {
        return allOf(
            hasJsonPath("$.authority", is(authority)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.source", is(source)),
            hasJsonPath("$.otherInformation", aMapWithSize(2)),
            allOf(
                hasJsonPath("$.otherInformation." + firstOtherMetadata, is(firstOtherValue)),
                hasJsonPath("$.otherInformation." + secondOtherMetadata, is(secondOtherValue))
            )
        );
    }

    public static Matcher<? super Object> matchItemAuthorityWithTwoMetadataInOtherInformations(
        String authority,
        String display,
        String value,
        String type,
        Map<String, String> orcidAndAffiliation
    ) {

        return allOf(
            hasJsonPath("$.authority", is(authority)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.otherInformation", aMapWithSize(4)),
            allOf(
                hasJsonPath("$.otherInformation", is(orcidAndAffiliation))
            )
        );
    }
}
