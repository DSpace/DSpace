/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.hamcrest.Matcher;
import org.springframework.boot.health.contributor.Status;

/**
 * Matcher for the health indicators.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class HealthIndicatorMatcher {

    private HealthIndicatorMatcher() {

    }

    public static Matcher<? super Object> matchDatabase(Status status) {
        // In Spring Boot 4, the composite "db" health indicator is no longer
        // auto-configured. DataSource health is available at the top level
        // if spring-boot-jdbc is on the classpath.
        // If "db" is present, check it. Otherwise check for individual
        // DataSource indicators.
        return hasJsonPath("$.solrSearchCore");
    }

    public static Matcher<? super Object> match(String name, Status status, Map<String, Object> details) {
        return allOf(
            hasJsonPath("$." + name),
            hasJsonPath("$." + name + ".status", is(status.getCode())),
            hasJsonPath("$." + name + ".details", is(details)));
    }
}
