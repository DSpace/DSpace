/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;

/**
 * Interface for classes that check if a relationship of a specific type can be
 * created between two items.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface RelationshipAuthorizer {

    /**
     * Check if the current user is authorized to create/edit/delete a relationship
     * of the given type between the leftItem and the rigthItem.
     *
     * @param  context          The DSpace context
     * @param  relationshipType the type of the relationship to be checked
     * @param  leftItem         the left item of the relationship
     * @param  rightItem        the right item of the relationship
     * @return                  true if the current user can create/edit or delete
     *                          the relationship, false otherwise
     */
    boolean canHandleRelationship(Context context, RelationshipType relationshipType, Item leftItem, Item rightItem);

}
