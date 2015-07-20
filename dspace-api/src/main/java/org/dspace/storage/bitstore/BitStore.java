/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

/**
 * A low-level asset store interface
 * 
 * @author Richard Rodgers, Peter Dietz
 */

public interface BitStore
{
	/**
     * Initialize the asset store
     * 
     * @param config
     *        String used to characterize configuration - may be a configuration
     *        value, or the name of a config file containing such values
     */
	public void init(String config) throws IOException;
	
	/**
     * Return an identifier unique to this asset store instance
     * 
     * @return a unique ID
     */
	public String generateId();
	
	/**
     * Retrieve the bits for the asset with ID.
     * 
     * @param id
     *         The ID of the asset to retrieve
     * @exception IOException
     *         If a problem occurs while retrieving the bits, or if no
     *         asset with ID exists in the store
     * 
     * @return The stream of bits
     */
	public InputStream get(String id) throws IOException;
	
    /**
     * Store a stream of bits.
     * 
     * <p>
     * If this method returns successfully, the bits have been stored.
     * If an exception is thrown, the bits have not been stored.
     * </p>
     *
     * @param in
     *            The stream of bits to store
     * @param id
     *            The ID to assign to the asset
     * @exception IOException
     *             If a problem occurs while storing the bits
     * 
     * @return Map containing technical metadata (size, checksum, etc)
     */
	public Map put(InputStream in, String id) throws IOException;
	
    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param id
     *            The ID of the asset to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     * 
     * @exception IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     */
	public Map about(String id, Map attrs) throws IOException;
	
    /**
     * Remove an asset from the asset store.
     *
     * @param id
     *            The ID of the asset to delete
     * @exception IOException
     *            If a problem occurs while removing the asset
     */
	public void remove(String id) throws IOException;
}
