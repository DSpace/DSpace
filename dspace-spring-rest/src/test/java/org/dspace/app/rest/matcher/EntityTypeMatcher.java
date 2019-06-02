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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.dspace.content.EntityType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class EntityTypeMatcher {

    private EntityTypeMatcher() {}

    public static Matcher<? super Object> matchEntityTypeEntry(EntityType entityType) {
        return matchEntityTypeExplicitValuesEntry(entityType.getID(), entityType.getLabel());
    }

    public static Matcher<? super Object> matchEntityTypeEntryForLabel(String label) {
        return matchEntityTypeExplicitValuesEntry(0, label);
    }

    private static Matcher<? super Object> matchId(int id) {
        return id == 0 ?
            allOf(
                hasJsonPath("$.id", Matchers.not(Matchers.empty()))
            ) :
            allOf(
                hasJsonPath("$.id", Matchers.is(id))
            );
    }

    private static Matcher<? super Object> matchSelfLink(int id) {
        return id == 0 ?
            allOf(
                hasJsonPath("$._links.self.href", containsString("/api/core/entitytypes/"))
            ) :
            allOf(
                hasJsonPath("$._links.self.href", containsString("/api/core/entitytypes/" + id))
            );
    }

    public static Matcher<? super Object> matchEntityTypeExplicitValuesEntry(int id, String label) {
        return allOf(
            matchId(id),
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is("entitytype")),
            matchSelfLink(id)
        );
    }
}
