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
import java.util.List;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class CreativeCommonsServiceImpl implements CreativeCommonsService, InitializingBean
{
    /** log4j category */
    private static Logger log = Logger.getLogger(CreativeCommonsServiceImpl.class);

    /**
     * The Bundle Name
     */

    protected static final String CC_BS_SOURCE = "org.dspace.license.CreativeCommons";

    /**
     * Some BitStream Names (BSN)
     * 
     * @deprecated use the metadata retrieved at {@link CreativeCommonsService#getCCField(String)} (see https://jira.duraspace.org/browse/DS-2604)
     */
    @Deprecated
    protected static final String BSN_LICENSE_URL = "license_url";

    /**
     * @deprecated to make uniform JSPUI and XMLUI approach the bitstream with the license in the textual format it is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
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
    
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    protected CreativeCommonsServiceImpl()
    {
        
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        // if defined, set a proxy server for http requests to Creative
        // Commons site
        String proxyHost = configurationService.getProperty("http.proxy.host");
        String proxyPort = configurationService.getProperty("http.proxy.port");

        if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort))
        {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
        
        try
        {
            templates = TransformerFactory.newInstance().newTemplates(
                        new StreamSource(CreativeCommonsServiceImpl.class
                                .getResourceAsStream("CreativeCommons.xsl")));
        }
        catch (TransformerConfigurationException e)
        {
            throw new RuntimeException(e.getMessage(),e);
        }
       
        
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

        // create the CC bundle if it doesn't exist
        // If it does, remove it and create a new one.
    protected Bundle getCcBundle(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

        if ((bundles.size() > 0) && (bundles.get(0) != null))
        {
            itemService.removeBundle(context, item, bundles.get(0));
        }
        return bundleService.create(context, item, CC_BUNDLE_NAME);
    }

    @Override
    public void setLicenseRDF(Context context, Item item, String licenseRdf)
        throws SQLException, IOException,
            AuthorizeException
    {
        Bundle bundle = getCcBundle(context, item);
        // set the format
        BitstreamFormat bs_rdf_format = bitstreamFormatService.findByShortDescription(context, "RDF XML");
        // set the RDF bitstream
        setBitstreamFromBytes(context, item, bundle, BSN_LICENSE_RDF, bs_rdf_format, licenseRdf.getBytes());
    }
    

    @Override
    public void setLicense(Context context, Item item,
                                  InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle bundle = getCcBundle(context, item);

     // set the format
        BitstreamFormat bs_format;
        if (mimeType.equalsIgnoreCase("text/xml"))
        {
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


    @Override
    public void removeLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // remove CC license bundle if one exists
        List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

        if ((bundles.size() > 0) && (bundles.get(0) != null))
        {
            itemService.removeBundle(context, item, bundles.get(0));
        }
    }

    @Override
    public boolean hasLicense(Context context, Item item)
            throws SQLException, IOException
    {
        // try to find CC license bundle
        List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

        if (bundles.size() == 0)
        {
            return false;
        }

        // verify it has correct contents
        try
        {
            if ((getLicenseURL(context, item) == null))
            {
                return false;
            }
        }
        catch (AuthorizeException ae)
        {
            return false;
        }

        return true;
    }

    @Override
    public String getLicenseRDF(Context context, Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(context, item, BSN_LICENSE_RDF);
    }

    @Override
    public Bitstream getLicenseRdfBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_RDF);
    }

    @Deprecated
    @Override
    public Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }

    @Override
    public String getLicenseURL(Context context, Item item) throws SQLException, IOException, AuthorizeException {
        String licenseUri = getCCField("uri").ccItemValue(item);
        if (StringUtils.isNotBlank(licenseUri)) {
            return licenseUri;
        }
        
        // JSPUI backward compatibility see https://jira.duraspace.org/browse/DS-2604
        return getStringFromBitstream(context, item, BSN_LICENSE_URL);
    }

    @Override
    public String fetchLicenseRDF(Document license)
    {
        StringWriter result = new StringWriter();
        
        try
        {
            templates.newTransformer().transform(
                    new JDOMSource(license),
                    new StreamResult(result)
                    );
        }
        catch (TransformerException e)
        {
            throw new IllegalStateException(e.getMessage(),e);
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
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     parent item
     * @param bundle
     *     parent bundle
     * @param bitstream_name
     *     bitstream name to set
     * @param format
     *     bitstream format
     * @param bytes
     *     bitstream data
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    protected void setBitstreamFromBytes(Context context, Item item, Bundle bundle,
            String bitstream_name, BitstreamFormat format, byte[] bytes)
            throws SQLException, IOException, AuthorizeException
    {
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
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     parent item
     * @param bitstream_name
     *     bitstream name to set
     * @return the bitstream as string
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    protected String getStringFromBitstream(Context context, Item item,
            String bitstream_name) throws SQLException, IOException,
            AuthorizeException
    {
        byte[] bytes = getBytesFromBitstream(context, item, bitstream_name);

        if (bytes == null)
        {
            return null;
        }

        return new String(bytes);
    }

    /**
     * This helper method retrieves the bytes of a bitstream for an item under
     * the CC bundle, with the given bitstream name
     *
     * @param item
     *     parent item
     * @param bitstream_name
     *     bitstream name to set
     * @return the bitstream
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    protected Bitstream getBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle cc_bundle = null;

        // look for the CC bundle
        try
        {
            List<Bundle> bundles = itemService.getBundles(item, CC_BUNDLE_NAME);

            if ((bundles != null) && (bundles.size() > 0))
            {
                cc_bundle = bundles.get(0);
            }
            else
            {
                return null;
            }
        }
        catch (Exception exc)
        {
            // this exception catching is a bit generic,
            // but basically it happens if there is no CC bundle
            return null;
        }

        return bundleService.getBitstreamByName(cc_bundle, bitstream_name);
    }

    protected byte[] getBytesFromBitstream(Context context, Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
        Bitstream bs = getBitstream(item, bitstream_name);

        // no such bitstream
        if (bs == null)
        {
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
    public LicenseMetadataValue getCCField(String fieldId)
    {
        return new LicenseMetadataValue(configurationService.getProperty("cc.license." + fieldId));
    }
    
    @Override
    public void removeLicense(Context context, LicenseMetadataValue uriField,
        LicenseMetadataValue nameField, Item item)
        throws AuthorizeException, IOException, SQLException
    {
        // only remove any previous licenses
        String licenseUri = uriField.ccItemValue(item);
        if (licenseUri != null) {
            uriField.removeItemValue(context, item, licenseUri);
            if (configurationService.getBooleanProperty("cc.submit.setname"))
            {
                String licenseName = nameField.keyedItemValue(item, licenseUri);
                nameField.removeItemValue(context, item, licenseName);
            }
            if (configurationService.getBooleanProperty("cc.submit.addbitstream"))
            {
                removeLicense(context, item);
            }
        }
    }

}
