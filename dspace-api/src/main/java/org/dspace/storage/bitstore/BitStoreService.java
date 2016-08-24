/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.dspace.content.Bitstream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A low-level asset store interface
 * 
 * @author Richard Rodgers, Peter Dietz
 */

public interface BitStoreService
{
    /**
     * Initialize the asset store
     *
     */
	public void init() throws IOException;

    /**
     * Return an identifier unique to this asset store instance
     * 
     * @return a unique ID
     */
	public String generateId();
	
    /**
     * Retrieve the bits for bitstream
     * 
     * @param bitstream
     *
     * @exception java.io.IOException
     *         If a problem occurs while retrieving the bits, or if no
     *         asset with ID exists in the store
     *
     * @return The stream of bits
     */
	public InputStream get(Bitstream bitstream) throws IOException;

    /**
     * Store a stream of bits.
     *
     * <p>
     * If this method returns successfully, the bits have been stored.
     * If an exception is thrown, the bits have not been stored.
     * </p>
     *
     * @param bitstream
     *            The bitstream object
     * @param inputStream
     *            The stream of bits
     * @exception java.io.IOException
     *             If a problem occurs while storing the bits
     */
	public void put(Bitstream bitstream, InputStream inputStream) throws IOException;

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param bitstream
     *            The bitstream to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     *
     * @exception java.io.IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     *            If file not found, then return null
     */
	public Map about(Bitstream bitstream, Map attrs) throws IOException;

    /**
     * Remove an asset from the asset store.
     *
     * @param bitstream
     *            The bitstream of the asset to delete
     * @exception java.io.IOException
     *            If a problem occurs while removing the asset
     */
	public void remove(Bitstream bitstream) throws IOException;
}
