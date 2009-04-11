/**
 * Copyright (c) 2009, Aberystwyth University
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;

/**
 * EntryDocumentServlet
 * 
 * @author Glen Robson
 * @author Stuart Lewis
 */
public class AtomDocumentServlet extends DepositServlet {

	/** Logger */
	private static Logger log = Logger.getLogger(AtomDocumentServlet.class);

	/**
	 * Initialise the servlet.
	 * 
	 * @throws ServletException
	 */
	public void init() throws ServletException {
		super.init();
	}

	/**
	 * Process the get request.
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			// Create the atom document request object
			AtomDocumentRequest adr = new AtomDocumentRequest();
			
			// Are there any authentication details?
			String usernamePassword = getUsernamePassword(request);
			if ((usernamePassword != null) && (!usernamePassword.equals(""))) {
				int p = usernamePassword.indexOf(":");
				if (p != -1) {
					adr.setUsername(usernamePassword.substring(0, p));
					adr.setPassword(usernamePassword.substring(p + 1));
				}
			} else if (authenticateWithBasic()) {
				String s = "Basic realm=\"SWORD\"";
				response.setHeader("WWW-Authenticate", s);
				response.setStatus(401);
				return;
			}
			
			// Set the IP address
			adr.setIPAddress(request.getRemoteAddr());

			// Set the deposit location
			adr.setLocation(getUrl(request));
			
			// Generate the response
			AtomDocumentResponse dr = myRepository.doAtomDocument(adr);
	
			// Print out the Deposit Response
			response.setStatus(dr.getHttpResponse());
			response.setContentType("application/atom+xml; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.write(dr.marshall());
			out.flush();
		} catch (SWORDAuthenticationException sae) {
			// Ask for credentials again
			String s = "Basic realm=\"SWORD\"";
			response.setHeader("WWW-Authenticate", s);
			response.setStatus(401);
		} catch (SWORDException se) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (SWORDErrorException see) {
			// Get the details and send the right SWORD error document
			super.makeErrorDocument(see.getErrorURI(), 
		               			   see.getStatus(),
		               			   see.getDescription(),
		                           request,
		                           response);
		}	
	}
}
