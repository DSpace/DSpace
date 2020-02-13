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

import java.util.List;
import java.util.UUID;

import org.dspace.content.Collection;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class CommunityMatcher {

    private CommunityMatcher() { }

    // Matcher for communities with no titles / no name
    // Since a name is simply the first title (see Community.java), we cannot use the matchers below
    public static Matcher<? super Object> matchCommunityEntry(UUID uuid, String handle) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.handle", is(handle)),
                hasJsonPath("$.type", is("community")),
                hasJsonPath("$._embedded.collections", Matchers.not(Matchers.empty())),
                hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty())),
                matchLinks(uuid)
        );
    }

    // Matcher for communities with multiple titles
    // The title metadata for communities with multiple titles contains a list, so the matchers below can't be used
    public static Matcher<? super Object> matchCommunityEntryMultipleTitles(List<String> titles, UUID uuid,
                                                                            String handle) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(titles.get(0))),
                hasJsonPath("$.handle", is(handle)),
                hasJsonPath("$.type", is("community")),
                hasJsonPath("$._embedded.collections", Matchers.not(Matchers.empty())),
                hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty())),
                matchLinks(uuid)
        );
    }

    public static Matcher<? super Object> matchCommunityEntry(String name, UUID uuid, String handle) {
        return allOf(
            matchProperties(name, uuid, handle),
            hasJsonPath("$._embedded.collections", Matchers.not(Matchers.empty())),
            hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty())),
            matchLinks(uuid)
        );
    }

    public static Matcher<? super Object> matchProperties(String name, UUID uuid, String handle) {
        return allOf(
            hasJsonPath("$.uuid", is(uuid.toString())),
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.handle", is(handle)),
            hasJsonPath("$.type", is("community")),
            hasJsonPath("$.metadata", Matchers.allOf(
                MetadataMatcher.matchMetadata("dc.title", name)
            ))
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "collections[]",
                "logo",
                "subcommunities[]"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/communities/" + uuid,
                "collections",
                "logo",
                "self",
                "subcommunities"
        );
    }

    public static Matcher<? super Object> matchCommunityWithCollectionEntry(String name, UUID uuid, String handle,
                                                                            Collection col) {
        return allOf(
            matchProperties(name, uuid, handle),
            hasJsonPath("$._embedded.collections._embedded.collections[0]",
                        CollectionMatcher
                            .matchCollectionEntry(col.getName(), col.getID(), col.getHandle(), col.getLogo())),
            hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty())),
            matchLinks(uuid)
        );
    }

}
