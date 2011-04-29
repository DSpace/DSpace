/*
 * MetadataExportServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.*;
import org.dspace.content.DSpaceObject;
import org.dspace.content.ItemIterator;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.handle.HandleManager;

/**
 * Servlet to export metadata as CSV (comma separated values)
 *
 * @author Stuart Lewis
 */
public class MetadataExportServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(MetadataExportServlet.class);

    /**
     * Respond to a post request
     *
     * @param context a DSpace Context object
     * @param request the HTTP request
     * @param response the HTTP response
     *
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Get the handle requested for the export
        String handle = request.getParameter("handle");
        MetadataExport exporter = null;
        if (handle != null)
        {
            log.info(LogManager.getHeader(context, "metadataexport", "exporting_handle:" + handle));
            DSpaceObject thing = HandleManager.resolveToObject(context, handle);
            if (thing != null)
            {
                if (thing.getType() == Constants.ITEM)
                {
                    ArrayList item = new ArrayList();
                    item.add(thing.getID());
                    exporter = new MetadataExport(context, new ItemIterator(context, item), false);
                }
                else if (thing.getType() == Constants.COLLECTION)
                {
                    Collection collection = (Collection)thing;
                    ItemIterator toExport = collection.getAllItems();
                    exporter = new MetadataExport(context, toExport, false);
                }
                else if (thing.getType() == Constants.COMMUNITY)
                {
                    exporter = new MetadataExport(context, (Community)thing, false);
                }

                // Perform the export
                DSpaceCSV csv = exporter.export();

                // Return the csv file
                response.setContentType("text/csv; charset=UTF-8");
                String filename = handle.replaceAll("/", "-") + ".csv";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                PrintWriter out = response.getWriter();
                out.write(csv.toString());
                out.flush();
                out.close();
                log.info(LogManager.getHeader(context, "metadataexport", "exported_file:" + filename));                
                return;
            }
        }

        // Something has gone wrong
        JSPManager.showIntegrityError(request, response);
    }
}