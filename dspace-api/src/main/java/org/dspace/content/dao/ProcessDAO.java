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

    public List<Process> findAllSortByScript(Context context) throws SQLException;
    public List<Process> findAllSortByStartTime(Context context) throws SQLException;
}
