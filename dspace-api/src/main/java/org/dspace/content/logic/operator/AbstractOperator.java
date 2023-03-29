/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.operator;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * Abstract class for an operator.
 * An operator contains a list of logical statements (conditions or more operators) and depending on the kind
 * of operator (AND, OR, NOT, etc.) the results of some or all sub-statements are evaluated and returned
 * as a logical result
 *
 * @author Kim Shepherd
 */
public abstract class AbstractOperator implements LogicalStatement {

    private List<LogicalStatement> statements = new ArrayList<>();

    /**
     * Get sub-statements for this operator
     * @return list of sub-statements
     */
    public List<LogicalStatement> getStatements() {
        return statements;
    }

    /**
     * Set sub-statements for this operator, as defined in item-filters.xml
     * @param statements    list of logical statements
     */
    public void setStatements(List<LogicalStatement> statements) {
        this.statements = statements;
    }

    /**
     * Default constructor
     */
    public AbstractOperator() {}

    /**
     * Constructor to create operator from some predefined statements
     * @param statements
     */
    public AbstractOperator(List<LogicalStatement> statements) {
        this.statements = statements;
    }

    /**
     *
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation (of sub-statements)
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        return false;
    }
}
