/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.dspace.content.Bitstream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class CollectionMatcher {

    public static Matcher<? super Object> matchCollectionEntry(String name, UUID uuid, String handle) {
        return matchCollectionEntry(name, uuid, handle, null);
    }

    public static Matcher<? super Object> matchCollectionEntry(String name, UUID uuid, String handle, Bitstream logo) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.handle", is(handle)),
                hasJsonPath("$.type", is("collection")),
                hasJsonPath("$.metadata", Matchers.contains(
                        CollectionMetadataMatcher.matchTitle(name)
                )),
                matchLinks(uuid),
                matchLogo(logo)
        );
    }

    private static Matcher<? super Object> matchLinks(UUID uuid){
        return allOf(
                hasJsonPath("$._links.logo.href", containsString("api/core/collections/" + uuid.toString() + "/logo")),
                hasJsonPath("$._links.self.href", containsString("api/core/collections/" + uuid.toString()))
        );
    }

    private static Matcher<? super Object> matchLogo(Bitstream logo) {
        return logo == null ?
                allOf(
                    hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty()))
                ) :
                allOf(
                    hasJsonPath("$._embedded.logo", BitstreamMatcher.matchBitstreamEntry(logo.getID(), logo.getSize()))
                );
    }

}
