/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * The base interface used by all logic classes: all operators and conditions are logical statements.
 * All statements must accept an Item object and return a boolean result.
 * The philosophy is that because Filter, Condition, Operator classes implement getResult(), they can all be
 * used as sub-statements in other Filters and Operators.
 *
 * @author Kim Shepherd
 */
public interface LogicalStatement {
    /**
     * Get the result of logical evaluation for an item
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    boolean getResult(Context context, Item item) throws LogicalStatementException;
}
