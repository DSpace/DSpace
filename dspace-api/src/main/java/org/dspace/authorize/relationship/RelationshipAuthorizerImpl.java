/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.notNull;

import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;

/**
 * Default implementation of {@link RelationshipAuthorizer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelationshipAuthorizerImpl implements RelationshipAuthorizer {

    private String leftEntityType;

    private String leftwardType;

    private String rightEntityType;

    private String rightwardType;

    private RelationshipItemAuthorizer leftItemAuthorizer;

    private RelationshipItemAuthorizer rightItemAuthorizer;

    private boolean andCondition;

    public RelationshipAuthorizerImpl(RelationshipItemAuthorizer leftItemAuthorizer,
        RelationshipItemAuthorizer rightItemAuthorizer) {
        this.leftItemAuthorizer = requireNonNull(leftItemAuthorizer, "Left item authorizer required");
        this.rightItemAuthorizer = requireNonNull(rightItemAuthorizer, "Right item authorizer required");
    }

    @Override
    public boolean canHandleRelationship(Context context,
        RelationshipType relationshipType, Item leftItem, Item rightItem) {

        notNull(relationshipType, "The relationship type is required to handle a relationship");
        notNull(leftItem, "The left item is required to handle a relationship");
        notNull(rightItem, "The right item is required to handle a relationship");

        if (notMatchesRelationshipType(relationshipType)) {
            return false;
        }

        if (andCondition) {
            return leftItemAuthorizer.canHandleRelationshipOnItem(context, leftItem)
                && rightItemAuthorizer.canHandleRelationshipOnItem(context, rightItem);
        } else {
            return leftItemAuthorizer.canHandleRelationshipOnItem(context, leftItem)
                || rightItemAuthorizer.canHandleRelationshipOnItem(context, rightItem);
        }

    }

    private boolean notMatchesRelationshipType(RelationshipType relationshipType) {

        if (leftEntityType != null && !leftEntityType.equals(getEntityTypeLabel(relationshipType.getLeftType()))) {
            return true;
        }

        if (rightEntityType != null && !rightEntityType.equals(getEntityTypeLabel(relationshipType.getRightType()))) {
            return true;
        }

        if (leftwardType != null && !leftwardType.equals(relationshipType.getLeftwardType())) {
            return true;
        }

        if (rightwardType != null && !rightwardType.equals(relationshipType.getRightwardType())) {
            return true;
        }

        return false;

    }

    private String getEntityTypeLabel(EntityType entityType) {
        return entityType != null ? entityType.getLabel() : null;
    }

    public void setLeftEntityType(String leftEntityType) {
        this.leftEntityType = leftEntityType;
    }

    public void setLeftwardType(String leftwardType) {
        this.leftwardType = leftwardType;
    }

    public void setRightEntityType(String rightEntityType) {
        this.rightEntityType = rightEntityType;
    }

    public void setRightwardType(String rightwardType) {
        this.rightwardType = rightwardType;
    }

    public void setAndCondition(boolean andCondition) {
        this.andCondition = andCondition;
    }


}
