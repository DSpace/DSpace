/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.batch.ImpRecord;
import org.dspace.batch.ImpWorkflowNState;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the ImpWorkflowNState object. The
 * implementation of this class is responsible for all database calls for the
 * ImpWorkflowNState object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public interface ImpWorkflowNStateDAO extends GenericDAO<ImpWorkflowNState> {
    /***
     * Search all ImpWorkflowNState objects related to a given Record Id.
     * 
     * @param context   The relevant DSpace Context
     * @param impRecord The ImpRecord object
     * @return the list of found ImpWorkflowNState objects
     * @throws SQLException
     */
    public List<ImpWorkflowNState> searchWorkflowOps(Context context, ImpRecord impRecord) throws SQLException;

    /***
     * Remove all ImpWorkflowNState objects.
     * 
     * @param context
     * @throws SQLException
     */
    public void deleteAll(Context context) throws SQLException;
}
