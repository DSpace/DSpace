/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Process;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;

public interface ProcessService {

    public Process create(Context context, EPerson ePerson, String scriptName,
                          List<DSpaceCommandLineParameter> parameters) throws SQLException;

    public Process find(Context context, int processId) throws SQLException;

    public List<Process> findAll(Context context) throws SQLException;

    public List<Process> findAllSortByScript(Context context) throws SQLException;

    public List<Process> findAllSortByStartTime(Context context) throws SQLException;

    public void start(Context context, Process process) throws SQLException;

    public void fail(Context context, Process process) throws SQLException;

    public void complete(Context context, Process process) throws SQLException;

    public void appendFile(Context context, Process process, InputStream is, String type)
        throws IOException, SQLException, AuthorizeException;

    public void delete(Context context, Process process) throws SQLException;

    public void update(Context context, Process process) throws SQLException;

    public List<DSpaceCommandLineParameter> getParameters(Process process);
}
