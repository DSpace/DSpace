/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that returns true if the item is withdrawn
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class IsWithdrawnCondition extends AbstractCondition {
    private final static Logger log = LogManager.getLogger();

    /**
     * Return true if item is withdrawn
     * Return false if not
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        log.debug("Result of isWithdrawn is " + item.isWithdrawn());
        return item.isWithdrawn();
    }
}
