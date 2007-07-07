/*
 * HandleTypeMatcher.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/08/08 21:00:07 $
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
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

/**
 * Test the current URL to see if it contains a reference to a DSpaceObject, if
 * it dose then the object type is compared against the given pattern. The
 * matcher succeeds only if the object type matches. Valid expressions may be
 * combined with a comma to produce a set of "OR" expressions.
 * 
 * Thus if you want to match all handles that are communities or collections
 * then use the pattern value of "community,collection".
 * 
 * @author Scott Phillips
 */

public class HandleTypeMatcher extends AbstractLogEnabled implements Matcher
{
    /** The community expression */
    public static final String COMMUNITY_EXPRESSION = "community";

    /** The collection expression */
    public static final String COLLECITON_EXPRESSION = "collection";

    /** The item expression */
    public static final String ITEM_EXPRESSION = "item";

    /**
     * Match the encoded DSpaceObject against a specified type.
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
        String[] expressions = pattern.split(",");
        for (String expression : expressions)
        {
            // Strip out any spaces.
            expression.replace(" ", "");

            if (!(COMMUNITY_EXPRESSION.equals(expression)
                    || COLLECITON_EXPRESSION.equals(expression) || ITEM_EXPRESSION
                    .equals(expression)))
            {
                getLogger().warn("Invalid test pattern, '" + pattern + "', encountered.");
                return null;
            }
        }

        DSpaceObject dso = null;
        try
        {
            // HandleUtil handles caching if needed.
            dso = HandleUtil.obtainHandle(objectModel);
        }
        catch (SQLException sqle)
        {
            throw new PatternException("Unable to obtain DSpace Object", sqle);
        }

        if (dso == null)
            return null;

        Map<String, String> result = new HashMap<String, String>();
        for (String expression : expressions)
        {
            if (ITEM_EXPRESSION.equals(expression)
                    && dso.getType() == Constants.ITEM)
            {
                result.put("type", ITEM_EXPRESSION);
                return result;
            }
            else if (COLLECITON_EXPRESSION.equals(expression)
                    && dso.getType() == Constants.COLLECTION)
            {
                result.put("type", COLLECITON_EXPRESSION);
                return result;
            }
            else if (COMMUNITY_EXPRESSION.equals(expression)
                    && dso.getType() == Constants.COMMUNITY)
            {
                result.put("type", COMMUNITY_EXPRESSION);
                return result;
            }
        }

        return null;

    }
}
