/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * Servlet to export metadata as CSV (comma separated values)
 *
 * @author Stuart Lewis
 */
public class MetadataExportServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(MetadataExportServlet.class);

    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();

    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();

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
    @Override
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
            DSpaceObject thing = handleService.resolveToObject(context, handle);
            if (thing != null)
            {
                if (thing.getType() == Constants.ITEM)
                {
                    List<Item> item = new ArrayList<>();
                    item.add((Item) thing);
                    exporter = new MetadataExport(context, item.iterator(), false);
                }
                else if (thing.getType() == Constants.COLLECTION)
                {
                    Collection collection = (Collection)thing;
					Iterator<Item> toExport = itemService.findAllByCollection(context, collection);
                    exporter = new MetadataExport(context, toExport, false);
                }
                else if (thing.getType() == Constants.COMMUNITY)
                {
                    exporter = new MetadataExport(context, (Community)thing, false);
                }

                if (exporter != null)
                {
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
        }

        // Something has gone wrong
        JSPManager.showIntegrityError(request, response);
    }
}
