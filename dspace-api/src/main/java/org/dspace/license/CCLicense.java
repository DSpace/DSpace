/**
 *
 */
package org.dspace.license; //package org.wendy.cc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

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
