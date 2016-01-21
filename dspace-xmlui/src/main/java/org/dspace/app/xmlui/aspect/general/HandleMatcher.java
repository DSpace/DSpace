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
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.commons.collections.CollectionUtils;
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
            {
                return null;
            }

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
            {
                return current;
            }

            if (dso.getType() == Constants.ITEM)
            {
                current = ((Item) current).getOwningCollection();
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                current = ((Collection) current).getCommunities().get(0);
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                List<Community> parentCommunities = ((Community) current).getParentCommunities();
                if(CollectionUtils.isNotEmpty(parentCommunities))
                {
                    current = parentCommunities.get(0);
                }else{
                    current = null;
                }
            }
        }

        // If the loop finished then we searched the entire parant-child chain
        // and did not find this handle, so the object was not found.

        return null;
    }
}
