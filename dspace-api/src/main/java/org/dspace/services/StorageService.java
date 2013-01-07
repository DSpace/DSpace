/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.io.InputStream;

import org.dspace.orm.entity.Bitstream;
import org.dspace.services.exceptions.StorageException;

/**
 * The Storage Service is intended to replace the BitstreamStorageManager class.
 * With a service approach.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public interface StorageService {
	/**
     * Retrieve the bits for the bitstream with ID. If the bitstream does not
     * exist, or is marked deleted throws an exception.
	 * 
	 * @param bitstream The bitstream to get
	 * @return InputStream of the file
	 * @throws StorageException If something wrong happens
	 */
	InputStream retrieve (Bitstream bitstream) throws StorageException;
	
	/**
     * <p>
     * Remove a bitstream from the asset store. This method does not delete any
     * bits, but simply marks the bitstreams as deleted (the context still needs
     * to be completed to finalize the transaction).
     * </p>
     * 
     * <p>
     * If the context is aborted, the bitstreams deletion status remains
     * unchanged.
     * </p>
     * 
	 * @param b The bitstream to delete 
	 * @throws StorageException If something wrong happens
	 */
	void delete (Bitstream b) throws StorageException;
	
	/**
     * Store a stream of bits.
     * 
     * <p>
     * If this method returns successfully, the bits have been stored, and RDBMS
     * metadata entries are in place (the context still needs to be completed to
     * finalize the transaction).
     * </p>
     * 
     * <p>
     * If this method returns successfully and the context is aborted, then the
     * bits will be stored in the asset store and the RDBMS metadata entries
     * will exist, but with the deleted flag set.
     * </p>
     * 
     * If this method throws an exception, then any of the following may be
     * true:
     * 
     * <ul>
     * <li>Neither bits nor RDBMS metadata entries have been stored.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * but no bits.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * and some or all of the bits have also been stored.
     * </ul>
     * 
	 * @param input  The stream of bits to store
	 * @return The stored bitstream
	 * @throws StorageException If something goes wrong
	 */
	Bitstream store (InputStream input) throws StorageException;
	
	/**
	 * Register a bitstream already in storage.
	 * 
	 * @param assetstore The assetstore number for the bitstream to be registered
	 * @param path The relative path of the bitstream to be registered.
	 * 		The path is relative to the path of ths assetstore
	 * @return The registered bitstream
	 * @throws StorageException
	 */
	Bitstream register (int assetstore, String path) throws StorageException;
}
