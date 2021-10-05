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

import org.dspace.eperson.Subscription;
import org.hamcrest.Matcher;

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
                hasJsonPath("$.subscriptionType", is(subscription.getType())),

                //Check links
                matchLinks(subscription.getID())
                );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(Integer id) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/subscriptions/" + id,
                "dSpaceObject",
                "ePerson",
                "self"
        );
    }

}