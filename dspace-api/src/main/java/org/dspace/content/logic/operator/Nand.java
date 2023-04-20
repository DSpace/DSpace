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
 * An operator that implements NAND by negating an AND operation
 *
 * @author Kim Shepherd
 */
public class Nand extends AbstractOperator {

    /**
     * Default constructor
     */
    public Nand() {
        super();
    }

    /**
     * Constructor that accepts predefined list of statements as defined in item-filters.xml
     * @param statements    List of logical statements
     */
    public Nand(List<LogicalStatement> statements) {
        super(statements);
    }

    /**
     * Return true if the result of AND'ing all sub-statements is false (ie. a NOT(AND())
     * Return false if not
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of NAND
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        return !(new And(getStatements()).getResult(context, item));
    }
}
