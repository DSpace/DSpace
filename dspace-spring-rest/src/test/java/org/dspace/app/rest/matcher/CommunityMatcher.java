/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.dspace.content.Collection;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class CommunityMatcher {

    public static Matcher<? super Object> matchCommunityEntry(String name, UUID uuid, String handle) {
        return allOf(
                matchProperties(name, uuid, handle),
                hasJsonPath("$._embedded.collections", Matchers.not(Matchers.empty())),
                hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty())),
                matchLinks(uuid)
        );
    }

    public static Matcher<? super Object> matchProperties(String name, UUID uuid, String handle){
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.handle", is(handle)),
                hasJsonPath("$.type", is("community")),
                hasJsonPath("$.metadata", Matchers.contains(
                        CommunityMetadataMatcher.matchTitle(name)
                ))
        );
    }

    public static Matcher<? super Object> matchLinks(UUID uuid){
        return allOf(
                hasJsonPath("$._links.collections.href", Matchers.containsString("/api/core/communities/" + uuid.toString() + "/collections")),
                hasJsonPath("$._links.logo.href", Matchers.containsString("/api/core/communities/" + uuid.toString() + "/logo")),
                hasJsonPath("$._links.self.href", Matchers.containsString("/api/core/communities/" + uuid.toString()))
        );
    }

    public static Matcher<? super Object> matchCommunityWithCollectionEntry(String name, UUID uuid, String handle, Collection col) {
        return allOf(
                matchProperties(name, uuid, handle),
                hasJsonPath("$._embedded.collections._embedded[0]",
                        CollectionMatcher.matchCollectionEntry(col.getName(), col.getID(), col.getHandle(), col.getLogo())),
                hasJsonPath("$._embedded.logo", Matchers.not(Matchers.empty())),
                matchLinks(uuid)
        );
    }

}
