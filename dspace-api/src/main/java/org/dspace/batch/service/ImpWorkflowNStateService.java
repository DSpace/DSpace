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
import org.dspace.batch.ImpWorkflowNState;
import org.dspace.core.Context;

/***
 * Interface used to access ImpBitstreamMetadatavalue entities.
 * 
 * @See {@link org.dspace.batch.ImpBitstreamMetadatavalue}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public interface ImpWorkflowNStateService {
    /**
     * Create a new ImpWorkflowNState object
     * 
     * @param context   The relevant DSpace Context.
     * @param impRecord The initial data of ImpWorkflowNState
     * @return the created ImpWorkflowNState object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpWorkflowNState create(Context context, ImpWorkflowNState impRecord) throws SQLException;

    /***
     * Search all ImpWorkflowNState objects related to a given Record Id.
     * 
     * @param context   The relevant DSpace Context
     * @param impRecord The ImpRecord object
     * @return the list of found ImpWorkflowNState objects
     * @throws SQLException
     */
    public List<ImpWorkflowNState> searchWorkflowOps(Context context, ImpRecord impRecord) throws SQLException;

    /**
     * Save a ImpWorkflowNState object
     *
     * @param context   The relevant DSpace Context.
     * @param impRecord The ImpWorkflowNState object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, ImpWorkflowNState impRecord) throws SQLException;

    /**
     * Save a ImpWorkflowNState object
     *
     * @param context   The relevant DSpace Context.
     * @param impRecord The ImpWorkflowNState object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void delete(Context context, ImpWorkflowNState impRecord) throws SQLException;

}
