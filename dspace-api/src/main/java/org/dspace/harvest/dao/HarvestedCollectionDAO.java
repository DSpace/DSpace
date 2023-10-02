/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.GenericDAO;
import org.dspace.harvest.HarvestedCollection;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the HarvestedCollection object.
 * The implementation of this class is responsible for all database calls for the HarvestedCollection object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HarvestedCollectionDAO extends GenericDAO<HarvestedCollection> {

    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedDesc(Session session, int status, int type,
                                                                                  int limit) throws SQLException;

    public HarvestedCollection findByStatusAndMinimalTypeOrderByLastHarvestedAsc(Session session, int status, int type,
                                                                                 int limit) throws SQLException;

    public List<HarvestedCollection> findByStatus(Session session, int status) throws SQLException;

    public HarvestedCollection findByCollection(Session session, Collection collection) throws SQLException;

    List<HarvestedCollection> findByLastHarvestedAndHarvestTypeAndHarvestStatusesAndHarvestTime(Session session,
                                                                                                Date startTime,
                                                                                                int minimalType,
                                                                                                int[] statuses,
                                                                                                int expirationStatus,
                                                                                                Date expirationTime)
        throws SQLException;

    public int count(Session session) throws SQLException;

}
