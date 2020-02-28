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
import org.dspace.content.RelationshipType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class RelationshipTypeMatcher {

    private RelationshipTypeMatcher() {}

    public static Matcher<? super Object> matchRelationshipTypeEntry(RelationshipType relationshipType) {
        return matchRelationshipTypeExplicitEntityTypes(relationshipType, relationshipType.getLeftType(),
                                                        relationshipType.getRightType());
    }

    private static Matcher<? super Object> matchRelationshipTypeExplicitEntityTypes(RelationshipType relationshipType,
                                                                                    EntityType leftType,
                                                                                    EntityType rightType) {
        return matchRelationshipTypeExplicitEntityTypeValues(relationshipType, leftType.getID(), leftType.getLabel(),
                                                             rightType.getID(), rightType.getLabel());
    }

    private static Matcher<? super Object> matchRelationshipTypeExplicitEntityTypeValues(
        RelationshipType relationshipType, int leftEntityTypeId, String leftEntityTypeLabel, int rightEntityTypeId,
        String rightEntityTypeLabel) {

        return matchExplicitRelationshipTypeValuesAndExplicitEntityTypeValues(relationshipType.getID(),
                                                                              relationshipType.getLeftwardType(),
                                                                              relationshipType.getRightwardType(),
                                                                              relationshipType.getLeftMinCardinality(),
                                                                              relationshipType.getLeftMaxCardinality(),
                                                                              relationshipType.getRightMinCardinality(),
                                                                              relationshipType.getRightMaxCardinality(),
                                                                              leftEntityTypeId, leftEntityTypeLabel,
                                                                              rightEntityTypeId, rightEntityTypeLabel,
                                                                              relationshipType.isCopyToLeft(),
                                                                              relationshipType.isCopyToRight());
    }

    private static Matcher<? super Object> matchExplicitRelationshipTypeValuesAndExplicitEntityType(int id,
        String leftwardType, String rightwardType, Integer leftMinCardinality, Integer leftMaxCardinality,
        Integer rightMinCardinality, Integer rightMaxCardinality,
        EntityType leftEntityType, EntityType rightEntityType, boolean copyToLeft, boolean copyToRight) {
        return matchExplicitRelationshipTypeValuesAndExplicitEntityTypeValues(id, leftwardType, rightwardType,
                                                                              leftMinCardinality, leftMaxCardinality,
                                                                              rightMinCardinality,
                                                                              rightMaxCardinality,
                                                                              leftEntityType.getID(),
                                                                              leftEntityType.getLabel(),
                                                                              rightEntityType.getID(),
                                                                              rightEntityType.getLabel(),
                                                                              copyToLeft, copyToRight);
    }

    private static Matcher<? super Object> matchExplicitRelationshipTypeValuesAndExplicitEntityTypeValues(int id,
        String leftwardType, String rightwardType, Integer leftMinCardinality, Integer leftMaxCardinality,
        Integer rightMinCardinality, Integer rightMaxCardinality, int leftEntityTypeId, String leftEntityTypeLabel,
        int rightEntityTypeId, String rightEntityTypeLabel, boolean copyToLeft, boolean copyToRight) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.leftwardType", is(leftwardType)),
            hasJsonPath("$.rightwardType", is(rightwardType)),
            hasJsonPath("$.copyToLeft", is(copyToLeft)),
            hasJsonPath("$.copyToRight", is(copyToRight)),
            hasJsonPath("$.leftMinCardinality", is(leftMinCardinality)),
            hasJsonPath("$.leftMaxCardinality", is(leftMaxCardinality)),
            hasJsonPath("$.rightMinCardinality", is(rightMinCardinality)),
            hasJsonPath("$.rightMaxCardinality", is(rightMaxCardinality)),
            hasJsonPath("$.type", is("relationshiptype")),
            hasJsonPath("$._links.self.href", containsString("/api/core/relationshiptypes/" + id)),
            hasJsonPath("$._embedded.leftType", Matchers.allOf(
                EntityTypeMatcher.matchEntityTypeExplicitValuesEntry(leftEntityTypeId, leftEntityTypeLabel)
            )),
            hasJsonPath("$._embedded.rightType", Matchers.is(
                EntityTypeMatcher.matchEntityTypeExplicitValuesEntry(rightEntityTypeId, rightEntityTypeLabel)
            ))
        );
    }
}