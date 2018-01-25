/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.dspace.content.MetadataSchema;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class MetadataschemaMatcher {

    public static Matcher<? super Object> matchEntry(){
        return allOf(
                hasJsonPath("$.prefix", Matchers.not(Matchers.empty())),
                hasJsonPath("$.namespace", Matchers.not(Matchers.empty())),
                hasJsonPath("$.type", is("metadataschema")),
                hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadataschemas"))
        );
    }

    public static Matcher<? super Object> matchEntry(MetadataSchema metadataSchema){
        return allOf(
                hasJsonPath("$.prefix", is(metadataSchema.getName())),
                hasJsonPath("$.namespace", is(metadataSchema.getNamespace())),
                hasJsonPath("$.type", is("metadataschema")),
                hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/metadataschemas"))
        );
    }
}
