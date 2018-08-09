/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Service interface class for the Bitstream object.
 * The implementation of this class is responsible for all business logic calls for the Bitstream object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BitstreamService extends DSpaceObjectService<Bitstream>, DSpaceObjectLegacySupportService<Bitstream>{

    public List<Bitstream> findAll(Context context) throws SQLException;

    /**
     * Clone the given bitstream by firstly creating a new bitstream, with a new ID.
     * Then set the internal identifier, file size, checksum, and 
     * checksum algorithm as same as the given bitstream. 
     * This allows multiple bitstreams to share the same internal identifier of assets . 
     * An example of such a use case scenario is versioning.
     * 
     * @param context
     *            DSpace context object
     * @param bitstream
     *            Bitstream to be cloned
     * @return the clone 
     * @throws SQLException if database error
     */
    public Bitstream clone(Context context, Bitstream bitstream) throws SQLException; 
   
    /**
     * Create a new bitstream, with a new ID. The checksum and file size are
     * calculated. No authorization checks are made in this method.
     * The newly created bitstream has the "unknown" format.
     *
     * @param context
     *            DSpace context object
     * @param is
     *            the bits to put in the bitstream
     *
     * @return the newly created bitstream
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public Bitstream create(Context context, InputStream is) throws IOException, SQLException;

    /**
     * Create a new bitstream, with a new ID. The checksum and file size are
     * calculated.
     * The newly created bitstream has the "unknown" format.
     *
     * @param context
     *            DSpace context object
     * @param bundle
     *            The bundle in which our bitstream should be added.
     *
     * @param is
     *            the bits to put in the bitstream
     *
     * @return the newly created bitstream
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Bitstream create(Context context, Bundle bundle, InputStream is) throws IOException, SQLException, AuthorizeException;
 
    /**
     * Register a new bitstream, with a new ID.  The checksum and file size
     * are calculated. The newly created bitstream has the "unknown"
     * format.
     *
     * @param  context DSpace context object
     * @param bundle The bundle in which our bitstream should be added.
     * @param assetstore corresponds to an assetstore in dspace.cfg
     * @param bitstreamPath the path and filename relative to the assetstore
     * @return  the newly registered bitstream
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Bitstream register(Context context, Bundle bundle, int assetstore, String bitstreamPath)
            throws IOException, SQLException, AuthorizeException;

    /**
     * Register a new bitstream, with a new ID.  The checksum and file size
     * are calculated. The newly created bitstream has the "unknown"
     * format.
     *
     * @param  context DSpace context object
     * @param assetstore corresponds to an assetstore in dspace.cfg
     * @param bitstreamPath the path and filename relative to the assetstore
     * @return  the newly registered bitstream
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Bitstream register(Context context, int assetstore, String bitstreamPath)
        	throws IOException, SQLException, AuthorizeException;

    /**
     * Set the user's format description. This implies that the format of the
     * bitstream is uncertain, and the format is set to "unknown."
     *
     * @param  context DSpace context object
     * @param  bitstream DSpace bitstream
     * @param desc
     *            the user's description of the format
     * @throws SQLException if database error
     */
    public void setUserFormatDescription(Context context, Bitstream bitstream, String desc) throws SQLException;

    /**
     * Get the description of the format - either the user's or the description
     * of the format defined by the system.
     * 
     * @param  context DSpace context object
     * @param  bitstream DSpace bitstream
     * @return a description of the format.
     * @throws SQLException if database error
     */
    public String getFormatDescription(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Set the format of the bitstream. If the user has supplied a type
     * description, it is cleared. Passing in <code>null</code> sets the type
     * of this bitstream to "unknown".
     *
     * @param  context DSpace context object
     * @param  bitstream DSpace bitstream
     * @param bitstreamFormat
     *            the format of this bitstream, or <code>null</code> for
     *            unknown
     * @throws SQLException if database error
     */
    public void setFormat(Context context, Bitstream bitstream, BitstreamFormat bitstreamFormat) throws SQLException;

    /**
     * Retrieve the contents of the bitstream
     *
     * @param  context DSpace context object
     * @param  bitstream DSpace bitstream
     * @return a stream from which the bitstream can be read.
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public InputStream retrieve(Context context, Bitstream bitstream) throws IOException, SQLException, AuthorizeException;

    /**
     * Determine if this bitstream is registered (available elsewhere on
     * filesystem than in assetstore). More about registered items:
     * https://wiki.duraspace.org/display/DSDOC3x/Registering+(not+Importing)+Bitstreams+via+Simple+Archive+Format
     *
     * @param  bitstream DSpace bitstream
     * @return true if the bitstream is registered, false otherwise
     */
    public boolean isRegisteredBitstream(Bitstream bitstream);

    /**
     * Retrieve all bitstreams with the deleted flag set to true
     * @param context the dspace context
     * @return a list of all bitstreams that have been "deleted"
     * @throws SQLException if database error
     */
    public List<Bitstream> findDeletedBitstreams(Context context) throws SQLException;


    /**
     * Remove a bitstream that has been set to "deleted" from the database
     * @param context the dspace context
     * @param bitstream the bitstream to deleted from the database
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void expunge(Context context, Bitstream bitstream) throws SQLException, AuthorizeException;

    public List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException;

    public Iterator<Bitstream> getItemBitstreams(Context context, Item item) throws SQLException;

    public Iterator<Bitstream> getCollectionBitstreams(Context context, Collection collection) throws SQLException;

    public Iterator<Bitstream> getCommunityBitstreams(Context context, Community community) throws SQLException;

    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException;

    public Bitstream getBitstreamByName(Item item, String bundleName, String bitstreamName) throws SQLException;

    public Bitstream getFirstBitstream(Item item, String bundleName) throws SQLException;

    public BitstreamFormat getFormat(Context context, Bitstream bitstream) throws SQLException;

    public Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException;

    public Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException;

    int countTotal(Context context) throws SQLException;

    int countDeletedBitstreams(Context context) throws SQLException;

    int countBitstreamsWithoutPolicy(Context context) throws SQLException;

    List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException;
}
