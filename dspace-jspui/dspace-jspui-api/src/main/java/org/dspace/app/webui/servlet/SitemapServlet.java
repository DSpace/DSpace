/*
 * SitemapServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2006, Hewlett-Packard Company and Massachusetts
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
        File f = new File(ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "sitemaps", file);

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

        // Set the response MIME type
        response.setContentType(mimeType);

        // Response length
        response.setHeader("Content-Length", String.valueOf(f.length()));

        Utils.bufferedCopy(is, response.getOutputStream());
        is.close();
        response.getOutputStream().flush();
    }
}
