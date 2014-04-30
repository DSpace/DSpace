/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Servlet for retrieving sitemaps.
 * <P>
 * The servlet is configured via the "type" config parameter to serve either
 * sitemaps.org or basic HTML sitemaps.
 * <P>
 * The "map" parameter specifies the index of a sitemap to serve. If no "map"
 * parameter is specified, the sitemap index is served.
 * 
 * @author Stuart Lewis
 * @author Robert Tansley
 * @version $Revision$
 */
public class SitemapServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SitemapServlet.class);

    /** true if we are for serving sitemap.org sitemaps, false otherwise */
    private boolean forSitemapsOrg;

    public void init()
    {
        forSitemapsOrg = false;

        String initParam = getInitParameter("type");

        if (initParam != null && initParam.equalsIgnoreCase("sitemaps.org"))
        {
            forSitemapsOrg = true;
        }
        else if (initParam == null || !initParam.equalsIgnoreCase("html"))
        {
            log.warn("Invalid initialization parameter for servlet "
                    + getServletName() + ": assuming basic HTML");
        }
    }

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String param = request.getParameter("map");

        String ext = (forSitemapsOrg ? ".xml.gz" : ".html");
        String mimeType = (forSitemapsOrg ? "text/xml" : "text/html");
        String fileStem = (param == null ? "sitemap_index" : "sitemap" + param);

        sendFile(request, response, fileStem + ext, mimeType, forSitemapsOrg);
    }

    private void sendFile(HttpServletRequest request,
            HttpServletResponse response, String file, String mimeType,
            boolean compressed) throws ServletException, IOException
    {
        File f = new File(ConfigurationManager.getProperty("sitemap.dir"), file);

        if (!f.exists())
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JSPManager.showJSP(request, response, "/error/404.jsp");
            return;
        }

        long lastMod = f.lastModified();
        response.setDateHeader("Last-Modified", lastMod);

        // Check for if-modified-since header
        long modSince = request.getDateHeader("If-Modified-Since");

        if (modSince != -1 && lastMod < modSince)
        {
            // Sitemap file has not been modified since requested date,
            // hence bitstream has not; return 304
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        if (compressed)
        {
            response.setHeader("Content-Encoding", "gzip");
        }

        // Pipe the bits
        InputStream is = new FileInputStream(f);
        try
        {
            // Set the response MIME type
            response.setContentType(mimeType);

            // Response length
            response.setHeader("Content-Length", String.valueOf(f.length()));

            Utils.bufferedCopy(is, response.getOutputStream());
        }
        finally
        {
            is.close();
        }
        
        response.getOutputStream().flush();
    }
}
