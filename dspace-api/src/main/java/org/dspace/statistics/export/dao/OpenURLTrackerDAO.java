/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.dao;

import org.dspace.core.GenericDAO;
import org.dspace.statistics.export.OpenURLTracker;

/**
 * Database Access Object interface class for the OpenURLTracker object.
 * The implementation of this class is responsible for all database calls for the OpenURLTracker object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 */
public interface OpenURLTrackerDAO extends GenericDAO<OpenURLTracker> {

}
