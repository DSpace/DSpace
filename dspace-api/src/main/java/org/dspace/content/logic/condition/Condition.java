/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * The Condition interface
 *
 * A condition is one logical statement testing an item for any idea. A condition is always a logical statements. An
 * operator is not a condition but also a logical statement.
 *
 * @author Kim Shepherd
 */
public interface Condition extends LogicalStatement {

    /**
     * Set parameters - used by Spring
     * @param parameters
     * @throws LogicalStatementException
     */
    void setParameters(Map<String, Object> parameters) throws LogicalStatementException;

    /**
     * Get parameters set by Spring in item-filters.xml
     * These could be any kind of map that the extending condition class needs for evaluation
     * @return map of parameters
     * @throws LogicalStatementException
     */
    Map<String, Object> getParameters() throws LogicalStatementException;

    /**
     * Get the result of logical evaluation for an item
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return result
     * @throws LogicalStatementException
     */
    @Override
    boolean getResult(Context context, Item item) throws LogicalStatementException;

    public void setItemService(ItemService itemService);

}
