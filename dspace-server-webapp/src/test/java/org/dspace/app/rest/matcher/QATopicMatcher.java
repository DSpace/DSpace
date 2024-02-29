/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.app.rest.model.hateoas.QATopicResource;
import org.dspace.content.QAEvent;
import org.hamcrest.Matcher;

/**
 * Matcher related to {@link QATopicResource}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QATopicMatcher {

    private QATopicMatcher() { }

    public static Matcher<? super Object> matchQATopicEntry(String topicName, int totalEvents) {
        return matchQATopicEntry(QAEvent.OPENAIRE_SOURCE, topicName, totalEvents);
    }


    public static Matcher<? super Object> matchQATopicEntry(String topicName) {
        return matchQATopicEntry(QAEvent.OPENAIRE_SOURCE, topicName);
    }

    public static Matcher<? super Object> matchQATopicEntry(String source, String topicName, int totalEvents) {
        return allOf(
            hasJsonPath("$.type", is("qualityassurancetopic")),
            hasJsonPath("$.name", is(topicName)),
            hasJsonPath("$.id", is(source + ":" + topicName.replace("/", "!"))),
            hasJsonPath("$.totalEvents", is(totalEvents))
        );
    }

    public static Matcher<? super Object> matchQATopicEntry(String source, String topicName) {
        return allOf(
            hasJsonPath("$.type", is("qualityassurancetopic")),
            hasJsonPath("$.name", is(topicName)),
            hasJsonPath("$.id", is(source + ":" + topicName.replace("/", "!")))
        );
    }

    public static Matcher<? super Object> matchQATopicEntry(String source, String topicName, String itemUuid,
           int totalEvents) {
        return allOf(
                hasJsonPath("$.type", is("qualityassurancetopic")),
                hasJsonPath("$.name", is(topicName)),
                hasJsonPath("$.id", is(source + ":" + topicName.replace("/", "!") + ":" + itemUuid)),
                hasJsonPath("$.totalEvents", is(totalEvents))
            );
    }

}
