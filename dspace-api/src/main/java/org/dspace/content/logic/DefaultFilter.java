/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * The default filter, a very simple implementation of Filter / LogicalStatement
 * The idea is to have this as a wrapper / root class for all logical operations, so it takes a single
 * statement as a property (unlike an operator) and takes no parameters (unlike a condition)
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class DefaultFilter implements Filter {
    private LogicalStatement statement;
    private final static Logger log = LogManager.getLogger();

    /**
     * Set statement from Spring configuration in item-filters.xml
     * Be aware that this is singular not plural. A filter can have one sub-statement only.
     *
     * @param statement LogicalStatement of this filter (operator, condition, or another filter)
     */
    public void setStatement(LogicalStatement statement) {
        this.statement = statement;
    }

    /**
     * Get the result of logical evaluation for an item
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean
     * @throws LogicalStatementException
     */
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        return this.statement.getResult(context, item);
    }
}
