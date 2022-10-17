/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.dspace.content.Bitstream;

/**
 * A low-level asset store interface
 *
 * @author Richard Rodgers, Peter Dietz
 */

public interface BitStoreService {
    /**
     * Initialize the asset store
     *
     * @throws IOException A general class of exceptions produced by failed or interrupted I/O operations.
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
     * @param bitstream DSpace Bitstream object
     * @return The stream of bits
     * @throws java.io.IOException If a problem occurs while retrieving the bits, or if no
     *                             asset with ID exists in the store
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
     * @param bitstream   The bitstream object
     * @param inputStream The stream of bits
     * @throws java.io.IOException If a problem occurs while storing the bits
     */
    public void put(Bitstream bitstream, InputStream inputStream) throws IOException;

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param bitstream The bitstream to describe
     * @param attrs     A Map whose keys consist of desired metadata fields
     * @return attrs
     * A Map with key/value pairs of desired metadata
     * If file not found, then return null
     * @throws java.io.IOException If a problem occurs while obtaining metadata
     */
    public Map about(Bitstream bitstream, Map attrs) throws IOException;

    /**
     * Remove an asset from the asset store.
     *
     * @param bitstream The bitstream of the asset to delete
     * @throws java.io.IOException If a problem occurs while removing the asset
     */
    public void remove(Bitstream bitstream) throws IOException;

    /**
     * Determines if a store has been initialized
     * 
     * @return {@code boolean} true if initialized, false otherwise
     */
    public boolean isInitialized();

    /**
     * Determines if a store is enabled, by default is enabled
     * 
     * @return {@code boolean} true if enabled, false otherwise
     */
    public default boolean isEnabled() {
        return true;
    }
}
