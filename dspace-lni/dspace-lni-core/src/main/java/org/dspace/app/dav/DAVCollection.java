/*
 * DAVCollection.java
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

import java.io.FilterInputStream;
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
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.jdom.Element;


/**
 * This defines the behavior of DSpace "resources" in the WebDAV interface; it
 * maps DAV operations onto DSpace object.s
 */
class DAVCollection extends DAVDSpaceObject
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVCollection.class);

    /** The collection. */
    private Collection collection = null;

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

    /** The Constant provenance_descriptionProperty. */
    private static final Element provenance_descriptionProperty = new Element(
            "provenance_description", DAV.NS_DSPACE);

    /** The Constant default_licenseProperty. */
    private static final Element default_licenseProperty = new Element(
            "default_license", DAV.NS_DSPACE);

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
        allProps.add(default_licenseProperty);
        allProps.add(provenance_descriptionProperty);
        allProps.add(handleProperty);
    }

    /**
     * Instantiates a new DAV collection.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param collection the collection
     */
    protected DAVCollection(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[],
            Collection collection)
    {
        super(context, request, response, pathElt, collection);
        this.collection = collection;
        this.type = TYPE_COLLECTION;
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
        ItemIterator ii = this.collection.getItems();
        while (ii.hasNext())
        {
            Item item = ii.next();
            result.add(new DAVItem(this.context, this.request, this.response,
                    makeChildPath(item), item));
        }
        return (DAVResource[]) result.toArray(new DAVResource[result.size()]);
    }

    /**
     * Gets the collection.
     * 
     * @return the DSpace Collection object represented by this resource.
     */
    protected Collection getCollection()
    {
        return this.collection;
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
                value = this.collection.getHandle();
            }
        }

        else if (elementsEqualIsh(property, handleProperty))
        {
            value = canonicalizeHandle(this.collection.getHandle());
        }
        else if (elementsEqualIsh(property, logoProperty))
        {
            Bitstream lbs = this.collection.getLogo();
            Element le;
            if (lbs != null
                    && (le = DAVBitstream.makeXmlBitstream(lbs, this)) != null)
            {
                Element p = new Element("logo", DAV.NS_DSPACE);
                p.addContent(le);
                return p;
            }
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
        else if (elementsEqualIsh(property, default_licenseProperty))
        {
            value = this.collection.hasCustomLicense() ? this.collection.getLicense()
                    : null;
        }
        else if (elementsEqualIsh(property, provenance_descriptionProperty))
        {
            value = getObjectMetadata("provenance_description");
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

    // syntactic sugar around getting collection metadata values:
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
            return this.collection.getMetadata(mdname);
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
                || elementsEqualIsh(prop, copyright_textProperty)
                || elementsEqualIsh(prop, provenance_descriptionProperty))
        {
            this.collection.setMetadata(prop.getName(), newValue);
        }
        else if (elementsEqualIsh(prop, displaynameProperty))
        {
            this.collection.setMetadata("name", newValue);
        }
        else if (elementsEqualIsh(prop, default_licenseProperty))
        {
            this.collection.setLicense(newValue);
        }
        else if (elementsEqualIsh(prop, logoProperty))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                this.collection.setLogo(null);
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
                    Bitstream nbs = this.collection.setLogo(bis);
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

        this.collection.update();
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
                "GET not implemented for Collection.");
    }

    /**
     * Wrapped input stream that hits end-of-file after reading a counted number
     * of bytes, even if its client stream appears to have more data. This fixes
     * a problem in the Servlet container's input stream which will try to read
     * past the end of the request body.
     */
    private class CountedInputStream extends FilterInputStream
    {
        
        /** The count. */
        private long count = 0;

        /** The length. */
        private long length = -1;

        /**
         * Instantiates a new counted input stream.
         * 
         * @param is the is
         * @param length the length
         */
        protected CountedInputStream(InputStream is, long length)
        {
            super(is);
            this.length = length;
        }

        /* (non-Javadoc)
         * @see java.io.FilterInputStream#read()
         */
        @Override
        public int read() throws IOException
        {
            if (++this.count > this.length)
            {
                return -1;
            }
            return super.read();
        }

        /* (non-Javadoc)
         * @see java.io.FilterInputStream#read(byte[])
         */
        @Override
        public int read(byte[] b) throws IOException
        {
            if (this.count >= this.length)
            {
                return -1;
            }
            int result = super.read(b);
            if (this.count > 0)
            {
                this.count += result;
            }
            return result;
        }

        /* (non-Javadoc)
         * @see java.io.FilterInputStream#read(byte[], int, int)
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            if (this.count >= this.length)
            {
                return -1;
            }
            int result = super.read(b, off, len);
            if (this.count > 0)
            {
                this.count += result;
            }
            return result;
        }

        /* (non-Javadoc)
         * @see java.io.FilterInputStream#skip(long)
         */
        @Override
        public long skip(long n) throws IOException
        {
            long result = super.skip(n);
            this.count += result;
            return result;
        }
    }

    /**
     * PUT ingests a package as a new Item. Package type (must match pluggable
     * packager name) is in either (a) "package" query arg in URI (b)
     * content-type request header
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        try
        {
            String packageType = this.request.getParameter("package");
            if (packageType == null)
            {
                packageType = this.request.getContentType();
            }
            if (packageType == null)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Cannot determine package type,  need content-type header or package param");
            }

            PackageIngester sip = (PackageIngester) PluginManager
                    .getNamedPlugin(PackageIngester.class, packageType);
            if (sip == null)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Cannot find importer for package type: " + packageType);
            }

            /*
             * Ugh. Servlet container doesn't get end-of-file right on input
             * stream so we have to count it, when possible.
             */
            int contentLength = this.request.getIntHeader("Content-Length");
            InputStream pis = this.request.getInputStream();
            if (contentLength >= 0)
            {
                pis = new CountedInputStream(pis, contentLength);
                log.debug("put: Using CountedInputStream, length="
                        + String.valueOf(contentLength));
            }
            WorkspaceItem wi = sip.ingest(this.context, this.collection, pis,
                    PackageParameters.create(this.request), null);
            WorkflowItem wfi = WorkflowManager.startWithoutNotify(this.context, wi);

            // get new item's location: if workflow completed, then look
            // for handle (but be ready for disappointment); otherwise,
            // return the workflow item's resource.
            int state = wfi.getState();
            String location = null;
            if (state == WorkflowManager.WFSTATE_ARCHIVE)
            {
                Item ni = wfi.getItem();
                String handle = HandleManager.findHandle(this.context, ni);
                String end = (handle != null) ? DAVDSpaceObject
                        .getPathElt(handle) : DAVItem.getPathElt(ni);
                DAVItem newItem = new DAVItem(this.context, this.request, this.response,
                        makeChildPath(end), ni);
                location = newItem.hrefURL();
            }
            else if (state == WorkflowManager.WFSTATE_SUBMIT
                    || state == WorkflowManager.WFSTATE_STEP1POOL)
            {
                location = hrefPrefix() + DAVWorkflow.getPath(wfi);
            }
            else
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Workflow object in unexpected state, state="
                                + String.valueOf(state) + ", aborting PUT.");
            }

            this.context.commit();
            log.info("Created new Item, location=" + location);
            this.response.setHeader("Location", location);
            this.response.setStatus(HttpServletResponse.SC_CREATED);
        }
        catch (PackageException pe)
        {
            pe.log(log);
            throw new DAVStatusException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, pe.toString());
        }
        catch (CrosswalkException ie)
        {
            String reason = "";
            if (ie.getCause() != null)
            {
                reason = ", Reason: " + ie.getCause().toString();
            }
            log.error(ie.toString() + reason);
            throw new DAVStatusException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ie.toString()
                            + reason);
        }
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
                "COPY method not implemented for Collection.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        Community[] ca = this.collection.getCommunities();
        if (ca != null)
            for (Community element : ca)
            {
                element.removeCollection(this.collection);
            }
        // collection.delete();
        return HttpServletResponse.SC_OK; // HTTP OK
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for Collection.");
    }
}
