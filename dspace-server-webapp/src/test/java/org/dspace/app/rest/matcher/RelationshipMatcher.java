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

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.hamcrest.Matcher;

public class RelationshipMatcher {

    private RelationshipMatcher() {}

    public static Matcher<? super Object> matchRelationship(Relationship relationship) {
        return matchRelationshipExplicitValues(relationship.getLeftItem(), relationship.getRightItem(),
                                               relationship.getLeftPlace(), relationship.getRightPlace(),
                                               relationship.getRelationshipType());
    }

    private static Matcher<? super Object> matchRelationshipExplicitValues(Item leftItem, Item rightItem, int leftPlace,
                                                                           int rightPlace,
                                                                           RelationshipType relationshipType) {
        return matchRelationshipExplicitObjectValues(leftItem.getID(), rightItem.getID(), leftPlace, rightPlace,
                                                     relationshipType);
    }

    private static Matcher<? super Object> matchRelationshipExplicitObjectValues(UUID leftId, UUID rightId,
                                                                                 int leftPlace, int rightPlace,
                                                                                 RelationshipType relationshipType) {
        return allOf(
            hasJsonPath("$._links.leftItem.href", containsString(leftId.toString())),
            hasJsonPath("$._links.rightItem.href", containsString(rightId.toString())),
            hasJsonPath("$.leftPlace", is(leftPlace)),
            hasJsonPath("$.rightPlace", is(rightPlace)),
            hasJsonPath("$._embedded.relationshipType",
                        RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType))
        );
    }
}
