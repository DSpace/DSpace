/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubscribeServiceImpl implements SubscribeService {
    /**
     * log4j logger
     */
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(SubscribeServiceImpl.class);

    @Autowired(required = true)
    protected SubscriptionDAO subscriptionDAO;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected CollectionService collectionService;

    protected SubscribeServiceImpl() {

    }

    @Override
    public List<Subscription> findAll(Context context) throws SQLException {
        return subscriptionDAO.findAllOrderedByEPerson(context);
    }

    @Override
    public Subscription subscribe(Context context, EPerson eperson,
                                  DSpaceObject dSpaceObject,
                                  List<SubscriptionParameter> subscriptionParameterList,
                                  String type) throws SQLException, AuthorizeException {
        // Check authorisation. Must be administrator, or the eperson.
        if (authorizeService.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                .getCurrentUser().getID().equals(eperson.getID())))) {
            if (!isSubscribed(context, eperson, dSpaceObject)) {
                Subscription new_subscription = subscriptionDAO.create(context, new Subscription());
                new_subscription.setSubscriptionParameterList(subscriptionParameterList);
                new_subscription.setePerson(eperson);
                new_subscription.setdSpaceObject(dSpaceObject);
                new_subscription.setType(type);
                return new_subscription;
            } else {
                throw new IllegalArgumentException("Subscription already exists");
            }
        } else {
            throw new AuthorizeException(
                    "Only admin or e-person themselves can subscribe");
        }
    }

    @Override
    public void unsubscribe(Context context, EPerson eperson,
                            Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation. Must be administrator, or the eperson.
        if (authorizeService.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                .getCurrentUser().getID().equals(eperson.getID())))) {
            if (collection == null) {
                // Unsubscribe from all
                subscriptionDAO.deleteByEPerson(context, eperson);
            } else {
                subscriptionDAO.deleteByCollectionAndEPerson(context, collection, eperson);

                log.info(LogHelper.getHeader(context, "unsubscribe",
                                              "eperson_id=" + eperson.getID() + ",collection_id="
                                                  + collection.getID()));
            }
        } else {
            throw new AuthorizeException(
                    "Only admin or e-person themselves can unsubscribe");
        }
    }

    @Override
    public List<Subscription> getSubscriptions(Context context, EPerson eperson)
            throws SQLException {
        return subscriptionDAO.findByEPerson(context, eperson);
    }

    @Override
    public List<Collection> getAvailableSubscriptions(Context context)
            throws SQLException {
        return getAvailableSubscriptions(context, null);
    }

    @Override
    public List<Collection> getAvailableSubscriptions(Context context, EPerson eperson)
            throws SQLException {
        List<Collection> collections;
        if (eperson != null) {
            context.setCurrentUser(eperson);
        }
        collections = collectionService.findAuthorized(context, null, Constants.ADD);

        return collections;
    }

    @Override
    public boolean isSubscribed(Context context, EPerson eperson,
                                DSpaceObject dSpaceObject) throws SQLException {
        return subscriptionDAO.findByCollectionAndEPerson(context, eperson, dSpaceObject) != null;
    }

    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException {
        subscriptionDAO.deleteByCollection(context, collection);
    }

    @Override
    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException {
        subscriptionDAO.deleteByEPerson(context, ePerson);
    }

    @Override
    public Subscription findById(Context context, int id) throws SQLException {
        return subscriptionDAO.findByID(context, Subscription.class, id);
    }

    @Override
    public Subscription updateSubscription(Context context, Integer id,
                                           EPerson eperson,
                                           DSpaceObject dSpaceObject,
                                           List<SubscriptionParameter> subscriptionParameterList,
                                           String type) throws SQLException, AuthorizeException {
        // must be admin or the subscriber of the subscription
        if (authorizeService.isAdmin(context, context.getCurrentUser()) || eperson.equals(context.getCurrentUser())) {
            Subscription subscription = subscriptionDAO.findByID(context, Subscription.class, id);
            subscription.setType(type);
            subscription.setdSpaceObject(dSpaceObject);
            subscription.setSubscriptionParameterList(subscriptionParameterList);
            subscription.setePerson(eperson);
            subscriptionDAO.save(context, subscription);
            return subscription;
        } else {
            throw new AuthorizeException("Only admin or e-person themselves can edit the subscription");
        }
    }

    @Override
    public void deleteSubscription(Context context, Integer id) throws SQLException, AuthorizeException {
        // initially find the eperson associated with the subscription
        Subscription subscription = subscriptionDAO.findByID(context, Subscription.class, id);
        if (subscription != null) {
            // must be admin or the subscriber of the subscription
            if (authorizeService.isAdmin(context, context.getCurrentUser()) || subscription.getePerson().equals(context.getCurrentUser())) {
                subscriptionDAO.delete(context, subscription);
            } else {
                throw new AuthorizeException("Only admin or e-person themselves can delete the subscription");
            }
        } else {
            throw new IllegalArgumentException("Subscription with id" + id + "is not found");
        }

    }
}
