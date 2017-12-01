package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class MetadataFieldMatcher {

    public static Matcher<? super Object> matchMetadataField(){
        return allOf(
                hasJsonPath("$.element", Matchers.not(Matchers.empty())),
                hasJsonPath("$.qualifier", Matchers.not(Matchers.empty())),
                hasJsonPath("$.type", is("metadatafield")),
                hasJsonPath("$._embedded.schema", Matchers.not(Matchers.empty())),
                hasJsonPath("$._links.schema.href", Matchers.containsString("/api/core/metadatafields")),
                hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields"))
        );
    }
}
