/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.databene.contiperf.junit;

import java.lang.reflect.Field;

import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * That is just a workaround to support JUnit 4.12 with the ContiPerfRule.
 * This performance rule extension makes sure that each statement is wrapped into
 * the JUnit 4.11 format.
 *
 * <p>
 * From JUnit 4.12, the {@code fNext} field has been replaced with the
 * {@code next} field on both {@code RunAfters} and {@code RunBefores}
 * statements. This class is for handling both cases gracefully.
 * <p>
 * More details about the issue can be found 
 * <a href="https://github.com/lucaspouzac/contiperf/issues/9">here</a>.
 *
 * The lastest ContiPerf release fixes this, but is not available in the Maven repositories:
 * <a href="https://github.com/lucaspouzac/contiperf/issues/8">https://github.com/lucaspouzac/contiperf/issues/8</a>
 *
 */
public class ContiPerfRuleExt extends ContiPerfRule {

    private static final String FIELD_NAME_JUNIT_411 = "fNext";
    private static final String FIELD_NAME_JUNIT_412 = "next";

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return super.apply(wrapStatement(base), method, target);
    }

    private Statement wrapStatement(final Statement base) {
        if(requiresFieldMapping(base)) {
            Statement fnext = getFieldValue(FIELD_NAME_JUNIT_412, base);
            if (base instanceof RunAfters) {
                return new RunAfters_411((RunAfters) base, fnext);
            } else if(base instanceof RunBefores) {
                return new RunBefores_411((RunBefores) base, fnext);
            }
            return null;
        } else {
            return base;
        }
    }

    private Statement getFieldValue(final String fieldNameJunit412, final Statement base) {
        try {
            Field field = base.getClass().getDeclaredField(fieldNameJunit412);
            return (Statement) field.get(base);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //ignore
            return null;
        }
    }

    private boolean requiresFieldMapping(Statement it) {
        return hasField(it, FIELD_NAME_JUNIT_412) && !hasField(it, FIELD_NAME_JUNIT_411);
    }

    private boolean hasField(Statement it, String fieldName) {
        try {
            it.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            //ignore
        }
        return false;
    }

    private class RunBefores_411 extends RunBefores {

        private Statement delegate;
        private Statement fNext;

        private RunBefores_411(RunBefores delegate, Statement fNext) {
            // We delegate to the evaluate method anyway.
            super(null, null, null);
            this.delegate = delegate;
            this.fNext = fNext;
        }

        @Override
        public void evaluate() throws Throwable {
            delegate.evaluate();
        }

    }

    private class RunAfters_411 extends RunAfters {

        private Statement delegate;
        private Statement fNext;

        private RunAfters_411(RunAfters delegate, Statement fNext) {
            // We delegate to the evaluate method anyway.
            super(null, null, null);
            this.delegate = delegate;
            this.fNext = fNext;
        }

        @Override
        public void evaluate() throws Throwable {
            delegate.evaluate();
        }
    }
}