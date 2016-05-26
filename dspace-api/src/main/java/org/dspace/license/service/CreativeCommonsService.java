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
     * This is a bit of the "do-the-right-thing" method for CC stuff in an item
     */
    public void setLicense(Context context, Item item,
            String cc_license_url) throws SQLException, IOException,
            AuthorizeException;

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

    public String getLicenseText(Context context, Item item) throws SQLException,
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
     * @return bitstream or null.
     */
    public Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException;

    public String fetchLicenseRdf(String ccResult);

    /**
    *
    *  The next two methods are old CC.
    * Remains until prev. usages are eliminated.
    * @Deprecated
    *
    */
    /**
     * Get a few license-specific properties. We expect these to be cached at
     * least per server run.
     */
    public String fetchLicenseText(String license_url);

    public String fetchLicenseRDF(String license_url);

    public LicenseMetadataValue getCCField(String fieldId);
}