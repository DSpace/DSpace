/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.submit.accesscondition;

import java.util.Map;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class UploadConfigurationService {

	private Map<String, UploadConfiguration> map;

	public Map<String, UploadConfiguration> getMap() {
		return map;
	}

	public void setMap(Map<String, UploadConfiguration> map) {
		this.map = map;
	}
	
	
}
