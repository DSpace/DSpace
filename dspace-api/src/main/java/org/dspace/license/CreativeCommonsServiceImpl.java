/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class CreativeCommonsServiceImpl implements CreativeCommonsService, InitializingBean
{
    /** log4j category */
    private static Logger log = Logger.getLogger(CreativeCommonsServiceImpl.class);

    // HTTP RFC says do not automatically follow more than 5 redirects, it indicates a probable loop
    // http://www.freesoft.org/CIE/RFC/1945/46.htm
    private static final int MAX_REDIRECTS = 5;

    /**
     * The Bundle Name
     */

    protected static final String CC_BS_SOURCE = "org.dspace.license.CreativeCommons";

    /**
     * Some BitStream Names (BSN)
     */
    protected static final String BSN_LICENSE_URL = "license_url";

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

    protected CreativeCommonsServiceImpl()
    {

    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        // if defined, set a proxy server for http requests to Creative
        // Commons site
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
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
            String cc_license_url) throws SQLException, IOException,
            AuthorizeException
    {
        Bundle bundle = getCcBundle(context, item);

        // get some more information
        String license_text = fetchLicenseText(cc_license_url);
        String license_rdf = fetchLicenseRDF(cc_license_url);
        
        // set the formats
        BitstreamFormat bs_url_format = bitstreamFormatService.findByShortDescription(
                context, "License");
        BitstreamFormat bs_text_format = bitstreamFormatService.findByShortDescription(
                context, "CC License");
        BitstreamFormat bs_rdf_format = bitstreamFormatService.findByShortDescription(
                context, "RDF XML");

        // set the URL bitstream
        setBitstreamFromBytes(context, item, bundle, BSN_LICENSE_URL, bs_url_format,
                cc_license_url.getBytes());

        // set the license text bitstream
        setBitstreamFromBytes(context, item, bundle, BSN_LICENSE_TEXT, bs_text_format,
                license_text.getBytes());

        // set the RDF bitstream
        setBitstreamFromBytes(context, item, bundle, BSN_LICENSE_RDF, bs_rdf_format,
                license_rdf.getBytes());
    }

    @Override
    public void setLicense(Context context, Item item,
                                  InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle bundle = getCcBundle(context, item);

     // set the format
        BitstreamFormat bs_format;
        if ("text/xml".equalsIgnoreCase(mimeType))
        {
        	bs_format = bitstreamFormatService.findByShortDescription(context, "CC License");
        } else if ("text/rdf".equalsIgnoreCase(mimeType)) {
            bs_format = bitstreamFormatService.findByShortDescription(context, "RDF XML");
        } else {
        	bs_format = bitstreamFormatService.findByShortDescription(context, "License");
        }

        Bitstream bs = bitstreamService.create(context, bundle, licenseStm);
        bs.setSource(context, CC_BS_SOURCE);
        bs.setName(context, "text/xml".equalsIgnoreCase(mimeType) || "text/rdf".equalsIgnoreCase(mimeType) ?
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
            if ((getLicenseURL(context, item) == null) || (getLicenseText(context, item) == null)
                    || (getLicenseRDF(context, item) == null))
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
    public String getLicenseURL(Context context, Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(context, item, BSN_LICENSE_URL);
    }

    @Override
    public String getLicenseText(Context context, Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(context, item, BSN_LICENSE_TEXT);
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

    @Override
    public Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }

    @Override
    public String fetchLicenseRdf(String ccResult) {
        StringWriter result = new StringWriter();
        try {
    		InputStream inputstream = new ByteArrayInputStream(ccResult.getBytes("UTF-8"));
    		templates.newTransformer().transform(new StreamSource(inputstream), new StreamResult(result));
    	} catch (TransformerException te) {
    		throw new RuntimeException("Transformer exception " + te.getMessage(), te);
    	} catch (IOException ioe) {
    		throw new RuntimeException("IOexception " + ioe.getCause().toString(), ioe);
    	} finally {
    		return result.getBuffer().toString();
    	}
    }
    

    @Override
    public String fetchLicenseText(String license_url)
    {
        String text = fetchURL(license_url, 0);

        return Objects.toString(text, "");
    }

    @Override
    public String fetchLicenseRDF(String license_url)
    {
        StringWriter result = new StringWriter();
        
        try
        {
            templates.newTransformer().transform(
                    new StreamSource(license_url + "rdf"),
                    new StreamResult(result)
                    );
        }
        catch (TransformerException e)
        {
            throw new IllegalStateException(e.getMessage(),e);
        }

        return result.getBuffer().toString();
    }

    // The following two helper methods assume that the CC
    // bitstreams are short and easily expressed as byte arrays in RAM

    /**
     * This helper method takes some bytes and stores them as a bitstream for an
     * item, under the CC bundle, with the given bitstream name
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
     * Fetch the contents of a URL
     */
    private String fetchURL(String url_string, int numRedirects)
    {
        if (numRedirects > MAX_REDIRECTS)
        {
            log.error("Could not retrieve Creative Commons license, maximum number of redirects reached (" + numRedirects + ")");
            return null;
        }

        try
        {
            URL url = new URL(url_string);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            String mailAdmin = configurationService.getProperty("mail.admin");
            String userAgent = "DSpace/" + Util.getSourceVersion() + "(+" + configurationService.getProperty("dspace.url");
            if (StringUtils.isNotBlank(mailAdmin)) {
                userAgent = userAgent + "; " + mailAdmin;
            }
            userAgent += ")";
            connection.addRequestProperty("User-Agent", userAgent);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setInstanceFollowRedirects(false); // we're handling redirects ourselves

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP)
            {
                // recursively follow redirects
                String locationHeader = connection.getHeaderField("Location");
                log.debug("Following redirect to " + locationHeader);
                InputStream inputStream = connection.getInputStream();
                if (inputStream != null)
                {
                    inputStream.close();
                }
                return fetchURL(locationHeader, numRedirects + 1);
            }
            else if (code == HttpURLConnection.HTTP_OK)
            {
                InputStream inputStream = connection.getInputStream();
                if (inputStream != null)
                {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sb = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line);
                    }
                    inputStream.close();
                    return sb.toString();
                }
            }
            else
            {
                log.error("Could not retrieve Creative Commons license from URL " + url.toString()
                        + "; response was " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }
        }
        catch (Exception exc)
        {
            log.error("Caught exception while trying to obtain Creative Commons license: " + exc.getMessage(), exc);
        }
        log.warn("Could not retrieve license");
        return null;
    }
    /**
     * Returns a metadata field handle for given field Id
     */
    @Override
    public LicenseMetadataValue getCCField(String fieldId)
    {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        return new LicenseMetadataValue(configurationService.getProperty("cc.license." + fieldId));
    }
    
}
