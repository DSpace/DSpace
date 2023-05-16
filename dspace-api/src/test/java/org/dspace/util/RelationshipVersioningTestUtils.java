/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.dspace.content.Item;
import org.dspace.content.Relationship.LatestVersionStatus;
import org.dspace.content.RelationshipType;
import org.hamcrest.Matcher;

/**
 * Methods for testing relationships and their behavior with versioned items.
 */
public class RelationshipVersioningTestUtils {

    private RelationshipVersioningTestUtils() {}

    public static Matcher<Object> isRel(
        Item leftItem, RelationshipType relationshipType, Item rightItem, LatestVersionStatus latestVersionStatus,
        int leftPlace, int rightPlace
    ) {
        return isRel(leftItem, relationshipType, rightItem, latestVersionStatus, null, null, leftPlace, rightPlace);
    }

    public static Matcher<Object> isRel(
        Item leftItem, RelationshipType relationshipType, Item rightItem, LatestVersionStatus latestVersionStatus,
        String leftwardValue, String rightwardValue, int leftPlace, int rightPlace
    ) {
        return allOf(
            hasProperty("leftItem", is(leftItem)),
            // NOTE: this is a painful one... class RelationshipType does not implement the equals method, so we cannot
            //       rely on object equality and have to compare ids instead. It has to be in capital letters,
            //       because the getter has been implemented inconsistently (#id vs #setId() vs #getID()).
            hasProperty("relationshipType", hasProperty("ID", is(relationshipType.getID()))),
            hasProperty("rightItem", is(rightItem)),
            hasProperty("leftPlace", is(leftPlace)),
            hasProperty("rightPlace", is(rightPlace)),
            hasProperty("leftwardValue", leftwardValue == null ? nullValue() : is(leftwardValue)),
            hasProperty("rightwardValue", rightwardValue == null ? nullValue() : is(rightwardValue)),
            hasProperty("latestVersionStatus", is(latestVersionStatus))
        );
    }

}
