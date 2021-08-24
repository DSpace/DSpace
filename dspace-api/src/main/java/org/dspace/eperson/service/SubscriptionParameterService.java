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
import org.dspace.core.Context;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;

/**
 * Service interface class for the SubscriptionParameter object.
 * The implementation of this class is responsible for all business logic calls for
 * the SubscriptionParameter object and is autowired by spring
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author Alba Aliu @atis.al
 */
public interface SubscriptionParameterService {
    /**
     * Finds all of subscriptions parameter
     *
     * @param context DSpace context
     * @return list of Subscription objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<SubscriptionParameter> findAll(Context context) throws SQLException;

    /**
     * Adds a new subscription parameter related with a subscription
     *
     * @param context    DSpace context
     * @param value      String value
     * @param name       String name
     * @param subscription    Subscription subscription
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public SubscriptionParameter add(Context context, String value,
                                  String name,
                                  Subscription subscription) throws SQLException, AuthorizeException;

    /**
     * Updates a  subscription parameter with id
     *
     * @param context    DSpace context
     * @param id    Integer id
     * @param value      String value
     * @param name       String name
     * @param subscription    Subscription subscription
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public SubscriptionParameter edit(Context context, Integer id, String value,
                                     String name,
                                     Subscription subscription) throws SQLException, AuthorizeException;

    /**
     * Finds a subscriptionParameter by id
     *
     * @param context DSpace context
     * @param id the id of subscriptionParameter to be searched
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public SubscriptionParameter findById(Context context, int id) throws SQLException;


    /**
     * Deletes a subscriptionParameter with id
     *
     * @param context DSpace context
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void deleteSubscriptionParameter(Context context, Integer id) throws SQLException, AuthorizeException;


}
