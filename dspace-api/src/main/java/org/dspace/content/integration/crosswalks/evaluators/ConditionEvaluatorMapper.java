/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import java.util.Map;
import java.util.Set;

/**
 * A Mapper between instances of {@link ConditionEvaluator} and their identifiers.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ConditionEvaluatorMapper {

    private final Map<String, ConditionEvaluator> conditionEvaluators;

    public ConditionEvaluatorMapper(Map<String, ConditionEvaluator> conditionEvaluators) {
        this.conditionEvaluators = conditionEvaluators;
    }

    public Set<String> getConditionEvaluatorNames() {
        return conditionEvaluators.keySet();
    }

    public ConditionEvaluator getConditionEvaluator(String name) {
        return conditionEvaluators.get(name);
    }

    public boolean contains(String name) {
        return conditionEvaluators.containsKey(name);
    }

}
