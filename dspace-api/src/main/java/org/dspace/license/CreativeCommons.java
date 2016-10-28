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
import java.util.ArrayList;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
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
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

public class CreativeCommons
{
    /** log4j category */
    private static Logger log = Logger.getLogger(CreativeCommons.class);

    /**
     * The Bundle Name
     */
    public static final String CC_BUNDLE_NAME = "CC-LICENSE";

    private static final String CC_BS_SOURCE = "org.dspace.license.CreativeCommons";

    /**
     * Some BitStream Names (BSN)
     * 
     * @deprecated use the metadata retrieved at {@link CreativeCommons#getCCField(String)} (see https://jira.duraspace.org/browse/DS-2604)
     */
    @Deprecated
    private static final String BSN_LICENSE_URL = "license_url";

    /**
     * 
     * @deprecated to make uniform JSPUI and XMLUI approach the bitstream with the license in the textual format it is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
     */
    @Deprecated
    private static final String BSN_LICENSE_TEXT = "license_text";

    private static final String BSN_LICENSE_RDF = "license_rdf";

    protected static final Templates templates;

    static
    {
        // if defined, set a proxy server for http requests to Creative
        // Commons site
        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");

        if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort))
        {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
        
        try
        {
            templates = TransformerFactory.newInstance().newTemplates(
                        new StreamSource(CreativeCommons.class
                                .getResourceAsStream("CreativeCommons.xsl")));
        }
        catch (TransformerConfigurationException e)
        {
            throw new RuntimeException(e.getMessage(),e);
        }
       
        
    }

    /**
     * Simple accessor for enabling of CC
     */
    public static boolean isEnabled()
    {
        return true;
    }

        // create the CC bundle if it doesn't exist
        // If it does, remove it and create a new one.
    private static Bundle getCcBundle(Item item)
        throws SQLException, AuthorizeException, IOException
    {
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

        if ((bundles.length > 0) && (bundles[0] != null))
        {
            item.removeBundle(bundles[0]);
        }
        return item.createBundle(CC_BUNDLE_NAME);
    }


     /** setLicenseRDF
     *
     * CC Web Service method for setting the RDF bitstream
     *
     */
    public static void setLicenseRDF(Context context, Item item, String licenseRdf)
    	throws SQLException, IOException,
            AuthorizeException
    {
        Bundle bundle = getCcBundle(item);
        // set the format
        BitstreamFormat bs_rdf_format = BitstreamFormat.findByShortDescription(context, "RDF XML");
        // set the RDF bitstream
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_RDF, bs_rdf_format, licenseRdf.getBytes());
    }
    
    /**
     * Used by DSpaceMetsIngester
     *
     * @param context
     * @param item
     * @param licenseStm
     * @param mimeType
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     *
     * * // PATCHED 12/01 FROM JIRA re: mimetypes for CCLicense and License RDF wjb
     */

    public static void setLicense(Context context, Item item,
                                  InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle bundle = getCcBundle(item);

     // set the format
        BitstreamFormat bs_format;
        if (mimeType.equalsIgnoreCase("text/xml"))
        {
        	bs_format = BitstreamFormat.findByShortDescription(context, "CC License");
        } else if (mimeType.equalsIgnoreCase("text/rdf")) {
            bs_format = BitstreamFormat.findByShortDescription(context, "RDF XML");
        } else {
        	bs_format = BitstreamFormat.findByShortDescription(context, "License");
        }

        Bitstream bs = bundle.createBitstream(licenseStm);
        bs.setSource(CC_BS_SOURCE);
        bs.setName((mimeType != null &&
                    (mimeType.equalsIgnoreCase("text/xml") ||
                     mimeType.equalsIgnoreCase("text/rdf"))) ?
                   BSN_LICENSE_RDF : BSN_LICENSE_TEXT);
        bs.setFormat(bs_format);
        bs.update();
    }

    
    public static void removeLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // remove CC license bundle if one exists
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

        if ((bundles.length > 0) && (bundles[0] != null))
        {
            item.removeBundle(bundles[0]);
        }
    }

    public static boolean hasLicense(Context context, Item item)
            throws SQLException, IOException
    {
        // try to find CC license bundle
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

        if (bundles.length == 0)
        {
            return false;
        }

        // verify it has correct contents
        try
        {
            if ((getLicenseURL(item) == null))
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

    public static String getLicenseRDF(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license RDF, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseRdfBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license Text, returning Bitstream object.
     * @return bitstream or null.
     * 
     * @deprecated to make uniform JSPUI and XMLUI approach the bitstream with the license in the textual format it is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
     */
    @Deprecated
    public static Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }
    
	/**
	 * Retrieve the license text
	 * 
	 * @param item - the item 
	 * @return the license in textual format
	 * @throws SQLException
	 * @throws IOException
	 * @throws AuthorizeException
	 * 
     * @deprecated to make uniform JSPUI and XMLUI approach the bitstream with the license in the textual format it is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
	 */
	public static String getLicenseText(Item item) throws SQLException, IOException, AuthorizeException {
		return getStringFromBitstream(item, BSN_LICENSE_TEXT);
	}
    
	public static String getLicenseURL(Item item) throws SQLException, IOException, AuthorizeException {
		String licenseUri = CreativeCommons.getCCField("uri").ccItemValue(item);
		if (StringUtils.isNotBlank(licenseUri)) {
			return licenseUri;
		}
		// JSPUI backward compatibility see https://jira.duraspace.org/browse/DS-2604
		return getStringFromBitstream(item, BSN_LICENSE_URL);
	}

    /**
     * Apply same transformation on the document to retrieve only the most relevant part of the document passed as parameter.
     * If no transformation is needed then take in consideration to empty the CreativeCommons.xml
     * 
     * @param license - an element that could be contains as part of your content the license rdf
     * @return the document license in textual format after the transformation
     */
    public static String fetchLicenseRDF(Document license)
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

    // The following two helper methods assume that the CC
    // bitstreams are short and easily expressed as byte arrays in RAM

    /**
     * This helper method takes some bytes and stores them as a bitstream for an
     * item, under the CC bundle, with the given bitstream name
     */
    private static void setBitstreamFromBytes(Item item, Bundle bundle,
            String bitstream_name, BitstreamFormat format, byte[] bytes)
            throws SQLException, IOException, AuthorizeException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Bitstream bs = bundle.createBitstream(bais);

        bs.setName(bitstream_name);
        bs.setSource(CC_BS_SOURCE);
        bs.setFormat(format);

        // commit everything
        bs.update();
    }

    /**
     * This helper method wraps a String around a byte array returned from the
     * bitstream method further down
     */
    private static String getStringFromBitstream(Item item,
            String bitstream_name) throws SQLException, IOException,
            AuthorizeException
    {
        byte[] bytes = getBytesFromBitstream(item, bitstream_name);

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
    private static Bitstream getBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle cc_bundle = null;

        // look for the CC bundle
        try
        {
            Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);

            if ((bundles != null) && (bundles.length > 0))
            {
                cc_bundle = bundles[0];
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

        return cc_bundle.getBitstreamByName(bitstream_name);
    }

    private static byte[] getBytesFromBitstream(Item item, String bitstream_name)
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
        Utils.copy(bs.retrieve(), baos);

        return baos.toByteArray();
    }

    /**
     * Returns a metadata field handle for given field Id
     */
    public static MdField getCCField(String fieldId)
    {
    	return new MdField(ConfigurationManager.getProperty("cc.license." + fieldId));
    }
    
    // Shibboleth for Creative Commons license data - i.e. characters that reliably indicate CC in a URI
    private static final String ccShib = "creativecommons";
    
    /**
     * Helper class for using CC-related Metadata fields
     * 
     */
    public static class MdField
    {
    	private String[] params = new String[4];
    	
    	public MdField(String fieldName)
    	{
    		if (fieldName != null && fieldName.length() > 0)
    		{
    			String[] fParams = fieldName.split("\\.");
    			for (int i = 0; i < fParams.length; i++)
    			{
    				params[i] = fParams[i];
    			}
    			params[3] = Item.ANY;
    		}
    	}
    	
    	/**
    	 * Returns first value that matches Creative Commons 'shibboleth',
    	 * or null if no matching values.
    	 * NB: this method will succeed only for metadata fields holding CC URIs
    	 * 
    	 * @param item - the item to read
    	 * @return value - the first CC-matched value, or null if no such value
    	 */
    	public String ccItemValue(Item item)
    	{
            Metadatum[] dcvalues = item.getMetadata(params[0], params[1], params[2], params[3]);
            for (Metadatum dcvalue : dcvalues)
            {
                if ((dcvalue.value).indexOf(ccShib) != -1) 
                {
                	// return first value that matches the shib
                	return dcvalue.value;
                }
            }
            return null;
    	}
    	
    	/**
    	 * Returns the value that matches the value mapped to the passed key if any.
    	 * NB: this only delivers a license name (if present in field) given a license URI
    	 * 
    	 * @param item - the item to read
    	 * @param key - the key for desired value
    	 * @return value - the value associated with key or null if no such value
    	 */
    	public String keyedItemValue(Item item, String key)
    		throws AuthorizeException, IOException, SQLException
    	{
    		 CCLookup ccLookup = new CCLookup();
             ccLookup.issue(key);
             String matchValue = ccLookup.getLicenseName();
             Metadatum[] dcvalues = item.getMetadata(params[0], params[1], params[2], params[3]);
             for (Metadatum dcvalue : dcvalues)
             {
            	 if (dcvalue.value.equals(matchValue))
            	 {
            		 return dcvalue.value;
            	 }
             }
    		return null;
    	}
    	
    	/**
    	 * Removes the passed value from the set of values for the field in passed item.
    	 * 
    	 * @param item - the item to update
    	 * @param value - the value to remove
    	 */
    	public void removeItemValue(Item item, String value) 
    			throws AuthorizeException, IOException, SQLException
    	{
    		if (value != null)
    		{
    			 Metadatum[] dcvalues  = item.getMetadata(params[0], params[1], params[2], params[3]);
                 ArrayList<String> arrayList = new ArrayList<String>();
                 for (Metadatum dcvalue : dcvalues)
                 {
                     if (! dcvalue.value.equals(value))
                     {
                         arrayList.add(dcvalue.value);
                     }
                  }
                  String[] values = (String[])arrayList.toArray(new String[arrayList.size()]);
                  item.clearMetadata(params[0], params[1], params[2], params[3]);
                  item.addMetadata(params[0], params[1], params[2], params[3], values);
    		}
    	}
    	
    	/**
    	 * Adds passed value to the set of values for the field in passed item.
    	 * 
    	 * @param item - the item to update
    	 * @param value - the value to add in this field
    	 */
    	public void addItemValue(Item item, String value)
    	{
    		item.addMetadata(params[0], params[1], params[2], params[3], value);
    	}
    }

	/**
	 * Remove license information, delete also the bitstream
	 * 
	 * @param context - DSpace Context
	 * @param uriField - the metadata field for license uri 
	 * @param nameField - the metadata field for license name
	 * @param item - the item
	 * @throws AuthorizeException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void removeLicense(Context context, MdField uriField,
			MdField nameField, Item item) throws AuthorizeException, IOException, SQLException {
		// only remove any previous licenses
		String licenseUri = uriField.ccItemValue(item);
		if (licenseUri != null) {
			uriField.removeItemValue(item, licenseUri);
			if (ConfigurationManager.getBooleanProperty("cc.submit.setname"))
		    {
		    	String licenseName = nameField.keyedItemValue(item, licenseUri);
		    	nameField.removeItemValue(item, licenseName);
		    }
		    if (ConfigurationManager.getBooleanProperty("cc.submit.addbitstream"))
		    {
		    	removeLicense(context, item);
		    }
		}
	}

}
