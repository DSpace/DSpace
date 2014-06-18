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

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.jdom.Element;


/**
 * Give read-only access to the contents of an EPerson object, through PROPFIND.
 * This resource is provided mainly so other resources (e.g. Item, WorkflowItem)
 * can refer to e-people as resources.
 * <p>
 * 
 * @author Larry Stone
 * @see DAVEPerson
 */
class DAVEPersonEPerson extends DAVResource
{
    /** The eperson. */
    private EPerson eperson = null;

    /**
     * Instantiates a new DAVE person E person.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param ep the ep
     */
    protected DAVEPersonEPerson(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[], EPerson ep)
    {
        super(context, request, response, pathElt);
        this.type = TYPE_OTHER;
        this.eperson = ep;
    }

    /** The Constant emailProperty. */
    private static final Element emailProperty = new Element("email",
            DAV.NS_DSPACE);

    /** The Constant first_nameProperty. */
    private static final Element first_nameProperty = new Element("first_name",
            DAV.NS_DSPACE);

    /** The Constant last_nameProperty. */
    private static final Element last_nameProperty = new Element("last_name",
            DAV.NS_DSPACE);

    /** The Constant handleProperty. */
    private static final Element handleProperty = new Element("handle",
            DAV.NS_DSPACE);

    /** The Constant require_certificateProperty. */
    private static final Element require_certificateProperty = new Element(
            "require_certificate", DAV.NS_DSPACE);

    /** The Constant self_registeredProperty. */
    private static final Element self_registeredProperty = new Element(
            "self_registered", DAV.NS_DSPACE);

    /** The Constant can_loginProperty. */
    private static final Element can_loginProperty = new Element("can_login",
            DAV.NS_DSPACE);

    /** The all props. */
    private static List<Element> allProps = new ArrayList<Element>();
    static
    {
        allProps.add(displaynameProperty);
        allProps.add(typeProperty);
        allProps.add(resourcetypeProperty);
        allProps.add(current_user_privilege_setProperty);
        allProps.add(emailProperty);
        allProps.add(first_nameProperty);
        allProps.add(last_nameProperty);
        allProps.add(handleProperty);
        allProps.add(require_certificateProperty);
        allProps.add(self_registeredProperty);
        allProps.add(can_loginProperty);
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
        return new Element("eperson", DAV.NS_DSPACE);
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
        return "ep_db_" + String.valueOf(dbid);
    }

    // format the final path element for one of these
    /**
     * Gets the path elt.
     * 
     * @param ep the ep
     * 
     * @return the path elt
     */
    protected static String getPathElt(EPerson ep)
    {
        return getPathElt(ep.getID());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#children()
     */
    @Override
    protected DAVResource[] children() throws SQLException
    {
        return new DAVResource[0];
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
     * @throws AuthorizeException the authorize exception
     */
    protected static DAVResource matchResourceURI(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[]) throws DAVStatusException, SQLException,
            AuthorizeException
    {
        try
        {
            // Match "/eperson/ep_db_<id>" URI
            // -or- "/eperson/<email-addr>"
            // -or- "/eperson/current" - magic path to _current_ user (if any)
            if (pathElt[0].equals("eperson") && pathElt.length > 1)
            {
                EPerson ep = null;
                if (pathElt.length > 2)
                {
                    throw new DAVStatusException(
                            HttpServletResponse.SC_NOT_FOUND,
                            "Invalid eperson resource path.");
                }
                if (pathElt[1].startsWith("ep_db_"))
                {
                    int id = Integer.parseInt(pathElt[1].substring(6));
                    ep = EPerson.find(context, id);
                }
                else if (pathElt[1].equalsIgnoreCase("current"))
                {
                    ep = context.getCurrentUser();
                }
                else
                {
                    ep = EPerson.findByEmail(context, pathElt[1]);
                }

                if (ep == null)
                {
                    throw new DAVStatusException(
                            HttpServletResponse.SC_NOT_FOUND,
                            "EPerson not found: " + pathElt[1]);
                }
                return new DAVEPersonEPerson(context, request, response,
                        pathElt, ep);
            }
            return null;
        }
        catch (NumberFormatException ne)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Error parsing number in request URI.", ne);
        }
    }

    // Authorization test - must be either admin or same as this user
    /**
     * Admin or self.
     * 
     * @param context the context
     * 
     * @return true, if successful
     * 
     * @throws SQLException the SQL exception
     */
    private boolean adminOrSelf(Context context) throws SQLException
    {
        if (AuthorizeManager.isAdmin(context))
        {
            return true;
        }
        EPerson self = context.getCurrentUser();
        return self != null && this.eperson != null
                && self.getID() == this.eperson.getID();
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
            value = this.eperson.getFullName();
        }
        else if (elementsEqualIsh(property, emailProperty))
        {
            value = this.eperson.getEmail();
        }
        else if (elementsEqualIsh(property, first_nameProperty))
        {
            value = this.eperson.getFirstName();
        }
        else if (elementsEqualIsh(property, last_nameProperty))
        {
            value = this.eperson.getLastName();
        }
        else if (elementsEqualIsh(property, handleProperty))
        {
            value = canonicalizeHandle(this.eperson.getHandle());
            if (!adminOrSelf(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to read this property.");
            }
        }
        else if (elementsEqualIsh(property, require_certificateProperty))
        {
            value = String.valueOf(this.eperson.getRequireCertificate());
            if (!adminOrSelf(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to read this property.");
            }
        }
        else if (elementsEqualIsh(property, self_registeredProperty))
        {
            value = String.valueOf(this.eperson.getSelfRegistered());
            if (!adminOrSelf(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to read this property.");
            }
        }
        else if (elementsEqualIsh(property, can_loginProperty))
        {
            value = String.valueOf(this.eperson.canLogIn());
            if (!adminOrSelf(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to read this property.");
            }
        }
        else
        {
            return commonPropfindInternal(property, false);
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
        if (!adminOrSelf(this.context))
        {
            throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                    "No authorization to read this EPerson.");
        }

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
