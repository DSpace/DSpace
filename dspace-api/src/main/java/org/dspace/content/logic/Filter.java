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
 * The interface for Filter currently doesn't add anything to LogicalStatement but inherits from it
 * just to keep naming / reflection clean, and in case Filters should do anything additional in future.
 * We need this as filters have to be specified in the spring configuration (item-filters.xml).
 * Filters are the top level elements of the logic. Only logical statements that implement this interface
 * are allowed to be the root element of a spring configuration (item-filters.xml) of this logic framework.
 * A filter is just helping to differentiate between logical statement that can be used as root elements and
 * logical statement that shouldn't be use as root element. A filter may contain only one substatement.
 *
 * @author Kim Shepherd
 * @version $Revision$
 * @see org.dspace.content.logic.DefaultFilter
 */
public interface Filter extends LogicalStatement {
    /**
     * Get the result of logical evaluation for an item
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean
     * @throws LogicalStatementException
     */
    boolean getResult(Context context, Item item) throws LogicalStatementException;
}
