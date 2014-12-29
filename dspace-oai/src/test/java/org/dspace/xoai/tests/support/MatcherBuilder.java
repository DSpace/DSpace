/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.support;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;

import java.util.ArrayList;
import java.util.Collection;

public class MatcherBuilder<M extends MatcherBuilder, T> extends BaseMatcher<T> {
    private final Collection<Matcher<? super T>> matchers = new ArrayList<>();

    @Override
    public boolean matches(Object item) {
        return matcher().matches(item);
    }

    private Matcher<T> matcher() {
        return AllOf.allOf(matchers);
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(matcher());
    }

    protected M with (Matcher<? super T> matcher) {
        matchers.add(matcher);
        return (M) this;
    }
}
