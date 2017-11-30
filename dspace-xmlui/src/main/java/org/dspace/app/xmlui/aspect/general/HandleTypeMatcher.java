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
        {
            return null;
        }

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
