/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;


import java.util.List;

/**
 * @author wbossons
 */
public class CCLicense {

    private String licenseName;
    private String licenseId;
    private List<CCLicenseField> ccLicenseFieldList;

    public CCLicense() {
        super();
    }

    public CCLicense(String licenseId, String licenseName, List<CCLicenseField> ccLicenseFieldList) {
        super();
        this.licenseId = licenseId;
        this.licenseName = licenseName;
        this.ccLicenseFieldList = ccLicenseFieldList;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    /**
     * Gets the list of CC License Fields
     * @return the list of CC License Fields
     */
    public List<CCLicenseField> getCcLicenseFieldList() {
        return ccLicenseFieldList;
    }

    /**
     * Sets the list of CC License Fields
     * @param ccLicenseFieldList
     */
    public void setCcLicenseFieldList(final List<CCLicenseField> ccLicenseFieldList) {
        this.ccLicenseFieldList = ccLicenseFieldList;
    }
}
