/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.service;

/**
 * Encapsulate the deposit license.
 *
 * @author mhwood
 */
public interface LicenseService {

    /**
     * Writes license to a text file.
     *
     * @param licenseFile
     *            name for the file into which license will be written,
     *            relative to the current directory.
     * @param newLicense new license
     */
    public void writeLicenseFile(String licenseFile,
            String newLicense);

    /**
     * Get the License
     *
     * @param
     *         licenseFile   file name
     *
     *  @return
     *         license text
     *
     */
    public String getLicenseText(String licenseFile);

    /**
     * Get the site-wide default license that submitters need to grant
     *
     * @return the default license
     */
    public String getDefaultSubmissionLicense();
}
