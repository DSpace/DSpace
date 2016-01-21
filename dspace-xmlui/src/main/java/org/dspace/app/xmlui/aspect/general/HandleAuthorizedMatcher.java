/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Test the current URL to see if the user has access to the described
 * resources. The privelege tested against uses the pattern attribute, the
 * possible values are listed in the DSpace Constant class.
 * 
 * @author Scott Phillips
 * @author Tim Van den Langenbergh
 */

public class HandleAuthorizedMatcher extends AbstractLogEnabled implements Matcher
{

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    /**
     * Match method to see if the sitemap parameter exists. If it does have a
     * value the parameter added to the array list for later sitemap
     * substitution.
     * 
     * @param pattern
     *            name of sitemap parameter to find
     * @param objectModel
     *            environment passed through via cocoon
     * @return null or map containing value of sitemap parameter 'pattern'
     */
    public Map match(String pattern, Map objectModel, Parameters parameters)
            throws PatternException
    {
    	// Are we checking for *NOT* the action or the action.
    	boolean not = false;
    	int action = -1; // the action to check
    	
    	if (pattern.startsWith("!"))
    	{
    		not = true;
    		pattern = pattern.substring(1);
    	}
    	
    	for (int i=0; i< Constants.actionText.length; i++)
    	{
    		if (Constants.actionText[i].equals(pattern))
    		{
    			action = i;
    		}
    	}
    	
    	// Is it a valid action?
    	if (action < 0 || action >= Constants.actionText.length)
    	{
    		getLogger().warn("Invalid action: '"+pattern+"'");
    		return null;
    	}
    	
        try
        {
        	Context context = ContextUtil.obtainContext(objectModel);
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            
            if (dso == null)
            {
                return null;
            }
            
            boolean authorized = authorizeService.authorizeActionBoolean(context, dso, action);

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
