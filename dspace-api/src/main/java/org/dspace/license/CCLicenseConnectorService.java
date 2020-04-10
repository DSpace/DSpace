/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.util.Map;

/**
 * Service interface class for the Creative commons license connector service.
 * The implementation of this class is responsible for all the calls to the CC license API and parsing the response
 * The service is autowired by spring
 */
public interface CCLicenseConnectorService {

    /**
     * Retrieves the CC Licenses for the provided language from the CC License API
     *
     * @param language - the language to retrieve the licenses for
     * @return a map of licenses with the id and the license for the provided language
     */
    public Map<String, CCLicense> retrieveLicenses(String language);

}
