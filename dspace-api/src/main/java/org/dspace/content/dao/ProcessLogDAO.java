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

public interface ProcessLogDAO extends GenericDAO<ProcessLog> {

    public List<ProcessLog> findByProcess(Context context, Process process) throws SQLException;
}
