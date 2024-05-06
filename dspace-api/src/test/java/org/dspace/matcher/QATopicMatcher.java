/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import static org.hamcrest.Matchers.is;

import org.dspace.qaevent.QATopic;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a QATopic by all its
 * attributes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class QATopicMatcher extends TypeSafeMatcher<QATopic> {

    private Matcher<String> keyMatcher;

    private Matcher<Long> totalEventsMatcher;

    private QATopicMatcher(Matcher<String> keyMatcher, Matcher<Long> totalEventsMatcher) {
        this.keyMatcher = keyMatcher;
        this.totalEventsMatcher = totalEventsMatcher;
    }

    /**
     * Creates an instance of {@link QATopicMatcher} that matches a QATopic with the
     * given key and total events count.
     * @param  key         the key to match
     * @param  totalEvents the total events count to match
     * @return             the matcher instance
     */
    public static QATopicMatcher with(String key, long totalEvents) {
        return new QATopicMatcher(is(key), is(totalEvents));
    }

    @Override
    public boolean matchesSafely(QATopic event) {
        return keyMatcher.matches(event.getKey()) && totalEventsMatcher.matches(event.getTotalEvents());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a QA topic with the following attributes:")
            .appendText(" key ").appendDescriptionOf(keyMatcher)
            .appendText(" and total events ").appendDescriptionOf(totalEventsMatcher);
    }

}
