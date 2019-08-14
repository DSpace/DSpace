/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Process;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface ProcessDAO extends GenericDAO<Process> {

    /**
     * This method will return all the Process objects in the database in a list and it'll be sorted by script name
     * @param context   The relevant DSpace context
     * @return          The list of all Process objects in the database sorted on scriptname
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByScript(Context context) throws SQLException;

    /**
     * This method will return all the Process objects in the database in a list and it'll be sorted by start time
     * @param context   The relevant DSpace context
     * @return          The list of all Process objects in the database sorted by starttime
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByStartTime(Context context) throws SQLException;
}
