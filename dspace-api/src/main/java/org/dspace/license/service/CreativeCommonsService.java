/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.LicenseMetadataValue;
import org.jdom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Service interface class for the Creative commons licensing.
 * The implementation of this class is responsible for all business logic calls for the Creative commons licensing and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CreativeCommonsService {

    public static final String CC_BUNDLE_NAME = "CC-LICENSE";

    /**
     * Simple accessor for enabling of CC
     */
    public boolean isEnabled();

    /** setLicenseRDF
    *
    * CC Web Service method for setting the RDF bitstream
    *
    */
    public void setLicenseRDF(Context context, Item item, String licenseRdf) throws SQLException, IOException, AuthorizeException;


    /**
     * Used by DSpaceMetsIngester
     *
     * @param context
     * @param item
     * @param licenseStm
     * @param mimeType
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     *
     * * // PATCHED 12/01 FROM JIRA re: mimetypes for CCLicense and License RDF wjb
     */
    public void setLicense(Context context, Item item,
                                  InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException;

    public void removeLicense(Context context, Item item)
            throws SQLException, IOException, AuthorizeException;

    public boolean hasLicense(Context context, Item item)
            throws SQLException, IOException;

    public String getLicenseURL(Context context, Item item) throws SQLException,
            IOException, AuthorizeException;

    public String getLicenseRDF(Context context, Item item) throws SQLException,
            IOException, AuthorizeException;

    /**
     * Get Creative Commons license RDF, returning Bitstream object.
     * @return bitstream or null.
     */
    public Bitstream getLicenseRdfBitstream(Item item) throws SQLException,
            IOException, AuthorizeException;

    /**
     * Get Creative Commons license Text, returning Bitstream object.
     * 
     * @return bitstream or null.
	 * @deprecated to make uniform JSPUI and XMLUI approach the bitstream with the license in the textual format it is no longer stored (see https://jira.duraspace.org/browse/DS-2604)
     */
    public Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException;

    /**
     * Get a few license-specific properties. We expect these to be cached at
     * least per server run.
     */

    public LicenseMetadataValue getCCField(String fieldId);
    
	/**
	 * Apply same transformation on the document to retrieve only the most
	 * relevant part of the document passed as parameter. If no transformation
	 * is needed then take in consideration to empty the CreativeCommons.xml
	 * 
	 * @param license
	 *            - an element that could be contains as part of your content
	 *            the license rdf
	 * @return the document license in textual format after the transformation
	 */
    public String fetchLicenseRDF(Document license);
    
	/**
	 * Remove license information, delete also the bitstream
	 * 
	 * @param context
	 *            - DSpace Context
	 * @param uriField
	 *            - the metadata field for license uri
	 * @param nameField
	 *            - the metadata field for license name
	 * @param item
	 *            - the item
	 * @throws AuthorizeException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void removeLicense(Context context, LicenseMetadataValue uriField,
			LicenseMetadataValue nameField, Item item) throws AuthorizeException, IOException, SQLException;
}