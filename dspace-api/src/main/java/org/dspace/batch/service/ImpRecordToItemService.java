/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.service;

import java.sql.SQLException;

import org.dspace.batch.ImpRecordToItem;
import org.dspace.core.Context;

public interface ImpRecordToItemService {
    /**
     * Create a new ImpRecordToItem object
     * 
     * @param context         The relevant DSpace Context.
     * @param impRecordToItem The initial data of ImpRecordToItem
     * @return the created ImpRecordToItem object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpRecordToItem create(Context context, ImpRecordToItem impRecordToItem) throws SQLException;

    /***
     * Search the ImpRecordToItem objects by its PK.
     * 
     * @param context     The relevant DSpace Context
     * @param impRecordId The PK
     * @return the list of founded ImpMetadatavalue objects
     * @throws SQLException
     */
    public ImpRecordToItem findByPK(Context context, String impRecordId) throws SQLException;

    /**
     * Save a ImpRecordToItem object
     *
     * @param context         The relevant DSpace Context.
     * @param impRecordToItem The ImpRecordToItem object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, ImpRecordToItem impRecordToItem) throws SQLException;

    /**
     * Save a ImpRecordToItem object
     *
     * @param context         The relevant DSpace Context.
     * @param impRecordToItem The ImpRecordToItem object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void delete(Context context, ImpRecordToItem impRecordToItem) throws SQLException;
}
