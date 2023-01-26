/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.util.stream.Collectors;

import org.dspace.eperson.Subscription;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Provide convenient org.hamcrest.Matcher to verify a SubscriptionRest json response
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SubscriptionMatcher {

    private SubscriptionMatcher() {}

    public static Matcher<? super Object> matchSubscription(Subscription subscription) {
        return allOf(
                hasJsonPath("$.id", is(subscription.getID())),
                hasJsonPath("$.type", is("subscription")),
                hasJsonPath("$.subscriptionType", is(subscription.getSubscriptionType())),
                hasJsonPath("$.subscriptionParameterList", Matchers.containsInAnyOrder(
                    subscription.getSubscriptionParameterList().stream()
                                .map(x -> SubscriptionMatcher.matchSubscriptionParameter(x.getName(), x.getValue()))
                                .collect(Collectors.toList())
                )),
                //Check links
                matchLinks(subscription.getID())
                );
    }

    public static Matcher<? super Object> matchSubscriptionParameter(String name, String value) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.value", is(value))
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(Integer id) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/subscriptions/" + id,
                "resource",
                "eperson",
                "self"
        );
    }

}