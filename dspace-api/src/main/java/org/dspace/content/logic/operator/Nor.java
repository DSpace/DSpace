/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.operator;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * An operator that implements NOR by negating an OR operation.
 *
 * @author Kim Shepherd
 */
public class Nor extends AbstractOperator {

    /**
     * Default constructor
     */
    public Nor() {
        super();
    }

    /**
     * Constructor that accepts predefined list of statements as defined in item-filters.xml
     * @param statements    List of logical statements
     */
    public Nor(List<LogicalStatement> statements) {
        super(statements);
    }

    /**
     * Return true if the result of OR'ing the sub-statements is false
     * Return false otherwise
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of NOR
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        return !(new Or(getStatements()).getResult(context, item));
    }
}
