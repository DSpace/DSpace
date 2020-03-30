/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.batch.ImpBitstream;
import org.dspace.batch.ImpRecord;
import org.dspace.core.Context;

/***
 * Interface used to access ImpBitstream entities.
 * 
 * @See {@link org.dspace.batch.ImpBitstream}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public interface ImpBitstreamService {
    /**
     * Create a new ImpBitstream object
     * 
     * @param context      The relevant DSpace Context.
     * @param impBitstream The initial data of ImpBitstream
     * @return the created ImpBitstream object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpBitstream create(Context context, ImpBitstream impBitstream) throws SQLException;

    /**
     * Find the ImpBitsteamRecord object by its id
     * 
     * @param context The relevant DSpace Context.
     * @param id      The PK of ImpBitstream
     * @return the created ImpBitstream object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpBitstream findByID(Context context, int id) throws SQLException;

    /***
     * Search all ImpBitstream objects by import Id
     * 
     * @param context   The relevant DSpace Context
     * @param impRecord The ImpRecord object
     * @return the list of founded ImpBitstream objects
     * @throws SQLException
     */
    public List<ImpBitstream> searchByImpRecord(Context context, ImpRecord impRecord) throws SQLException;

    /**
     * Save a ImpBitstream object
     *
     * @param context      The relevant DSpace Context.
     * @param impBitstream The ImpBitstream object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, ImpBitstream impBitstream) throws SQLException;

    /**
     * Save a ImpBitstream object
     *
     * @param context      The relevant DSpace Context.
     * @param impBitstream The ImpBitstream object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void delete(Context context, ImpBitstream impBitstream) throws SQLException;
}
