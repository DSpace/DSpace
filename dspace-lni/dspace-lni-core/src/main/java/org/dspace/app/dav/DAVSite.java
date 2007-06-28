/*
 * DAVSite.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.dav;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.jdom.Element;


/**
 * Model the DSpace Site as a resource. The URI is the top level "collection".
 * Only an Administrator can modify properties.
 */
class DAVSite extends DAVResource
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVSite.class);

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
    private static List allProps = new Vector(commonProps);
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
    protected List getAllProperties()
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
            value = ConfigurationManager.readNewsFile("news-top.html");
        }
        else if (elementsEqualIsh(property, news_sideProperty))
        {
            value = ConfigurationManager.readNewsFile("news-side.html");
        }
        else if (elementsEqualIsh(property, default_licenseProperty))
        {
            value = ConfigurationManager.getDefaultSubmissionLicense();
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
            ConfigurationManager.writeNewsFile("news-top.html", newValue);
        }
        else if (elementsEqualIsh(prop, news_sideProperty))
        {
            if (!AuthorizeManager.isAdmin(this.context))
            {
                throw new DAVStatusException(HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to modify this property.");
            }
            ConfigurationManager.writeNewsFile("news-side.html", newValue);
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
            ServletException, IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "GET is not implemented for Site.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
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
