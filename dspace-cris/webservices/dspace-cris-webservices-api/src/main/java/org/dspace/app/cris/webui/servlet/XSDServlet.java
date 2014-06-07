/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.webui.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;

public class XSDServlet extends HttpServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(XSDServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException
    {
       
        String path = req.getPathInfo();
        if (path != null && path.startsWith("/"))
        {
            path = path.substring(1);
        }
        else
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        InputStream is = null;
        try
        {            
            String dspacedir = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "webservices.xsd.path") + File.separatorChar + "xsd" + File.separatorChar;
            is = new FileInputStream(new File(dspacedir + path));
            response.setContentType("text/xml");           

            Utils.bufferedCopy(is, response.getOutputStream());
            is.close();
            response.getOutputStream().flush();
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }

    }
}
