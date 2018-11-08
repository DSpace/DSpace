/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.integration.orcid;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * This class provides utilities methods to share information among processors
 * avoiding the need to replicate ORCID calls if possible
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class AORCIDWebHookCallbackProcessor implements ORCIDWebHookCallbackProcessor {
	
	/** Logger */
	protected static Logger log = Logger.getLogger(AORCIDWebHookCallbackProcessor.class);

	/**
	 * Get the ORCID Information (Record, Works, Bio, etc.) from the request if
	 * already retrieved by another processor.
	 * 
	 * @param clazz
	 *            the ORCID information jaxb class
	 * @param req
	 *            the callback request
	 * @return the ORCID information if available otherwise null
	 */
	public <T> T getORCIDInfo(Class<T> clazz, HttpServletRequest req) {
		return (T) req.getAttribute("orcid-information." + clazz.getCanonicalName());
	}

	/**
	 * Set the ORCID Information (Record, Works, Bio, etc.) in the request in a
	 * standard way to make it available to other processors
	 * 
	 * @param info
	 *            the ORCID information to set (jaxb class)
	 * @param req
	 *            the callback request
	 */
	public <T> void setORCIDInfo(T info, HttpServletRequest req) {
		req.setAttribute("orcid-information." + info.getClass().getCanonicalName(), req);
	}

}
