/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.webui.util.VersionUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Servlet for handling the operations in the version history page
 * 
 * @author Pascarelli Luigi Andrea
 * @version $Revision$
 */
public class VersionHistoryServlet extends DSpaceServlet
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(VersionHistoryServlet.class);

    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();

    private final transient VersionHistoryService versionHistoryService
             = VersionServiceFactory.getInstance().getVersionHistoryService();
    
    private final transient VersioningService versioningService
            = VersionServiceFactory.getInstance().getVersionService();

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        UUID itemID = UIUtil.getUUIDParameter(request, "itemID");
        String versionID = request.getParameter("versionID");

        Item item = itemService.find(context, itemID);

        if (item == null) {
        	throw new IllegalArgumentException("Item is null");
        }
        
        // using configurationService.getPropertyAsType instead of getBooleanProperty
        // to get an instance of java.lang.Boolean instead of the primary type boolean.
        // Doing this prevents to rely on Javas auto boxing and unboxing feature.
        Boolean show_submitter = new DSpace()
                .getConfigurationService()
                .getPropertyAsType("versioning.item.history.include.submitter",
                        Boolean.FALSE);

        if(!authorizeService.isAdmin(context,
                item.getOwningCollection()))
        {
            // Check if only administrators can view the item history
            if (DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyAsType(
                    "versioning.item.history.view.admin", true))
            {
                throw new AuthorizeException();
            }
        } else {
            // if user is Admin override show_submitter
            show_submitter = Boolean.TRUE;
        }
        request.setAttribute("showSubmitter", show_submitter);
        
        // manage if versionID is not came by request
        VersionHistory history = versionHistoryService.findByItem(context, item);
        if (versionID == null || versionID.isEmpty())
        {
            Version version = versionHistoryService.getVersion(context, history, item);
            if (version != null)
            {
                versionID = String.valueOf(version.getID());
            }
        }
        String submit = UIUtil.getSubmitButton(request, "submit");
        if (submit != null && submit.equals("submit_cancel"))
        {
            // Pressed the cancel button, redirect us to the item page
            response.sendRedirect(request.getContextPath() + "/handle/"
                    + item.getHandle());
            context.complete();
            return;
        }
        else if (submit != null && submit.equals("submit_delete"))
        {
            String[] versionIDs = request.getParameterValues("remove");
            Item latestVersion = doDeleteVersions(request, itemID, versionIDs);
            
			if (latestVersion != null)
            {
                response.sendRedirect(request.getContextPath()
                        + "/tools/history?delete=true&itemID="+latestVersion.getID().toString());
            }
            else
            {
                // We have removed everything, redirect us to the home page !
                response.sendRedirect(request.getContextPath());
            }
            context.complete();
            return;

        }
        else if (submit != null && submit.equals("submit_restore"))
        {
            doRestoreVersion(request, itemID, versionID);
        }
        else if (submit != null && submit.equals("submit_update"))
        {
            doUpdateVersion(request, itemID, versionID);
            response.sendRedirect(request.getContextPath()
                    + "/tools/history?itemID=" + itemID + "&versionID="
                    + versionID);
            context.complete();
            return;
        }

        request.setAttribute("item", item);
        request.setAttribute("itemID", itemID);
        request.setAttribute("versionID", versionID);
        request.setAttribute("allVersions", versioningService.getVersionsByHistory(context, history));
        JSPManager.showJSP(request, response, "/tools/version-history.jsp");
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // If this is not overridden, we invoke the raw HttpServlet "doGet" to
        // indicate that POST is not supported by this servlet.
        doDSGet(UIUtil.obtainContext(request), request, response);
    }

    /**
     * Delete the given version(s)
     * 
     * @throws IOException
     * @throws AuthorizeException
     * @throws SQLException
     */
    private Item doDeleteVersions(HttpServletRequest request,
            UUID itemID, String... versionIDs) throws SQLException,
            AuthorizeException, IOException
    {

        return VersionUtil.processDeleteVersions(UIUtil.obtainContext(request),
                itemID, versionIDs);

    }

    /**
     * Restore the given version
     * 
     * @throws IOException
     * @throws AuthorizeException
     * @throws SQLException
     * @throws NumberFormatException
     */
    private UUID doRestoreVersion(HttpServletRequest request,
            UUID itemID, String versionID) throws NumberFormatException,
            SQLException, AuthorizeException, IOException
    {

        String summary = request.getParameter("summary");
        VersionUtil.processRestoreVersion(UIUtil.obtainContext(request),
                Integer.parseInt(versionID), summary);
        return itemID;
    }

    /**
     * Update the summary of the given version
     * 
     * @throws IOException
     * @throws AuthorizeException
     * @throws SQLException
     */
    private UUID doUpdateVersion(HttpServletRequest request, UUID itemID,
            String versionID) throws SQLException, AuthorizeException,
            IOException
    {

        String summary = request.getParameter("summary");
        VersionUtil.processUpdateVersion(UIUtil.obtainContext(request), itemID,
                summary);
        return itemID;

    }

}
