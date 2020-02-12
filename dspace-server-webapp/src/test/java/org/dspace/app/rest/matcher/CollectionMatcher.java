/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.dspace.content.Bitstream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class CollectionMatcher {

    private CollectionMatcher() {
    }

    public static Matcher<? super Object> matchCollectionEntry(String name, UUID uuid, String handle) {
        return matchCollectionEntry(name, uuid, handle, null);
    }

    public static Matcher<? super Object> matchCollectionEntry(String name, UUID uuid, String handle, Bitstream logo) {
        return allOf(
                matchProperties(name, uuid, handle),
                matchLinks(uuid),
                matchLogo(logo)
        );
    }

    public static Matcher<? super Object> matchProperties(String name, UUID uuid, String handle) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.handle", is(handle)),
                hasJsonPath("$.type", is("collection")),
                hasJsonPath("$.metadata", Matchers.allOf(
                        MetadataMatcher.matchMetadata("dc.title", name)
                )));
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "license",
                "logo",
                "mappedItems[]"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/collections/" + uuid,
                "harvester",
                "itemtemplate",
                "license",
                "logo",
                "mappedItems",
                "self"
        );
    }

    private static Matcher<? super Object> matchLogo(Bitstream logo) {
        return logo == null ?
            allOf(
                hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty()))
            ) :
            allOf(
                hasJsonPath("$._embedded.logo",
                        BitstreamMatcher.matchBitstreamEntry(logo.getID(), logo.getSizeBytes()))
            );
    }
}
