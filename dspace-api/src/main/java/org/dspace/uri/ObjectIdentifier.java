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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uri.dao.ObjectIdentifierDAO;
import org.dspace.uri.dao.ObjectIdentifierDAOFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Richard Jones
 * @author James Rutherford
 */
public class ObjectIdentifier implements DSpaceIdentifier
{
    private static Logger log = Logger.getLogger(ObjectIdentifier.class);

    private int resourceID = -1;
    private int resourceTypeID = -1;
    private UUID uuid = null;

    // FIXME: it is acceptable to have an object identifier with only a UUID provided that it can be
    // looked up in the database.  That is, this class can go and look up the resource id and the type
    //
    // What to do in instances where the oid isn't already bound to an object?
    // - First, new DSpaceObjects get ObjectIdentifiers through the Mint, so shouldn't be an issue
    // - Second, non-DSpaceObjects which have ObjectIdentifiers need to exist without resource types, so this is required
    // - Third, worst case scenario we throw a NotSupportedException or somesuch when getObject can't be run
    //
    // Thoughts?
    public ObjectIdentifier(UUID uuid)
    {
        this.uuid = uuid;
    }

    public ObjectIdentifier(String uuid)
    {
        this.uuid = UUID.fromString(uuid);
    }

    public ObjectIdentifier(String uuid, int resourceType, int resourceID)
    {
        this.uuid = UUID.fromString(uuid);
        this.resourceTypeID = resourceType;
        this.resourceID = resourceID;
    }

    public ObjectIdentifier(UUID uuid, int resourceType, int resourceID)
    {
        this.uuid = uuid;
        this.resourceTypeID = resourceType;
        this.resourceID = resourceID;
    }

    public int getResourceID()
    {
        return resourceID;
    }

    public int getResourceTypeID()
    {
        return resourceTypeID;
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public void setResourceID(int resourceID)
    {
        this.resourceID = resourceID;
    }

    public void setResourceTypeID(int resourceTypeID)
    {
        this.resourceTypeID = resourceTypeID;
    }

    public String getURLForm()
    {
        if (uuid == null)
        {
            return null;
        }
        return "uuid/" + uuid.toString();
    }

    @Deprecated
    public URL getURL()
    {
        /*
        try
        {
            String base = ConfigurationManager.getProperty("dspace.url");
            String urlForm = this.getURLForm();

            if (base == null || "".equals(base))
            {
                throw new RuntimeException("No configuration, or configuration invalid for dspace.url");
            }

            if (urlForm == null)
            {
                throw new RuntimeException("Unable to assign URL: no UUID available");
            }

            String url = base + "/resource/" + urlForm;


            return new URL(url);
        }
        catch (MalformedURLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }*/
        return IdentifierFactory.getURL(this);
    }

    public String getCanonicalForm()
    {
        if (uuid == null)
        {
            return null;
        }
        return "uuid:" + uuid.toString();
    }

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

    // STATIC METHODS

    public static ObjectIdentifier parseCanonicalForm(String canonicalForm)
    {
        if (!canonicalForm.startsWith("uuid:"))
        {
            return null;
        }

        String value = canonicalForm.substring(5);

        return new ObjectIdentifier(value);
    }

    public static ObjectIdentifier extractURLIdentifier(String str)
    {
        String oidRX = ".*uuid/([a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}).*";
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


    // PRIVATE METHODS

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
            default:
                throw new RuntimeException("Not a valid DSpaceObject type");
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
