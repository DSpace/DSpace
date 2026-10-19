/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to convert the {@code MatomoRequestDetails} cookies
 * to the Matomo format.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoCookieConverter {

    private MatomoCookieConverter() {
    }

    /**
     * Converts a single MatomoRequestDetails object's cookies into a Matomo-formatted string
     *
     * @param matomoRequestDetails The MatomoRequestDetails object containing cookies to convert
     * @return A string containing the cookies in key=value format, separated by semicolons
     */
    public static String convert(MatomoRequestDetails matomoRequestDetails) {
        return matomoRequestDetails.cookies
                    .entrySet()
                    .stream()
                    .map((entry) ->
                             MessageFormat.format("{0}={1}", entry.getKey(), entry.getValue())
                    )
                    .collect(Collectors.joining(";"));
    }

    /**
     * Converts a list of MatomoRequestDetails objects' cookies into a single Matomo-formatted string.
     * If multiple cookies have the same key, the last value will be used.
     *
     * @param matomoRequestDetails List of MatomoRequestDetails objects containing cookies to convert
     * @return A string containing the merged cookies in key=value format, separated by semicolons.
     *         Returns empty string if input is null or empty.
     */
    public static String convert(List<MatomoRequestDetails> matomoRequestDetails) {
        if (matomoRequestDetails == null || matomoRequestDetails.isEmpty()) {
            return "";
        }
        Map<String, String> reducedCookies =
            matomoRequestDetails.stream()
                                .flatMap(details -> details.cookies.entrySet().stream())
                                .collect(
                                    Collectors.groupingBy(
                                        Map.Entry::getKey,
                                        Collectors.mapping(
                                            Map.Entry::getValue,
                                            Collectors.reducing(
                                                "",
                                                (a, b) -> b
                                            )
                                        )
                                    )
                                );
        return reducedCookies.entrySet().stream()
            .map((entry) ->
                     MessageFormat.format("{0}={1}", entry.getKey(), entry.getValue())
            )
            .collect(Collectors.joining(";"));
    }
}