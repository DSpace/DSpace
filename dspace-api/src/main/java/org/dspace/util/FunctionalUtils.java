/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 
 * These methods are linked to the functional paradigm and use {@code Functional} interfaces of java 8+, all the main
 * interfaces are in the package {@link java.util.function}.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class FunctionalUtils {

    /**
     * Private constructor, it's an Utils class with static methods / functions.
     */
    private FunctionalUtils() {
    }

    /**
     * 
     * Tests that {@code defaultValue} isn't null. If this test is positive, then
     * returns the {@code defaultValue}; Otherwise builds a new instance using the
     * {@code builder}
     * 
     * @param defaultValue default instance value
     * @param builder      instance generator
     * @return corresponding non-null instance
     */
    public static <T> T getDefaultOrBuild(T defaultValue, Supplier<T> builder) {
        return getCheckDefaultOrBuild(Objects::nonNull, defaultValue, builder);
    }

    /**
     * Tests the {@code defaultValue} using the {@code defaultValueChecker}. If its
     * test is positive, then returns the {@code defaultValue}; Otherwise builds a
     * new instance using the {@code builder}
     * 
     * @param defaultValueChecker checker that tests the defaultValue
     * @param defaultValue        default instance value
     * @param builder             supplier that generates a typed instance
     * @return corresponding instance after check
     */
    public static <T> T getCheckDefaultOrBuild(Predicate<T> defaultValueChecker, T defaultValue, Supplier<T> builder) {
        if (defaultValueChecker.test(defaultValue)) {
            return defaultValue;
        }
        return builder.get();
    }

}
