/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Class that can be extended to evaluate a given condition on items.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class ConditionEvaluator {

    /**
     * Test the given condition on the given item. The condition should have the
     * format [not].evaluatorIdentifier.value, where:
     * <ul>
     * <li>not can be used to negate the result of the condition
     * <li>evaluatorIdentifier is the unique identifier of the evaluator
     * <li>value can be any string useful to the evaluator
     * </ul>
     *
     * @param context   the DSpace Context
     * @param item      the item to evaluate
     * @param condition the condition to evaluate
     * @param place     the position to consider to evaluate the condition (useful
     *                  for nested and repeatable metadata), -1 means that all the
     *                  place must be considered
     * @return the evaluation result
     */
    public final boolean test(Context context, Item item, String condition, int place) {
        if (StringUtils.isBlank(condition)) {
            return false;
        }

        if (condition.startsWith("not.")) {
            return !doTest(context, item, condition.substring("not.".length()), place);
        }

        return doTest(context, item, condition, place);

    }

    /**
     * Test the given condition on the given item.
     * The condition should have the format evaluatorIdentifier.value, where:
     * <ul>
     * <li> evaluatorIdentifier is the unique identifier of the evaluator
     * <li> value can be any string useful to the evaluator
     * </ul>
     *
     * @param context the DSpace Context
     * @param item the item to evaluate
     * @param condition the condition to evaluate
     * @param place     the position to consider to evaluate the condition (useful
     *                  for nested and repeatable metadata), -1 means that all the
     *                  place must be considered
     * @return the evaluation result
     */
    protected abstract boolean doTest(Context context, Item item, String condition, int place);

}
