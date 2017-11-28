/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class BitstreamMatcher {

    public static Matcher<? super Object> matchBitstreamEntry(String name, UUID uuid) {
        return allOf(
                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                //Check links
                matchBitstreamLinks(uuid)
        );
    }

    private static Matcher<? super Object> matchBitstreamLinks(UUID uuid) {
        return allOf(
                hasJsonPath("$._links.format.href", containsString("/api/core/bitstreams/" + uuid + "/format")),
                hasJsonPath("$._links.self.href", containsString("/api/core/bitstreams/"+uuid)),
                hasJsonPath("$._links.content.href", containsString("/api/core/bitstreams/"+uuid+"/content"))
        );
    }


}
