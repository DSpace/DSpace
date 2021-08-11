/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the Subscription object.
 * The implementation of this class is responsible for all business logic calls for the Subscription object and is
 * autowired by spring
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SubscriptionParameterService {
    /**
     * Subscribe an e-person to a collection. An e-mail will be sent every day a
     * new item appears in the collection.
     *
     * @param context DSpace context
     * @return list of Subscription objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<SubscriptionParameter> findAll(Context context) throws SQLException;

    /**
     * Subscribe an e-person to a collection. An e-mail will be sent every day a
     * new item appears in the collection.
     *
     * @param context    DSpace context
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public SubscriptionParameter add(Context context, String value,
                                  String name,
                                  Subscription subscription) throws SQLException, AuthorizeException;

    /**
     * Subscribe an e-person to a collection. An e-mail will be sent every day a
     * new item appears in the collection.
     *
     * @param context    DSpace context
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public SubscriptionParameter edit(Context context, Integer id, String value,
                                     String name,
                                     Subscription subscription) throws SQLException, AuthorizeException;

    /**
     * Finds a subscription by id
     *
     * @param context DSpace context
     * @param id the id of subscription to be searched
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public SubscriptionParameter findById(Context context, int id) throws SQLException;


    /**
     * Deletes a subscription
     *
     * @param context DSpace context
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void deleteSubscriptionParameter(Context context, Integer id) throws SQLException, AuthorizeException;


}
