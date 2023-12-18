/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import static org.hamcrest.Matchers.is;

import org.dspace.qaevent.QASource;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a QASource by all its
 * attributes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class QASourceMatcher extends TypeSafeMatcher<QASource> {

    private Matcher<String> nameMatcher;

    private Matcher<Long> totalEventsMatcher;

    private QASourceMatcher(Matcher<String> nameMatcher, Matcher<Long> totalEventsMatcher) {
        this.nameMatcher = nameMatcher;
        this.totalEventsMatcher = totalEventsMatcher;
    }

    /**
     * Creates an instance of {@link QASourceMatcher} that matches a QATopic with
     * the given name and total events count.
     * @param  name        the name to match
     * @param  totalEvents the total events count to match
     * @return             the matcher instance
     */
    public static QASourceMatcher with(String name, long totalEvents) {
        return new QASourceMatcher(is(name), is(totalEvents));
    }

    @Override
    public boolean matchesSafely(QASource event) {
        return nameMatcher.matches(event.getName()) && totalEventsMatcher.matches(event.getTotalEvents());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a QA source with the following attributes:")
            .appendText(" name ").appendDescriptionOf(nameMatcher)
            .appendText(" and total events ").appendDescriptionOf(totalEventsMatcher);
    }

}
