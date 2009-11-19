/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager.example;

import org.dspace.services.RequestService;
import org.dspace.services.model.RequestInterceptor;
import org.dspace.services.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision$
 */
public class RequestInterceptorExample implements RequestInterceptor {

	private static Logger log = LoggerFactory.getLogger(RequestInterceptorExample.class);
	
	/**
	 * Constructor which will inject the instantiated 
	 * Interceptor into a service handed to it.
	 * 
	 * @param service
	 */
	public RequestInterceptorExample(RequestService service)
	{
		service.registerRequestInterceptor(this);
	}
	
	public void onEnd(String requestId, Session session, boolean succeeded,
			Exception failure) {
		log.info("Intercepting End of Request: id=" + requestId + ", session=" + session.getId() + ", succeeded=" + succeeded);
	}

	public void onStart(String requestId, Session session) {
		log.info("Intercepting Start of Request: id=" + requestId + ", session=" + session.getId());
	}

	public int getOrder() {
		// TODO Auto-generated method stub
		return 0;
	}

}
