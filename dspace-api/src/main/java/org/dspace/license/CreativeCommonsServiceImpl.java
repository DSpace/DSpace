/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Document;
import org.jdom2.transform.JDOMSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class CreativeCommonsServiceImpl implements CreativeCommonsService, InitializingBean {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(CreativeCommonsServiceImpl.class);

    /**
     * The Bundle Name
     */

    protected static final String CC_BS_SOURCE = "org.dspace.license.CreativeCommons";

    /**
     * Some BitStream Names (BSN)
     *
     * @deprecated use the metadata retrieved at {@link CreativeCommonsService#getCCField(String)} (see https://jira
     * .duraspace.org/browse/DS-2604)
     */
    @Deprecated
    protected static final String BSN_LICENSE_URL = "license_url";

    /**
     * @deprecated the bitstream with the license in the textual format it
     * is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
     */
    @Deprecated
    protected static final String BSN_LICENSE_TEXT = "license_text";

    protected static final String BSN_LICENSE_RDF = "license_rdf";

    protected Templates templates;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired
    protected CCLicenseConnectorService ccLicenseConnectorService;

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private String defaultLanguage;
    private String jurisdiction;
    private static final String JURISDICTION_KEY = "jurisdiction";


    private Map<String, Map<String, CCLicense>> ccLicenses;

    protected CreativeCommonsServiceImpl() {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // if defined, set a proxy server for http requests to Creative
        // Commons site
        String proxyHost = configurationService.getProperty("http.proxy.host");
        String proxyPort = configurationService.getProperty("http.proxy.port");

        if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }

        ccLicenses = new HashMap<>();
        defaultLanguage = configurationService.getProperty("cc.license.locale", "en");
        jurisdiction = configurationService.getProperty("cc.license.jurisdiction", "");

        try {
            templates = TransformerFactory.newInstance().newTemplates(
                    new StreamSource(CreativeCommonsServiceImpl.class
                                             .getResourceAsStream("CreativeCommons.xsl")));
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }


    }

    // create the CC bundle if it doesn't exist
    // If it does, remove it and create a new one.
    protected Bundle getCcBundle(Context context, Item item)
            throws SQLException, AuthorizeException, IOException {
        List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

        if ((bundles.size() > 0) && (bundles.get(0) != null)) {
            itemService.removeBundle(context, item, bundles.get(0));
        }
        return bundleService.create(context, item, CC_BUNDLE_NAME);
    }

    @Override
    public void setLicenseRDF(Context context, Item item, String licenseRdf)
            throws SQLException, IOException,
            AuthorizeException {
        Bundle bundle = getCcBundle(context, item);
        // set the format
        BitstreamFormat bs_rdf_format = bitstreamFormatService.findByShortDescription(context, "RDF XML");
        // set the RDF bitstream
        setBitstreamFromBytes(context, item, bundle, BSN_LICENSE_RDF, bs_rdf_format, licenseRdf.getBytes());
    }


    @Override
    public void setLicense(Context context, Item item,
                           InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException {
        Bundle bundle = getCcBundle(context, item);

        // set the format
        BitstreamFormat bs_format;
        if (mimeType.equalsIgnoreCase("text/xml")) {
            bs_format = bitstreamFormatService.findByShortDescription(context, "CC License");
        } else if (mimeType.equalsIgnoreCase("text/rdf")) {
            bs_format = bitstreamFormatService.findByShortDescription(context, "RDF XML");
        } else {
            bs_format = bitstreamFormatService.findByShortDescription(context, "License");
        }

        Bitstream bs = bitstreamService.create(context, bundle, licenseStm);
        bs.setSource(context, CC_BS_SOURCE);
        bs.setName(context, (mimeType != null &&
                (mimeType.equalsIgnoreCase("text/xml") ||
                        mimeType.equalsIgnoreCase("text/rdf"))) ?
                BSN_LICENSE_RDF : BSN_LICENSE_TEXT);
        bs.setFormat(context, bs_format);
        bitstreamService.update(context, bs);
    }


    /**
     * Removes the license file from the item
     *
     * @param context   - The relevant DSpace Context
     * @param item      - The item from which the license file needs to be removed
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public void removeLicenseFile(Context context, Item item)
            throws SQLException, IOException, AuthorizeException {
        // remove CC license bundle if one exists
        List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

        if ((bundles.size() > 0) && (bundles.get(0) != null)) {
            itemService.removeBundle(context, item, bundles.get(0));
        }
    }

    @Override
    public Bitstream getLicenseRdfBitstream(Item item) throws SQLException,
            IOException, AuthorizeException {
        return getBitstream(item, BSN_LICENSE_RDF);
    }

    @Deprecated
    @Override
    public Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }

    @Override
    public String getLicenseURL(Context context, Item item) throws SQLException, IOException, AuthorizeException {
        String licenseUri = getCCField("uri");
        if (StringUtils.isNotBlank(licenseUri)) {
            return getLicenseURI(item);
        }

        // backward compatibility see https://jira.duraspace.org/browse/DS-2604
        return getStringFromBitstream(context, item, BSN_LICENSE_URL);
    }

    /**
     * Returns the stored license uri of the item
     *
     * @param item  - The item for which to retrieve the stored license uri
     * @return the stored license uri of the item
     */
    @Override
    public String getLicenseURI(Item item) {
        String licenseUriField = getCCField("uri");
        if (StringUtils.isNotBlank(licenseUriField)) {
            String metadata = itemService.getMetadata(item, licenseUriField);
            if (StringUtils.isNotBlank(metadata)) {
                return metadata;
            }
        }
        return null;
    }

    /**
     * Returns the stored license name of the item
     *
     * @param item  - The item for which to retrieve the stored license name
     * @return the stored license name of the item
     */
    @Override
    public String getLicenseName( Item item) {
        String licenseNameField = getCCField("name");
        if (StringUtils.isNotBlank(licenseNameField)) {
            String metadata = itemService.getMetadata(item, licenseNameField);
            if (StringUtils.isNotBlank(metadata)) {
                return metadata;
            }
        }
        return null;
    }

    @Override
    public String fetchLicenseRDF(Document license) {
        StringWriter result = new StringWriter();

        try {
            templates.newTransformer().transform(
                    new JDOMSource(license),
                    new StreamResult(result)
            );
        } catch (TransformerException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return result.getBuffer().toString();
    }

    /**
     * This helper method takes some bytes and stores them as a bitstream for an
     * item, under the CC bundle, with the given bitstream name
     *
     * Note: This helper method assumes that the CC
     * bitstreams are short and easily expressed as byte arrays in RAM
     *
     * @param context        The relevant DSpace Context.
     * @param item           parent item
     * @param bundle         parent bundle
     * @param bitstream_name bitstream name to set
     * @param format         bitstream format
     * @param bytes          bitstream data
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    protected void setBitstreamFromBytes(Context context, Item item, Bundle bundle,
                                         String bitstream_name, BitstreamFormat format, byte[] bytes)
            throws SQLException, IOException, AuthorizeException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Bitstream bs = bitstreamService.create(context, bundle, bais);

        bs.setName(context, bitstream_name);
        bs.setSource(context, CC_BS_SOURCE);
        bs.setFormat(context, format);

        // commit everything
        bitstreamService.update(context, bs);
    }

    /**
     * This helper method wraps a String around a byte array returned from the
     * bitstream method further down
     *
     * Note: This helper method assumes that the CC
     * bitstreams are short and easily expressed as byte arrays in RAM
     *
     * @param context        The relevant DSpace Context.
     * @param item           parent item
     * @param bitstream_name bitstream name to set
     * @return the bitstream as string
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    protected String getStringFromBitstream(Context context, Item item,
                                            String bitstream_name) throws SQLException, IOException,
            AuthorizeException {
        byte[] bytes = getBytesFromBitstream(context, item, bitstream_name);

        if (bytes == null) {
            return null;
        }

        return new String(bytes);
    }

    /**
     * This helper method retrieves the bytes of a bitstream for an item under
     * the CC bundle, with the given bitstream name
     *
     * @param item           parent item
     * @param bitstream_name bitstream name to set
     * @return the bitstream
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    protected Bitstream getBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException {
        Bundle cc_bundle = null;

        // look for the CC bundle
        try {
            List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

            if ((bundles != null) && (bundles.size() > 0)) {
                cc_bundle = bundles.get(0);
            } else {
                return null;
            }
        } catch (Exception exc) {
            // this exception catching is a bit generic,
            // but basically it happens if there is no CC bundle
            return null;
        }

        return bundleService.getBitstreamByName(cc_bundle, bitstream_name);
    }

    protected byte[] getBytesFromBitstream(Context context, Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException {
        Bitstream bs = getBitstream(item, bitstream_name);

        // no such bitstream
        if (bs == null) {
            return null;
        }

        // create a ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.copy(bitstreamService.retrieve(context, bs), baos);

        return baos.toByteArray();
    }

    /**
     * Returns a metadata field handle for given field Id
     */
    @Override
    public String getCCField(String fieldId) {
        return configurationService.getProperty("cc.license." + fieldId);
    }

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
    @Override
    public void removeLicense(Context context, Item item)
            throws AuthorizeException, IOException, SQLException {

        String uriField = getCCField("uri");
        String nameField = getCCField("name");

        String licenseUri = itemService.getMetadata(item, uriField);

        // only remove any previous licenses
        if (licenseUri != null) {
            removeLicenseField(context, item, uriField);
            if (configurationService.getBooleanProperty("cc.submit.setname")) {
                removeLicenseField(context, item, nameField);
            }
            if (configurationService.getBooleanProperty("cc.submit.addbitstream")) {
                removeLicenseFile(context, item);
            }
        }
    }

    private void removeLicenseField(Context context, Item item, String field) throws SQLException {
        String[] params = splitField(field);
        itemService.clearMetadata(context, item, params[0], params[1], params[2], params[3]);

    }

    private void addLicenseField(Context context, Item item, String field, String value) throws SQLException {
        String[] params = splitField(field);
        itemService.addMetadata(context, item, params[0], params[1], params[2], params[3], value);

    }

    /**
     * Find all CC Licenses using the default language found in the configuration
     *
     * @return A list of available CC Licenses
     */
    @Override
    public List<CCLicense> findAllCCLicenses() {
        return findAllCCLicenses(defaultLanguage);
    }

    /**
     * Find all CC Licenses for the provided language
     *
     * @param language - the language for which to find the CC Licenses
     * @return A list of available CC Licenses for the provided language
     */
    @Override
    public List<CCLicense> findAllCCLicenses(String language) {

        if (!ccLicenses.containsKey(language)) {
            initLicenses(language);
        }
        return new LinkedList<>(ccLicenses.get(language).values());
    }

    /**
     * Find the CC License corresponding to the provided ID using the default language found in the configuration
     *
     * @param id - the ID of the license to be found
     * @return the corresponding license if found or null when not found
     */
    @Override
    public CCLicense findOne(String id) {
        return findOne(id, defaultLanguage);
    }

    /**
     * Find the CC License corresponding to the provided ID and provided language
     *
     * @param id       - the ID of the license to be found
     * @param language - the language for which to find the CC License
     * @return the corresponding license if found or null when not found
     */
    @Override
    public CCLicense findOne(String id, String language) {
        if (!ccLicenses.containsKey(language)) {
            initLicenses(language);
        }
        Map<String, CCLicense> licenseMap = ccLicenses.get(language);
        if (licenseMap.containsKey(id)) {
            return licenseMap.get(id);
        }
        return null;
    }

    /**
     * Retrieves the licenses for a specific language and cache them in this service
     *
     * @param language - the language for which to find the CC Licenses
     */
    private void initLicenses(final String language) {
        Map<String, CCLicense> licenseMap = ccLicenseConnectorService.retrieveLicenses(language);
        ccLicenses.put(language, licenseMap);
    }

    /**
     * Retrieve the CC License URI for the provided license ID, based on the provided answers, using the default
     * language found in the configuration
     *
     * @param licenseId - the ID of the license
     * @param answerMap - the answers to the different field questions
     * @return the corresponding license URI
     */
    @Override
    public String retrieveLicenseUri(String licenseId, Map<String, String> answerMap) {
        return retrieveLicenseUri(licenseId, defaultLanguage, answerMap);

    }

    /**
     * Retrieve the CC License URI for the provided license ID and language based on the provided answers
     *
     * @param licenseId - the ID of the license
     * @param language  - the language for which to find the CC License URI
     * @param answerMap - the answers to the different field questions
     * @return the corresponding license URI
     */
    @Override
    public String retrieveLicenseUri(String licenseId, String language, Map<String, String> answerMap) {
        return ccLicenseConnectorService.retrieveRightsByQuestion(licenseId, language, answerMap);

    }

    /**
     * Verify whether the answer map contains a valid response to all field questions and no answers that don't have a
     * corresponding question in the license, using the default language found in the config to check the license
     *
     * @param licenseId     - the ID of the license
     * @param fullAnswerMap - the answers to the different field questions
     * @return whether the information is valid
     */
    @Override
    public boolean verifyLicenseInformation(String licenseId, Map<String, String> fullAnswerMap) {
        return verifyLicenseInformation(licenseId, defaultLanguage, fullAnswerMap);
    }

    /**
     * Verify whether the answer map contains a valid response to all field questions and no answers that don't have a
     * corresponding question in the license, using the provided language to check the license
     *
     * @param licenseId     - the ID of the license
     * @param language      - the language for which to retrieve the full answerMap
     * @param fullAnswerMap - the answers to the different field questions
     * @return whether the information is valid
     */
    @Override
    public boolean verifyLicenseInformation(String licenseId, String language, Map<String, String> fullAnswerMap) {
        CCLicense ccLicense = findOne(licenseId, language);

        List<CCLicenseField> ccLicenseFieldList = ccLicense.getCcLicenseFieldList();

        for (String field : fullAnswerMap.keySet()) {
            CCLicenseField ccLicenseField = findCCLicenseField(field, ccLicenseFieldList);
            if (ccLicenseField == null) {
                return false;
            }
            if (!containsAnswerEnum(fullAnswerMap.get(field), ccLicenseField)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieve the full answer map containing empty values when an answer for a field was not provided in the
     * answerMap, using the default language found in the configuration
     *
     * @param licenseId - the ID of the license
     * @param answerMap - the answers to the different field questions
     * @return the answerMap supplemented with all other license fields with a blank answer
     */
    @Override
    public Map<String, String> retrieveFullAnswerMap(String licenseId, Map<String, String> answerMap) {
        return retrieveFullAnswerMap(licenseId, defaultLanguage, answerMap);
    }

    /**
     * Retrieve the full answer map for a provided language, containing empty values when an answer for a field was not
     * provided in the answerMap.
     *
     * @param licenseId - the ID of the license
     * @param language  - the language for which to retrieve the full answerMap
     * @param answerMap - the answers to the different field questions
     * @return the answerMap supplemented with all other license fields with a blank answer for the provided language
     */
    @Override
    public Map<String, String> retrieveFullAnswerMap(String licenseId, String language, Map<String, String> answerMap) {
        CCLicense ccLicense = findOne(licenseId, language);
        if (ccLicense == null) {
            return null;
        }
        Map<String, String> fullParamMap = new HashMap<>(answerMap);
        List<CCLicenseField> ccLicenseFieldList = ccLicense.getCcLicenseFieldList();
        for (CCLicenseField ccLicenseField : ccLicenseFieldList) {
            if (!fullParamMap.containsKey(ccLicenseField.getId())) {
                fullParamMap.put(ccLicenseField.getId(), "");
            }
        }

        updateJurisdiction(fullParamMap);

        return fullParamMap;
    }

    private void updateJurisdiction(final Map<String, String> fullParamMap) {
        if (fullParamMap.containsKey(JURISDICTION_KEY)) {
            fullParamMap.put(JURISDICTION_KEY, jurisdiction);
        }
    }

    private boolean containsAnswerEnum(final String enumAnswer, final CCLicenseField ccLicenseField) {
        List<CCLicenseFieldEnum> fieldEnums = ccLicenseField.getFieldEnum();
        for (CCLicenseFieldEnum fieldEnum : fieldEnums) {
            if (StringUtils.equals(fieldEnum.getId(), enumAnswer)) {
                return true;
            }
        }
        return false;
    }

    private CCLicenseField findCCLicenseField(final String field, final List<CCLicenseField> ccLicenseFieldList) {
        for (CCLicenseField ccLicenseField : ccLicenseFieldList) {
            if (StringUtils.equals(ccLicenseField.getId(), field)) {
                return ccLicenseField;
            }
        }

        return null;
    }

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
    @Override
    public boolean updateLicense(final Context context, final String licenseUri, final Item item)
            throws AuthorizeException, SQLException {
        try {
            Document doc = ccLicenseConnectorService.retrieveLicenseRDFDoc(licenseUri);
            if (doc == null) {
                return false;
            }
            String licenseName = ccLicenseConnectorService.retrieveLicenseName(doc);
            if (StringUtils.isBlank(licenseName)) {
                return false;
            }

            removeLicense(context, item);
            addLicense(context, item, licenseUri, licenseName, doc);

            return true;

        } catch (IOException e) {
            log.error("Error while updating the license of item: " + item.getID(), e);
        }
        return false;
    }

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
    @Override
    public void addLicense(Context context, Item item, String licenseUri, String licenseName, Document doc)
            throws SQLException, IOException, AuthorizeException {
        String uriField = getCCField("uri");
        String nameField = getCCField("name");

        addLicenseField(context, item, uriField, licenseUri);
        if (configurationService.getBooleanProperty("cc.submit.addbitstream")) {
            setLicenseRDF(context, item, fetchLicenseRDF(doc));
        }
        if (configurationService.getBooleanProperty("cc.submit.setname")) {
            addLicenseField(context, item, nameField, licenseName);
        }
    }

    private String[] splitField(String fieldName) {
        String[] params = new String[4];
        String[] fParams = fieldName.split("\\.");
        for (int i = 0; i < fParams.length; i++) {
            params[i] = fParams[i];
        }
        params[3] = Item.ANY;
        return params;
    }

}
