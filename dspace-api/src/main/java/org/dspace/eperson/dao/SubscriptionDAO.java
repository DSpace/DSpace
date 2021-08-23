/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;

/**
 * Database Access Object interface class for the Subscription object.
 * The implementation of this class is responsible for all database calls for the Subscription object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SubscriptionDAO extends GenericDAO<Subscription> {

    public void deleteByDspaceObject(Context context, DSpaceObject dSpaceObject) throws SQLException;

    public List<Subscription> findByEPerson(Context context,
                                            EPerson eperson, Integer limit, Integer offset) throws SQLException;

    public List<Subscription> findByEPersonAndDso(Context context,
                                                  EPerson eperson, DSpaceObject dSpaceObject,
                                                  Integer limit, Integer offset) throws SQLException;

    public void deleteByEPerson(Context context, EPerson eperson) throws SQLException;

    public void deleteByDSOAndEPerson(Context context, DSpaceObject dSpaceObject, EPerson eperson)
            throws SQLException;

    public List<Subscription> findAllOrderedByEPersonAndResourceType(Context context, String resourceType,
                                                      Integer limit, Integer offset) throws SQLException;

    public List<Subscription> findAllOrderedByEPerson(Context context) throws SQLException;
}
