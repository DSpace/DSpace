/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link Matcher} to match a NotifyServiceEntity by all its
 * attributes.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
public class NotifyServiceEntityMatcher extends TypeSafeMatcher<NotifyServiceEntity> {

    private final NotifyServiceEntity expectedEntity;

    private NotifyServiceEntityMatcher(NotifyServiceEntity expectedEntity) {
        this.expectedEntity = expectedEntity;
    }

    public static NotifyServiceEntityMatcher matchesNotifyServiceEntity(NotifyServiceEntity expectedEntity) {
        return new NotifyServiceEntityMatcher(expectedEntity);
    }

    @Override
    protected boolean matchesSafely(NotifyServiceEntity actualEntity) {
        return actualEntity.getName().equals(expectedEntity.getName()) &&
            actualEntity.getDescription().equals(expectedEntity.getDescription()) &&
            actualEntity.getUrl().equals(expectedEntity.getUrl()) &&
            actualEntity.getLdnUrl().equals(expectedEntity.getLdnUrl()) &&
            actualEntity.getInboundPatterns() == expectedEntity.getInboundPatterns() &&
            actualEntity.isEnabled() == expectedEntity.isEnabled() &&
            actualEntity.getScore() == expectedEntity.getScore();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a Notify Service Entity with the following attributes:")
                   .appendText(", name ").appendValue(expectedEntity.getName())
                   .appendText(", description ").appendValue(expectedEntity.getDescription())
                   .appendText(", URL ").appendValue(expectedEntity.getUrl())
                   .appendText(", LDN URL ").appendValue(expectedEntity.getLdnUrl())
                   .appendText(", inbound patterns ").appendValue(expectedEntity.getInboundPatterns())
                   .appendText(", enabled ").appendValue(expectedEntity.isEnabled())
                   .appendText(", score ").appendValue(expectedEntity.getScore());
    }

}
