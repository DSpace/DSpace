/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;


/**
 * @author wbossons
 *
 */
public class CCLicense {

	private String licenseName;
	private String licenseId;
	private int order = 0;

	public CCLicense() {
		super();
	}

	public CCLicense(String licenseId, String licenseName, int order) {
		super();
		this.licenseId 		= licenseId;
		this.licenseName = licenseName;
		this.order 			= order;
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

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}


}
