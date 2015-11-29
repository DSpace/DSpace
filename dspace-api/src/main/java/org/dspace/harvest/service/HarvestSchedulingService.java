/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import org.dspace.authorize.AuthorizeException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Service interface class for the scheduling of harvesting tasks.
 * The implementation of this class is responsible for all business logic calls for the harvesting tasks and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HarvestSchedulingService {

    /**
     * Start harvest scheduler.
     */
    void startNewScheduler() throws SQLException, AuthorizeException;

    /**
     * Stop an active harvest scheduler.
     */
    void stopScheduler() throws SQLException, AuthorizeException;

    /**
   	 * Pause an active harvest scheduler.
   	 */
    void pauseScheduler() throws SQLException, AuthorizeException;

    /**
   	 * Resume a paused harvest scheduler.
   	 */
    void resumeScheduler() throws SQLException, AuthorizeException;


    void resetScheduler() throws SQLException, AuthorizeException, IOException;
}
