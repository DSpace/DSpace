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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.CommunityGroup;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Servlet for listing communities (and collections within them)
 * 
 * @author Robert Tansley
 * @version $Revision: 5845 $
 */
public class CommunityListServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(CommunityListServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "view_community_list", ""));

        // This will map group IDs to top level communities
        Map topcommMap = new HashMap();

        // This will map community IDs to arrays of collections
        Map<Integer, Collection[]> colMap = new HashMap<Integer, Collection[]>();

        // This will map communityIDs to arrays of sub-communities
        Map<Integer, Community[]> commMap = new HashMap<Integer, Community[]>();

        CommunityGroup[] groups = CommunityGroup.findAll(context);
        for (int k=0; k < groups.length; k++) {
            Integer groupID = new Integer(groups[k].getID());

            Community[] communities = groups[k].getCommunities();
            topcommMap.put(groupID, communities);

        for (int com = 0; com < communities.length; com++)
        {
            Integer comID = Integer.valueOf(communities[com].getID());

            // Find collections in community
            Collection[] colls = communities[com].getCollections();
            colMap.put(comID, colls);

            // Find subcommunties in community
            Community[] comms = communities[com].getSubcommunities();
            commMap.put(comID, comms);
                
        }
        }

        // can they admin communities?
        if (AuthorizeManager.isAdmin(context))
        {
            // set a variable to create an edit button
            request.setAttribute("admin_button", Boolean.TRUE);
        }

        request.setAttribute("groups", groups);
        request.setAttribute("communities.map", topcommMap);
        request.setAttribute("collections.map", colMap);
        request.setAttribute("subcommunities.map", commMap);
        JSPManager.showJSP(request, response, "/community-list.jsp");
    }
}
