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
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the ImpRecord object. The
 * implementation of this class is responsible for all database calls for the
 * ImpRecord object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public interface ImpRecordDAO extends GenericDAO<ImpRecord> {
    /***
     * Search all ImpRecord objects with modification date null.
     * 
     * @param context The relevant DSpace Context
     * @return the list of found ImpRecord objects
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

    /***
     * Remove all ImpRecord objects.
     * 
     * @param context
     * @throws SQLException
     */
    public void deleteAll(Context context) throws SQLException;
}
