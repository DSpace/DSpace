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
 * Implementation of {@link RelationshipItemAuthorizer} that returns always
 * true.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelationshipItemAlwaysTrueAuthorizer implements RelationshipItemAuthorizer {

    @Override
    public boolean canHandleRelationshipOnItem(Context context, Item item) {
        return true;
    }

}
