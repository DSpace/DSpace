/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.integration.orcid;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.core.Context;

public interface ORCIDWebHookCallbackProcessor {

	/**
	 * Method to implement to process changes on the orcid registry for the
	 * identified local profile. Use the
	 * 
	 * @param context
	 *            the dspace context
	 * @param rp
	 *            the local profile linked to the ORCID record if any
	 * @param orcid
	 *            the orcid where changes occur. It can be also retrieved in the rp
	 *            but it is offered as extra param to make processor implementation
	 *            easier
	 * @param req
	 *            the HttpServletRequest of the webhook callback, useful to access
	 *            information made available by other processors
	 * @return true if the change has been processed, false will result in a failure
	 *         and a retry from ORCID
	 */
	public boolean processChange(Context context, ResearcherPage rp, String orcid, HttpServletRequest req);
}
