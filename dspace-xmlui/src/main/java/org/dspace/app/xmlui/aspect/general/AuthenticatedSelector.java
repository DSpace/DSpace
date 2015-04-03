/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * This simple selector operates on the authenticated DSpace user and selects
 * between two levels of access.
 * 
 * <map:selector name="AuthenticatedSelector" src="org.dspace.app.xmlui.AuthenticatedSelector"/>
 * 
 * 
 * 
 * <map:select type="AuthenticatedSelector"> 
 *   <map:when test="administrator">
 *     ...
 *   </map:when> 
 *   <map:when test="eperson"> 
 *     ... 
 *   </map:when> 
 *   <map:otherwise> 
 *     ...
 *   </map:otherwise> 
 * </map:select>
 * 
 * There are only two defined test expressions: "administrator" and "eperson".
 * Remember an administrator is also an eperson so if you need to check for
 * administrators distinct from epersons that select must come first.
 * 
 * based on class by Scott Phillips
 * modified for LINDAT/CLARIN
 */

public class AuthenticatedSelector extends AbstractLogEnabled implements
        Selector
{

    private static Logger log = Logger.getLogger(AuthenticatedSelector.class);

    /** Test expressions */
    public static final String AUTHORIZED = "authorized";

    public static final String EPERSON = "eperson";

    public static final String ADMINISTRATOR = "administrator";

    /**
     * Determine if the authenticated eperson matches the given expression.
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters)
    {
        try
        {
            Context context = ContextUtil.obtainContext(objectModel);

            EPerson eperson = context.getCurrentUser();
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            /** UFAL: first check the dtoken */
            Request request = ObjectModelHelper.getRequest(objectModel);
            String dtoken = request.getParameter("dtoken");
            if(dtoken!=null && !dtoken.isEmpty()) {
	            IFunctionalities manager = DSpaceApi.getFunctionalityManager();
	            manager.openSession();
	            boolean tokenVerified = true;
                if (dso instanceof Item) { // should be an item
                	Item item = (Item) dso;
        			Bundle[] originals = item.getBundles("ORIGINAL");
        			for (Bundle original : originals) {
        				for(Bitstream bitstream : original.getBitstreams()) { // all bitstream should have the same and valid token
        					tokenVerified = manager.verifyToken(bitstream.getID(), dtoken);
        					if(!tokenVerified) break;
        				}
        			}
                }
	            manager.closeSession();

	            if(tokenVerified) {
	            	return true;
	            }
            }
            /** UFAL: dtoken checking done */

            /** UFAL: second check authorized access */
            if (AUTHORIZED.equals(expression)) {
                if (dso instanceof Item) { // should be an item
                    Item item = (Item) dso;
                    Bundle[] originals = item.getBundles("ORIGINAL");
                    for (Bundle original : originals) {
                        for (Bitstream bitstream : original.getBitstreams()) {
                            try {
                                AuthorizeManager.authorizeAction(context, bitstream, Constants.READ);
                            }
                            catch (Exception e)
                            {
                                return false;
                            }
                        }
                    }

                    if (item != null && item.isWithdrawn() && !AuthorizeManager.isAdmin(context)) {
                        return false;
                    }

                    return true;
                }
            }
            /** UFAL: authorized access checking done */


            if (eperson == null)
            {
                // No one is authenticated.
                return false;
            }

            if (EPERSON.equals(expression))
            {
                // At least someone is authenticated.
                return true;
            }
            else if (ADMINISTRATOR.equals(expression))
            {
                // Is this eperson an administrator?
                return AuthorizeManager.isAdmin(context);
            }

            // Otherwise return false;
            return false;

        }
        catch (Exception e)
        {
            // Log it and returned no match.
            log.error("Error selecting based on authentication status: "
                    + e.getMessage());

            return false;
        }
    }

}
