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

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessLog;

/**
 * This is a DAO class that deals with DB calls for the {@link ProcessLog} entity
 */
public interface ProcessLogDAO extends GenericDAO<ProcessLog> {

    /**
     * This method will return a list of {@link ProcessLog} objects for the given {@link Process}
     * @param context   The relevant DSpace context
     * @param process   The {@link Process} from which we'll retrieve the {@link ProcessLog} objects
     * @return          The list of {@link ProcessLog} objects
     * @throws SQLException If something goes wrong
     */
    public List<ProcessLog> findByProcess(Context context, Process process) throws SQLException;
}
