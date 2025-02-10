/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the details of a single request
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestDetails {

    final Map<String, String> parameters = new HashMap<>();

    public MatomoRequestDetails addParameter(String key, String value) {
        parameters.put(key, value);
        return this;
    }

}

