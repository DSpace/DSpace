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

import org.dspace.batch.ImpRecord;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface ImpRecordService {
    public static final Character SEND_BACK_TO_WORKSPACE_STATUS = 'p';
    public static final Character SEND_THROUGH_WORKFLOW_STATUS = 'w';
    public static final Character REINSTATE_WITHDRAW_ITEM_STATUS = 'z';
    public static final Character SET_ITEM_WITHDRAW_STATUS = 'g';

    public static final String INSERT_OR_UPDATE_OPERATION = "update";
    public static final String DELETE_OPERATION = "delete";

    /**
     * Create a new ImpRecord object
     * 
     * @param context   The relevant DSpace Context.
     * @param impRecord The initial data of ImpRecord
     * @return the created ImpRecord object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpRecord create(Context context, ImpRecord impRecord) throws SQLException;

    /***
     * Set the collection
     * 
     * @param impRecord  the ImpRecord object
     * @param collection the collection
     */
    public void setImpCollection(ImpRecord impRecord, Collection collection);

    /***
     * Set the eperson to use to perform the action
     * 
     * @param impRecord the ImpRecord object
     * @param ePerson   the person
     */
    public void setImpEperson(ImpRecord impRecord, EPerson ePerson);

    /***
     * Set the status that define a supported action
     * 
     * p = Send submission back to workspace w = Send submission through
     * collection's workflow z = Reinstate a withdrawn item g = Set item in
     * withdrawn state
     * 
     * @param impRecord the ImpRecord object
     * @param ePerson   the person
     */
    public void setStatus(ImpRecord impRecord, Character status);

    /***
     * Set the operation
     * 
     * update = update or create a new record delete = delete the record
     * 
     * @param impRecord the ImpRecord object
     * @param ePerson   the person
     */
    public void setOperation(ImpRecord impRecord, String operation);

    /**
     * Find the ImpRecord object by its id
     * 
     * @param context The relevant DSpace Context.
     * @param id      The PK of ImpRecord
     * @return the created ImpRecord object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpRecord findByID(Context context, int id) throws SQLException;

    /***
     * Search all ImpRecord objects with modification date null.
     * 
     * @param context The relevant DSpace Context
     * @return the list of found ImpMetadatavalue objects
     * @throws SQLException
     */
    public List<ImpRecord> searchNewRecords(Context context) throws SQLException;

    /***
     * Count all ImpRecord objects with modification date null and with a given
     * Record Id.
     * 
     * @param context   The relevant DSpace Context
     * @param impRecord The ImpRecord object
     * @return the number of found ImpMetadatavalue objects
     * @throws SQLException
     */
    public int countNewImpRecords(Context context, ImpRecord impRecord) throws SQLException;

    /**
     * Save a ImpRecord object
     *
     * @param context   The relevant DSpace Context.
     * @param impRecord The ImpRecord object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, ImpRecord impRecord) throws SQLException;

    /**
     * Save a ImpRecord object
     *
     * @param context   The relevant DSpace Context.
     * @param impRecord The ImpRecord object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void delete(Context context, ImpRecord impRecord) throws SQLException;

    /***
     * Remove all object inside imp tables
     * 
     * @param context
     * @throws SQLException
     */
    public void cleanupTables(Context context) throws SQLException;
}
