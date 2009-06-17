/**
 * Copyright (c) 2008, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */

package org.purl.sword.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;

/**
 * ServiceDocumentServlet
 * 
 * @author Stuart Lewis
 */
public class ServiceDocumentServlet extends HttpServlet {

	/** The repository */
	private SWORDServer myRepository;

	/** Authentication type. */
	private String authN;

	/** Maximum file upload size in kB **/
	private int maxUploadSize;

	/** Logger */
	private static Logger log = Logger.getLogger(ServiceDocumentServlet.class);

	/**
	 * Initialise the servlet.
	 * 
	 * @throws ServletException
	 */
	public void init() throws ServletException {
		// Instantiate the correct SWORD Server class
		String className = getServletContext().getInitParameter("sword-server-class");
		if (className == null) {
			log.fatal("Unable to read value of 'sword-server-class' from Servlet context");
		} else {
			try {
				myRepository = (SWORDServer) Class.forName(className)
						.newInstance();
				log.info("Using " + className + " as the SWORDServer");
			} catch (Exception e) {
				log.fatal("Unable to instantiate class from 'server-class': "
						+ className);
				throw new ServletException(
						"Unable to instantiate class from 'server-class': "
								+ className);
			}
		}

		// Set the authentication method
		authN = getServletContext().getInitParameter("authentication-method");
		if ((authN == null) || ("".equals(authN))) {
			authN = "None";
		}
		log.info("Authentication type set to: " + authN);
		
		String maxUploadSizeStr = getServletContext().getInitParameter("maxUploadSize");
		if ((maxUploadSizeStr == null) || 
		    (maxUploadSizeStr.equals("")) || 
		    (maxUploadSizeStr.equals("-1"))) {
			maxUploadSize = -1;
			log.warn("No maxUploadSize set, so setting max file upload size to unlimited.");
		} else {
			try {
				maxUploadSize = Integer.parseInt(maxUploadSizeStr);
				log.info("Setting max file upload size to " + maxUploadSize);
			} catch (NumberFormatException nfe) {
				maxUploadSize = -1;
				log.warn("maxUploadSize not a number, so setting max file upload size to unlimited.");
			}
		}
	}

	/**
	 * Process the get request.
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Create the ServiceDocumentRequest
		ServiceDocumentRequest sdr = new ServiceDocumentRequest();

        // Are there any authentication details?
		String usernamePassword = getUsernamePassword(request);
		if ((usernamePassword != null) && (!usernamePassword.equals(""))) {
			int p = usernamePassword.indexOf(":");
			if (p != -1) {
				sdr.setUsername(usernamePassword.substring(0, p));
				sdr.setPassword(usernamePassword.substring(p + 1));
			}
		} else if (authenticateWithBasic()) {
			String s = "Basic realm=\"SWORD\"";
			response.setHeader("WWW-Authenticate", s);
			response.setStatus(401);
			return;
		}

		// Set the x-on-behalf-of header
		sdr.setOnBehalfOf(request.getHeader(HttpHeaders.X_ON_BEHALF_OF
				.toString()));

		// Set the IP address
		sdr.setIPAddress(request.getRemoteAddr());

		// Set the deposit location
		sdr.setLocation(getUrl(request));

		// Get the ServiceDocument
		try {
			ServiceDocument sd = myRepository.doServiceDocument(sdr);
			if ((sd.getService().getMaxUploadSize() == -1) && (maxUploadSize != -1)) {
				sd.getService().setMaxUploadSize(maxUploadSize);
			}
		
			// Print out the Service Document
			response.setContentType("application/atomsvc+xml; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.write(sd.marshall());
			out.flush();
		} catch (SWORDAuthenticationException sae) {
			if (authN.equals("Basic")) {
				String s = "Basic realm=\"SWORD\"";
				response.setHeader("WWW-Authenticate", s);
				response.setStatus(401);
			}
		} catch (SWORDErrorException see) {
			// Return the relevant HTTP status code
			response.sendError(see.getStatus(), see.getDescription());
		} catch (SWORDException se) {
			se.printStackTrace();
			// Throw a HTTP 500
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage());
		}
	}

	/**
	 * Process the post request. This will return an unimplemented response.
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Send a '501 Not Implemented'
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	/**
	 * Utiliy method to return the username and password (separated by a colon
	 * ':')
	 * 
	 * @param request
	 * @return The username and password combination
	 */
	private String getUsernamePassword(HttpServletRequest request) {
		try {
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null) {
				StringTokenizer st = new StringTokenizer(authHeader);
				if (st.hasMoreTokens()) {
					String basic = st.nextToken();
					if (basic.equalsIgnoreCase("Basic")) {
						String credentials = st.nextToken();
						String userPass = new String(Base64
								.decodeBase64(credentials.getBytes()));
						return userPass;
					}
				}
			}
		} catch (Exception e) {
			log.debug(e.toString());
		}
		return null;
	}

	/**
	 * Utility method to decide if we are using HTTP Basic authentication
	 * 
	 * @return if HTTP Basic authentication is in use or not
	 */
	private boolean authenticateWithBasic() {
		if (authN.equalsIgnoreCase("Basic")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Utility method to construct the URL called for this Servlet
	 * 
	 * @param req The request object
	 * @return The URL
	 */
	private static String getUrl(HttpServletRequest req) {
		String reqUrl = req.getRequestURL().toString();
		String queryString = req.getQueryString();
        log.debug("Requested url is: " + reqUrl);
		if (queryString != null) {
			reqUrl += "?" + queryString;
		}
        log.debug("Requested url with Query String is: " + reqUrl);
		return reqUrl;
	}
}
