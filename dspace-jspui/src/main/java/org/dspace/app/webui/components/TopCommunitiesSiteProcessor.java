/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

/**
 * This class add top communities object to the request attributes to use in
 * the site home page implementing the SiteHomeProcessor.
 * 
 * @author Andrea Bollini
 * 
 */
public class TopCommunitiesSiteProcessor implements SiteHomeProcessor
{

    /**
     * blank constructor - does nothing.
     * 
     */
    public TopCommunitiesSiteProcessor()
    {

    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response) throws PluginException,
            AuthorizeException
    {
        // Get the top communities to shows in the community list
        Community[] communities;
        try
        {
            communities = Community.findAllTop(context);
            List<Community> topCom = new ArrayList<Community>();
            String showCrisComm = ConfigurationManager.getProperty("community-list.topcommunity.show");

            if(AuthorizeManager.isAdmin(context) || 
                    StringUtils.equalsIgnoreCase(showCrisComm, "all") ||
                    ( context.getCurrentUser() != null && StringUtils.equalsIgnoreCase(showCrisComm, "user") ) ){
                for (int com = 0; com < communities.length; com++)
                {
                    topCom.add(communities[com]);
                }
            }else{                
                for (int com = 0; com < communities.length; com++)
                {
                    Group[] groups = AuthorizeManager.getAuthorizedGroups(context, communities[com], Constants.READ);
                    for(Group group : groups){
                        if(group.getID()==0|| Group.isMember(context, group.getID())){
                            topCom.add(communities[com]);
                            break;
                        }
                    }
                }
            }
            
            communities = new Community[topCom.size()];
            communities = topCom.toArray(communities);
        }
        catch (SQLException e)
        {
            throw new PluginException(e.getMessage(), e);
        }
        request.setAttribute("communities", communities);
    }

}
