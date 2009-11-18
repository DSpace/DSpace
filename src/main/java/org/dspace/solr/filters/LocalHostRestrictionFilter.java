/**
 * $Id: $
 * $URL: $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.solr.filters;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LocalHostRestrictionFilter implements Filter {

	private static Log log = LogFactory
			.getLog(LocalHostRestrictionFilter.class);

	private boolean enabled = true;
	
	public LocalHostRestrictionFilter() {
		// TODO Auto-generated constructor stub
	}

	public void destroy() {
		// TODO Auto-generated method stub

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		if(enabled){
			InetAddress ia = InetAddress.getLocalHost();
			String localAddr = ia.getHostAddress();
			String remoteAddr = request.getRemoteAddr();

			if(!(localAddr.equals(remoteAddr) || remoteAddr.equals("127.0.0.1")))
			{
				((HttpServletResponse)response).sendError(403);
		                return;
			}
			
		}
		
		chain.doFilter(request, response);
	}

	/**
	 * 
	 */
	public void init(FilterConfig arg0) throws ServletException {
		String restrict = arg0.getServletContext().getInitParameter("LocalHostRestrictionFilter.localhost");
		if("false".equalsIgnoreCase(restrict))
		{
			enabled = false;
		}
		
	}

}
