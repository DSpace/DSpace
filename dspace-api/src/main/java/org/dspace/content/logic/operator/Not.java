/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.operator;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * An operator that implements NOT by simply negating a statement
 * Note that this operator doesn't actually implement the 'AbstractOperator' interface because
 * we only want one sub-statement. So it's actually just a simple implementation of LogicalStatement.
 * Not can have one sub-statement only, while and, or, nor, ... can have multiple sub-statements.
 *
 * @author Kim Shepherd
 */
public class Not implements LogicalStatement {

    private LogicalStatement statement;

    /**
     * Get sub-statement (note: singular! even though we keep the method name) for this operator
     * @return list of sub-statements
     */
    public LogicalStatement getStatements() {
        return statement;
    }

    /**
     * Set sub-statement (note: singular!) for this operator, as defined in item-filters.xml
     * @param statement    a single statement to apply to NOT operation
     */
    public void setStatements(LogicalStatement statement) {
        this.statement = statement;
    }

    /**
     * Default constructor
     */
    public Not() {}

    /**
     * Constructor that accepts predefined list of statements as defined in item-filters.xml
     * @param statement    Single logical statement
     */
    public Not(LogicalStatement statement) {
        this.statement = statement;
    }

    /**
     * Return true if the result of the sub-statement is false
     * Return false otherwise
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of NOT
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        return !statement.getResult(context, item);
    }
}
