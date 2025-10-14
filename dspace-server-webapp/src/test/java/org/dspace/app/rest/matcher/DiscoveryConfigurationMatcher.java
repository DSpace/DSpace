package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.hamcrest.Matcher;

public class DiscoveryConfigurationMatcher {

    private DiscoveryConfigurationMatcher() { }

    public static Matcher matchDiscoveryConfiguration(DiscoveryConfiguration discoveryConfiguration) {
        return allOf(
            hasJsonPath("id", is(discoveryConfiguration.getId()))
        );
    }
}
