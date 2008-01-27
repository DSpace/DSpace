/*
 * LicenseEditServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Servlet for editing the default license
 * 
 * @author Stuart Lewis
 */
public class LicenseEditServlet extends DSpaceServlet
{
	/** The logger */
    private static Logger log = Logger.getLogger(LicenseEditServlet.class);

    /**
     * Handle GET requests. This does nothing but forwards
     * the request on to the POST handler. 
     */
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	// Forward on to the post handler
        this.doDSPost(c, request, response);
    }

    /**
     * Handle the POST requests.
     */
    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        //Get submit button
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_cancel"))
        {
            // Show main admin index
            JSPManager.showJSP(request, response, "/dspace-admin/index.jsp");
        }
        else if (!button.equals("submit_save"))
        {
            // Get the existing text from the ConfigurationManager
            String license = ConfigurationManager.getDefaultSubmissionLicense();

            // Pass the existing license back to the JSP
            request.setAttribute("license", license);

            // Show edit page
            JSPManager.showJSP(request, response, "/dspace-admin/license-edit.jsp");
        }
        else
        {
            // Get text string from form
            String license = (String)request.getParameter("license");

            // Is the license empty?
            if (license.trim().equals(""))
            {
            	// Get the existing text from the ConfigurationManager
                license = ConfigurationManager.getDefaultSubmissionLicense();

                // Pass the existing license back to the JSP
                request.setAttribute("license", license);
                
                // Pass the 'empty' message back
	            request.setAttribute("empty", "true");

                // Show edit page
                JSPManager.showJSP(request, response, "/dspace-admin/license-edit.jsp");
            }
            else
            {
	            // Write the string out to file
	            ConfigurationManager.writeLicenseFile(license);
	
	            // Pass the existing license back to the JSP
	            request.setAttribute("license", license);
	            
	            // Pass the 'edited' message back
	            request.setAttribute("edited", "true");
	            
	            // Show edit page
	            JSPManager.showJSP(request, response, "/dspace-admin/license-edit.jsp");
            }
        }
    }
}
