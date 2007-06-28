/*
 * DAVDSpaceObject.java
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.jdom.Element;


/**
 * Superclass for resources representing DSpace Objects like Item, Collection
 * etc. Defines the behavior of DSpace "resources" in the WebDAV interface; maps
 * DAV operations onto DSpace object.
 */
abstract class DAVDSpaceObject extends DAVResource
{
    
    /** Object of this resource, set by subclass' initializer. */
    protected DSpaceObject dso = null;

    /** Prototype of DAV Property "handle". */
    protected static final Element handleProperty = new Element("handle",
            DAV.NS_DSPACE);

    /** Special character used to separate handle prefix from suffix in DAV resource URIs - substitute this for the '/' normally used in Handle syntax since the '/' causes all sorts of problems for broken DAV clients. */
    private static final char handleSeparator = '$';

    /**
     * Instantiates a new DAVD space object.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param dso the dso
     */
    protected DAVDSpaceObject(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[], DSpaceObject dso)
    {
        super(context, request, response, pathElt);
        this.dso = dso;
    }

    /**
     * Make URI path element for a DSpaceObject.
     * 
     * @param dso the DSpaceObject, which needs to have a valid Handle.
     * 
     * @return path element string or null if no handle.
     */
    protected static String getPathElt(DSpaceObject dso)
    {
        String handle = dso.getHandle();
        if (handle == null)
        {
            return null;
        }
        return getPathElt(handle);
    }

    /**
     * Make URI path element for a DSpaceObject.
     * 
     * @param handle handle of a DSpaceObject.
     * 
     * @return path element string or null if no handle.
     */
    protected static String getPathElt(String handle)
    {
        int hs;
        if (handleSeparator != '/' && (hs = handle.indexOf('/')) >= 0)
        {
            char hc[] = handle.toCharArray();
            hc[hs] = handleSeparator;
            handle = String.copyValueOf(hc);
        }
        return "dso_" + encodeHandle(handle);
    }

    /**
     * Match the URIs this subclass understands and return the corresponding
     * resource. Since the "dso_" format can lead to several different resource
     * types, handle it here.
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
        // Match /dso_<handle>{...} .. look for last "dso_" element
        if (pathElt[0].startsWith("dso_"))
        {
            int i = 1;
            for (; i < pathElt.length && pathElt[i].startsWith("dso_"); ++i)
            {
                // empty
            }
            --i;
            String handle = decodeHandle(pathElt[i].substring(4));

            // Replace substituted handle separator char with '/' to
            // get back a normal handle: (inverse of getPathElt() above)
            int sepIndex = handle.indexOf(handleSeparator);
            if (sepIndex >= 0)
            {
                char hc[] = handle.toCharArray();
                hc[sepIndex] = '/';
                handle = String.copyValueOf(hc);
            }

            DSpaceObject dso = HandleManager.resolveToObject(context, handle);
            if (dso == null)
            {
                throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                        "Cannot resolve handle \"" + handle + "\"");
            }
            else if (dso.getType() == Constants.ITEM)
            {
                if (i + 1 < pathElt.length)
                {
                    if (pathElt[i + 1].startsWith("bitstream_"))
                    {
                        Bitstream bs = DAVBitstream.findBitstream(context,
                                (Item) dso, pathElt[i + 1]);
                        if (bs == null)
                        {
                            throw new DAVStatusException(
                                    HttpServletResponse.SC_NOT_FOUND,
                                    "Bitstream \"" + pathElt[i + 1]
                                            + "\" not found in item: "
                                            + pathElt[i]);
                        }
                        return new DAVBitstream(context, request, response,
                                pathElt, (Item) dso, bs);
                    }
                    else
                    {
                        throw new DAVStatusException(
                                HttpServletResponse.SC_NOT_FOUND,
                                "Illegal resource path, \""
                                        + pathElt[i + 1]
                                        + "\" is not a Bitstream identifier for item: "
                                        + pathElt[i]);
                    }
                }
                else
                {
                    return new DAVItem(context, request, response, pathElt,
                            (Item) dso);
                }
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                return new DAVCollection(context, request, response, pathElt,
                        (Collection) dso);
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                return new DAVCommunity(context, request, response, pathElt,
                        (Community) dso);
            }
            else
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Unrecognized DSpace object type for handle=" + handle);
            }
        }
        return null;
    }

    /**
     * Interposed between subclass and common props, take care of shared props
     * like privileges, handle, dspace:type.
     * 
     * @param property the property
     * 
     * @return the element
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        String value = null;

        if (elementsEqualIsh(property, handleProperty))
        {
            value = canonicalizeHandle(this.dso.getHandle());
        }
        else if (elementsEqualIsh(property, current_user_privilege_setProperty))
        {
            Element c = (Element) current_user_privilege_setProperty.clone();

            // if we're an admin we have all privs everywhere.
            if (AuthorizeManager.isAdmin(this.context))
            {
                addPrivilege(c, new Element("all", DAV.NS_DAV));
            }
            else
            {
                for (int i = 0; i < Constants.actionText.length; ++i)
                {
                    if (AuthorizeManager
                            .authorizeActionBoolean(this.context, this.dso, i))
                    {
                        Element priv = actionToPrivilege(i);
                        if (priv != null)
                        {
                            addPrivilege(c, priv);
                        }
                    }
                }
            }
            return c;
        }
        else
        {
            return commonPropfindInternal(property,
                    this.dso.getType() != Constants.BITSTREAM);
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

    /**
     * Return value of DSpace type property. It is conveniently the same as our
     * internal type name.
     * 
     * @return the element
     */
    @Override
    protected Element typeValue()
    {
        return new Element(Constants.typeText[this.dso.getType()].toLowerCase(),
                DAV.NS_DSPACE);
    }

}
