/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to split a list of {@code MatomoRequestDetails} into a map
 * of {@code List<MatomoRequestDetails>}. <br/>
 * The key of the map is the value of the parameter named "_id", or "default" if the "_id" is not set.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestDetailsSplitter {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MatomoRequestDetailsSplitter () { }

    /**
     * Splits a list of MatomoRequestDetails into a map grouped by their "_id" parameter.
     *
     * @param details The list of MatomoRequestDetails to split
     * @return A Map where the key is the "_id" parameter value (or "default" if not set) and
     *         the value is a List of MatomoRequestDetails with that "_id"
     */
    public static Map<String, List<MatomoRequestDetails>> split(List<MatomoRequestDetails> details) {
        return details.stream().collect(
                Collectors.groupingBy(
                    detail -> detail.parameters.getOrDefault("_id","default"))
            );
    }

}