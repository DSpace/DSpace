/*
 * HandleMatcher.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2005/11/18 00:49:32 $
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
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;

/**
 * Test the current URL to see if it or any of it's parants match against the
 * given handle.
 * 
 * @author Scott Phillips
 */

public class HandleMatcher extends AbstractLogEnabled implements Matcher
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
        try
        {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso == null)
                return null;

            DSpaceObject parent = dspaceObjectWalk(dso, pattern);

            if (parent != null)
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

    /**
     * Private method to determine if the parent hirearchy matches the given
     * handle.
     * 
     * @param dso
     *            The child DSO object.
     * @param handle
     *            The Handle to test against.
     * @return The matched DSO object or null if none found.
     */
    private DSpaceObject dspaceObjectWalk(DSpaceObject dso, String handle)
            throws SQLException
    {

        DSpaceObject current = dso;

        while (current != null)
        {

            // Check if the current object has the handle we are looking for.
            if (current.getHandle().equals(handle))
                return current;

            if (dso.getType() == Constants.ITEM)
            {
                current = ((Item) current).getOwningCollection();
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                current = ((Collection) current).getCommunities()[0];
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                current = ((Community) current).getParentCommunity();
            }
        }

        // If the loop finished then we searched the entire parant-child chain
        // and did not find this handle, so the object was not found.

        return null;
    }
}
