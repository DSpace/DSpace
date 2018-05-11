/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the Subscription object.
 * The implementation of this class is responsible for all business logic calls for the Subscription object and is autowired by spring
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SubscribeService {

    public List<Subscription> findAll(Context context) throws SQLException;

    /**
     * Subscribe an e-person to a collection. An e-mail will be sent every day a
     * new item appears in the collection.
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson to subscribe
     * @param collection
     *            Collection to subscribe to
     */
    public void subscribe(Context context, EPerson eperson,
            Collection collection) throws SQLException, AuthorizeException;

    /**
     * Unsubscribe an e-person to a collection. Passing in <code>null</code>
     * for the collection unsubscribes the e-person from all collections they
     * are subscribed to.
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson to unsubscribe
     * @param collection
     *            Collection to unsubscribe from
     */
    public void unsubscribe(Context context, EPerson eperson,
            Collection collection) throws SQLException, AuthorizeException;

    /**
     * Find out which collections an e-person is subscribed to
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson
     * @return array of collections e-person is subscribed to
     */
    public List<Subscription> getSubscriptions(Context context, EPerson eperson) throws SQLException;

    /**
     * Find out which collections the currently logged in e-person can subscribe to
     *
     * @param context
     *            DSpace context
     * @return array of collections the currently logged in e-person can subscribe to
     */
    public List<Collection> getAvailableSubscriptions(Context context)
            throws SQLException;

    /**
     * Find out which collections an e-person can subscribe to
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson
     * @return array of collections e-person can subscribe to
     */
    public List<Collection> getAvailableSubscriptions(Context context, EPerson eperson)
            throws SQLException;

    /**
     * Is that e-person subscribed to that collection?
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            find out if this e-person is subscribed
     * @param collection
     *            find out if subscribed to this collection
     * @return <code>true</code> if they are subscribed
     */
    public boolean isSubscribed(Context context, EPerson eperson,
            Collection collection) throws SQLException;

    public void deleteByCollection(Context context, Collection collection) throws SQLException;

    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException;
}
