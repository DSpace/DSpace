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

import org.dspace.content.MetadataField;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class MetadataFieldMatcher {

    private MetadataFieldMatcher() { }

    public static Matcher<? super Object> matchMetadataField() {
        return allOf(
            hasJsonPath("$.element", Matchers.not(Matchers.empty())),
            hasJsonPath("$.qualifier", Matchers.not(Matchers.empty())),
            hasJsonPath("$.type", is("metadatafield")),
            hasJsonPath("$._embedded.schema", Matchers.not(Matchers.empty())),
            hasJsonPath("$._links.schema.href", Matchers.containsString("/api/core/metadatafields")),
            hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields"))
        );
    }

    public static Matcher<? super Object> matchMetadataField(MetadataField metadataField) {
        return allOf(
            hasJsonPath("$.element", is(metadataField.getElement())),
            hasJsonPath("$.qualifier", is(metadataField.getQualifier())),
            hasJsonPath("$.type", is("metadatafield")),
            hasJsonPath("$._embedded.schema", Matchers.not(Matchers.empty())),
            hasJsonPath("$._links.schema.href", Matchers.containsString("/api/core/metadatafields")),
            hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields"))
        );
    }

    public static Matcher<? super Object> matchMetadataFieldByKeys(String schema, String element, String qualifier) {
        return allOf(
            hasJsonPath("$.element", is(element)),
            hasJsonPath("$.qualifier", is(qualifier)),
            hasJsonPath("$.type", is("metadatafield")),
            hasJsonPath("$._embedded.schema.prefix", is(schema)),
            hasJsonPath("$._links.schema.href", Matchers.containsString("/api/core/metadatafields")),
            hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields"))
        );
    }

}
