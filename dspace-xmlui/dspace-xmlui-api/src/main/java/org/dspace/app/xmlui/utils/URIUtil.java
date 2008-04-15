package org.dspace.app.xmlui.utils;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.uri.ResolvableIdentifier;
import org.dspace.uri.IdentifierService;
import org.dspace.uri.IdentifierException;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;

import java.util.Map;
import java.sql.SQLException;

public class URIUtil
{
    /** The URL prefix of all object uris */
    protected static final String URI_PREFIX = "resource/";

    protected static final String DSPACE_OBJECT = "dspace.object";
    
    /**
     * Obtain the current DSpace handle for the specified request.
     *
     * @param objectModel
     *            The cocoon model.
     * @return A DSpace handle, or null if none found.
     */
    public static DSpaceObject resolve(Map objectModel)
            throws SQLException
    {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            DSpaceObject dso = (DSpaceObject) request.getAttribute(DSPACE_OBJECT);

            if (dso == null)
            {
                String uri = request.getSitemapURI();
                Context context = ContextUtil.obtainContext(objectModel);
                ResolvableIdentifier ri = IdentifierService.resolve(context, uri);
                if (ri == null)
                {
                    // FIXME: this is not right, but there's not much choice just now

                    // maybe we have a legacy url problem
                    dso = HandleUtil.obtainHandle(objectModel);
                    return dso;
                }
                dso = (DSpaceObject) IdentifierService.getResource(context, ri);

                request.setAttribute(DSPACE_OBJECT, dso);
            }

            return dso;
        }
        catch (IdentifierException e)
        {
            throw new RuntimeException(e);
        }
    }
}
