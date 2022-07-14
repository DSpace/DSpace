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

import org.dspace.content.MetadataValue;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Utility class to construct a Matcher for an item
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class MetadataValueMatcher {

    private MetadataValueMatcher() { }

    public static Matcher<? super Object> matchMetadataValue(MetadataValue metadataValue) {
        return allOf(
                hasJsonPath("$.value", is(metadataValue.getValue())),
                hasJsonPath("$.language", is(metadataValue.getLanguage())),
                hasJsonPath("$.type", is("metadatavalue")),
                hasJsonPath("$.authority", is(metadataValue.getAuthority())),
                hasJsonPath("$.confidence", is(metadataValue.getConfidence())),
                hasJsonPath("$.place", is(metadataValue.getPlace())),
                hasJsonPath("$._embedded.field", Matchers.not(Matchers.empty())),
                hasJsonPath("$._links.field.href", Matchers.containsString("/api/core/metadatavalues")),
                hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatavalues"))
        );
    }

    public static Matcher<? super Object> matchMetadataValueByKeys(String value, String language, String authority,
                                                                   int confidence, int place) {
        return allOf(
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.language", is(language)),
                hasJsonPath("$.type", is("metadatavalue")),
                hasJsonPath("$.authority", is(authority)),
                hasJsonPath("$.confidence", is(confidence)),
                hasJsonPath("$.place", is(place)),
                hasJsonPath("$._embedded.field", Matchers.not(Matchers.empty())),
                hasJsonPath("$._links.field.href", Matchers.containsString("/api/core/metadatavalues")),
                hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatavalues"))
        );
    }
}
