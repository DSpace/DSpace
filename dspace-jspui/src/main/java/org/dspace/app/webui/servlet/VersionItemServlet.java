/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.webui.util.VersionUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Servlet to handling the versioning of the item
 * 
 * @author Pascarelli Luigi Andrea
 * @version $Revision$
 */
public class VersionItemServlet extends DSpaceServlet
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(VersionItemServlet.class);

    private final transient ItemService itemService =
            ContentServiceFactory.getInstance().getItemService();

    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        UUID itemID = UIUtil.getUUIDParameter(request,"itemID");
        Item item = itemService.find(context,itemID);
        String submit = UIUtil.getSubmitButton(request,"submit");
        if (submit!=null && submit.equals("submit")){
            request.setAttribute("itemID", itemID);
            JSPManager.showJSP(request, response,
                    "/tools/version-summary.jsp");
            return;
        }
        
        String summary = request.getParameter("summary");
        if (submit!=null && submit.equals("submit_version")){                        
            Integer wsid = VersionUtil.processCreateNewVersion(context, item.getID(), summary);            
            response.sendRedirect(request.getContextPath()+"/submit?resume=" + wsid);
            context.complete();
            return;
        }
        else if (submit!=null && submit.equals("submit_update_version")){
            String versionID = request.getParameter("versionID");
            request.setAttribute("itemID", itemID);
            request.setAttribute("versionID", versionID);
            JSPManager.showJSP(request, response,
                    "/tools/version-update-summary.jsp");
            return;
        }
        
        //Send us back to the item page if we cancel !
        response.sendRedirect(request.getContextPath() + "/handle/" + item.getHandle());
        context.complete();
        
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

}
