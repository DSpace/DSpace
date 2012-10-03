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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.jdom.Element;


/**
 * The "eperson" resource is a collection (in the DAV sense) of all E-people in
 * DSpace.
 * <p>
 * <p>
 * Its children are all the relevant <code>EPerson</code> objects. These
 * resources cannot be altered.
 * <p>
 * 
 * @author Larry Stone
 * @see DAVEPersonEPerson
 */
class DAVEPerson extends DAVResource
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVEPerson.class);

    /** The all props. */
    private static List<Element> allProps = new ArrayList<Element>(commonProps);

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return new Element("eperson-collection", DAV.NS_DSPACE);
    }

    /**
     * Instantiates a new DAVE person.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     */
    protected DAVEPerson(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[])
    {
        super(context, request, response, pathElt);
        this.type = TYPE_OTHER;
    }

    /**
     * Gets the path.
     * 
     * @param ep the ep
     * 
     * @return the path
     */
    protected static String getPath(EPerson ep)
    {
        return "eperson/" + DAVEPersonEPerson.getPathElt(ep);
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
     * @throws AuthorizeException the authorize exception
     */
    protected static DAVResource matchResourceURI(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[]) throws DAVStatusException, SQLException,
            AuthorizeException
    {
        // The "/eperson" URI:
        if (pathElt.length > 0 && pathElt[0].equals("eperson"))
        {
            if (pathElt.length > 1)
            {
                return DAVEPersonEPerson.matchResourceURI(context, request,
                        response, pathElt);
            }
            else
            {
                return new DAVEPerson(context, request, response, pathElt);
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

    /**
     * If authenticated user is an Administrator, show all epersons, otherwise
     * just show current user (if there is one).
     * 
     * @return array of children, all the EPerson records on site.
     * 
     * @throws SQLException the SQL exception
     */
    @Override
    protected DAVResource[] children() throws SQLException
    {
        if (AuthorizeManager.isAdmin(this.context))
        {
            EPerson all[] = EPerson.findAll(this.context, EPerson.ID);
            DAVResource result[] = new DAVResource[all.length];
            log.debug("EPerson children(), got " + String.valueOf(all.length)
                    + " e-people.");
            for (int i = 0; i < all.length; ++i)
            {
                result[i] = new DAVEPersonEPerson(this.context, this.request, this.response,
                        makeChildPath(DAVEPersonEPerson.getPathElt(all[i]
                                .getID())), all[i]);
            }
            return result;
        }
        EPerson self = this.context.getCurrentUser();
        if (self == null)
        {
            return new DAVResource[0];
        }
        else
        {
            DAVResource result[] = new DAVResource[1];
            result[0] = new DAVEPersonEPerson(this.context, this.request, this.response,
                    makeChildPath(DAVEPersonEPerson.getPathElt(self.getID())),
                    self);
            return result;
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
                "GET method not implemented for eperson.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT method not implemented for eperson.");
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
                "COPY method not implemented for eperson.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "DELETE method not implemented for eperson.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for eperson.");
    }
}
