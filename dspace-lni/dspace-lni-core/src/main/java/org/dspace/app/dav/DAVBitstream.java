/*
 * DAVBitstream.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.jdom.Element;
import org.jdom.Namespace;


/**
 * This defines the behavior of DSpace "resources" in the WebDAV interface; it
 * maps DAV operations onto DSpace object.
 */
class DAVBitstream extends DAVDSpaceObject
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVBitstream.class);

    /** The item. */
    private Item item = null;

    /** The bitstream. */
    private Bitstream bitstream = null;
 
    /** The Constant BITSTREAM_INLINE_THRESHOLD. 
     * The longest bitstream that should be rendered "inline" (base64)
     * see makeXmlBitstream
     */
    private final static int BITSTREAM_INLINE_THRESHOLD = 2000;

    /** The Constant getcontentlengthProperty. */
    private static final Element getcontentlengthProperty = new Element(
            "getcontentlength", DAV.NS_DAV);

    /** The Constant getcontenttypeProperty. */
    private static final Element getcontenttypeProperty = new Element(
            "getcontenttype", DAV.NS_DAV);

    /** The Constant sourceProperty. */
    private static final Element sourceProperty = new Element("source",
            DAV.NS_DSPACE);

    /** The Constant descriptionProperty. */
    private static final Element descriptionProperty = new Element(
            "description", DAV.NS_DSPACE);

    /** The Constant formatProperty. */
    private static final Element formatProperty = new Element("format",
            DAV.NS_DSPACE);

    /** The Constant format_descriptionProperty. */
    private static final Element format_descriptionProperty = new Element(
            "format_description", DAV.NS_DSPACE);

    /** The Constant checksumProperty. */
    private static final Element checksumProperty = new Element("checksum",
            DAV.NS_DSPACE);

    /** The Constant checksum_algorithmProperty. */
    private static final Element checksum_algorithmProperty = new Element(
            "checksum_algorithm", DAV.NS_DSPACE);

    /** The Constant sequence_idProperty. */
    private static final Element sequence_idProperty = new Element(
            "sequence_id", DAV.NS_DSPACE);

    /** The Constant bundleProperty. */
    private static final Element bundleProperty = new Element("bundle",
            DAV.NS_DSPACE);

    /** The all props. */
    private static List allProps = new Vector(commonProps);
    static
    {
        allProps.add(getcontentlengthProperty);
        allProps.add(getcontenttypeProperty);
        allProps.add(sourceProperty);
        allProps.add(descriptionProperty);
        allProps.add(formatProperty);
        allProps.add(format_descriptionProperty);
        allProps.add(checksumProperty);
        allProps.add(checksum_algorithmProperty);
        allProps.add(sequence_idProperty);
        allProps.add(bundleProperty);
        allProps.add(handleProperty);
    }

    /**
     * Instantiates a new DAV bitstream.
     * This gets called by matchResourceURI, for /retrieve_<dbid> format
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param bitstream the bitstream
     */
    protected DAVBitstream(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[], Bitstream bitstream)
    {
        super(context, request, response, pathElt, bitstream);
        this.bitstream = bitstream;
        this.type = TYPE_BITSTREAM;
    }

    /**
     * Instantiates a new DAV bitstream.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param item the item
     * @param bitstream the bitstream
     */
    protected DAVBitstream(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[], Item item,
            Bitstream bitstream)
    {
        super(context, request, response, pathElt, bitstream);
        this.bitstream = bitstream;
        this.type = TYPE_BITSTREAM;
        this.item = item;
    }

    /**
     * Make bitstream path element with filename extension, if given.
     * 
     * @param sid the sid
     * @param ext the ext
     * 
     * @return bitstream path element
     */
    protected static String getPathElt(int sid, String ext)
    {
        return "bitstream_" + String.valueOf(sid)
                + (ext == null ? "" : "." + ext);
    }

    /**
     * Attempt to locate Bitstream object from URI. pathElt is
     * "bitstream_{sid}.ext" or "retrieve_{db-id}.ext"
     * 
     * @param context the context
     * @param item the item
     * @param pathElt the path elt
     * 
     * @return the bitstream found (any errors throw an exception)
     * 
     * @throws SQLException the SQL exception
     * @throws DAVStatusException the DAV status exception
     */
    protected static Bitstream findBitstream(Context context, Item item,
            String pathElt) throws SQLException, DAVStatusException
    {
        try
        {
            // get rid of extension, if any, e.g. ".pdf"
            int dot = pathElt.indexOf('.');
            String strId = (dot >= 0) ? pathElt.substring(0, dot) : new String(
                    pathElt);
            Bitstream result = null;

            if (strId.startsWith("bitstream_"))
            {
                strId = strId.substring(10);
                result = getBitstreamBySequenceID(item, Integer.parseInt(strId));
            }
            else if (strId.startsWith("retrieve_"))
            {
                strId = strId.substring(9);
                result = Bitstream.find(context, Integer.parseInt(strId));
            }
            else
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Unrecognized bitstream URI format.");
            }
            if (result == null)
            {
                throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                        "No bitstream at this sequence ID: " + pathElt);
            }
            return result;
        }
        catch (NumberFormatException nfe)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid Bitstream Sequence ID in URI: " + pathElt);
        }
    }

    /**
     * Find bitstream with matching sequence id.
     * 
     * @param item the item
     * @param sid the sid
     * 
     * @return bitstream, or null if none found.
     * 
     * @throws SQLException the SQL exception
     */
    protected static Bitstream getBitstreamBySequenceID(Item item, int sid)
            throws SQLException
    {

        Bundle[] bundles = item.getBundles();
        for (Bundle element : bundles)
        {
            Bitstream[] bitstreams = element.getBitstreams();

            for (Bitstream element0 : bitstreams)
            {
                if (sid == element0.getSequenceID())
                {
                    return element0;
                }
            }
        }
        return null;
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
        return new DAVResource[0];
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVDSpaceObject#propfindInternal(org.jdom.Element)
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        String value = null;

        /*
         * FIXME: This implements permission check that really belongs in
         * business logic. Although communities and collections don't check for
         * read auth, Bitstream may contain sensitive data and should always
         * check for READ permission.
         */
        AuthorizeManager.authorizeAction(this.context, this.bitstream, Constants.READ);

        // displayname - title or handle.
        if (elementsEqualIsh(property, displaynameProperty))
        {
            value = this.bitstream.getName();
            if (value == null)
            {
                value = makeDisplayname();
            }
        }
        else if (elementsEqualIsh(property, getcontentlengthProperty))
        {
            value = String.valueOf(this.bitstream.getSize());
        }
        else if (elementsEqualIsh(property, getcontenttypeProperty))
        {
            value = this.bitstream.getFormat().getMIMEType();
        }
        else if (elementsEqualIsh(property, sourceProperty))
        {
            value = this.bitstream.getSource();
        }
        else if (elementsEqualIsh(property, descriptionProperty))
        {
            value = this.bitstream.getDescription();
        }
        else if (elementsEqualIsh(property, formatProperty))
        {
            BitstreamFormat bsf = this.bitstream.getFormat();
            value = bsf == null ? null : bsf.getShortDescription();
        }
        else if (elementsEqualIsh(property, format_descriptionProperty))
        {
            value = this.bitstream.getFormatDescription();
        }
        else if (elementsEqualIsh(property, checksumProperty))
        {
            value = this.bitstream.getChecksum();
        }
        else if (elementsEqualIsh(property, checksum_algorithmProperty))
        {
            value = this.bitstream.getChecksumAlgorithm();
        }
        else if (elementsEqualIsh(property, sequence_idProperty))
        {
            int sid = this.bitstream.getSequenceID();
            if (sid >= 0)
            {
                value = String.valueOf(sid);
            }
        }
        else if (elementsEqualIsh(property, bundleProperty))
        {
            Bundle bn[] = this.bitstream.getBundles();
            if (bn != null && bn.length > 0)
            {
                value = bn[0].getName();
            }
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

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#proppatchInternal(int, org.jdom.Element)
     */
    @Override
    protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        Namespace ns = prop.getNamespace();
        String propName = prop.getName();
        boolean nsDspace = ns != null && ns.equals(DAV.NS_DSPACE);
        String newValue = (action == DAV.PROPPATCH_REMOVE) ? null : prop
                .getText();

        // displayname - arbitrary string
        if (elementsEqualIsh(prop, displaynameProperty))
        {
            this.bitstream.setName(newValue);
        }
        else if (nsDspace && propName.equals("description"))
        {
            this.bitstream.setDescription(newValue);
        }
        else if (nsDspace && propName.equals("source"))
        {
            this.bitstream.setSource(newValue);
        }
        else if (nsDspace && propName.equals("format_description"))
        {
            this.bitstream.setUserFormatDescription(newValue);
        }
        else if (nsDspace && propName.equals("format"))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "The format property cannot be removed.");
            }
            BitstreamFormat bsf = BitstreamFormat.findByShortDescription(
                    this.context, newValue);
            if (bsf == null)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "Cannot set format, no such Bitstream Format: "
                                + newValue);
            }
            this.bitstream.setFormat(bsf);
        }
        else
        {
            throw new DAVStatusException(DAV.SC_CONFLICT, "The "
                    + prop.getName() + " property cannot be changed.");
        }

        // this assumes we got through an IF clause and changed something:
        this.bitstream.update();
        return HttpServletResponse.SC_OK;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#get()
     */
    @Override
    protected void get() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        if (this.bitstream == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                    "Bitstream not found, URI=\"" + hrefURL() + "\"");
        }
        else
        {
            if (this.item != null)
            {
                log.info(LogManager.getHeader(this.context, "DAV GET Bitstream",
                        "item handle=" + this.item.getHandle() + ", bitstream_id="
                                + this.bitstream.getID()));
            }

            // Set the response MIME type
            this.response.setContentType(this.bitstream.getFormat().getMIMEType());

            // Response length
            this.response.setHeader("Content-Length", String.valueOf(this.bitstream
                    .getSize()));

            // Pipe the bits
            InputStream is = this.bitstream.retrieve();
            Utils.bufferedCopy(is, this.response.getOutputStream());
            is.close();
            this.response.getOutputStream().flush();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT is not implemented for Bitstream (yet?).");
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

    /**
     * Make a default name to go in properties' displayname Should be last path
     * element of canonical resource URI.
     * 
     * @return name string
     */
    private String makeDisplayname()
    {
        String ext[] = this.bitstream.getFormat().getExtensions();
        String prefix = (this.item == null) ? "retrieve_"
                + String.valueOf(this.bitstream.getID()) : "bitstream_"
                + String.valueOf(this.bitstream.getSequenceID());
        return prefix + (ext.length > 0 ? ext[0] : "");
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
        /**
         * Match URI /retrieve_<DbId> NOTE: This is an evil kludge to get raw
         * bitstreams by DB ID, required to implement link form of
         * <dspace:bitstream> element in properties. The "logo" of Community or
         * Collection is a loose bitstream not connected to any Item, so it can
         * only be identified by a direct database-ID reference. Ugh.
         */
        if (pathElt[0].startsWith("retrieve_"))
        {
            Bitstream bs = findBitstream(context, null, pathElt[0]);
            return new DAVBitstream(context, request, response, pathElt, bs);
        }
        return null;
    }

    /**
     * Returns an XML representation of a bitstream -- either inline content or
     * a link reference. The XML looks like:
     * 
     * <pre>
     * &lt;dspace:bitstream&gt;
     * &lt;dspace:link href=&quot;url-to-bitstream&quot;&gt;
     * &lt;/dspace:bitstream&gt;
     * ...or...
     * &lt;dspace:bitstream&gt;
     * &lt;dspace:content contenttype=&quot;image/gif&quot; contentlength=&quot;299&quot; contentencoding=&quot;base64&quot;&gt;
     * ...text of base64..
     * &lt;/dspace:content&gt;
     * &lt;/dspace:bitstream&gt;
     * NOTE: contentlength is the DECODED length of the content.
     * </pre>
     * 
     * Used by the "logo" property on collections and communities.
     * 
     * @param bitstream the bitstream
     * @param resource the resource
     * 
     * @return the element
     * 
     * @throws AuthorizeException the authorize exception
     * @throws SQLException the SQL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static Element makeXmlBitstream(Bitstream bitstream,
            DAVResource resource) throws AuthorizeException, SQLException,
            IOException
    {
        Element b = new Element("bitstream", DAV.NS_DSPACE);
        long length = bitstream.getSize();
        BitstreamFormat bf = bitstream.getFormat();

        if (length > BITSTREAM_INLINE_THRESHOLD)
        {
            Element e = new Element("link", DAV.NS_DSPACE);
            e.setAttribute("href", resource.hrefPrefix() + "retrieve_"
                    + String.valueOf(bitstream.getID()));
            b.addContent(e);
        }
        else
        {
            Element e = new Element("content", DAV.NS_DSPACE);
            if (bf != null)
            {
                e.setAttribute("contenttype", bf.getMIMEType());
            }
            e.setAttribute("contentlength", String.valueOf(length));
            e.setAttribute("contentencoding", "base64");
            b.addContent(e);

            // write encoding of bitstream contents
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) length);
            Utils.copy(bitstream.retrieve(), baos);
            e.setText(new String(Base64.encodeBase64(baos.toByteArray())));
        }
        return b;
    }

    /**
     * Extract bitstream from the XML representation, i.e.
     * 
     * <pre>
     * &lt;dspace:bitstream&gt;
     * &lt;dspace:content contenttype=&quot;image/gif&quot;
     * contentlength=&quot;299&quot;
     * contentencoding=&quot;base64&quot;&gt;
     * ...text of base64..
     * &lt;/dspace:content&gt;
     * &lt;/dspace:bitstream&gt;
     * </pre>
     * 
     * In the above format, contenttype and contentencoding attributes of
     * content are REQUIRED.
     * 
     * @param context the context
     * @param xb the xb
     * 
     * @return inputstream of the contents of the data, or null on error.
     */
    protected static InputStream getXmlBitstreamContent(Context context,
            Element xb)
    {
        Element c = xb.getChild("content", DAV.NS_DSPACE);
        if (c != null)
        {
            String enc = c.getAttributeValue("contentencoding");
            if (enc != null && enc.equals("base64"))
            {
                byte value[] = Base64.decodeBase64(c.getText().getBytes());
                return new ByteArrayInputStream(value);
            }
        }
        return null;
    }

    /**
     * Get the content-type from an XML-encoded bitstream.
     * 
     * @param context required for reading the DB.
     * @param xb XML bitstream representation in JDOM.
     * 
     * @return First BitstreamFormat matching content-type string, or null if
     * none.
     * 
     * @throws SQLException the SQL exception
     */
    protected static BitstreamFormat getXmlBitstreamFormat(Context context,
            Element xb) throws SQLException
    {
        Element c = xb.getChild("content", DAV.NS_DSPACE);
        if (c != null)
        {
            String ctype = c.getAttributeValue("contenttype");
            if (ctype != null)
            {
                BitstreamFormat af[] = BitstreamFormat.findAll(context);
                for (BitstreamFormat element : af)
                {
                    if (ctype.equals(element.getMIMEType()))
                    {
                        return element;
                    }
                }
            }
        }
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
                "DELETE method not implemented for BitStream.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for BitStream.");
    }
}
