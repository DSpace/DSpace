/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.LicenseService;

/**
 * Servlet for editing the default license
 *
 * @author Stuart Lewis
 */
public class LicenseEditServlet extends DSpaceServlet
{
	private final transient LicenseService licenseService
             = CoreServiceFactory.getInstance().getLicenseService();
	
    /**
     * Handle GET requests. This does nothing but forwards
     * the request on to the POST handler.
     */
    @Override
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
    @Override
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
            String license = licenseService.getLicenseText(I18nUtil.getDefaultLicense(c));

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
                license = licenseService.getLicenseText(I18nUtil.getDefaultLicense(c));

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
            	licenseService.writeLicenseFile(I18nUtil.getDefaultLicense(c), license);

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
