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
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LicenseManager;
import org.dspace.core.NewsManager;
import org.jdom.Element;


/**
 * Model the DSpace Site as a resource. The URI is the top level "collection".
 * Only an Administrator can modify properties.
 */
class DAVSite extends DAVResource
{
    /** The Constant news_topProperty. */
    private static final Element news_topProperty = new Element("news_top",
            DAV.NS_DSPACE);

    /** The Constant news_sideProperty. */
    private static final Element news_sideProperty = new Element("news_side",
            DAV.NS_DSPACE);

    /** The Constant default_licenseProperty. */
    private static final Element default_licenseProperty = new Element(
            "default_license", DAV.NS_DSPACE);

    /** The all props. */
    private static List<Element> allProps = new ArrayList<Element>(commonProps);
    static
    {
        allProps.add(news_topProperty);
        allProps.add(news_sideProperty);
        allProps.add(default_licenseProperty);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return new Element("site", DAV.NS_DSPACE);
    }

    /**
     * Instantiates a new DAV site.
     *
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     */
    protected DAVSite(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[])
    {
        super(context, request, response, pathElt);
        this.type = TYPE_SITE;
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
        if (pathElt.length == 0 || pathElt[0].length() == 0)
        {
            return new DAVSite(context, request, response, new String[0]);
        }
        else
        {
            return null;
        }
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
        Community top[] = Community.findAllTop(this.context);
        DAVResource result[] = new DAVResource[top.length];

        for (int i = 0; i < top.length; ++i)
        {
            result[i] = new DAVCommunity(this.context, this.request, this.response,
                    makeChildPath(top[i]), top[i]);
        }
        return result;
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
            value = ConfigurationManager.getProperty("dspace.name");
        }
        else if (elementsEqualIsh(property, news_topProperty))
        {
            value = NewsManager.readNewsFile("news-top.html");
        }
        else if (elementsEqualIsh(property, news_sideProperty))
        {
            value = NewsManager.readNewsFile("news-side.html");
        }
        else if (elementsEqualIsh(property, default_licenseProperty))
        {
            value = LicenseManager.getDefaultSubmissionLicense();
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
        String newValue = (action == DAV.PROPPATCH_REMOVE) ? null : prop
                .getText();
        if (elementsEqualIsh(prop, news_topProperty))
        {
            if (!AuthorizeManager.isAdmin(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to modify this property.");
            }
            NewsManager.writeNewsFile("news-top.html", newValue);
        }
        else if (elementsEqualIsh(prop, news_sideProperty))
        {
            if (!AuthorizeManager.isAdmin(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to modify this property.");
            }
            NewsManager.writeNewsFile("news-side.html", newValue);
        }
        else if (elementsEqualIsh(prop, displaynameProperty))
        {
            throw new DAVStatusException(
                    DAV.SC_CONFLICT,
                    "The site name can only be changed through the DSpace Configuration, \"dspace.name\" property.");
        }
        else
        {
            throw new DAVStatusException(DAV.SC_CONFLICT, "The "
                    + prop.getName() + " property cannot be changed.");
        }
        return HttpServletResponse.SC_OK;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#get()
     */
    @Override
    protected void get() throws SQLException, AuthorizeException,
            IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "GET is not implemented for Site.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT is not implemented for Site.");
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
                "COPY method not implemented.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "DELETE method not implemented for Site.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for Site.");
    }
}
