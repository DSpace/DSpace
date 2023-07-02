/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class defining methods for sending new item e-mail alerts to users.
 *
 * @author Robert Tansley
 */
public class SubscribeServiceImpl implements SubscribeService {

    private final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    private SubscriptionDAO subscriptionDAO;
    @Autowired(required = true)
    private AuthorizeService authorizeService;
    @Autowired(required = true)
    private CollectionService collectionService;

    @Override
    public List<Subscription> findAll(Context context, String resourceType, Integer limit, Integer offset)
            throws Exception {
        if (StringUtils.isBlank(resourceType)) {
            return subscriptionDAO.findAllOrderedByDSO(context.getSession(), limit, offset);
        } else {
            if (resourceType.equals(Collection.class.getSimpleName()) ||
                resourceType.equals(Community.class.getSimpleName())) {
                return subscriptionDAO.findAllOrderedByIDAndResourceType(context.getSession(),
                        resourceType, limit, offset);
            } else {
                log.error("Resource type must be Collection or Community");
                throw new Exception("Resource type must be Collection or Community");
            }
        }
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
            Subscription newSubscription = subscriptionDAO.create(context.getSession(), new Subscription());
            subscriptionParameterList.forEach(subscriptionParameter ->
                    newSubscription.addParameter(subscriptionParameter));
            newSubscription.setEPerson(eperson);
            newSubscription.setDSpaceObject(dSpaceObject);
            newSubscription.setSubscriptionType(type);
            return newSubscription;
        } else {
            throw new AuthorizeException("Only admin or e-person themselves can subscribe");
        }
    }

    @Override
    public void unsubscribe(Context context, EPerson eperson, DSpaceObject dSpaceObject)
            throws SQLException, AuthorizeException {
        // Check authorisation. Must be administrator, or the eperson.
        if (authorizeService.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                .getCurrentUser().getID().equals(eperson.getID())))) {
            if (dSpaceObject == null) {
                // Unsubscribe from all
                subscriptionDAO.deleteByEPerson(context.getSession(), eperson);
            } else {
                subscriptionDAO.deleteByDSOAndEPerson(context.getSession(), dSpaceObject, eperson);

                log.info(LogHelper.getHeader(context, "unsubscribe",
                                              "eperson_id=" + eperson.getID() + ",collection_id="
                                                  + dSpaceObject.getID()));
            }
        } else {
            throw new AuthorizeException("Only admin or e-person themselves can unsubscribe");
        }
    }

    @Override
    public List<Subscription> findSubscriptionsByEPerson(Context context, EPerson eperson, Integer limit,Integer offset)
            throws SQLException {
        return subscriptionDAO.findByEPerson(context.getSession(), eperson, limit, offset);
    }

    @Override
    public List<Subscription> findSubscriptionsByEPersonAndDso(Context context, EPerson eperson,
                                                               DSpaceObject dSpaceObject,
                                                               Integer limit, Integer offset) throws SQLException {
        return subscriptionDAO.findByEPersonAndDso(context.getSession(), eperson, dSpaceObject, limit, offset);
    }

    @Override
    public List<Collection> findAvailableSubscriptions(Context context) throws SQLException {
        return findAvailableSubscriptions(context, null);
    }

    @Override
    public List<Collection> findAvailableSubscriptions(Context context, EPerson eperson) throws SQLException {
        if (Objects.nonNull(eperson)) {
            context.setCurrentUser(eperson);
        }
        return collectionService.findAuthorized(context, null, Constants.ADD);
    }

    @Override
    public boolean isSubscribed(Context context, EPerson eperson, DSpaceObject dSpaceObject) throws SQLException {
        return subscriptionDAO.findByEPersonAndDso(context.getSession(), eperson, dSpaceObject, -1, -1) != null;
    }

    @Override
    public void deleteByDspaceObject(Context context, DSpaceObject dSpaceObject) throws SQLException {
        subscriptionDAO.deleteByDspaceObject(context.getSession(), dSpaceObject);
    }

    @Override
    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException {
        subscriptionDAO.deleteByEPerson(context.getSession(), ePerson);
    }

    @Override
    public Subscription findById(Context context, int id) throws SQLException {
        return subscriptionDAO.findByID(context.getSession(), Subscription.class, id);
    }

    @Override
    public Subscription updateSubscription(Context context, Integer id, String subscriptionType,
                                           List<SubscriptionParameter> subscriptionParameterList)
                                           throws SQLException {
        Subscription subscriptionDB = subscriptionDAO.findByID(context.getSession(), Subscription.class, id);
        subscriptionDB.removeParameterList();
        subscriptionDB.setSubscriptionType(subscriptionType);
        subscriptionParameterList.forEach(x -> subscriptionDB.addParameter(x));
        subscriptionDAO.save(context.getSession(), subscriptionDB);
        return subscriptionDB;
    }

    @Override
    public Subscription addSubscriptionParameter(Context context, Integer id, SubscriptionParameter subscriptionParam)
            throws SQLException {
        Subscription subscriptionDB = subscriptionDAO.findByID(context.getSession(), Subscription.class, id);
        subscriptionDB.addParameter(subscriptionParam);
        subscriptionDAO.save(context.getSession(), subscriptionDB);
        return subscriptionDB;
    }

    @Override
    public Subscription removeSubscriptionParameter(Context context,Integer id, SubscriptionParameter subscriptionParam)
            throws SQLException {
        Subscription subscriptionDB = subscriptionDAO.findByID(context.getSession(), Subscription.class, id);
        subscriptionDB.removeParameter(subscriptionParam);
        subscriptionDAO.save(context.getSession(), subscriptionDB);
        return subscriptionDB;
    }

    @Override
    public void deleteSubscription(Context context, Subscription subscription) throws SQLException {
        subscriptionDAO.delete(context.getSession(), subscription);
    }

    @Override
    public List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
            String subscriptionType, String frequencyValue) throws SQLException {
        return subscriptionDAO.findAllSubscriptionsBySubscriptionTypeAndFrequency(context.getSession(),
                subscriptionType, frequencyValue);
    }

    @Override
    public Long countAll(Context context) throws SQLException {
        return subscriptionDAO.countAll(context.getSession());
    }

    @Override
    public Long countSubscriptionsByEPerson(Context context, EPerson ePerson) throws SQLException {
        return subscriptionDAO.countAllByEPerson(context.getSession(), ePerson);
    }

    @Override
    public Long countByEPersonAndDSO(Context context, EPerson ePerson, DSpaceObject dSpaceObject)
            throws SQLException {
        return subscriptionDAO.countAllByEPersonAndDso(context.getSession(), ePerson, dSpaceObject);
    }

}
