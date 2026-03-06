/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for classes used by {@link RelationshipAuthorizerImpl} to check if
 * a relationship can be created between two item. An instance of
 * RelationshipItemAuthorizer check the creation condition on a single item
 * (left or right).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface RelationshipItemAuthorizer {

    /**
     * Check if the current user is authorized to create/edit/delete a relationship
     * that involves the given item.
     *
     * @param  context the DSpace context
     * @param  item    the item to check
     * @return         true if a relationship can be modified, false otherwise
     */
    public boolean canHandleRelationshipOnItem(Context context, Item item);

}
