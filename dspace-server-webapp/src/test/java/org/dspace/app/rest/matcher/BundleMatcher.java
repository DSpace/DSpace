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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class BundleMatcher {

    private BundleMatcher() {
    }


    public static Matcher<? super Object> matchBundle(String name,
                                                      UUID uuid,
                                                      String handle,
                                                      int type,
                                                      List<Bitstream> bitstreams) {
        return allOf(
            hasJsonPath("$.uuid", is(uuid.toString())),
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.handle", is(handle)),
            hasJsonPath("$.type", is(Constants.typeText[type].toLowerCase())),
            hasJsonPath("$.metadata", Matchers.allOf(
                MetadataMatcher.matchMetadata("dc.title", name)
            )),
            hasJsonPath("$._embedded.bitstreams._embedded.bitstreams", Matchers.containsInAnyOrder(
                bitstreams
                    .stream()
                    .map(x -> BitstreamMatcher.matchBitstreamEntry(x.getID(), x.getSizeBytes()))
                    .collect(Collectors.toList())
            )),
            matchLinks(uuid)
        );
    }

    private static Matcher<? super Object> matchLinks(UUID uuid) {
        return allOf(
            hasJsonPath("$._links.primaryBitstream.href", endsWith("/api/core/bundles/" + uuid + "/primaryBitstream")),
            hasJsonPath("$._links.bitstreams.href", endsWith("/api/core/bundles/" + uuid + "/bitstreams")),
            hasJsonPath("$._links.self.href", endsWith("/api/core/bundles/" + uuid))
        );
    }

}
