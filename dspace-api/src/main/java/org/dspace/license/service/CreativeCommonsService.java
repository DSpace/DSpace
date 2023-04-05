/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.CCLicense;
import org.jdom2.Document;

/**
 * Service interface class for the Creative commons licensing.
 * The implementation of this class is responsible for all business logic calls for the Creative commons licensing
 * and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CreativeCommonsService {

    public static final String CC_BUNDLE_NAME = "CC-LICENSE";

    /**
     * setLicenseRDF
     *
     * CC Web Service method for setting the RDF bitstream
     *
     * @param context    The relevant DSpace Context.
     * @param item       The item to set license on.
     * @param licenseRdf license RDF string
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public void setLicenseRDF(Context context, Item item, String licenseRdf)
            throws SQLException, IOException, AuthorizeException;


    /**
     * Used by DSpaceMetsIngester
     *
     * @param context    The relevant DSpace Context.
     * @param item       The item to set license on.
     * @param licenseStm InputStream with the license text.
     * @param mimeType   License text file MIME type ("text/xml", "text/rdf" or generic)
     * @throws SQLException       if database error
     *                            An exception that provides information on a database access error or other errors.
     * @throws IOException        if IO error
     *                            A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws AuthorizeException if authorization error
     *                            Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     *
     *                            * // PATCHED 12/01 FROM JIRA re: mimetypes for CCLicense and License RDF wjb
     */
    public void setLicense(Context context, Item item,
                           InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException;

    /**
     * Removes the license file from the item
     *
     * @param context   - The relevant DSpace Context
     * @param item      - The item from which the license file needs to be removed
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void removeLicenseFile(Context context, Item item)
            throws SQLException, IOException, AuthorizeException;


    public String getLicenseURL(Context context, Item item)
            throws SQLException, IOException, AuthorizeException;


    /**
     * Returns the stored license uri of the item
     *
     * @param item  - The item for which to retrieve the stored license uri
     * @return the stored license uri of the item
     */
    public String getLicenseURI(Item item);

    /**
     * Returns the stored license name of the item
     *
     * @param item  - The item for which to retrieve the stored license name
     * @return the stored license name of the item
     */
    public String getLicenseName(Item item);

    /**
     * Get Creative Commons license RDF, returning Bitstream object.
     *
     * @param item bitstream's parent item
     * @return bitstream or null.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public Bitstream getLicenseRdfBitstream(Item item)
            throws SQLException, IOException, AuthorizeException;

    /**
     * Get Creative Commons license Text, returning Bitstream object.
     *
     * @param item bitstream's parent item
     * @return bitstream or null.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     * @deprecated the bitstream with the license in the textual format it
     * is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
     */
    @Deprecated
    public Bitstream getLicenseTextBitstream(Item item)
            throws SQLException, IOException, AuthorizeException;

    /**
     * Get a few license-specific properties. We expect these to be cached at
     * least per server run.
     *
     * @param fieldId name of the property.
     * @return its value.
     */
    public String getCCField(String fieldId);

    /**
     * Apply same transformation on the document to retrieve only the most
     * relevant part of the document passed as parameter. If no transformation
     * is needed then take in consideration to empty the CreativeCommons.xml
     *
     * @param license - an element that could be contains as part of your content
     *                the license rdf
     * @return the document license in textual format after the transformation
     */
    public String fetchLicenseRDF(Document license);

    /**
     * Remove license information, delete also the bitstream
     *
     * @param context   - DSpace Context
     * @param item      - the item
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     */
    public void removeLicense(Context context, Item item)
            throws AuthorizeException, IOException, SQLException;

    /**
     * Find all CC Licenses using the default language found in the configuration
     *
     * @return A list of available CC Licenses
     */
    public List<CCLicense> findAllCCLicenses();

    /**
     * Find all CC Licenses for the provided language
     *
     * @param language - the language for which to find the CC Licenses
     * @return A list of available CC Licenses for the provided language
     */
    public List<CCLicense> findAllCCLicenses(String language);

    /**
     * Find the CC License corresponding to the provided ID using the default language found in the configuration
     *
     * @param id - the ID of the license to be found
     * @return the corresponding license if found or null when not found
     */
    public CCLicense findOne(String id);

    /**
     * Find the CC License corresponding to the provided ID and provided language
     *
     * @param id       - the ID of the license to be found
     * @param language - the language for which to find the CC License
     * @return the corresponding license if found or null when not found
     */
    public CCLicense findOne(String id, String language);

    /**
     * Retrieve the CC License URI for the provided license ID, based on the provided answers, using the default
     * language found in the configuration
     *
     * @param licenseId - the ID of the license
     * @param answerMap - the answers to the different field questions
     * @return the corresponding license URI
     */
    public String retrieveLicenseUri(String licenseId, Map<String, String> answerMap);

    /**
     * Retrieve the CC License URI for the provided license ID and language based on the provided answers
     *
     * @param licenseId - the ID of the license
     * @param language  - the language for which to find the CC License URI
     * @param answerMap - the answers to the different field questions
     * @return the corresponding license URI
     */
    public String retrieveLicenseUri(String licenseId, String language, Map<String, String> answerMap);

    /**
     * Retrieve the full answer map containing empty values when an answer for a field was not provided in the
     * answerMap, using the default language found in the configuration
     *
     * @param licenseId - the ID of the license
     * @param answerMap - the answers to the different field questions
     * @return the answerMap supplemented with all other license fields with a blank answer
     */
    public Map<String, String> retrieveFullAnswerMap(String licenseId, Map<String, String> answerMap);

    /**
     * Retrieve the full answer map for a provided language, containing empty values when an answer for a field was not
     * provided in the answerMap.
     *
     * @param licenseId - the ID of the license
     * @param language  - the language for which to retrieve the full answerMap
     * @param answerMap - the answers to the different field questions
     * @return the answerMap supplemented with all other license fields with a blank answer for the provided language
     */
    public Map<String, String> retrieveFullAnswerMap(String licenseId, String language, Map<String, String> answerMap);

    /**
     * Verify whether the answer map contains a valid response to all field questions and no answers that don't have a
     * corresponding question in the license, using the default language found in the config to check the license
     *
     * @param licenseId     - the ID of the license
     * @param fullAnswerMap - the answers to the different field questions
     * @return whether the information is valid
     */
    public boolean verifyLicenseInformation(String licenseId, Map<String, String> fullAnswerMap);

    /**
     * Verify whether the answer map contains a valid response to all field questions and no answers that don't have a
     * corresponding question in the license, using the provided language to check the license
     *
     * @param licenseId     - the ID of the license
     * @param language      - the language for which to retrieve the full answerMap
     * @param fullAnswerMap - the answers to the different field questions
     * @return whether the information is valid
     */
    public boolean verifyLicenseInformation(String licenseId, String language, Map<String, String> fullAnswerMap);

    /**
     * Update the license of the item with a new one based on the provided license URI
     *
     * @param context       - The relevant DSpace context
     * @param licenseUri    - The license URI to be used in the update
     * @param item          - The item for which to update the license
     * @return true when the update was successful, false when not
     * @throws AuthorizeException
     * @throws SQLException
     */
    public boolean updateLicense(final Context context, String licenseUri, final Item item)
            throws AuthorizeException, SQLException;

    /**
     * Add a new license to the item
     *
     * @param context       - The relevant Dspace context
     * @param item          - The item to which the license will be added
     * @param licenseUri    - The license URI to add
     * @param licenseName   - The license name to add
     * @param doc           - The license to document to add
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void addLicense(Context context, Item item, String licenseUri, String licenseName, Document doc)
            throws SQLException, IOException, AuthorizeException;
}
