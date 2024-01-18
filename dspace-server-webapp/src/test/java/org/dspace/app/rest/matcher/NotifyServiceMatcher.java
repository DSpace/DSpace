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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.hamcrest.Matcher;

/**
 * Class to match JSON NotifyServiceEntity in ITs
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceMatcher {

    private NotifyServiceMatcher() { }

    public static Matcher<? super Object> matchNotifyService(String name, String description, String url,
                                                             String ldnUrl) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.description", is(description)),
            hasJsonPath("$.url", is(url)),
            hasJsonPath("$.ldnUrl", is(ldnUrl)),
            hasJsonPath("$._links.self.href", containsString("/api/ldn/ldnservices/"))
        );
    }

    public static Matcher<? super Object> matchNotifyServiceWithoutLinks(
        String name, String description, String url, String ldnUrl) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.description", is(description)),
            hasJsonPath("$.url", is(url)),
            hasJsonPath("$.ldnUrl", is(ldnUrl))
        );
    }

    public static Matcher<? super Object> matchNotifyService(String name, String description, String url,
                                                             String ldnUrl, boolean enabled) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.description", is(description)),
            hasJsonPath("$.url", is(url)),
            hasJsonPath("$.ldnUrl", is(ldnUrl)),
            hasJsonPath("$.enabled", is(enabled)),
            hasJsonPath("$._links.self.href", containsString("/api/ldn/ldnservices/"))
        );
    }

    public static Matcher<? super Object> matchNotifyService(String name, String description, String url,
                                                             String ldnUrl, boolean enabled,
                                                             String lowerIp, String upperIp) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.description", is(description)),
            hasJsonPath("$.url", is(url)),
            hasJsonPath("$.ldnUrl", is(ldnUrl)),
            hasJsonPath("$.enabled", is(enabled)),
            hasJsonPath("$.lowerIp", is(lowerIp)),
            hasJsonPath("$.upperIp", is(upperIp)),
            hasJsonPath("$._links.self.href", containsString("/api/ldn/ldnservices/"))
        );
    }

    public static Matcher<? super Object> matchNotifyService(int id, String name, String description,
                                                             String url, String ldnUrl) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            matchNotifyService(name, description, url, ldnUrl),
            hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
            hasJsonPath("$._links.self.href", endsWith("/api/ldn/ldnservices/" + id))
        );
    }

    public static Matcher<? super Object> matchNotifyService(int id, String name, String description,
                                                             String url, String ldnUrl, boolean enabled) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            matchNotifyService(name, description, url, ldnUrl, enabled),
            hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
            hasJsonPath("$._links.self.href", endsWith("/api/ldn/ldnservices/" + id))
        );
    }

    public static Matcher<? super Object> matchNotifyService(int id, String name, String description,
                                                             String url, String ldnUrl, boolean enabled,
                                                             String lowerIp, String upperIp) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            matchNotifyService(name, description, url, ldnUrl, enabled, lowerIp, upperIp),
            hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
            hasJsonPath("$._links.self.href", endsWith("/api/ldn/ldnservices/" + id))
        );
    }

    public static Matcher<? super Object> matchNotifyServiceWithoutLinks(
        int id, String name, String description, String url, String ldnUrl) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            matchNotifyServiceWithoutLinks(name, description, url, ldnUrl)
        );
    }

    public static Matcher<? super Object> matchNotifyServicePattern(String pattern, String constraint) {
        return allOf(
            hasJsonPath("$.pattern", is(pattern)),
            hasJsonPath("$.constraint", is(constraint))
        );
    }

    public static Matcher<? super Object> matchNotifyServicePattern(String pattern,
                                                                    String constraint,
                                                                    Boolean automatic) {
        return allOf(
            matchNotifyServicePattern(pattern, constraint),
            hasJsonPath("$.automatic", is(automatic))
        );
    }

}
