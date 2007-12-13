/*
 * Bitstream.java
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.dspace.storage.bitstore.BitstreamStorageManager;


/**
 * Class representing bitstreams stored in the DSpace system.
 * <P>
 * When modifying the bitstream metadata, changes are not reflected in the
 * database until <code>update</code> is called. Note that you cannot alter
 * the contents of a bitstream; you need to create a new bitstream.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Bitstream extends DSpaceObject
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(Bitstream.class);

    private BitstreamDAO dao;
    private BundleDAO bundleDAO;

    private int sequenceID;
    private String name;
    private String source;
    private String description;
    private String checksum;
    private String checksumAlgorithm;
    private Long sizeBytes;
    private String userFormatDescription;
    private BitstreamFormat bitstreamFormat;
    private int storeNumber;
    private String internalID;
    private boolean deleted;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;
    
    public Bitstream(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = BitstreamDAOFactory.getInstance(context);
        bundleDAO = BundleDAOFactory.getInstance(context);

        modified = modifiedMetadata = false;
        clearDetails();
    }

    public int getSequenceID()
    {
        return sequenceID;
    }

    public void setSequenceID(int sequenceID)
    {
        this.sequenceID = sequenceID;
        modifiedMetadata = true;
        addDetails("SequenceID");
    }

    // FIXME: Do we even want this exposed?
    public String getInternalID()
    {
        return internalID;
    }

    // FIXME: Do we even want this exposed?
    public void setInternalID(String internalID)
    {
        this.internalID = internalID;
    }

    /**
     * Get the name of this bitstream - typically the filename, without any path
     * information
     * 
     * @return the name of the bitstream
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
        modifiedMetadata = true;
        addDetails("Name");
    }

    /**
     * Get the source of this bitstream - typically the filename with path
     * information (if originally provided) or the name of the tool that
     * generated this bitstream
     * 
     * @return the source of the bitstream
     */
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
        modifiedMetadata = true;
        addDetails("Source");
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
        modifiedMetadata = true;
        addDetails("Description");
    }

    /**
     * Get the checksum of the content of the bitstream.
     * 
     * @return the checksum
     */
    public String getChecksum()
    {
        return checksum;
    }

    // FIXME: Do we even want this exposed?
    public void setChecksum(String checksum)
    {
        this.checksum = checksum;
    }

    /**
     * Get the algorithm used to calculate the checksum
     * 
     * @return the algorithm, e.g. "MD5"
     */
    public String getChecksumAlgorithm()
    {
        return checksumAlgorithm;
    }

    // FIXME: Do we even want this exposed?
    public void setChecksumAlgorithm(String checksumAlgorithm)
    {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    /**
     * Get the size of the bitstream
     * 
     * @return the size in bytes
     */
    public long getSize()
    {
        return (sizeBytes == null ? 0 : sizeBytes.longValue());
    }

    // FIXME: Do we even want this exposed?
    public void setSize(Long sizeBytes)
    {
        this.sizeBytes = sizeBytes;
    }

    /**
     * Set the user's format description. This implies that the format of the
     * bitstream is uncertain, and the format is set to "unknown."
     * 
     * @param desc the user's description of the format
     */
    public void setUserFormatDescription(String desc)
    {
        setFormat(null);
        this.userFormatDescription = desc;
        modifiedMetadata = true;
        addDetails("UserFormatDescription");
    }

    /**
     * Get the user's format description. Returns null if the format is known by
     * the system.
     * 
     * @return the user's format description.
     */
    public String getUserFormatDescription()
    {
        return userFormatDescription;
    }

    /**
     * Get the description of the format - either the user's or the description
     * of the format defined by the system.
     * 
     * @return a description of the format.
     */
    public String getFormatDescription()
    {
        if (BitstreamFormat.UNKNOWN_SHORT_DESCRIPTION.equals(
                    bitstreamFormat.getShortDescription()))
        {
            // Get user description if there is one
            if (userFormatDescription == null)
            {
                return BitstreamFormat.UNKNOWN_SHORT_DESCRIPTION;
            }

            return userFormatDescription;
        }

        // not null or Unknown
        return bitstreamFormat.getShortDescription();
    }

    /**
     * Get the format of the bitstream
     * 
     * @return the format of this bitstream
     */
    public BitstreamFormat getFormat()
    {
        return bitstreamFormat;
    }

    /**
     * Set the format of the bitstream. If the user has supplied a type
     * description, it is cleared. Passing in <code>null</code> sets the type
     * of this bitstream to "unknown".
     * 
     * @param f
     *            the format of this bitstream, or <code>null</code> for
     *            unknown
     */
    public void setFormat(BitstreamFormat f)
    {
        if (f == null)
        {
            // Use "Unknown" format
            bitstreamFormat = BitstreamFormat.findUnknown(context);
        }
        else
        {
            bitstreamFormat = f;
        }

        // Remove user type description
        userFormatDescription = null;
        modified = true;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    /**
     * Retrieve the contents of the bitstream
     * 
     * @return a stream from which the bitstream can be read.
     * @throws AuthorizeException
     */
    public InputStream retrieve() throws AuthorizeException, IOException
    {
        // Maybe should return AuthorizeException??
        AuthorizeManager.authorizeAction(context, this, Constants.READ);

        return BitstreamStorageManager.retrieve(context, getID());
    }
    
    /**
     * Determine if this bitstream is registered
     * 
     * @return true if the bitstream is registered, false otherwise
     */
    public boolean isRegisteredBitstream()
    {
        return BitstreamStorageManager.isRegisteredBitstream(internalID);
    }
    
    /**
     * Get the asset store number where this bitstream is stored
     * 
     * @return the asset store number of the bitstream
     */
    public int getStoreNumber()
    {
        return storeNumber;
    }

    // FIXME: Do we even want this exposed?
    public void setStoreNumber(int storeNumber)
    {
        this.storeNumber = storeNumber;
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public int getType()
    {
        return Constants.BITSTREAM;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public static Bitstream find(Context context, int id)
    {
        return BitstreamDAOFactory.getInstance(context).retrieve(id);
    }

    @Deprecated
    public Bundle[] getBundles()
    {
        List<Bundle> bundles = bundleDAO.getBundles(this);
        return bundles.toArray(new Bundle[0]);
    }

    @Deprecated
    static Bitstream create(Context context, InputStream is)
            throws AuthorizeException, IOException
    {
        Bitstream bitstream = BitstreamDAOFactory.getInstance(context).store(is);
        context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM, bitstream.getID(), null));
        return bitstream;
    }

    @Deprecated
    static Bitstream register(Context context, int assetstore,
            String bitstreamPath) throws AuthorizeException, IOException
    {
        
        Bitstream bitstream =  BitstreamDAOFactory.getInstance(context).register(assetstore,
                bitstreamPath);
        context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM, bitstream.getID(), "REGISTER"));
        return bitstream;
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);
        
        if (modified)
         {
             context.addEvent(new Event(Event.MODIFY, Constants.BITSTREAM, getID(), null));
             modified = false;
         }
         if (modifiedMetadata)
         {
             context.addEvent(new Event(Event.MODIFY_METADATA, Constants.BITSTREAM, getID(), getDetails()));
             modifiedMetadata = false;
             clearDetails();
         }
    }

    @Deprecated
    void delete() throws AuthorizeException
    {
        dao.delete(this.getID());
        context.addEvent(new Event(Event.DELETE, Constants.BITSTREAM, getID(), getIdentifier().getCanonicalForm()));
    }
}
