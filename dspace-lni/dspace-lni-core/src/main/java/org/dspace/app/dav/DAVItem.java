/*
 * DAVItem.java
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

import java.io.ByteArrayOutputStream;
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
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.license.CreativeCommons;
import org.jdom.Element;
import org.jdom.Namespace;


/**
 * This defines the behavior of DSpace "resources" in the WebDAV interface; it
 * maps DAV operations onto DSpace object.s
 */
class DAVItem extends DAVDSpaceObject
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVItem.class);

    /** The item. */
    private Item item = null;

    /** The Constant submitterProperty. */
    private static final Element submitterProperty = new Element("submitter",
            DAV.NS_DSPACE);

    /** The Constant getlastmodifiedProperty. */
    private static final Element getlastmodifiedProperty = new Element(
            "getlastmodified", DAV.NS_DAV);

    /** The Constant licenseProperty. */
    private static final Element licenseProperty = new Element("license",
            DAV.NS_DSPACE);

    /** The Constant cc_license_textProperty. */
    private static final Element cc_license_textProperty = new Element(
            "cc_license_text", DAV.NS_DSPACE);

    /** The Constant cc_license_rdfProperty. */
    private static final Element cc_license_rdfProperty = new Element(
            "cc_license_rdf", DAV.NS_DSPACE);

    /** The Constant cc_license_urlProperty. */
    private static final Element cc_license_urlProperty = new Element(
            "cc_license_url", DAV.NS_DSPACE);

    /** The Constant owning_collectionProperty. */
    private static final Element owning_collectionProperty = new Element(
            "owning_collection", DAV.NS_DSPACE);

    /** The Constant withdrawnProperty. */
    private static final Element withdrawnProperty = new Element("withdrawn",
            DAV.NS_DSPACE);

    /** The all props. */
    private static List allProps = new Vector(commonProps);
    static
    {
        allProps.add(submitterProperty);
        allProps.add(getlastmodifiedProperty);
        allProps.add(licenseProperty);
        allProps.add(cc_license_textProperty);
        allProps.add(cc_license_rdfProperty);
        allProps.add(cc_license_urlProperty);
        allProps.add(owning_collectionProperty);
        allProps.add(handleProperty);
        allProps.add(withdrawnProperty);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#getAllProperties()
     */
    @Override
    protected List getAllProperties()
    {
        return allProps;
    }

    /**
     * Instantiates a new DAV item.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param item the item
     */
    protected DAVItem(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[], Item item)
    {
        super(context, request, response, pathElt, item);
        this.type = TYPE_ITEM;
        this.item = item;
    }

    /**
     * Match Item URIs that identify the item by a database ID. Handle URIs are
     * matched by DAVDSpaceObject.
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
        int id = -1;
        String bsElt = null;

        try
        {
            // The "/item_db_" element in last or next-to-last element
            if (pathElt[pathElt.length - 1].startsWith("item_db_"))
            {
                id = Integer.parseInt(pathElt[pathElt.length - 1].substring(8));
            }
            else if (pathElt[pathElt.length - 1].startsWith("bitstream_")
                    && pathElt.length > 1
                    && pathElt[pathElt.length - 2].startsWith("item_db_"))
            {
                id = Integer.parseInt(pathElt[pathElt.length - 2].substring(8));
                bsElt = pathElt[pathElt.length - 1];
            }
            if (id >= 0)
            {
                Item item = Item.find(context, id);
                if (item == null)
                {
                    throw new DAVStatusException(
                            HttpServletResponse.SC_NOT_FOUND, "Item with ID="
                                    + String.valueOf(id) + " not found.");
                }
                if (bsElt != null)
                {
                    Bitstream bs = DAVBitstream.findBitstream(context, item,
                            bsElt);
                    if (bs == null)
                    {
                        throw new DAVStatusException(
                                HttpServletResponse.SC_NOT_FOUND,
                                "Bitstream not found.");
                    }
                    return new DAVBitstream(context, request, response,
                            pathElt, item, bs);
                }
                else
                {
                    return new DAVItem(context, request, response, pathElt,
                            item);
                }
            }
            return null;
        }
        catch (NumberFormatException ne)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Error parsing number in request URI.");
        }
    }

    /**
     * Return pathname element in db-id format.
     * 
     * @param dbid the dbid
     * 
     * @return the path elt
     */
    protected static String getPathElt(int dbid)
    {
        return "item_db_" + String.valueOf(dbid);
    }

    /**
     * Return pathname element to this Item. Use db-id format if no handle,
     * otherwise the DSpace object format.
     * 
     * @param item the item
     * 
     * @return the path elt
     */
    protected static String getPathElt(Item item)
    {
        String handle = item.getHandle();
        if (handle == null)
        {
            return getPathElt(item.getID());
        }
        else
        {
            return DAVDSpaceObject.getPathElt(item);
        }
    }

    /**
     * Return this resource's children. Item's children are its bitstreams.
     * 
     * @return the DAV resource[]
     * 
     * @throws SQLException the SQL exception
     */
    @Override
    protected DAVResource[] children() throws SQLException
    {
        // Check for overall read permission on Item
        if (!AuthorizeManager.authorizeActionBoolean(this.context, this.item,
                Constants.READ))
        {
            return new DAVResource[0];
        }

        Vector result = new Vector();
        Bundle[] bundles = this.item.getBundles();
        for (Bundle element : bundles)
        {
            // check read permission on this Bundle
            if (!AuthorizeManager.authorizeActionBoolean(this.context, element,
                    Constants.READ))
            {
                continue;
            }

            Bitstream[] bitstreams = element.getBitstreams();
            for (Bitstream element0 : bitstreams)
            {
                String ext[] = element0.getFormat().getExtensions();
                result.add(new DAVBitstream(this.context, this.request, this.response,
                        makeChildPath(DAVBitstream.getPathElt(element0
                                .getSequenceID(), ext.length < 1 ? null
                                : ext[0])), this.item, element0));
            }
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

        /*
         * FIXME: This implements permission check that really belongs in
         * business logic. Although communities and collections don't check for
         * read auth, Item may contain sensitive data and should always check
         * for READ permission. Exception: allow "withdrawn" property to be
         * checked regardless of authorization since all permissions are removed
         * when item is withdrawn.
         */
        if (!elementsEqualIsh(property, withdrawnProperty))
        {
            AuthorizeManager.authorizeAction(this.context, this.item, Constants.READ);
        }

        if (elementsEqualIsh(property, withdrawnProperty))
        {
            value = String.valueOf(this.item.isWithdrawn());
        }
        else if (elementsEqualIsh(property, displaynameProperty))
        {
            // displayname - title or handle.
            DCValue titleDc[] = this.item.getDC("title", Item.ANY, Item.ANY);
            value = titleDc.length > 0 ? titleDc[0].value : this.item.getHandle();
        }
        else if (elementsEqualIsh(property, handleProperty))
        {
            value = canonicalizeHandle(this.item.getHandle());
        }
        else if (elementsEqualIsh(property, submitterProperty))
        {
            EPerson ep = this.item.getSubmitter();
            if (ep != null)
            {
                value = hrefToEPerson(ep);
            }
        }
        else if (elementsEqualIsh(property, owning_collectionProperty))
        {
            Collection owner = this.item.getOwningCollection();
            if (owner != null)
            {
                value = canonicalizeHandle(owner.getHandle());
            }
        }
        else if (elementsEqualIsh(property, getlastmodifiedProperty))
        {
            value = DAV.HttpDateFormat.format(this.item.getLastModified());
        }
        else if (elementsEqualIsh(property, licenseProperty))
        {
            value = getLicenseAsString();
        }
        else if (elementsEqualIsh(property, cc_license_textProperty))
        {
            value = CreativeCommons.getLicenseText(this.item);
        }
        else if (elementsEqualIsh(property, cc_license_rdfProperty))
        {
            value = CreativeCommons.getLicenseRDF(this.item);
        }
        else if (elementsEqualIsh(property, cc_license_urlProperty))
        {
            value = CreativeCommons.getLicenseURL(this.item);
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

    /**
     * Get the license for this Item as String. License is the first bitstream
     * named "license.txt" in a LICENSE bundle, apparently?
     * <p>
     * FIXME: is this correct? there's no counterexample..
     * 
     * @return license string, or null if none found.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private String getLicenseAsString() throws SQLException,
            AuthorizeException, IOException
    {
        Bundle lb[] = this.item.getBundles(Constants.LICENSE_BUNDLE_NAME);
        for (Bundle element : lb)
        {
            Bitstream lbs = element.getBitstreamByName("license.txt");
            if (lbs != null)
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(
                        (int) lbs.getSize());
                Utils.copy(lbs.retrieve(), baos);
                return baos.toString();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#proppatchInternal(int, org.jdom.Element)
     */
    @Override
    protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        // Don't need any authorization checks since the Item layer
        // checks authorization for write and delete.

        Namespace ns = prop.getNamespace();
        String propName = prop.getName();

        // "submitter" is the only Item-specific mutable property, rest are
        // live and unchangeable.
        if (ns != null && ns.equals(DAV.NS_DSPACE)
                && propName.equals("submitter"))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "The submitter property cannot be removed.");
            }
            String newName = prop.getText();
            EPerson ep = EPerson.findByEmail(this.context, newName);
            if (ep == null)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "Cannot set submitter, no EPerson found for email address: "
                                + newName);
            }
            this.item.setSubmitter(ep);
            this.item.update();
            return HttpServletResponse.SC_OK;
        }
        throw new DAVStatusException(DAV.SC_CONFLICT, "The " + prop.getName()
                + " property cannot be changed.");
    }

    /**
     * GET implementation returns the contents of the Item as a package. The
     * query arg "package" must be specified.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    @Override
    protected void get() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        // Check for overall read permission on Item, because nothing else will
        AuthorizeManager.authorizeAction(this.context, this.item, Constants.READ);

        String packageType = this.request.getParameter("package");
        Bundle[] original = this.item.getBundles("ORIGINAL");
        int bsid;

        if (packageType == null)
        {
            packageType = "default";
        }
        PackageDisseminator dip = (PackageDisseminator) PluginManager
                .getNamedPlugin(PackageDisseminator.class, packageType);
        if (dip == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot find a disseminate plugin for package="
                            + packageType);
        }
        else
        {
            try
            {
                PackageParameters pparams = PackageParameters.create(this.request);
                this.response.setContentType(dip.getMIMEType(pparams));
                dip.disseminate(this.context, this.item, pparams, this.response
                        .getOutputStream());
            }
            catch (CrosswalkException pe)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed in crosswalk of metadata: " + pe.toString());
            }
            catch (PackageException pe)
            {
                pe.log(log);
                throw new DAVStatusException(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR, pe
                                .toString());
            }
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
                "PUT is not implemented for Item.");
    }

    /**
     * COPY is implemented by an "add", which is the closest we can get in
     * DSpace semantics.
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
        return addItemToCollection(this.context, this.item, destination, overwrite);
    }

    /**
     * Do the work of "copy" method; this code is shared with e.g.
     * DAVInProgressSubmission.
     * 
     * @param context the context
     * @param item the item
     * @param destination the destination
     * @param overwrite the overwrite
     * 
     * @return HTTP status code.
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static int addItemToCollection(Context context, Item item,
            DAVResource destination, boolean overwrite)
            throws DAVStatusException, SQLException, AuthorizeException,
            IOException
    {
        // sanity checks
        if (!(destination instanceof DAVCollection))
        {
            throw new DAVStatusException(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "COPY of Item is only allowed when destination is a DSpace Collection.");
        }

        // access check
        AuthorizeManager.authorizeAction(context, item, Constants.READ);

        // make sure item doesn't belong to this collection
        Collection destColl = ((DAVCollection) destination).getCollection();

        log.debug("COPY from=" + item.toString() + " (" + item.getHandle()
                + "), to=" + destColl.toString() + " (" + destColl.getHandle()
                + ")");

        // check if it's already a member
        Collection refs[] = item.getCollections();
        for (Collection element : refs)
        {
            if (destColl.equals(element))
            {
                log.debug("COPY - item @ " + item.getHandle()
                        + " is already a member of collection @ "
                        + destColl.getHandle());
                if (overwrite)
                {
                    return DAV.SC_NO_CONTENT;
                }
                else
                {
                    throw new DAVStatusException(DAV.SC_CONFLICT,
                            "This Item is already a member of collection handle="
                                    + destColl.getHandle());
                }
            }
        }

        destColl.addItem(item);
        return DAV.SC_NO_CONTENT;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        this.item.withdraw();
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
                "MKCOL method not allowed for Item.");
    }
}
