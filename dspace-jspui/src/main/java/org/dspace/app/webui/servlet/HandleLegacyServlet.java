package org.dspace.app.webui.servlet;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.uri.IdentifierFactory;
import org.dspace.uri.ResolvableIdentifier;
import org.dspace.app.webui.util.JSPManager;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class HandleLegacyServlet extends DSpaceServlet
{
        /** log4j category */
    private static Logger log = Logger.getLogger(HandleLegacyServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException, AuthorizeException
    {
        this.doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException, AuthorizeException
    {
        String handle = null;
        String extraPathInfo = null;
        DSpaceObject dso = null;

        // Original path info, of the form "1721.x/1234"
        // or "1721.x/1234/extra/stuff"
        String path = request.getPathInfo();

        if (path != null)
        {
            // substring(1) is to remove initial '/'
            path = path.substring(1);

            try
            {
                // Extract the Handle
                int firstSlash = path.indexOf('/');
                int secondSlash = path.indexOf('/', firstSlash + 1);

                if (secondSlash != -1)
                {
                    // We have extra path info
                    handle = path.substring(0, secondSlash);
                    extraPathInfo = path.substring(secondSlash);
                }
                else
                {
                    // The path is just the Handle
                    handle = path;
                }
            }
            catch (NumberFormatException nfe)
            {
                // Leave handle as null
            }
        }

        // now parse the handle in its canonical form to locate the item
        // NOTE: my god how good is the API?  It's great, that's how good it is
        handle = "hdl:" + handle;
        ResolvableIdentifier ri = IdentifierFactory.resolve(context, handle);
        dso = ri.getObject(context);

        // if there is no object, display the invalid id error
        if (dso == null)
        {
            log.info(LogManager.getHeader(context, "invalid_id", "path=" + path));
            JSPManager.showInvalidIDError(request, response, path, -1);
        }
        else
        {
            String urlForm = ri.getURLForm();
            int index = path.indexOf(urlForm);
            int startFrom = index + urlForm.length();
            if (startFrom < path.length())
            {
                extraPathInfo = path.substring(startFrom);
            }

            // we've got a standard content delivery servlet to deal with this, to allow for alternative URI
            // handling mechanisms.  Not the best decoupling, but it'll do for the moment to allow the handle
            // system to offer a legacy url interpretation
            DSpaceObjectServlet dos = new DSpaceObjectServlet();
            dos.processDSpaceObject(context, request, response, dso, extraPathInfo);
        }
    }
}
