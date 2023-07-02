/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Subscription object.
 * The implementation of this class is responsible for all database calls for the Subscription object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SubscriptionDAO extends GenericDAO<Subscription> {

    /**
     * Delete all subscription of provided dSpaceObject
     *
     * @param session         The current request's database context.
     * @param dSpaceObject    DSpace resource
     * @throws SQLException   If database error
     */
    public void deleteByDspaceObject(Session session, DSpaceObject dSpaceObject) throws SQLException;

    /**
     * Return a paginated list of all subscriptions of the eperson
     *
     * @param session        The current request's database context.
     * @param eperson        ePerson whose subscriptions want to find
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findByEPerson(Session session, EPerson eperson, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Return a paginated list of subscriptions related to a DSpaceObject belong to an ePerson
     *
     * @param session        The current request's database context.
     * @param eperson        ePerson whose subscriptions want to find
     * @param dSpaceObject   DSpaceObject of whom subscriptions want to find
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findByEPersonAndDso(Session session, EPerson eperson, DSpaceObject dSpaceObject,
                                                  Integer limit, Integer offset) throws SQLException;

    /**
     * Delete all subscription of provided ePerson
     *
     * @param session        The current request's database context.
     * @param eperson        ePerson whose subscriptions want to delete
     * @throws SQLException  If database error
     */
    public void deleteByEPerson(Session session, EPerson eperson) throws SQLException;

    /**
     * Delete all subscriptions related to a DSpaceObject belong to an ePerson
     *
     * @param session        The current request's database context.
     * @param dSpaceObject   DSpaceObject of whom subscriptions want to delete
     * @param eperson        ePerson whose subscriptions want to delete
     * @throws SQLException  If database error
     */
    public void deleteByDSOAndEPerson(Session session, DSpaceObject dSpaceObject, EPerson eperson) throws SQLException;

    /**
     * Return a paginated list of all subscriptions ordered by ID and resourceType
     *
     * @param session        The current request's database context.
     * @param resourceType   Could be Collection or Community
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findAllOrderedByIDAndResourceType(Session session, String resourceType,
                                                                Integer limit, Integer offset) throws SQLException;

    /**
     * Return a paginated list of subscriptions ordered by DSpaceObject
     *
     * @param session        The current request's database context.
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findAllOrderedByDSO(Session session, Integer limit, Integer offset) throws SQLException;

    /**
     * Return a list of all subscriptions by subscriptionType and frequency
     *
     * @param session           The current request's database context.
     * @param subscriptionType  Could be "content" or "statistics". NOTE: in DSpace we have only "content"
     * @param frequencyValue    Could be "D" stand for Day, "W" stand for Week, and "M" stand for Month
     * @return
     * @throws SQLException     If database error
     */
    public List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Session session,
            String subscriptionType, String frequencyValue) throws SQLException;

    /**
     * Count all subscriptions
     *
     * @param session        The current request's database context.
     * @return               Total of all subscriptions
     * @throws SQLException  If database error
     */
    public Long countAll(Session session) throws SQLException;

    /**
     * Count all subscriptions belong to an ePerson
     *
     * @param session        The current request's database context.
     * @param ePerson        ePerson whose subscriptions want count
     * @return               Total of all subscriptions belong to an ePerson
     * @throws SQLException  If database error
     */
    public Long countAllByEPerson(Session session, EPerson ePerson) throws SQLException;

    /**
     * Count all subscriptions related to a DSpaceObject belong to an ePerson
     *
     * @param session        The current request's database context.
     * @param ePerson        ePerson whose subscriptions want count
     * @param dSpaceObject   DSpaceObject of whom subscriptions want count
     * @return
     * @throws SQLException  If database error
     */
    public Long countAllByEPersonAndDso(Session session, EPerson ePerson,DSpaceObject dSpaceObject) throws SQLException;

}
