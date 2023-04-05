/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;

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

    /**
     * Retrieve the CC License URI based on the provided license id, language and answers to the field questions from
     * the CC License API
     *
     * @param licenseId - the ID of the license
     * @param language  - the language for which to retrieve the full answerMap
     * @param answerMap - the answers to the different field questions
     * @return the CC License URI
     */
    public String retrieveRightsByQuestion(String licenseId,
                                           String language,
                                           Map<String, String> answerMap);

    /**
     * Retrieve the license RDF document based on the license URI
     *
     * @param licenseURI    - The license URI for which to retrieve the license RDF document
     * @return the license RDF document
     * @throws IOException
     */
    public Document retrieveLicenseRDFDoc(String licenseURI) throws IOException;

    /**
     * Retrieve the license Name from the license document
     *
     * @param doc   - The license document from which to retrieve the license name
     * @return the license name
     */
    public String retrieveLicenseName(final Document doc);

}
