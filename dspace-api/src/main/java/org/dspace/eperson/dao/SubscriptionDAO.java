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

    /**
     * Delete all subscription of provided dSpaceObject
     * 
     * @param context         DSpace context object
     * @param dSpaceObject    DSpace resource
     * @throws SQLException   If database error
     */
    public void deleteByDspaceObject(Context context, DSpaceObject dSpaceObject) throws SQLException;

    /**
     * Return a paginated list of all subscriptions of the eperson
     * 
     * @param context        DSpace context object
     * @param eperson        ePerson whose subscriptions want to find
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findByEPerson(Context context, EPerson eperson, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Return a paginated list of subscriptions related to a DSpaceObject belong to an ePerson
     * 
     * @param context        DSpace context object
     * @param eperson        ePerson whose subscriptions want to find
     * @param dSpaceObject   DSpaceObject of whom subscriptions want to find
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findByEPersonAndDso(Context context, EPerson eperson, DSpaceObject dSpaceObject,
                                                  Integer limit, Integer offset) throws SQLException;

    /**
     * Delete all subscription of provided ePerson
     * 
     * @param context        DSpace context object
     * @param eperson        ePerson whose subscriptions want to delete
     * @throws SQLException  If database error
     */
    public void deleteByEPerson(Context context, EPerson eperson) throws SQLException;

    /**
     * Delete all subscriptions related to a DSpaceObject belong to an ePerson
     * 
     * @param context        DSpace context object
     * @param dSpaceObject   DSpaceObject of whom subscriptions want to delete
     * @param eperson        ePerson whose subscriptions want to delete
     * @throws SQLException  If database error
     */
    public void deleteByDSOAndEPerson(Context context, DSpaceObject dSpaceObject, EPerson eperson) throws SQLException;

    /**
     * Return a paginated list of all subscriptions ordered by ID and resourceType
     * 
     * @param context        DSpace context object
     * @param resourceType   Could be Collection or Community
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findAllOrderedByIDAndResourceType(Context context, String resourceType,
                                                                Integer limit, Integer offset) throws SQLException;

    /**
     * Return a paginated list of subscriptions ordered by DSpaceObject
     * 
     * @param context        DSpace context object
     * @param limit          Paging limit
     * @param offset         The position of the first result to return
     * @return
     * @throws SQLException  If database error
     */
    public List<Subscription> findAllOrderedByDSO(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * Return a list of all subscriptions by subscriptionType and frequency
     * 
     * @param context           DSpace context object
     * @param subscriptionType  Could be "content" or "statistics". NOTE: in DSpace we have only "content"
     * @param frequencyValue    Could be "D" stand for Day, "W" stand for Week, and "M" stand for Month
     * @return
     * @throws SQLException     If database error
     */
    public List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
            String subscriptionType, String frequencyValue) throws SQLException;

    /**
     * Count all subscriptions
     * 
     * @param context        DSpace context object
     * @return               Total of all subscriptions
     * @throws SQLException  If database error
     */
    public Long countAll(Context context) throws SQLException;

    /**
     * Count all subscriptions belong to an ePerson
     * 
     * @param context        DSpace context object
     * @param ePerson        ePerson whose subscriptions want count
     * @return               Total of all subscriptions belong to an ePerson
     * @throws SQLException  If database error
     */
    public Long countAllByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * Count all subscriptions related to a DSpaceObject belong to an ePerson
     * 
     * @param context        DSpace context object
     * @param ePerson        ePerson whose subscriptions want count
     * @param dSpaceObject   DSpaceObject of whom subscriptions want count
     * @return
     * @throws SQLException  If database error
     */
    public Long countAllByEPersonAndDso(Context context, EPerson ePerson,DSpaceObject dSpaceObject) throws SQLException;

}
