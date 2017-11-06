/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the base class for any Rest Repository. It provides utility method to
 * access the DSpaceContext
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class AbstractDSpaceRestRepository {

	/**
	 * log4j category
	 */
	private static final Logger log = Logger.getLogger(AbstractDSpaceRestRepository.class);

	@Autowired
	protected Utils utils;

	protected RequestService requestService = new DSpace().getRequestService();



	protected Context obtainContext() {
		Request currentRequest = requestService.getCurrentRequest();
		try {
			return ContextUtil.obtainContext(currentRequest.getServletRequest());
		} catch (SQLException e) {
			log.error("Unable to obtain context object", e);
			return null;
		}
	}
}
