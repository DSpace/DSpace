/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import static org.dspace.content.QAEvent.OPENAIRE_SOURCE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a QAEvent by all its
 * attributes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class QAEventMatcher extends TypeSafeMatcher<QAEvent> {

    private Matcher<String> eventIdMatcher;

    private Matcher<String> originalIdMatcher;

    private Matcher<String> relatedMatcher;

    private Matcher<String> sourceMatcher;

    private Matcher<String> statusMatcher;

    private Matcher<String> targetMatcher;

    private Matcher<String> titleMatcher;

    private Matcher<String> messageMatcher;

    private Matcher<String> topicMatcher;

    private Matcher<Double> trustMatcher;

    private QAEventMatcher(Matcher<String> eventIdMatcher, Matcher<String> originalIdMatcher,
        Matcher<String> relatedMatcher, Matcher<String> sourceMatcher, Matcher<String> statusMatcher,
        Matcher<String> targetMatcher, Matcher<String> titleMatcher, Matcher<String> messageMatcher,
        Matcher<String> topicMatcher, Matcher<Double> trustMatcher) {
        this.eventIdMatcher = eventIdMatcher;
        this.originalIdMatcher = originalIdMatcher;
        this.relatedMatcher = relatedMatcher;
        this.sourceMatcher = sourceMatcher;
        this.statusMatcher = statusMatcher;
        this.targetMatcher = targetMatcher;
        this.titleMatcher = titleMatcher;
        this.messageMatcher = messageMatcher;
        this.topicMatcher = topicMatcher;
        this.trustMatcher = trustMatcher;
    }

    /**
     * Creates an instance of {@link QAEventMatcher} that matches an OPENAIRE
     * QAEvent with PENDING status, with an event id, without a related item and
     * with the given attributes.
     * 
     * @param  originalId the original id to match
     * @param  target     the target to match
     * @param  title      the title to match
     * @param  message    the message to match
     * @param  topic      the topic to match
     * @param  trust      the trust to match
     * @return            the matcher instance
     */
    public static QAEventMatcher pendingOpenaireEventWith(String originalId, Item target,
        String title, String message, String topic, Double trust) {

        return new QAEventMatcher(notNullValue(String.class), is(originalId), nullValue(String.class),
            is(OPENAIRE_SOURCE), is("PENDING"), is(target.getID().toString()), is(title), is(message), is(topic),
            is(trust));

    }

    @Override
    public boolean matchesSafely(QAEvent event) {
        return eventIdMatcher.matches(event.getEventId())
            && originalIdMatcher.matches(event.getOriginalId())
            && relatedMatcher.matches(event.getRelated())
            && sourceMatcher.matches(event.getSource())
            && statusMatcher.matches(event.getStatus())
            && targetMatcher.matches(event.getTarget())
            && titleMatcher.matches(event.getTitle())
            && messageMatcher.matches(event.getMessage())
            && topicMatcher.matches(event.getTopic())
            && trustMatcher.matches(event.getTrust());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a QA event with the following attributes:")
            .appendText(" event id ").appendDescriptionOf(eventIdMatcher)
            .appendText(", original id ").appendDescriptionOf(originalIdMatcher)
            .appendText(", related ").appendDescriptionOf(relatedMatcher)
            .appendText(", source ").appendDescriptionOf(sourceMatcher)
            .appendText(", status ").appendDescriptionOf(statusMatcher)
            .appendText(", target ").appendDescriptionOf(targetMatcher)
            .appendText(", title ").appendDescriptionOf(titleMatcher)
            .appendText(", message ").appendDescriptionOf(messageMatcher)
            .appendText(", topic ").appendDescriptionOf(topicMatcher)
            .appendText(" and trust ").appendDescriptionOf(trustMatcher);
    }

}
