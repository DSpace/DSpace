/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import java.util.function.Predicate;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Matcher based on an {@link Predicate}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @param  <T> the type of the instance to match
 */
public class LambdaMatcher<T> extends BaseMatcher<T> {

    private final Predicate<T> matcher;
    private final String description;

    public static <T> LambdaMatcher<T> matches(Predicate<T> matcher) {
        return new LambdaMatcher<T>(matcher, "Matches the given predicate");
    }

    public static <T> LambdaMatcher<T> matches(Predicate<T> matcher, String description) {
        return new LambdaMatcher<T>(matcher, description);
    }

    public static <T> Matcher<java.lang.Iterable<? super T>> has(Predicate<T> matcher) {
        return Matchers.hasItem(matches(matcher));
    }

    private LambdaMatcher(Predicate<T> matcher, String description) {
        this.matcher = matcher;
        this.description = description;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object argument) {
        return matcher.test((T) argument);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(this.description);
    }
}
