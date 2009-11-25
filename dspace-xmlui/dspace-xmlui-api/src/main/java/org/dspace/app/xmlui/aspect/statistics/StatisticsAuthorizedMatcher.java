/**
 * $Id: $
 * $URL: $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.statistics;

import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.authorize.AuthorizeManager;

import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 19-nov-2009
 * Time: 17:19:56
 */
public class StatisticsAuthorizedMatcher extends AbstractLogEnabled implements Matcher{


    public Map match(String pattern, Map objectModel, Parameters parameters) throws PatternException {
        // Are we checking for *NOT* the action or the action.
        boolean not = false;
        int action = -1; // the action to check

        if (pattern.startsWith("!"))
        {
            not = true;
            pattern = pattern.substring(1);
        }

        if(!pattern.equals("READ"))
        {
            getLogger().warn("Invalid action: '"+pattern+"'");
            return null;
        }

        try
        {
            Context context = ContextUtil.obtainContext(objectModel);
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            	return null;

            boolean authorized = AuthorizeManager.authorizeActionBoolean(context, dso, action, false);
            //If we are not authorized check for any other authorizations present
            if(!authorized && context.getCurrentUser() != null
                    && ConfigurationManager.getBooleanProperty("statistics.item.authorization.admin"))
            {
                //Check for admin
                authorized = AuthorizeManager.isAdmin(context);
                if(!authorized)
                    //Check if we have authorization for the owning colls, comms, ...
                    authorized = checkParentAuthorization(context, dso);
            }

            // XOR
            if (not ^ authorized)
            {
            	return new HashMap();
            }
            else
            {
                return null;
            }


        }
        catch (SQLException sqle)
        {
            throw new PatternException("Unable to obtain DSpace Context", sqle);
        }
    }

    public static boolean checkParentAuthorization(Context context, DSpaceObject dso) throws SQLException {
        if(dso instanceof Community)
        {
            Community comm = (Community) dso;
            if(AuthorizeManager.isAdmin(context, comm))
                return true;
            else if(comm.getParentCommunity() != null)
                return checkParentAuthorization(context, comm);
        }else
        if(dso instanceof Collection)
        {
            Collection coll = (Collection) dso;
            if(AuthorizeManager.isAdmin(context, coll))
                return true;
            else{
                //Check if any of our parent communities has authorization
                for (int i = 0; i < coll.getCommunities().length; i++) {
                    Community community = coll.getCommunities()[i];
                    boolean authorized = checkParentAuthorization(context, community);
                    if(authorized)
                        return true;
                }
            }
        }else
        if(dso instanceof Item){
            //Check if we have read rights for our owning collections
            for(Collection coll : ((Item) dso).getCollections()){
                boolean authorized = checkParentAuthorization(context, coll);
                if(authorized)
                    return true;
            }
        }
        return false;
    }
}
