/*
 * BitstreamFormat.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.content;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.content.dao.BitstreamFormatDAOFactory;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.core.Context;

/**
 * FIXME: Should this extend DSpaceObject?
 */
public class BitstreamFormat
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(BitstreamFormat.class);

    /**
     * The "unknown" support level - for bitstream formats that are unknown to
     * the system
     */
    public static final int UNKNOWN = 0;

    /**
     * The "known" support level - for bitstream formats that are known to the
     * system, but not fully supported
     */
    public static final int KNOWN = 1;

    /**
     * The "supported" support level - for bitstream formats known to the system
     * and fully supported.
     */
    public static final int SUPPORTED = 2;

    /**
     * FIXME This is a bit of a hack, but it stops us from having to reach into
     * the database just to set the short description.
     */
    public static final String UNKNOWN_SHORT_DESCRIPTION = "Unknown";

    private Context context;
    private BitstreamFormatDAO dao;

    private int id;
    private ObjectIdentifier oid;
    private String shortDescription;
    private String description;
    private String mimeType;
    private int supportLevel; // FIXME: enum
    private boolean internal;
    private List<String> extensions;

    public BitstreamFormat(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = BitstreamFormatDAOFactory.getInstance(context);
        extensions = new ArrayList<String>();
    }

    public int getID()
    {
        return id;
    }

    public ObjectIdentifier getIdentifier()
    {
        return oid;
    }

    public void setIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    public String getShortDescription()
    {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription)
    {
        /*
        // You can not reset the unknown's registry's name
        BitstreamFormat unknown = null;
		try
        {
			unknown = findUnknown(context);
		}
        catch (IllegalStateException e)
        {
			// No short_description='Unknown' found in bitstreamformatregistry
			// table. On first load of registries this is expected because it
			// hasn't been inserted yet! So, catch but ignore this runtime 
			// exception thrown by method findUnknown.
		}
		
        // If the exception was thrown, unknown will == null so go ahead and
        // load the new description. If not, check that the unknown's
        // registry's name is not being reset.
		if (unknown == null || unknown.getID() != getID())
        */
        if (!UNKNOWN_SHORT_DESCRIPTION.equals(getShortDescription()))
        {
            this.shortDescription = shortDescription;
		}
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Get the MIME type of this bitstream format, for example
     * <code>text/plain</code>
     */
    public String getMIMEType()
    {
        return mimeType;
    }

    public void setMIMEType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    /**
     * Get the support level for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     */
    public int getSupportLevel()
    {
        return supportLevel;
    }

    /**
     * Set the support level for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     */
    public void setSupportLevel(int supportLevel)
    {
        // Sanity check
        if ((supportLevel < 0) || (supportLevel > 2))
        {
            throw new IllegalArgumentException("Invalid support level");
        }

        this.supportLevel = supportLevel;
    }

    /**
     * Find out if the bitstream format is an internal format - that is, one
     * that is used to store system information, rather than the content of
     * items in the system
     */
    public boolean isInternal()
    {
        return internal;
    }

    /**
     * Set whether the bitstream format is an internal format
     */
    public void setInternal(boolean internal)
    {
        this.internal = internal;
    }

    /**
     * Get the filename extensions associated with this format
     */
    public String[] getExtensions()
    {
        return (String[]) extensions.toArray(new String[0]);
    }

    /**
     * Set the filename extensions associated with this format
     */
    public void setExtensions(String[] extensions)
    {
        this.extensions = new ArrayList<String>();

        for (String extension : extensions)
        {
            this.extensions.add(extension);
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

    /** Deprecated by the introduction of DAOs */
    @Deprecated
    public static BitstreamFormat create(Context context)
        throws AuthorizeException
    {
        BitstreamFormatDAO dao = BitstreamFormatDAOFactory.getInstance(context);
        return dao.create();
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);
    }

    @Deprecated
    public void delete() throws AuthorizeException
    {
        dao.delete(getID());
    }

    @Deprecated
    public static BitstreamFormat find(Context context, int id)
    {
        BitstreamFormatDAO dao = BitstreamFormatDAOFactory.getInstance(context);
        return dao.retrieve(id);
    }

    @Deprecated
    public static BitstreamFormat findByMIMEType(Context context,
            String mimeType)
    {
        BitstreamFormatDAO dao = BitstreamFormatDAOFactory.getInstance(context);
        return dao.retrieveByMimeType(mimeType);
    }

    @Deprecated
    public static BitstreamFormat findByShortDescription(Context context,
            String desc)
    {
        BitstreamFormatDAO dao = BitstreamFormatDAOFactory.getInstance(context);
        return dao.retrieveByShortDescription(desc);
    }

    @Deprecated
    public static BitstreamFormat findUnknown(Context context)
    {
        BitstreamFormat bf = findByShortDescription(context,
                UNKNOWN_SHORT_DESCRIPTION);

        if (bf == null)
        {
            throw new IllegalStateException(
                    "No `Unknown' bitstream format in registry");
        }

        return bf;
    }

    @Deprecated
    public static BitstreamFormat[] findAll(Context context)
    {
        BitstreamFormatDAO dao = BitstreamFormatDAOFactory.getInstance(context);
        List<BitstreamFormat> formats = dao.getBitstreamFormats();

        return formats.toArray(new BitstreamFormat[0]);
    }

    @Deprecated
    public static BitstreamFormat[] findNonInternal(Context context)
    {
        BitstreamFormatDAO dao = BitstreamFormatDAOFactory.getInstance(context);
        List<BitstreamFormat> formats = dao.getBitstreamFormats(false);

        return formats.toArray(new BitstreamFormat[0]);
    }
}
