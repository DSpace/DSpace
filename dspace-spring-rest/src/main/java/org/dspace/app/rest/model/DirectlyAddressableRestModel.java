/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A directly addressable REST resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface DirectlyAddressableRestModel extends RestModel {
	public static final String CORE = "core";
	public static final String EPERSON = "eperson";
	public static final String DISCOVER = "discover";
	public static final String CONFIGURATION = "config";
	public static final String INTEGRATION = "integration";
	public static final String SUBMISSION = "submission";
	
	@JsonIgnore
	public String getCategory();
	
	@JsonIgnore
	public Class getController();
}
