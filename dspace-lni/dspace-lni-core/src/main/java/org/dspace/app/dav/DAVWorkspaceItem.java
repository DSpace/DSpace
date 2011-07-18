/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.dav;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowManager;
import org.jdom.Element;


/**
 * Reflect the contents of a WorkspaceItem object, which is mainly an
 * in-progress submission wrapper around an Item. Allow the client to read and
 * set the slots in the workspace item and explore its child Item.
 * <p>
 * WorkspaceItem resources are reached through the Workspace resource, typically
 * with a path of /workspace/wsi_db_{id}
 * <p>
 * 
 * @author Larry Stone
 * @see DAVInProgressSubmission
 * @see DAVWorkspace
 */
class DAVWorkspaceItem extends DAVInProgressSubmission
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVWorkspaceItem.class);

    /** The Constant stage_reachedProperty. */
    private static final Element stage_reachedProperty = new Element(
            "stage_reached", DAV.NS_DSPACE);

    /** The all props. */
    private static List<Element> allProps = new ArrayList<Element>(inProgressProps);
    static
    {
        allProps.add(stage_reachedProperty);
    }

    /**
     * Instantiates a new DAV workspace item.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param wi the wi
     */
    protected DAVWorkspaceItem(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[],
            InProgressSubmission wi)
    {
        super(context, request, response, pathElt, wi);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#getAllProperties()
     */
    @Override
    protected List<Element> getAllProperties()
    {
        return allProps;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return new Element("workspace-item", DAV.NS_DSPACE);
    }

    // format the final path element for one of these
    /**
     * Gets the path elt.
     * 
     * @param dbid the dbid
     * 
     * @return the path elt
     */
    protected static String getPathElt(int dbid)
    {
        return "wsi_db_" + String.valueOf(dbid);
    }

    /**
     * Match resource URI.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * 
     * @return the DAV resource
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     */
    protected static DAVResource matchResourceURI(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[]) throws DAVStatusException, SQLException
    {
        try
        {
            // Match "/workspace/wsi_db_<id>" URI
            if (pathElt.length >= 2 && pathElt[0].equals("workspace")
                    && pathElt[1].startsWith("wsi_db_"))
            {
                // match /workspace/wsi_db_<id>/item_db_<id> ...
                // should be an Item (or Bitstream) URI, child of this
                // WorkspaceItem.
                if (pathElt.length >= 3)
                {
                    DAVResource result = DAVItem.matchResourceURI(context,
                            request, response, pathElt);
                    if (result == null)
                    {
                        throw new DAVStatusException(
                                HttpServletResponse.SC_NOT_FOUND,
                                "Invalid resource path.");
                    }
                    else
                    {
                        return result;
                    }
                }

                // get this WSI
                int id = Integer.parseInt(pathElt[1].substring(7));
                InProgressSubmission ips = WorkspaceItem.find(context, id);
                if (ips == null)
                {
                    log.warn("invalid WorkspaceItem DB ID in DAV URI, " + "id="
                            + pathElt[1]);
                    throw new DAVStatusException(
                            HttpServletResponse.SC_NOT_FOUND, "Not found: "
                                    + pathElt[1] + " does not exist.");
                }
                else
                {
                    return new DAVWorkspaceItem(context, request, response,
                            pathElt, ips);
                }
            }
            return null;
        }
        catch (NumberFormatException ne)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Error parsing number in request URI.", ne);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVInProgressSubmission#propfindInternal(org.jdom.Element)
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        String value = null;

        // displayname - title or handle.
        if (elementsEqualIsh(property, displaynameProperty))
        {
            value = getPathElt(this.inProgressItem.getID());
        }
        else if (elementsEqualIsh(property, stage_reachedProperty))
        {
            value = String.valueOf(((WorkspaceItem) this.inProgressItem)
                    .getStageReached());
        }
        else
        {
            return super.propfindInternal(property);
        }

        // value was set up by "if" clause:
        if (value == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                    "Not found.");
        }
        Element p = new Element(property.getName(), property.getNamespace());
        p.setText(filterForXML(value));
        return p;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVInProgressSubmission#proppatchInternal(int, org.jdom.Element)
     */
    @Override
    protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        if (super.proppatchInternal(action, prop) == HttpServletResponse.SC_OK)
        {
            return HttpServletResponse.SC_OK;
        }
        else if (elementsEqualIsh(prop, stateProperty))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "The state property cannot be removed.");
            }
            String key = prop.getTextTrim();

            if (key.equalsIgnoreCase("start"))
            {
                WorkflowManager.start(this.context, (WorkspaceItem) this.inProgressItem);
            }
            else if (key.equalsIgnoreCase("start_without_notify"))
            {
                WorkflowManager.startWithoutNotify(this.context,
                        (WorkspaceItem) this.inProgressItem);
            }
        }

        // setStageReached - arbitrary number.
        else if (elementsEqualIsh(prop, stage_reachedProperty))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "The stage property cannot be removed.");
            }
            try
            {
                ((WorkspaceItem) this.inProgressItem).setStageReached(Integer
                        .parseInt(prop.getTextTrim()));
            }
            catch (NumberFormatException ne)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Error parsing number in property value.", ne);
            }
        }
        else
        {
            throw new DAVStatusException(DAV.SC_CONFLICT, "The "
                    + prop.getName() + " property cannot be changed.");
        }

        this.inProgressItem.update();
        return HttpServletResponse.SC_OK;
    }
}
