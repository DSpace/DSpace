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

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.webui.util.VersionUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * 
 * 
 * @author Pascarelli Luigi Andrea
 * @version $Revision$
 */
public class VersionItemsServlet extends DSpaceServlet
{

    /** log4j category */
    private static Logger log = Logger.getLogger(VersionItemsServlet.class);


    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        Integer itemID = UIUtil.getIntParameter(request,"itemID");
                
        if (UIUtil.getSubmitButton(request,"submit")!=null){
            JSPManager.showJSP(request, response,
                    "/dspace-admin/version-summary.jsp");
            return;
        }
        
        String summary = request.getParameter("summary");
        if (UIUtil.getSubmitButton(request,"submit_version")!=null){                        
            Integer wsid = VersionUtil.processCreateNewVersion(context, itemID, summary);            
            response.sendRedirect(request.getContextPath()+"/submit?workspaceID=" + wsid);         
        }
        else if (UIUtil.getSubmitButton(request,"submit_update_version")!=null){
            VersionUtil.processUpdateVersion(context, itemID, summary);
        }
        Item item = Item.find(context,itemID);
        //Send us back to the item page if we cancel !
        response.sendRedirect(request.getContextPath() + "/handle/" + item.getHandle());
        context.complete();
        
    }

   
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // If this is not overridden, we invoke the raw HttpServlet "doGet" to
        // indicate that POST is not supported by this servlet.
        super.doGet(request, response);
    }

}
