/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;

/**
 * This simple selector looks for the If-Modified-Since header, and
 * returns true if the Item in the request has not been modified since that
 * date.  The expression is ignored since the test is inherent in the request.
 *
 * <p>Typical sitemap usage:
 *
 * <pre>
 * {@code
 *  <map:match type="HandleTypeMatcher" pattern="item">
 *    <map:select type="IfModifiedSinceSelector">
 *      <map:when test="true">
 *        <map:act type="NotModifiedAction"/>
 *        <map:serialize/>
 *      </map:when>
 *      <map:otherwise>
 *        <map:transform type="ItemViewer"/>
 *        <map:serialize type="xml"/>
 *      </map:otherwise>
 *    </map:select>
 *  </map:match>
 * }
 * </pre>
 *
 * @author Larry Stone
 */
public class IfModifiedSinceSelector implements Selector
{

    private static final Logger log = Logger.getLogger(IfModifiedSinceSelector.class);

    /**
     * Check for If-Modified-Since header on request,
     * and returns true if the Item should *not* be sent, i.e.
     * if the response status should be 304 (HttpServletResponse.SC_NOT_MODIFIED).
     *
     * @param expression is ignored
     * @param objectModel
     *            environment passed through via Cocoon.
     * @param parameters
     *            sitemap parameters.
     * @return null or map containing value of sitemap parameter 'pattern'
     */
    @Override
    public boolean select(String expression, Map objectModel,
            Parameters parameters)
    {
        try
        {
            Request request = ObjectModelHelper.getRequest(objectModel);
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                long modSince = request.getDateHeader("If-Modified-Since");
                if (modSince != -1 && item.getLastModified().getTime() < modSince)
                {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e)
        {
            log.error("Error selecting based on If-Modified-Since: "+e.toString());
            return false;
        }
    }
}
