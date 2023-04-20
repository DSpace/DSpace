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
 * An operator that implements OR by evaluating sub-statements and returns
 * true if one or more sub-statements return true
 *
 * @author Kim Shepherd
 */
public class Or extends AbstractOperator {

    /**
     * Default constructor
     */
    public Or() {
        super();
    }

    /**
     * Constructor that accepts predefined list of statements as defined in item-filters.xml
     * @param statements    List of logical statements
     */
    public Or(List<LogicalStatement> statements) {
        super(statements);
    }

    /**
     * Return true if any sub-statement returns true
     * Return false otherwise
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of OR
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {

        for (LogicalStatement statement : getStatements()) {
            if (statement.getResult(context, item)) {
                return true;
            }
        }

        return false;
    }
}
