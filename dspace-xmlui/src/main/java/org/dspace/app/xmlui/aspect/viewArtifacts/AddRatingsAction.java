/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.viewArtifacts;

import javax.servlet.http.HttpServletResponse;
import com.jonathanblood.content.RatingsManager;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.handle.HandleManager;
import org.apache.log4j.Logger;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.avalon.framework.parameters.Parameters;

import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Jonathan Blood
 */
public class AddRatingsAction extends AbstractAction
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(AddRatingsAction.class);

    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String string, Parameters parameters) throws Exception
    {
        try
        {
            final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            Request request = ObjectModelHelper.getRequest(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);

            int itemID = Integer.parseInt(request.getParameter("itemid"));
            int userID = Integer.parseInt(request.getParameter("userid"));
            int rating = Integer.parseInt(request.getParameter("rating"));

            Item item = Item.find(context, itemID);

            RatingsManager ratingManager = new RatingsManager();
            boolean hasRating = ratingManager.hasRating(context, userID, itemID);
            if (hasRating)
            {
                ratingManager.updateRating(context, userID, itemID, rating);
            }
            else
            {
                ratingManager.addRating(context, userID, itemID, rating);
            }

            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(request.getContextPath() +
                    "/handle/" + item.getHandle()));

        }
        catch (Exception e)
        {
            log.error("Add rating action failed", e);
        }

        return null;
    }
}