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
import java.util.ListIterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.jdom.Element;


/**
 * The "workflow_*" resources are collections (in the DAV sense) of all the
 * current user's <code>WorkflowItem</code>s. Each one is nothing more than a
 * read-only collection to list the items.
 * <p>
 * The <code>workflow_own</code> resource lists items <em>owned</em> by the
 * current user, while the <code>workflow_pool</code> lists items in the
 * workflow pool which may be acquired by the current user.
 * <p>
 * Its children are all the relevant <code>WorkflowItem</code>s. These
 * resources cannot be altered.
 * <p>
 * 
 * @author Larry Stone
 * @see DAVWorkflowItem
 */
class DAVWorkflow extends DAVResource
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVWorkflow.class);

    /** The all props. */
    private static List<Element> allProps = new ArrayList<Element>(commonProps);

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return new Element("workflow", DAV.NS_DSPACE);
    }

    /**
     * Instantiates a new DAV workflow.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     */
    protected DAVWorkflow(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[])
    {
        super(context, request, response, pathElt);
        this.type = TYPE_OTHER;
    }

    /**
     * Gets the path.
     * 
     * @param wfi the wfi
     * 
     * @return URI path to this object.
     */
    protected static String getPath(WorkflowItem wfi)
    {
        return "workflow_pool/" + DAVWorkflowItem.getPathElt(wfi);
    }

    /**
     * Match the URIs this subclass understands and return the corresponding
     * resource.
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
        // The "/workflow" URI:
        if (pathElt.length > 0 && pathElt[0].startsWith("workflow_"))
        {
            if (pathElt.length > 1)
            {
                return DAVWorkflowItem.matchResourceURI(context, request,
                        response, pathElt);
            }
            else if (pathElt[0].equals("workflow_own")
                    || pathElt[0].equals("workflow_pool"))
            {
                return new DAVWorkflow(context, request, response, pathElt);
            }
            else
            {
                throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                        "Unrecognized URI path element: " + pathElt[0]);
            }
        }
        return null;
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
     * @see org.dspace.app.dav.DAVResource#children()
     */
    @Override
    protected DAVResource[] children() throws SQLException
    {

        EPerson ep = this.context.getCurrentUser();
        if (ep != null)
        {
            List<WorkflowItem> wi = null;
            if (this.pathElt[0].equals("workflow_own"))
            {
                wi = WorkflowManager.getOwnedTasks(this.context, ep);
            }
            else if (this.pathElt[0].equals("workflow_pool"))
            {
                wi = WorkflowManager.getPooledTasks(this.context, ep);
            }
            if (wi != null)
            {
                log.debug("children(): Got " + String.valueOf(wi.size())
                        + " Workflow Items.");
                DAVResource result[] = new DAVResource[wi.size()];
                ListIterator wii = wi.listIterator();
                int i = 0;
                while (wii.hasNext())
                {
                    WorkflowItem wfi = (WorkflowItem) wii.next();
                    result[i++] = new DAVWorkflowItem(this.context, this.request,
                            this.response, makeChildPath(DAVWorkflowItem
                                    .getPathElt(wfi.getID())), wfi);
                }
                return result;
            }
        }
        return new DAVResource[0];
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#propfindInternal(org.jdom.Element)
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        String value = null;

        // displayname - title or handle.
        if (elementsEqualIsh(property, displaynameProperty))
        {
            value = this.pathElt[0];
        }
        else
        {
            return commonPropfindInternal(property, true);
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
     * @see org.dspace.app.dav.DAVResource#proppatchInternal(int, org.jdom.Element)
     */
    @Override
    protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        throw new DAVStatusException(DAV.SC_CONFLICT, "The " + prop.getName()
                + " property cannot be changed.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#get()
     */
    @Override
    protected void get() throws SQLException, AuthorizeException,
            IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "GET method not implemented for workflow.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT method not implemented for workflow.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#copyInternal(org.dspace.app.dav.DAVResource, int, boolean, boolean)
     */
    @Override
    protected int copyInternal(DAVResource destination, int depth,
            boolean overwrite, boolean keepProperties)
            throws DAVStatusException, SQLException, AuthorizeException,
            IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "COPY method not implemented for workflow.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "DELETE method not implemented for Workflow.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for Workflow.");
    }
}
