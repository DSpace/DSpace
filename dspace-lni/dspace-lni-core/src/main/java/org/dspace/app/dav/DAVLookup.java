/*
 * DAVLookup.java
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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.jdom.Element;


/**
 * The Lookup resource translates a DSpace persistent object identifier (i.e. an
 * Item or Bitstream Handle) into a DAV resource URI for the LNI. It accepts two
 * simple, flexible formats: one for Item handles and one for a bitstream within
 * an Item.
 * <p>
 * Any GET, PUT, PROPFIND, etc response gets a "Temporarily Moved" status and
 * the DAV URL in the "Location:" header of the response.
 * <p>
 * The "lookup" URI format:
 * 
 * <pre>
 * {prefix}/lookup/handle/{hdl-prefix}/{hdl-suffix}   ... item Handle
 * e.g.
 * {prefix}/lookup/handle/1234.56/99   ... item Handle
 * {prefix}/lookup/handle/1234.56%2f99   ... item Handle
 * {prefix}/lookup/bitstream-handle/{seq-id}/{hdl-prefix}/{hdl-suffix}
 * e.g.
 * {prefix}/lookup/bitstream-handle/13/1234.56/99   ... bitstream Handle
 * {prefix}/lookup/bitstream-handle/13/1234.56%2f99   ... bitstream Handle
 * </pre>
 */
class DAVLookup extends DAVResource
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVLookup.class);

    /**
     * Instantiates a new DAV lookup.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     */
    protected DAVLookup(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[])
    {
        super(context, request, response, pathElt);
    }

    // empty property list, this class doesn't implement propfind.
    /** The Constant allProps. */
    private static final List allProps = new Vector();

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#getAllProperties()
     */
    @Override
    protected List getAllProperties()
    {
        return allProps;
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
     * @return a DAVLookup resource if we can parse this URI, or null.
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     */
    protected static DAVResource matchResourceURI(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[]) throws DAVStatusException, SQLException
    {
        // The "/lookup" request:
        if (pathElt[0].equals("lookup"))
        {
            return new DAVLookup(context, request, response, pathElt);
        }

        return null;
    }

    /**
     * Send a redirect (302) response to client with DAV URL of the resource for
     * this handle and/or bitstream. Puts URL in the <code>Location:</code>
     * header.
     * 
     * @return URL in string form for desired DAV resource.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     * @throws DAVStatusException the DAV status exception
     * 
     * @throw IOException
     * @throw SQLException
     */
    private void doRedirect() throws IOException, SQLException,
            DAVStatusException
    {
        DSpaceObject dso = null;
        String bsPid = null;

        /*
         * FIXME: (maybe?) NOTE: This is currently hard-wired to accomodate the
         * syntax of Handles, with "prefix/suffix" separated by the slash --
         * that means the Handle probably takes up multiple path elements,
         * unless the client escaped the '/'. This code *might* need adjusting
         * if we allow other kinds of persistent identifiers for DSpace objects.
         */
        int hdlStart = -1;
        if (this.pathElt.length > 2 && this.pathElt[1].equals("handle"))
        {
            hdlStart = 2;
        }
        else if (this.pathElt.length > 3 && this.pathElt[1].equals("bitstream-handle"))
        {
            bsPid = this.pathElt[2];
            hdlStart = 3;
        }
        else
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Unrecognized 'lookup' request format.");
        }
        String prefix = decodeHandle(this.pathElt[hdlStart]);
        String handle = null;

        // if "prefix" contains a slash, then it's the whole handle:
        if (prefix.indexOf("/") >= 0)
        {
            handle = prefix;
            log.debug("Lookup: resolving escaped handle \"" + handle + "\"");
        }
        else if (this.pathElt.length >= hdlStart + 2)
        {
            StringBuffer hdl = new StringBuffer(prefix);
            for (int i = hdlStart + 1; i < this.pathElt.length; ++i)
            {
                hdl.append("/");
                hdl.append(this.pathElt[i]);
            }
            handle = hdl.toString();
            log.debug("Lookup: resolving multielement handle \"" + handle
                    + "\"");
        }
        else
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Incomplete handle in lookup request.");
        }

        // did handle lookup fail?
        dso = HandleManager.resolveToObject(this.context, handle);
        if (dso == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                    "Cannot resolve handle \"" + handle + "\"");
        }

        // bitstream must exist too
        String location = makeLocation(dso, bsPid);
        if (location == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                    "Bitstream \"" + bsPid + "\" does not exist in \"" + handle
                            + "\"");
        }

        // add query string -- unnecessary, but it helps naive clients that
        // use GET with "package" query arg to download an Item.
        String qs = this.request.getQueryString();
        if (qs != null)
        {
            location += "?" + qs;
        }

        log.debug("Lookup returning redirect to: " + location);
        this.response.setHeader("Location", location);
        this.response.sendError(HttpServletResponse.SC_MOVED_TEMPORARILY,
                "These are not the droids you are looking for.");
    }

    /**
     * Create URI as string for a given handle and optional bitstream. URI is
     * relative to top of DAV hierarchy, but starts with '/'.
     * 
     * @param handle handle of a DSpace object (Item, Collection, etc)
     * @param bsPid bitstream persistent identifier.
     * 
     * @return "absolute" URI from top of DAV hierarchy
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    protected String makeURI(String handle, String bsPid) throws IOException,
            SQLException
    {
        DSpaceObject dso = HandleManager.resolveToObject(this.context, handle);
        if (dso == null)
        {
            return null;
        }
        return makeURI(dso, bsPid);
    }

    /**
     * Create URI as string for a given handle and optional bitstream. URI is
     * relative to top of DAV hierarchy, but starts with '/'.
     * 
     * @param dso a DSpace object (Item, Collection, etc)
     * @param bsPid bitstream persistent identifier.
     * 
     * @return "absolute" URI from top of DAV hierarchy
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    private String makeURI(DSpaceObject dso, String bsPid) throws IOException,
            SQLException
    {
        // make sure that bitstream actually exists:
        if (bsPid != null)
        {
            if (dso.getType() != Constants.ITEM)
            {
                log.warn("Non-Item with Bitstream Sequence ID in DAV Lookup.");
                return null;
            }
            try
            {
                int pid = Integer.parseInt(bsPid);
                if (DAVBitstream.getBitstreamBySequenceID((Item) dso, pid) == null)
                {
                    log
                            .warn("Bitstream Sequence ID Not Found in DAV Lookup: \""
                                    + bsPid + "\"");
                    return null;
                }
            }
            catch (NumberFormatException nfe)
            {
                log.warn("Invalid Bitstream Sequence ID in DAV Lookup: \""
                        + bsPid + "\"");
                return null;
            }
        }
        String base = "/" + DAVDSpaceObject.getPathElt(dso);
        if (bsPid != null)
        {
            return base + "/bitstream_" + bsPid;
        }
        else
        {
            return base;
        }

    }

    // returns fully-qualified URL or null upon error.
    /**
     * Make location.
     * 
     * @param dso the dso
     * @param bsPid the bs pid
     * 
     * @return the string
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    private String makeLocation(DSpaceObject dso, String bsPid)
            throws IOException, SQLException
    {
        String prefix = hrefPrefix();

        String rest = makeURI(dso, bsPid);
        if (rest == null)
        {
            return null;
        }

        // delete leading '/' from URI since prefix has trailing one
        return prefix + rest.substring(1);
    }

    /**
     * placeholder that does nothing since propfind() is overridden.
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
        return null;
    }

    /**
     * Override propfind() to make sure it always returns a redirect.
     * 
     * @throws ServletException the servlet exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    @Override
    protected void propfind() throws ServletException, SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        this.doRedirect();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#proppatchInternal(int, org.jdom.Element)
     */
    @Override
    protected int proppatchInternal(int mode, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#get()
     */
    @Override
    protected void get() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        this.doRedirect();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        this.doRedirect();
    }

    /**
     * Reject copy. Client should get resource URL first.
     * 
     * @param destination the destination
     * @param depth the depth
     * @param overwrite the overwrite
     * @param keepProperties the keep properties
     * 
     * @return the int
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected int copyInternal(DAVResource destination, int depth,
            boolean overwrite, boolean keepProperties)
            throws DAVStatusException, SQLException, AuthorizeException,
            IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "COPY method not allowed on lookup resource.");
    }

    /**
     * This should never get called.
     * 
     * @return the DAV resource[]
     * 
     * @throws SQLException the SQL exception
     */
    @Override
    protected DAVResource[] children() throws SQLException
    {
        return new DAVResource[0];
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "DELETE method not implemented for Lookup.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for Lookup.");
    }
}
