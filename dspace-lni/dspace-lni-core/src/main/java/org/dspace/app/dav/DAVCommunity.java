/*
 * DAVCommunity.java
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
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.jdom.Element;


/**
 * This defines the behavior of DSpace "resources" in the WebDAV interface; it
 * maps DAV operations onto DSpace object.s
 */
class DAVCommunity extends DAVDSpaceObject
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVCommunity.class);

    /** The community. */
    private Community community = null;

    /** The Constant short_descriptionProperty. */
    private static final Element short_descriptionProperty = new Element(
            "short_description", DAV.NS_DSPACE);

    /** The Constant introductory_textProperty. */
    private static final Element introductory_textProperty = new Element(
            "introductory_text", DAV.NS_DSPACE);

    /** The Constant side_bar_textProperty. */
    private static final Element side_bar_textProperty = new Element(
            "side_bar_text", DAV.NS_DSPACE);

    /** The Constant copyright_textProperty. */
    private static final Element copyright_textProperty = new Element(
            "copyright_text", DAV.NS_DSPACE);

    /** The Constant logoProperty. */
    private static final Element logoProperty = new Element("logo",
            DAV.NS_DSPACE);

    /** The all props. */
    private static List allProps = new Vector(commonProps);
    
    static
    {
        allProps.add(logoProperty);
        allProps.add(short_descriptionProperty);
        allProps.add(introductory_textProperty);
        allProps.add(side_bar_textProperty);
        allProps.add(copyright_textProperty);
        allProps.add(handleProperty);
    }

    /**
     * Instantiates a new DAV community.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param community the community
     */
    protected DAVCommunity(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[], Community community)
    {
        super(context, request, response, pathElt, community);
        this.community = community;
        this.type = TYPE_COMMUNITY;
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
        Vector result = new Vector();
        Community subs[] = this.community.getSubcommunities();
        for (Community element : subs)
        {
            result.add(new DAVCommunity(this.context, this.request, this.response,
                    makeChildPath(element), element));
        }
        Collection colls[] = this.community.getCollections();
        for (Collection element : colls)
        {
            result.add(new DAVCollection(this.context, this.request, this.response,
                    makeChildPath(element), element));
        }
        return (DAVResource[]) result.toArray(new DAVResource[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVDSpaceObject#propfindInternal(org.jdom.Element)
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        String value = null;

        // displayname - title or handle.
        if (elementsEqualIsh(property, displaynameProperty))
        {
            value = getObjectMetadata("name");
            if (value == null)
            {
                value = this.community.getHandle();
            }
        }

        // special case, value is XML, not string:
        else if (elementsEqualIsh(property, logoProperty))
        {
            Bitstream lbs = this.community.getLogo();
            Element le;
            if (lbs != null
                    && (le = DAVBitstream.makeXmlBitstream(lbs, this)) != null)
            {
                Element p = new Element("logo", DAV.NS_DSPACE);
                p.addContent(le);
                return p;
            }
        }

        else if (elementsEqualIsh(property, handleProperty))
        {
            value = canonicalizeHandle(this.community.getHandle());
        }
        else if (elementsEqualIsh(property, short_descriptionProperty))
        {
            value = getObjectMetadata("short_description");
        }
        else if (elementsEqualIsh(property, introductory_textProperty))
        {
            value = getObjectMetadata("introductory_text");
        }
        else if (elementsEqualIsh(property, side_bar_textProperty))
        {
            value = getObjectMetadata("side_bar_text");
        }
        else if (elementsEqualIsh(property, copyright_textProperty))
        {
            value = getObjectMetadata("copyright_text");
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

    // syntactic sugar around getting community metadata values:
    /**
     * Gets the object metadata.
     * 
     * @param mdname the mdname
     * 
     * @return the object metadata
     */
    private String getObjectMetadata(String mdname)
    {
        try
        {
            return this.community.getMetadata(mdname);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
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

        // these are "metadata" values..
        if (elementsEqualIsh(prop, short_descriptionProperty)
                || elementsEqualIsh(prop, introductory_textProperty)
                || elementsEqualIsh(prop, side_bar_textProperty)
                || elementsEqualIsh(prop, copyright_textProperty))
        {
            this.community.setMetadata(prop.getName(), newValue);
        }
        else if (elementsEqualIsh(prop, displaynameProperty))
        {
            this.community.setMetadata("name", newValue);
        }
        else if (elementsEqualIsh(prop, logoProperty))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                this.community.setLogo(null);
            }
            else
            {
                Element bs = prop.getChild("bitstream", DAV.NS_DSPACE);
                if (bs != null)
                {
                    InputStream bis = DAVBitstream.getXmlBitstreamContent(
                            this.context, bs);
                    BitstreamFormat bsf = DAVBitstream.getXmlBitstreamFormat(
                            this.context, bs);
                    if (bis == null || bsf == null)
                    {
                        throw new DAVStatusException(DAV.SC_CONFLICT,
                                "Unacceptable value for logo property.");
                    }
                    Bitstream nbs = this.community.setLogo(bis);
                    nbs.setFormat(bsf);
                    nbs.update();
                }
                else
                {
                    throw new DAVStatusException(DAV.SC_CONFLICT,
                            "No <bitstream> element value found for logo property.");
                }
            }
        }
        else
        {
            throw new DAVStatusException(DAV.SC_CONFLICT, "The "
                    + prop.getName() + " property cannot be changed.");
        }

        this.community.update();
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
                "GET method not implemented for Community.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT method not implemented for Community.");
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
                "COPY method not implemented for Community.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        Community c = this.community.getParentCommunity();
        if (c != null)
        {
            c.removeSubcommunity(this.community);
        }
        this.community.delete();
        return HttpServletResponse.SC_OK;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String name) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        Collection newColl = this.community.createCollection();
        newColl.setMetadata("name", name);
        newColl.update();
        return HttpServletResponse.SC_OK;
    }
}
