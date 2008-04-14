/*
 * ObjectIdentifier.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.uri;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.uri.dao.ObjectIdentifierDAO;
import org.dspace.uri.dao.ObjectIdentifierDAOFactory;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entity class to represent the native DSpace identification mechanism for full
 * scale DSpaceObjects.  This extends the SimpleIdentifier class, which simply supports
 * the use of UUIDs, and implements the ResolvableIdentifier interface so that it
 * can be used to construct URLs and to have objects behind those URLs resolved
 *
 * @author Richard Jones
 * @author James Rutherford
 */
public class ObjectIdentifier extends SimpleIdentifier implements ResolvableIdentifier
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ObjectIdentifier.class);

    /** naming prefix for this identifier type */
    private static final String PREFIX = "uuid";

    /** url identifier regular expression */
    private static final String URL_IDENTIFIER_REGEX = ".*uuid/([a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}).*";

    /** the database id of the resource being identified */
    protected int resourceID = -1;

    /** the constant id of the resource type being identifier, from Constants */
    protected int resourceTypeID = -1;

    /**
     * Construct an object identifier with just a UUID.
     *
     * This should be used with care, as <code>ObjectIdentifiers</code> can be expected to resolve
     * to an object upon request.  Therefore, when creating a new identifier this
     * way it should immediately be added to an <code>Identifiable</code> object using the
     * <code>setIdentifier</code> method, or the <code>setResourceID</code> and
     * <code>setResourceTypeID</code> methods should be called immediately after instantiation.
     * If neither of these are done, then it will be assumed that the UUID is available in
     * the identifier registry, and attempts to retrieve the additional data will be made
     * from there
     *
     * @param uuid
     */
    public ObjectIdentifier(UUID uuid)
    {
        super(uuid);
    }

    /**
     * Construct an object identifier with the string representation of a uuid
     *
     * This should be used with care, as <code>ObjectIdentifiers</code> can be expected to resolve
     * to an object upon request.  Therefore, when creating a new identifier this
     * way it should immediately be added to an <code>Identifiable</code> object using the
     * <code>setIdentifier</code> method, or the <code>setResourceID</code> and
     * <code>setResourceTypeID</code> methods should be called immediately after instantiation.
     * If neither of these are done, then it will be assumed that the UUID is available in
     * the identifier registry, and attempts to retrieve the additional data will be made
     * from there
     *
     * @param uuid
     */
    public ObjectIdentifier(String uuid)
    {
        super(uuid);
    }

    /**
     * Create a full ObjectIdentifier using a string representation of the uuid
     *
     * @param uuid
     * @param resourceType
     * @param resourceID
     */
    public ObjectIdentifier(String uuid, int resourceType, int resourceID)
    {
        super(uuid);
        this.resourceTypeID = resourceType;
        this.resourceID = resourceID;
    }

    /**
     * Create a full ObjectIdentifier
     *
     * @param uuid
     * @param resourceType
     * @param resourceID
     */
    public ObjectIdentifier(UUID uuid, int resourceType, int resourceID)
    {
        super(uuid);
        this.resourceTypeID = resourceType;
        this.resourceID = resourceID;
    }

    /**
     * Get the resource id, which is the id the storage mechanism uses to identifiy
     * the item (e.g. the database row id)
     *
     * @return
     */
    public int getResourceID()
    {
        return resourceID;
    }

    /**
     * Get the resource type id, which will be one of the DSpaceObject types in
     * <code>Constants</code>
     *
     * @return
     */
    public int getResourceTypeID()
    {
        return resourceTypeID;
    }

    /**
     * Set the storage layer identifier which this ObjectIdentifier represents.  e.g.
     * the database row id
     *
     * @param resourceID
     */
    public void setResourceID(int resourceID)
    {
        this.resourceID = resourceID;
    }

    /**
     * set the resource type id, which will be the <code>Constants</code> for the DSpaceObject
     *
     * @param resourceTypeID
     */
    public void setResourceTypeID(int resourceTypeID)
    {
        this.resourceTypeID = resourceTypeID;
    }

    ///////////////////////////////////////////////////////////////////
    // ResolvableIdentifier Methods (overrides SimpleIdentifier)
    ///////////////////////////////////////////////////////////////////

    /**
     * Get the context path URL form of the identifier.  This will be of the form:
     *
     * uuid/[uuid]
     *
     * @return
     */
    public String getURLForm()
    {
        if (uuid == null)
        {
            return null;
        }
        return ObjectIdentifier.PREFIX + "/" + uuid.toString();
    }

    /**
     * Return a string representation of the identifier type; specifically for use when
     * working with ResolvableIdentifiers with no notion of whether the underlying identifier
     * is an ExternalIdentifier or an ObjectIdentifier
     *
     * @return
     */
    public String getIdentifierType()
    {
        return ObjectIdentifier.PREFIX;
    }

    /**
     * obtain the actual DSpaceObject the object identifier represents.
     *
     * FIXME: it is debatable as to whether this legitimately belongs here
     *
     * @param context
     * @return
     */
    /* NOTE: this has been totally removed, and exists here for reference for a few cycles
    public DSpaceObject getObject(Context context)
    {
        // do we know what the resource type and id is?
        if (this.resourceTypeID == -1 || this.resourceID == -1)
        {
            // we don't have resource type or resource id for this item
            // check the UUID cache and see if we can find them
            ObjectIdentifierDAO dao = ObjectIdentifierDAOFactory.getInstance(context);
            ObjectIdentifier noid = dao.retrieve(uuid);

            // if there is no object identifier, just return null
            if (noid == null)
            {
                return null;
            }

            // move the values up to this object for convenience
            this.resourceTypeID = noid.getResourceTypeID();
            this.resourceID = noid.getResourceID();
        }

        // now we can select the object based on its resource type and id
        return this.getObjectByResourceID(context);
    }
*/

    /**
     * Return the ObjectIdentifier to which this ResolvableIdentifier refers.  This
     * is the degenerate case, as this method simply returns the current object "this"
     *
     * @return this
     */
    public ObjectIdentifier getObjectIdentifier()
    {
        return this;
    }

    ////////////////////////////////////////////////////////////////////////
    // Static methods
    ////////////////////////////////////////////////////////////////////////

    /**
     * Take a string in the canonical form for this identifier and turn it into
     * and ObjectIdentifier
     *
     * @param canonicalForm
     * @return
     */
    public static ObjectIdentifier parseCanonicalForm(String canonicalForm)
    {
        if (!canonicalForm.startsWith(ObjectIdentifier.PREFIX + ":"))
        {
            return null;
        }

        String value = canonicalForm.substring(5);

        return new ObjectIdentifier(value);
    }

    /**
     * Take a string which is a url path segment and attempt to locate within it
     * an identifier which conforms to the profile of an ObjectIdentifier.  If one
     * is found then the ObjectIdentifier is created and returned
     *
     * @param str
     * @return
     */
    public static ObjectIdentifier extractURLIdentifier(String str)
    {
        String oidRX = ObjectIdentifier.URL_IDENTIFIER_REGEX;
        Pattern p = Pattern.compile(oidRX);
        Matcher m = p.matcher(str);
        if (!m.matches())
        {
            return null;
        }
        String value = m.group(1);
        ObjectIdentifier oid = new ObjectIdentifier(value);
        return oid;
    }

    ///////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    //////////////////////////////////////////////////////////////////

    /**
     * Use the member variable resource type id to determine the type of object
     * we are attempting to get hold of, and then use the resource id through
     * the DAO layer to obtain an instance of the object
     *
     * @param context
     * @return
     */
    /*
    private DSpaceObject getObjectByResourceID(Context context)
    {
        switch(resourceTypeID)
        {
            case (Constants.BITSTREAM):
                BitstreamDAO bitstreamDAO = BitstreamDAOFactory.getInstance(context);
                return bitstreamDAO.retrieve(resourceID);
            case (Constants.BUNDLE):
                BundleDAO bundleDAO = BundleDAOFactory.getInstance(context);
                return bundleDAO.retrieve(resourceID);
            case (Constants.ITEM):
                ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
                return itemDAO.retrieve(resourceID);
            case (Constants.COLLECTION):
                CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);
                return collectionDAO.retrieve(resourceID);
            case (Constants.COMMUNITY):
                CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);
                return communityDAO.retrieve(resourceID);
            case (Constants.EPERSON):
                EPersonDAO epDAO = EPersonDAOFactory.getInstance(context);
                return epDAO.retrieve(resourceID);
            case (Constants.GROUP):
                GroupDAO gDAO = GroupDAOFactory.getInstance(context);
                return gDAO.retrieve(resourceID);
            default:
                throw new RuntimeException("Not a valid DSpaceObject type");
        }
    }*/

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * Convert the current object to a string.  For use in debugging.
     * @return
     */
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
