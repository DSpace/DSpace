/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * This class adds the {@code _cvar} parameter to the {@code MatomoRequestDetails}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestCustomVariablesEnricher implements MatomoRequestDetailsEnricher {

    public static final String VARIABLE_PATTERN = "\"{0}\":[\"{1}\",\"{2}\"]";
    public static final String PK_CVAR = "_pk_cvar";

    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        Cookie[] cookies = usageEvent.getRequest().getCookies();
        if (cookies == null) {
            return matomoRequestDetails;
        }
        for (Cookie cookie : cookies) {
            String cookieName = cookie.getName();
            String baseName = null;
            if (cookieName != null && cookieName.contains(".")) {
                baseName = cookieName.substring(0, cookieName.indexOf("."));
            }
            if (baseName != null && baseName.toLowerCase(Locale.ROOT).startsWith(PK_CVAR)) {
                return addCustomVariables(matomoRequestDetails, cookie.getValue());
            }
        }
        return matomoRequestDetails;
    }

    private static MatomoRequestDetails addCustomVariables(
        MatomoRequestDetails matomoRequestDetails, String customVariables
    ) {
        Map<Integer, Map.Entry<String, String>> parsedCustomVariables = parse(customVariables);
        if (parsedCustomVariables == null) {
            return matomoRequestDetails;
        }

        String variables =
            parsedCustomVariables.entrySet()
                                 .stream()
                                 .map(entry ->
                                          MessageFormat.format(
                                              VARIABLE_PATTERN,
                                              entry.getKey(),
                                              entry.getValue().getKey(), entry.getValue().getValue()
                                          )
                                 )
                                 .collect(Collectors.joining(","));

        if (variables.isEmpty()) {
            return matomoRequestDetails;
        }
        return matomoRequestDetails.addParameter("_cvar", "{" + variables + "}");
    }

    /**
     * Parses a JSON representation of custom variables.
     *
     * <p>The format is as follows: {@code {"1":["key1","value1"],"2":["key2","value2"]}}
     *
     * <p>Example: {@code {"1":["OS","Windows"],"2":["Browser","Firefox"]}}
     *
     * <p>This is mainly used to parse the custom variables from the cookie.
     *
     * @param value The JSON representation of the custom variables to parse or null
     * @return The parsed custom variables or null if the given value is null or empty
     */
    public static Map<Integer, Map.Entry<String, String>> parse(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(value, ":{}\"");

        Integer key = null;
        String customVariableKey = null;
        String customVariableValue = null;
        Map<Integer, Map.Entry<String, String>> parsedVariables = new HashMap<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (!token.isEmpty()) {
                if (token.matches("\\d+")) {
                    key = Integer.parseInt(token);
                } else if (token.startsWith("[") && key != null) {
                    customVariableKey = tokenizer.nextToken();
                    tokenizer.nextToken();
                    customVariableValue = tokenizer.nextToken();
                } else if (key != null && customVariableKey != null && customVariableValue != null) {
                    parsedVariables.put(key, Map.entry(customVariableKey, customVariableValue));
                } else if (token.equals(",")) {
                    key = null;
                    customVariableKey = null;
                    customVariableValue = null;
                }
            }
        }
        return parsedVariables;
    }
}
