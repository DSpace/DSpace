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

    public static String convert(MatomoRequestDetails matomoRequestDetails) {
        return matomoRequestDetails.cookies
                    .entrySet()
                    .stream()
                    .map((entry) ->
                             MessageFormat.format("{0}={1}", entry.getKey(), entry.getValue())
                    )
                    .collect(Collectors.joining(";"));
    }

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
