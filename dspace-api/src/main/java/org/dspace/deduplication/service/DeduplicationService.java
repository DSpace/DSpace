/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.deduplication.Deduplication;

public interface DeduplicationService {
    /**
     * Create a new Deduplication object
     *
     * @param context The relevant DSpace Context.
     * @return the created Deduplication object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public Deduplication create(Context context, Deduplication dedup) throws SQLException;

    /***
     * Return all deduplication objects
     * 
     * @param context
     * @param pageSize
     * @param offset
     * @return The list al all deduplication objects
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public List<Deduplication> findAll(Context context, int pageSize, int offset) throws SQLException;

    /**
     * Count all accounts.
     *
     * @param context The relevant DSpace Context.
     * @return the total number of deduplication
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Save a Deduplication object
     *
     * @param context The relevant DSpace Context.
     * @param dedup   The deduplication object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, Deduplication dedup) throws SQLException;

    public List<Deduplication> getDeduplicationByFirstAndSecond(Context context, UUID firstId, UUID secondId)
            throws SQLException;

    public Deduplication uniqueDeduplicationByFirstAndSecond(Context context, UUID firstId, UUID secondId)
            throws SQLException;
}
