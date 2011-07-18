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
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.jdom.Element;


/**
 * The "workspace" resource is a collection (in the DAV sense) of all the
 * current user's <code>WorkspaceItem</code>s. It is nothing more than a
 * read-only collection to list these.
 * <p>
 * Its children are all the relevant <code>WorkspaceItem</code>s. It cannot
 * be altered.
 * <p>
 * 
 * @author Larry Stone
 * @see DAVWorkspaceItem
 */
class DAVWorkspace extends DAVResource
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVWorkspace.class);

    /** The all props. */
    private static List<Element> allProps = new ArrayList<Element>(commonProps);

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return new Element("workspace", DAV.NS_DSPACE);
    }

    /**
     * Instantiates a new DAV workspace.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     */
    protected DAVWorkspace(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[])
    {
        super(context, request, response, pathElt);
        this.type = TYPE_OTHER;
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
        // The "/workspace" URI:
        if (pathElt.length > 0 && pathElt[0].equals("workspace"))
        {
            if (pathElt.length > 1)
            {
                return DAVWorkspaceItem.matchResourceURI(context, request,
                        response, pathElt);
            }
            else
            {
                return new DAVWorkspace(context, request, response, pathElt);
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
            WorkspaceItem wi[] = WorkspaceItem.findByEPerson(this.context, ep);
            log.debug("children(): Got " + String.valueOf(wi.length)
                    + " Workspace Items.");
            DAVResource result[] = new DAVResource[wi.length];
            for (int i = 0; i < wi.length; ++i)
            {
                result[i] = new DAVWorkspaceItem(this.context, this.request, this.response,
                        makeChildPath(DAVWorkspaceItem
                                .getPathElt(wi[i].getID())), wi[i]);
            }
            return result;
        }
        else
        {
            return new DAVResource[0];
        }
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
            value = "workspace";
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
                "GET method not implemented for workspace.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT method not implemented for workspace.");
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
                "COPY method not implemented for workspace.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "DELETE method not implemented for Workspace.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for Workspace.");
    }
}
