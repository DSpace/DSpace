/*
 * HandleAuthorizedMatcher.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/08/08 20:59:05 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Test the current URL to see if the user has access to the described
 * resources. The privelege tested against uses the pattern attribute, the
 * possible values are listed in the DSpace Constant class.
 * 
 * @author Scott Phillips
 */

public class HandleAuthorizedMatcher extends AbstractLogEnabled implements Matcher
{
    
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
    	if (action > 0 || action >= Constants.actionText.length)
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
            
            boolean authorized = 
            	AuthorizeManager.authorizeActionBoolean(context, dso, action);

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
