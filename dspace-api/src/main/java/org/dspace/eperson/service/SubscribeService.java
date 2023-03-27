/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;

/**
 * Service interface class for the Subscription object.
 * The implementation of this class is responsible for all business logic calls for the Subscription object and is
 * autowired by spring
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SubscribeService {

    /**
     * Subscribe an e-person to a collection. An e-mail will be sent every day a
     * new item appears in the collection.
     *
     * @param context DSpace context
     * @param limit   Number of subscriptions to return
     * @param offset  Offset number
     * @return list of Subscription objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Subscription> findAll(Context context, String resourceType, Integer limit, Integer offset)
            throws Exception;

    /**
     * Subscribe an EPerson to a dSpaceObject (Collection or Community). An e-mail will be sent every day a
     * new item appears in the Collection or Community.
     * 
     * @param context                 DSpace context object
     * @param eperson                 EPerson to subscribe
     * @param dSpaceObject            DSpaceObject to subscribe
     * @param subscriptionParameters  list of @SubscriptionParameter
     * @param subscriptionType        Currently supported only "content"
     * @return
     * @throws SQLException           An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException     Exception indicating the current user of the context does not have permission
     *                                to perform a particular action.
     */
    public Subscription subscribe(Context context, EPerson eperson, DSpaceObject dSpaceObject,
                                  List<SubscriptionParameter> subscriptionParameters,
                                  String subscriptionType) throws SQLException, AuthorizeException;

    /**
     * Unsubscribe an e-person to a collection. Passing in <code>null</code>
     * for the collection unsubscribes the e-person from all collections they
     * are subscribed to.
     *
     * @param context      DSpace context
     * @param eperson      EPerson to unsubscribe
     * @param dSpaceObject DSpaceObject to unsubscribe from
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public void unsubscribe(Context context, EPerson eperson, DSpaceObject dSpaceObject)
            throws SQLException, AuthorizeException;

    /**
     * Find out which collections an e-person is subscribed to
     *
     * @param context DSpace context
     * @param eperson EPerson
     * @param limit   Number of subscriptions to return
     * @param offset  Offset number
     * @return array of collections e-person is subscribed to
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Subscription> findSubscriptionsByEPerson(Context context, EPerson eperson, Integer limit,Integer offset)
            throws SQLException;

    /**
     * Find out which collections an e-person is subscribed to and related with dso
     *
     * @param context      DSpace context
     * @param eperson      EPerson
     * @param dSpaceObject DSpaceObject
     * @param limit        Number of subscriptions to return
     * @param offset       Offset number
     * @return array of collections e-person is subscribed to and related with dso
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Subscription> findSubscriptionsByEPersonAndDso(Context context, EPerson eperson,
                                                              DSpaceObject dSpaceObject,
                                                              Integer limit, Integer offset) throws SQLException;

    /**
     * Find out which collections the currently logged in e-person can subscribe to
     *
     * @param context DSpace context
     * @return array of collections the currently logged in e-person can subscribe to
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Collection> findAvailableSubscriptions(Context context) throws SQLException;

    /**
     * Find out which collections an e-person can subscribe to
     *
     * @param context DSpace context
     * @param eperson EPerson
     * @return array of collections e-person can subscribe to
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Collection> findAvailableSubscriptions(Context context, EPerson eperson) throws SQLException;

    /**
     * Is that e-person subscribed to that collection?
     *
     * @param context      DSpace context
     * @param eperson      find out if this e-person is subscribed
     * @param dSpaceObject find out if subscribed to this dSpaceObject
     * @return <code>true</code> if they are subscribed
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public boolean isSubscribed(Context context, EPerson eperson, DSpaceObject dSpaceObject) throws SQLException;

    /**
     * Delete subscription by collection.
     *
     * @param context      DSpace context
     * @param dSpaceObject find out if subscribed to this dSpaceObject
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void deleteByDspaceObject(Context context, DSpaceObject dSpaceObject) throws SQLException;

    /**
     * Delete subscription by eperson (subscriber).
     *
     * @param context DSpace context
     * @param ePerson find out if this e-person is subscribed
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * Finds a subscription by id
     *
     * @param context DSpace context
     * @param id      the id of subscription to be searched
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public Subscription findById(Context context, int id) throws SQLException;

    /**
     * Updates a subscription by id
     *
     * @param context                   DSpace context
     * @param id                        Integer id
     * @param subscriptionParameterList List<SubscriptionParameter>  subscriptionParameterList
     * @param subscriptionType          type
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public Subscription updateSubscription(Context context, Integer id, String subscriptionType,
            List<SubscriptionParameter> subscriptionParameterList) throws SQLException;

    /**
     * Adds a parameter to a subscription
     *
     * @param context               DSpace context
     * @param id                    Integer id
     * @param subscriptionParameter SubscriptionParameter subscriptionParameter
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public Subscription addSubscriptionParameter(Context context,Integer id,
            SubscriptionParameter subscriptionParameter) throws SQLException;

    /**
     * Deletes a parameter from subscription
     *
     * @param context               DSpace context
     * @param id                    Integer id
     * @param subscriptionParam     SubscriptionParameter subscriptionParameter
     * @throws SQLException         An exception that provides information on a database access error or other errors.
     */
    public Subscription removeSubscriptionParameter(Context context, Integer id,
            SubscriptionParameter subscriptionParam) throws SQLException;

    /**
     * Deletes a subscription
     *
     * @param context DSpace context
     * @param subscription The subscription to delete
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void deleteSubscription(Context context, Subscription subscription) throws SQLException;

    /**
     * Finds all subscriptions by subscriptionType and frequency
     *
     * @param context             DSpace context
     * @param subscriptionType    Could be "content" or "statistics". NOTE: in DSpace we have only "content"
     * @param frequencyValue      Could be "D" stand for Day, "W" stand for Week, and "M" stand for Month
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     */
    public List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
            String subscriptionType, String frequencyValue) throws SQLException;

    /**
     * Counts all subscriptions
     *
     * @param context DSpace context
     */
    public Long countAll(Context context) throws SQLException;

    /**
     * Counts all subscriptions by ePerson
     *
     * @param context DSpace context
     * @param ePerson EPerson ePerson
     */
    public Long countSubscriptionsByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * Counts all subscriptions by ePerson and DSO
     *
     * @param context      DSpace context
     * @param ePerson      EPerson ePerson
     * @param dSpaceObject DSpaceObject dSpaceObject
     */
    public Long countByEPersonAndDSO(Context context, EPerson ePerson, DSpaceObject dSpaceObject) throws SQLException;

}