/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * Enricher implementation that extracts and validates a tracker identifier from request parameters
 * and adds it to the Matomo request details.
 *
 * <p>This enricher looks for a 'trackerId' parameter in the usage event request parameters.
 * If found and the value matches the expected 16-character hexadecimal format, it will be added
 * to the Matomo request details as a visitor identifier.</p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class MatomoRequestTrackerIdentifierParamEnricher implements MatomoRequestDetailsEnricher {

    public static final String ID_REGEX = "(^([a-f]|[0-9]){16})";
    static final String TRACKER_ID = "trackerId";
    static final String VISITOR_IDENTIFIER = "_id";

    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        if (usageEvent == null || usageEvent.getRequest() == null) {
            return matomoRequestDetails;
        }

        Map<String, String[]> parameterMap = usageEvent.getRequest().getParameterMap();
        Optional<String> validParameterValue = getValidParameterValue(parameterMap);
        if (validParameterValue.isEmpty()) {
            return matomoRequestDetails;
        }

        return matomoRequestDetails.addParameter(VISITOR_IDENTIFIER, validParameterValue.get());
    }

    private Optional<String> getValidParameterValue(Map<String, String[]> parameterMap) {
        return parameterMap.entrySet().stream()
                           .filter(entry -> entry.getKey().equals(TRACKER_ID))
                           .flatMap(entry ->
                                        Arrays.stream(entry.getValue())
                                              .filter(value -> value.matches(ID_REGEX))
                           )
                           .findFirst();
    }
}
