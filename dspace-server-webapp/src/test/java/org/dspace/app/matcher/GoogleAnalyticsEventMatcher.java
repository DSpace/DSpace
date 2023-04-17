/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;

import org.dspace.google.GoogleAnalyticsEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for {@link GoogleAnalyticsEvent}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalyticsEventMatcher extends TypeSafeMatcher<GoogleAnalyticsEvent> {

    private final Matcher<String> clientIdMatcher;

    private final Matcher<String> userIpMatcher;

    private final Matcher<String> userAgentMatcher;

    private final Matcher<String> documentReferrerMatcher;

    private final Matcher<String> documentPathMatcher;

    private final Matcher<String> documentTitleMatcher;

    private final Matcher<Long> timeMatcher;

    public static GoogleAnalyticsEventMatcher event(String clientId, String userIp, String userAgent,
        String documentReferrer, String documentPath, String documentTitle) {

        return new GoogleAnalyticsEventMatcher(is(clientId), is(userIp), is(userAgent),
            is(documentReferrer), is(documentPath), is(documentTitle), any(Long.class));
    }

    private GoogleAnalyticsEventMatcher(Matcher<String> clientIdMatcher, Matcher<String> userIpMatcher,
        Matcher<String> userAgentMatcher, Matcher<String> documentReferrerMatcher, Matcher<String> documentPathMatcher,
        Matcher<String> documentTitleMatcher, Matcher<Long> timeMatcher) {
        this.clientIdMatcher = clientIdMatcher;
        this.userIpMatcher = userIpMatcher;
        this.userAgentMatcher = userAgentMatcher;
        this.documentReferrerMatcher = documentReferrerMatcher;
        this.documentPathMatcher = documentPathMatcher;
        this.documentTitleMatcher = documentTitleMatcher;
        this.timeMatcher = timeMatcher;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a Google Analytics event with the following attributes:")
            .appendText(" client id ").appendDescriptionOf(clientIdMatcher)
            .appendText(", user ip address ").appendDescriptionOf(userIpMatcher)
            .appendText(", user agent ").appendDescriptionOf(userAgentMatcher)
            .appendText(", document referrer ").appendDescriptionOf(documentReferrerMatcher)
            .appendText(", document path ").appendDescriptionOf(documentPathMatcher)
            .appendText(", document title ").appendDescriptionOf(documentTitleMatcher)
            .appendText(" and time ").appendDescriptionOf(timeMatcher);
    }

    @Override
    protected boolean matchesSafely(GoogleAnalyticsEvent event) {
        return clientIdMatcher.matches(event.getClientId())
            && userIpMatcher.matches(event.getUserIp())
            && userAgentMatcher.matches(event.getUserAgent())
            && documentReferrerMatcher.matches(event.getDocumentReferrer())
            && documentPathMatcher.matches(event.getDocumentPath())
            && documentTitleMatcher.matches(event.getDocumentTitle())
            && timeMatcher.matches(event.getTime());
    }

}
