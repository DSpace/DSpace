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
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

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
 * @author Scott Phillips
 */

public class AuthenticatedSelector extends AbstractLogEnabled implements
        Selector
{

    private static Logger log = Logger.getLogger(AuthenticatedSelector.class);

    /** Test expressiots */
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
