/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.dao;

import java.sql.SQLException;

import org.dspace.batch.ImpRecordToItem;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the ImpRecordToItem object. The
 * implementation of this class is responsible for all database calls for the
 * ImpRecordToItem object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public interface ImpRecordToItemDAO extends GenericDAO<ImpRecordToItem> {
    /***
     * Search the ImpRecordToItem objects by its PK.
     * 
     * @param context     The relevant DSpace Context
     * @param impRecordId The PK
     * @return the list of found ImpMetadatavalue objects
     * @throws SQLException
     */
    public ImpRecordToItem findByPK(Context context, String impRecordId) throws SQLException;

    /***
     * Remove all ImpRecordToItem objects.
     * 
     * @param context
     * @throws SQLException
     */
    public void deleteAll(Context context) throws SQLException;
}
