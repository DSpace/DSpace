/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Constants;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;

import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class StatisticsAuthorizedMatcher extends AbstractLogEnabled implements Matcher{

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public Map match(String pattern, Map objectModel, Parameters parameters) throws PatternException {
        String[] statisticsDisplayTypes = parameters.getParameter("type", "").split(",");

        // Are we checking for *NOT* the action or the action.
        boolean not = false;
        int action = Constants.READ; // the action to check

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

            //We have always got rights to view stats on the home page (admin rights will be checked later)
            boolean authorized = dso == null || authorizeService.authorizeActionBoolean(context, dso, action, false);
            //Check if (one of our) display type is admin only
            //If one of the given ones isn't admin only, no need to check !
            boolean  adminCheckNeeded = true;
            for (String statisticsDisplayType : statisticsDisplayTypes) {
                //Only usage statics are available on an item level
                if(!"usage".equals(statisticsDisplayType) && dso != null && dso.getType() == Constants.ITEM){
                    continue;
                }
                //If one isn't admin enabled no need to check for admin
                if(!DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("usage-statistics.authorization.admin." + statisticsDisplayType, true)){
                    adminCheckNeeded = false;
                }
            }

            //If we are authorized check for any other authorization actions present
            if(authorized && adminCheckNeeded)
            {
                //If we have no user, we cannot be admin
                if(context.getCurrentUser() == null)
                {
                    authorized = false;
                }

                if(authorized){
                    //Check for admin
                    authorized = authorizeService.isAdmin(context);
                    if(!authorized)
                    {
                        //Check if we have authorization for the owning colls, comms, ...
                        authorized = authorizeService.isAdmin(context, dso);
                    }
                }
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
}
