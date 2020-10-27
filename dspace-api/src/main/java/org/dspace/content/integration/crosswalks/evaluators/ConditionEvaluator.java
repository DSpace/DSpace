/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for classes that can evaluate a given condition on items.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ConditionEvaluator {

    /**
     * Test the given condition on the given item.
     * The condition should have the format [not].evaluatorIdentifier.value,
     * where:
     * <ul>
     * <li> not can be used to negate the result of the condition
     * <li> evaluatorIdentifier is the unique identifier of the evaluator
     * <li> value can be any string useful to the evaluator
     * </ul>
     *
     * @param context the DSpace Context
     * @param item the item to evaluate
     * @param condition the condition to evaluate
     * @return the evaluation result
     */
    public boolean test(Context context, Item item, String condition);

}
